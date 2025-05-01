// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.edge.endpoints

import arktrio.common.data.TaggedTimestamp
import arktrio.common.data.TimestampExtensions.*
import arktrio.edge.actors.EdgeConfigurator
import arktrio.edge.configs.CullingConfig
import arktrio.edge.util.EndpointExtensions.serverLogicWithLog
import arktrio.edge.util.JsonDerivation.given
import arktrio.edge.util.{BadRequest, EdgeKamon, ErrorStatus}
import cats.data.Validated.{Invalid, Valid}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode.Accepted
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.{ExecutionContext, Future}

object EdgeConfigCullingPut:
  type Request = CullingConfig
  type Response = Unit
  val Request: CullingConfig.type = CullingConfig
  given JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator

  val inExample: Request = Request(edgeCulling = true, 9)

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "edge" / "config" / "culling")
      .in(jsonBody[Request].example(inExample))
      .out(statusCode(Accepted))
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest,
          ErrorStatus.internalServerError
        )
      )

  def route(configurator: ActorRef[EdgeConfigurator.Message], kamon: EdgeKamon)(using
      ExecutionContext
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogicWithLog: request =>
        val requestTime = TaggedTimestamp.machineNow()
        (request.validated("") match
          case Invalid(errors) =>
            Future.successful(Left(BadRequest(errors.toChain.toVector)))
          case Valid(request) =>
            configurator ! request
            Future.successful(Right[ErrorStatus, Response](()))
        ).andThen: _ =>
          requestNumCounter.increment()
          processMachineTimeHistogram.record(TaggedTimestamp.machineNow() - requestTime)
