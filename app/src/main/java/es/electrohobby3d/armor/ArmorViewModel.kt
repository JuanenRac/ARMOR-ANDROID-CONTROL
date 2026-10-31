// ARMOR-ANDROID-CONTROL - lifecycle-aware operator and monitoring state.
package es.electrohobby3d.armor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.SiteDevice
import es.electrohobby3d.armor.model.ArmorSnapshot
import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.model.MediaCatalogue
import es.electrohobby3d.armor.network.ArmorApi
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.SessionFacts
import es.electrohobby3d.armor.network.WeatherApiClient
import es.electrohobby3d.armor.model.Forecast
import es.electrohobby3d.armor.model.AirQuality
import es.electrohobby3d.armor.model.Place
import es.electrohobby3d.armor.model.PendingVoice
import es.electrohobby3d.armor.model.VoiceMessage
import es.electrohobby3d.armor.model.VoiceParser
import es.electrohobby3d.armor.model.VoiceUiState
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.ElevationRequiredException
import es.electrohobby3d.armor.network.SignIn
import es.electrohobby3d.armor.model.SecurityParser
import es.electrohobby3d.armor.model.SessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class WeatherUiState(
    val place: Place? = null, val forecast: Forecast? = null, val air: AirQuality? = null, val loading: Boolean = false, val error: String? = null, val results: List<Place> = emptyList(),
)

data class MonitorUiState(
    /** The role of the signed-in user; a viewer only looks. */
    val role: String = "",
    /** What the server asks of the phone's lock: "user" (the person decides), "sensitive" or "always". */
    val deviceLock: String = "user",
    val loading: Boolean = false,
    val snapshot: ArmorSnapshot = ArmorSnapshot(),
    val cameras: List<CameraView> = emptyList(),
    val media: MediaCatalogue = MediaCatalogue(),
    val events: List<ArmorEvent> = emptyList(),
    /** Camera id to "online" / "offline" / "unknown", from the server's watchdog. */
    val cameraHealth: Map<String, String> = emptyMap(),
    /** Alarms waiting for a person or not yet cleared, and the closed record; empty against a server that predates alarms. */
    val alarms: List<Alarm> = emptyList(),
    val closedAlarms: List<Alarm> = emptyList(),
    val devices: List<SiteDevice> = emptyList(),
    /** Solar inverters and batteries; null until the server has answered (an older server never does). */
    val solar: es.electrohobby3d.armor.model.SolarOverview? = null,
    /** The alarm panels (ARMOR-NODE-ALARM nodes); null until the server has answered (an older server never does). */
    val alarmPanels: es.electrohobby3d.armor.model.AlarmPanelsOverview? = null,
    /** The arming modes of the server (Away, Home, Night); null until it has answered, and while it is off (or an older server) the app arms and disarms as it always did. */
    val armingModes: es.electrohobby3d.armor.model.ArmingModesView? = null,
    /** The arming modes of the alarm panels (Away, Home, Night, the owner's own). */
    val armProfiles: List<es.electrohobby3d.armor.model.ArmProfileView> = emptyList(),
    /** The devices and apps that connect to the server; null until the server has answered (an older server never does). */
    val connections: List<es.electrohobby3d.armor.model.ConnectionClient>? = null,
    /** The health of the nodes; null until the server has answered. */
    val nodeHealth: es.electrohobby3d.armor.model.NodeHealthOverview? = null,
    /** Whether the server may arm and disarm the panels, and the latest results of the commands. */
    val alarmCommands: es.electrohobby3d.armor.model.AlarmCommandsStatus = es.electrohobby3d.armor.model.AlarmCommandsStatus(),
    /** The electrical nodes' meters; null until the server has answered (an older server never does). */
    val electrical: es.electrohobby3d.armor.model.ElectricalOverview? = null,
    /** The local network as the ARMOR-NETWORK nodes see it; null until the server has answered (an older server never does). */
    val network: es.electrohobby3d.armor.model.NetworkOverview? = null,
    /** Every service of the system and field node, running or not; null until the server has answered (an older server never does). */
    val services: es.electrohobby3d.armor.model.ServicesOverview? = null,
    /** The weather menu: its own place, forecast and air quality, nothing to do with ARMOR-SERVER. */
    val weather: WeatherUiState = WeatherUiState(),
    /** The site as Studio designed it, for the radar screen; [siteLoaded] tells "not asked yet" from "no design saved". */
    val site: es.electrohobby3d.armor.model.SiteDesign? = null,
    val siteLoaded: Boolean = false,
    val authenticated: Boolean = false,
    /** The server has not answered at all for a few passes: the address in use may be the wrong one for where the phone is. */
    val silent: Boolean = false,
    val message: String? = null,
) {
    val readOnly: Boolean get() = role.isNotEmpty() && role != "admin" && role != "operator"   // a viewer and a wall panel (which may read, arm, disarm and acknowledge, and nothing else) only look, and so does any role this app does not know; an empty role is "not told yet"
    /** Only an administrator may arm and disarm the alarm panels from the server. */
    val isAdmin: Boolean get() = role == "admin"
}

class ArmorViewModel(private val client: ArmorApi = SharedFeed.api, private val weatherClient: WeatherApiClient = WeatherApiClient()) : ViewModel() {
    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    private val _voice = MutableStateFlow(VoiceUiState())
    /** The conversation of the Assistant screen (written and spoken commands). */
    val voice: StateFlow<VoiceUiState> = _voice.asStateFlow()

