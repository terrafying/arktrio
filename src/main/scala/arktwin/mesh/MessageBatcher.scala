package arktwin.mesh

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import io.kamon.Kamon
import scala.collection.mutable
import scala.concurrent.duration._
import java.time.Instant

object MessageBatcher {
  sealed trait Message
  case class EnqueueMessage(msg: Any, target: String) extends Message
  case class FlushBatch() extends Message
  case class BatchConfig(
    maxBatchSize: Int,
    maxBatchDelay: FiniteDuration,
    maxMessageSize: Int
  )
  
  private case class BatchedMessage(
    messages: Vector[Any],
    target: String,
    timestamp: Instant
  )
  
  def apply(
    config: BatchConfig,
    materializer: Materializer
  ): Behavior[Message] = Behaviors.setup { context =>
    // Initialize metrics
    val batchSizeHistogram = Kamon.histogram("mesh.batch.size")
    val batchLatencyHistogram = Kamon.histogram("mesh.batch.latency")
    val messageQueueSizeGauge = Kamon.gauge("mesh.message.queue.size")
    
    // State
    val messageQueues = mutable.Map[String, mutable.Queue[Any]]()
    val batchTimestamps = mutable.Map[String, Instant]()
    
    // Create message processing flow
    val messageFlow = Flow[BatchedMessage]
      .map { batch =>
        batchSizeHistogram.record(batch.messages.size)
        batchLatencyHistogram.record(
          java.time.Duration.between(batch.timestamp, Instant.now()).toMillis
        )
        // TODO: Send batch to target
        batch
      }
    
    // Create message source
    val (queue, source) = Source
      .queue[BatchedMessage](1000, OverflowStrategy.backpressure)
      .via(messageFlow)
      .to(Sink.ignore)
      .run()(materializer)
    
    def createBatch(target: String): Option[BatchedMessage] = {
      messageQueues.get(target).flatMap { queue =>
        if (queue.nonEmpty) {
          val messages = queue.dequeueAll(_ => true).toVector
          if (messages.nonEmpty) {
            Some(BatchedMessage(
              messages = messages,
              target = target,
              timestamp = batchTimestamps.remove(target).getOrElse(Instant.now())
            ))
          } else None
        } else None
      }
    }
    
    def scheduleBatch(target: String): Unit = {
      if (!batchTimestamps.contains(target)) {
        batchTimestamps(target) = Instant.now()
        context.system.scheduler.scheduleOnce(
          config.maxBatchDelay,
          () => context.self ! FlushBatch()
        )
      }
    }
    
    Behaviors.receiveMessage {
      case EnqueueMessage(msg, target) =>
        messageQueues.getOrElseUpdate(target, mutable.Queue.empty).enqueue(msg)
        messageQueueSizeGauge.update(messageQueues.values.map(_.size).sum)
        
        // Check if we should create a batch
        if (messageQueues(target).size >= config.maxBatchSize) {
          createBatch(target).foreach { batch =>
            queue.offer(batch)
          }
        } else {
          scheduleBatch(target)
        }
        Behaviors.same
        
      case FlushBatch() =>
        messageQueues.keys.foreach { target =>
          createBatch(target).foreach { batch =>
            queue.offer(batch)
          }
        }
        Behaviors.same
    }
  }
} 