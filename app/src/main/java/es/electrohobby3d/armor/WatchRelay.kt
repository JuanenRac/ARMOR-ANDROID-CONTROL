// ARMOR-ANDROID-CONTROL - the alarms on a Wear OS watch: what the phone says to the watch of ARMOR-WATCH and what it does with the confirmation that comes back. Off by default.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The contract is the one of ARMOR-WATCH (docs/watch_alert_contract.json): four messages (the switch of the relay, an alert, the clear of an alert and the confirmation), a version, limits of size and
// an alert that lives at most ten minutes. The phone sends only the title and the text of a notice the person would see (never an alarm that the notices leave out, such as the duress PIN) and only
// while the person turned the option on. The confirmation from the watch is a request, not an order: the phone acts only on an alert it sent a moment ago and acknowledges it with its own session.
package es.electrohobby3d.armor

import android.content.Context
import org.json.JSONObject

/** Whatever carries a message to the watch: the Data Layer on a phone, a list in a test. */
fun interface WatchTransport {
    fun send(path: String, payload: String)
}

/** The alerts the phone sent and that the watch may confirm: alert id to the alarm it stands for, for a while, and never more than [CAPACITY]. */
class SentAlerts(private val now: () -> Long) {
    private class Entry(val alarmId: String, val expiresAt: Long)

    private val entries = LinkedHashMap<String, Entry>()

    @Synchronized
    fun remember(alertId: String, alarmId: String, ttlSeconds: Int) {
        entries.remove(alertId)
        entries[alertId] = Entry(alarmId, now() + ttlSeconds)
        while (entries.size > CAPACITY) entries.remove(entries.keys.first())
    }

    /** The alarm of [alertId] when the phone sent it and it has not expired; one confirmation only (the entry is used up). */
    @Synchronized
    fun take(alertId: String): String? {
        val entry = entries.remove(alertId) ?: return null
        return if (entry.expiresAt > now()) entry.alarmId else null
    }

    companion object {
        const val CAPACITY = 32
    }
}

object WatchRelay {
    const val PROTOCOL_VERSION = 1
    const val PATH_TO_WATCH = "/armor/alert"
    const val PATH_TO_PHONE = "/armor/alert/ack"
    const val TITLE_MAX = 60
    const val TEXT_MAX = 160
    const val CRITICAL_TTL_S = 600
    const val OTHER_TTL_S = 120
    private const val PREFERENCES = "armor-watch"
    private const val ENABLED = "relay"
    private const val ID_MAX = 64

    /** The alerts that can still be confirmed from the watch. */
    val sent = SentAlerts { System.currentTimeMillis() / 1000 }

    @Volatile
    var transport: WatchTransport? = null

    fun enabled(context: Context): Boolean = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    /** Switches the option; the watch is told at once, and what it shows is forgotten when the option goes off. */
    fun setEnabled(context: Context, on: Boolean) {
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, on).apply()
        transportFor(context).send(PATH_TO_WATCH, relayMessage(on))
    }

    private fun transportFor(context: Context): WatchTransport = transport ?: DataLayerTransport.of(context)

    /** A notice of the phone, as an alert for the watch (a no-op while the option is off, and for a notice the person chose not to see). */
    fun onNotice(context: Context, notice: AlarmNotice) {
        if (!enabled(context)) return
        if (NoticePreferences.load(context).decide(notice) == NoticeDecision.HIDE) return
        val message = alertMessage(notice) ?: return
        val link = transportFor(context)
        link.send(PATH_TO_WATCH, relayMessage(true))   // a watch that started again is off until the phone says otherwise
        notice.alarmId?.let { sent.remember(alertId(notice), it, ttlOf(notice)) }
        link.send(PATH_TO_WATCH, message)
    }

    fun severityOf(severity: String): String = when (severity) {
        "critical" -> "critical"
        "high" -> "warning"
        else -> "info"
    }

    private fun ttlOf(notice: AlarmNotice): Int = if (notice.severity == "critical") CRITICAL_TTL_S else OTHER_TTL_S

    fun validId(id: String): Boolean = id.isNotEmpty() && id.length <= ID_MAX && id.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' }

    /** The id of the alert: the alarm the notice tells about, or a number of the notice. */
    fun alertId(notice: AlarmNotice): String = notice.alarmId?.takeIf { validId(it) } ?: "n${notice.id.coerceAtLeast(0)}"

    fun relayMessage(on: Boolean): String = JSONObject().put("type", "relay").put("protocolVersion", PROTOCOL_VERSION).put("enabled", on).toString()

    fun clearMessage(alertId: String): String = JSONObject().put("type", "alert_clear").put("protocolVersion", PROTOCOL_VERSION).put("alertId", alertId).toString()

    /** The alert message of a notice, or null when it has nothing to say (the watch refuses an empty title or text). */
    fun alertMessage(notice: AlarmNotice): String? {
        val title = notice.title.trim().take(TITLE_MAX)
        val text = notice.text.trim().take(TEXT_MAX)
        if (title.isEmpty() || text.isEmpty()) return null
        return JSONObject().put("type", "alert").put("protocolVersion", PROTOCOL_VERSION).put("alertId", alertId(notice)).put("severity", severityOf(notice.severity))
            .put("title", title).put("text", text).put("needsAck", notice.alarmId != null && validId(notice.alarmId)).put("ttlSeconds", ttlOf(notice)).toString()
    }

    /** The alert id of a confirmation from the watch, or null when the text is not one (strict: the type, the version and nothing else that is not expected). */
    fun parseAck(text: String): String? {
        if (text.length > 512) return null
        val obj = try { JSONObject(text) } catch (_: Exception) { return null }
        if (obj.opt("type") != "alert_ack" || obj.opt("protocolVersion") != PROTOCOL_VERSION) return null
        val id = obj.opt("alertId") as? String ?: return null
        return id.takeIf { validId(it) }
    }

    /** What the phone does with a confirmation that came from the watch: the alarm to acknowledge, once, or null (not enabled, an alert the phone did not send, one that expired or one already used). */
    fun alarmToAcknowledge(enabled: Boolean, text: String): Pair<String, String>? {
        if (!enabled) return null
        val alertId = parseAck(text) ?: return null
        val alarm = sent.take(alertId) ?: return null
        return alertId to alarm
    }
}
