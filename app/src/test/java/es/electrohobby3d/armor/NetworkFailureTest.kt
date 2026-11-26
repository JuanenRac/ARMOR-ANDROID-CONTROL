// ARMOR-ANDROID-CONTROL - the client when the network or the server fails (a refused connection, an answer cut short or too long, an outage that ends), and what a read-only account can never do.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.CapabilityAction
import es.electrohobby3d.armor.network.ElevationRequiredException
import es.electrohobby3d.armor.network.SessionCapabilities
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class NetworkFailureTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val origin get() = server.origin

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(origin); server.stop() }

    /** What the call throws; the test fails if it comes back with an answer. */
    private fun thrown(block: () -> Unit): Exception {
        try { block() } catch (error: Exception) { return error }
        fail("the call must fail")
        throw IllegalStateException()
    }

    /** A server that is not an HTTP server: it reads the start of the request and plays whatever the test says on the socket. */
    private class RawServer(private val play: (Socket) -> Unit) {
        private val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        val origin: String get() = "http://127.0.0.1:${socket.localPort}"
        private val thread = Thread {
            try {
                while (!socket.isClosed) {
                    val peer = socket.accept()
                    try { peer.getInputStream().read(ByteArray(4096)); play(peer) } catch (_: IOException) { } finally { try { peer.close() } catch (_: IOException) { } }
                }
            } catch (_: IOException) { }
        }.apply { isDaemon = true; start() }
        fun stop() { socket.close(); thread.join(2_000) }
    }

    private class Call(val name: String, val run: (ArmorApiClient, String) -> Unit)
    private fun call(name: String, run: (ArmorApiClient, String) -> Unit) = Call(name, run)

    private val reads: List<Call> = listOf(
        call("status") { c, o -> c.status(o) },
        call("alarms") { c, o -> c.alarms(o) },
        call("solar") { c, o -> c.solar(o) },
        call("electrical") { c, o -> c.electrical(o) },
        call("network") { c, o -> c.network(o) },
        call("services") { c, o -> c.services(o) },
        call("devices") { c, o -> c.devices(o) },
        call("history") { c, o -> c.history(o, 10, null, null) },
        call("preflight") { c, o -> c.preflight(o) },
        call("cameraStatus") { c, o -> c.cameraStatus(o) },
        call("nodeHealth") { c, o -> c.nodeHealth(o) },
        call("armingModes") { c, o -> c.armingModes(o) },
        call("batteryOperations") { c, o -> c.batteryOperations(o) },
        call("alarmPanels") { c, o -> c.alarmPanels(o) },
        call("sessionFacts") { c, o -> c.sessionFacts(o) },
    )

    @Test fun `no read call comes back with data when the server is down, whatever the reason it gives`() {
        for (read in reads) {
            val name = read.name
            for (status in listOf(401, 500, 502, 503)) {
                server.reply(status, """{"error":"nope","code":"some_code"}""")
                val error = thrown { read.run(client, origin) }
                assertTrue("$name $status: ${error::class.simpleName}", error is ArmorApiException)
                assertEquals("$name $status", status, (error as ArmorApiException).code)
                assertEquals("$name $status", "some_code", error.apiError.code)
            }
        }
    }

    @Test fun `a connection that is refused is an IO error for every read, never an answer of the server`() {
        val closed = ServerSocket(0).use { it.localPort }
        for (read in reads) {
            val error = thrown { read.run(client, "http://127.0.0.1:$closed") }
            assertTrue("${read.name}: ${error::class.simpleName}", error is IOException && error !is ArmorApiException)
        }
    }

    @Test fun `an outage that ends leaves the client as good as new`() {
        server.reply(200, """{"mode":"armed","revision":1,"nodes":{}}""")
        assertEquals("armed", client.status(origin).mode)
        server.reply(503, "")
        assertEquals(503, (thrown { client.status(origin) } as ArmorApiException).code)
        assertEquals(503, (thrown { client.status(origin) } as ArmorApiException).code)
        server.reply(200, """{"mode":"disarmed","revision":2,"nodes":{}}""")
        assertEquals("disarmed", client.status(origin).mode)
        assertEquals(4, server.seen.size)
    }

    @Test fun `the server goes away between two calls`() {
        val brief = FakeServer()
        try {
            brief.reply(200, """{"mode":"armed","revision":1,"nodes":{}}""")
            assertEquals("armed", client.status(brief.origin).mode)
            brief.stop()
            val error = thrown { client.status(brief.origin) }
            assertTrue(error::class.simpleName, error is IOException && error !is ArmorApiException)
        } finally { client.forgetSession(brief.origin) }
    }

    @Test fun `an answer that is not JSON is a failure, not an empty state`() {
        for (body in listOf("<html>gateway error</html>", "", "not json", "[1,2,3]", "{\"mode\":", "null")) {
            server.reply(200, body)
            thrown { client.status(origin) }
            thrown { client.alarms(origin) }
        }
    }

    @Test fun `an answer cut short is a failure`() {
        val raw = RawServer { peer ->
            peer.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 400\r\nConnection: close\r\n\r\n{\"mode\":\"armed\",\"nodes\":".toByteArray())
            peer.getOutputStream().flush()
        }
        try { thrown { client.status(raw.origin) } } finally { raw.stop() }
    }

    @Test fun `a server that closes the connection without a word is an IO error`() {
        val raw = RawServer { }
        try {
            val error = thrown { client.status(raw.origin) }
            assertTrue(error::class.simpleName, error is IOException)
        } finally { raw.stop() }
    }

    @Test fun `a server that does not speak HTTP is a failure`() {
        val raw = RawServer { peer -> peer.getOutputStream().write("SSH-2.0-OpenSSH_9.6\r\n".toByteArray()); peer.getOutputStream().flush() }
        try { thrown { client.status(raw.origin) } } finally { raw.stop() }
    }

    @Test fun `an answer longer than the limit is cut off as an IO error`() {
        server.reply(200, "{\"mode\":\"armed\",\"pad\":\"" + "x".repeat(9 * 1024 * 1024) + "\"}")
        val error = thrown { client.status(origin) }
        assertTrue(error::class.simpleName, error is IOException)
    }

    @Test fun `every write is refused with the status of the server when the account may only look`() {
        val writes: List<Call> = listOf(
            call("acknowledgeAlarm") { c, o -> c.acknowledgeAlarm(o, "a1") },
            call("acknowledgeAllAlarms") { c, o -> c.acknowledgeAllAlarms(o) },
            call("panicGesture") { c, o -> c.panicGesture(o) },
            call("arm") { c, o -> c.setMode(o, "armed", null) },
            call("disarm") { c, o -> c.setMode(o, "disarmed", "1234") },
            call("commandDevice") { c, o -> c.commandDevice(o, "light-1", "on", false) },
            call("scanNetworkNow") { c, o -> c.scanNetworkNow(o) },
            call("forgetNode") { c, o -> c.forgetNode(o, "node-1") },
            call("nodePanelTicket") { c, o -> c.nodePanelTicket(o, "node-1") },
            call("panelCommand") { c, o -> c.sendAlarmCommand(o, "panel-1", "armed", false, null, null) },
            call("silenceSiren") { c, o -> c.silenceAlarmSiren(o, "panel-1", null) },
        )
        for (write in writes) {
            server.seen.clear()
            server.reply(403, """{"error":"forbidden","code":"forbidden"}""")
            val error = thrown { write.run(client, origin) }
            assertTrue("${write.name}: ${error::class.simpleName}", error is ArmorApiException)
            assertFalse("${write.name} is not a request for a new proof", error is ElevationRequiredException)
            assertEquals(write.name, 403, (error as ArmorApiException).code)
            assertTrue("${write.name} sent nothing that only reads", server.seen.isNotEmpty() && server.seen.all { it.method != "GET" })
        }
        server.reply(403, """{"error":"forbidden","code":"forbidden"}""")
        assertEquals("forbidden", (thrown { client.acknowledgeAlarm(origin, "a1") } as ArmorApiException).apiError.code)
    }

    @Test fun `a refused write is not repeated by the client on its own`() {
        server.reply(403, """{"error":"forbidden","code":"forbidden"}""")
        thrown { client.acknowledgeAlarm(origin, "a1") }
        thrown { client.panicGesture(origin) }
        assertEquals(listOf("/api/v1/alarms/a1/acknowledge", "/api/v1/alarm/panic-gesture"), server.seen.map { it.path })
    }

    @Test fun `the role decides what a phone offers when the server announces nothing, and only the exact word counts`() {
        val operator = setOf(CapabilityAction.ARM, CapabilityAction.DISARM, CapabilityAction.ACKNOWLEDGE, CapabilityAction.OPERATE, CapabilityAction.CAMERA_CONTROL)
        val admin = operator + setOf(CapabilityAction.ADMINISTER, CapabilityAction.MANAGE_USERS)
        for (role in listOf("viewer", "panel", "guest", "", " ", "ADMIN", "Admin", "root", "operator ", " admin", "admin\n", "operador")) {
            assertTrue("[$role]", CapabilityAction.ALL.none { SessionCapabilities.fromRole(role).can(it) })
        }
        assertEquals(operator, CapabilityAction.ALL.filter { SessionCapabilities.fromRole("operator").can(it) }.toSet())
        assertEquals(admin, CapabilityAction.ALL.filter { SessionCapabilities.fromRole("admin").can(it) }.toSet())
        assertFalse(SessionCapabilities.fromRole("admin").announced)
        assertFalse(SessionCapabilities.fromRole("viewer").has("rtsp_tunnel"))
        assertNull(SessionCapabilities.fromRole("viewer").security)
    }

    @Test fun `an announced field only allows what it says with a real true, and nothing of a read-only account`() {
        fun parsed(json: String) = SessionCapabilities.parse(JSONObject(json))!!
        val none = parsed("""{"version":1,"actions":{}}""")
        assertTrue(none.announced)
        assertTrue(CapabilityAction.ALL.none { none.can(it) })
        val loose = parsed("""{"version":1,"actions":{"arm":"true","disarm":1,"acknowledge":null,"operate":"yes","camera_control":0,"administer":[true],"manage_users":{"a":true}}}""")
        assertTrue("only a boolean true allows", CapabilityAction.ALL.none { loose.can(it) })
        val some = parsed("""{"version":1,"actions":{"arm":true,"disarm":false},"features":["a",1,null,"b",true]}""")
        assertTrue(some.can("arm"))
        assertFalse(some.can("disarm"))
        assertFalse(some.can("Arm"))
        assertFalse(some.can(""))
        assertEquals(setOf("a", "b"), listOf("a", "b", "c", "1", "true").filter { some.has(it) }.toSet())
        assertTrue(parsed("""{"version":1,"actions":{"arm":true},"features":"rtsp_tunnel"}""").let { !it.has("rtsp_tunnel") && it.can("arm") })
        assertNull(SessionCapabilities.parse(JSONObject("""{"version":1,"actions":[]}""")))
        assertNull(SessionCapabilities.parse(JSONObject("""{"version":0,"actions":{"arm":true}}""")))
        assertNull(SessionCapabilities.parse(JSONObject("""{"actions":{"arm":true}}""")))
    }

    @Test fun `two readings of the same announcement are equal and a change of one action is not`() {
        val a = SessionCapabilities.parse(JSONObject("""{"version":1,"actions":{"arm":true},"features":["x"]}"""))!!
        val b = SessionCapabilities.parse(JSONObject("""{"version":1,"actions":{"arm":true},"features":["x"]}"""))!!
        val c = SessionCapabilities.parse(JSONObject("""{"version":1,"actions":{"arm":true,"disarm":true},"features":["x"]}"""))!!
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assertNotEquals(a, SessionCapabilities.fromRole("viewer"))
    }

    @Test fun `the screens treat every account that may not change anything as read-only`() {
        for (role in listOf("viewer", "panel", "guest")) {
            val state = MonitorUiState(role = role)
            assertTrue(role, state.readOnly)
            assertFalse(role, state.isAdmin)
            assertTrue(role, CapabilityAction.ALL.none { state.can(it) })
        }
        val operator = MonitorUiState(role = "operator")
        assertFalse(operator.readOnly)
        assertFalse(operator.isAdmin)
        assertTrue(operator.can(CapabilityAction.ARM))
        assertFalse(operator.can(CapabilityAction.ADMINISTER))
        val admin = MonitorUiState(role = "admin")
        assertFalse(admin.readOnly)
        assertTrue(admin.isAdmin)
        val announcedNothing = MonitorUiState(role = "admin", capabilities = SessionCapabilities.parse(JSONObject("""{"version":1,"actions":{}}""")))
        assertTrue("an announcement of nothing beats the role", announcedNothing.readOnly)
        assertFalse(announcedNothing.isAdmin)
        assertTrue(CapabilityAction.ALL.none { announcedNothing.can(it) })
    }
}
