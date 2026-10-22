package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The alarm window of the open app: which alarm it shows, when it comes back after being closed, and what it never shows. */
class AlarmBannerTest {
    private fun alarm(id: String, severity: String = "high", code: String = "smoke", raisedAt: String = "2030-01-01T10:00:00Z", by: String? = null) =
        Alarm(id = id, sourceType = "device", sourceId = "dev-$id", severity = severity, code = code, raisedAt = raisedAt, acknowledgedBy = by, cleared = false)

    @Test fun nothingWaitingMeansNoBanner() {
        assertNull(AlarmBannerPolicy.from(emptyList(), null))
        assertNull(AlarmBannerPolicy.from(listOf(alarm("a", by = "ana")), null))
    }

    @Test fun theWorstUnconfirmedAlarmIsShownAndTheCountSaysHowManyWait() {
        val banner = AlarmBannerPolicy.from(listOf(alarm("a", "warning"), alarm("b", "critical"), alarm("c", "high")), null)!!
        assertEquals("critical", banner.severity)
        assertEquals("b", banner.alarm.id)
        assertEquals(3, banner.count)
        assertTrue(banner.title.contains("3"))
        assertTrue(banner.detail.contains("dev-b"))
    }

    @Test fun ofTwoEqualAlarmsTheNewerOneIsShown() {
        val banner = AlarmBannerPolicy.from(listOf(alarm("old", raisedAt = "2030-01-01T09:00:00Z"), alarm("new", raisedAt = "2030-01-01T11:00:00Z")), null)!!
        assertEquals("new", banner.alarm.id)
    }

    @Test fun aSingleAlarmNamesItsSeverityInsteadOfACount() {
        val banner = AlarmBannerPolicy.from(listOf(alarm("a", "critical")), null)!!
        assertEquals(1, banner.count)
        assertTrue(banner.title.contains("CRÍTICA"))
    }

    @Test fun aConfirmedAlarmDoesNotWaitAndDoesNotCount() {
        val banner = AlarmBannerPolicy.from(listOf(alarm("a", "critical", by = "ana"), alarm("b", "warning")), null)!!
        assertEquals("b", banner.alarm.id)
        assertEquals(1, banner.count)
    }

    @Test fun theAlarmOfADuressPinIsNeverShownOnAPhone() {
        assertNull(AlarmBannerPolicy.from(listOf(alarm("a", "critical", code = "duress_disarm")), null))
        val banner = AlarmBannerPolicy.from(listOf(alarm("a", "critical", code = "duress_disarm"), alarm("b", "warning")), null)!!
        assertEquals("b", banner.alarm.id)
    }

    @Test fun aClosedBannerStaysClosedUntilTheSetOfWaitingAlarmsChanges() {
        val first = listOf(alarm("a"))
        val closed = AlarmBannerPolicy.from(first, null)!!.key
        assertNull(AlarmBannerPolicy.from(first, closed))
        assertNull(AlarmBannerPolicy.from(listOf(alarm("a", severity = "critical")), closed))   // the same alarm, however its text changes
        assertNotNull(AlarmBannerPolicy.from(first + alarm("b"), closed))                       // a new alarm brings it back
    }

    @Test fun theKeyDoesNotDependOnTheOrder() {
        assertEquals(AlarmBannerPolicy.key(listOf(alarm("b"), alarm("a"))), AlarmBannerPolicy.key(listOf(alarm("a"), alarm("b"))))
        assertNotEquals(AlarmBannerPolicy.key(listOf(alarm("a"))), AlarmBannerPolicy.key(listOf(alarm("a"), alarm("b"))))
    }

    @Test fun criticalIsRedAndBeatsFasterThanTheRest() {
        assertEquals(ArmorColors.Alert, AlarmBannerPolicy.colour("critical"))
        assertEquals(ArmorColors.Amber, AlarmBannerPolicy.colour("high"))
        assertEquals(ArmorColors.Amber, AlarmBannerPolicy.colour("warning"))
        assertTrue(AlarmBannerPolicy.pulseMillis("critical") < AlarmBannerPolicy.pulseMillis("high"))
    }
}
