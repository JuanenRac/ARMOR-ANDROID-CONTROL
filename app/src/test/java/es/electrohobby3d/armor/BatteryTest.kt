package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.BatteryParser
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.ElevationRequiredException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** Writing to a battery from the phone: what the server answers is read as it is, the calls are the ones the server has, and every refusal has words in the eight languages. */
class BatteryTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val origin get() = server.origin

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(origin); server.stop() }

    private val armed = """{"id":"a1b2c3d4e5f60718","node":"solar-1","port":2,"op":"output_set","params":{"output":"cfet","state":"off"},"actor":"ana","created_at":"2026-01-01T10:00:00Z","updated_at":"2026-01-01T10:00:05Z",
        "stage":"armed","summary":"switch cfet off","risk":"medium","atomic":false,"canonical":"output_set cfet off","battery":{"model":"US3000C","serial":"PPTBH02200409223","soft_version":"V3.0","board":""},
        "armed_until":"2026-01-01T10:02:00Z"}"""

    @Test fun anOperationIsReadWithItsBatteryRiskAndProgress() {
        val operation = BatteryParser.operation(JSONObject(armed))
        assertEquals(listOf("a1b2c3d4e5f60718", "solar-1", 2, "output_set", "armed"), listOf(operation.id, operation.node, operation.port, operation.op, operation.stage))
        assertEquals(listOf("US3000C", "PPTBH02200409223", "V3.0", "medium", false), listOf(operation.model, operation.serial, operation.softVersion, operation.risk, operation.atomic))
        assertTrue(operation.active)
        assertEquals(0, operation.percent)
        val running = BatteryParser.operation(JSONObject("""{"id":"x","stage":"running","steps_done":3,"steps_total":9,"atomic":true}"""))
        assertEquals(33, running.percent)
        assertEquals(25, BatteryParser.operation(JSONObject("""{"id":"x","stage":"running","steps_done":3,"steps_total":9,"packets_sent":50,"packets_total":200}""")).percent)
        val done = BatteryParser.operation(JSONObject("""{"id":"x","stage":"done","outcome":"ok","diff":[{"label":"output","before":"cfet : ON","after":"cfet : OFF"}],"restore":{"op":"output_set","params":{}}}"""))
        assertFalse(done.active)
        assertEquals(100, done.percent)
        assertEquals(listOf("output"), done.diff.map { it.label })
        assertTrue(done.restorable)
    }

    @Test fun theListIsReadAndAnEntryWithoutAnIdIsLeftOut() {
        val overview = BatteryParser.overview(JSONObject("""{"enabled":true,"operations":[$armed,{"stage":"done"}]}"""))
        assertTrue(overview.enabled)
        assertEquals(1, overview.operations.size)
        assertFalse(BatteryParser.overview(JSONObject("{}")).enabled)
        assertEquals(emptyList<String>(), BatteryParser.overview(JSONObject("{}")).operations.map { it.id })
    }

    @Test fun theNodesToChooseFromAreTheOnesWhoseBatteriesReport() {
        assertEquals(emptyList<String>(), BatteryParser.nodes(null))
    }

    @Test fun aSerialNumberIsTakenTheWayTheNodeTakesIt() {
        for (good in listOf("PPTBH02200409223", "H221014C32213244", "NOSERIAL", "ABC-12345_xyz")) assertTrue(good, BatteryParser.serialLooksRight(good))
        for (bad in listOf("", "short", "has space 12345", "x".repeat(30), "noserial")) assertFalse(bad, BatteryParser.serialLooksRight(bad))
    }

    @Test fun thePhoneOffersOnlyTheOperationsThatNeedNoMoreThanAChoice() {
        assertEquals(9, BatteryParser.phoneOperations.size)
        for (not in listOf("limits_set", "protections_set", "firmware_update", "write_barcode", "eeprom_erase", "soc_reset", "pcb_setup", "expert_command", "clear_data")) assertFalse(not, not in BatteryParser.phoneOperations)
        assertEquals(listOf("cfet", "dfet", "buzz", "heat"), BatteryParser.outputs)
    }

    @Test fun armingSendsTheOrderTheServerTakesAndNothingElse() {
        server.reply(202, armed)
        val operation = client.batteryArm(origin, "solar-1", 2, "output_set", "cfet", "off", "PPTBH02200409223")
        assertEquals("armed", operation.stage)
        val sent = server.seen.single()
        assertEquals(listOf("POST", "/api/v1/solar/battery-ops"), listOf(sent.method, sent.path))
        val body = JSONObject(sent.body)
        assertEquals(setOf("node", "port", "op", "params", "serial"), body.keys().asSequence().toSet())
        assertEquals(2, body.getInt("port"))
        assertEquals("cfet", body.getJSONObject("params").getString("output"))
        // an operation without values sends none
        server.reply(202, armed)
        client.batteryArm(origin, "solar-1", 2, "set_clock", null, null, "NOSERIAL")
        assertEquals(0, JSONObject(server.seen.last().body).getJSONObject("params").length())
    }

    @Test fun executingAndCancellingNameTheOperationAndTheSerial() {
        server.reply(202, armed)
        client.batteryExecute(origin, "a1b2c3d4e5f60718", "PPTBH02200409223")
        assertEquals("/api/v1/solar/battery-ops/a1b2c3d4e5f60718/execute", server.seen.last().path)
        assertEquals("PPTBH02200409223", JSONObject(server.seen.last().body).getString("serial"))
        client.batteryCancel(origin, "a1b2c3d4e5f60718")
        assertEquals("/api/v1/solar/battery-ops/a1b2c3d4e5f60718/cancel", server.seen.last().path)
        server.reply(200, armed)
        assertEquals("armed", client.batteryOperation(origin, "a1b2c3d4e5f60718").stage)
        assertEquals("GET", server.seen.last().method)
    }

    @Test fun aProofOfIdentityThatIsOldIsAskedForAgainAndTheRefusalsAreKnown() {
        server.reply(403, """{"error":"this needs the password or the code again","code":"elevation_required"}""")
        val error = try { client.batteryArm(origin, "solar-1", 2, "set_clock", null, null, "NOSERIAL"); null } catch (e: ElevationRequiredException) { e }
        assertNotNull(error)
        server.reply(409, """{"error":"busy","code":"busy"}""")
        val busy = try { client.batteryArm(origin, "solar-1", 2, "set_clock", null, null, "NOSERIAL"); null } catch (e: ArmorApiException) { e }
        assertEquals("busy", busy!!.apiError.code)
        assertEquals(batteryRefusalText("busy"), batteryErrorText(busy))
    }

    @Test fun everyWordOfTheScreenHasATextInEveryLanguage() {
        Tr.useTable("es", emptyMap())
        val codes = listOf("en", "de", "fr", "it", "ja", "zh", "pt")
        val needed = mutableListOf<String>()
        for (op in BatteryParser.phoneOperations + "other") needed += batteryOperationName(op)
        for (output in BatteryParser.outputs) needed += batteryOutputName(output)
        for (stage in listOf("arming", "armed", "starting", "running", "done", "rejected", "expired", "lost", "withdrawn")) needed += batteryStageName(stage)
        for (risk in listOf("low", "medium", "high", "irreversible")) needed += batteryRiskName(risk)
        val refusals = listOf("battery_write_disabled", "disabled", "unknown_node", "invalid_request", "invalid_params", "busy", "serial_mismatch", "serial_unreadable", "not_armed", "expired", "bad_token", "mqtt_unavailable", "rate_limited",
            "no_password", "login_failed", "not_idle", "critical_alarm", "cooldown", "locked", "no_battery", "not_pylontech", "no_answer", "preflight_failed", "clock_not_set", "no_official_value", "anything_else")
        for (code in refusals) needed += batteryRefusalText(code)
        assertTrue(needed.size > 40)
        for (code in codes) {
            val table = JSONObject(File("src/main/assets/i18n/$code.json").readText(Charsets.UTF_8))
            val missing = needed.filter { !table.has(it) || table.getString(it).isBlank() }
            assertEquals("$code lacks texts", emptyList<String>(), missing)
        }
    }
}
