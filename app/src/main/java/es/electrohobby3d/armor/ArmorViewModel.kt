// ARMOR-ANDROID-CONTROL - lifecycle-aware operator and monitoring state.
package es.electrohobby3d.armor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.electrohobby3d.armor.model.ArmorSnapshot
import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.model.MediaCatalogue
import es.electrohobby3d.armor.network.ArmorApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MonitorUiState(
    val loading: Boolean = false,
    val snapshot: ArmorSnapshot = ArmorSnapshot(),
    val cameras: List<CameraView> = emptyList(),
    val media: MediaCatalogue = MediaCatalogue(),
    val authenticated: Boolean = false,
    val message: String? = null,
)

class ArmorViewModel(private val client: ArmorApiClient = ArmorApiClient()) : ViewModel() {
    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

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

    fun ptz(origin: String, camera: CameraView, command: String) = action(origin, "PTZ command sent: $command") {
        require(_state.value.authenticated) { "Sign in first" }
        client.ptz(origin, camera.id, command)
    }

    fun deleteMedia(origin: String, item: es.electrohobby3d.armor.model.MediaItem) = action(origin, "Evidence removed") {
        require(_state.value.authenticated) { "Sign in first" }
        client.deleteMedia(origin, item)
        _state.value = _state.value.copy(media = client.media(origin))
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
        _state.value = _state.value.copy(snapshot = status, cameras = cameras, media = if (loadMedia) client.media(origin) else _state.value.media)
    }
}
