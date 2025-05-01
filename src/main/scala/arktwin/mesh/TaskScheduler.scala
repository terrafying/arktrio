package arktwin.mesh

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.typed.Cluster
import io.kamon.Kamon
import scala.collection.mutable
import java.time.Instant
import scala.concurrent.duration._

object TaskScheduler {
  sealed trait Message
  case class ScheduleTask(task: MeshTask) extends Message
  case class TaskCompleted(taskId: String, result: Any) extends Message
  case class NodeStatusUpdate(nodeId: String, status: MeshNode.NodeStatus) extends Message
  case class CheckTaskTimeout() extends Message
  
  private case class ScheduledTask(
    task: MeshTask,
    assignedTo: String,
    scheduledAt: Instant,
    dependencies: Set[String]
  )
  
  def apply(
    nodeId: String,
    config: MeshConfig
  ): Behavior[Message] = Behaviors.setup { context =>
    // Initialize metrics
    val taskLatencyHistogram = Kamon.histogram("mesh.task.latency")
    val taskQueueSizeGauge = Kamon.gauge("mesh.task.queue.size")
    val taskDependencyGraphSizeGauge = Kamon.gauge("mesh.task.dependency.graph.size")
    
    // State
    val taskQueue = mutable.PriorityQueue[MeshTask]()(
      Ordering.by[MeshTask, Int](_.priority).reverse
    )
    val scheduledTasks = mutable.Map[String, ScheduledTask]()
    val nodeStatuses = mutable.Map[String, MeshNode.NodeStatus]()
    val dependencyGraph = mutable.Map[String, Set[String]]()
    
    // Start periodic timeout check
    context.system.scheduler.scheduleAtFixedRate(
      1.second,
      1.second,
      () => context.self ! CheckTaskTimeout()
    )
    
    def canExecuteTask(task: MeshTask, nodeStatus: MeshNode.NodeStatus): Boolean = {
      val hasCapabilities = task.requiredCapabilities.subsetOf(nodeStatus.capabilities)
      val hasCapacity = nodeStatus.load < config.maxLoad
      val dependenciesMet = task.dependencies.forall(depId => 
        scheduledTasks.get(depId).exists(_.task.completed)
      )
      hasCapabilities && hasCapacity && dependenciesMet
    }
    
    def findBestNode(task: MeshTask): Option[String] = {
      nodeStatuses
        .filter { case (_, status) => canExecuteTask(task, status) }
        .minByOption { case (_, status) => status.load }
        .map { case (nodeId, _) => nodeId }
    }
    
    Behaviors.receiveMessage {
      case ScheduleTask(task) =>
        taskQueue.enqueue(task)
        taskQueueSizeGauge.update(taskQueue.size)
        
        // Update dependency graph
        dependencyGraph.getOrElseUpdate(task.taskId, task.dependencies)
        taskDependencyGraphSizeGauge.update(dependencyGraph.size)
        
        // Try to schedule the task
        findBestNode(task).foreach { nodeId =>
          val scheduledTask = ScheduledTask(
            task = task,
            assignedTo = nodeId,
            scheduledAt = Instant.now(),
            dependencies = task.dependencies
          )
          scheduledTasks(task.taskId) = scheduledTask
          // TODO: Send task to node
        }
        Behaviors.same
        
      case TaskCompleted(taskId, result) =>
        scheduledTasks.get(taskId).foreach { scheduledTask =>
          val latency = java.time.Duration.between(
            scheduledTask.scheduledAt,
            Instant.now()
          ).toMillis
          taskLatencyHistogram.record(latency)
          
          // Update node status
          nodeStatuses.get(scheduledTask.assignedTo).foreach { status =>
            nodeStatuses(scheduledTask.assignedTo) = status.copy(
              load = status.load - 0.1 // Reduce load after task completion
            )
          }
          
          // Remove from scheduled tasks
          scheduledTasks.remove(taskId)
          
          // Check if any waiting tasks can now be scheduled
          taskQueue.foreach { task =>
            if (canExecuteTask(task, nodeStatuses(scheduledTask.assignedTo))) {
              findBestNode(task).foreach { nodeId =>
                val newScheduledTask = ScheduledTask(
                  task = task,
                  assignedTo = nodeId,
                  scheduledAt = Instant.now(),
                  dependencies = task.dependencies
                )
                scheduledTasks(task.taskId) = newScheduledTask
                // TODO: Send task to node
              }
            }
          }
        }
        Behaviors.same
        
      case NodeStatusUpdate(nodeId, status) =>
        nodeStatuses(nodeId) = status
        Behaviors.same
        
      case CheckTaskTimeout() =>
        val now = Instant.now()
        scheduledTasks.foreach {
          case (taskId, scheduledTask) =>
            val duration = java.time.Duration.between(scheduledTask.scheduledAt, now)
            if (duration.compareTo(config.taskTimeout) > 0) {
              // Task has timed out, reschedule
              scheduledTasks.remove(taskId)
              taskQueue.enqueue(scheduledTask.task)
              // TODO: Notify about timeout
            }
        }
        Behaviors.same
    }
  }
} 