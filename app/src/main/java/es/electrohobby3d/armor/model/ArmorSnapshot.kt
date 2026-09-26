// ARMOR-ANDROID-CONTROL — server state models.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

data class FieldNode(val id: String, val online: Boolean, val lux: Double?, val tracks: Int, val alert: String, val stale: Boolean = false, val targets: List<LiveTarget> = emptyList())
data class ArmorSnapshot(
    val mode: String = "disarmed",
    val revision: Long = 0,
    val updatedAt: String = "",
    val nodes: List<FieldNode> = emptyList(),
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
) {
    val configured: Boolean get() = host.isNotBlank() && (hasCredentials || rtspPath.isNotBlank() || snapshotUrl.isNotBlank())
}

data class MediaItem(val id: String, val cameraId: String, val kind: String, val file: String, val createdAt: String, val bytes: Long)
data class MediaCatalogue(val items: List<MediaItem> = emptyList(), val activeCameraIds: Set<String> = emptySet())
