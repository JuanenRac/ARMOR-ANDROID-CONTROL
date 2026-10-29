package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSoundTest {
    private val own = SoundChoice(SoundChoice.OWN, "content://media/external/audio/media/42", "siren.ogg")
    private val system = SoundChoice(SoundChoice.SYSTEM, "content://media/internal/audio/media/7", "Beep")

    @Test fun nothingChosenIsTheDefaultOfEverySeverity() {
        val settings = SoundSettings()
        for (severity in SoundSettings.SEVERITIES) assertTrue(settings.choiceFor(severity, "alarm.device").isDefault)
    }

    @Test fun theKindWinsOverTheSeverityAndTheSeverityOverTheDefault() {
        val settings = SoundSettings().withSeverity("critical", system).withKind("alarm.solar", own)
        assertEquals(own, settings.choiceFor("critical", "alarm.solar"))
        assertEquals(system, settings.choiceFor("critical", "alarm.device"))
        assertTrue(settings.choiceFor("warning", "alarm.device").isDefault)
    }

    @Test fun choosingTheDefaultAgainTakesTheChoiceAway() {
        val settings = SoundSettings().withSeverity("high", own).withSeverity("high", SoundChoice())
        assertTrue(settings.bySeverity.isEmpty())
    }

    @Test fun aSeverityOrKindThatIsNotKnownIsNotKept() {
        val settings = SoundSettings().withSeverity("mystery", own).withKind("mystery", own)
        assertTrue(settings.bySeverity.isEmpty() && settings.byKind.isEmpty())
    }

    @Test fun theChoicesSurviveBeingWrittenAndRead() {
        val settings = SoundSettings().withSeverity("critical", own).withSeverity("warning", system).withKind("camera", system)
        assertEquals(settings, SoundSettings.decode(settings.encode()))
    }

    @Test fun aTabOrALineBreakInTheNameCannotBreakTheStore() {
        val settings = SoundSettings().withSeverity("high", SoundChoice(SoundChoice.OWN, "content://a/b", "bad\tname\nline"))
        val back = SoundSettings.decode(settings.encode())
        assertEquals("bad name line", back.bySeverity["high"]?.name)
    }

    @Test fun damagedTextIsTheDefault() {
        assertEquals(SoundSettings(), SoundSettings.decode(null))
        assertEquals(SoundSettings(), SoundSettings.decode("   "))
        assertEquals(SoundSettings(), SoundSettings.decode("S\tcritical\town"))                       // too few fields
        assertEquals(SoundSettings(), SoundSettings.decode("S\tcritical\town\t\tname"))               // no address
        assertEquals(SoundSettings(), SoundSettings.decode("S\tcritical\tdefault\tcontent://x\tn"))     // the default carries nothing
        assertEquals(SoundSettings(), SoundSettings.decode("S\tmystery\town\tcontent://x\tn"))          // a severity that is not known
        assertEquals(SoundSettings(), SoundSettings.decode("K\tmystery\town\tcontent://x\tn"))          // a kind that is not known
    }

    @Test fun theDefaultKeepsTheChannelsThatAlreadyExisted() {
        assertEquals(AlarmNotifier.CRITICAL_CHANNEL, SoundSettings.channelFor("critical", SoundChoice()))
        assertEquals(AlarmNotifier.WARNING_CHANNEL, SoundSettings.channelFor("warning", SoundChoice()))
        assertEquals(AlarmNotifier.ALARM_CHANNEL, SoundSettings.channelFor("high", SoundChoice()))
    }

    @Test fun eachSoundHasItsOwnChannelAndTheSameSoundTheSameOne() {
        val a = SoundSettings.channelFor("critical", own)
        val b = SoundSettings.channelFor("critical", system)
        assertNotEquals(AlarmNotifier.CRITICAL_CHANNEL, a)
        assertNotEquals(a, b)
        assertEquals(a, SoundSettings.channelFor("critical", own.copy(name = "another name")))
        assertTrue(a.startsWith(AlarmNotifier.CRITICAL_CHANNEL))
    }

    @Test fun applyingAChoiceGoesToTheSeverityOrToTheKind() {
        assertEquals(own, applyChoice(SoundSettings(), "high" to false, own).bySeverity["high"])
        assertEquals(own, applyChoice(SoundSettings(), "camera" to true, own).byKind["camera"])
        assertNull(applyChoice(SoundSettings(), "camera" to true, own).bySeverity["camera"])
        assertFalse(applyChoice(SoundSettings(), "high" to false, own).byKind.isNotEmpty())
    }
}
