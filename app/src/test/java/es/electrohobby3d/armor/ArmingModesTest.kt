// ARMOR-ANDROID-CONTROL - the arming modes of the server (Away, Home, Night): what it answers, what the app sends, how a refusal is read and what the picker asks next.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmingModeException
import es.electrohobby3d.armor.model.ArmingModeText
import es.electrohobby3d.armor.model.ArmingModesParser
import es.electrohobby3d.armor.model.Preflight
import es.electrohobby3d.armor.model.PreflightIssue
import es.electrohobby3d.armor.network.ArmorApiClient
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ArmingModesTest {
    private val answer = """
    {"enabled":true,"current":"away","available":["disarmed","away","home","night"],"unassigned":["night"],"downgrade_pin_required":true,"schedule_enabled":false,"schedule_may_lower":false,
     "max_schedule":16,"revision":4,"updated_at":"x","updated_by":"admin",
     "modes":{"away":{},"home":{"defends":["zone:Hall","device:door-1"],"entry":["device:door-1"],"exit_delay_s":30},"night":{}},"schedule":[]}
    """.trimIndent()

    @Test fun theModesAreReadWithWhatEachOneDefendsAndWhatIsStillUnassigned() {
        val view = ArmingModesParser.parse(JSONObject(answer))
        assertTrue(view.enabled)
        assertEquals("away", view.current)
        assertEquals(listOf("away", "home", "night"), view.armable)
        assertEquals(listOf("night"), view.unassigned)
        assertEquals(mapOf("home" to 2, "night" to 0), view.defended)
        assertTrue(view.downgradePinRequired)
    }

    @Test fun anOffServerAnOddAnswerOrNothingIsTheOldWayOfArmingAndDisarming() {
        assertFalse(ArmingModesParser.parse(JSONObject("{}")).enabled)
        assertEquals(emptyList<String>(), ArmingModesParser.parse(JSONObject("{}")).armable)
        val odd = ArmingModesParser.parse(JSONObject("""{"enabled":true,"current":"visiting","available":["away","party","home"],"unassigned":[""]}"""))
        assertEquals("disarmed", odd.current)
        assertEquals(listOf("away", "home"), odd.armable)
        assertEquals(emptyList<String>(), odd.unassigned)
    }

    @Test fun theWordsNameTheModeAndWhatItDefends() {
        Tr.useTable("es", emptyMap())
        val view = ArmingModesParser.parse(JSONObject(answer))
        assertEquals("Modo fuera de casa", ArmingModeText.name("away"))
        assertEquals("Modo en casa", ArmingModeText.name("home"))
        assertEquals("Modo noche", ArmingModeText.name("night"))
        assertEquals("defiende todo", ArmingModeText.defends(view, "away"))
        assertEquals("2 marcados", ArmingModeText.defends(view, "home"))
        assertEquals("aún nada asignado", ArmingModeText.defends(view, "night"))
        assertEquals("Desarmado", ArmingModeText.armedTitle(false, "home"))
        assertEquals("Armado", ArmingModeText.armedTitle(true, null))
        assertEquals("Armado · Modo en casa", ArmingModeText.armedTitle(true, "home"))
        Tr.useTable("en", mapOf("Armado · %s" to "Armed · %s", "Modo en casa" to "Home"))
        assertEquals("Armed · Home", ArmingModeText.armedTitle(true, "home"))
        Tr.useTable("es", emptyMap())
    }

    @Test fun thePickerAsksForTheConfirmationOrThePinWhenTheServerSaysSo() {
        assertEquals(ArmingStage.Confirm("night"), nextArmingStage("night", "no_zones_assigned", pinTyped = false))
        assertEquals(ArmingStage.Pin("home"), nextArmingStage("home", "wrong_pin", pinTyped = false))
        assertEquals(ArmingStage.Pin("home", "wrong_pin"), nextArmingStage("home", "wrong_pin", pinTyped = true))
        assertEquals(ArmingStage.Pin("home", "pin_locked"), nextArmingStage("home", "pin_locked", pinTyped = false))
        assertEquals(ArmingStage.Pin("home", "pin_not_set"), nextArmingStage("home", "pin_not_set", pinTyped = false))
        assertNull(nextArmingStage("home", "failed", pinTyped = false))
        assertNull(nextArmingStage("home", "modes_disabled", pinTyped = false))
    }

    @Test fun armingInAModeFromDisarmedRunsTheSameCheckAsAPlainArm() {
        val blind = Preflight(false, listOf(PreflightIssue("node", "block", "node_offline", "radar-1", null)))
        val noisy = Preflight(true, listOf(PreflightIssue("zone", "warn", "zone_open", "panel-1", "Z3")))
        assertEquals(ArmingStage.Check("home", blind), checkStage("home", systemArmed = false, check = blind))
        assertEquals(ArmingStage.Check("night", noisy), checkStage("night", systemArmed = false, check = noisy))
        // nothing found, no answer (an older server or no network): the server decides, the picker goes on
        assertNull(checkStage("home", systemArmed = false, check = Preflight(true, emptyList())))
        assertNull(checkStage("home", systemArmed = false, check = null))
        // a switch between modes of an armed system is not an arm: no check, as in the console
        assertNull(checkStage("night", systemArmed = true, check = blind))
    }

    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(server.origin); server.stop() }

    @Test fun armingInAModeSendsTheModeThePinAndTheConfirmationOnlyWhenGiven() {
        server.respond = { seen -> if (seen.path == "/api/v1/mode") Triple(200, "{}", emptyMap()) else Triple(200, """{"mode":"armed","arming_mode":"home","revision":9,"nodes":{}}""", emptyMap()) }
        val snapshot = client.setArmingMode(server.origin, "home", null, false)
        assertEquals("home", snapshot.armingMode)
        assertEquals(JSONObject("""{"mode":"home"}""").toString(), JSONObject(server.seen.first { it.path == "/api/v1/mode" }.body).toString())
        server.seen.clear()
        client.setArmingMode(server.origin, "night", "1234", true)
        val body = JSONObject(server.seen.first { it.path == "/api/v1/mode" }.body)
        assertEquals("night", body.getString("mode")); assertEquals("1234", body.getString("pin")); assertTrue(body.getBoolean("confirm_unassigned"))
    }

    @Test fun aModeTheClientDoesNotKnowIsNeverSent() {
        try { client.setArmingMode(server.origin, "disarmed", null, false); fail("disarming has its own call") } catch (_: IllegalArgumentException) { }
        try { client.setArmingMode(server.origin, "party", null, false); fail("unknown mode") } catch (_: IllegalArgumentException) { }
        assertTrue(server.seen.isEmpty())
    }

    @Test fun theRefusalsOfTheServerAreReadByTheirCode() {
        for ((status, code) in listOf(409 to "no_zones_assigned", 403 to "wrong_pin", 429 to "pin_locked", 403 to "pin_not_set", 400 to "modes_disabled")) {
            server.reply(status, """{"error":"x","code":"$code","retry_after_s":120}""")
            try { client.setArmingMode(server.origin, "home", null, false); fail("must be refused: $code") }
            catch (error: ArmingModeException) { assertEquals(code, error.reason); assertEquals(status, error.code); assertTrue(error.message.orEmpty().isNotBlank()) }
        }
        server.reply(500, """{"error":"boom","code":"internal"}""")
        try { client.setArmingMode(server.origin, "home", null, false); fail("a server error is not a mode refusal") } catch (error: ArmingModeException) { fail("wrong type") } catch (_: es.electrohobby3d.armor.network.ArmorApiException) { }
    }

    @Test fun theStateCarriesTheModeInForceOnlyWhileTheServerAnnouncesOne() {
        server.reply(200, """{"mode":"armed","arming_mode":"night","revision":1,"nodes":{}}""")
        assertEquals("night", client.status(server.origin).armingMode)
        server.reply(200, """{"mode":"armed","revision":1,"nodes":{}}""")
        assertNull(client.status(server.origin).armingMode)
    }

    @Test fun theModesAreReadFromTheRouteOfTheContract() {
        server.reply(200, answer)
        assertTrue(client.armingModes(server.origin).enabled)
        assertEquals("/api/v1/alarm/modes", server.seen.single().path)
    }
}
