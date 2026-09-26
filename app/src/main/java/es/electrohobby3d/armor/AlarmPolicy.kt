// ARMOR-ANDROID-CONTROL - which events wake the operator, and how they read.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.DeviceText

data class AlarmNotice(val id: Long, val title: String, val text: String)

object AlarmPolicy {
    /**
     * The events worth a notification, oldest first: a node reaching HIGH, a camera that stops
     * answering, and (only while the system is armed) a node that goes offline or silent, because
     * a dead sensor is how a perimeter is defeated. An alarm raised by a device (smoke, gas, flood, panic; a door, window or
     * motion sensor while armed) always wakes the operator, and so does one raised by solar equipment (an inverter fault, a battery that is low or
     * protecting itself, equipment that went silent: the server raises each once until it clears) or by an electrical node (a meter's alarm, the mains out of range, the grid lost, a node that went silent). Alarms of nodes and cameras are not announced a second
     * time: their own events above already were. Everything else stays in the history.
     */
    fun notices(events: List<ArmorEvent>, armed: Boolean): List<AlarmNotice> = events.sortedBy { it.id }.mapNotNull { event ->
        when {
            event.type == "alert" && event.to == "high" -> AlarmNotice(event.id, "ALERTA ALTA", "${event.subject}: ${event.targets ?: 0} objetivos")
            event.type == "camera" && event.to == "offline" -> AlarmNotice(event.id, "Cámara sin respuesta", event.subject)
            event.type == "node" && armed && event.to == "offline" -> AlarmNotice(event.id, "Nodo fuera de línea", event.subject)
            event.type == "node" && armed && event.to == "stale" -> AlarmNotice(event.id, "Nodo en silencio", event.subject)
            event.type == "alarm" && event.to == "raised" && (event.sourceType == "device" || event.sourceType == "solar" || event.sourceType == "electrical") ->
                AlarmNotice(event.id, "ALARMA ${DeviceText.severityLabel(event.severity ?: "warning")}", "${DeviceText.alarmText(event.code.orEmpty())} · ${event.subject}")
            else -> null
        }
    }

    private fun level(value: String?) = when (value) { "normal" -> "normal"; "review" -> "revisar"; "high" -> "ALTA"; else -> value.orEmpty() }
    private fun status(value: String?) = when (value) { "online" -> "en línea"; "offline" -> "fuera de línea"; "stale" -> "en silencio"; "unknown" -> "desconocido"; else -> value.orEmpty() }

    private fun alarmState(value: String?) = when (value) { "raised" -> "generada"; "acknowledged" -> "confirmada"; "cleared" -> "cerrada"; else -> value.orEmpty() }
    private fun fieldLabel(field: String?) = when (field) {
        "triggered" -> "activado"; "open" -> "abierto"; "on" -> "encendido"; "locked" -> "cerrada"; "tamper" -> "manipulación"; "online" -> "conexión"; else -> field.orEmpty()
    }
    private fun value(value: String?) = when (value) { "true" -> "sí"; "false" -> "no"; else -> value.orEmpty() }

    /** One line for the history list. */
    fun describe(event: ArmorEvent): String = when (event.type) {
        "mode" -> if (event.mode == "armed") "Sistema ARMADO" else "Sistema DESARMADO"
        "alert" -> "${event.subject}: ${level(event.from)} → ${level(event.to)} (${event.targets ?: 0} objetivos)"
        "camera" -> "${event.subject}: ${status(event.from)} → ${status(event.to)}"
        "alarm" -> "${DeviceText.alarmText(event.code.orEmpty())} · ${event.subject} · ${alarmState(event.to)}"
        "device" -> "${event.subject}: ${fieldLabel(event.field)} ${event.from?.let { value(it) + " → " }.orEmpty()}${value(event.to)}"
        else -> "${event.subject}: ${event.from?.let { status(it) + " → " }.orEmpty()}${status(event.to)}"
    }
}

/**
 * Remembers the newest event already handled so that neither the open app nor the background
 * service announces the same event twice. The first call only records a baseline: opening the
 * app for the first time must not replay the whole history as alarms.
 */
class AlarmTracker(private val load: () -> Long, private val save: (Long) -> Unit) {
    @Synchronized
    fun claim(events: List<ArmorEvent>, armed: Boolean): List<AlarmNotice> {
        if (events.isEmpty()) return emptyList()
        val newest = events.maxOf { it.id }
        val last = load()
        if (last < 0) { save(newest); return emptyList() }
        val fresh = events.filter { it.id > last }
        if (newest > last) save(newest)
        return AlarmPolicy.notices(fresh, armed)
    }
}
