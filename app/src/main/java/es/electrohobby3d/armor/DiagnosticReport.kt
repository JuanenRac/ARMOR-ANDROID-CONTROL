// ARMOR-ANDROID-CONTROL - the report a person can share with whoever helps them: what the app is, what the phone allows it, and the last answers the server gave, with no secret in it.
// Nothing here leaves the phone by itself: the person chooses the button and then the app to send it with. It holds no password, token, PIN, cookie, address with a sign-in, camera name or message text.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import java.util.ArrayDeque

/** The last answers of the server, kept in memory only (never written to disk): the shape of the route and the status, nothing of the request or the answer. */
object DiagnosticLog {
    data class Entry(val atMs: Long, val method: String, val route: String, val status: Int)
    private const val KEEP = 25
    private val entries = ArrayDeque<Entry>()

    /** A route reduced to its first four parts with no query: "/api/v1/cameras/gate/snapshot?ticket=x" is "/api/v1/cameras/…". */
    fun shape(path: String): String {
        val parts = path.substringBefore('?').substringBefore('#').split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "/"
        return "/" + parts.take(4).joinToString("/") + if (parts.size > 4) "/…" else ""
    }

    /** The route of the hidden panic gesture: it leaves no trace in the report that a person may share, or that somebody looking over the shoulder may read. */
    private const val QUIET_ROUTE = "/panic-gesture"

    @Synchronized fun record(method: String, path: String, status: Int, atMs: Long = System.currentTimeMillis()) {
        if (path.substringBefore('?').endsWith(QUIET_ROUTE)) return
        if (entries.size >= KEEP) entries.removeFirst()
        entries.addLast(Entry(atMs, method.take(8), shape(path), status))
    }

    @Synchronized fun recent(): List<Entry> = entries.toList()
    @Synchronized fun clear() = entries.clear()
}

/** What the report says, gathered by the screen (the phone's answers) and kept apart from the text so that the text can be tested. */
data class DiagnosticInfo(
    val appVersion: String, val appCode: Int, val androidRelease: String, val sdk: Int, val device: String, val language: String,
    val origin: String, val role: String, val notificationsAllowed: Boolean?, val batteryUnrestricted: Boolean?, val watcherOn: Boolean?,
    val recent: List<DiagnosticLog.Entry>, val nowMs: Long,
)

object DiagnosticReport {
    /** The address as "scheme://host" (no port, path, query or sign-in), or "not set". */
    fun originLabel(origin: String): String {
        val parsed = try { java.net.URI(origin.trim()) } catch (_: Exception) { return "not set" }
        val scheme = parsed.scheme?.lowercase()
        val host = parsed.host
        return if ((scheme == "http" || scheme == "https") && !host.isNullOrBlank()) "$scheme://$host" else "not set"
    }

    private fun yesNo(value: Boolean?) = when (value) { true -> "yes"; false -> "no"; null -> "unknown" }

    fun render(info: DiagnosticInfo): String = buildString {
        appendLine("A.R.M.O.R. diagnostic report")
        appendLine("App: ${info.appVersion} (code ${info.appCode})")
        appendLine("Android: ${info.androidRelease} (SDK ${info.sdk})")
        appendLine("Device: ${info.device}")
        appendLine("Language: ${info.language}")
        appendLine("Server: ${originLabel(info.origin)}")
        appendLine("Role: ${info.role.ifBlank { "not signed in" }}")
        appendLine("Notifications allowed: ${yesNo(info.notificationsAllowed)}")
        appendLine("Battery optimisation off for the app: ${yesNo(info.batteryUnrestricted)}")
        appendLine("Alarm watcher on: ${yesNo(info.watcherOn)}")
        appendLine("Last answers of the server (newest last):")
        if (info.recent.isEmpty()) appendLine("  none yet")
        for (entry in info.recent) {
            val ago = ((info.nowMs - entry.atMs).coerceAtLeast(0) / 1000)
            appendLine("  ${entry.method} ${entry.route} -> ${if (entry.status > 0) entry.status.toString() else "no answer"} (${ago}s ago)")
        }
        append("No password, token, PIN or message text is in this report.")
    }
}
