package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNoticesTest {
    @Test fun `a notice of a known kind is read with its id and version, and nothing else travels`() {
        for (type in LiveNotices.TYPES) assertEquals(LiveNotice(type, "alm-00001", 12), LiveNotices.parse("""{"type":"$type","id":"alm-00001","version":12}"""))
        assertEquals(LiveNotice("alarm", "a", 1), LiveNotices.parse("""{"type":"alarm","id":"a","version":1,"detail":"not kept"}"""))
    }

    @Test fun `the state, an unknown kind, a missing or wrong field, text that is not JSON and a huge message are ignored`() {
        assertNull(LiveNotices.parse("""{"type":"state","state":{}}"""))
        assertNull(LiveNotices.parse("""{"type":"weather","id":"x","version":1}"""))
        assertNull(LiveNotices.parse("""{"type":"alarm","version":1}"""))
        assertNull(LiveNotices.parse("""{"type":"alarm","id":5,"version":1}"""))
        assertNull(LiveNotices.parse("""{"type":"alarm","id":"a"}"""))
        assertNull(LiveNotices.parse("""{"type":"alarm","id":"a","version":"1"}"""))
        assertNull(LiveNotices.parse("""{"type":"alarm","id":"${"x".repeat(65)}","version":1}"""))
        assertNull(LiveNotices.parse("not json"))
        assertNull(LiveNotices.parse(null))
        assertNull(LiveNotices.parse("x".repeat(2000)))
    }

    @Test fun `the address of the channel follows the scheme of the server`() {
        assertEquals("ws://192.168.0.180:18080/api/v1/events", LiveNotices.url("http://192.168.0.180:18080"))
        assertEquals("wss://armor.example.org/api/v1/events", LiveNotices.url("https://armor.example.org/"))
        assertNull(LiveNotices.url("ftp://host"))
        assertNull(LiveNotices.url(""))
    }

    @Test fun `the wait before the next try starts at two seconds, doubles and stops at a minute`() {
        assertEquals(listOf(2000L, 4000L, 8000L, 16000L, 32000L, 60000L, 60000L, 60000L), listOf(1, 2, 3, 4, 5, 6, 7, 20).map { LiveNotices.retryDelay(it) })
        assertEquals(2000L, LiveNotices.retryDelay(0))
    }

    @Test fun `the screens ask slowly only while a notice arrived within the last minute`() {
        val now = 1_000_000L
        assertEquals(LiveNotices.POLL_FAST_MS, LiveNotices.pollDelay(0, now))
        assertEquals(LiveNotices.POLL_SLOW_MS, LiveNotices.pollDelay(now - 5_000, now))
        assertEquals(LiveNotices.POLL_SLOW_MS, LiveNotices.pollDelay(now - LiveNotices.FRESH_MS, now))
        assertEquals(LiveNotices.POLL_FAST_MS, LiveNotices.pollDelay(now - LiveNotices.FRESH_MS - 1, now))
        assertEquals(LiveNotices.POLL_FAST_MS, LiveNotices.pollDelay(now + 5_000, now))
        assertTrue(LiveNotices.POLL_SLOW_MS > LiveNotices.POLL_FAST_MS)
        assertFalse(LiveNotices.TYPES.contains("state"))
    }
}
