package arktwin.center.services

import akka.actor.typed.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Sink, Source}
import com.google.protobuf.empty.Empty
import arktwin.common.services.agent.{AgentCapability, AgentConfig, AgentResponse, AgentService, AgentTask}
import arktwin.common.services.agent.AgentServiceClient
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

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

  override def submitTask(in: AgentTask): Source[AgentResponse, akka.NotUsed] = {
    client.submitTask(in)
  }

  override def getAvailableAgents(in: AgentTask): Source[AgentConfig, akka.NotUsed] = {
    client.getAvailableAgents(in)
  }

  override def streamTaskResponses(in: AgentTask): Source[AgentResponse, akka.NotUsed] = {
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