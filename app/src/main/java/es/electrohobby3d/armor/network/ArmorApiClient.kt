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
open class ArmorApiException(message: String, val code: Int? = null) : IllegalStateException(message) {
    /** What the server said, read once: the machine-readable code and its words, so that no caller has to dig them out of the message. */
    val apiError: ApiError by lazy { ApiError.parse(code, message.orEmpty()) }
}

/** The refusal of the server: [status] is the HTTP status (null when there was no answer), [code] the short code it gave ("wrong_pin", "elevation_required", ... or empty) and [detail] its words. */
data class ApiError(val status: Int?, val code: String, val detail: String) {
    companion object {
        private val codeField = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"")
        private val errorField = Regex("\"error\"\\s*:\\s*\"([^\"]*)\"")

        /** [message] is the text of an [ArmorApiException]: "Server operation failed (HTTP 403): {json}". */
        fun parse(status: Int?, message: String): ApiError {
            val code = codeField.find(message)?.groupValues?.get(1) ?: if (message.contains("elevation_required")) "elevation_required" else ""
            return ApiError(status, code, errorField.find(message)?.groupValues?.get(1).orEmpty())
        }
    }
}

/** The server refused a critical action because the session came from outside the house and has not proved itself again: the app asks for the password and tries again. */
class ElevationRequiredException : ArmorApiException("elevation_required", 403)

/** The first step of the sign-in: the session is open, or the account has the second factor on and the code is asked for. */
sealed class SignIn {
    object Done : SignIn()
    class NeedsCode(val challenge: String) : SignIn()
}

