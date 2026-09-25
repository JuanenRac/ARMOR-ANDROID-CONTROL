package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.EventParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPolicyTest {
    private fun alert(id: Long, from: String, to: String, targets: Int = 2) = ArmorEvent(id, "t", "alert", "north-1", from, to, targets, null)
    private fun node(id: Long, to: String) = ArmorEvent(id, "t", "node", "north-1", "online", to, null, null)
    private fun camera(id: Long, to: String) = ArmorEvent(id, "t", "camera", "cam-01", "online", to, null, null)
    private fun mode(id: Long, mode: String) = ArmorEvent(id, "t", "mode", "", null, null, null, mode)

    @Test fun onlyHighAlertsAndDeadCamerasAlwaysWakeTheOperator() {
        val events = listOf(alert(1, "normal", "review", 1), alert(2, "review", "high"), alert(3, "high", "normal", 0), camera(4, "offline"), camera(5, "online"), mode(6, "armed"))
        assertEquals(listOf(2L, 4L), AlarmPolicy.notices(events, armed = false).map { it.id })
    }

    @Test fun silentAndOfflineNodesMatterOnlyWhileArmed() {
        val events = listOf(node(1, "stale"), node(2, "offline"), node(3, "online"))
        assertTrue(AlarmPolicy.notices(events, armed = false).isEmpty())
        assertEquals(listOf(1L, 2L), AlarmPolicy.notices(events, armed = true).map { it.id })
    }

    @Test fun noticesComeOldestFirstEvenWhenTheServerListsNewestFirst() {
        val events = listOf(alert(9, "review", "high"), camera(7, "offline"), alert(8, "review", "high"))
        assertEquals(listOf(7L, 8L, 9L), AlarmPolicy.notices(events, armed = true).map { it.id })
    }

    @Test fun theHistoryReadsInSpanish() {
        assertEquals("Sistema ARMADO", AlarmPolicy.describe(mode(1, "armed")))
        assertEquals("north-1: revisar → ALTA (2 objetivos)", AlarmPolicy.describe(alert(2, "review", "high")))
        assertEquals("cam-01: en línea → fuera de línea", AlarmPolicy.describe(camera(3, "offline")))
        assertEquals("north-1: en línea → en silencio", AlarmPolicy.describe(node(4, "stale")))
    }

    @Test fun theFirstRunOnlyRecordsABaselineAndNothingIsAnnouncedTwice() {
        var stored = -1L
        val tracker = AlarmTracker({ stored }, { stored = it })
        val history = listOf(alert(5, "review", "high"), alert(4, "normal", "review", 1))
        assertTrue("the whole history must not replay as alarms", tracker.claim(history, armed = true).isEmpty())
        assertEquals(5L, stored)
        assertTrue(tracker.claim(history, armed = true).isEmpty())
        val next = listOf(camera(6, "offline")) + history
        assertEquals(listOf(6L), tracker.claim(next, armed = true).map { it.id })
        assertEquals(6L, stored)
        assertTrue("already claimed", tracker.claim(next, armed = true).isEmpty())
    }

    @Test fun anEmptyHistoryChangesNothing() {
        var stored = -1L
        val tracker = AlarmTracker({ stored }, { stored = it })
        assertTrue(tracker.claim(emptyList(), armed = true).isEmpty())
        assertEquals(-1L, stored)
    }

    @Test fun parsingAcceptsKnownEventsAndRefusesEverythingElse() {
        val parsed = EventParser.parse(JSONObject("""{"id":7,"at":"2026-01-01T00:00:00.000Z","type":"alert","node_id":"north-1","from":"review","to":"high","targets":2}"""))
        assertNotNull(parsed)
        assertEquals("north-1", parsed!!.subject)
        assertEquals(2, parsed.targets)
        assertEquals("cam-01", EventParser.parse(JSONObject("""{"id":8,"at":"x","type":"camera","camera_id":"cam-01","from":"online","to":"offline"}"""))!!.subject)
        assertEquals("armed", EventParser.parse(JSONObject("""{"id":9,"at":"x","type":"mode","mode":"armed"}"""))!!.mode)
        assertNull(EventParser.parse(JSONObject("""{"id":0,"type":"alert","node_id":"n"}""")))
        assertNull(EventParser.parse(JSONObject("""{"id":1,"type":"unknown"}""")))
        assertNull(EventParser.parse(JSONObject("""{"id":1,"type":"node"}""")))
        assertNull(EventParser.parse(JSONObject("""{"type":"mode","mode":"armed"}""")))
    }
}
