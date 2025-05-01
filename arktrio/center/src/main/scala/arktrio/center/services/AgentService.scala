package arktrio.center.services

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import com.google.protobuf.empty.Empty
import arktrio.common.services.agent.{AgentCapability, AgentConfig, AgentResponse, AgentService, AgentTask}
import arktrio.common.services.agent.AgentServiceClient
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import org.apache.pekko.NotUsed

class AgentServiceImpl(
    system: ActorSystem[_],
    pythonAgentServiceUrl: String
)(implicit ec: ExecutionContext) extends AgentService {

  private val client = AgentServiceClient(
    GrpcClientSettings.connectToServiceAt(
      host = pythonAgentServiceUrl.split(":")(0),
      port = pythonAgentServiceUrl.split(":")(1).toInt
    ).withTls(false)
  )

  override def registerAgent(in: AgentConfig): Future[Empty] = {
    client.registerAgent(in)
  }

  override def submitTask(in: AgentTask): Source[AgentResponse, NotUsed] = {
    client.submitTask(in)
  }

  override def getAvailableAgents(in: AgentTask): Source[AgentConfig, NotUsed] = {
    client.getAvailableAgents(in)
  }

  override def streamTaskResponses(in: AgentTask): Source[AgentResponse, NotUsed] = {
    client.streamTaskResponses(in)
  }
}

object AgentServiceImpl {
  def apply(
      system: ActorSystem[_],
      pythonAgentServiceUrl: String
  )(implicit ec: ExecutionContext): AgentServiceImpl = {
    new AgentServiceImpl(system, pythonAgentServiceUrl)
  }
} 