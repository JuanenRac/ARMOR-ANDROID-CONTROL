// ARMOR-ANDROID-CONTROL — server state models.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

data class FieldNode(val id: String, val online: Boolean, val lux: Double?, val tracks: Int, val alert: String, val stale: Boolean = false, val targets: List<LiveTarget> = emptyList())
data class ArmorSnapshot(
    val mode: String = "disarmed",
    val revision: Long = 0,
    val updatedAt: String = "",
    val nodes: List<FieldNode> = emptyList(),
    /** The arming mode in force ("disarmed", "away", "home", "night") while the server has the arming modes on; null otherwise. */
    val armingMode: String? = null,
)

/** Deliberately excludes camera passwords: only ARMOR-SERVER holds them. */
data class CameraView(
    val id: String,
    val name: String,
    val host: String,
    val snapshotUrl: String,
    val rtspPath: String,
    val onvifPort: Int,
    val rtspPort: Int,
    val hasCredentials: Boolean,
    val liveVideoAvailable: Boolean,
    /** What the camera can do, as the operator noted it in Studio: null when it never said (every button is shown, as before). */
    val ptz: Boolean? = null,
    val zoom: Boolean? = null,
    /** "split": the stream carries two lenses, one above the other, and the app shows them in two windows. */
    val layout: String? = null,
    /** The name of the lens when the camera is one lens of a body with several, and the address of the camera's own web page. */
    val lensName: String? = null,
    val webUrl: String? = null,
    /** Which half of the picture this window shows (a window of a camera with two lenses in one stream; see [withLensWindows]). */
    val lens: es.electrohobby3d.armor.LensHalf = es.electrohobby3d.armor.LensHalf.WHOLE,
    /** The real camera behind a window (the identifier the server knows). */
    val sourceId: String? = null,
) {
    val configured: Boolean get() = host.isNotBlank() && (hasCredentials || rtspPath.isNotBlank() || snapshotUrl.isNotBlank())

    /** Whether the lens turns, and whether it zooms: a camera that never said keeps both. */
    val canPanTilt: Boolean get() = ptz != false
    val canZoom: Boolean get() = zoom != false

    /** A fixed camera with no zoom has nothing to move, so its movement button is not drawn at all. */
    val hasMovement: Boolean get() = canPanTilt || canZoom
}

data class MediaItem(val id: String, val cameraId: String, val kind: String, val file: String, val createdAt: String, val bytes: Long)
data class MediaCatalogue(val items: List<MediaItem> = emptyList(), val activeCameraIds: Set<String> = emptySet())