    /** Asks the server whether this install has the voice gateway. */
    fun checkVoice(origin: String) {
        if (ServerEndpoint.parse(origin) == null) return
        viewModelScope.launch {
            val available = runCatching { withContext(Dispatchers.IO) { client.voiceAvailable(origin) } }.getOrNull()
            _voice.value = _voice.value.copy(available = available)
        }
    }

    fun clearVoice() { _voice.value = _voice.value.copy(messages = emptyList()) }

    /** The person did not confirm: the question is closed (the token would expire by itself) and nothing was done. */
    fun cancelVoice() {
        _voice.value = _voice.value.copy(messages = _voice.value.messages.map { if (it.pending != null) it.copy(pending = null) else it } + VoiceMessage(false, tr("Cancelado: no he hecho nada.")))
    }

    /**
     * Sends a command (what was typed, or what the phone's speech recognition heard). [confirm] is the command whose second turn this is; its token goes with the phrase.
     * What comes back is added to the conversation, and when the server carried something out the state of the system is read again.
     * [spoken] is called with the sentence to say, for the voice of the phone.
     */
    fun sendVoice(origin: String, text: String, language: String = Tr.voiceLanguage(), confirm: PendingVoice? = null, spoken: (String) -> Unit = {}) {
        if (viewerRefused()) return
        val phrase = (confirm?.text ?: text).trim()
        if (phrase.isEmpty() || _voice.value.busy) return
        if (ServerEndpoint.parse(origin) == null) { _voice.value = _voice.value.copy(messages = _voice.value.messages + VoiceMessage(false, tr("Configura el servidor en Ajustes."), problem = true)); return }
        // The person's words go in the conversation (a confirmation is shown as a tap, not as words), and the question that was waiting is closed.
        val before = _voice.value.messages.map { if (it.pending != null) it.copy(pending = null) else it }
        _voice.value = _voice.value.copy(busy = true, messages = if (confirm == null) before + VoiceMessage(true, phrase) else before)
        viewModelScope.launch {
            val answer = runCatching { withContext(Dispatchers.IO) { client.voiceCommand(origin, phrase, language, confirm?.token) } }
            val message = answer.fold(
                onSuccess = { reply ->
                    val pending = reply.confirmationToken?.let { PendingVoice(phrase, it) }
                    val line = VoiceParser.lineFor(reply)
                    spoken(line)
                    VoiceMessage(false, line, pending = pending, problem = reply.outcome == "not-understood" || reply.outcome == "confirmation-refused") to reply.executed
                },
                onFailure = { error -> VoiceMessage(false, VoiceParser.errorText(error.message), problem = true) to false },
            )
            _voice.value = _voice.value.copy(busy = false, messages = _voice.value.messages + message.first)
            if (message.second) runCatching { withContext(Dispatchers.IO) { refreshInternal(origin, false) } }
        }
    }

