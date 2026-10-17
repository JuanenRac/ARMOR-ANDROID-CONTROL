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

    @Test fun theAlarmOfADuressPinIsNeverAnnouncedOrKeptInTheHistory() {
        // the person forced to disarm types the duress PIN: the phone must not sound, speak or list it, whoever is looking over their shoulder
        val duress = """{"id":7,"at":"2026-10-17T10:00:00Z","type":"alarm","source":"duress","source_type":"alarm","severity":"critical","code":"duress_disarm","state":"raised"}"""
        assertNull(EventParser.parse(JSONObject(duress)))
        assertTrue(AlarmPolicy.isSilent("duress_disarm") && !AlarmPolicy.isSilent("smoke") && !AlarmPolicy.isSilent(null))
        val smoke = ArmorEvent(8, "t", "alarm", "d1", null, "raised", null, null, severity = "critical", code = "smoke", sourceType = "device")
        val forced = ArmorEvent(9, "t", "alarm", "duress", null, "raised", null, null, severity = "critical", code = "duress_disarm", sourceType = "alarm")
        assertEquals(listOf(8L), AlarmPolicy.notices(listOf(smoke, forced), armed = true).map { it.id })
    }

    @Test fun anEmptyPageEndsTheSearchForMissedEvents() {
        var asked = 0
        val result = freshEvents(10, listOf(alert(50, "review", "high")), { asked++; emptyList() })
        assertEquals(1, asked)
        assertEquals(listOf(50L), result.map { it.id })
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
        val stored = mutableMapOf<String, Long>()
        val tracker = AlarmTracker({ stored[it] ?: -1L }, { origin, id -> stored[origin] = id })
        val history = listOf(alert(5, "review", "high"), alert(4, "normal", "review", 1))
        assertTrue("the whole history must not replay as alarms", tracker.claim("s", history, armed = true).isEmpty())
        assertEquals(5L, stored["s"])
        assertTrue(tracker.claim("s", history, armed = true).isEmpty())
        val next = listOf(camera(6, "offline")) + history
        assertEquals(listOf(6L), tracker.claim("s", next, armed = true).map { it.id })
        assertEquals(6L, stored["s"])
        assertTrue("already claimed", tracker.claim("s", next, armed = true).isEmpty())
    }

    @Test fun eachServerHasItsOwnMemoryAndANewNumberingIsReadFromItsStart() {
        val stored = mutableMapOf<String, Long>()
        val tracker = AlarmTracker({ stored[it] ?: -1L }, { origin, id -> stored[origin] = id })
        tracker.claim("a", listOf(alert(900, "review", "high")), armed = true)
        // another server starts from its own baseline: its low numbers are not "old" because server a had reached 900
        assertTrue(tracker.claim("b", listOf(alert(3, "review", "high")), armed = true).isEmpty())
        assertEquals(3L, stored["b"])
        // the database of server a was started again: its numbering is back at 2, and what it has now is news
        assertEquals(listOf(2L), tracker.claim("a", listOf(camera(2, "offline")), armed = true).map { it.id })
        assertEquals(2L, stored["a"])
    }

    @Test fun anEventOlderThanTheLimitIsHistoryAndNotNews() {
        val stored = mutableMapOf("s" to 1L)
        val now = java.time.Instant.parse("2026-01-01T12:00:00Z").toEpochMilli()
        val tracker = AlarmTracker({ stored[it] ?: -1L }, { origin, id -> stored[origin] = id }, { now })
        fun at(id: Long, time: String) = ArmorEvent(id, time, "camera", "cam-01", "online", "offline", null, null)
        val news = tracker.claim("s", listOf(at(3, "2026-01-01T11:59:00Z"), at(2, "2025-12-31T08:00:00Z")), armed = true)
        assertEquals(listOf(3L), news.map { it.id })
    }

    @Test fun whenMoreEventsCameThanTheWindowHoldsTheGapIsFetchedWithNoneMissed() {
        val newest = (300L downTo 251L).map { alert(it, "review", "high") }   // the last 50, newest first
        val asked = mutableListOf<Long>()
        val gap = freshEvents(100, newest, { after -> asked += after; (after + 1..minOf(after + 100, 250)).map { alert(it, "review", "high") } })
        assertEquals((101L..300L).toSet(), gap.map { it.id }.toSet())
        assertTrue(asked.isNotEmpty())
        // no gap: nothing is asked
        asked.clear()
        assertEquals(newest, freshEvents(290, newest, { asked += it; emptyList() }))
        assertTrue(asked.isEmpty())
    }

    @Test fun olderEventsTheOperatorAskedForSurviveAPoll() {
        val shown = (60L downTo 11L).map { alert(it, "review", "high") }   // the window plus 'ver anteriores'
        val polled = (62L downTo 13L).map { alert(it, "review", "high") }
        val merged = mergeEvents(polled, shown)
        assertEquals(62L, merged.first().id)
        assertEquals(11L, merged.last().id)
        assertEquals(merged.size, merged.map { it.id }.toSet().size)
    }

    @Test fun anEmptyHistoryChangesNothing() {
        val stored = mutableMapOf<String, Long>()
        val tracker = AlarmTracker({ stored[it] ?: -1L }, { origin, id -> stored[origin] = id })
        assertTrue(tracker.claim("s", emptyList(), armed = true).isEmpty())
        assertEquals(null, stored["s"])
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

    @Test fun aSoundingAlarmNodeWakesTheOperatorWhetherOrNotTheServerIsArmed() {
        val sounding = ArmorEvent(7, "t", "alarm", "alarm-1", null, "raised", null, null, "critical", "alarm_sounding", "alarm")
        val tamper = ArmorEvent(8, "t", "alarm", "alarm-1/front-door", null, "raised", null, null, "high", "alarm_tamper", "alarm")
        val cleared = ArmorEvent(9, "t", "alarm", "alarm-1", null, "cleared", null, null, "critical", "alarm_sounding", "alarm")
        for (armed in listOf(true, false)) {
            val notices = AlarmPolicy.notices(listOf(sounding, tamper, cleared), armed)
            assertEquals(listOf(7L, 8L), notices.map { it.id })
        }
    }

    @Test fun theRefusalsOfAPinAreToldInWords() {
        assertEquals("El PIN no es correcto", es.electrohobby3d.armor.model.AlarmPanelText.error("wrong_pin"))
        assertEquals("Demasiados PIN erróneos: espera un poco", es.electrohobby3d.armor.model.AlarmPanelText.error("pin_locked"))
        assertEquals("Este usuario no tiene PIN: lo pone un administrador", es.electrohobby3d.armor.model.AlarmPanelText.error("pin_not_set"))
        assertEquals("12345678", onlyDigits("12a3-4 5678"))
        assertEquals("1234567890", onlyDigits("12345678901234"))
    }
}
