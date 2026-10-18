// ARMOR-ANDROID-CONTROL - the devices and apps that connect to the server, as the server's history lists them. Reading only.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * One client the server has seen: Studio in a browser, this app, the panel of a node, the voice service or anything else. [kind] is "studio", "android", "node", "voice" or "other".
 * [ip] and [agent] come only when the signed-in user is an administrator; no password, token or cookie is ever part of it.
 */
data class ConnectionClient(
    val id: String,
    val kind: String,
    val user: String?,
    val label: String?,
    val appVersion: String?,
    val firstSeen: String,
    val lastSeen: String,
    val activeSessions: Int,
    val loginsOk: Int,
    val loginsFailed: Int,
    val ip: String? = null,
    val agent: String? = null,
)

object ConnectionsParser {
    private val KINDS = setOf("studio", "android", "node", "voice", "other")

    private fun JSONObject.text(key: String): String? = if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    /** A client from the server's JSON, or null when it is not a well-formed one (never trusted blindly). */
    fun client(json: JSONObject): ConnectionClient? {
        val id = json.optStr("id")
        val kind = json.optStr("kind")
        if (!Regex("[0-9a-f]{16}").matches(id) || kind !in KINDS) return null
        return ConnectionClient(
            id = id, kind = kind, user = json.text("user"), label = json.text("label"), appVersion = json.text("app_version"),
            firstSeen = json.optStr("first_seen"), lastSeen = json.optStr("last_seen"),
            activeSessions = json.optInt("active_sessions", 0).coerceAtLeast(0), loginsOk = json.optInt("logins_ok", 0).coerceAtLeast(0), loginsFailed = json.optInt("logins_failed", 0).coerceAtLeast(0),
            ip = json.text("ip"), agent = json.text("agent"),
        )
    }

    /** The list of the answer `{ "clients": [...] }`; what is not a client is left out. */
    fun list(json: JSONObject): List<ConnectionClient> {
        val array: JSONArray = json.optJSONArray("clients") ?: return emptyList()
        return buildList { for (index in 0 until array.length()) array.optJSONObject(index)?.let(::client)?.let(::add) }
    }
}
