// ARMOR-ANDROID-CONTROL - the hidden panic gesture: only while the server offers it, only for a person who may operate, silent whatever the answer, and no trace in the report.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.SessionFacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PanicGestureTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private lateinit var model: ArmorViewModel
    private val origin get() = server.origin

    @Before fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = FakeServer(); client = ArmorApiClient(); model = ArmorViewModel(client)
        DiagnosticLog.clear()
    }
    @After fun stop() { client.forgetSession(origin); server.stop(); Dispatchers.resetMain() }

    private fun until(what: String, condition: () -> Boolean) {
        val end = System.nanoTime() + 5_000_000_000L
        while (!condition()) { if (System.nanoTime() > end) throw AssertionError("timed out waiting for: $what"); Thread.sleep(5) }
    }

    /** A server where the password is "pw"; the session carries [security] and the gesture route answers [gesture]. */
    private fun serve(role: String, security: String, gesture: Int = 200) {
        server.respond = { request ->
            when {
                request.method == "POST" && request.path == "/api/v1/studio/session" -> Triple(201, "{}", mapOf("Set-Cookie" to "armor_studio_sid=s1; Path=/; HttpOnly"))
                request.method == "GET" && request.path == "/api/v1/studio/session" ->
                    Triple(200, """{"authenticated":true,"user":{"role":"$role"},"capabilities":{"version":1,"actions":{"operate":${role != "viewer"},"arm":${role != "viewer"}},"security":$security}}""", emptyMap())
                request.method == "POST" && request.path == "/api/v1/alarm/panic-gesture" -> Triple(gesture, if (gesture == 200) """{"ok":true}""" else """{"error":"not found"}""", emptyMap())
                request.method == "GET" && request.path == "/api/v1/status" -> Triple(200, """{"mode":"armed","revision":2,"nodes":{}}""", emptyMap())
                request.method == "GET" && request.path == "/api/v1/alarms" -> Triple(200, """{"active":[],"recent":[]}""", emptyMap())
                else -> Triple(200, "{}", emptyMap())
            }
        }
        model.login(origin, "ana", "pw")
        until("signed in") { model.state.value.authenticated }
        until("the work to finish") { !model.state.value.loading }
    }
    private fun gestures() = server.seen.count { it.method == "POST" && it.path == "/api/v1/alarm/panic-gesture" }

    @Test fun `five quick touches make the gesture, four do not, and slow ones do not add up`() {
        val counter = TapCounter()
        assertFalse((1..4).any { counter.touch(it * 400L) })
        assertTrue(counter.touch(2_000L))
        assertFalse("it starts again after a gesture", counter.touch(2_100L))
        val slow = TapCounter()
        assertFalse((0..9).any { slow.touch(it * 1_000L) })   // one a second: never five inside three seconds
    }

    @Test fun `the window is a number of the config object and five touches just inside it count`() {
        val counter = TapCounter()
        val last = PanicGesture.WINDOW_MS
        assertFalse(counter.touch(0)); assertFalse(counter.touch(last / 4)); assertFalse(counter.touch(last / 2)); assertFalse(counter.touch(last * 3 / 4))
        assertTrue(counter.touch(last))
        val late = TapCounter()
        assertFalse(late.touch(0)); assertFalse(late.touch(1)); assertFalse(late.touch(2)); assertFalse(late.touch(3))
        assertFalse("the fifth comes after the window", late.touch(last + 10))
    }

    @Test fun `the session says whether the server offers it, and an older server or the default offers nothing`() {
        fun offered(security: String) = SessionFacts.parse(JSONObject("""{"user":{"role":"operator"},"capabilities":{"version":1,"security":$security}}""")).panicGesture
        assertTrue(offered("""{"client.panic_gesture":true}"""))
        assertFalse(offered("""{"client.panic_gesture":false}"""))
        assertFalse(offered("""{}"""))
        assertFalse(SessionFacts.parse(JSONObject("""{"user":{"role":"operator"}}""")).panicGesture)
        assertFalse(SessionFacts().panicGesture)
    }

    @Test fun `while the server does not offer it the gesture sends nothing`() {
        serve("operator", """{"client.panic_gesture":false}""")
        assertFalse(model.state.value.panicGesture)
        model.panicGesture(origin)
        Thread.sleep(150)
        assertEquals(0, gestures())
    }

    @Test fun `offered and made, it asks for the silent alarm once, shows nothing and leaves no trace in the report`() {
        serve("operator", """{"client.panic_gesture":true}""")
        assertTrue(model.state.value.panicGesture)
        val before = model.state.value.message
        model.panicGesture(origin)
        until("the gesture reached the server") { gestures() == 1 }
        Thread.sleep(100)
        assertEquals("not a word on the screen", before, model.state.value.message)
        assertTrue(DiagnosticLog.recent().none { it.route.contains("panic") })
        assertTrue("the rest of the calls still are in the log", DiagnosticLog.recent().isNotEmpty())
    }

    @Test fun `a viewer never sends it even when the server offers it`() {
        serve("viewer", """{"client.panic_gesture":true}""")
        model.panicGesture(origin)
        Thread.sleep(150)
        assertEquals(0, gestures())
    }

    @Test fun `a 404 means the option was turned off since, the app forgets the gesture and says nothing`() {
        serve("operator", """{"client.panic_gesture":true}""", gesture = 404)
        val before = model.state.value.message
        model.panicGesture(origin)
        until("the app forgot the gesture") { !model.state.value.panicGesture }
        assertEquals(before, model.state.value.message)
        model.panicGesture(origin)
        Thread.sleep(100)
        assertEquals("it does not ask again", 1, gestures())
    }
}
