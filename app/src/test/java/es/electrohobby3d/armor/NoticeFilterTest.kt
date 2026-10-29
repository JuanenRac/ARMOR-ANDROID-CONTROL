package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeFilterTest {
    private fun notice(severity: String, kind: String) = AlarmNotice(1, "t", "x", severity, kind)

    @Test fun `by default every notice shows as before`() {
        val settings = NoticeSettings()
        for (kind in NoticeKinds.CHOICES + NoticeKinds.SYSTEM) for (severity in listOf("critical", "high", "warning")) assertEquals(NoticeDecision.SHOW, settings.decide(notice(severity, kind)))
    }

    @Test fun `silencing everything hides every notice, the critical ones too`() {
        val settings = NoticeSettings(muted = true)
        assertEquals(NoticeDecision.HIDE, settings.decide(notice("critical", "alarm.device")))
        assertEquals(NoticeDecision.HIDE, settings.decide(notice("warning", "camera")))
    }

    @Test fun `a severity or a kind that is switched off hides the notice but never a critical one`() {
        val settings = NoticeSettings().withSeverity("warning", off = true).withKind("alarm.network", off = true)
        assertEquals(NoticeDecision.HIDE, settings.decide(notice("warning", "camera")))
        assertEquals(NoticeDecision.HIDE, settings.decide(notice("high", "alarm.network")))
        assertEquals(NoticeDecision.SHOW, settings.decide(notice("high", "camera")))
        assertEquals(NoticeDecision.SHOW, settings.decide(notice("critical", "alarm.network")))
    }

    @Test fun `without sound shows the notice quietly except the critical ones`() {
        val settings = NoticeSettings(silent = true)
        assertEquals(NoticeDecision.SILENT, settings.decide(notice("high", "alert")))
        assertEquals(NoticeDecision.SILENT, settings.decide(notice("warning", "node")))
        assertEquals(NoticeDecision.SHOW, settings.decide(notice("critical", "alarm.device")))
    }

    @Test fun `the choices are kept and read back, and a damaged text means everything shows`() {
        val settings = NoticeSettings(muted = false, silent = true).withSeverity("high", off = true).withKind("node", off = true).withKind("alarm.solar", off = true)
        assertEquals(settings, NoticeSettings.decode(settings.encode()))
        assertEquals(NoticeSettings(), NoticeSettings.decode(null))
        assertEquals(NoticeSettings(), NoticeSettings.decode("nonsense"))
        assertEquals(NoticeSettings(), NoticeSettings.decode("false|false|critical,bogus|moon"))
    }

    @Test fun `a notice carries the kind of what it is about`() {
        val events = listOf(
            es.electrohobby3d.armor.model.ArmorEvent(id = 1, at = "", type = "alarm", subject = "s", from = null, to = "raised", targets = null, mode = null, severity = "high", code = "smoke", sourceType = "solar"),
            es.electrohobby3d.armor.model.ArmorEvent(id = 2, at = "", type = "camera", subject = "c", from = null, to = "offline", targets = null, mode = null),
        )
        val kinds = AlarmPolicy.notices(events, armed = false).map { it.kind }
        assertTrue(kinds.contains("alarm.solar") && kinds.contains("camera"))
        assertEquals("alarm.other", NoticeKinds.ofAlarm("weird"))
    }
}
