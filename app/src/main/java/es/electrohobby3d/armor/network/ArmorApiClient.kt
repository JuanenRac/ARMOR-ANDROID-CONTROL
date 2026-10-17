// ARMOR-ANDROID-CONTROL - ARMOR-SERVER HTTP client.
// Camera passwords and RTSP URLs never enter this client or the Android UI.
package es.electrohobby3d.armor.network

import es.electrohobby3d.armor.model.optStr
import es.electrohobby3d.armor.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** [code] is the HTTP status when the server answered, so a caller can tell an expired session (401) from an outage. */
class ArmorApiException(message: String, val code: Int? = null) : IllegalStateException(message)

class ArmorApiClient {
    init {
        // Retain only the server's HttpOnly operator-session cookie. Android's
        // HttpURLConnection has no cookie store until one is explicitly set.
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
        }
    }

    /** The site as Studio designed it (terrain, buildings, radars, cameras); null when no design has been saved yet. */
    fun site(origin: String): SiteDesign? = SiteParser.parse(getJson(origin, "/api/v1/site"))

    fun status(origin: String): ArmorSnapshot = getJson(origin, "/api/v1/status").let { root ->
        val nodeObject = root.optJSONObject("nodes") ?: JSONObject()
        val nodes = nodeObject.keys().asSequence().map { id ->
            nodeObject.getJSONObject(id).let { node ->
                FieldNode(id, node.optBoolean("online"), if (node.isNull("lux")) null else node.optDouble("lux"), node.optInt("target_count"), node.optStr("alert_level", "normal"), node.optBoolean("stale"), SiteParser.targets(node))
            }
        }.toList()
        ArmorSnapshot(root.optStr("mode", "disarmed"), root.optLong("revision"), root.optStr("updated_at"), nodes)
    }

    /** The newest events first; [before] is an event id (only older events are returned). With [after] the events that came after that id, oldest first (a client that polls misses none). */
    fun history(origin: String, limit: Int = 50, before: Long? = null, after: Long? = null): List<ArmorEvent> {
        val query = "?limit=${limit}" + (before?.let { "&before=${it}" } ?: "") + (after?.let { "&after=${it}" } ?: "")
        return getJson(origin, "/api/v1/history${query}").optJSONArray("events").asObjects().mapNotNull(EventParser::parse)
    }

    /** What would leave the perimeter blind or noisy, asked before arming. */
    fun preflight(origin: String): es.electrohobby3d.armor.model.Preflight = es.electrohobby3d.armor.model.PreflightParser.parse(getJson(origin, "/api/v1/preflight"))

    /** Reachability of every configured camera, by id. */
    fun cameraStatus(origin: String): List<CameraHealth> = getJson(origin, "/api/v1/camera-status").optJSONArray("cameras").asObjects()
        .map { CameraHealth(it.optStr("id"), it.optStr("status", "unknown")) }.filter { it.id.isNotBlank() }

    fun cameraViews(origin: String): List<CameraView> = getJson(origin, "/api/v1/camera-views").optJSONArray("cameras").asObjects().filter { it.optStr("id").isNotBlank() }.map { camera ->
        CameraView(camera.optStr("id"), camera.optStr("name", camera.optStr("id")), camera.optStr("host"), camera.optStr("snapshotUrl"), camera.optStr("rtspPath"), camera.optInt("onvifPort", 80), camera.optInt("rtspPort", 554), camera.optBoolean("hasCredentials"), camera.optBoolean("liveVideoAvailable"))
    }

    /** Arm or disarm the system; the server audits it with the signed-in user's name. Disarming carries the PIN of the user when there is one to type. */
    fun setMode(origin: String, mode: String, pin: String? = null): ArmorSnapshot {
        require(mode == "armed" || mode == "disarmed") { "El modo es armed o disarmed" }
        val body = JSONObject().put("mode", mode)
        if (mode == "disarmed" && !pin.isNullOrEmpty()) body.put("pin", pin)
        try {
            request(origin, "/api/v1/mode", "POST", mapOf("Content-Type" to "application/json"), body.toString(), setOf(200)).disconnect()
        } catch (error: ArmorApiException) {
            throw pinFailure(error) ?: error
        }
        return status(origin)
    }

    /** The refusal of a PIN (wrong, locked, none set) in words; null for any other failure. */
    private fun pinFailure(error: ArmorApiException): ArmorApiException? {
        val code = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"").find(error.message.orEmpty())?.groupValues?.get(1).orEmpty()
        return if (code == "wrong_pin" || code == "pin_locked" || code == "pin_not_set") ArmorApiException(es.electrohobby3d.armor.model.AlarmPanelText.error(code), error.code) else null
    }

    /** Active alarms first (raised and not yet cleared, or cleared but waiting for a person), then the closed record. */
    fun alarms(origin: String): Pair<List<Alarm>, List<Alarm>> = getJson(origin, "/api/v1/alarms").let { root ->
        root.optJSONArray("active").asObjects().mapNotNull(DeviceParser::alarm) to root.optJSONArray("recent").asObjects().mapNotNull(DeviceParser::alarm)
    }
    fun acknowledgeAlarm(origin: String, id: String) { request(origin, "/api/v1/alarms/${part(id)}/acknowledge", "POST", emptyMap(), null, setOf(200)).disconnect() }
    fun acknowledgeAllAlarms(origin: String) { request(origin, "/api/v1/alarms/acknowledge", "POST", emptyMap(), null, setOf(200)).disconnect() }

    /** Every inverter and battery that has reported, the declared ones still waiting, and the sums; an operator session is needed. */
    fun solar(origin: String): SolarOverview = SolarParser.overview(getJson(origin, "/api/v1/solar"))
    fun electrical(origin: String): es.electrohobby3d.armor.model.ElectricalOverview = es.electrohobby3d.armor.model.ElectricalParser.overview(getJson(origin, "/api/v1/electrical/readings"))

    /** The alarm nodes (ARMOR-ALARM): each panel's phase and mode, its zones and its last events; an operator session is needed. */
    fun alarmPanels(origin: String): es.electrohobby3d.armor.model.AlarmPanelsOverview = es.electrohobby3d.armor.model.AlarmPanelsParser.overview(getJson(origin, "/api/v1/alarm/nodes"))
    /** Whether the server may arm and disarm the panels at all, and what became of the latest commands. */
    fun alarmCommands(origin: String): es.electrohobby3d.armor.model.AlarmCommandsStatus = es.electrohobby3d.armor.model.AlarmPanelsParser.commands(getJson(origin, "/api/v1/alarm/commands"))
    /** Arms a panel (mode "away" or "stay", optionally leaving out the open zones) or disarms it (mode null, with the PIN of the signed-in user when there is one). The server checks the PIN and never sends it to the node. */
    fun sendAlarmCommand(origin: String, node: String, mode: String?, force: Boolean, pin: String? = null) {
        val body = JSONObject().put("node", node).put("action", if (mode == null) "disarm" else "arm")
        if (mode != null) { body.put("mode", mode); if (force) body.put("force", true) }
        else if (!pin.isNullOrEmpty()) body.put("pin", pin)
        try {
            request(origin, "/api/v1/alarm/command", "POST", mapOf("Content-Type" to "application/json"), body.toString(), setOf(202)).disconnect()
        } catch (error: ArmorApiException) {
            pinFailure(error)?.let { throw it }
            val code = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"").find(error.message.orEmpty())?.groupValues?.get(1).orEmpty()
            throw ArmorApiException(es.electrohobby3d.armor.model.AlarmPanelText.error(code), error.code)
        }
    }

    /** The local network as the ARMOR-NETWORK nodes see it: the internet, the devices with the names an administrator gave them, the events and the outages. */
    fun network(origin: String): es.electrohobby3d.armor.model.NetworkOverview = es.electrohobby3d.armor.model.NetworkParser.overview(getJson(origin, "/api/v1/network"))
    fun services(origin: String): es.electrohobby3d.armor.model.ServicesOverview = es.electrohobby3d.armor.model.ServicesParser.overview(getJson(origin, "/api/v1/system/services"))
    /** Asks the network node to sweep the network now, for the node finder. */
    fun scanNetworkNow(origin: String) {
        request(origin, "/api/v1/network/commands", "POST", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("type" to "scan_now")).toString(), setOf(200, 201, 202)).disconnect()
    }

    fun devices(origin: String): List<SiteDevice> = getJson(origin, "/api/v1/devices").optJSONArray("devices").asObjects().mapNotNull(DeviceParser::device)
    /** [command] is "on", "off" or "toggle"; the server refuses it for a sensor. */
    fun commandDevice(origin: String, id: String, command: String, confirm: Boolean = false) {
        val body = JSONObject(mapOf("command" to command)).apply { if (confirm) put("confirm", true) }
        request(origin, "/api/v1/devices/${part(id)}/command", "POST", mapOf("Content-Type" to "application/json"), body.toString(), setOf(200)).disconnect()
    }

    /** The server's session cookie as it stands now (it is renewed while in use), to keep between launches; null when there is none. */
    fun sessionCookie(origin: String): Pair<String, String>? {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return null
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return null
        return manager.cookieStore.get(uri).firstOrNull { it.name.endsWith("armor_studio_sid") && it.value.isNotBlank() }?.let { it.name to it.value }
    }

    /** Puts a kept session cookie back, so the next request carries it. */
    fun restoreSessionCookie(origin: String, name: String, value: String) {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return
        manager.cookieStore.add(uri, java.net.HttpCookie(name, value).apply { path = "/"; isHttpOnly = true; secure = uri.scheme == "https"; maxAge = 7L * 24 * 3600 })
    }

    fun studioSessionActive(origin: String): Boolean = getJson(origin, "/api/v1/studio/session").optBoolean("authenticated")

    /** The role of the signed-in user ("admin", "operator", "viewer"), empty when the server does not say. */
    fun studioRole(origin: String): String = getJson(origin, "/api/v1/studio/session").optJSONObject("user")?.optString("role").orEmpty()

    /** Sends the human login only once; the server returns an HttpOnly session cookie. */
    fun openStudioSession(origin: String, username: String, password: String) {
        request(
            origin,
            "/api/v1/studio/session",
            "POST",
            mapOf("Content-Type" to "application/json"),
            JSONObject(mapOf("username" to username, "password" to password)).toString(),
            setOf(201),
        ).disconnect()
    }

    /** Forgets the session cookie held here, whatever the server says (a logout with the server down). */
    fun forgetSession(origin: String) {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return
        manager.cookieStore.get(uri).forEach { manager.cookieStore.remove(uri, it) }
    }

    /** Tells the server the token Firebase gave this phone, so that it can push alarms to it (a server with push off answers 409, which is not a failure of the app). */
    fun registerPush(origin: String, token: String) {
        request(origin, "/api/v1/push/register", "POST", mapOf("Content-Type" to "application/json"), JSONObject().put("token", token).toString(), setOf(200, 201, 409)).disconnect()
    }

    /** Takes the phone's token back (it signs out): by POST, because some HTTP stacks send no body with DELETE. */
    fun unregisterPush(origin: String, token: String) {
        request(origin, "/api/v1/push/unregister", "POST", mapOf("Content-Type" to "application/json"), JSONObject().put("token", token).toString(), setOf(200)).disconnect()
    }

    fun closeStudioSession(origin: String) {
        request(origin, "/api/v1/studio/session", "DELETE", emptyMap(), null, setOf(204)).disconnect()
    }

    fun snapshot(origin: String, cameraId: String) { request(origin, "/api/v1/cameras/${part(cameraId)}/snapshot", "POST", emptyMap(), null, setOf(201)).disconnect() }
    fun recording(origin: String, cameraId: String, start: Boolean) {
        request(origin, "/api/v1/cameras/${part(cameraId)}/recordings/${if (start) "start" else "stop"}", "POST", emptyMap(), null, setOf(if (start) 202 else 200)).disconnect()
    }
    fun ptz(origin: String, cameraId: String, command: String) {
        request(origin, "/api/v1/cameras/${part(cameraId)}/ptz", "POST", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("command" to command)).toString(), setOf(200)).disconnect()
    }
    fun media(origin: String): MediaCatalogue = getJson(origin, "/api/v1/media").let { root ->
        // one item with a field missing is left out, not the whole library
        val items = root.optJSONArray("items").asObjects().filter { it.optStr("id").isNotBlank() && it.optStr("cameraId").isNotBlank() && it.optStr("kind").isNotBlank() && it.optStr("file").isNotBlank() }
            .map { item -> MediaItem(item.optStr("id"), item.optStr("cameraId"), item.optStr("kind"), item.optStr("file"), item.optStr("createdAt"), item.optLong("bytes")) }
        MediaCatalogue(items, root.optJSONArray("activeCameraIds").asStrings().toSet())
    }
    fun deleteMedia(origin: String, item: MediaItem) {
        val kind = if (item.kind == "snapshot") "snapshots" else "recordings"
        request(origin, "/api/v1/media/${part(item.cameraId)}/${kind}/${part(item.file)}", "DELETE", emptyMap(), null, setOf(204)).disconnect()
    }
    fun mediaUrl(origin: String, item: MediaItem): String {
        val kind = if (item.kind == "snapshot") "snapshots" else "recordings"
        return "${origin}/api/v1/media/${part(item.cameraId)}/${kind}/${part(item.file)}"
    }
    fun mjpegUrl(origin: String, id: String): String = "${origin}/api/v1/cameras/${part(id)}/mjpeg"

    /** A ticket to watch one camera (every role may ask for it); the address to open is the origin plus the path the server gives. */
    fun streamTicket(origin: String, cameraId: String): String {
        val connection = request(origin, "/api/v1/cameras/${part(cameraId)}/stream-ticket", "POST", mapOf("Accept" to "application/json"), null, setOf(200, 201))
        val path = try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optString("path") } finally { connection.disconnect() }
        require(path.startsWith("/api/v1/cameras/")) { "The server gave no stream ticket" }
        return origin.trimEnd('/') + path
    }

    /** Whether this install has the voice gateway, so written and spoken commands work. */
    fun voiceAvailable(origin: String): Boolean = getJson(origin, "/api/v1/voice/status").optBoolean("available")

    /** One written or spoken command, as text. Arming and disarming need a second turn: send the same phrase again with the token of the first answer. */
    fun voiceCommand(origin: String, text: String, language: String, confirmation: String?): VoiceReply {
        val body = JSONObject().put("text", text).put("language", language)
        if (confirmation != null) body.put("confirmation", confirmation)
        val connection = request(origin, "/api/v1/voice/command", "POST", mapOf("Content-Type" to "application/json", "Accept" to "application/json"), body.toString(), setOf(200))
        return try { VoiceParser.parse(JSONObject(connection.inputStream.bufferedReader().use { it.readText() })) } finally { connection.disconnect() }
    }

    private fun getJson(origin: String, path: String): JSONObject {
        val connection = request(origin, path, "GET", mapOf("Accept" to "application/json"), null, setOf(200))
        return try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }) } finally { connection.disconnect() }
    }
    private fun request(origin: String, path: String, method: String, headers: Map<String, String>, body: String?, expected: Set<Int>): HttpURLConnection {
        val connection = (URL(origin.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 12_000; instanceFollowRedirects = false
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) { doOutput = true; outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) } }
        }
        // the connection is closed whatever goes wrong (a refused connection, a timeout), and the status is read once
        val code = try { connection.responseCode } catch (error: java.io.IOException) { connection.disconnect(); throw error }
        if (code !in expected) {
            val detail = try { connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(180).orEmpty() } catch (_: java.io.IOException) { "" }
            connection.disconnect()
            throw ArmorApiException("Server operation failed (HTTP ${code})${if (detail.isBlank()) "" else ": ${detail}"}", code)
        }
        return connection
    }
    /** A piece of a path: a space is %20 (the form encoding of URLEncoder writes "+", which in a path is a plus sign, so "cam 1" went to "cam+1"). */
    private fun part(value: String) = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}

private fun JSONArray?.asObjects(): List<JSONObject> = buildList { if (this@asObjects != null) for (index in 0 until length()) optJSONObject(index)?.let(::add) }
private fun JSONArray?.asStrings(): List<String> = buildList { if (this@asStrings != null) for (index in 0 until length()) add(optStr(index)) }
