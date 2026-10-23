package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Push messages with data only: which event, in the words of the phone, once, whether it comes by push or by the watch. */
class PushEventsTest {
    private val origin = "http://192.168.0.180:8080"
    private val store = MapStore()
    private fun announced() = AnnouncedIds({ store.get(it) }, { key, value -> store.put(key, value) })
    private val smoke = mapOf("event_id" to "120", "type" to "alarm", "source" to "smoke-1", "to" to "raised", "severity" to "critical", "code" to "smoke", "source_type" to "device", "at" to "2030-01-01T00:00:00Z")

    @Before fun spanish() { Tr.useTable("es", emptyMap()) }

    @Test fun `a message without a usable event id is not an event of ours`() {
        assertNull(PushEvents.parse(emptyMap()))
        assertNull(PushEvents.parse(smoke - "event_id"))
        assertNull(PushEvents.parse(smoke + ("event_id" to "0")))
        assertNull(PushEvents.parse(smoke + ("event_id" to "abc")))
        assertNull(PushEvents.parse(smoke + ("type" to "mystery")))
        assertNull(PushEvents.parse(smoke - "source"))
    }

    @Test fun `the event is read from the data`() {
        val event = PushEvents.parse(smoke)!!
        assertEquals(120L, event.id)
        assertEquals("smoke-1", event.subject)
        assertEquals("critical", event.severity)
        assertEquals("device", event.sourceType)
    }

    @Test fun `a device alarm gives a notice in the language of the phone, once`() {
        val ids = announced()
        val notice = PushEvents.notice(smoke, origin, ids)!!
        assertEquals(120L, notice.id)
        assertEquals("critical", notice.severity)
        assertTrue(notice.title.startsWith("ALARMA"))
        assertNull("the same event again gives nothing", PushEvents.notice(smoke, origin, ids))
    }

    @Test fun `the alarm of a duress pin is never shown`() {
        assertNull(PushEvents.notice(smoke + ("code" to "duress_disarm"), origin, announced()))
    }

    @Test fun `a node going quiet matters only while armed`() {
        val node = mapOf("event_id" to "7", "type" to "node", "node_id" to "radar-1", "to" to "offline")
        assertNull(PushEvents.notice(node, origin, announced()))
        assertNotNull(PushEvents.notice(node + ("armed" to "true") + ("event_id" to "8"), origin, announced()))
    }

    @Test fun `what the system already showed is not said again by the watch`() {
        val ids = announced()
        PushEvents.noteShown(smoke, origin, ids)
        assertTrue(ids.seen(origin, 120))
        assertNull(PushEvents.notice(smoke, origin, ids))
    }

    @Test fun `the watch and the push do not announce one event twice`() {
        val ids = announced()
        val tracker = AlarmTracker({ 100L }, { _, _ -> }, now = { java.time.Instant.parse("2030-01-01T00:01:00Z").toEpochMilli() }, announced = ids)
        // the push says event 120 first
        assertNotNull(PushEvents.notice(smoke, origin, ids))
        // then the watch reads 119 and 120: it announces 119 and not 120
        val read = listOf(
            ArmorEvent(119, "2030-01-01T00:00:00Z", "alarm", "door-1", null, "raised", null, null, "high", "door_open", "device"),
            ArmorEvent(120, "2030-01-01T00:00:00Z", "alarm", "smoke-1", null, "raised", null, null, "critical", "smoke", "device"),
        )
        assertEquals(listOf(119L), tracker.claim(origin, read, armed = true).map { it.id })
        assertFalse(ids.firstTime(origin, 119))
    }

    @Test fun `only the newest ids are kept`() {
        val ids = AnnouncedIds({ store.get(it) }, { key, value -> store.put(key, value) }, capacity = 3)
        for (id in 1L..5L) assertTrue(ids.firstTime(origin, id))
        assertFalse(ids.seen(origin, 1))
        assertTrue(ids.seen(origin, 5))
    }
}
