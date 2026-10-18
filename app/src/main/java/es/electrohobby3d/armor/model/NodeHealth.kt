// ARMOR-ANDROID-CONTROL - the health of the nodes as the server reports it: the same report from every radar, solar, electrical, alarm and wall-panel node, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.tr
import es.electrohobby3d.armor.trf
import org.json.JSONArray
import org.json.JSONObject

/** One warning the server raised about a node: "node_low_memory", "node_frequent_resets", "node_link_flapping" or "node_weak_signal". */
data class NodeHealthWarning(val code: String)

/** What a node says about itself, and what the server makes of it. `rssiDbm` is null unless the node is on a Wi-Fi link that is up. */
data class NodeHealthView(
    val nodeId: String,
    /** "radar", "solar", "electrical", "alarm" or "hmi". */ val family: String,
    val firmware: String, val board: String?,
    /** "ok", "warning" or "stale" (no report for a few minutes). */ val status: String,
    val receivedAt: String,
    val uptimeSeconds: Long, val resetReason: String,
    val heapFree: Long, val heapMin: Long,
    /** "wifi", "ethernet" or "none". */ val link: String, val linkUp: Boolean, val rssiDbm: Int?,
    val brokerConnections: Long, val lowStackTasks: Int, val lastError: String?,
    val restarts24h: Int, val warnings: List<NodeHealthWarning>,
) {
    val needsLook get() = status == "warning"
    val silent get() = status == "stale"
}

data class NodeHealthTotals(val nodes: Int = 0, val stale: Int = 0, val withWarnings: Int = 0)
/** A node the server knows but that has sent no report yet (an older firmware, or the broker does not let it write one). */
data class NodeWaiting(val nodeId: String, val family: String)
data class NodeHealthOverview(val nodes: List<NodeHealthView> = emptyList(), val waiting: List<NodeWaiting> = emptyList(), val totals: NodeHealthTotals = NodeHealthTotals()) {
    val isEmpty get() = nodes.isEmpty()
}

object NodeHealthParser {
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun overview(root: JSONObject): NodeHealthOverview = NodeHealthOverview(
        nodes = root.optJSONArray("nodes").objects().mapNotNull(::node),
        waiting = root.optJSONArray("waiting").objects().mapNotNull { item -> item.optStr("node_id").takeIf { it.isNotBlank() }?.let { NodeWaiting(it, item.optStr("family")) } },
        totals = root.optJSONObject("totals")?.let { NodeHealthTotals(it.optInt("nodes"), it.optInt("stale"), it.optInt("with_warnings")) } ?: NodeHealthTotals(),
    )

    private fun node(json: JSONObject): NodeHealthView? {
        val id = json.optStr("node_id")
        val report = json.optJSONObject("report") ?: return null
        if (id.isBlank()) return null
        return NodeHealthView(
            nodeId = id, family = json.optStr("family"), firmware = json.optStr("firmware"), board = json.optStr("board").takeIf { it.isNotBlank() },
            status = json.optStr("status", "ok"), receivedAt = json.optStr("received_at"),
            uptimeSeconds = report.optLong("uptime_s"), resetReason = report.optStr("reset_reason", "unknown"),
            heapFree = report.optLong("heap_free"), heapMin = report.optLong("heap_min"),
            link = report.optStr("link", "none"), linkUp = report.optBoolean("link_up"), rssiDbm = if (report.has("rssi_dbm")) report.optInt("rssi_dbm") else null,
            brokerConnections = report.optLong("broker_reconnects"), lowStackTasks = report.optInt("low_stack_tasks"), lastError = report.optStr("last_error").takeIf { it.isNotBlank() },
            restarts24h = json.optInt("restarts_24h"), warnings = json.optJSONArray("warnings").objects().map { NodeHealthWarning(it.optStr("code")) },
        )
    }
}

/** The wording of the health of the nodes, in plain Spanish. */
object NodeHealthText {
    private val abnormalResets = setOf("panic", "int_wdt", "task_wdt", "other_wdt", "brownout", "power_glitch", "cpu_lockup")

