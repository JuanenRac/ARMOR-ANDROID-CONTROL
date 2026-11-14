// ARMOR-ANDROID-CONTROL - the text of the QR code that sets up the app (made by Studio): the address of the installation, the fingerprint of its local authority and a code of one use that
// lives five minutes. It holds no password and opens no session: the person still types their user and password.
//   armor-enrol:{"ca":"<64 hex or empty>","code":"<base64url>","origin":"https://host[:port]","v":1}
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject
import java.net.URI

/** What the code holds; [caFingerprint] is empty when the server names no authority. */
data class Enrolment(val origin: String, val code: String, val caFingerprint: String)

object EnrolmentCode {
    const val PREFIX = "armor-enrol"
    const val VERSION = 1
    private const val MAX_TEXT = 300
    private val code = Regex("^[A-Za-z0-9_-]{16,64}$")
    private val fingerprint = Regex("^[0-9a-f]{64}$")

    /** The https address as the code holds it (scheme, host and port only), or null when it is anything else. */
    fun cleanOrigin(value: String): String? {
        if (value.isEmpty() || value.length > 200 || value.any { it.code < 33 || it.code == 127 }) return null
        val uri = try { URI(value) } catch (_: Exception) { return null }
        if (uri.scheme != "https" || uri.host.isNullOrEmpty() || uri.userInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        if (!uri.rawPath.isNullOrEmpty() && uri.rawPath != "/") return null
        if (uri.port != -1 && uri.port !in 1..65535) return null
        return "https://${uri.host}" + if (uri.port == -1) "" else ":${uri.port}"
    }

    /** Reads the text of a scanned code; null when it is not exactly what Studio writes. */
    fun parse(text: String?): Enrolment? {
        if (text == null || !text.startsWith("$PREFIX:") || text.length > MAX_TEXT) return null
        val body = try { JSONObject(text.substring(PREFIX.length + 1)) } catch (_: Exception) { return null }
        if (body.keys().asSequence().toSet() != setOf("ca", "code", "origin", "v")) return null
        if (body.opt("v") != VERSION) return null
        val origin = cleanOrigin(body.opt("origin") as? String ?: return null) ?: return null
        val one = body.opt("code") as? String ?: return null
        val ca = body.opt("ca") as? String ?: return null
        if (!code.matches(one) || (ca.isNotEmpty() && !fingerprint.matches(ca))) return null
        return Enrolment(origin, one, ca)
    }
}
