// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.e2e

import arktrio.e2e.endpoints.*
import io.gatling.core.Predef.*
import io.gatling.http.Predef.*

class AtOnceSimulation extends Simulation:
  setUp(
    Seq(
      CenterClockSpeedPutScenario.builder,
      EdgeAgentsPostScenario.builder,
      EdgeConfigCoordinatePutScenario.builder,
      EdgeConfigCullingPutScenario.builder
    ).map(_.inject(atOnceUsers(1))).reduceRight(_ andThen _)
  ).protocols(http.baseUrl("http://localhost:2237"))
    .assertions(global.failedRequests.count.is(0))
