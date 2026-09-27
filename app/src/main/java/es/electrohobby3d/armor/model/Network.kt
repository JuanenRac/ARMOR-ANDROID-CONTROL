// ARMOR-ANDROID-CONTROL - the local network as the ARMOR-NETWORK nodes see it: what the server reports, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

/** What an operator has said about a device: a name, whether it is known. Null fields were never set. */
data class NetworkNote(val name: String?, val notes: String?, val trusted: Boolean, val kind: String?)

data class NetworkPort(val port: Int, val proto: String, val service: String?, val banner: String?)

data class NetworkDevice(
    val id: String, val ip: String, val mac: String?, val randomizedMac: Boolean, val vendor: String?, val hostname: String?, val kind: String, val os: String?, val online: Boolean,
    val firstSeenMs: Long, val lastSeenMs: Long, val latencyMs: Double?, val ports: List<NetworkPort>, val services: List<String>, val note: NetworkNote?,
) {
    /** What to call it: what the operator named it, else the name it gives itself, else its maker, else its address. */
    val name get() = note?.name?.takeIf { it.isNotBlank() } ?: hostname?.takeIf { it.isNotBlank() } ?: vendor?.takeIf { it.isNotBlank() } ?: ip
    val known get() = note?.trusted == true
    val shownKind get() = note?.kind ?: kind
}

data class NetworkProbe(val target: String, val kind: String, val ok: Boolean, val latencyMs: Double?)
data class NetworkInternet(
    /** "up", "degraded", "down" (the router answers, nothing beyond does), "lan_down" (the router does not answer) or "unknown". */
    val state: String, val sinceMs: Long?, val gatewayOk: Boolean?, val latencyMs: Double?, val lossPercent: Double?, val probes: List<NetworkProbe>, val outages24h: Int, val downtime24hS: Int,
)
data class NetworkNode(val nodeId: String, val stale: Boolean, val ip: String, val cidr: String, val gateway: String?, val rxBps: Long?, val txBps: Long?, val internet: NetworkInternet, val devices: List<NetworkDevice>)
data class NetworkEvent(val id: String, val nodeId: String, val kind: String, val atMs: Long, val deviceId: String?, val port: Int?, val outageS: Int?, val detail: String?)
data class NetworkOutage(val kind: String, val startedMs: Long, val durationS: Int)
data class NetworkTotals(val nodes: Int = 0, val stale: Int = 0, val devices: Int = 0, val online: Int = 0, val unknown: Int = 0, val internet: String? = null)

data class NetworkOverview(
    val nodes: List<NetworkNode> = emptyList(), val totals: NetworkTotals = NetworkTotals(), val events: List<NetworkEvent> = emptyList(), val outages: List<NetworkOutage> = emptyList(),
) { val isEmpty get() = nodes.isEmpty() }

object NetworkParser {
    private fun JSONObject.number(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
    private fun JSONObject.text(key: String): String? = if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun overview(root: JSONObject): NetworkOverview = NetworkOverview(
        nodes = root.optJSONArray("nodes").objects().mapNotNull(::node),
        totals = root.optJSONObject("totals")?.let(::totals) ?: NetworkTotals(),
        events = root.optJSONArray("events").objects().mapNotNull(::event),
        outages = root.optJSONArray("outages").objects().mapNotNull(::outage),
    )

    private fun totals(json: JSONObject) = NetworkTotals(json.optInt("nodes"), json.optInt("stale"), json.optInt("devices"), json.optInt("online"), json.optInt("unknown"), json.text("internet"))

    private fun node(json: JSONObject): NetworkNode? {
        val id = json.optString("node_id")
        val iface = json.optJSONObject("interface") ?: return null
        val internet = json.optJSONObject("internet") ?: return null
        if (id.isBlank()) return null
        return NetworkNode(
            nodeId = id, stale = json.optBoolean("stale"), ip = iface.optString("ip"), cidr = iface.optString("cidr"), gateway = iface.text("gateway"),
            rxBps = iface.number("rx_bps")?.toLong(), txBps = iface.number("tx_bps")?.toLong(), internet = internet(internet),
            devices = json.optJSONArray("devices").objects().mapNotNull(::device),
        )
    }

    private fun internet(json: JSONObject) = NetworkInternet(
        state = json.optString("state", "unknown"), sinceMs = json.number("since_ms")?.toLong(), gatewayOk = if (json.has("gateway_ok")) json.optBoolean("gateway_ok") else null,
        latencyMs = json.number("latency_ms"), lossPercent = json.number("loss_percent"),
        probes = json.optJSONArray("probes").objects().map { NetworkProbe(it.optString("target"), it.optString("kind"), it.optBoolean("ok"), it.number("latency_ms")) },
        outages24h = json.optInt("outages_24h"), downtime24hS = json.optInt("downtime_24h_s"),
    )

    private fun device(json: JSONObject): NetworkDevice? {
        val id = json.optString("id")
        val ip = json.optString("ip")
        if (id.isBlank() || ip.isBlank()) return null
        val note = json.optJSONObject("note")?.let { NetworkNote(it.text("name"), it.text("notes"), it.optBoolean("trusted"), it.text("kind")) }
        return NetworkDevice(
            id = id, ip = ip, mac = json.text("mac"), randomizedMac = json.optBoolean("randomized_mac"), vendor = json.text("vendor"), hostname = json.text("hostname"),
            kind = json.optString("kind", "unknown"), os = json.text("os"), online = json.optBoolean("online"), firstSeenMs = json.optLong("first_seen_ms"), lastSeenMs = json.optLong("last_seen_ms"),
            latencyMs = json.number("latency_ms"),
            ports = json.optJSONArray("ports").objects().map { NetworkPort(it.optInt("port"), it.optString("proto", "tcp"), it.text("service"), it.text("banner")) },
            services = buildList { json.optJSONArray("services")?.let { for (i in 0 until it.length()) add(it.optString(i)) } }, note = note,
        )
    }

    private fun event(json: JSONObject): NetworkEvent? {
        val id = json.optString("id")
        if (id.isBlank()) return null
        return NetworkEvent(id, json.optString("node_id"), json.optString("kind"), json.optLong("at_ms"), json.text("device_id"), if (json.has("port")) json.optInt("port") else null,
            if (json.has("outage_s")) json.optInt("outage_s") else null, json.text("detail"))
    }

    private fun outage(json: JSONObject) = NetworkOutage(json.optString("kind"), json.optLong("started_ms"), json.optInt("duration_s"))
}

/** The wording of the network, in plain Spanish. */
object NetworkText {
    /** The ports a house rarely wants open on a device anyone on the network can reach. */
    val riskyPorts = setOf(21, 23, 445, 3306, 3389, 5432, 5900, 6379, 7547)

