// ARMOR-ANDROID-CONTROL - the check before arming: what would leave the perimeter blind or noisy, as the server tells it, and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

/** One finding: `level` is "block" (it should stop the person) or "warn" (only to know); `code` says what, `id` where, `detail` the zone of a panel. */
data class PreflightIssue(val kind: String, val level: String, val code: String, val id: String, val detail: String?) {
    val blocks: Boolean get() = level == "block"
}

data class Preflight(val ready: Boolean, val issues: List<PreflightIssue>) {
    val blocking: Int get() = issues.count { it.blocks }
}

object PreflightParser {
    fun parse(root: JSONObject): Preflight {
        val array = root.optJSONArray("issues")
        val issues = (0 until (array?.length() ?: 0)).mapNotNull { index ->
            val item = array?.optJSONObject(index) ?: return@mapNotNull null
            val code = item.optStr("code")
            if (code.isBlank()) null else PreflightIssue(item.optStr("kind"), item.optStr("level", "warn"), code, item.optStr("id"), item.optStr("detail").ifBlank { null })
        }
        return Preflight(root.optBoolean("ready", false) && issues.none { it.blocks }, issues)
    }
}

object PreflightText {
    fun describe(issue: PreflightIssue): String = when (issue.code) {
        "node_offline" -> "El nodo de radar ${issue.id} no responde."
        "panel_silent" -> "La central de alarma ${issue.id} está en silencio."
        "zone_open" -> "La zona ${issue.detail.orEmpty()} de la central ${issue.id} está abierta."
        "camera_down" -> "La cámara ${issue.id} no responde."
        "opening_open" -> "La puerta o ventana ${issue.id} está abierta."
        "device_offline" -> "El dispositivo ${issue.id} dejó de responder."
        "alarm_unseen" -> "Hay una alarma que nadie ha visto (${issue.id})."
        else -> issue.code
    }
}
