// ARMOR-ANDROID-CONTROL - the HTTP client against a real local server: what it sends, what it refuses, and what it makes of answers that are odd.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.AlarmPanelText
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.ElevationRequiredException
import es.electrohobby3d.armor.network.SignIn
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

class ApiClientTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val origin get() = server.origin

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(origin); server.stop() }

    private fun failure(block: () -> Unit): ArmorApiException = try { block(); fail("expected the call to fail"); throw IllegalStateException() } catch (error: ArmorApiException) { error }

    @Test fun `every call says what it is, so the history of connections can tell the app from the others`() {
        server.reply(200, """{"mode":"armed","revision":3,"nodes":{}}""")
        client.status(origin)
        val client = server.seen.single().headers["x-armor-client"]
        assertNotNull(client)
        assertTrue(client!!, client.startsWith("android/"))
        assertEquals("application/json", server.seen.single().headers["accept"])
    }

    @Test fun `a trailing slash on the address does not double the slash in the path`() {
        server.reply(200, """{"nodes":{}}""")
        client.status("$origin///")
        assertEquals("/api/v1/status", server.seen.single().path)
    }

    @Test fun `the state of the perimeter reads a node without light, a missing list and unknown fields`() {
        server.reply(200, """{"mode":"armed","revision":7,"updated_at":"t","nodes":{"north-1":{"online":true,"lux":null,"target_count":2,"alert_level":"high","stale":true,"future_field":[1]},"south-2":{}}}""")
        val snapshot = client.status(origin)
        assertEquals("armed", snapshot.mode)
        assertEquals(7L, snapshot.revision)
        val north = snapshot.nodes.first { it.id == "north-1" }
        assertEquals(listOf(true, null, 2, "high", true), listOf(north.online, north.lux, north.tracks, north.alert, north.stale))
        val south = snapshot.nodes.first { it.id == "south-2" }
        assertEquals(listOf(false, null, 0, "normal"), listOf(south.online, south.lux, south.tracks, south.alert))
        server.reply(200, "{}")
        val empty = client.status(origin)
        assertEquals("disarmed", empty.mode)
        assertTrue(empty.nodes.isEmpty())
    }

    @Test fun `only the modes armed and disarmed can be asked for, before anything is sent`() {
        try { client.setMode(origin, "away"); fail("away is not a mode of this call") } catch (_: IllegalArgumentException) { }
        assertTrue(server.seen.isEmpty())
    }

    @Test fun `the PIN goes only with a disarm, and only when there is one`() {
        server.reply(200, """{"mode":"disarmed","nodes":{}}""")
        client.setMode(origin, "armed", "1234")
        client.setMode(origin, "disarmed", "")
        client.setMode(origin, "disarmed", null)
        client.setMode(origin, "disarmed", "2468")
        val posts = server.seen.filter { it.method == "POST" }.map { JSONObject(it.body) }
        assertEquals(listOf(false, false, false, true), posts.map { it.has("pin") })
        assertEquals("2468", posts[3].getString("pin"))
        assertEquals(listOf("armed", "disarmed", "disarmed", "disarmed"), posts.map { it.getString("mode") })
        assertEquals("application/json", server.seen.first { it.method == "POST" }.headers["content-type"])
        assertEquals("after each change the state is read again", 4L, server.seen.count { it.method == "GET" }.toLong())
    }

    @Test fun `a refused PIN is told in words and keeps the status of the server`() {
        for ((code, status) in listOf("wrong_pin" to 403, "pin_locked" to 429, "pin_not_set" to 403)) {
            server.reply(status, """{"error":"x","code":"$code"}""")
            val error = failure { client.setMode(origin, "disarmed", "0000") }
            assertEquals(AlarmPanelText.error(code), error.message)
            assertEquals(status, error.code)
        }
        server.reply(500, """{"error":"boom"}""")
        val other = failure { client.setMode(origin, "disarmed", "0000") }
        assertTrue(other.message!!.contains("HTTP 500"))
    }

    @Test fun `a refusal carries the status, a short piece of what the server said, and a server that says nothing is still an answer`() {
        server.reply(401, "x".repeat(500))
        val error = failure { client.status(origin) }
        assertEquals(401, error.code)
        assertTrue(error.message!!.contains("HTTP 401"))
        assertTrue("the detail is cut", error.message!!.length < 260)
        server.reply(503, "")
        val empty = failure { client.status(origin) }
        assertEquals(503, empty.code)
        assertFalse(empty.message!!.endsWith(": "))
    }

    @Test fun `a success that is not the expected one is also a failure, and a redirect is never followed`() {
        server.reply(200, "{}")
        failure { client.closeStudioSession(origin) }      // the server answers 200, the call expects 204
        server.reply(302, "", "Location" to "$origin/elsewhere")
        val redirected = failure { client.status(origin) }
        assertEquals(302, redirected.code)
        assertEquals(listOf("/api/v1/status"), server.seen.takeLast(1).map { it.path })
    }

    @Test fun `a refusal for want of a new proof becomes the error that makes the app ask for the password`() {
        server.reply(403, """{"error":"critical action","code":"elevation_required"}""")
        try { client.endOtherSessions(origin); fail("the call must fail") } catch (error: ElevationRequiredException) { assertEquals(403, error.code) }
        server.reply(403, """{"error":"forbidden","code":"forbidden"}""")
        val plain = failure { client.endOtherSessions(origin) }
        assertFalse(plain is ElevationRequiredException)
    }

    @Test fun `a server that is not there is an IOException quickly, not a hang`() {
        val closed = ServerSocket(0).use { it.localPort }
        val started = System.nanoTime()
        try { client.status("http://127.0.0.1:$closed"); fail("nothing is listening") } catch (_: java.io.IOException) { }
        assertTrue("it must give up at once on a refused connection", (System.nanoTime() - started) / 1_000_000 < 5_000)
    }

    @Test fun `names in a path are encoded so a space or a slash cannot change where the call goes`() {
        server.reply(204, "")
        client.deleteMedia(origin, es.electrohobby3d.armor.model.MediaItem("m", "cam 1/x", "snapshot", "a b+c.jpg", "", 0))
        assertEquals("/api/v1/media/cam%201%2Fx/snapshots/a%20b%2Bc.jpg", server.seen.single().rawPath)
        server.seen.clear(); server.reply(200, "{}")
        client.acknowledgeAlarm(origin, "alarm ../1")
        assertEquals("/api/v1/alarms/alarm%20..%2F1/acknowledge", server.seen.single().rawPath)
        assertEquals("$origin/api/v1/cameras/c%201/mjpeg", client.mjpegUrl(origin, "c 1"))
        assertEquals("$origin/api/v1/media/c/recordings/r.mp4", client.mediaUrl(origin, es.electrohobby3d.armor.model.MediaItem("m", "c", "recording", "r.mp4", "", 0)))
    }

    @Test fun `a stream ticket is used only when it points inside the cameras of the server`() {
        server.reply(200, """{"path":"/api/v1/cameras/c1/stream?ticket=t"}""")
        assertEquals("$origin/api/v1/cameras/c1/stream?ticket=t", client.streamTicket("$origin/", "c1"))
        for (bad in listOf("""{"path":"https://evil.example/x"}""", """{"path":"/api/v1/admin"}""", "{}")) {
            server.reply(200, bad)
            try { client.streamTicket(origin, "c1"); fail("a ticket outside the cameras must be refused: $bad") } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun `the first step of signing in tells a finished sign-in from one that waits for the second factor`() {
        server.reply(201, "{}")
        assertTrue(client.signIn(origin, "ana", "pw") is SignIn.Done)
        assertEquals(listOf("ana", "pw"), JSONObject(server.seen.single().body).let { listOf(it.getString("username"), it.getString("password")) })
        server.reply(200, """{"totp_required":true,"challenge":"abc"}""")
        val step = client.signIn(origin, "ana", "pw")
        assertEquals("abc", (step as SignIn.NeedsCode).challenge)
        server.reply(200, """{"totp_required":true}""")
        assertTrue("a challenge is needed to ask for a code", client.signIn(origin, "ana", "pw") is SignIn.Done)
        server.reply(200, "not json at all")
        assertTrue(client.signIn(origin, "ana", "pw") is SignIn.Done)
        server.reply(401, "{}")
        assertEquals(401, failure { client.signIn(origin, "ana", "bad") }.code)
        server.reply(429, "{}")
        assertEquals(429, failure { client.signIn(origin, "ana", "bad") }.code)
    }

    @Test fun `the second step sends the challenge and the code and expects a created session`() {
        server.reply(201, "{}")
        client.completeSignIn(origin, "abc", "123456")
        val sent = JSONObject(server.seen.single().body)
        assertEquals("abc", sent.getString("challenge"))
        assertEquals("123456", sent.getString("code"))
        assertEquals("/api/v1/studio/session/totp", server.seen.single().path)
        server.reply(401, """{"code":"bad_code"}""")
        assertEquals(401, failure { client.completeSignIn(origin, "abc", "000000") }.code)
    }

    @Test fun `a new proof is a code when there is one and the password otherwise, never both`() {
        server.reply(200, "{}")
        client.elevate(origin, "pw", "")
        client.elevate(origin, "pw", "123456")
        val bodies = server.seen.map { JSONObject(it.body) }
        assertEquals(listOf(true, false), bodies.map { it.has("password") })
        assertEquals(listOf(false, true), bodies.map { it.has("code") })
    }

    @Test fun `the session cookie of the server is kept, sent back, and forgotten on request`() {
        server.respond = { request ->
            if (request.path == "/api/v1/studio/session" && request.method == "POST") Triple(201, "{}", mapOf("Set-Cookie" to "armor_studio_sid=s3ssion; Path=/; HttpOnly"))
            else Triple(200, """{"authenticated":true,"user":{"role":"operator"}}""", emptyMap())
        }
        client.openStudioSession(origin, "ana", "pw")
        assertEquals("armor_studio_sid" to "s3ssion", client.sessionCookie(origin))
        assertTrue(client.studioSessionActive(origin))
        assertEquals("operator", client.studioRole(origin))
        assertTrue("the cookie goes back with the next request", server.seen.last().headers["cookie"]!!.contains("armor_studio_sid=s3ssion"))
        client.forgetSession(origin)
        assertNull(client.sessionCookie(origin))
        client.studioRole(origin)
        assertNull("nothing is sent once forgotten", server.seen.last().headers["cookie"])
    }

    @Test fun `a session cookie put back goes out as a plain name=value, the way the server reads it`() {
        client.restoreSessionCookie(origin, "armor_studio_sid", "kept-value")
        server.reply(200, """{"authenticated":true}""")
        assertTrue(client.studioSessionActive(origin))
        assertEquals("armor_studio_sid=kept-value", server.seen.single().headers["cookie"])
    }

    @Test fun `the role is empty when the server does not say, and a session that is not valid is reported as such`() {
        server.reply(200, """{"authenticated":false}""")
        assertFalse(client.studioSessionActive(origin))
        assertEquals("", client.studioRole(origin))
        server.reply(200, """{"authenticated":true,"user":{}}""")
        assertEquals("", client.studioRole(origin))
    }

    @Test fun `the history asks for the newest first, or for what came after an event, never mixing the two by accident`() {
        server.reply(200, """{"events":[]}""")
        client.history(origin)
        client.history(origin, limit = 10, before = 99)
        client.history(origin, limit = 5, after = 7)
        assertEquals(listOf("limit=50", "limit=10&before=99", "limit=5&after=7"), server.seen.map { it.query })
        server.reply(200, """{"events":[null,5,{"id":1}]}""")
        client.history(origin)       // entries that are not events do not break the list
    }

    @Test fun `a camera or a media item with a field missing is left out of the list, not the whole list`() {
        server.reply(200, """{"cameras":[{"id":"c1","name":"Gate","host":"h"},{"name":"no id"},{"id":""},{"id":"c2"}]}""")
        val cameras = client.cameraViews(origin)
        assertEquals(listOf("c1", "c2"), cameras.map { it.id })
        assertEquals("c2", cameras[1].name)
        server.reply(200, """{"items":[{"id":"1","cameraId":"c","kind":"snapshot","file":"a.jpg","bytes":5},{"id":"2","cameraId":"c","kind":"snapshot"},{"id":"","cameraId":"c","kind":"k","file":"f"}],"activeCameraIds":["c",7,null]}""")
        val media = client.media(origin)
        assertEquals(listOf("1"), media.items.map { it.id })
        assertEquals(5L, media.items[0].bytes)
        assertTrue(media.activeCameraIds.contains("c"))
        server.reply(200, """{"cameras":[{"id":"c1","status":"online"},{"status":"online"}]}""")
        assertEquals(listOf("c1" to "online"), client.cameraStatus(origin).map { it.id to it.status })
    }

    @Test fun `a command to a panel names the node and the action, carries the profile only when arming, the PIN only when disarming`() {
        server.reply(202, "{}")
        client.sendAlarmCommand(origin, "panel-1", "away", false)
        client.sendAlarmCommand(origin, "panel-1", "stay", true, pin = "1234", profile = "night")
        client.sendAlarmCommand(origin, "panel-1", null, false, pin = "2468", profile = "night")
        client.sendAlarmCommand(origin, "panel-1", null, true, pin = "")
        client.silenceAlarmSiren(origin, "panel-1", "1357")
        client.silenceAlarmSiren(origin, "panel-1")
        val bodies = server.seen.map { JSONObject(it.body) }
        assertEquals(listOf("arm", "away", false, false), listOf(bodies[0].getString("action"), bodies[0].getString("mode"), bodies[0].has("force"), bodies[0].has("pin")))
        assertEquals(listOf("arm", "stay", "night", true, false), listOf(bodies[1].getString("action"), bodies[1].getString("mode"), bodies[1].getString("profile"), bodies[1].getBoolean("force"), bodies[1].has("pin")))
        assertEquals(listOf("disarm", "2468", false, false), listOf(bodies[2].getString("action"), bodies[2].getString("pin"), bodies[2].has("profile"), bodies[2].has("mode")))
        assertEquals(false, bodies[3].has("pin"))
        assertEquals(listOf("silence", "1357"), listOf(bodies[4].getString("action"), bodies[4].getString("pin")))
        assertEquals(false, bodies[5].has("pin"))
        assertTrue(server.seen.all { it.path == "/api/v1/alarm/command" && it.method == "POST" })
    }

    @Test fun `a refused panel command is explained by its code and a code it does not know gets the general sentence`() {
        for (code in listOf("commands_disabled", "unknown_node", "node_unavailable", "busy", "mqtt_unavailable", "wrong_pin", "pin_locked", "unknown_profile", "something_new")) {
            server.reply(409, """{"error":"x","code":"$code"}""")
            val error = failure { client.sendAlarmCommand(origin, "p", "away", false) }
            assertEquals(code, AlarmPanelText.error(code), error.message)
            assertEquals(409, error.code)
        }
        assertNotEquals(AlarmPanelText.error("busy"), AlarmPanelText.error("something_new"))
    }

    private fun assertNotEquals(a: String, b: String) = assertTrue("$a must differ from $b", a != b)

    @Test fun `push tokens are registered even when the server has push off, and taken back with a POST`() {
        server.reply(409, "{}")
        client.registerPush(origin, "tok")
        server.reply(200, "{}")
        client.unregisterPush(origin, "tok")
        assertEquals(listOf("POST" to "/api/v1/push/register", "POST" to "/api/v1/push/unregister"), server.seen.map { it.method to it.path })
        assertEquals("tok", JSONObject(server.seen[1].body).getString("token"))
        server.reply(500, "{}")
        assertEquals(500, failure { client.registerPush(origin, "tok") }.code)
    }

    @Test fun `a device command carries the confirmation only when it was given`() {
        server.reply(200, "{}")
        client.commandDevice(origin, "siren 1", "on")
        client.commandDevice(origin, "siren 1", "off", confirm = true)
        assertEquals("/api/v1/devices/siren%201/command", server.seen[0].rawPath)
        assertEquals(listOf(false, true), server.seen.map { JSONObject(it.body).has("confirm") })
    }

    @Test fun `the voice and the preflight and the lists read their answers whatever extra the server sends`() {
        server.reply(200, """{"available":true,"extra":{"a":1}}""")
        assertTrue(client.voiceAvailable(origin))
        server.reply(200, "{}")
        assertFalse(client.voiceAvailable(origin))
        server.reply(200, """{"devices":[],"future":1}""")
        assertTrue(client.devices(origin).isEmpty())
        server.reply(200, """{"active":[],"recent":[]}""")
        val (active, recent) = client.alarms(origin)
        assertTrue(active.isEmpty() && recent.isEmpty())
    }
}
