// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.edge.actors.sinks

import arktrio.center.services.ClockBase
import arktrio.common.data.Timestamp
import arktrio.edge.actors.sinks.Clock.*
import arktrio.edge.endpoints.EdgeConfigGet
import arktrio.edge.test.ActorTestBase

class ClockSuite extends ActorTestBase:
  test(Clock.getClass.getSimpleName):
    val config = EdgeConfigGet.outExample
    val clock = testKit.spawn(Clock(config.static))
    val clockReader = testKit.createTestProbe[ClockBase]()

    clock ! Catch(ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6))
    clock ! Get(clockReader.ref)
    clockReader.receiveMessage() shouldEqual ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)

    clock ! Catch(ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66))
    clock ! Get(clockReader.ref)
    clockReader.receiveMessage() shouldEqual ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66)
