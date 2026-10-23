package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** One request for two askers, a fresh one after an order, and the watch leaving its turn to the open app. */
class SharedFeedTest {
    private lateinit var server: FakeServer
    private var clock = 1_000_000L
    private lateinit var api: CachingApi
    private val inner = ArmorApiClient()

    @Before fun start() {
        server = FakeServer()
        server.respond = { request -> when (request.path) {
            "/api/v1/status" -> Triple(200, """{"mode":"disarmed","revision":1,"nodes":{}}""", emptyMap())
            "/api/v1/history" -> Triple(200, """{"events":[]}""", emptyMap())
            else -> Triple(200, "{}", emptyMap())
        } }
        api = CachingApi(inner, now = { clock })
    }
    @After fun stop() { server.stop() }

    private fun count(path: String) = server.seen.count { it.path == path }

    @Test fun `two askers in the same moment cost one request`() {
        api.status(server.origin); api.status(server.origin)
        assertEquals(1, count("/api/v1/status"))
        clock += 1_300
        api.status(server.origin)
        assertEquals(2, count("/api/v1/status"))
    }

    @Test fun `the history is remembered per question`() {
        api.history(server.origin, 50); api.history(server.origin, 50); api.history(server.origin, 200, after = 5)
        assertEquals(2, count("/api/v1/history"))
        clock += 3_100
        api.history(server.origin, 50)
        assertEquals(3, count("/api/v1/history"))
    }

    @Test fun `an order makes what was remembered stale`() {
        api.status(server.origin)
        api.setMode(server.origin, "armed", null)
        val before = count("/api/v1/status")
        api.status(server.origin)
        assertEquals(before + 1, count("/api/v1/status"))
    }

    @Test fun `the watch leaves its turn to the app while the app reads`() {
        assertFalse(SharedFeed.appIsReading(now = 100_000))
        SharedFeed.appRead(now = 100_000)
        assertTrue(SharedFeed.appIsReading(now = 110_000))
        assertFalse(SharedFeed.appIsReading(now = 100_000 + SharedFeed.WINDOW_MS + 1))
    }

    @Test fun `the pause between asking for a stream again grows and starts over`() {
        val backoff = Backoff()
        assertEquals(1_000L, backoff.delayMs)
        backoff.failed(); assertEquals(2_000L, backoff.delayMs)
        backoff.failed(); assertEquals(4_000L, backoff.delayMs)
        repeat(20) { backoff.failed() }
        assertEquals(30_000L, backoff.delayMs)
        backoff.succeeded()
        assertEquals(1_000L, backoff.delayMs)
    }
}
