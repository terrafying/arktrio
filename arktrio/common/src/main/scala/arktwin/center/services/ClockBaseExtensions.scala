// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktrio.center.services

import arktrio.common.data.*
import arktrio.common.data.DurationExtensions.*
import arktrio.common.data.TimestampExtensions.*

object ClockBaseExtensions:
  extension (a: ClockBase)
    def fromMachine(machineTimestamp: MachineTimestamp): VirtualTimestamp =
      a.baseTimestamp.tagVirtual +
        ((machineTimestamp - a.baseMachineTimestamp.tagMachine) * a.clockSpeed).untag.tagVirtual

    def now(): VirtualTimestamp =
      fromMachine(TaggedTimestamp.machineNow())

  extension (a: ClockBase.type)
    inline def apply(
        baseMachineTimestamp: MachineTimestamp,
        baseVirtualTimestamp: VirtualTimestamp,
        clockSpeed: Double
    ): ClockBase =
      ClockBase(baseMachineTimestamp.untag, baseVirtualTimestamp.untag, clockSpeed)
