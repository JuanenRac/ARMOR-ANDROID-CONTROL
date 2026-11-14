// ARMOR-ANDROID-CONTROL - reads, once, the public facts of an installation with the code of a set-up QR (GET /api/v1/enrolment/{code}): its address and the fingerprint of its local authority.
// Nothing secret travels either way; the code works once for five minutes.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.network

import es.electrohobby3d.armor.TrustPins
import es.electrohobby3d.armor.model.Enrolment
import es.electrohobby3d.armor.model.EnrolmentCode
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object EnrolmentReader {
    /** What the server says it is, as the code must agree: the address and the fingerprint ("" when it names no authority). */
    data class Facts(val origin: String, val caFingerprint: String)

    /** The facts in an answer of the server, or null when it is not one. */
    fun parseFacts(text: String): Facts? {
        val body = try { JSONObject(text) } catch (_: Exception) { return null }
        val origin = EnrolmentCode.cleanOrigin(body.opt("origin") as? String ?: return null) ?: return null
        val ca = if (body.isNull("ca_fingerprint")) "" else body.opt("ca_fingerprint") as? String ?: return null
        return Facts(origin, ca)
    }

    /** True when what the server says is what the code says: the same address and the same fingerprint. */
    fun agrees(code: Enrolment, facts: Facts): Boolean = facts.origin == code.origin && facts.caFingerprint == code.caFingerprint

    /** Asks the server of the code for its facts; null when the code is unknown, used or ended (404). Blocking: call it off the main thread. */
    fun read(code: Enrolment): Facts? {
        val connection = (URL(code.origin + "/api/v1/enrolment/" + code.code).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8_000; readTimeout = 12_000; instanceFollowRedirects = false
            TrustPins.apply(this, url.host)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            if (status == 404) return null
            if (status != 200) throw ArmorApiException("Server operation failed (HTTP $status)", status)
            return parseFacts(BoundedRead.text(connection.inputStream))
        } finally { connection.disconnect() }
    }
}
