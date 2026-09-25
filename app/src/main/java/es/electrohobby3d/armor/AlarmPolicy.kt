// ARMOR-ANDROID-CONTROL - which events wake the operator, and how they read.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent

data class AlarmNotice(val id: Long, val title: String, val text: String)

object AlarmPolicy {
    /**
     * The events worth a notification, oldest first: a node reaching HIGH, a camera that stops
     * answering, and (only while the system is armed) a node that goes offline or silent, because
     * a dead sensor is how a perimeter is defeated. Everything else stays in the history.
     */
    fun notices(events: List<ArmorEvent>, armed: Boolean): List<AlarmNotice> = events.sortedBy { it.id }.mapNotNull { event ->
        when {
            event.type == "alert" && event.to == "high" -> AlarmNotice(event.id, "ALERTA ALTA", "${event.subject}: ${event.targets ?: 0} objetivos")
            event.type == "camera" && event.to == "offline" -> AlarmNotice(event.id, "Cámara sin respuesta", event.subject)
            event.type == "node" && armed && event.to == "offline" -> AlarmNotice(event.id, "Nodo fuera de línea", event.subject)
            event.type == "node" && armed && event.to == "stale" -> AlarmNotice(event.id, "Nodo en silencio", event.subject)
            else -> null
        }
    }

    private fun level(value: String?) = when (value) { "normal" -> "normal"; "review" -> "revisar"; "high" -> "ALTA"; else -> value.orEmpty() }
    private fun status(value: String?) = when (value) { "online" -> "en línea"; "offline" -> "fuera de línea"; "stale" -> "en silencio"; "unknown" -> "desconocido"; else -> value.orEmpty() }

    /** One line for the history list. */
    fun describe(event: ArmorEvent): String = when (event.type) {
        "mode" -> if (event.mode == "armed") "Sistema ARMADO" else "Sistema DESARMADO"
        "alert" -> "${event.subject}: ${level(event.from)} → ${level(event.to)} (${event.targets ?: 0} objetivos)"
        "camera" -> "${event.subject}: ${status(event.from)} → ${status(event.to)}"
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
