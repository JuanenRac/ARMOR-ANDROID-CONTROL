// ARMOR-ANDROID-CONTROL - small helpers of the Alarms and Devices screens (the screens themselves are in HomeScreens.kt).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.SiteDevice

/** The actuators that open and close something: a valve, a roller shutter, the motor of a gate or of a garage door. "on" opens them. */
internal val openingKinds = setOf("valve", "shutter", "gate_motor", "garage_motor")

/** The words of the two buttons that command an actuator: a lock is locked by "on", a valve, a shutter or a gate motor is opened by "on". */
internal fun commandLabels(kind: String): Pair<String, String> = when (kind) {
    "lock" -> tr("Cerrar") to tr("Abrir"); in openingKinds -> tr("Abrir") to tr("Cerrar"); "siren" -> tr("Sonar") to tr("Silenciar"); else -> tr("Encender") to tr("Apagar")
}

/** Where an alarm came from, in words: the device's own name when it is one. */
internal fun alarmSourceName(alarm: Alarm, devices: List<SiteDevice>, cameraNames: Map<String, String>, solarNames: Map<String, String> = emptyMap()): String = when (alarm.sourceType) {
    "device" -> devices.firstOrNull { it.id == alarm.sourceId }?.name ?: alarm.sourceId
    "camera" -> cameraNames[alarm.sourceId] ?: alarm.sourceId
    "solar" -> solarNames[alarm.sourceId] ?: alarm.sourceId.substringAfter('/')   // the source of a solar alarm is "node/device"
    "electrical" -> alarm.sourceId.replace("/", " · ")   // the source of an electrical alarm is "node/channel", or the node
    "network" -> alarm.sourceId.replace("/", " · ")   // the source of a network alarm is "node/device" (the device's MAC), or the node
    "alarm" -> alarm.sourceId.replace("/", " · ")   // the source of an alarm-node alarm is the node, or "node/zone" for a tamper
    else -> alarm.sourceId
}
