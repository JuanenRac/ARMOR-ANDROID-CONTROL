// ARMOR-ANDROID-CONTROL - every service of the system, running or not: the programs of the machine (read from systemd) and the field nodes (read from what
// they last reported). Mirrors ARMOR-STUDIO's Services menu. Read only.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

/** "running", "stopped", "failed", "starting", "not_installed", "online", "offline" or "unknown". */
data class ServiceInfo(
    val id: String, val name: String, val family: String, val description: String, val kind: String, val state: String, val subState: String?, val unit: String?,
    val enabled: Boolean?, val pid: Long?, val sinceMs: Long?, val memoryBytes: Long?, val restarts: Long?, val port: Int?,
)
data class ServicesOverview(val timeMs: Long = 0, val systemd: Boolean = false, val services: List<ServiceInfo> = emptyList())

object ServicesParser {
    private fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) optLong(key) else null
    private fun JSONObject.boolOrNull(key: String): Boolean? = if (has(key) && !isNull(key)) optBoolean(key) else null
    private fun JSONObject.textOrNull(key: String): String? = if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    fun overview(root: JSONObject): ServicesOverview {
        val array = root.optJSONArray("services")
        val services = buildList { if (array != null) for (i in 0 until array.length()) array.optJSONObject(i)?.let(::service)?.let(::add) }
        return ServicesOverview(root.optLong("time_ms"), root.optBoolean("systemd"), services)
    }

    private fun service(json: JSONObject) = ServiceInfo(
        id = json.optString("id"), name = json.optString("name"), family = json.optString("family"), description = json.optString("description"),
        kind = json.optString("kind"), state = json.optString("state", "unknown"), subState = json.textOrNull("sub_state"), unit = json.textOrNull("unit"),
        enabled = json.boolOrNull("enabled"), pid = json.longOrNull("pid"), sinceMs = json.longOrNull("since_ms"), memoryBytes = json.longOrNull("memory_bytes"),
        restarts = json.longOrNull("restarts"), port = if (json.has("port") && !json.isNull("port")) json.optInt("port") else null,
    )
}

/** The wording of the Services screen, in plain Spanish (matches ARMOR-STUDIO's). */
object ServicesText {
    val familyOrder = listOf("Core", "Network", "AI_and_voice", "Field_nodes")
    fun family(id: String): String = when (id) {
        "Core" -> "Núcleo"; "Network" -> "Red"; "AI_and_voice" -> "IA y voz"; "Field_nodes" -> "Nodos de campo"; else -> id
    }
    fun state(state: String): String = when (state) {
        "running" -> "En marcha"; "stopped" -> "Parado"; "failed" -> "Fallido"; "starting" -> "Arrancando"; "not_installed" -> "No instalado"
        "online" -> "En línea"; "offline" -> "Sin responder"; else -> "Desconocido"
    }
    fun memory(bytes: Long?): String = when {
        bytes == null -> "—"
        bytes >= 1_073_741_824 -> String.format(java.util.Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.0f MB", bytes / 1_048_576.0)
        else -> "${bytes / 1024} KB"
    }
    /** "Hace 3 min" / "desde hace 2 h" style, for when it started or last reported. */
    fun since(sinceMs: Long?, nowMs: Long): String {
        if (sinceMs == null || sinceMs <= 0) return "—"
        val seconds = ((nowMs - sinceMs) / 1000).coerceAtLeast(0)
        return when {
            seconds < 90 -> "hace ${seconds} s"
            seconds < 5400 -> "hace ${seconds / 60} min"
            seconds < 36 * 3600 -> "hace ${seconds / 3600} h"
            else -> "hace ${seconds / 86400} d"
        }
    }
    fun summary(overview: ServicesOverview): String {
        val failed = overview.services.count { it.state == "failed" || it.state == "offline" }
        val running = overview.services.count { it.state == "running" || it.state == "online" }
        return if (failed > 0) "$failed con problemas de ${overview.services.size}" else "$running de ${overview.services.size} en marcha"
    }
}