    fun family(family: String): String = when (family) {
        "radar" -> tr("Radar"); "solar" -> tr("Pasarela solar"); "electrical" -> tr("Eléctrico"); "alarm" -> tr("Central de alarma"); "hmi" -> tr("Panel de pared"); else -> family
    }

    fun status(status: String): String = when (status) { "ok" -> tr("sano"); "warning" -> tr("conviene mirarlo"); else -> tr("sin informe") }

    fun warning(code: String): String = when (code) {
        "node_low_memory" -> tr("Anda corto de memoria")
        "node_frequent_resets" -> tr("Se reinicia demasiado")
        "node_link_flapping" -> tr("Pierde el broker una y otra vez")
        "node_weak_signal" -> tr("Señal Wi-Fi débil")
        else -> code
    }

    /** Why the node last started, in words. */
    fun resetReason(reason: String): String = when (reason) {
        "power_on" -> tr("alimentación conectada"); "external" -> tr("pin de reinicio"); "software" -> tr("reinicio pedido (una actualización, por ejemplo)")
        "panic" -> tr("fallo del programa (panic)"); "int_wdt" -> tr("perro guardián (interrupciones)"); "task_wdt" -> tr("perro guardián (una tarea se paró)"); "other_wdt" -> tr("perro guardián")
        "deep_sleep" -> tr("despertó del sueño profundo"); "brownout" -> tr("caída de tensión (brown-out)"); "sdio" -> tr("reinicio por SDIO"); "usb" -> tr("reinicio por USB")
        "jtag" -> tr("reinicio por el depurador"); "efuse" -> tr("reinicio por un error de eFuse"); "power_glitch" -> tr("interferencia en la alimentación"); "cpu_lockup" -> tr("procesador bloqueado")
        else -> tr("desconocido")
    }

    /** A restart nobody asked for: a crash, a watchdog, a sag of the supply. */
    fun isUnexpected(reason: String): Boolean = reason in abnormalResets

    /** The link in words: "Wi-Fi · activo · -61 dBm (buena)", "Cable Ethernet · caído". */
    fun link(view: NodeHealthView): String {
        val kind = when (view.link) { "wifi" -> tr("Wi-Fi"); "ethernet" -> tr("Cable Ethernet"); else -> tr("sin enlace") }
        val state = if (view.linkUp) tr("activo") else tr("caído")
        val reading = view.rssiDbm?.let { " · " + trf("%s dBm (%s)", it, signal(it)) } ?: ""
        return "$kind · $state$reading"
    }

    /** The usual reading of a Wi-Fi signal: -60 dBm or better is good, down to -70 fair, down to -80 weak, below that very weak. */
    fun signal(dbm: Int): String = when { dbm >= -60 -> tr("buena"); dbm >= -70 -> tr("aceptable"); dbm >= -80 -> tr("débil"); else -> tr("muy débil") }

    /** 142000 is "139 KB", a megabyte or more "1,2 MB". */
    fun bytes(value: Long): String = when {
        value < 0 -> "–"
        value >= 1_048_576 -> String.format(java.util.Locale.ROOT, "%.1f MB", value / 1_048_576.0)
        value >= 1024 -> "${Math.round(value / 1024.0)} KB"
        else -> "$value B"
    }

    /** "3 d 4 h", "5 h 12 min" or "7 min". */
    fun uptime(seconds: Long): String {
        val days = seconds / 86_400; val hours = (seconds % 86_400) / 3_600; val minutes = (seconds % 3_600) / 60
        return when {
            seconds < 0 -> "–"
            days > 0 -> trf("%s d %s h", days, hours)
            hours > 0 -> trf("%s h %s min", hours, minutes)
            else -> trf("%s min", minutes)
        }
    }

    /** The line the Status screen shows under the health tile. */
    fun summary(overview: NodeHealthOverview): String = when {
        overview.totals.nodes == 0 -> tr("Ningún nodo ha enviado aún su informe de salud")
        overview.totals.withWarnings > 0 -> if (overview.totals.withWarnings == 1) tr("Un nodo con aviso de salud") else trf("%s nodos con aviso de salud", overview.totals.withWarnings)
        overview.totals.stale > 0 -> trf("%s nodos sin informe reciente", overview.totals.stale)
        else -> tr("Todos los nodos sanos")
    }
}
