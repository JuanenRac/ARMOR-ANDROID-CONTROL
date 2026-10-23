// ARMOR-ANDROID-CONTROL - push messages that carry only data: the server says which event happened and the phone decides what to show, in the language of the phone, once.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent

/**
 * The data of a push message, as the server sends it: `event_id` (the number the history gives the event, which is what keeps one event from being announced twice whether it arrives by push or
 * by reading), `type` (`alarm`, `alert`, `node`, `camera`, `device`), the subject (`node_id`, `camera_id`, `device_id` or `source`), `to` (the new state; `raised` for an alarm), `severity`,
 * `code`, `source_type`, `targets` for an alert, `at`, and `armed` (whether the system is armed, which decides whether a node going quiet matters). Anything else in the data is ignored, and a
 * message without a usable `event_id` is not an event of ours.
 */
object PushEvents {
    fun parse(data: Map<String, String>): ArmorEvent? {
        val id = data["event_id"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val type = data["type"].orEmpty()
        if (type !in setOf("alert", "node", "camera", "mode", "device", "alarm")) return null
        val subject = when (type) {
            "node", "alert" -> data["node_id"]
            "camera" -> data["camera_id"]
            "device" -> data["device_id"]
            "alarm" -> data["source"]
            else -> ""
        }.orEmpty()
        if (type != "mode" && subject.isBlank()) return null
        fun text(key: String): String? = data[key]?.takeIf { it.isNotBlank() }
        return ArmorEvent(
            id = id, at = data["at"].orEmpty(), type = type, subject = subject, from = text("from"), to = text("to"), targets = data["targets"]?.toIntOrNull(), mode = text("mode"),
            severity = text("severity"), code = text("code"), sourceType = text("source_type"), field = text("field"),
        )
    }

    /** Whether the system is armed according to the message; absent means "not said", which counts as not armed (a node going quiet is then not announced from the push alone). */
    fun armed(data: Map<String, String>): Boolean = data["armed"] == "true"

    /**
     * The notice for a push message, or nothing: the event of a duress PIN, an event that does not matter and one already announced (by an earlier push or by the watch) give none.
     * [announced] is told the id when the notice is given, so the next push or poll of the same event gives none.
     */
    fun notice(data: Map<String, String>, origin: String, announced: AnnouncedIds): AlarmNotice? {
        val event = parse(data) ?: return null
        val notice = AlarmPolicy.notices(listOf(event), armed(data)).firstOrNull() ?: return null
        return if (announced.firstTime(origin, notice.id)) notice else null
    }

    /** A message the system already showed by itself (it had a notification part): only the id is remembered, so the watch does not say it again. */
    fun noteShown(data: Map<String, String>, origin: String, announced: AnnouncedIds) {
        parse(data)?.let { announced.firstTime(origin, it.id) }
    }
}

/**
 * The ids of the events already announced, per server, the newest [capacity] of them: a watermark alone cannot tell "a push announced 120 before the watch saw 118 and 119" from "all up to 120 were
 * announced". Kept in the preferences so that it survives the process.
 */
class AnnouncedIds(private val load: (String) -> String?, private val save: (String, String) -> Unit, private val capacity: Int = 200) {
    private fun key(origin: String) = "announced|$origin"

    /** True the first time an id is seen for this server (and remembers it); false after. */
    @Synchronized
    fun firstTime(origin: String, id: Long): Boolean {
        val ids = load(key(origin))?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (id in ids) return false
        save(key(origin), (ids + id).takeLast(capacity).joinToString(","))
        return true
    }

    @Synchronized fun seen(origin: String, id: Long): Boolean = id in (load(key(origin))?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty())
}
