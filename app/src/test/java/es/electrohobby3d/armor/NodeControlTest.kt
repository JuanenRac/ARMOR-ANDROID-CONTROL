package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.NodeControlParser
import es.electrohobby3d.armor.model.NodeHealthParser
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.ElevationRequiredException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** Controlling the nodes from the phone: what the server answers is read as it is, the calls are the ones the server has, what is typed again is what the server asks for, and every word has a text in the eight languages. */
class NodeControlTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val origin get() = server.origin

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(origin); server.stop() }

    private val started = """{"id":"a1b2c3d4e5f60718","scope":"node","operation":"restart","mode":"now","actor":"ana","created_at":"2026-01-01T10:00:00Z","updated_at":"2026-01-01T10:00:05Z","state":"running",
        "nodes":[{"node_id":"radar-1","family":"radar","stage":"acknowledged","restart_in_s":2,"firmware_before":"0.7.7"}]}"""

    @Test fun anOrderIsReadWithTheLineOfEachNode() {
        val operation = NodeControlParser.operation(JSONObject(started))
        assertEquals(listOf("a1b2c3d4e5f60718", "node", "restart", "now", "running"), listOf(operation.id, operation.scope, operation.operation, operation.mode, operation.state))
        assertTrue(operation.active)
        val line = operation.nodes.single()
        assertEquals(listOf("radar-1", "radar", "acknowledged", "0.7.7"), listOf(line.nodeId, line.family, line.stage, line.firmwareBefore))
        assertFalse(line.finished)
        assertNull(line.untilS)
        val done = NodeControlParser.operation(JSONObject("""{"id":"x1","scope":"all","operation":"pause","state":"done","duration_s":600,"nodes":[
            {"node_id":"alarm-1","family":"alarm","stage":"applied","state":"paused","until_s":590,"maintenance":false},
            {"node_id":"solar-1","family":"solar","stage":"refused","refusal":"busy_battery"},
            {"node_id":"radar-1","family":"radar","stage":"applied","state":"paused","diagnostics":{"firmware":"0.7.8","uptime_s":120}}]}"""))
        assertFalse(done.active)
        assertEquals(600, done.durationS)
        assertEquals(listOf(true, true, true), done.nodes.map { it.finished })
        assertEquals(590, done.nodes[0].untilS)
        assertEquals("busy_battery", done.nodes[1].refusal)
        assertEquals("120", done.nodes[2].diagnostics["uptime_s"])
    }

    @Test fun theListIsReadAndAnEntryWithoutAnIdIsLeftOut() {
        val list = NodeControlParser.list(JSONObject("""{"operations":[$started,{"state":"done"}]}"""))
        assertEquals(listOf("a1b2c3d4e5f60718"), list.map { it.id })
        assertEquals(emptyList<String>(), NodeControlParser.list(JSONObject("{}")).map { it.id })
    }

    @Test fun theHealthOfANodeCarriesWhatItsControlSays() {
        val overview = NodeHealthParser.overview(JSONObject("""{"nodes":[{"node_id":"alarm-1","family":"alarm","firmware":"0.4.2","status":"ok","report":{"uptime_s":5},"control":{"state":"standby","maintenance":true}},
            {"node_id":"radar-1","family":"radar","firmware":"0.7.8","status":"ok","report":{"uptime_s":5}}]}"""))
        assertEquals(listOf("standby", ""), overview.nodes.map { it.controlState })
        assertEquals(listOf(true, false), overview.nodes.map { it.maintenance })
        assertEquals("En espera (sigue conectado) · en mantenimiento", controlStateName("standby", true))
    }

    @Test fun theContractHasTheEightOperationsAndNoneThatErasesOrSleeps() {
        assertEquals(listOf("restart", "pause", "resume", "standby", "identify", "diagnostics", "maintenance_on", "maintenance_off"), NodeControlParser.operations)
        for (not in listOf("factory_reset", "erase", "wipe", "deep_sleep", "sleep", "wake")) assertFalse(not, not in NodeControlParser.operations)
    }

    @Test fun whatIsTypedAgainIsTheNodeNameForOneNodeAndTheWordForAll() {
        assertEquals("radar-1", NodeControlParser.confirmation("restart", "radar-1"))
        assertEquals("RESTART", NodeControlParser.confirmation("restart", null))
        assertEquals("PAUSE", NodeControlParser.confirmation("pause", null))
        assertEquals("STANDBY", NodeControlParser.confirmation("standby", null))
        for (op in listOf("resume", "identify", "diagnostics", "maintenance_on", "maintenance_off")) { assertNull(op, NodeControlParser.confirmation(op, "radar-1")); assertNull(op, NodeControlParser.confirmation(op, null)) }
    }

    @Test fun aDurationIsOnlyForTheOperationsThatTakeOneAndInsideTheirRange() {
        assertTrue(NodeControlParser.durationOk("pause", "")); assertTrue(NodeControlParser.durationOk("pause", "60")); assertTrue(NodeControlParser.durationOk("pause", "3600"))
        assertFalse(NodeControlParser.durationOk("pause", "59")); assertFalse(NodeControlParser.durationOk("pause", "3601"))
        assertTrue(NodeControlParser.durationOk("identify", "5")); assertFalse(NodeControlParser.durationOk("identify", "301"))
        assertTrue(NodeControlParser.durationOk("maintenance_on", "28800")); assertFalse(NodeControlParser.durationOk("maintenance_on", "28801"))
        assertTrue(NodeControlParser.durationOk("restart", "")); assertFalse(NodeControlParser.durationOk("restart", "30")); assertFalse(NodeControlParser.durationOk("standby", "30"))
        assertEquals(600, NodeControlParser.duration("pause", "600")); assertNull(NodeControlParser.duration("pause", "")); assertNull(NodeControlParser.duration("restart", "600")); assertNull(NodeControlParser.duration("pause", "5"))
    }

    @Test fun orderingOneNodeSendsTheBodyTheServerTakesAndNothingElse() {
        server.reply(202, started)
        val operation = client.nodeControl(origin, "radar-1", "restart", "radar-1", null)
        assertEquals("running", operation.state)
        val sent = server.seen.single()
        assertEquals(listOf("POST", "/api/v1/nodes/radar-1/control"), listOf(sent.method, sent.path))
        assertEquals(setOf("operation", "confirm"), JSONObject(sent.body).keys().asSequence().toSet())
        server.reply(202, started)
        client.nodeControl(origin, "alarm-1", "pause", "alarm-1", 600)
        val body = JSONObject(server.seen.last().body)
        assertEquals(setOf("operation", "confirm", "duration_s"), body.keys().asSequence().toSet())
        assertEquals(600, body.getInt("duration_s"))
        // an operation that asks for no confirmation sends none
        server.reply(202, started)
        client.nodeControl(origin, "radar-1", "identify", null, null)
        assertEquals(setOf("operation"), JSONObject(server.seen.last().body).keys().asSequence().toSet())
    }

    @Test fun orderingAllNodesSendsTheModeAndTheWord() {
        server.reply(202, started)
        client.nodesControl(origin, "restart", "rolling", "RESTART", null)
        val sent = server.seen.single()
        assertEquals(listOf("POST", "/api/v1/nodes/control"), listOf(sent.method, sent.path))
        val body = JSONObject(sent.body)
        assertEquals(setOf("operation", "mode", "confirm"), body.keys().asSequence().toSet())
        assertEquals("rolling", body.getString("mode"))
        assertEquals("RESTART", body.getString("confirm"))
    }

    @Test fun theListAndOneOrderAreReadFromTheirRoutes() {
        server.reply(200, """{"operations":[$started]}""")
        assertEquals(1, client.nodeControlOperations(origin).size)
        assertEquals(listOf("GET", "/api/v1/nodes/control/operations"), listOf(server.seen.last().method, server.seen.last().path))
        server.reply(200, started)
        assertEquals("a1b2c3d4e5f60718", client.nodeControlOperation(origin, "a1b2c3d4e5f60718").id)
        assertEquals("/api/v1/nodes/control/operations/a1b2c3d4e5f60718", server.seen.last().path)
    }

    @Test fun aProofOfIdentityThatIsOldIsAskedForAgainAndTheRefusalsAreKnown() {
        server.reply(403, """{"error":"this needs the password or the code again","code":"elevation_required"}""")
        val error = try { client.nodeControl(origin, "radar-1", "restart", "radar-1", null); null } catch (e: ElevationRequiredException) { e }
        assertNotNull(error)
        for ((status, code) in listOf(409 to "already_in_state", 409 to "busy", 429 to "rate_limited", 404 to "unknown_node", 503 to "mqtt_unavailable", 400 to "invalid_request")) {
            server.reply(status, """{"error":"x","code":"$code"}""")
            val refused = try { client.nodesControl(origin, "pause", "now", "PAUSE", null); null } catch (e: ArmorApiException) { e }
            assertEquals(code, refused!!.apiError.code)
            assertEquals(controlRefusalText(code), controlErrorText(refused))
        }
        assertEquals(tr("No se pudo completar."), controlErrorText(RuntimeException("x")))
    }

    @Test fun anAlarmPanelOrARadarPausedOrOnStandbyIsSaidToLeaveTheHouseUnprotected() {
        Tr.useTable("es", emptyMap())
        for (op in listOf("pause", "standby")) {
            assertTrue(op, controlWarning(op, false, "now", true)!!.startsWith("Atención"))
            assertFalse(op, controlWarning(op, false, "now", false)!!.startsWith("Atención"))
        }
        assertTrue(controlWarning("restart", true, "now", true)!!.contains("sin vigilar"))
        assertTrue(controlWarning("restart", false, "now", true)!!.contains("sin vigilar"))
        assertFalse(controlWarning("restart", false, "now", false)!!.contains("sin vigilar"))
        assertTrue(controlWarning("standby", false, "now", false)!!.contains("No es apagarlo"))
        assertNull(controlWarning("maintenance_off", false, "now", false))
    }

    @Test fun everyWordOfTheScreenHasATextInEveryLanguage() {
        Tr.useTable("es", emptyMap())
        val codes = listOf("en", "de", "fr", "it", "ja", "zh", "pt")
        val needed = mutableListOf<String>()
        for (op in NodeControlParser.operations) { needed += controlOperationName(op); controlWarning(op, false, "now", false)?.let { needed += it } }
        needed += tr("Atención: con una central de alarma o un radar así, la casa NO está protegida.")
        controlWarning("restart", true, "rolling", true)?.let { needed += it }
        controlWarning("restart", true, "now", false)?.let { needed += it }
        controlWarning("restart", false, "now", true)?.let { needed += it }
        for (stage in listOf("pending", "armed", "sent", "acknowledged", "applied", "offline", "back", "refused", "no_answer", "not_back")) needed += controlStageName(stage)
        for (state in listOf("running", "paused", "standby")) needed += controlStateName(state, false)
        needed += controlStateName("", true)
        val refusals = listOf("invalid_request", "unknown_node", "busy", "node_unavailable", "no_nodes", "already_in_state", "rate_limited", "mqtt_unavailable", "too_many_nodes", "busy_update", "busy_upload", "busy_slot_copy",
            "busy_battery", "already_restarting", "not_armed", "invalid_token", "token_expired", "unavailable", "anything_else")
        for (code in refusals) needed += controlRefusalText(code)
        assertTrue(needed.size > 40)
        for (code in codes) {
            val table = JSONObject(File("src/main/assets/i18n/$code.json").readText(Charsets.UTF_8))
            val missing = needed.filter { !table.has(it) || table.getString(it).isBlank() }
            assertEquals("$code lacks texts", emptyList<String>(), missing)
        }
    }
}
