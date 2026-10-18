package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.AlarmPanelText
import es.electrohobby3d.armor.model.AlarmPanelsParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The arming modes (profiles): what GET /api/v1/alarm/profiles answers, what a panel's state says about them, and their wording. */
class ArmProfilesTest {
    private val answer = """
    {"revision":3,"updated_at":"2026-01-01T10:00:00.000Z","updated_by":"admin","max":8,"profiles":[
      {"id":"away","name":"Away","base":"away"},
      {"id":"stay","name":"Home","base":"stay"},
      {"id":"night","name":"Night","base":"stay","entry_delay_s":45,"siren":false,"notify":"normal","zones":[{"id":"hall-motion","armed":true},{"id":"garage","armed":false}]},
      {"id":"mine","name":"Garden","base":"away","exit_delay_s":0},
      {"id":"","name":"No id","base":"away"},
      {"id":"odd","name":"Odd base","base":"night"},
      {"id":"empty","name":"","base":"away"}
    ]}
    """.trimIndent()

    @Test fun theModesAreReadAndWhatIsNotOneIsLeftOut() {
        val modes = AlarmPanelsParser.profiles(JSONObject(answer))
        assertEquals(listOf("away", "stay", "night", "mine"), modes.map { it.id })
        assertFalse(modes[0].stay); assertTrue(modes[1].stay)
        val night = modes[2]
        assertTrue(night.stay)
        assertEquals(45, night.entryDelaySeconds); assertNull(night.exitDelaySeconds)
        assertFalse(night.siren); assertEquals("normal", night.notify); assertEquals(2, night.differingZones)
        assertEquals(0, modes[3].exitDelaySeconds)                       // a delay of 0 is a delay, not "the panel's own"
        assertTrue(modes[0].siren); assertEquals("urgent", modes[0].notify)
        assertTrue(AlarmPanelsParser.profiles(JSONObject("{}")).isEmpty())
    }

    private val state = """
    {"nodes":[
      {"node_id":"alarm-1","received_at":"x","stale":false,"state":{"phase":"armed","mode":"stay","siren":false,"locked_out":false,"commands_enabled":true,"arm_profiles":true,"profile":"night","zones":[],"open_zones":[],"events":[]}},
      {"node_id":"alarm-2","received_at":"x","stale":false,"state":{"phase":"disarmed","mode":"disarmed","siren":false,"locked_out":false,"commands_enabled":true,"zones":[],"open_zones":[],"events":[]}}
    ],"totals":{"nodes":2,"stale":0,"armed":1,"sounding":0}}
    """.trimIndent()

    @Test fun aPanelSaysWhetherItKnowsTheModesAndWhichOneItIsArmedWith() {
        val panels = AlarmPanelsParser.overview(JSONObject(state)).nodes
        assertTrue(panels[0].armProfiles); assertEquals("night", panels[0].profile)
        assertFalse(panels[1].armProfiles); assertNull(panels[1].profile)    // an older firmware says nothing: it is armed away or at home only
    }

    @Test fun aCommandRecordKeepsTheModeItWasSentWith() {
        val status = AlarmPanelsParser.commands(JSONObject("""{"enabled":true,"recent":[{"command_id":"c0ffee0123456789","node_id":"alarm-1","action":"arm","mode":"stay","profile":"night","accepted":true,"refusal":"none"},
            {"command_id":"c0ffee0123456780","node_id":"alarm-1","action":"arm","mode":"away","accepted":true,"refusal":"none"}]}"""))
        assertEquals("night", status.recent[0].profile)
        assertNull(status.recent[1].profile)
    }

    @Test fun theWordingNeverShowsACode() {
        val modes = AlarmPanelsParser.profiles(JSONObject(answer))
        assertEquals("Fuera", AlarmPanelText.profileName("away", "Away"))
        assertEquals("En casa", AlarmPanelText.profileName("stay", "Home"))
        assertEquals("Noche", AlarmPanelText.profileName("night", "Night"))
        assertEquals("Jardín", AlarmPanelText.profileName("night", "Jardín"))        // the owner renamed it: the name is theirs
        assertEquals("Garden", AlarmPanelText.profileName("mine", "Garden"))
        assertEquals("como en casa · entrada 45 s · sirena silenciosa · 2 zonas distintas", AlarmPanelText.profileSummary(modes[2]))
        assertEquals("como fuera", AlarmPanelText.profileSummary(modes[0]))
        assertEquals("como fuera · salida 0 s", AlarmPanelText.profileSummary(modes[3]))
        for (code in listOf("unknown_profile", "profiles_unsupported")) assertNotEquals("No se pudo enviar la orden", AlarmPanelText.error(code))
    }
}
