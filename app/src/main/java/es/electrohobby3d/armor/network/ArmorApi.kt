// ARMOR-ANDROID-CONTROL - what the app asks of ARMOR-SERVER, as an interface: the screens and the view model depend on this, the HTTP client is one implementation, a test can be another.
package es.electrohobby3d.armor.network

import es.electrohobby3d.armor.model.*

/** What the session of the server tells about the signed-in person and the options in force for the phone. */
data class SessionFacts(val role: String = "", val deviceLock: String = "user") {
    companion object {
        /** From the answer of `GET /api/v1/studio/session`. */
        fun parse(json: org.json.JSONObject): SessionFacts = SessionFacts(
            role = json.optJSONObject("user")?.optString("role").orEmpty(),
            deviceLock = json.optJSONObject("capabilities")?.optJSONObject("security")?.optString("client.device_lock").orEmpty().ifBlank { "user" },
        )
    }
}

interface ArmorApi {
    /** The site as Studio designed it (terrain, buildings, radars, cameras); null when no design has been saved yet. */
    fun site(origin: String): SiteDesign?
    fun status(origin: String): ArmorSnapshot
    /** The newest events first; [before] is an event id (only older events are returned). With [after] the events that came after that id, oldest first (a client that polls misses none). */
    fun history(origin: String, limit: Int = 50, before: Long? = null, after: Long? = null): List<ArmorEvent>
    /** What would leave the perimeter blind or noisy, asked before arming. */
    fun preflight(origin: String): es.electrohobby3d.armor.model.Preflight
    /** Reachability of every configured camera, by id. */
    fun cameraStatus(origin: String): List<CameraHealth>
    fun cameraViews(origin: String): List<CameraView>
    /** Arm or disarm the system; the server audits it with the signed-in user's name. Disarming carries the PIN of the user when there is one to type. */
    fun setMode(origin: String, mode: String, pin: String? = null): ArmorSnapshot
    fun armingModes(origin: String): es.electrohobby3d.armor.model.ArmingModesView
    fun setArmingMode(origin: String, mode: String, pin: String? = null, confirmUnassigned: Boolean = false): ArmorSnapshot
    /** Active alarms first (raised and not yet cleared, or cleared but waiting for a person), then the closed record. */
    fun alarms(origin: String): Pair<List<Alarm>, List<Alarm>>
    fun acknowledgeAlarm(origin: String, id: String)
    fun acknowledgeAllAlarms(origin: String)
    /** Every inverter and battery that has reported, the declared ones still waiting, and the sums; an operator session is needed. */
    fun solar(origin: String): SolarOverview
    fun electrical(origin: String): es.electrohobby3d.armor.model.ElectricalOverview
    /** The battery operations of the last weeks, newest first, and whether the server lets anything be written at all; an administrator's. */
    fun batteryOperations(origin: String): es.electrohobby3d.armor.model.BatteryOverview
    /** Step one of writing to a battery: the server asks the node to read it and check everything; the answer is the operation, still being checked. The typed serial number must be the battery's. */
    fun batteryArm(origin: String, node: String, port: Int, op: String, output: String?, state: String?, serial: String): es.electrohobby3d.armor.model.BatteryOperationView
    fun batteryOperation(origin: String, id: String): es.electrohobby3d.armor.model.BatteryOperationView
    /** Step two: the operation must be armed; the serial number is typed again. */
    fun batteryExecute(origin: String, id: String, serial: String): es.electrohobby3d.armor.model.BatteryOperationView
    fun batteryCancel(origin: String, id: String): es.electrohobby3d.armor.model.BatteryOperationView
    /** The alarm nodes (ARMOR-NODE-ALARM): each panel's phase and mode, its zones and its last events; an operator session is needed. */
    fun alarmPanels(origin: String): es.electrohobby3d.armor.model.AlarmPanelsOverview
    /** The arming modes (Away, Home, Night, the owner's own) the server keeps; any signed-in session may read them. */
    fun armProfiles(origin: String): List<es.electrohobby3d.armor.model.ArmProfileView>
    /** Every device and app the server has seen (the Connections list of the history); the addresses come only for an administrator. */
    fun connections(origin: String): List<es.electrohobby3d.armor.model.ConnectionClient>
    fun nodeHealth(origin: String): es.electrohobby3d.armor.model.NodeHealthOverview
    /** Gives one order (restart, pause, resume, standby, identify, diagnostics, maintenance_on, maintenance_off) to one node; an administrator who proved themselves a moment ago. [confirm] is the node's name where it is asked. */
    fun nodeControl(origin: String, nodeId: String, operation: String, confirm: String?, durationS: Int?): es.electrohobby3d.armor.model.ControlOperationView
    /** The same order to every node that reports; [mode] is "now" or "rolling" (only a restart can be rolling); [confirm] is the word RESTART, PAUSE or STANDBY where it is asked. */
    fun nodesControl(origin: String, operation: String, mode: String, confirm: String?, durationS: Int?): es.electrohobby3d.armor.model.ControlOperationView
    /** The orders of the last weeks with a line per node; never a token. */
    fun nodeControlOperations(origin: String): List<es.electrohobby3d.armor.model.ControlOperationView>
    fun nodeControlOperation(origin: String, id: String): es.electrohobby3d.armor.model.ControlOperationView
    fun alarmCommands(origin: String): es.electrohobby3d.armor.model.AlarmCommandsStatus
    /** Arms a panel (mode "away" or "stay", optionally leaving out the open zones) or disarms it (mode null, with the PIN of the signed-in user when there is one). The server checks the PIN and never sends it to the node. */
    fun sendAlarmCommand(origin: String, node: String, mode: String?, force: Boolean, pin: String? = null, profile: String? = null)
    /** Silences the siren of a panel with the system still armed (the alarm stays latched): the PIN of the signed-in user, checked by the server like a disarm PIN and never sent to the node. */
    fun silenceAlarmSiren(origin: String, node: String, pin: String? = null)
    /** The local network as the ARMOR-NETWORK nodes see it: the internet, the devices with the names an administrator gave them, the events and the outages. */
    fun network(origin: String): es.electrohobby3d.armor.model.NetworkOverview
    fun services(origin: String): es.electrohobby3d.armor.model.ServicesOverview
    /** Asks the network node to sweep the network now, for the node finder. */
    fun scanNetworkNow(origin: String)
    /** Forgets a node that was taken away, with its health warnings (an administrator only; a node the server no longer knows is not a failure). */
    fun forgetNode(origin: String, id: String) {}
    fun devices(origin: String): List<SiteDevice>
    /** [command] is "on", "off" or "toggle"; the server refuses it for a sensor. */
    fun commandDevice(origin: String, id: String, command: String, confirm: Boolean = false)
    /** The server's session cookie as it stands now (it is renewed while in use), to keep between launches; null when there is none. */
    fun sessionCookie(origin: String): Pair<String, String>?
    fun restoreSessionCookie(origin: String, name: String, value: String)
    fun studioSessionActive(origin: String): Boolean
    /** The role of the signed-in user ("admin", "operator", "viewer"), empty when the server does not say. */
    fun studioRole(origin: String): String
    /** The role and what the server asks of the phone's lock, in one request; an older server says nothing and then the person decides. */
    fun sessionFacts(origin: String): SessionFacts = SessionFacts(role = studioRole(origin))
    /** Sends the human login only once; the server returns an HttpOnly session cookie. */
    fun openStudioSession(origin: String, username: String, password: String)
    /** Like [openStudioSession] but tells when the second factor is asked. */
    fun signIn(origin: String, username: String, password: String): SignIn
    /** The second step: the challenge of the first and the code of the app (or a recovery code). */
    fun completeSignIn(origin: String, challenge: String, code: String)
    fun mySessions(origin: String): List<SessionInfo>
    fun endSession(origin: String, id: String)
    fun endOtherSessions(origin: String)
    fun totpState(origin: String): TotpInfo
    fun totpSetup(origin: String, password: String): Pair<String, String>
    fun totpEnable(origin: String, code: String): List<String>
    fun totpDisable(origin: String, password: String)
    /** A new proof of identity (the password, or a code of the app), good for a few minutes for the critical actions. */
    fun elevate(origin: String, password: String, code: String)
    fun remoteCheck(origin: String): RemoteInfo
    fun remoteSetup(origin: String): RemoteSetup
    fun vpnSetup(origin: String, endpoint: String, port: Int, lan: String, routeLan: String): RemoteSetup
    fun vpnAddPeer(origin: String, name: String, fullTunnel: Boolean): PeerConfig
    fun vpnShowPeer(origin: String, name: String): PeerConfig
    fun vpnRemovePeer(origin: String, name: String)
    fun ddnsSetup(origin: String, provider: String, domain: String, token: String, zone: String, record: String, url: String): RemoteSetup
    fun ddnsUpdate(origin: String): RemoteSetup
    fun ddnsRemove(origin: String): RemoteSetup
    /** Forgets the session cookie held here, whatever the server says (a logout with the server down). */
    fun forgetSession(origin: String)
    /** Tells the server the token Firebase gave this phone, so that it can push alarms to it (a server with push off answers 409, which is not a failure of the app). `language` is the one the app is in: the server tells the notice of an alarm in it. */
    fun registerPush(origin: String, token: String, language: String = "")
    /** Takes the phone's token back (it signs out): by POST, because some HTTP stacks send no body with DELETE. */
    fun unregisterPush(origin: String, token: String)
    fun closeStudioSession(origin: String)
    fun snapshot(origin: String, cameraId: String)
    fun recording(origin: String, cameraId: String, start: Boolean)
    fun ptz(origin: String, cameraId: String, command: String)
    fun media(origin: String): MediaCatalogue
    fun deleteMedia(origin: String, item: MediaItem)
    fun mediaUrl(origin: String, item: MediaItem): String
    fun mjpegUrl(origin: String, id: String): String
    /** A ticket to watch one camera (every role may ask for it); the address to open is the origin plus the path the server gives. [quality] is what the app asks for explicitly: "low" (the lighter picture), "normal" or "auto" (the server decides by where the person came from). */
    fun streamTicket(origin: String, cameraId: String, quality: String = "auto"): String
    /** Whether this install has the voice gateway, so written and spoken commands work. */
    fun voiceAvailable(origin: String): Boolean
    /** One written or spoken command, as text. Arming and disarming need a second turn: send the same phrase again with the token of the first answer. */
    fun voiceCommand(origin: String, text: String, language: String, confirmation: String?): VoiceReply
}
