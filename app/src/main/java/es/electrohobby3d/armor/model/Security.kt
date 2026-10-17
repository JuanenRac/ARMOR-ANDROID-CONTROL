// ARMOR-ANDROID-CONTROL - what the server says about the security of the account: the second factor, the open sessions and how it sees this phone.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

data class SessionInfo(val id: String, val createdMs: Long, val lastSeenMs: Long, val remote: Boolean, val ip: String, val agent: String, val current: Boolean)
data class TotpInfo(val enabled: Boolean, val recoveryLeft: Int, val policy: String)
data class RemoteInfo(val ip: String, val remote: Boolean, val cookieSecure: Boolean, val trustProxy: String, val policy: String)

data class VpnPeer(val name: String, val address: String)
data class PeerConfig(val name: String, val config: String)
/** The way in from outside the house as the maintenance helper reports it (never a key or a token). */
data class RemoteSetup(
    val vpnScript: Boolean, val vpnTool: Boolean, val vpnConfigured: Boolean, val vpnRunning: Boolean, val vpnEndpoint: String, val vpnSubnet: String, val vpnPort: String, val peers: List<VpnPeer>,
    val ddnsScript: Boolean, val ddnsConfigured: Boolean, val ddnsProvider: String, val ddnsName: String, val ddnsLast: String,
)

object SecurityParser {
    fun sessions(root: JSONObject): List<SessionInfo> {
        val list: JSONArray = root.optJSONArray("sessions") ?: return emptyList()
        return (0 until list.length()).mapNotNull { index ->
            val item = list.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            if (id.isBlank()) null
            else SessionInfo(id, item.optLong("created"), item.optLong("last_seen"), item.optString("origin") == "remote", item.optString("ip"), item.optString("agent"), item.optBoolean("current"))
        }
    }

    fun totp(root: JSONObject) = TotpInfo(root.optBoolean("enabled"), root.optInt("recovery_left"), root.optString("policy", "off"))

    fun remote(root: JSONObject) = RemoteInfo(
        root.optString("ip"), root.optString("origin") == "remote", root.optBoolean("cookie_secure"),
        root.opt("trust_proxy").let { value -> if (value == null || value == false || value == JSONObject.NULL) "-" else value.toString() },
        root.optString("totp_policy", "off"),
    )

    fun remoteSetup(root: JSONObject): RemoteSetup {
        val vpn = root.optJSONObject("wireguard") ?: JSONObject()
        val ddns = root.optJSONObject("ddns") ?: JSONObject()
        val list = vpn.optJSONArray("peers")
        val peers = if (list == null) emptyList() else (0 until list.length()).mapNotNull { index ->
            val item = list.optJSONObject(index) ?: return@mapNotNull null
            if (item.optString("name").isBlank()) null else VpnPeer(item.optString("name"), item.optString("address"))
        }
        return RemoteSetup(
            vpn.optBoolean("script"), vpn.optBoolean("tool"), vpn.optBoolean("configured"), vpn.optBoolean("running"), vpn.optString("endpoint"), vpn.optString("subnet"), vpn.optString("port"), peers,
            ddns.optBoolean("script"), ddns.optBoolean("configured"), ddns.optString("provider"), ddns.optString("name"), ddns.optString("last_address"),
        )
    }

    fun peerConfig(root: JSONObject) = PeerConfig(root.optString("name"), root.optString("config"))

    /** The `code` the server gave in an error answer ("bad_proof", "elevation_required"...), or empty. */
    fun errorCode(detail: String?): String = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"").find(detail.orEmpty())?.groupValues?.get(1).orEmpty()

    /** The strings of a list of recovery codes. */
    fun codes(root: JSONObject): List<String> {
        val list = root.optJSONArray("recovery_codes") ?: return emptyList()
        return (0 until list.length()).map { list.optString(it) }.filter { it.isNotBlank() }
    }
}
