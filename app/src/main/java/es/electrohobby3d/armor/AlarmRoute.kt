// ARMOR-ANDROID-CONTROL - where an alarm takes the person: the screen that handles what raised it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Alarm

/** The screen of the bottom bar ([screen]: "Radar", "Alarms", "Devices", "Cameras" or "More") and, for "More", the tab to open. */
data class AlarmTarget(val screen: String, val more: MoreTab? = null)

object AlarmRoute {
    /**
     * Where an alarm is handled, as Studio does it: what a camera raised opens the cameras, a device's the devices, solar equipment's the inverters or the batteries, an electrical node's
     * the electrical screen, the network's the network, an alarm panel's the panels, and what a field node raised the radar. Anything else stays on the alarms.
     */
    fun of(alarm: Alarm): AlarmTarget = when (alarm.sourceType) {
        "camera" -> AlarmTarget("Cameras")
        "device" -> AlarmTarget("Devices")
        "solar" -> AlarmTarget("More", if (Regex("battery|pylon", RegexOption.IGNORE_CASE).containsMatchIn(alarm.code)) MoreTab.Batteries else MoreTab.Solar)
        "electrical" -> AlarmTarget("More", MoreTab.Electrical)
        "network" -> AlarmTarget("More", MoreTab.Network)
        "alarm" -> AlarmTarget("More", MoreTab.AlarmPanels)
        "node" -> AlarmTarget("Radar")
        else -> AlarmTarget("Alarms")
    }
}