    /** The site design for the radar screen (once, and again when asked). A server that predates it, or has no design, leaves it empty. */
    suspend fun loadSite(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.site(origin) }.onSuccess { site -> _state.update { it.copy(site = site, siteLoaded = true) } }.onFailure { _state.update { it.copy(siteLoaded = true) } }
    }

    /** Only the nodes' state, quickly, for the live radar: the targets move every second. A failure is ignored; the next pass tries again. */
    suspend fun pollRadar(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.status(origin) }.onSuccess { value -> _state.update { it.copy(snapshot = value) } }
    }

    /** The solar equipment, quickly, while the Solar screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollSolar(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.solar(origin) }.onSuccess { value -> _state.update { it.copy(solar = value) } }
    }

    fun reloadSolar(origin: String) { viewModelScope.launch { pollSolar(origin) } }

    // ---- battery maintenance (administrators): the operations on a Pylontech battery through its gateway node ----
    private val _battery = MutableStateFlow(BatteryUiState())
    val battery: StateFlow<BatteryUiState> = _battery.asStateFlow()

    private fun batteryCall(block: suspend () -> Unit) {
        viewModelScope.launch {
            _battery.update { it.copy(busy = true, message = null) }
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onFailure { error ->
                    if (error is ElevationRequiredException) {   // a new proof of identity, always for this: then the same action by itself
                        retryAfterElevation = { block() }
                        _security.update { it.copy(elevationNeeded = true, elevationError = null) }
                        _battery.update { it.copy(busy = false) }
                    } else _battery.update { it.copy(busy = false, message = batteryErrorText(error)) }
                }
                .onSuccess { _battery.update { it.copy(busy = false) } }
        }
    }

    fun loadBattery(origin: String) = batteryCall { val value = client.batteryOperations(origin); _battery.update { it.copy(overview = value) } }

    /** The operation on its way, and the list, while the screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollBattery(origin: String) = withContext(Dispatchers.IO) {
        val current = _battery.value.active
        if (current != null && current.active) runCatching { client.batteryOperation(origin, current.id) }.onSuccess { value -> _battery.update { it.copy(active = value) } }
        runCatching { client.batteryOperations(origin) }.onSuccess { value -> _battery.update { it.copy(overview = value) } }
    }

    fun armBattery(origin: String, node: String, port: Int, op: String, output: String, state: String, serial: String) = batteryCall {
        val armed = client.batteryArm(origin, node, port, op, output, state, serial)
        _battery.update { it.copy(active = armed) }
    }
    fun executeBattery(origin: String, serial: String) = confirmed(tr("Operar una batería"), { text -> _battery.update { it.copy(message = text) } }) {
        batteryCall {
            val current = _battery.value.active ?: return@batteryCall
            val started = client.batteryExecute(origin, current.id, serial)
            _battery.update { it.copy(active = started) }
        }
    }
    fun cancelBattery(origin: String) = batteryCall {
        val current = _battery.value.active ?: return@batteryCall
        val next = client.batteryCancel(origin, current.id)
        _battery.update { it.copy(active = next) }
    }
    fun openBattery(operation: es.electrohobby3d.armor.model.BatteryOperationView) { _battery.update { it.copy(active = operation, message = null) } }
    fun newBatteryOperation() { _battery.update { it.copy(active = null, message = null) } }

    // ---- node control (administrators): restart, pause, standby, identify, diagnostics and maintenance of one node or of all ----
    private val _control = MutableStateFlow(NodeControlUiState())
    val control: StateFlow<NodeControlUiState> = _control.asStateFlow()

    private fun controlCall(block: suspend () -> Unit) {
        viewModelScope.launch {
            _control.update { it.copy(busy = true, message = null) }
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onFailure { error ->
                    if (error is ElevationRequiredException) {   // a new proof of identity, always for this: then the same order by itself
                        retryAfterElevation = { block() }
                        _security.update { it.copy(elevationNeeded = true, elevationError = null) }
                        _control.update { it.copy(busy = false) }
                    } else _control.update { it.copy(busy = false, message = controlErrorText(error)) }
                }
                .onSuccess { _control.update { it.copy(busy = false) } }
        }
    }

    /** The orders on their way and the list, while the screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollControl(origin: String) = withContext(Dispatchers.IO) {
        val current = _control.value.active
        if (current != null && current.active) runCatching { client.nodeControlOperation(origin, current.id) }.onSuccess { value -> _control.update { it.copy(active = value) } }
        runCatching { client.nodeControlOperations(origin) }.onSuccess { value -> _control.update { it.copy(operations = value) } }
    }

    fun orderNode(origin: String, nodeId: String, operation: String, confirm: String?, durationS: Int?) = confirmed(trf("Ordenar a %s", nodeId), { text -> _control.update { it.copy(message = text) } }) {
        controlCall {
            val started = client.nodeControl(origin, nodeId, operation, confirm, durationS)
            _control.update { it.copy(active = started) }
        }
    }
    fun orderAllNodes(origin: String, operation: String, mode: String, confirm: String?, durationS: Int?) = confirmed(tr("Ordenar a todos los nodos"), { text -> _control.update { it.copy(message = text) } }) {
        controlCall {
            val started = client.nodesControl(origin, operation, mode, confirm, durationS)
            _control.update { it.copy(active = started) }
        }
    }
    fun openControl(operation: es.electrohobby3d.armor.model.ControlOperationView) { _control.update { it.copy(active = operation, message = null) } }
    fun closeControl() { _control.update { it.copy(active = null, message = null) } }

    /** The alarm panels and the status of their commands, quickly, while the Alarm panels screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollAlarmPanels(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.alarmPanels(origin) }.onSuccess { value -> _state.update { it.copy(alarmPanels = value) } }
        runCatching { client.alarmCommands(origin) }.onSuccess { value -> _state.update { it.copy(alarmCommands = value) } }
        runCatching { client.armProfiles(origin) }.onSuccess { value -> _state.update { it.copy(armProfiles = value) } }
    }

    /** The health of the nodes, quickly, while its screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollNodeHealth(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.nodeHealth(origin) }.onSuccess { value -> _state.update { it.copy(nodeHealth = value) } }
    }

    /** The clients the server has seen, while the Connections list is open. A failure is ignored; the next pass tries again. */
    suspend fun pollConnections(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.connections(origin) }.onSuccess { value -> _state.update { it.copy(connections = value) } }
    }

    fun reloadNodeHealth(origin: String) { viewModelScope.launch { pollNodeHealth(origin) } }

    fun reloadAlarmPanels(origin: String) { viewModelScope.launch { pollAlarmPanels(origin) } }

    /** Arms a panel (mode "away" or "stay") or disarms it (mode null); the node's own answer shows in the panel's card a moment later. */
    fun alarmCommand(origin: String, node: String, mode: String?, force: Boolean = false, pin: String? = null, profile: String? = null) {
        if (viewerRefused()) return
        confirmed(if (mode == null) trf("Desarmar %s", node) else trf("Armar %s", node)) {
            mutating(origin, if (mode == null) trf("%s: orden de desarmar enviada", node) else trf("%s: orden de armar enviada", node)) {
                client.sendAlarmCommand(origin, node, mode, force, pin, profile)
                pollAlarmPanels(origin)
            }
        }
    }

    /** Silences the siren of a panel with the system still armed; the PIN is checked by the server. */
    fun silenceAlarm(origin: String, node: String, pin: String? = null) {
        if (viewerRefused()) return
        confirmed(trf("Silenciar la sirena de %s", node)) {
            mutating(origin, trf("%s: orden de silenciar la sirena enviada", node)) {
                client.silenceAlarmSiren(origin, node, pin)
                pollAlarmPanels(origin)
            }
        }
    }

    /** The electrical network, quickly, while the Electrical screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollElectrical(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.electrical(origin) }.onSuccess { value -> _state.update { it.copy(electrical = value) } }
    }

    fun reloadElectrical(origin: String) { viewModelScope.launch { pollElectrical(origin) } }

    /** The local network, quickly, while the Network screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollNetwork(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.network(origin) }.onSuccess { value -> _state.update { it.copy(network = value) } }
    }

    fun reloadNetwork(origin: String) { viewModelScope.launch { pollNetwork(origin) } }

    /** Every service, quickly, while the Services screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollServices(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.services(origin) }.onSuccess { value -> _state.update { it.copy(services = value) } }
    }

    fun reloadServices(origin: String) { viewModelScope.launch { pollServices(origin) } }

    /** Asks the network node to sweep now, for the node finder (Solar, Electrical and Radar screens); a failure is shown as a message. */
    fun scanNetworkNow(origin: String) = mutating(origin, tr("Búsqueda en la red iniciada")) { client.scanNetworkNow(origin) }

    /** Forgets a node the server still lists although it was taken away, and reads the health of the nodes again. */
    fun forgetNode(origin: String, id: String) = mutating(origin, tr("Nodo olvidado")) { client.forgetNode(origin, id); pollNodeHealth(origin) }

    /** Looks for a place by name (Open-Meteo's geocoding); empty for fewer than two characters. */
    fun searchPlaces(query: String) {
        val ticket = ++placeTicket
        viewModelScope.launch {
            val results = runCatching { withContext(Dispatchers.IO) { weatherClient.search(query) } }.getOrDefault(emptyList())
            if (ticket == placeTicket) _state.update { it.copy(weather = it.weather.copy(results = results)) }   // an answer to an older search is not shown over a newer one
        }
    }
    @Volatile private var placeTicket = 0
    @Volatile private var weatherTicket = 0

    /** Loads the weather of a place and remembers it as the current one. Nothing is asked of ARMOR-SERVER; only this place's coordinates leave the phone. */
    fun loadWeather(place: Place) {
        val ticket = ++weatherTicket
        _state.update { it.copy(weather = it.weather.copy(place = place, loading = true, error = null, results = emptyList())) }
        viewModelScope.launch {
            val outcome = runCatching {
                withContext(Dispatchers.IO) { val forecast = weatherClient.forecast(place); val air = weatherClient.airQuality(place); forecast to air }
            }
            if (ticket != weatherTicket) return@launch   // another place was chosen meanwhile
            outcome.onSuccess { (forecast, air) -> _state.update { it.copy(weather = it.weather.copy(forecast = forecast, air = air, loading = false)) } }
                .onFailure { _state.update { it.copy(weather = it.weather.copy(loading = false, error = tr("No se pudo obtener el tiempo."))) } }
        }
    }

    fun reloadWeather() { _state.value.weather.place?.let(::loadWeather) }
    fun setWeatherPlace(place: Place) = loadWeather(place)
    fun clearWeatherPlace() { _state.update { it.copy(weather = WeatherUiState()) } }

    /** A viewer only looks: what changes something is refused here before it is asked of the server (which refuses it too). */
    fun viewerRefused(): Boolean {
        if (!_state.value.readOnly) return false
        _state.update { it.copy(message = tr("Tu usuario solo puede mirar: no puede cambiar nada.")) }
        return true
    }

    /** Signed in: the role and what the server asks of the phone's lock come from one request to the session. */
    private fun MonitorUiState.signedIn(origin: String): MonitorUiState {
        val facts = runCatching { client.sessionFacts(origin) }.getOrDefault(SessionFacts())
        return copy(authenticated = true, role = facts.role, deviceLock = facts.deviceLock)
    }

    /** Asked before an action that must be confirmed with the lock of the phone; the screen puts the real question here, a test a stand-in. Null: nothing is asked. */
    var sensitiveGate: SensitiveGate? = null

    /** Runs [run] once the lock of the phone allowed it (at once when nothing is asked); a refusal leaves a message and nothing is sent. */
    private fun confirmed(reason: String, denied: (String) -> Unit = { text -> _state.update { it.copy(message = text) } }, run: () -> Unit) {
        val gate = sensitiveGate ?: return run()
        viewModelScope.launch {
            if (gate.allow(reason)) run() else denied(tr("No se ha confirmado con el bloqueo del teléfono: no se ha hecho nada."))
        }
    }

    private fun mutating(origin: String, success: String, operation: suspend () -> Unit) {
        if (viewerRefused()) return
        action(origin, success, operation)
    }

    /** Shows a message on screen. */
    fun say(text: String) { _state.update { it.copy(message = text) } }

    /** The message on screen has been read. */
    fun dismissMessage() { _state.update { it.copy(message = null) } }

    fun refresh(origin: String) = action(origin, "Connection refreshed") {
        val status = client.status(origin)
        val cameras = client.cameraViews(origin)
        _state.update { it.copy(snapshot = status, cameras = cameras) }
    }

    fun login(origin: String, username: String, password: String) = action(origin, "Session established") {
        require(username.isNotBlank() && password.isNotBlank()) { "Enter username and password" }
        try {
            when (val step = client.signIn(origin, username.trim(), password)) {
                is SignIn.NeedsCode -> { _security.update { it.copy(loginChallenge = step.challenge, message = null) }; return@action }
                SignIn.Done -> Unit
            }
        } catch (error: ArmorApiException) {
            val code = error.apiError.code
            if (code == "totp_setup_required") throw ArmorApiException(securityError(code), error.code)
            throw error
        }
        _state.update { it.signedIn(origin) }
        refreshInternal(origin, loadMedia = true)
    }

    // ---- security: the second step of the sign-in, the second factor, the sessions, the proof for critical actions ----
    private val _security = MutableStateFlow(SecurityUiState())
    val security: StateFlow<SecurityUiState> = _security.asStateFlow()
    @Volatile private var retryAfterElevation: (suspend () -> Unit)? = null

    fun cancelCode() { _security.update { it.copy(loginChallenge = null, message = null) } }

    fun submitCode(origin: String, code: String) {
        val challenge = _security.value.loginChallenge ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    client.completeSignIn(origin, challenge, code.trim())
                    _state.update { it.signedIn(origin) }
                    refreshInternal(origin, loadMedia = true)
                }
            }.onSuccess { _security.update { it.copy(loginChallenge = null) }; _state.update { it.copy(loading = false, message = "Session established") } }
                .onFailure { error ->
                    val failure = ((error as? ArmorApiException)?.apiError?.code.orEmpty())
                    if (failure == "challenge_expired") _security.update { it.copy(loginChallenge = null) }
                    _state.update { it.copy(loading = false, message = securityError(failure)) }
                }
        }
    }

    private fun securityCall(origin: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _security.update { it.copy(busy = true, message = null) }
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onFailure { error ->
                    if (error is ElevationRequiredException) { retryAfterElevation = { block() }; _security.update { it.copy(busy = false, elevationNeeded = true, elevationError = null) } }
                    else _security.update { it.copy(busy = false, message = securityError(((error as? ArmorApiException)?.apiError?.code.orEmpty()))) }
                }
                .onSuccess { _security.update { it.copy(busy = false) } }
        }
    }

    fun loadSecurity(origin: String) = securityCall(origin) {
        val totp = client.totpState(origin)
        val sessions = client.mySessions(origin)
        _security.update { it.copy(totp = totp, sessions = sessions) }
        // only an administrator is answered; for anybody else the cards of the VPN simply are not there
        _security.update { it.copy(setup = runCatching { client.remoteSetup(origin) }.getOrNull(), peerConfig = null) }
    }
    fun addVpnDevice(origin: String, name: String, fullTunnel: Boolean) = securityCall(origin) {
        val config = client.vpnAddPeer(origin, name, fullTunnel)
        _security.update { it.copy(peerConfig = config, setup = client.remoteSetup(origin)) }
    }
    fun showVpnDevice(origin: String, name: String) = securityCall(origin) { _security.update { it.copy(peerConfig = client.vpnShowPeer(origin, name)) } }
    fun removeVpnDevice(origin: String, name: String) = securityCall(origin) {
        client.vpnRemovePeer(origin, name)
        _security.update { it.copy(peerConfig = it.peerConfig?.takeUnless { shown -> shown.name == name }, setup = client.remoteSetup(origin)) }
    }
    fun setupVpn(origin: String, endpoint: String, port: Int, lan: String, routeLan: String) = securityCall(origin) { _security.update { it.copy(setup = client.vpnSetup(origin, endpoint, port, lan, routeLan)) } }
    fun closeVpnConfig() { _security.update { it.copy(peerConfig = null) } }
    fun setupDdns(origin: String, provider: String, domain: String, token: String, zone: String, record: String, url: String) = securityCall(origin) { _security.update { it.copy(setup = client.ddnsSetup(origin, provider, domain, token, zone, record, url)) } }
    fun updateDdns(origin: String) = securityCall(origin) { _security.update { it.copy(setup = client.ddnsUpdate(origin)) } }
    fun stopDdns(origin: String) = securityCall(origin) { _security.update { it.copy(setup = client.ddnsRemove(origin)) } }
    fun startSecondFactor(origin: String, password: String) = securityCall(origin) {
        val (secret, uri) = client.totpSetup(origin, password)
        _security.update { it.copy(setupSecret = secret, setupUri = uri, recoveryCodes = emptyList()) }
    }
    fun enableSecondFactor(origin: String, code: String) = securityCall(origin) {
        val codes = client.totpEnable(origin, code)
        _security.update { it.copy(setupSecret = null, setupUri = null, recoveryCodes = codes, totp = client.totpState(origin), sessions = client.mySessions(origin)) }
    }
    fun disableSecondFactor(origin: String, password: String) = securityCall(origin) {
        client.totpDisable(origin, password)
        _security.update { it.copy(recoveryCodes = emptyList(), setupSecret = null, setupUri = null, totp = client.totpState(origin), sessions = client.mySessions(origin)) }
    }
    fun endSession(origin: String, session: SessionInfo) = securityCall(origin) { client.endSession(origin, session.id); _security.update { it.copy(sessions = client.mySessions(origin)) } }
    fun endOtherSessions(origin: String) = securityCall(origin) { client.endOtherSessions(origin); _security.update { it.copy(sessions = client.mySessions(origin)) } }
    fun checkRemote(origin: String) = securityCall(origin) { _security.update { it.copy(remote = client.remoteCheck(origin)) } }

    /** The password (or a code) for the critical action that was refused; when it is right, the action is tried again by itself. */
    fun elevate(origin: String, password: String, code: String) {
        viewModelScope.launch {
            _security.update { it.copy(busy = true, elevationError = null) }
            runCatching { withContext(Dispatchers.IO) { client.elevate(origin, password, code) } }
                .onFailure { error -> _security.update { it.copy(busy = false, elevationError = securityError(((error as? ArmorApiException)?.apiError?.code.orEmpty()))) } }
                .onSuccess {
                    _security.update { it.copy(busy = false, elevationNeeded = false) }
                    val retry = retryAfterElevation; retryAfterElevation = null
                    if (retry != null) action(origin, "Done") { retry() }
                }
        }
    }
    fun dismissElevation() { retryAfterElevation = null; _security.update { it.copy(elevationNeeded = false, elevationError = null) } }

    @Volatile private var pushToken: String? = null

    /** Registers this phone's push token with the server (once signed in); kept to take it back at sign-out. */
    fun registerPush(origin: String, token: String, onRegistered: () -> Unit = {}) {
        pushToken = token
        viewModelScope.launch(Dispatchers.IO) { runCatching { client.registerPush(origin, token, Tr.voiceLanguage()) }.onSuccess { onRegistered() } }
    }

    /** The session cookie as it stands now, for the vault. */
    fun sessionCookie(origin: String): Pair<String, String>? = client.sessionCookie(origin)

    /**
     * Opens the app already signed in when the server still honours the session kept from the last time. Returns false (and shows no message) when there is none or the server
     * refused it; [refused] tells that case from a server that could not be reached, in which the kept session is worth keeping.
     */
    suspend fun tryRestore(origin: String, session: StoredSession?, refused: () -> Unit): Boolean = withContext(Dispatchers.IO) {
        if (session == null || ServerEndpoint.parse(origin) == null) return@withContext false
        client.restoreSessionCookie(origin, session.name, session.value)
        try {
            if (!client.studioSessionActive(origin)) { refused(); return@withContext false }
            _state.update { it.signedIn(origin) }
            refreshInternal(origin, loadMedia = true)
            true
        } catch (error: ArmorApiException) {
            if (error.code == 401 || error.code == 403) refused()
            _state.update { it.copy(authenticated = false) }
            false
        } catch (error: Exception) {
            _state.update { it.copy(authenticated = false) }
            false
        }
    }

    fun restoreSession(origin: String) = action(origin, "Session restored") {
        require(client.studioSessionActive(origin)) { "Sign in to ARMOR-SERVER" }
        _state.update { it.signedIn(origin) }
        refreshInternal(origin, loadMedia = true)
    }

    /** Closes the session on the server when it can, and always forgets it here: with the server down the person is no longer "inside", with the polling stopped and the cookie gone. */
    fun logout(origin: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pushToken?.let { token -> runCatching { client.unregisterPush(origin, token) } }   // a signed-out phone gets no alarms from the server
                runCatching { client.closeStudioSession(origin) }; client.forgetSession(origin)
            }
            _state.update { MonitorUiState(message = "Session closed") }
        }
    }

    fun loadMedia(origin: String) = action(origin, "Evidence library refreshed") {
        require(_state.value.authenticated) { "Sign in first" }
        _state.update { it.copy(media = client.media(origin)) }
    }

    fun snapshot(origin: String, camera: CameraView) = mutating(origin, "Snapshot saved: ${camera.name}") {
        require(_state.value.authenticated) { "Sign in first" }
        client.snapshot(origin, camera.id)
        _state.update { it.copy(media = client.media(origin)) }
    }

    fun toggleRecording(origin: String, camera: CameraView) = mutating(origin, if (_state.value.media.activeCameraIds.contains(camera.id)) "Recording finalized: ${camera.name}" else "Recording started: ${camera.name}") {
        require(_state.value.authenticated) { "Sign in first" }
        val start = !_state.value.media.activeCameraIds.contains(camera.id)
        client.recording(origin, camera.id, start)
        _state.update { it.copy(media = client.media(origin)) }
    }

    // PTZ commands go out strictly in the order they were given (a "stop" must never overtake its move), and
    // they do not flash the loading state or a success message: only a failure is worth showing.
    private val ptzLock = Mutex()

    fun ptz(origin: String, camera: CameraView, command: String) {
        if (viewerRefused()) return
        if (ServerEndpoint.parse(origin) == null || !_state.value.authenticated) return
        viewModelScope.launch {
            ptzLock.withLock {
                runCatching { withContext(Dispatchers.IO) { client.ptz(origin, camera.id, command) } }
                    .onFailure { error -> _state.update { it.copy(message = error.message ?: "PTZ command failed") } }
            }
        }
    }

    fun deleteMedia(origin: String, item: es.electrohobby3d.armor.model.MediaItem) {
        if (viewerRefused()) return
        confirmed(tr("Borrar una grabación")) {
            mutating(origin, "Evidence removed") {
                require(_state.value.authenticated) { "Sign in first" }
                client.deleteMedia(origin, item)
                _state.update { it.copy(media = client.media(origin)) }
            }
        }
    }

    /**
     * One refresh of everything the screens show, plus the alarm check. Called every ten seconds while
     * the app is on screen; a transient failure is ignored and an expired session sends the user back to sign-in.
     */
    suspend fun pollOnce(origin: String, app: Context) = withContext(Dispatchers.IO) {
        try {
            SharedFeed.appRead()   // the watch in the background leaves its turn to the app while the app is reading
            val status = client.status(origin)
            val cameras = client.cameraViews(origin)
            val window = client.history(origin, limit = 50)
            val health = client.cameraStatus(origin).associate { it.id to it.status }
            val tracker = AlarmNotifier.tracker(app)
            val news = freshEvents(tracker.last(origin), window, { after -> client.history(origin, limit = 200, after = after) })
            tracker.claim(origin, news, status.mode == "armed").forEach { AlarmNotifier.post(app, it) }
            // An older server has neither: keep what was there rather than failing the whole pass.
            val alarms = runCatching { client.alarms(origin) }.getOrNull()
            val devices = runCatching { client.devices(origin) }.getOrNull()
            val solar = runCatching { client.solar(origin) }.getOrNull()
            val electrical = runCatching { client.electrical(origin) }.getOrNull()
            val network = runCatching { client.network(origin) }.getOrNull()
            val armingModes = runCatching { client.armingModes(origin) }.getOrNull()
            _state.update { it.copy(
                solar = solar ?: it.solar, electrical = electrical ?: it.electrical, network = network ?: it.network, armingModes = armingModes ?: it.armingModes,
                snapshot = status, cameras = cameras, events = mergeEvents(window, it.events), cameraHealth = health,
                alarms = alarms?.first ?: it.alarms, closedAlarms = alarms?.second ?: it.closedAlarms, devices = devices ?: it.devices, silent = false,
            ) }
            silentPasses = 0
        } catch (error: ArmorApiException) {
            if (error.code == 401) _state.update { MonitorUiState(message = tr("La sesión ha caducado: inicia sesión de nuevo")) }
            else if (error.code == null) noteSilence()
        } catch (_: Exception) {
            // The server may be restarting or the network changing; the next pass tries again.
            noteSilence()
        }
    }

    private var silentPasses = 0

    /** Three passes in a row without an answer of any kind: the screen is told, and it looks for the other address of the house. */
    private fun noteSilence() { silentPasses += 1; if (silentPasses == 3) _state.update { it.copy(silent = true) } }

    /** The check before arming; null when the server cannot answer it (the person may still arm). */
    suspend fun preflight(origin: String): es.electrohobby3d.armor.model.Preflight? = withContext(Dispatchers.IO) { runCatching { client.preflight(origin) }.getOrNull() }

    /** Arm or disarm. The screen asks the person first; a failure shows as a message and changes nothing. */
    fun setMode(origin: String, mode: String, pin: String? = null) {
        if (viewerRefused()) return
        confirmed(if (mode == "armed") tr("Armar el sistema") else tr("Desarmar el sistema")) {
            mutating(origin, if (mode == "armed") tr("Sistema ARMADO") else tr("Sistema DESARMADO")) {
                require(_state.value.authenticated) { "Sign in first" }
                _state.update { it.copy(snapshot = client.setMode(origin, mode, pin)) }
            }
        }
    }

    /**
     * Arms in a mode of the server (Away, Home or Night). Returns null when it was done, else the refusal as a pair of its reason ("no_zones_assigned", "wrong_pin", ... or "failed") and its
     * words, so that the picker can ask for the PIN or for the confirmation and ask again. A phone that asks for its lock before arming is asked first.
     */
    suspend fun armInMode(origin: String, mode: String, pin: String? = null, confirmUnassigned: Boolean = false): Pair<String, String>? {
        if (viewerRefused()) return "viewer" to tr("Tu usuario solo puede mirar: no puede cambiar nada.")
        val gate = sensitiveGate
        if (gate != null && !gate.allow(tr("Armar el sistema"))) return "gate" to tr("No se ha confirmado con el bloqueo del teléfono: no se ha hecho nada.")
        return withContext(Dispatchers.IO) {
            try {
                require(_state.value.authenticated) { "Sign in first" }
                val snapshot = client.setArmingMode(origin, mode, pin, confirmUnassigned)
                val modes = runCatching { client.armingModes(origin) }.getOrNull()
                _state.update { it.copy(snapshot = snapshot, armingModes = modes ?: it.armingModes, message = trf("Sistema armado: %s", es.electrohobby3d.armor.model.ArmingModeText.name(mode))) }
                null
            } catch (error: es.electrohobby3d.armor.model.ArmingModeException) {
                error.reason to error.message.orEmpty()
            } catch (error: ElevationRequiredException) {   // from outside the house: the password again, then the same arming by itself
                retryAfterElevation = { armInMode(origin, mode, pin, confirmUnassigned) }
                _security.update { it.copy(elevationNeeded = true, elevationError = null) }
                "elevation" to ""
            } catch (error: Exception) {
                "failed" to (error.message ?: tr("No se pudo cambiar el modo."))
            }
        }
    }

    fun acknowledge(origin: String, alarm: Alarm) = mutating(origin, tr("Alarma confirmada")) {
        client.acknowledgeAlarm(origin, alarm.id)
        client.alarms(origin).let { pair -> _state.update { it.copy(alarms = pair.first, closedAlarms = pair.second) } }
    }

    fun acknowledgeAll(origin: String) = mutating(origin, tr("Alarmas confirmadas")) {
        client.acknowledgeAllAlarms(origin)
        client.alarms(origin).let { pair -> _state.update { it.copy(alarms = pair.first, closedAlarms = pair.second) } }
    }

    /** "on", "off" or "toggle" to a plug, light, switch, siren, lock or valve. */
    fun command(origin: String, device: SiteDevice, command: String, confirm: Boolean = false) {
        if (viewerRefused()) return
        val send = {
            mutating(origin, trf("%s: orden enviada", device.name)) {
                client.commandDevice(origin, device.id, command, confirm)
                _state.update { it.copy(devices = client.devices(origin)) }
            }
        }
        // a lock, a circuit of the board and a critical device are not a click: they ask for the lock of the phone first
        if (device.kind == "lock" || device.risk != "low") confirmed(trf("Ordenar a %s", device.name)) { send() } else send()
    }

    fun loadOlderEvents(origin: String) = action(origin, tr("Historial ampliado")) {
        val oldest = _state.value.events.lastOrNull()?.id ?: return@action
        val older = client.history(origin, limit = 50, before = oldest)
        _state.update { it.copy(events = it.events + older) }
    }

    fun mjpegUrl(origin: String, camera: CameraView) = client.mjpegUrl(origin, camera.id)

    /**
     * The address to watch a camera at: a stream ticket (the server gives one to every role, and a viewer cannot watch without it), or the plain address when the server cannot give one
     * (an older server, which takes the session cookie).
     */
    suspend fun streamUrl(origin: String, camera: CameraView, quality: String = "auto"): String = withContext(Dispatchers.IO) { runCatching { client.streamTicket(origin, camera.id, quality) }.getOrNull() ?: client.mjpegUrl(origin, camera.id) }
    fun mediaUrl(origin: String, item: es.electrohobby3d.armor.model.MediaItem) = client.mediaUrl(origin, item)

    private fun action(origin: String, success: String, operation: suspend () -> Unit) {
        if (ServerEndpoint.parse(origin) == null) { _state.update { it.copy(message = "Enter a valid HTTPS origin or a private-LAN HTTP origin") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { withContext(Dispatchers.IO) { operation() } }
                .onSuccess { _state.update { it.copy(loading = false, message = success) } }
                .onFailure { error ->
                    if (error is ElevationRequiredException) {   // from outside the house: the password again, then the same action by itself
                        retryAfterElevation = operation
                        _security.update { it.copy(elevationNeeded = true, elevationError = null) }
                        _state.update { it.copy(loading = false) }
                    } else _state.update { it.copy(loading = false, message = error.message ?: "Server operation failed") }
                }   // the error, not the (empty) message of the state: a wrong PIN, a lock and a refusal are told as they are
        }
    }

    // ---- the two addresses of the house: at home and away ----

    /** An answer of any kind from the server (even a refusal) means the address is the right one; only silence (no HTTP status) means it is not. */
    private fun answers(origin: String): Boolean = try { client.sessionFacts(origin); true } catch (error: ArmorApiException) { error.code != null } catch (error: Exception) { false }

    /**
     * Chooses the address to use (the private one at home, the one for outside away, and the other when the chosen one is silent) and reports it. [pinMismatch] tells whether the rules of the
     * certificate refused something meanwhile, so that the message can say the certificate changed instead of "unreachable".
     */
    fun reach(addresses: ServerAddresses, place: NetworkPlace, networkAvailable: Boolean, pinMismatch: () -> Boolean = { TrustPins.takeMismatch() }, done: (Selection) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            val selection = withContext(Dispatchers.IO) { Connector.select(addresses, place, networkAvailable, pinMismatch) { endpoint -> answers(endpoint.origin) } }
            _state.update { it.copy(loading = false, message = (selection as? Selection.Failed)?.problem?.message) }
            done(selection)
        }
    }

    /**
     * The phone changed place and the address in use is not the one for it: the session is the server's, not the address's, so the cookie is copied to the new address and the session is
     * checked there; only if it holds is the new address taken. [done] gets the new origin, or null when the session could not be carried over (the old address stays).
     */
    fun moveTo(old: String, new: String, done: (String?) -> Unit) {
        if (old == new || ServerEndpoint.parse(new) == null) return
        viewModelScope.launch {
            val moved = withContext(Dispatchers.IO) {
                client.sessionCookie(old)?.let { client.restoreSessionCookie(new, it.first, it.second) }
                runCatching { client.studioSessionActive(new) }.getOrDefault(false)
            }
            done(if (moved) new else null)
            if (moved) runCatching { withContext(Dispatchers.IO) { refreshInternal(new, false) } }
        }
    }

    private fun refreshInternal(origin: String, loadMedia: Boolean) {
        val status = client.status(origin)
        val cameras = client.cameraViews(origin)
        val alarms = runCatching { client.alarms(origin) }.getOrNull()
        val devices = runCatching { client.devices(origin) }.getOrNull()
        val solar = runCatching { client.solar(origin) }.getOrNull()
        val electrical = runCatching { client.electrical(origin) }.getOrNull()
        val network = runCatching { client.network(origin) }.getOrNull()
        _state.update { it.copy(solar = solar, electrical = electrical, network = network, snapshot = status, cameras = cameras, alarms = alarms?.first ?: emptyList(), closedAlarms = alarms?.second ?: emptyList(), devices = devices ?: emptyList(), media = if (loadMedia) client.media(origin) else it.media) }
    }
}
