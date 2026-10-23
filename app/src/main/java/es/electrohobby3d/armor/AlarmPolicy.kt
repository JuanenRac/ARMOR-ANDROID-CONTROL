// ARMOR-ANDROID-CONTROL - which events wake the operator, and how they read.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.DeviceText

/** `severity` is "critical", "high" or "warning": it picks the channel (the sound) the notification goes to. */
data class AlarmNotice(val id: Long, val title: String, val text: String, val severity: String = "high")

object AlarmPolicy {
    /** Alarms a phone never announces, shows or counts (the server tells the people who must know in silence). */
    fun isSilent(code: String?): Boolean = code == "duress_disarm"

    /**
     * The events worth a notification, oldest first: a node reaching HIGH, a camera that stops
     * answering, and (only while the system is armed) a node that goes offline or silent, because
     * a dead sensor is how a perimeter is defeated. An alarm raised by a device (smoke, gas, flood, panic; a door, window or
     * motion sensor while armed) always wakes the operator, and so does one raised by solar equipment (an inverter fault, a battery that is low or
     * protecting itself, equipment that went silent: the server raises each once until it clears) or by an electrical node (a meter's alarm, the mains out of range, the grid lost, a node that went silent) or by the panel of an alarm node (sounding, a zone in tamper, locked out, gone quiet) or by a network node (the internet or the local network down, a device that was never seen, two machines for one address, a port that opened). Alarms of nodes and cameras are not announced a second
     * time: their own events above already were. Everything else stays in the history.
     */
    fun notices(events: List<ArmorEvent>, armed: Boolean): List<AlarmNotice> = events.sortedBy { it.id }.mapNotNull { event ->
        when {
            isSilent(event.code) -> null   // the alarm of a duress PIN is never announced on a phone: whoever forced the person to disarm may be looking at it
            event.type == "alert" && event.to == "high" -> AlarmNotice(event.id, tr("ALERTA ALTA"), trf("%s: %s objetivos", event.subject, event.targets ?: 0))
            event.type == "camera" && event.to == "offline" -> AlarmNotice(event.id, tr("Cámara sin respuesta"), event.subject)
            event.type == "node" && armed && event.to == "offline" -> AlarmNotice(event.id, tr("Nodo fuera de línea"), event.subject)
            event.type == "node" && armed && event.to == "stale" -> AlarmNotice(event.id, tr("Nodo en silencio"), event.subject)
            event.type == "alarm" && event.to == "raised" && (event.sourceType == "device" || event.sourceType == "solar" || event.sourceType == "electrical" || event.sourceType == "network" || event.sourceType == "alarm") ->
                AlarmNotice(event.id, trf("ALARMA %s", DeviceText.severityLabel(event.severity ?: "warning")), "${DeviceText.alarmText(event.code.orEmpty())} · ${event.subject}", event.severity ?: "warning")
            else -> null
        }
    }

    private fun level(value: String?) = when (value) { "normal" -> "normal"; "review" -> tr("revisar"); "high" -> tr("ALTA"); else -> value.orEmpty() }
    private fun status(value: String?) = when (value) { "online" -> tr("en línea"); "offline" -> tr("fuera de línea"); "stale" -> tr("en silencio"); "unknown" -> tr("desconocido"); else -> value.orEmpty() }

    private fun alarmState(value: String?) = when (value) { "raised" -> tr("generada"); "acknowledged" -> tr("confirmada"); "cleared" -> tr("cerrada"); else -> value.orEmpty() }
    private fun fieldLabel(field: String?) = when (field) {
        "triggered" -> tr("activado"); "open" -> tr("abierto"); "on" -> tr("encendido"); "locked" -> tr("cerrada"); "tamper" -> tr("manipulación"); "online" -> tr("conexión"); else -> field.orEmpty()
    }
    private fun value(value: String?) = when (value) { "true" -> tr("sí"); "false" -> tr("no"); else -> value.orEmpty() }

