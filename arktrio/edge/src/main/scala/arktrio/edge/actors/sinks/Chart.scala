// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.edge.actors.sinks

import arktrio.center.services.ChartAgent
import arktrio.common.data.TimestampExtensions.*
import arktrio.common.data.Vector3EnuExtensions.*
import arktrio.common.data.{TaggedTimestamp, VirtualTag}
import arktrio.common.util.BehaviorsExtensions.*
import arktrio.common.util.MailboxConfig
import arktrio.edge.actors.EdgeConfigurator
import arktrio.edge.configs.CullingConfig
import arktrio.edge.util.CommonMessages.Nop
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.dispatch.ControlMessage

import scala.collection.mutable

object Chart:
  type Message = Catch | Get | UpdateFirstAgents | CullingConfig | Nop.type
  case class Catch(agent: ChartAgent)
  case class Get(replyTo: ActorRef[ReadReply]) extends ControlMessage
  case class UpdateFirstAgents(agents: Seq[ChartAgent])

  case class ReadReply(sortedAgents: Seq[CullingAgent]) extends AnyVal
  case class CullingAgent(agent: ChartAgent, nearestDistance: Option[Double])

  def spawn(
      initCullingConfig: CullingConfig
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initCullingConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      initCullingConfig: CullingConfig
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    context.system.receptionist ! Receptionist.Register(
      EdgeConfigurator.cullingObserverKey,
      context.self
    )

    var cullingConfig = initCullingConfig
    val distances = mutable.Map[String, Double]()
    val orderedAgents = mutable.TreeMap[(Double, String), ChartAgent]()
    var firstAgents = Seq[ChartAgent]()

    Behaviors.receiveMessage:
      case Catch(agent) =>
        val oldDistance = distances.get(agent.agentId)
        val oldTimestamp = oldDistance
          .map(orderedAgents(_, agent.agentId).transform.timestamp.tagVirtual)
          .getOrElse(TaggedTimestamp.minValue[VirtualTag])
        if agent.transform.timestamp.tagVirtual >= oldTimestamp then
          for od <- oldDistance do orderedAgents -= ((od, agent.agentId))

          // TODO consider relative coordinates
          // TODO extrapolate first agents based on previous transforms?
          val distance = firstAgents
            .map(a =>
              agent.transform.localTranslationMeter.distance(a.transform.localTranslationMeter)
            )
            .minOption
            .getOrElse(Double.PositiveInfinity)
          distances += agent.agentId -> distance
          orderedAgents += (distance, agent.agentId) -> agent
        Behaviors.same

      case Get(actorRef) =>
        actorRef ! ReadReply(orderedAgents.toSeq.map:
          case ((dist, _), agent) =>
            CullingAgent(agent, Some(dist).filter(_.isFinite)))
        Behaviors.same

      case UpdateFirstAgents(newFirstAgents) =>
        if cullingConfig.edgeCulling then
          if newFirstAgents.size <= cullingConfig.maxFirstAgents then firstAgents = newFirstAgents
          else
            if firstAgents.nonEmpty then
              logger.warn(
                "edge culling is disabled because first agents is greater than arktrio.edge.culling.maxFirstAgents"
              )
            firstAgents = Seq()
        else firstAgents = Seq()
        Behaviors.same

      case newCullingConfig: CullingConfig =>
        cullingConfig = newCullingConfig
        Behaviors.same

      case Nop =>
        Behaviors.same
