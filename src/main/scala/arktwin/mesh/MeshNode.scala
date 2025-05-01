package arktwin.mesh

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.typed.{Cluster, Join}
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap
import org.apache.pekko.management.scaladsl.PekkoManagement
import io.kamon.Kamon
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object MeshNode {
  // Core message types for mesh communication
  sealed trait Message
  case class JoinMesh(seedNodes: Set[String]) extends Message
  case class LeaveMesh() extends Message
  case class MeshStatus(status: NodeStatus) extends Message
  case class TaskAssignment(task: MeshTask) extends Message
  case class TaskResult(taskId: String, result: Any) extends Message
  
  // Node status tracking
  case class NodeStatus(
    nodeId: String,
    isActive: Boolean,
    load: Double,
    neighbors: Set[String],
    capabilities: Set[String]
  )
  
  // Task definition
  case class MeshTask(
    taskId: String,
    priority: Int,
    dependencies: Set[String],
    payload: Any,
    requiredCapabilities: Set[String]
  )
  
  // JSON codecs
  implicit val nodeStatusEncoder: Encoder[NodeStatus] = deriveEncoder
  implicit val nodeStatusDecoder: Decoder[NodeStatus] = deriveDecoder
  implicit val meshTaskEncoder: Encoder[MeshTask] = deriveEncoder
  implicit val meshTaskDecoder: Decoder[MeshTask] = deriveDecoder
  
  def apply(
    nodeId: String,
    config: MeshConfig
  ): Behavior[Message] = Behaviors.setup { context =>
    // Initialize Kamon monitoring
    Kamon.init()
    
    // Start Pekko Management and Cluster Bootstrap
    PekkoManagement(context.system).start()
    ClusterBootstrap(context.system).start()
    
    // Join the cluster
    val cluster = Cluster(context.system)
    if (config.seedNodes.nonEmpty) {
      cluster.manager ! Join(config.seedNodes.head)
    }
    
    // Initialize metrics
    val nodeLoadGauge = Kamon.gauge("mesh.node.load").withTag("node", nodeId)
    val neighborCountGauge = Kamon.gauge("mesh.node.neighbors").withTag("node", nodeId)
    val taskCounter = Kamon.counter("mesh.node.tasks").withTag("node", nodeId)
    
    // State
    var status = NodeStatus(
      nodeId = nodeId,
      isActive = true,
      load = 0.0,
      neighbors = Set.empty,
      capabilities = config.capabilities
    )
    
    // Task queue with priority
    val taskQueue = scala.collection.mutable.PriorityQueue[MeshTask]()(
      Ordering.by[MeshTask, Int](_.priority).reverse
    )
    
    Behaviors.receiveMessage {
      case JoinMesh(seedNodes) =>
        seedNodes.foreach { node =>
          cluster.manager ! Join(node)
        }
        Behaviors.same
        
      case LeaveMesh() =>
        status = status.copy(isActive = false)
        cluster.manager ! Leave(cluster.selfMember.address)
        Behaviors.same
        
      case MeshStatus(newStatus) =>
        status = newStatus
        nodeLoadGauge.update(status.load)
        neighborCountGauge.update(status.neighbors.size)
        Behaviors.same
        
      case TaskAssignment(task) =>
        taskCounter.increment()
        taskQueue.enqueue(task)
        // TODO: Implement task scheduling and execution
        Behaviors.same
        
      case TaskResult(taskId, result) =>
        // TODO: Handle task results and update metrics
        Behaviors.same
    }
  }
}

// Configuration for mesh nodes
case class MeshConfig(
  seedNodes: Set[String],
  capabilities: Set[String],
  maxLoad: Double = 1.0,
  taskTimeout: java.time.Duration = java.time.Duration.ofSeconds(30)
) 