    /** One line for the history list. */
    fun describe(event: ArmorEvent): String = when (event.type) {
        "mode" -> if (event.mode == "armed") tr("Sistema ARMADO") else tr("Sistema DESARMADO")
        "alert" -> trf("%s: %s → %s (%s objetivos)", event.subject, level(event.from), level(event.to), event.targets ?: 0)
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
class AlarmTracker(private val load: (String) -> Long, private val save: (String, Long) -> Unit, private val now: () -> Long = System::currentTimeMillis, private val announced: AnnouncedIds? = null) {
    /** The newest event handled for this server, or -1 when it was never looked at. */
    fun last(origin: String): Long = load(origin)

    /**
     * The notices for what is new. The memory is per server (a number is only meaningful for the server that gave it), an event older than [MAX_AGE_MS] is history and not news (the app
     * opened after days, or a service that was asleep), and a server whose numbering started again (a new database, another machine at the same address) is read from its start.
     */
    @Synchronized
    fun claim(origin: String, events: List<ArmorEvent>, armed: Boolean): List<AlarmNotice> {
        if (events.isEmpty()) return emptyList()
        val newest = events.maxOf { it.id }
        val last = load(origin)
        if (last < 0) { save(origin, newest); return emptyList() }
        if (newest < last) { save(origin, newest); return once(origin, AlarmPolicy.notices(recent(events), armed)) }
        val fresh = events.filter { it.id > last }
        if (newest > last) save(origin, newest)
        return once(origin, AlarmPolicy.notices(recent(fresh), armed))
    }

    /** What a push message already announced is not announced again by the watch, and the other way round. */
    private fun once(origin: String, notices: List<AlarmNotice>): List<AlarmNotice> = if (announced == null) notices else notices.filter { announced.firstTime(origin, it.id) }

    private fun recent(events: List<ArmorEvent>): List<ArmorEvent> = events.filter { event ->
        val at = runCatching { java.time.Instant.parse(event.at).toEpochMilli() }.getOrNull()
        at == null || now() - at <= MAX_AGE_MS
    }

    companion object {
        /** Older than this an event is shown in the history but does not wake anybody. */
        const val MAX_AGE_MS = 15 * 60_000L
    }
}

/**
 * What came after the last event handled, with no gap: [newest] is the newest events the screen already has (newest first); when the window does not reach back to [last] (more than a
 * window of events came between two looks) [after] is asked, page by page, for the ones in between (oldest first). A server that was reset (its newest is below [last]) is read from its newest.
 */
fun freshEvents(last: Long, newest: List<ArmorEvent>, after: (Long) -> List<ArmorEvent>, maxPages: Int = 5): List<ArmorEvent> {
    if (last < 0 || newest.isEmpty()) return newest
    val top = newest.maxOf { it.id }
    if (top < last) return newest
    val bottom = newest.minOf { it.id }
    if (bottom <= last + 1) return newest
    val gap = ArrayList<ArmorEvent>()
    var cursor = last
    for (round in 1..maxPages) {
        val page = after(cursor)
        if (page.isEmpty()) break   // nothing more: asking again for the same page would give the same nothing
        gap += page
        cursor = page.maxOf { it.id }
        if (cursor >= bottom - 1) return (gap + newest).distinctBy { it.id }
    }
    return (gap + newest).distinctBy { it.id }
}

/** The events the screen shows after a poll: the newest window from the server plus the older ones the person asked to see ("Ver anteriores") that are not in it, so a poll no longer erases them. */
fun mergeEvents(fresh: List<ArmorEvent>, previous: List<ArmorEvent>, keep: Int = 500): List<ArmorEvent> {
    if (fresh.isEmpty()) return previous
    val oldestFresh = fresh.minOf { it.id }
    return (fresh + previous.filter { it.id < oldestFresh }).distinctBy { it.id }.sortedByDescending { it.id }.take(keep)
}
