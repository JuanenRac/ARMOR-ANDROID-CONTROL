// ARMOR-ANDROID-CONTROL - the servers whose session is kept on the phone, as the sign-in offers them: how one is written on a button and how it fills the address fields.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import java.net.URI

object ServerChoice {
    /** How a server reads on its button: the name or address with its port, without the scheme (a server over https has a lock mark). */
    fun label(origin: String): String {
        val uri = runCatching { URI(origin) }.getOrNull() ?: return origin
        val host = uri.host ?: return origin
        val name = if (uri.port == -1) host else "$host:${uri.port}"
        return if (uri.scheme == "https") "🔒 $name" else name
    }

    /** The two address fields (host, with `https://` in front when it asks for it, and port) that name this server, or null when the origin is not one the app accepts. */
    fun fields(origin: String): Pair<String, String>? {
        val endpoint = ServerEndpoint.parse(origin) ?: return null
        val uri = runCatching { URI(endpoint.origin) }.getOrNull() ?: return null
        val secure = uri.scheme == "https"
        val port = if (uri.port in 1..65535) uri.port else if (secure) 443 else 8080
        return ((if (secure) "https://" else "") + uri.host) to port.toString()
    }

    /** The servers to offer: those with a session kept that the app still accepts, each once, with the one in use first. */
    fun offered(kept: List<String>, inUse: String): List<String> {
        val valid = kept.mapNotNull { ServerEndpoint.parse(it)?.origin }.distinct()
        return if (inUse in valid) listOf(inUse) + valid.filter { it != inUse } else valid
    }
}
