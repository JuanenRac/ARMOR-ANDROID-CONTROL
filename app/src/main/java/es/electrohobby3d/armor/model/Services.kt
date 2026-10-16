// ARMOR-ANDROID-CONTROL - every service of the system, running or not: the programs of the machine (read from systemd) and the field nodes (read from what
// they last reported). Mirrors ARMOR-STUDIO's Services menu. Read only.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.tr
import es.electrohobby3d.armor.trf
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
    private fun JSONObject.textOrNull(key: String): String? = if (has(key) && !isNull(key)) optStr(key).takeIf { it.isNotBlank() } else null

    fun overview(root: JSONObject): ServicesOverview {
        val array = root.optJSONArray("services")
        val services = buildList { if (array != null) for (i in 0 until array.length()) array.optJSONObject(i)?.let(::service)?.let(::add) }
        return ServicesOverview(root.optLong("time_ms"), root.optBoolean("systemd"), services)
    }

    private fun service(json: JSONObject) = ServiceInfo(
        id = json.optStr("id"), name = json.optStr("name"), family = json.optStr("family"), description = json.optStr("description"),
        kind = json.optStr("kind"), state = json.optStr("state", "unknown"), subState = json.textOrNull("sub_state"), unit = json.textOrNull("unit"),
        enabled = json.boolOrNull("enabled"), pid = json.longOrNull("pid"), sinceMs = json.longOrNull("since_ms"), memoryBytes = json.longOrNull("memory_bytes"),
        restarts = json.longOrNull("restarts"), port = if (json.has("port") && !json.isNull("port")) json.optInt("port") else null,
    )
}

/** The wording of the Services screen, in plain Spanish (matches ARMOR-STUDIO's). */
object ServicesText {
    val familyOrder = listOf("Core", "Network", "AI_and_voice", "Field_nodes")
    fun family(id: String): String = when (id) {
        "Core" -> tr("Núcleo"); "Network" -> tr("Red"); "AI_and_voice" -> tr("IA y voz"); "Field_nodes" -> tr("Nodos de campo"); else -> id
    }
    fun state(state: String): String = when (state) {
        "running" -> tr("En marcha"); "stopped" -> tr("Parado"); "failed" -> tr("Fallido"); "starting" -> tr("Arrancando"); "not_installed" -> tr("No instalado")
        "online" -> tr("En línea"); "offline" -> tr("Sin responder"); else -> tr("Desconocido")
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
            seconds < 90 -> trf("hace %s s", seconds)
            seconds < 5400 -> trf("hace %s min", seconds / 60)
            seconds < 36 * 3600 -> trf("hace %s h", seconds / 3600)
            else -> trf("hace %s d", seconds / 86400)
        }
    }
    fun summary(overview: ServicesOverview): String {
        val failed = overview.services.count { it.state == "failed" || it.state == "offline" }
        val running = overview.services.count { it.state == "running" || it.state == "online" }
        return if (failed > 0) trf("%s con problemas de %s", failed, overview.services.size) else trf("%s de %s en marcha", running, overview.services.size)
    }
}
