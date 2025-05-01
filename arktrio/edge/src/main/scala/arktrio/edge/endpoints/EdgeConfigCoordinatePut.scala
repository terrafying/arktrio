// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.edge.endpoints

import arktrio.common.data.TaggedTimestamp
import arktrio.common.data.TimestampExtensions.*
import arktrio.edge.actors.EdgeConfigurator
import arktrio.edge.configs.AxisConfig.Direction
import arktrio.edge.configs.CoordinateConfig.{LengthUnit, SpeedUnit}
import arktrio.edge.configs.EulerAnglesConfig.{AngleUnit, RotationMode, RotationOrder}
import arktrio.edge.configs.{AxisConfig, CoordinateConfig, EulerAnglesConfig}
import arktrio.edge.data.Vector3
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

object EdgeConfigCoordinatePut:
  type Request = CoordinateConfig
  type Response = Unit
  val Request: CoordinateConfig.type = CoordinateConfig
  given JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator

  val inExample: Request = Request(
    AxisConfig(
      Direction.East,
      Direction.North,
      Direction.Up
    ),
    Vector3(0, 0, 0),
    EulerAnglesConfig(
      AngleUnit.Degree,
      RotationMode.Extrinsic,
      RotationOrder.XYZ
    ),
    LengthUnit.Meter,
    SpeedUnit.MeterPerSecond
  )

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "edge" / "config" / "coordinate")
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
