// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.edge.actors.adapters

import arktrio.center.services.{ChartAgent, ClockBase}
import arktrio.common.data.*
import arktrio.edge.actors.adapters.EdgeAgentsPutAdapter.*
import arktrio.edge.actors.sinks.{Chart, Clock}
import arktrio.edge.configs.AxisConfig.Direction.{East, North, Up}
import arktrio.edge.configs.CoordinateConfig.LengthUnit.Meter
import arktrio.edge.configs.CoordinateConfig.SpeedUnit.MeterPerSecond
import arktrio.edge.configs.{AxisConfig, CoordinateConfig, QuaternionConfig}
import arktrio.edge.connectors.{ChartConnector, RegisterConnector}
import arktrio.edge.data.*
import arktrio.edge.endpoints.EdgeAgentsPut.{Request, Response}
import arktrio.edge.endpoints.{EdgeAgentsPutRequestAgent, EdgeConfigGet}
import arktrio.edge.test.ActorTestBase
import arktrio.edge.util.{EdgeKamon, ErrorStatus}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.collection.mutable

class EdgeAgentsPutAdapterSuite extends ActorTestBase:
  test(EdgeAgentsPutAdapter.getClass.getSimpleName):
    val config = EdgeConfigGet.outExample
    val chart = testKit.createTestProbe[Chart.UpdateFirstAgents]()
    val chartPublish = testKit.createTestProbe[ChartConnector.Publish]()
    val registerPublish = testKit.createTestProbe[RegisterConnector.Publish]()
    val clockReadQueue = mutable.Queue[ClockBase]()
    val clock = testKit.spawn[Clock.Get](Behaviors.receiveMessage:
      case Clock.Get(replyTo) =>
        replyTo ! clockReadQueue.dequeue()
        Behaviors.same)
    val adapter =
      testKit.spawn(
        EdgeAgentsPutAdapter(
          chart.ref,
          chartPublish.ref,
          registerPublish.ref,
          clock.ref,
          config.static,
          config.dynamic.coordinate,
          EdgeKamon("run", "edge")
        )
      )
    val endpoint = testKit.createTestProbe[Either[ErrorStatus, Response]]()

    adapter ! CoordinateConfig(
      AxisConfig(East, North, Up),
      Vector3(0, 0, 0),
      QuaternionConfig,
      Meter,
      MeterPerSecond
    )
    clockReadQueue += ClockBase(Timestamp(0, 0), Timestamp(0, 0), 1)
    adapter ! Put(
      Request(
        Some(VirtualTimestamp(1, 0)),
        Map(
          "a" -> EdgeAgentsPutRequestAgent(
            Some(transform(Vector3(1, 2, 3), Some(Vector3(1, 1, 1)))),
            None
          ),
          "b" -> EdgeAgentsPutRequestAgent(Some(transform(Vector3(1, 2, 3), None)), None)
        )
      ),
      MachineTimestamp(0, 0),
      endpoint.ref
    )
    chart.receiveMessage().agents shouldEqual Seq(
      ChartAgent("a", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(1, 1, 1))),
      ChartAgent("b", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0)))
    )
    chartPublish.receiveMessage().agents shouldEqual Seq(
      ChartAgent("a", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(1, 1, 1))),
      ChartAgent("b", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0)))
    )

    clockReadQueue += ClockBase(Timestamp(0, 0), Timestamp(0, 0), 1)
    adapter ! Put(
      Request(
        Some(VirtualTimestamp(1, 500_000_000)),
        Map(
          "a" -> EdgeAgentsPutRequestAgent(Some(transform(Vector3(4, 4, 4), None)), None),
          "b" -> EdgeAgentsPutRequestAgent(Some(transform(Vector3(1, 2, 3), None)), None)
        )
      ),
      MachineTimestamp(0, 0),
      endpoint.ref
    )
    chart.receiveMessage().agents shouldEqual Seq(
      ChartAgent(
        "a",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(4, 4, 4), Vector3Enu(6, 4, 2))
      ),
      ChartAgent(
        "b",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0))
      )
    )
    chartPublish.receiveMessage().agents shouldEqual Seq(
      ChartAgent(
        "a",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(4, 4, 4), Vector3Enu(6, 4, 2))
      ),
      ChartAgent(
        "b",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0))
      )
    )

  private def transform(localTranslation: Vector3, localTranslationSpeed: Option[Vector3]) =
    Transform(
      None,
      Vector3(1, 1, 1),
      Quaternion(1, 0, 0, 0),
      localTranslation,
      localTranslationSpeed,
      None
    )

  private def transformEnu(
      timestamp: Timestamp,
      localTranslation: Vector3Enu,
      localTranslationSpeed: Vector3Enu
  ) =
    TransformEnu(
      timestamp,
      None,
      Vector3Enu(1, 1, 1),
      QuaternionEnu(1, 0, 0, 0),
      localTranslation,
      localTranslationSpeed,
      Map()
    )
