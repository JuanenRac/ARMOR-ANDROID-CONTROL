// ARMOR-ANDROID-CONTROL - event history models and parsing.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

/**
 * One line of the server's event history: an alert-level, node-status, camera
 * or security-mode change. [subject] is the node or camera id ("" for a mode change).
 */
data class ArmorEvent(
    val id: Long,
    val at: String,
    val type: String,
    val subject: String,
    val from: String?,
    val to: String?,
    val targets: Int?,
    val mode: String?,
)

/** Reachability of a camera as seen by the server's watchdog: "unknown", "online" or "offline". */
data class CameraHealth(val id: String, val status: String)

object EventParser {
    private val KNOWN = setOf("alert", "node", "camera", "mode")

    /** An event from the server's JSON, or null when it is not a well-formed known event (never trusted blindly). */
    fun parse(json: JSONObject): ArmorEvent? {
        val id = json.optLong("id", -1)
        val type = json.optString("type")
        if (id < 1 || type !in KNOWN) return null
        val subject = when (type) {
            "node", "alert" -> json.optString("node_id")
            "camera" -> json.optString("camera_id")
            else -> ""
        }
        if (type != "mode" && subject.isBlank()) return null
        fun text(key: String): String? = if (json.isNull(key) || !json.has(key)) null else json.optString(key).ifBlank { null }
        return ArmorEvent(
            id = id, at = json.optString("at"), type = type, subject = subject,
            from = text("from"), to = text("to"),
            targets = if (json.has("targets")) json.optInt("targets") else null,
            mode = text("mode"),
        )
    }
}
