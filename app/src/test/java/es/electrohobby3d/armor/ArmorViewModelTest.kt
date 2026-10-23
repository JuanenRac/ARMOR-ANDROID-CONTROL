// ARMOR-ANDROID-CONTROL - the state of the screens against a real local server: who may change what, how a sign-in goes, what a refusal looks like, and what a sign-out forgets.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.network.ArmorApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

@OptIn(ExperimentalCoroutinesApi::class)
class ArmorViewModelTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private lateinit var model: ArmorViewModel
    private val origin get() = server.origin

    /** The routes of a server where the password is "pw", with the role of the user to choose. */
    private fun serve(role: String = "operator", twoFactor: Boolean = false, extra: (Seen) -> Triple<Int, String, Map<String, String>>? = { null }) {
        server.respond = { request ->
            extra(request) ?: when {
                request.method == "POST" && request.path == "/api/v1/studio/session" ->
                    if (request.body.contains("\"pw\"")) (if (twoFactor) Triple(200, """{"totp_required":true,"challenge":"abc"}""", emptyMap()) else Triple(201, "{}", mapOf("Set-Cookie" to "armor_studio_sid=s1; Path=/; HttpOnly")))
                    else Triple(401, """{"error":"no"}""", emptyMap())
                request.method == "POST" && request.path == "/api/v1/studio/session/totp" -> Triple(201, "{}", mapOf("Set-Cookie" to "armor_studio_sid=s2; Path=/; HttpOnly"))
                request.method == "GET" && request.path == "/api/v1/studio/session" -> Triple(200, """{"authenticated":true,"user":{"role":"$role"}}""", emptyMap())
                request.method == "DELETE" && request.path == "/api/v1/studio/session" -> Triple(204, "", emptyMap())
                request.method == "POST" && request.path == "/api/v1/mode" -> Triple(200, "{}", emptyMap())
                request.method == "POST" && request.path == "/api/v1/alarms/acknowledge" -> Triple(200, "{}", emptyMap())
                request.method == "GET" && request.path == "/api/v1/status" -> Triple(200, """{"mode":"armed","revision":2,"nodes":{"n1":{"online":true,"target_count":0}}}""", emptyMap())
                request.method == "GET" && request.path == "/api/v1/alarms" -> Triple(200, """{"active":[],"recent":[]}""", emptyMap())
                else -> Triple(200, "{}", emptyMap())
            }
        }
    }

    @Before fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = FakeServer(); client = ArmorApiClient(); model = ArmorViewModel(client)
        serve()
    }
    @After fun stop() { client.forgetSession(origin); server.stop(); Dispatchers.resetMain() }

    /** Waits (a real, short time) for work that runs on other threads. */
    private fun until(what: String, condition: () -> Boolean) {
        val end = System.nanoTime() + 5_000_000_000L
        while (!condition()) { if (System.nanoTime() > end) throw AssertionError("timed out waiting for: $what"); Thread.sleep(5) }
    }
    private fun idle() = until("the work to finish") { !model.state.value.loading && !model.security.value.busy }
    private fun signIn(role: String = "operator") { serve(role); model.login(origin, "ana", "pw"); until("signed in") { model.state.value.authenticated }; idle() }
    private fun posts(path: String) = server.seen.count { it.method == "POST" && it.path == path }
    private val camera = CameraView("c1", "Gate", "h", "", "", 80, 554, false, false)

    @Test fun `an address that is not a valid origin is refused before anything is sent`() {
        model.refresh("ftp://nope")
        model.refresh("http://8.8.8.8:80")      // plain http to a public address would send the password in clear
        assertTrue(model.state.value.message!!.contains("valid HTTPS origin"))
        assertTrue(server.seen.isEmpty())
    }

    @Test fun `signing in needs a name and a password, and says so without asking the server`() {
        model.login(origin, "ana", "   "); idle()
        assertEquals("Enter username and password", model.state.value.message)
        model.login(origin, "", "pw"); idle()
        assertEquals("Enter username and password", model.state.value.message)
        assertTrue(server.seen.isEmpty())
        assertFalse(model.state.value.authenticated)
    }

    @Test fun `a right password opens the session, learns the role and reads the state`() {
        signIn("operator")
        val state = model.state.value
        assertEquals("operator", state.role)
        assertFalse(state.readOnly)
        assertFalse(state.isAdmin)
        assertEquals("armed", state.snapshot.mode)
        assertEquals(listOf("n1"), state.snapshot.nodes.map { it.id })
        assertEquals("Session established", state.message)
        assertTrue(model.sessionCookie(origin)!!.second == "s1")
    }

    @Test fun `a wrong password leaves nobody signed in and says the server refused`() {
        model.login(origin, "ana", "wrong"); idle()
        assertFalse(model.state.value.authenticated)
        assertTrue(model.state.value.message!!.contains("HTTP 401"))
        assertNull(model.sessionCookie(origin))
    }

    @Test fun `a user is trimmed and the roles decide what is only looked at`() {
        for ((role, readOnly) in listOf("admin" to false, "operator" to false, "viewer" to true, "panel" to true)) {
            val fresh = ArmorViewModel(client)
            serve(role)
            fresh.login(origin, "  ana ", "pw")
            until("signed in as $role") { fresh.state.value.authenticated && !fresh.state.value.loading }
            assertEquals(role, role, fresh.state.value.role)
            assertEquals(role, readOnly, fresh.state.value.readOnly)
            assertEquals(role, role == "admin", fresh.state.value.isAdmin)
        }
        assertTrue(server.seen.filter { it.path == "/api/v1/studio/session" && it.method == "POST" }.all { it.body.contains("\"ana\"") })
    }

    private fun assertEquals(message: String, expected: Any?, actual: Any?) = org.junit.Assert.assertEquals(message, expected, actual)

    @Test fun `with the second factor on the sign-in waits for the code and opens only with it`() {
        serve(twoFactor = true)
        model.login(origin, "ana", "pw"); idle()
        assertFalse(model.state.value.authenticated)
        assertEquals("abc", model.security.value.loginChallenge)
        model.submitCode(origin, " 123456 ")
        until("signed in") { model.state.value.authenticated }; idle()
        assertNull(model.security.value.loginChallenge)
        val sent = server.seen.last { it.path == "/api/v1/studio/session/totp" }.body
        assertTrue(sent, sent.contains("\"123456\"") && sent.contains("\"abc\""))
    }

    @Test fun `a code that is not accepted is explained, and an expired sign-in starts again`() {
        serve(twoFactor = true) { request -> if (request.path == "/api/v1/studio/session/totp") Triple(401, """{"code":"bad_code"}""", emptyMap()) else null }
        model.login(origin, "ana", "pw"); idle()
        model.submitCode(origin, "000000"); idle()
        assertEquals(securityError("bad_code"), model.state.value.message)
        assertEquals("abc", model.security.value.loginChallenge)       // the person can try again
        assertFalse(model.state.value.authenticated)
        serve(twoFactor = true) { request -> if (request.path == "/api/v1/studio/session/totp") Triple(401, """{"code":"challenge_expired"}""", emptyMap()) else null }
        model.submitCode(origin, "000000"); idle()
        assertNull(model.security.value.loginChallenge)
        assertEquals(securityError("challenge_expired"), model.state.value.message)
    }

    @Test fun `cancelling the code forgets the challenge, and a code with no challenge asks nothing`() {
        model.submitCode(origin, "123456")
        assertTrue(server.seen.isEmpty())
        serve(twoFactor = true)
        model.login(origin, "ana", "pw"); idle()
        model.cancelCode()
        assertNull(model.security.value.loginChallenge)
    }

    @Test fun `a viewer or a wall panel cannot change anything, and nothing is asked of the server`() {
        for (role in listOf("viewer", "panel")) {
            val fresh = ArmorViewModel(client)
            serve(role); fresh.login(origin, "ana", "pw"); until("signed in") { fresh.state.value.authenticated && !fresh.state.value.loading }
            server.seen.clear()
            fresh.setMode(origin, "disarmed", "1234")
            fresh.acknowledgeAll(origin)
            fresh.snapshot(origin, camera)
            fresh.toggleRecording(origin, camera)
            fresh.ptz(origin, camera, "left")
            fresh.alarmCommand(origin, "panel-1", "away")
            fresh.silenceAlarm(origin, "panel-1", "1234")
            fresh.scanNetworkNow(origin)
            fresh.sendVoice(origin, "turn the lights off")
            assertEquals(role, tr("Tu usuario solo puede mirar: no puede cambiar nada."), fresh.state.value.message)
            assertEquals(role, 0, server.seen.count { it.method != "GET" })
        }
    }

    @Test fun `changing something before signing in is refused with the reason`() {
        model.setMode(origin, "armed"); idle()
        assertEquals("Sign in first", model.state.value.message)
        assertEquals(0, posts("/api/v1/mode"))
        model.deleteMedia(origin, es.electrohobby3d.armor.model.MediaItem("m", "c", "snapshot", "a.jpg", "", 0)); idle()
        assertEquals("Sign in first", model.state.value.message)
        model.ptz(origin, camera, "left")       // a camera move before signing in is dropped without a word
        assertTrue(server.seen.none { it.path.contains("ptz") })
    }

    @Test fun `an operator arms and the screen shows the state the server gives back`() {
        signIn()
        model.setMode(origin, "armed", null); idle()
        assertEquals(tr("Sistema ARMADO"), model.state.value.message)
        assertEquals("armed", model.state.value.snapshot.mode)
        assertEquals(1, posts("/api/v1/mode"))
    }

    @Test fun `a server that refuses is shown as it said it, with the text of its refusal`() {
        signIn()
        serve { request -> if (request.path == "/api/v1/mode") Triple(403, """{"error":"wrong","code":"wrong_pin"}""", emptyMap()) else null }
        model.setMode(origin, "disarmed", "0000"); idle()
        assertEquals(es.electrohobby3d.armor.model.AlarmPanelText.error("wrong_pin"), model.state.value.message)
        assertFalse(model.state.value.loading)
    }

    @Test fun `a critical action from outside the house asks for the password and then does itself again`() {
        signIn()
        var refused = true
        serve { request ->
            when {
                request.path == "/api/v1/mode" && refused -> Triple(403, """{"error":"proof","code":"elevation_required"}""", emptyMap())
                request.path == "/api/v1/studio/elevate" -> { refused = false; Triple(200, """{"elevated_for_s":300}""", emptyMap()) }
                else -> null
            }
        }
        model.setMode(origin, "armed"); idle()
        assertTrue(model.security.value.elevationNeeded)
        assertNull("no error message while it waits for the proof", model.state.value.message)
        model.elevate(origin, "pw", "")
        until("the action done again") { model.state.value.message == "Done" }
        assertFalse(model.security.value.elevationNeeded)
        assertEquals(2, posts("/api/v1/mode"))
        assertEquals(1, posts("/api/v1/studio/elevate"))
        model.elevate(origin, "pw", "")
        idle()
        assertEquals("with nothing waiting a proof repeats nothing", 2, posts("/api/v1/mode"))
    }

    @Test fun `a wrong proof is explained and keeps the question open, and cancelling it forgets the action`() {
        signIn()
        serve { request ->
            when (request.path) {
                "/api/v1/mode" -> Triple(403, """{"error":"proof","code":"elevation_required"}""", emptyMap())
                "/api/v1/studio/elevate" -> Triple(401, """{"error":"no","code":"bad_proof"}""", emptyMap())
                else -> null
            }
        }
        model.setMode(origin, "armed"); idle()
        model.elevate(origin, "wrong", "")
        until("the refusal") { model.security.value.elevationError != null }
        assertEquals(securityError("bad_proof"), model.security.value.elevationError)
        assertTrue(model.security.value.elevationNeeded)
        model.dismissElevation()
        assertFalse(model.security.value.elevationNeeded)
        assertNull(model.security.value.elevationError)
        assertEquals(1, posts("/api/v1/mode"))
    }

    @Test fun `acknowledging an alarm reads the list again, and a message can be dismissed`() {
        signIn()
        serve { request -> if (request.method == "GET" && request.path == "/api/v1/alarms") Triple(200, """{"active":[],"recent":[]}""", emptyMap()) else null }
        model.acknowledgeAll(origin); idle()
        assertEquals(tr("Alarmas confirmadas"), model.state.value.message)
        assertEquals(1, posts("/api/v1/alarms/acknowledge"))
        model.dismissMessage()
        assertNull(model.state.value.message)
    }

    @Test fun `a camera move that fails is the one thing worth a message, and a success says nothing`() {
        signIn()
        model.ptz(origin, camera, "left")
        until("the move") { server.seen.any { it.path.contains("/ptz") } }
        Thread.sleep(50)
        assertNull(model.state.value.message ?.takeIf { it.contains("PTZ") })
        serve { request -> if (request.path.contains("/ptz")) Triple(502, """{"error":"camera silent"}""", emptyMap()) else null }
        model.ptz(origin, camera, "stop")
        until("the failure shown") { model.state.value.message?.contains("HTTP 502") == true }
    }

    @Test fun `signing out closes the session on the server, forgets it here and returns the screens to their start`() {
        signIn()
        model.registerPush(origin, "tok")
        until("the token registered") { posts("/api/v1/push/register") == 1 }
        model.logout(origin)
        until("signed out") { !model.state.value.authenticated }
        assertEquals("Session closed", model.state.value.message)
        assertEquals("", model.state.value.role)
        assertEquals(1, posts("/api/v1/push/unregister"))
        assertEquals(1, server.seen.count { it.method == "DELETE" && it.path == "/api/v1/studio/session" })
        assertNull("the cookie is gone", model.sessionCookie(origin))
    }

    @Test fun `signing out with the server down still forgets everything here`() {
        signIn()
        serve { request -> if (request.method == "DELETE") Triple(500, "{}", emptyMap()) else null }
        model.logout(origin)
        until("signed out") { !model.state.value.authenticated }
        assertNull(model.sessionCookie(origin))
        val closed = ServerSocket(0).use { it.localPort }
        signIn()
        model.logout("http://127.0.0.1:$closed")      // no server at all at that address
        until("signed out with no server") { !model.state.value.authenticated }
    }

    @Test fun `a kept session is used when the server still honours it, refused when it does not, and kept when the server could not be reached`() {
        runBlocking {
            assertFalse(model.tryRestore(origin, null) { error("nothing to refuse") })
            assertTrue(server.seen.isEmpty())
            val kept = StoredSession(origin, "armor_studio_sid", "old")
            var refused = 0
            serve { request -> if (request.path == "/api/v1/studio/session") Triple(401, "{}", emptyMap()) else null }
            assertFalse(model.tryRestore(origin, kept) { refused += 1 })
            assertEquals(1, refused)
            serve { request -> if (request.path == "/api/v1/studio/session") Triple(200, """{"authenticated":false}""", emptyMap()) else null }
            assertFalse(model.tryRestore(origin, kept) { refused += 1 })
            assertEquals(2, refused)
            val closed = ServerSocket(0).use { it.localPort }
            assertFalse(model.tryRestore("http://127.0.0.1:$closed", StoredSession("http://127.0.0.1:$closed", "armor_studio_sid", "old")) { refused += 1 })
            assertEquals("a server that could not be asked is not a refusal", 2, refused)
            serve("viewer")
            assertTrue(model.tryRestore(origin, kept) { refused += 1 })
            assertTrue(model.state.value.authenticated)
            assertEquals("viewer", model.state.value.role)
            assertTrue(server.seen.map { it.headers["cookie"] }.toString(), server.seen.any { it.headers["cookie"]?.contains("armor_studio_sid=old") == true })
        }
    }

    @Test fun `older events are asked for from the oldest one the screen has`() {
        signIn()
        serve { request ->
            if (request.path == "/api/v1/history") Triple(200, """{"events":[{"id":9,"type":"mode","at":"2026-01-01T00:00:00Z","mode":"armed"},{"id":8,"type":"mode","at":"2026-01-01T00:00:00Z","mode":"disarmed"}]}""", emptyMap()) else null
        }
        model.loadOlderEvents(origin); idle()          // nothing on screen yet: nothing to be older than
        assertTrue(server.seen.none { it.path == "/api/v1/history" })
    }

    @Test fun `arming waits for the lock of the phone and is not sent when the lock refuses`() {
        signIn()
        val reasons = mutableListOf<String>()
        model.sensitiveGate = SensitiveGate { reason -> reasons += reason; false }
        model.setMode(origin, "disarmed"); idle()
        assertEquals(0, posts("/api/v1/mode"))
        assertEquals(tr("No se ha confirmado con el bloqueo del teléfono: no se ha hecho nada."), model.state.value.message)
        assertEquals(listOf(tr("Desarmar el sistema")), reasons)
        model.sensitiveGate = SensitiveGate { true }
        model.setMode(origin, "disarmed"); idle()
        until("the order went") { posts("/api/v1/mode") == 1 }
    }

    @Test fun `a viewer is told off before the lock of the phone is asked`() {
        signIn("viewer")
        var asked = false
        model.sensitiveGate = SensitiveGate { asked = true; true }
        model.setMode(origin, "armed"); idle()
        assertFalse(asked)
        assertEquals(0, posts("/api/v1/mode"))
    }

    @Test fun `without a gate everything goes as before`() {
        signIn()
        model.setMode(origin, "armed"); idle()
        assertEquals(1, posts("/api/v1/mode"))
    }

    @Test fun `the server's request for the lock comes with the sign-in`() {
        serve { request ->
            if (request.method == "GET" && request.path == "/api/v1/studio/session") Triple(200, """{"authenticated":true,"user":{"role":"operator"},"capabilities":{"version":1,"actions":{},"features":[],"security":{"client.device_lock":"always"}}}""", emptyMap()) else null
        }
        model.login(origin, "ana", "pw"); until("signed in") { model.state.value.authenticated }; idle()
        assertEquals("always", model.state.value.deviceLock)
        assertEquals("operator", model.state.value.role)
    }

    @Test fun `the voice is not sent for an empty phrase or to an address that is not valid`() {
        signIn()
        server.seen.clear()
        model.sendVoice(origin, "   ")
        assertTrue(server.seen.none { it.path.contains("voice") })
        model.sendVoice("ftp://nope", "lights on")
        assertTrue(model.voice.value.messages.last().problem)
        assertFalse(model.voice.value.busy)
        model.cancelVoice()
        assertEquals(tr("Cancelado: no he hecho nada."), model.voice.value.messages.last().text)
        model.clearVoice()
        assertTrue(model.voice.value.messages.isEmpty())
    }
}