    fun state(state: String): String = when (state) {
        "up" -> "Hay internet"
        "degraded" -> "Internet va lento o pierde paquetes"
        "down" -> "Sin internet: el router responde y nada más allá"
        "lan_down" -> "La red local está caída: el router no responde"
        else -> "Comprobando internet…"
    }

    /** Whose fault it is, when it is a fault. */
    fun hint(state: String): String? = when (state) {
        "down" -> "La avería está más allá del router: el operador, la línea o la conexión del propio router."
        "lan_down" -> "La avería está de este lado: el router apagado, un cable o un conmutador, o la máquina del nodo."
        else -> null
    }

    fun kind(kind: String): String = when (kind) {
        "router" -> "Router"; "computer" -> "Ordenador"; "phone" -> "Móvil o tableta"; "tv" -> "TV o reproductor"; "printer" -> "Impresora"; "camera" -> "Cámara"
        "iot" -> "Dispositivo inteligente"; "server" -> "Servidor"; "nas" -> "Almacenamiento (NAS)"; "network" -> "Equipo de red"; else -> "Desconocido"
    }

    /** 45 -> "45 s", 200 -> "3 min", 7500 -> "2 h 5 min", 200000 -> "2 d 7 h". */
    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        if (s < 60) return "$s s"
        val m = Math.round(s / 60.0)
        if (m < 60) return "$m min"
        val h = m / 60
        if (h < 24) return if (m % 60 != 0L) "$h h ${m % 60} min" else "$h h"
        val d = h / 24
        return if (h % 24 != 0L) "$d d ${h % 24} h" else "$d d"
    }

    fun bps(value: Long?): String = when {
        value == null -> "—"
        value >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.2f Gbit/s", value / 1e9)
        value >= 10_000_000 -> "${Math.round(value / 1e6)} Mbit/s"
        value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f Mbit/s", value / 1e6)
        value >= 1_000 -> "${Math.round(value / 1e3)} kbit/s"
        else -> "$value bit/s"
    }

    /** The sentence of an event; the device is named as the operator would call it. */
    fun event(event: NetworkEvent, devices: List<NetworkDevice>): String {
        val device = devices.firstOrNull { it.id == event.deviceId }
        val name = device?.let { if (it.name == it.ip) it.ip else "${it.name} (${it.ip})" } ?: event.deviceId.orEmpty()
        return when (event.kind) {
            "new_device" -> "Ha aparecido un dispositivo que nunca se había visto: $name"
            "device_online" -> "$name ha vuelto"
            "device_offline" -> "$name ha dejado de responder"
            "ip_changed" -> "$name ha cambiado de dirección"
            "arp_conflict" -> "Dos máquinas responden por una misma dirección: ${event.detail ?: name}"
            "port_opened" -> "$name ha abierto el puerto ${event.port}"
            "port_closed" -> "$name ha cerrado el puerto ${event.port}"
            "internet_down" -> "Se cayó internet"
            "internet_up" -> "Vuelve internet tras ${duration((event.outageS ?: 0).toLong())}"
            "gateway_down" -> "El router ha dejado de responder"
            "gateway_up" -> "El router vuelve a responder"
            else -> event.kind
        }
    }

    /** The line the Status screen shows under the Network tile: the state of the internet and how many devices there are. */
    fun summary(overview: NetworkOverview): String {
        val node = overview.nodes.firstOrNull() ?: return ""
        val devices = "${overview.totals.online}/${overview.totals.devices} dispositivos"
        return "${state(node.internet.state)} · $devices" + if (overview.totals.unknown > 0) " · ${overview.totals.unknown} sin conocer" else ""
    }
}