class ArmorApiClient : ArmorApi {
    init {
        // Retain only the server's HttpOnly operator-session cookie. Android's
        // HttpURLConnection has no cookie store until one is explicitly set.
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
        }
    }

    /** The site as Studio designed it (terrain, buildings, radars, cameras); null when no design has been saved yet. */
    override fun site(origin: String): SiteDesign? = SiteParser.parse(getJson(origin, "/api/v1/site"))

    override fun status(origin: String): ArmorSnapshot = getJson(origin, "/api/v1/status").let { root ->
        val nodeObject = root.optJSONObject("nodes") ?: JSONObject()
        val nodes = nodeObject.keys().asSequence().map { id ->
            nodeObject.getJSONObject(id).let { node ->
                FieldNode(id, node.optBoolean("online"), if (node.isNull("lux")) null else node.optDouble("lux"), node.optInt("target_count"), node.optStr("alert_level", "normal"), node.optBoolean("stale"), SiteParser.targets(node))
            }
        }.toList()
        ArmorSnapshot(root.optStr("mode", "disarmed"), root.optLong("revision"), root.optStr("updated_at"), nodes)
    }

    /** The newest events first; [before] is an event id (only older events are returned). With [after] the events that came after that id, oldest first (a client that polls misses none). */
    override fun history(origin: String, limit: Int, before: Long?, after: Long?): List<ArmorEvent> {
        val query = "?limit=${limit}" + (before?.let { "&before=${it}" } ?: "") + (after?.let { "&after=${it}" } ?: "")
        return getJson(origin, "/api/v1/history${query}").optJSONArray("events").asObjects().mapNotNull(EventParser::parse)
    }

    /** What would leave the perimeter blind or noisy, asked before arming. */
    override fun preflight(origin: String): es.electrohobby3d.armor.model.Preflight = es.electrohobby3d.armor.model.PreflightParser.parse(getJson(origin, "/api/v1/preflight"))

    /** Reachability of every configured camera, by id. */
    override fun cameraStatus(origin: String): List<CameraHealth> = getJson(origin, "/api/v1/camera-status").optJSONArray("cameras").asObjects()
        .map { CameraHealth(it.optStr("id"), it.optStr("status", "unknown")) }.filter { it.id.isNotBlank() }

    override fun cameraViews(origin: String): List<CameraView> = getJson(origin, "/api/v1/camera-views").optJSONArray("cameras").asObjects().filter { it.optStr("id").isNotBlank() }.map { camera ->
        CameraView(camera.optStr("id"), camera.optStr("name", camera.optStr("id")), camera.optStr("host"), camera.optStr("snapshotUrl"), camera.optStr("rtspPath"), camera.optInt("onvifPort", 80), camera.optInt("rtspPort", 554), camera.optBoolean("hasCredentials"), camera.optBoolean("liveVideoAvailable"))
    }

    /** Arm or disarm the system; the server audits it with the signed-in user's name. Disarming carries the PIN of the user when there is one to type. */
    override fun setMode(origin: String, mode: String, pin: String?): ArmorSnapshot {
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
        val body = error.message.orEmpty()
        val code = error.apiError.code
        return if (code == "wrong_pin" || code == "pin_locked" || code == "pin_not_set") ArmorApiException(es.electrohobby3d.armor.model.AlarmPanelText.pinRefusal(code, body), error.code) else null
    }

    /** Active alarms first (raised and not yet cleared, or cleared but waiting for a person), then the closed record. */
    override fun alarms(origin: String): Pair<List<Alarm>, List<Alarm>> = getJson(origin, "/api/v1/alarms").let { root ->
        root.optJSONArray("active").asObjects().mapNotNull(DeviceParser::alarm) to root.optJSONArray("recent").asObjects().mapNotNull(DeviceParser::alarm)
    }
    override fun acknowledgeAlarm(origin: String, id: String) { request(origin, "/api/v1/alarms/${part(id)}/acknowledge", "POST", emptyMap(), null, setOf(200)).disconnect() }
    override fun acknowledgeAllAlarms(origin: String) { request(origin, "/api/v1/alarms/acknowledge", "POST", emptyMap(), null, setOf(200)).disconnect() }

    /** Every inverter and battery that has reported, the declared ones still waiting, and the sums; an operator session is needed. */
    override fun solar(origin: String): SolarOverview = SolarParser.overview(getJson(origin, "/api/v1/solar"))
    override fun electrical(origin: String): es.electrohobby3d.armor.model.ElectricalOverview = es.electrohobby3d.armor.model.ElectricalParser.overview(getJson(origin, "/api/v1/electrical/readings"))

    override fun batteryOperations(origin: String): es.electrohobby3d.armor.model.BatteryOverview = es.electrohobby3d.armor.model.BatteryParser.overview(getJson(origin, "/api/v1/solar/battery-ops"))
    override fun batteryArm(origin: String, node: String, port: Int, op: String, output: String?, state: String?, serial: String): es.electrohobby3d.armor.model.BatteryOperationView {
        val params = JSONObject()
        if (op == "output_set") params.put("output", output.orEmpty()).put("state", state.orEmpty())
        val body = JSONObject().put("node", node).put("port", port).put("op", op).put("params", params).put("serial", serial)
        return es.electrohobby3d.armor.model.BatteryParser.operation(sendJson(origin, "/api/v1/solar/battery-ops", "POST", body, setOf(202)))
    }
    override fun batteryOperation(origin: String, id: String): es.electrohobby3d.armor.model.BatteryOperationView =
        es.electrohobby3d.armor.model.BatteryParser.operation(getJson(origin, "/api/v1/solar/battery-ops/" + java.net.URLEncoder.encode(id, "UTF-8")))
    override fun batteryExecute(origin: String, id: String, serial: String): es.electrohobby3d.armor.model.BatteryOperationView =
        es.electrohobby3d.armor.model.BatteryParser.operation(sendJson(origin, "/api/v1/solar/battery-ops/" + java.net.URLEncoder.encode(id, "UTF-8") + "/execute", "POST", JSONObject().put("serial", serial), setOf(202)))
    override fun batteryCancel(origin: String, id: String): es.electrohobby3d.armor.model.BatteryOperationView =
        es.electrohobby3d.armor.model.BatteryParser.operation(sendJson(origin, "/api/v1/solar/battery-ops/" + java.net.URLEncoder.encode(id, "UTF-8") + "/cancel", "POST", JSONObject(), setOf(202)))

    override fun nodeControl(origin: String, nodeId: String, operation: String, confirm: String?, durationS: Int?): es.electrohobby3d.armor.model.ControlOperationView {
        val body = JSONObject().put("operation", operation)
        if (confirm != null) body.put("confirm", confirm)
        if (durationS != null) body.put("duration_s", durationS)
        return es.electrohobby3d.armor.model.NodeControlParser.operation(sendJson(origin, "/api/v1/nodes/" + java.net.URLEncoder.encode(nodeId, "UTF-8") + "/control", "POST", body, setOf(202)))
    }
    override fun nodesControl(origin: String, operation: String, mode: String, confirm: String?, durationS: Int?): es.electrohobby3d.armor.model.ControlOperationView {
        val body = JSONObject().put("operation", operation).put("mode", mode)
        if (confirm != null) body.put("confirm", confirm)
        if (durationS != null) body.put("duration_s", durationS)
        return es.electrohobby3d.armor.model.NodeControlParser.operation(sendJson(origin, "/api/v1/nodes/control", "POST", body, setOf(202)))
    }
    override fun nodeControlOperations(origin: String): List<es.electrohobby3d.armor.model.ControlOperationView> =
        es.electrohobby3d.armor.model.NodeControlParser.list(getJson(origin, "/api/v1/nodes/control/operations"))
    override fun nodeControlOperation(origin: String, id: String): es.electrohobby3d.armor.model.ControlOperationView =
        es.electrohobby3d.armor.model.NodeControlParser.operation(getJson(origin, "/api/v1/nodes/control/operations/" + java.net.URLEncoder.encode(id, "UTF-8")))

    /** The alarm nodes (ARMOR-ALARM): each panel's phase and mode, its zones and its last events; an operator session is needed. */
    override fun alarmPanels(origin: String): es.electrohobby3d.armor.model.AlarmPanelsOverview = es.electrohobby3d.armor.model.AlarmPanelsParser.overview(getJson(origin, "/api/v1/alarm/nodes"))
    /** Whether the server may arm, disarm and silence the panels at all, and what became of the latest commands. */
    /** The arming modes (Away, Home, Night, the owner's own) the server keeps; any signed-in session may read them. */
    override fun armProfiles(origin: String): List<es.electrohobby3d.armor.model.ArmProfileView> = es.electrohobby3d.armor.model.AlarmPanelsParser.profiles(getJson(origin, "/api/v1/alarm/profiles"))

    /** The health report of every node, with the warnings the server raised about each (an operator session is needed). */
    /** Every device and app the server has seen (the Connections list of the history); the addresses come only for an administrator. */
    override fun connections(origin: String): List<es.electrohobby3d.armor.model.ConnectionClient> = es.electrohobby3d.armor.model.ConnectionsParser.list(getJson(origin, "/api/v1/history/connections"))
    override fun nodeHealth(origin: String): es.electrohobby3d.armor.model.NodeHealthOverview = es.electrohobby3d.armor.model.NodeHealthParser.overview(getJson(origin, "/api/v1/node-health"))

    override fun alarmCommands(origin: String): es.electrohobby3d.armor.model.AlarmCommandsStatus = es.electrohobby3d.armor.model.AlarmPanelsParser.commands(getJson(origin, "/api/v1/alarm/commands"))
    /** Arms a panel (mode "away" or "stay", optionally leaving out the open zones) or disarms it (mode null, with the PIN of the signed-in user when there is one). The server checks the PIN and never sends it to the node. */
    override fun sendAlarmCommand(origin: String, node: String, mode: String?, force: Boolean, pin: String?, profile: String?) {
        val body = JSONObject().put("node", node).put("action", if (mode == null) "disarm" else "arm")
        if (mode != null) { body.put("mode", mode); if (profile != null) body.put("profile", profile); if (force) body.put("force", true) }
        else if (!pin.isNullOrEmpty()) body.put("pin", pin)
        postAlarmCommand(origin, body)
    }

    /** Silences the siren of a panel with the system still armed (the alarm stays latched): the PIN of the signed-in user, checked by the server like a disarm PIN and never sent to the node. */
    override fun silenceAlarmSiren(origin: String, node: String, pin: String?) {
        val body = JSONObject().put("node", node).put("action", "silence")
        if (!pin.isNullOrEmpty()) body.put("pin", pin)
        postAlarmCommand(origin, body)
    }

    private fun postAlarmCommand(origin: String, body: JSONObject) {
        try {
            request(origin, "/api/v1/alarm/command", "POST", mapOf("Content-Type" to "application/json"), body.toString(), setOf(202)).disconnect()
        } catch (error: ArmorApiException) {
            pinFailure(error)?.let { throw it }
            val code = error.apiError.code
            throw ArmorApiException(es.electrohobby3d.armor.model.AlarmPanelText.error(code), error.code)
        }
    }

    /** The local network as the ARMOR-NETWORK nodes see it: the internet, the devices with the names an administrator gave them, the events and the outages. */
    override fun network(origin: String): es.electrohobby3d.armor.model.NetworkOverview = es.electrohobby3d.armor.model.NetworkParser.overview(getJson(origin, "/api/v1/network"))
    override fun services(origin: String): es.electrohobby3d.armor.model.ServicesOverview = es.electrohobby3d.armor.model.ServicesParser.overview(getJson(origin, "/api/v1/system/services"))
    /** Asks the network node to sweep the network now, for the node finder. */
    override fun scanNetworkNow(origin: String) {
        request(origin, "/api/v1/network/commands", "POST", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("type" to "scan_now")).toString(), setOf(200, 201, 202)).disconnect()
    }

    override fun devices(origin: String): List<SiteDevice> = getJson(origin, "/api/v1/devices").optJSONArray("devices").asObjects().mapNotNull(DeviceParser::device)
    /** [command] is "on", "off" or "toggle"; the server refuses it for a sensor. */
    override fun commandDevice(origin: String, id: String, command: String, confirm: Boolean) {
        val body = JSONObject(mapOf("command" to command)).apply { if (confirm) put("confirm", true) }
        request(origin, "/api/v1/devices/${part(id)}/command", "POST", mapOf("Content-Type" to "application/json"), body.toString(), setOf(200)).disconnect()
    }

    /** The server's session cookie as it stands now (it is renewed while in use), to keep between launches; null when there is none. */
    override fun sessionCookie(origin: String): Pair<String, String>? {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return null
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return null
        return manager.cookieStore.get(uri).firstOrNull { it.name.endsWith("armor_studio_sid") && it.value.isNotBlank() }?.let { it.name to it.value }
    }

    /**
     * Puts a kept session cookie back, so the next request carries it. The cookie is a plain one (version 0): a cookie made by hand is otherwise sent in the RFC 2965 form
     * (`$Version="1"; name="value";$Path="/"`), and the server would read the value with its quotes and never find the session.
     */
    override fun restoreSessionCookie(origin: String, name: String, value: String) {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return
        manager.cookieStore.add(uri, java.net.HttpCookie(name, value).apply { version = 0; path = "/"; isHttpOnly = true; secure = uri.scheme == "https"; maxAge = 7L * 24 * 3600 })
    }

    override fun studioSessionActive(origin: String): Boolean = getJson(origin, "/api/v1/studio/session").optBoolean("authenticated")

    /** The role of the signed-in user ("admin", "operator", "viewer"), empty when the server does not say. */
    override fun studioRole(origin: String): String = getJson(origin, "/api/v1/studio/session").optJSONObject("user")?.optString("role").orEmpty()

    /** Sends the human login only once; the server returns an HttpOnly session cookie. */
    override fun openStudioSession(origin: String, username: String, password: String) {
        request(
            origin,
            "/api/v1/studio/session",
            "POST",
            mapOf("Content-Type" to "application/json"),
            JSONObject(mapOf("username" to username, "password" to password)).toString(),
            setOf(201),
        ).disconnect()
    }

    /** Like [openStudioSession] but tells when the second factor is asked. */
    override fun signIn(origin: String, username: String, password: String): SignIn {
        val connection = request(origin, "/api/v1/studio/session", "POST", mapOf("Content-Type" to "application/json", "Accept" to "application/json"), JSONObject(mapOf("username" to username, "password" to password)).toString(), setOf(200, 201))
        return try {
            val body = runCatching { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }) }.getOrDefault(JSONObject())
            if (body.optBoolean("totp_required") && body.optString("challenge").isNotBlank()) SignIn.NeedsCode(body.optString("challenge")) else SignIn.Done
        } finally { connection.disconnect() }
    }

    /** The second step: the challenge of the first and the code of the app (or a recovery code). */
    override fun completeSignIn(origin: String, challenge: String, code: String) {
        request(origin, "/api/v1/studio/session/totp", "POST", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("challenge" to challenge, "code" to code)).toString(), setOf(201)).disconnect()
    }

    override fun mySessions(origin: String): List<SessionInfo> = SecurityParser.sessions(getJson(origin, "/api/v1/studio/sessions"))
    override fun endSession(origin: String, id: String) { request(origin, "/api/v1/studio/sessions/${part(id)}", "DELETE", emptyMap(), null, setOf(204)).disconnect() }
    override fun endOtherSessions(origin: String) { request(origin, "/api/v1/studio/sessions/end-others", "POST", emptyMap(), null, setOf(204)).disconnect() }
    override fun totpState(origin: String): TotpInfo = SecurityParser.totp(getJson(origin, "/api/v1/studio/totp"))
    override fun totpSetup(origin: String, password: String): Pair<String, String> {
        val connection = request(origin, "/api/v1/studio/totp/setup", "POST", mapOf("Content-Type" to "application/json", "Accept" to "application/json"), JSONObject(mapOf("password" to password)).toString(), setOf(200))
        return try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).let { it.optString("secret") to it.optString("uri") } } finally { connection.disconnect() }
    }
    override fun totpEnable(origin: String, code: String): List<String> {
        val connection = request(origin, "/api/v1/studio/totp/enable", "POST", mapOf("Content-Type" to "application/json", "Accept" to "application/json"), JSONObject(mapOf("code" to code)).toString(), setOf(200))
        return try { SecurityParser.codes(JSONObject(connection.inputStream.bufferedReader().use { it.readText() })) } finally { connection.disconnect() }
    }
    override fun totpDisable(origin: String, password: String) {
        request(origin, "/api/v1/studio/totp", "DELETE", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("password" to password)).toString(), setOf(204)).disconnect()
    }
    /** A new proof of identity (the password, or a code of the app), good for a few minutes for the critical actions. */
    override fun elevate(origin: String, password: String, code: String) {
        val body = JSONObject().apply { if (code.isNotBlank()) put("code", code) else put("password", password) }
        request(origin, "/api/v1/studio/elevate", "POST", mapOf("Content-Type" to "application/json"), body.toString(), setOf(200)).disconnect()
    }
    override fun remoteCheck(origin: String): RemoteInfo = SecurityParser.remote(getJson(origin, "/api/v1/remote/check"))

    // ---- the way in from outside the house, set up from the phone (administrators): the VPN and the dynamic DNS name ----
    override fun remoteSetup(origin: String): RemoteSetup = SecurityParser.remoteSetup(getJson(origin, "/api/v1/admin/remote"))
    override fun vpnSetup(origin: String, endpoint: String, port: Int, lan: String, routeLan: String): RemoteSetup {
        val body = JSONObject().put("endpoint", endpoint).put("port", port)
        if (lan.isNotBlank()) body.put("lan", lan)
        if (routeLan.isNotBlank()) body.put("route_lan", routeLan)
        return SecurityParser.remoteSetup(sendJson(origin, "/api/v1/admin/remote/wireguard/setup", "POST", body, setOf(200)))
    }
    override fun vpnAddPeer(origin: String, name: String, fullTunnel: Boolean): PeerConfig =
        SecurityParser.peerConfig(sendJson(origin, "/api/v1/admin/remote/wireguard/peers", "POST", JSONObject().put("name", name).put("full_tunnel", fullTunnel), setOf(201)))
    override fun vpnShowPeer(origin: String, name: String): PeerConfig = SecurityParser.peerConfig(getJson(origin, "/api/v1/admin/remote/wireguard/peers/${part(name)}"))
    override fun vpnRemovePeer(origin: String, name: String) { sendJson(origin, "/api/v1/admin/remote/wireguard/peers/${part(name)}", "DELETE", null, setOf(200)) }
    override fun ddnsSetup(origin: String, provider: String, domain: String, token: String, zone: String, record: String, url: String): RemoteSetup {
        val body = JSONObject().put("provider", provider)
        mapOf("domain" to domain, "token" to token, "zone" to zone, "record_name" to record, "url" to url).forEach { (key, value) -> if (value.isNotBlank()) body.put(key, value) }
        return SecurityParser.remoteSetup(sendJson(origin, "/api/v1/admin/remote/ddns/setup", "POST", body, setOf(200)))
    }
    override fun ddnsUpdate(origin: String): RemoteSetup = SecurityParser.remoteSetup(sendJson(origin, "/api/v1/admin/remote/ddns/update", "POST", JSONObject(), setOf(200)))
    override fun ddnsRemove(origin: String): RemoteSetup = SecurityParser.remoteSetup(sendJson(origin, "/api/v1/admin/remote/ddns/remove", "POST", JSONObject(), setOf(200)))

    /** Forgets the session cookie held here, whatever the server says (a logout with the server down). */
    override fun forgetSession(origin: String) {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return
        manager.cookieStore.get(uri).forEach { manager.cookieStore.remove(uri, it) }
    }

    /** Tells the server the token Firebase gave this phone, so that it can push alarms to it (a server with push off answers 409, which is not a failure of the app). */
    override fun registerPush(origin: String, token: String) {
        request(origin, "/api/v1/push/register", "POST", mapOf("Content-Type" to "application/json"), JSONObject().put("token", token).toString(), setOf(200, 201, 409)).disconnect()
    }

    /** Takes the phone's token back (it signs out): by POST, because some HTTP stacks send no body with DELETE. */
    override fun unregisterPush(origin: String, token: String) {
        request(origin, "/api/v1/push/unregister", "POST", mapOf("Content-Type" to "application/json"), JSONObject().put("token", token).toString(), setOf(200)).disconnect()
    }

    override fun closeStudioSession(origin: String) {
        request(origin, "/api/v1/studio/session", "DELETE", emptyMap(), null, setOf(204)).disconnect()
    }

    override fun snapshot(origin: String, cameraId: String) { request(origin, "/api/v1/cameras/${part(cameraId)}/snapshot", "POST", emptyMap(), null, setOf(201)).disconnect() }
    override fun recording(origin: String, cameraId: String, start: Boolean) {
        request(origin, "/api/v1/cameras/${part(cameraId)}/recordings/${if (start) "start" else "stop"}", "POST", emptyMap(), null, setOf(if (start) 202 else 200)).disconnect()
    }
    override fun ptz(origin: String, cameraId: String, command: String) {
        request(origin, "/api/v1/cameras/${part(cameraId)}/ptz", "POST", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("command" to command)).toString(), setOf(200)).disconnect()
    }
    override fun media(origin: String): MediaCatalogue = getJson(origin, "/api/v1/media").let { root ->
        // one item with a field missing is left out, not the whole library
        val items = root.optJSONArray("items").asObjects().filter { it.optStr("id").isNotBlank() && it.optStr("cameraId").isNotBlank() && it.optStr("kind").isNotBlank() && it.optStr("file").isNotBlank() }
            .map { item -> MediaItem(item.optStr("id"), item.optStr("cameraId"), item.optStr("kind"), item.optStr("file"), item.optStr("createdAt"), item.optLong("bytes")) }
        MediaCatalogue(items, root.optJSONArray("activeCameraIds").asStrings().toSet())
    }
    override fun deleteMedia(origin: String, item: MediaItem) {
        val kind = if (item.kind == "snapshot") "snapshots" else "recordings"
        request(origin, "/api/v1/media/${part(item.cameraId)}/${kind}/${part(item.file)}", "DELETE", emptyMap(), null, setOf(204)).disconnect()
    }
    override fun mediaUrl(origin: String, item: MediaItem): String {
        val kind = if (item.kind == "snapshot") "snapshots" else "recordings"
        return "${origin}/api/v1/media/${part(item.cameraId)}/${kind}/${part(item.file)}"
    }
    override fun mjpegUrl(origin: String, id: String): String = "${origin}/api/v1/cameras/${part(id)}/mjpeg"

    /** A ticket to watch one camera (every role may ask for it); the address to open is the origin plus the path the server gives. */
    override fun streamTicket(origin: String, cameraId: String): String {
        val connection = request(origin, "/api/v1/cameras/${part(cameraId)}/stream-ticket", "POST", mapOf("Accept" to "application/json"), null, setOf(200, 201))
        val path = try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optString("path") } finally { connection.disconnect() }
        require(path.startsWith("/api/v1/cameras/")) { "The server gave no stream ticket" }
        return origin.trimEnd('/') + path
    }

    /** Whether this install has the voice gateway, so written and spoken commands work. */
    override fun voiceAvailable(origin: String): Boolean = getJson(origin, "/api/v1/voice/status").optBoolean("available")

    /** One written or spoken command, as text. Arming and disarming need a second turn: send the same phrase again with the token of the first answer. */
    override fun voiceCommand(origin: String, text: String, language: String, confirmation: String?): VoiceReply {
        val body = JSONObject().put("text", text).put("language", language)
        if (confirmation != null) body.put("confirmation", confirmation)
        val connection = request(origin, "/api/v1/voice/command", "POST", mapOf("Content-Type" to "application/json", "Accept" to "application/json"), body.toString(), setOf(200))
        return try { VoiceParser.parse(JSONObject(connection.inputStream.bufferedReader().use { it.readText() })) } finally { connection.disconnect() }
    }

    /** A request with a JSON body (or none) whose answer is a JSON object (or nothing). */
    private fun sendJson(origin: String, path: String, method: String, body: JSONObject?, expected: Set<Int>): JSONObject {
        val headers = mapOf("Accept" to "application/json") + if (body != null) mapOf("Content-Type" to "application/json") else emptyMap()
        val connection = request(origin, path, method, headers, body?.toString(), expected)
        return try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }.ifBlank { "{}" }) } finally { connection.disconnect() }
    }

    private fun getJson(origin: String, path: String): JSONObject {
        val connection = request(origin, path, "GET", mapOf("Accept" to "application/json"), null, setOf(200))
        return try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }) } finally { connection.disconnect() }
    }
    private fun request(origin: String, path: String, method: String, headers: Map<String, String>, body: String?, expected: Set<Int>): HttpURLConnection {
        val connection = (URL(origin.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 12_000; instanceFollowRedirects = false
            // what the app says it is, so that the history of connections of the server can tell it from Studio and from the other clients (only informative)
            setRequestProperty("X-Armor-Client", "android/${es.electrohobby3d.armor.BuildConfig.VERSION_NAME}")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) { doOutput = true; outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) } }
        }
        // the connection is closed whatever goes wrong (a refused connection, a timeout), and the status is read once
        val code = try { connection.responseCode } catch (error: java.io.IOException) { connection.disconnect(); throw error }
        if (code !in expected) {
            val detail = try { connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(180).orEmpty() } catch (_: java.io.IOException) { "" }
            connection.disconnect()
            if (code == 403 && detail.contains("elevation_required")) throw ElevationRequiredException()
            throw ArmorApiException("Server operation failed (HTTP ${code})${if (detail.isBlank()) "" else ": ${detail}"}", code)
        }
        return connection
    }
    /** A piece of a path: a space is %20 (the form encoding of URLEncoder writes "+", which in a path is a plus sign, so "cam 1" went to "cam+1"). */
    private fun part(value: String) = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}

private fun JSONArray?.asObjects(): List<JSONObject> = buildList { if (this@asObjects != null) for (index in 0 until length()) optJSONObject(index)?.let(::add) }
private fun JSONArray?.asStrings(): List<String> = buildList { if (this@asStrings != null) for (index in 0 until length()) add(optStr(index)) }
