package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.SecurityParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the server says about the account's security is read as it is sent, and every answer it can give has its own phrase. */
class SecurityTest {
    @Test fun theSessionsOfThePersonAreRead() {
        val root = JSONObject("""{"sessions":[{"id":"abcdef0123456789","created":1000,"last_seen":2000,"origin":"remote","ip":"203.0.113.9","agent":"Pixel","current":true},{"id":"","origin":"local"},{"id":"0123456789abcdef","created":5,"last_seen":6,"origin":"local","ip":"","agent":"","current":false}]}""")
        val sessions = SecurityParser.sessions(root)
        assertEquals(2, sessions.size)
        assertTrue(sessions[0].remote && sessions[0].current)
        assertEquals("203.0.113.9", sessions[0].ip)
        assertFalse(sessions[1].remote)
        assertEquals(emptyList<Any>(), SecurityParser.sessions(JSONObject("{}")))
    }

    @Test fun theSecondFactorAndTheViewFromOutsideAreRead() {
        val totp = SecurityParser.totp(JSONObject("""{"enabled":true,"recovery_left":8,"policy":"remote"}"""))
        assertTrue(totp.enabled)
        assertEquals(8, totp.recoveryLeft)
        val remote = SecurityParser.remote(JSONObject("""{"ip":"203.0.113.9","origin":"remote","cookie_secure":true,"trust_proxy":1,"totp_policy":"always"}"""))
        assertTrue(remote.remote && remote.cookieSecure)
        assertEquals("1", remote.trustProxy)
        assertEquals("-", SecurityParser.remote(JSONObject("""{"ip":"10.0.0.2","origin":"local","trust_proxy":false}""")).trustProxy)
        assertEquals(listOf("aaaaa-bbbbb", "ccccc-ddddd"), SecurityParser.codes(JSONObject("""{"recovery_codes":["aaaaa-bbbbb","","ccccc-ddddd"]}""")))
    }

    @Test fun theCodeOfAnErrorIsFoundInTheDetail() {
        assertEquals("elevation_required", SecurityParser.errorCode("""Server operation failed (HTTP 403): {"error":"this needs the password","code":"elevation_required"}"""))
        assertEquals("bad_code", SecurityParser.errorCode("""{"error":"x","code":"bad_code"}"""))
        assertEquals("", SecurityParser.errorCode(null))
    }

    @Test fun everyAnswerThatCanBeGivenHasItsOwnPhrase() {
        val codes = listOf("bad_proof", "bad_code", "totp_locked", "challenge_expired", "remote_policy", "already_enabled", "totp_setup_required")
        val phrases = codes.map { securityError(it) }
        assertEquals(codes.size, phrases.toSet().size)
        for (phrase in phrases) assertNotEquals(securityError("something_else"), phrase)
    }

    @Test fun theVpnAndTheDynamicNameAreReadWithoutAKey() {
        val root = JSONObject("""{"wireguard":{"tool":true,"script":true,"configured":true,"running":true,"qr":false,"endpoint":"casa.example.org:51820","subnet":"10.66.0.0/24","port":"51820","peers":[{"name":"phone-ana","address":"10.66.0.2"},{"name":"","address":"x"}]},"ddns":{"script":true,"configured":true,"provider":"duckdns","name":"casa","last_address":"203.0.113.9"}}""")
        val setup = SecurityParser.remoteSetup(root)
        assertTrue(setup.vpnConfigured && setup.vpnRunning && setup.vpnTool)
        assertEquals("casa.example.org:51820", setup.vpnEndpoint)
        assertEquals(listOf("phone-ana"), setup.peers.map { it.name })
        assertEquals("duckdns", setup.ddnsProvider)
        assertEquals("203.0.113.9", setup.ddnsLast)
        val empty = SecurityParser.remoteSetup(JSONObject("{}"))
        assertFalse(empty.vpnConfigured || empty.ddnsConfigured || empty.vpnScript)
        assertEquals("[Interface]", SecurityParser.peerConfig(JSONObject("""{"name":"phone","config":"[Interface]"}""")).config)
    }
}
