// ARMOR-ANDROID-CONTROL - small helpers of the Alarms and Devices screens (the screens themselves are in HomeScreens.kt).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.SiteDevice

/** The words of the two buttons that command an actuator: a lock is locked by "on", a valve is opened by "on". */
internal fun commandLabels(kind: String): Pair<String, String> = when (kind) {
    "lock" -> "Cerrar" to "Abrir"; "valve" -> "Abrir" to "Cerrar"; "siren" -> "Sonar" to "Silenciar"; else -> "Encender" to "Apagar"
}

/** Where an alarm came from, in words: the device's own name when it is one. */
internal fun alarmSourceName(alarm: Alarm, devices: List<SiteDevice>, cameraNames: Map<String, String>, solarNames: Map<String, String> = emptyMap()): String = when (alarm.sourceType) {
    "device" -> devices.firstOrNull { it.id == alarm.sourceId }?.name ?: alarm.sourceId
    "camera" -> cameraNames[alarm.sourceId] ?: alarm.sourceId
    "solar" -> solarNames[alarm.sourceId] ?: alarm.sourceId.substringAfter('/')   // the source of a solar alarm is "node/device"
    "electrical" -> alarm.sourceId.replace("/", " · ")   // the source of an electrical alarm is "node/channel", or the node
    "network" -> alarm.sourceId.replace("/", " · ")   // the source of a network alarm is "node/device" (the device's MAC), or the node
    else -> alarm.sourceId
}
