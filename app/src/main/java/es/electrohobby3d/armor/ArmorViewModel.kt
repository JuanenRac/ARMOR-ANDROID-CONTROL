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
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class MonitorUiState(
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
    /** The site as Studio designed it, for the radar screen; [siteLoaded] tells "not asked yet" from "no design saved". */
    val site: es.electrohobby3d.armor.model.SiteDesign? = null,
    val siteLoaded: Boolean = false,
    val authenticated: Boolean = false,
    val message: String? = null,
)

class ArmorViewModel(private val client: ArmorApiClient = ArmorApiClient()) : ViewModel() {
    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    /** The site design for the radar screen (once, and again when asked). A server that predates it, or has no design, leaves it empty. */
    suspend fun loadSite(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.site(origin) }.onSuccess { _state.value = _state.value.copy(site = it, siteLoaded = true) }.onFailure { _state.value = _state.value.copy(siteLoaded = true) }
    }

    /** Only the nodes' state, quickly, for the live radar: the targets move every second. A failure is ignored; the next pass tries again. */
    suspend fun pollRadar(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.status(origin) }.onSuccess { _state.value = _state.value.copy(snapshot = it) }
    }

    /** The solar equipment, quickly, while the Solar screen is open. A failure is ignored; the next pass tries again. */
    suspend fun pollSolar(origin: String) = withContext(Dispatchers.IO) {
        runCatching { client.solar(origin) }.onSuccess { _state.value = _state.value.copy(solar = it) }
    }

    fun reloadSolar(origin: String) { viewModelScope.launch { pollSolar(origin) } }

    /** The message on screen has been read. */
    fun dismissMessage() { _state.value = _state.value.copy(message = null) }

    fun refresh(origin: String) = action(origin, "Connection refreshed") {
        val status = client.status(origin)
        val cameras = client.cameraViews(origin)
        _state.value = _state.value.copy(snapshot = status, cameras = cameras)
    }

    fun login(origin: String, username: String, password: String) = action(origin, "Session established") {
        require(username.isNotBlank() && password.isNotBlank()) { "Enter username and password" }
        client.openStudioSession(origin, username.trim(), password)
        _state.value = _state.value.copy(authenticated = true)
        refreshInternal(origin, loadMedia = true)
    }

    fun restoreSession(origin: String) = action(origin, "Session restored") {
        require(client.studioSessionActive(origin)) { "Sign in to ARMOR-SERVER" }
        _state.value = _state.value.copy(authenticated = true)
        refreshInternal(origin, loadMedia = true)
    }

    fun logout(origin: String) = action(origin, "Session closed") {
        client.closeStudioSession(origin)
        _state.value = MonitorUiState()
    }

    fun loadMedia(origin: String) = action(origin, "Evidence library refreshed") {
        require(_state.value.authenticated) { "Sign in first" }
        _state.value = _state.value.copy(media = client.media(origin))
    }

    fun snapshot(origin: String, camera: CameraView) = action(origin, "Snapshot saved: ${camera.name}") {
        require(_state.value.authenticated) { "Sign in first" }
        client.snapshot(origin, camera.id)
        _state.value = _state.value.copy(media = client.media(origin))
    }

    fun toggleRecording(origin: String, camera: CameraView) = action(origin, if (_state.value.media.activeCameraIds.contains(camera.id)) "Recording finalized: ${camera.name}" else "Recording started: ${camera.name}") {
        require(_state.value.authenticated) { "Sign in first" }
        val start = !_state.value.media.activeCameraIds.contains(camera.id)
        client.recording(origin, camera.id, start)
        _state.value = _state.value.copy(media = client.media(origin))
    }

    // PTZ commands go out strictly in the order they were given (a "stop" must never overtake its move), and
    // they do not flash the loading state or a success message: only a failure is worth showing.
    private val ptzLock = Mutex()

    fun ptz(origin: String, camera: CameraView, command: String) {
        if (ServerEndpoint.parse(origin) == null || !_state.value.authenticated) return
        viewModelScope.launch {
            ptzLock.withLock {
                runCatching { withContext(Dispatchers.IO) { client.ptz(origin, camera.id, command) } }
                    .onFailure { _state.value = _state.value.copy(message = it.message ?: "PTZ command failed") }
            }
        }
    }

    fun deleteMedia(origin: String, item: es.electrohobby3d.armor.model.MediaItem) = action(origin, "Evidence removed") {
        require(_state.value.authenticated) { "Sign in first" }
        client.deleteMedia(origin, item)
        _state.value = _state.value.copy(media = client.media(origin))
    }

    /**
     * One refresh of everything the screens show, plus the alarm check. Called every ten seconds while
     * the app is on screen; a transient failure is ignored and an expired session sends the user back to sign-in.
     */
    suspend fun pollOnce(origin: String, app: Context) = withContext(Dispatchers.IO) {
        try {
            val status = client.status(origin)
            val cameras = client.cameraViews(origin)
            val events = client.history(origin, limit = 50)
            val health = client.cameraStatus(origin).associate { it.id to it.status }
            AlarmNotifier.tracker(app).claim(events, status.mode == "armed").forEach { AlarmNotifier.post(app, it) }
            // An older server has neither: keep what was there rather than failing the whole pass.
            val alarms = runCatching { client.alarms(origin) }.getOrNull()
            val devices = runCatching { client.devices(origin) }.getOrNull()
            val solar = runCatching { client.solar(origin) }.getOrNull()
            _state.value = _state.value.copy(
                solar = solar ?: _state.value.solar,
                snapshot = status, cameras = cameras, events = events, cameraHealth = health,
                alarms = alarms?.first ?: _state.value.alarms, closedAlarms = alarms?.second ?: _state.value.closedAlarms, devices = devices ?: _state.value.devices,
            )
        } catch (error: ArmorApiException) {
            if (error.code == 401) _state.value = MonitorUiState(message = "La sesión ha caducado: inicia sesión de nuevo")
        } catch (_: Exception) {
            // The server may be restarting or the network changing; the next pass tries again.
        }
    }

    /** Arm or disarm. The screen asks the person first; a failure shows as a message and changes nothing. */
    fun setMode(origin: String, mode: String) = action(origin, if (mode == "armed") "Sistema ARMADO" else "Sistema DESARMADO") {
        require(_state.value.authenticated) { "Sign in first" }
        _state.value = _state.value.copy(snapshot = client.setMode(origin, mode))
    }

    fun acknowledge(origin: String, alarm: Alarm) = action(origin, "Alarma confirmada") {
        client.acknowledgeAlarm(origin, alarm.id)
        client.alarms(origin).let { _state.value = _state.value.copy(alarms = it.first, closedAlarms = it.second) }
    }

    fun acknowledgeAll(origin: String) = action(origin, "Alarmas confirmadas") {
        client.acknowledgeAllAlarms(origin)
        client.alarms(origin).let { _state.value = _state.value.copy(alarms = it.first, closedAlarms = it.second) }
    }

    /** "on", "off" or "toggle" to a plug, light, switch, siren, lock or valve. */
    fun command(origin: String, device: SiteDevice, command: String) = action(origin, "${device.name}: orden enviada") {
        client.commandDevice(origin, device.id, command)
        _state.value = _state.value.copy(devices = client.devices(origin))
    }

    fun loadOlderEvents(origin: String) = action(origin, "Historial ampliado") {
        val oldest = _state.value.events.lastOrNull()?.id ?: return@action
        val older = client.history(origin, limit = 50, before = oldest)
        _state.value = _state.value.copy(events = _state.value.events + older)
    }

    fun mjpegUrl(origin: String, camera: CameraView) = client.mjpegUrl(origin, camera.id)
    fun mediaUrl(origin: String, item: es.electrohobby3d.armor.model.MediaItem) = client.mediaUrl(origin, item)

    private fun action(origin: String, success: String, operation: suspend () -> Unit) {
        if (ServerEndpoint.parse(origin) == null) { _state.value = _state.value.copy(message = "Enter a valid HTTPS origin or a private-LAN HTTP origin"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            runCatching { withContext(Dispatchers.IO) { operation() } }
                .onSuccess { _state.value = _state.value.copy(loading = false, message = success) }
                .onFailure { _state.value = _state.value.copy(loading = false, message = it.message ?: "Server operation failed") }
        }
    }

    private fun refreshInternal(origin: String, loadMedia: Boolean) {
        val status = client.status(origin)
        val cameras = client.cameraViews(origin)
        val alarms = runCatching { client.alarms(origin) }.getOrNull()
        val devices = runCatching { client.devices(origin) }.getOrNull()
        val solar = runCatching { client.solar(origin) }.getOrNull()
        _state.value = _state.value.copy(solar = solar, snapshot = status, cameras = cameras, alarms = alarms?.first ?: emptyList(), closedAlarms = alarms?.second ?: emptyList(), devices = devices ?: emptyList(), media = if (loadMedia) client.media(origin) else _state.value.media)
    }
}
