// ARMOR-ANDROID-CONTROL - the check before arming: what would leave the perimeter blind or noisy, as the server tells it, and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.trf
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
        "node_offline" -> trf("El nodo de radar %s no responde.", issue.id)
        "panel_silent" -> trf("La central de alarma %s está en silencio.", issue.id)
        "zone_open" -> trf("La zona %s de la central %s está abierta.", issue.detail.orEmpty(), issue.id)
        "camera_down" -> trf("La cámara %s no responde.", issue.id)
        "opening_open" -> trf("La puerta o ventana %s está abierta.", issue.id)
        "device_offline" -> trf("El dispositivo %s dejó de responder.", issue.id)
        "alarm_unseen" -> trf("Hay una alarma que nadie ha visto (%s).", issue.id)
        else -> issue.code
    }
}
