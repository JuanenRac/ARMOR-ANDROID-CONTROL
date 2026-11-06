// ARMOR-ANDROID-CONTROL - the text of the pairing QR code: the id of a node and the address of the server, nothing else (the same text the shared Python library writes).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject
import java.net.URI

/** What a pairing code holds. */
data class Pairing(val nodeId: String, val server: String)

/** The code holds no password, no token and no certificate; reading it refuses anything that could hold a secret. */
object PairingCode {
    const val PREFIX = "armor-provision"
    const val VERSION = 1
    private const val MAX_TEXT = 256
    private val nodeId = Regex("^[a-z0-9][a-z0-9_-]{0,63}$")

    fun validNodeId(text: String): Boolean = nodeId.matches(text)

    /** The server address as the code holds it (no trailing slash), or null when it is not an address without a secret in it. */
    fun cleanServer(server: String): String? {
        if (server.isEmpty() || server.length > MAX_TEXT) return null
        if (server.any { it.code < 33 || it.code == 127 }) return null
        if ('?' in server || '#' in server || '@' in server) return null
        val uri = try { URI(server) } catch (_: Exception) { return null }
        if (uri.scheme != "http" && uri.scheme != "https") return null
        if (uri.host.isNullOrEmpty() || uri.userInfo != null) return null
        if (!uri.rawPath.isNullOrEmpty() && uri.rawPath != "/") return null
        if (uri.port != -1 && uri.port !in 1..65535) return null
        return server.trimEnd('/')
    }

    /** The text to turn into a QR code (keys in alphabetical order, compact), or null when the id or the server are not valid. */
    fun build(nodeId: String, server: String): String? {
        val clean = cleanServer(server)
        if (!validNodeId(nodeId) || clean == null) return null
        // Written by hand in the order the shared library uses (org.json does not promise an order of the keys and escapes slashes); the id and the address cannot hold a quote or a backslash.
        return "$PREFIX:{\"node_id\":\"$nodeId\",\"server\":\"$clean\",\"v\":$VERSION}"
    }

    /** Reads the text of a scanned code; null when it is not exactly what [build] writes. */
    fun parse(text: String?): Pairing? {
        if (text == null || !text.startsWith("$PREFIX:") || text.length > 2 * MAX_TEXT) return null
        val body = try { JSONObject(text.substring(PREFIX.length + 1)) } catch (_: Exception) { return null }
        if (body.keys().asSequence().toSet() != setOf("v", "node_id", "server")) return null
        if (body.opt("v") != VERSION) return null
        val id = body.opt("node_id") as? String ?: return null
        val server = body.opt("server") as? String ?: return null
        val clean = cleanServer(server) ?: return null
        if (!validNodeId(id)) return null
        return Pairing(id, clean)
    }
}
