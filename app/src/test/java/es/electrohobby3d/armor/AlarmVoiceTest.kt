package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmVoiceTest {
    private val voices = listOf(
        VoiceCandidate("en-gb-x-rjs-local", "en-GB"),
        VoiceCandidate("es-es-x-eed-local#female_1", "es-ES"),
        VoiceCandidate("es-es-x-eec-local#male_1", "es-ES"),
        VoiceCandidate("es-us-x-sfb-local", "es-US"),
    )

    @Test fun theVoiceByDefaultIsAnAdultPlainOneAtTheNormalPace() {
        val voice = VoiceSettings()
        assertEquals(1f, voice.pitch(false), 0.001f)
        assertEquals(1f, voice.rate(), 0.001f)
        assertFalse(voice.speaks("warning"))
        assertTrue(voice.speaks("critical") && voice.speaks("high"))
    }

    @Test fun theAgeAndTheStyleMultiplyAsInStudio() {
        val child = VoiceSettings(age = "child", style = "hero")
        assertEquals(1.6f * 1.12f, child.pitch(false), 0.001f)
        assertEquals(1.1f * 1.22f, child.rate(), 0.001f)
        val senior = VoiceSettings(age = "senior", style = "dark", speed = 50)
        assertEquals(0.4f, senior.pitch(false), 0.001f)   // 0.7 * 0.55 is under what an engine takes: the pitch stops at 0.4
        assertEquals(0.88f * 0.82f * 0.5f, senior.rate(), 0.001f)
    }

    @Test fun thePitchAndThePaceStayWhereASpeechEngineCanTakeThem() {
        val extreme = VoiceSettings(age = "senior", style = "dark", gender = "male")
        assertTrue(extreme.pitch(false) >= 0.4f)
        assertTrue(VoiceSettings(age = "child", style = "hero", gender = "female", speed = 200).let { it.pitch(false) <= 2f && it.rate() <= 3f })
    }

    @Test fun theGenderChangesThePitchOnlyWhenNoVoiceSaidIt() {
        assertEquals(1.12f, VoiceSettings(gender = "female").pitch(false), 0.001f)
        assertEquals(0.82f, VoiceSettings(gender = "male").pitch(false), 0.001f)
        assertEquals(1f, VoiceSettings(gender = "female").pitch(true), 0.001f)
    }

    @Test fun aNameThatSaysTheGenderIsRead() {
        assertEquals("female", VoiceSettings.hint("es-es-x-eed-local#female_1"))
        assertEquals("male", VoiceSettings.hint("es-es-x-eec-local#male_1"))
        assertEquals("female", VoiceSettings.hint("Microsoft Helena Woman"))
        assertEquals("", VoiceSettings.hint("en-gb-x-rjs-local"))
        assertEquals("", VoiceSettings.hint("human voice"))
    }

    @Test fun theVoiceOfTheLanguageWithTheWantedGenderIsPicked() {
        val (woman, womanFound) = VoiceSettings(gender = "female").pick(voices, "es")
        assertEquals("es-es-x-eed-local#female_1", woman?.name)
        assertTrue(womanFound)
        val (man, manFound) = VoiceSettings(gender = "male").pick(voices, "es")
        assertEquals("es-es-x-eec-local#male_1", man?.name)
        assertTrue(manFound)
    }

    @Test fun withoutAVoiceOfThatGenderTheFirstOfTheLanguageIsUsedAndThePitchStandsIn() {
        val (voice, found) = VoiceSettings(gender = "female").pick(voices, "en")
        assertEquals("en-gb-x-rjs-local", voice?.name)
        assertFalse(found)
    }

    @Test fun noVoiceOfTheLanguageLeavesTheEnginesOwn() {
        assertNull(VoiceSettings().pick(voices, "ja").first)
        assertNull(VoiceSettings().pick(emptyList(), "es").first)
    }

    @Test fun theChoicesSurviveBeingWrittenAndRead() {
        val voice = VoiceSettings("male", "senior", "dark", 130, true)
        assertEquals(voice, VoiceSettings.decode(voice.encode()))
    }

    @Test fun damagedTextOrValuesThatAreNotKnownAreTheDefaultOnes() {
        assertEquals(VoiceSettings(), VoiceSettings.decode(null))
        assertEquals(VoiceSettings(), VoiceSettings.decode("x|y"))
        assertEquals(VoiceSettings(), VoiceSettings.decode("robot|ancient|chaos|abc|false"))
        assertEquals(200, VoiceSettings.decode("any|adult|plain|900|false").speed)
        assertEquals(50, VoiceSettings.decode("any|adult|plain|1|false").speed)
    }
}
