package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.MachineUpdateParser
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

/** Updating the programs of the machine from the phone: what the server answers is read as it is, the calls are the ones the server has, the name typed again is what the server asks for, and every word has a text in the eight languages. */
class MachineUpdateTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val origin get() = server.origin

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(origin); server.stop() }

    private val plan = "0123456789abcdef01234567"

    @Test fun theSwitchAndTheProgramsAreReadAndAProgramOutsideTheContractIsLeftOut() {
        val on = MachineUpdateParser.state(JSONObject("""{"enabled":true,"available":true,"require_signed":true,"projects":["ARMOR-SERVER","ARMOR-STUDIO","not a project","../etc"],"reason":""}"""))
        assertTrue(on.enabled)
        assertTrue(on.requireSigned)
        assertEquals(listOf("ARMOR-SERVER", "ARMOR-STUDIO"), on.projects)
        val off = MachineUpdateParser.state(JSONObject("""{"enabled":false,"reason":"switched_off"}"""))
        assertFalse(off.enabled)
        assertEquals("switched_off", off.reason)
        assertTrue(off.projects.isEmpty())
        // the safe reading: what the server does not say is off, and the signature is required
        val empty = MachineUpdateParser.state(JSONObject("{}"))
        assertFalse(empty.enabled)
        assertTrue(empty.requireSigned)
    }

    @Test fun thePreviewCarriesThePlanOnlyWhenItIsAValidOneAndAnUpdateIsAvailable() {
        val outdated = MachineUpdateParser.preview(JSONObject("""{"project":"ARMOR-SERVER","installed":true,"local_version":"0.8.1.0","github_version":"0.8.2.0","state":"outdated","update_available":true,"plan_id":"$plan","expires_in_s":300}"""))
        assertTrue(outdated.updateAvailable)
        assertEquals(plan, outdated.planId)
        assertEquals(300, outdated.expiresInS)
        val current = MachineUpdateParser.preview(JSONObject("""{"project":"ARMOR-SERVER","local_version":"0.8.2.0","github_version":"0.8.2.0","update_available":false}"""))
        assertFalse(current.updateAvailable)
        assertEquals("", current.planId)
        // a plan that is not 24 hex digits is dropped: the phone never sends what the server would not hand out
        assertEquals("", MachineUpdateParser.preview(JSONObject("""{"project":"ARMOR-SERVER","update_available":true,"plan_id":"../../x"}""")).planId)
        assertEquals("", MachineUpdateParser.preview(JSONObject("""{"project":"ARMOR-SERVER","update_available":true,"plan_id":null}""")).planId)
    }

    @Test fun theNameTypedAgainMustBeExactlyTheProgramsAndTheCodeEmptyOrSixDigits() {
        assertTrue(MachineUpdateParser.confirmationOk("ARMOR-SERVER", "ARMOR-SERVER"))
        assertFalse(MachineUpdateParser.confirmationOk("ARMOR-SERVER", "armor-server"))
        assertFalse(MachineUpdateParser.confirmationOk("ARMOR-SERVER", "ARMOR-SERVER "))
        assertFalse(MachineUpdateParser.confirmationOk("ARMOR-SERVER", ""))
        assertFalse(MachineUpdateParser.confirmationOk("server", "server"))
        assertTrue(MachineUpdateParser.codeOk(""))
        assertTrue(MachineUpdateParser.codeOk("012345"))
        assertFalse(MachineUpdateParser.codeOk("12345"))
        assertFalse(MachineUpdateParser.codeOk("1234567"))
        assertFalse(MachineUpdateParser.codeOk("12345a"))
        assertTrue(MachineUpdateParser.isPlan(plan))
        assertFalse(MachineUpdateParser.isPlan("ABCDEF0123456789abcdef01"))
    }

    @Test fun thePlanCountsDownAndNeverGoesBelowZero() {
        assertEquals(300, MachineUpdateParser.secondsLeft(300, 0))
        assertEquals(290, MachineUpdateParser.secondsLeft(300, 10_400))
        assertEquals(0, MachineUpdateParser.secondsLeft(300, 400_000))
        assertEquals(300, MachineUpdateParser.secondsLeft(300, -5_000))
    }

    @Test fun theJobIsReadWithItsStepsAndTheLogIsMergedWithoutRepeatingLines() {
        val job = MachineUpdateParser.job(JSONObject("""{"id":"j1","state":"running","percent":140,"steps":[{"id":"server.download","state":"done","percent":100},{"id":"server.install","state":"running","percent":30},{"state":"x"}],"error":"","log":["a","b"],"log_total":2}"""))
        assertTrue(job.running)
        assertEquals(100.0, job.percent, 0.0)
        assertEquals(listOf("server.download", "server.install"), job.steps.map { it.id })
        assertEquals(2, job.logTotal)
        assertEquals(listOf("a", "b", "c"), MachineUpdateParser.mergeLog(listOf("a", "b"), 2, listOf("c")))
        assertEquals(listOf("a", "x", "y"), MachineUpdateParser.mergeLog(listOf("a", "b"), 1, listOf("x", "y")))
        assertEquals(listOf("a", "b"), MachineUpdateParser.mergeLog(listOf("a", "b"), 2, emptyList()))
        val failed = MachineUpdateParser.job(JSONObject("""{"id":"j1","state":"failed","error":"the signature is not valid"}"""))
        assertTrue(failed.failed)
        assertFalse(failed.running)
        assertEquals("download", MachineUpdateParser.stepPart("server.download"))
        assertEquals("check", MachineUpdateParser.stepPart("check"))
    }

    @Test fun theCallsAreTheOnesTheServerHasAndSendOnlyWhatItTakes() {
        server.reply(200, """{"enabled":true,"available":true,"require_signed":true,"projects":["ARMOR-SERVER"],"reason":""}""")
        assertEquals(listOf("ARMOR-SERVER"), client.machineUpdateState(origin).projects)
        assertEquals(listOf("GET", "/api/v1/admin/machine-update"), listOf(server.seen.last().method, server.seen.last().path))

        server.reply(200, """{"project":"ARMOR-SERVER","local_version":"0.8.1.0","github_version":"0.8.2.0","update_available":true,"plan_id":"$plan","expires_in_s":300}""")
        assertEquals(plan, client.machineUpdatePreview(origin, "ARMOR-SERVER").planId)
        assertEquals(listOf("POST", "/api/v1/admin/machine-update/preview"), listOf(server.seen.last().method, server.seen.last().path))
        assertEquals(setOf("project"), JSONObject(server.seen.last().body).keys().asSequence().toSet())

        server.reply(202, """{"id":"job-1"}""")
        assertEquals("job-1", client.machineUpdateApply(origin, "ARMOR-SERVER", plan, "ARMOR-SERVER", "123456"))
        val applied = JSONObject(server.seen.last().body)
        assertEquals("/api/v1/admin/machine-update/apply", server.seen.last().path)
        assertEquals(setOf("project", "plan_id", "confirm", "totp"), applied.keys().asSequence().toSet())
        assertEquals("ARMOR-SERVER", applied.getString("confirm"))
        // without a code none is sent, and an empty one is not a code
        server.reply(202, """{"id":"job-2"}""")
        client.machineUpdateApply(origin, "ARMOR-SERVER", plan, "ARMOR-SERVER", null)
        assertEquals(setOf("project", "plan_id", "confirm"), JSONObject(server.seen.last().body).keys().asSequence().toSet())
        client.machineUpdateApply(origin, "ARMOR-SERVER", plan, "ARMOR-SERVER", "")
        assertFalse(JSONObject(server.seen.last().body).has("totp"))

        server.reply(200, """{"id":"job-1","state":"done","percent":100,"steps":[],"log":["ok"],"log_total":1}""")
        assertEquals("done", client.machineUpdateJob(origin, "job-1", 0).state)
        assertEquals("/api/v1/admin/machine-update/jobs/job-1", server.seen.last().path)
        assertEquals("since=0", server.seen.last().query)
        client.machineUpdateJob(origin, "job-1", -4)
        assertEquals("since=0", server.seen.last().query)
    }

    @Test fun aProofOfIdentityThatIsOldIsAskedForAgainAndEveryRefusalHasItsWords() {
        server.reply(403, """{"error":"this needs the password or the code again","code":"elevation_required"}""")
        val needed = try { client.machineUpdateApply(origin, "ARMOR-SERVER", plan, "ARMOR-SERVER", null); null } catch (e: ElevationRequiredException) { e }
        assertNotNull(needed)
        val expected = listOf(
            Triple(403, "machine_update_disabled", "La actualización desde el móvil está apagada en el servidor o en el ayudante de mantenimiento."),
            Triple(404, "no_plan", "El plan ya no existe o ya se usó. Comprueba las versiones otra vez."),
            Triple(410, "plan_expired", "El plan ha caducado. Comprueba las versiones otra vez."),
            Triple(409, "busy", "Ya hay otro trabajo en marcha en el equipo."),
            Triple(422, "confirmation_mismatch", "El nombre escrito no coincide con el del programa."),
            Triple(422, "bad_code", "El código de un solo uso tiene seis cifras."),
            Triple(422, "bad_plan", "El plan no es válido. Comprueba las versiones otra vez."),
            Triple(422, "unknown_project", "Ese programa no está en la lista de los que se pueden actualizar."),
            Triple(429, "too_soon", "Se actualizó otro programa hace un momento. Espera unos minutos."),
            Triple(503, "maintenance_unavailable", "El ayudante de mantenimiento no está instalado o no contesta."),
        )
        Tr.useTable("es", emptyMap())
        for ((status, error, words) in expected) {
            server.reply(status, """{"error":"$error"}""")
            val refused = try { client.machineUpdateApply(origin, "ARMOR-SERVER", plan, "ARMOR-SERVER", null); null } catch (e: ArmorApiException) { e }
            assertEquals(status, refused!!.apiError.status)
            assertEquals(error, refused.apiError.detail)
            assertEquals(words, machineUpdateErrorText(refused))
        }
        assertEquals(tr("No se pudo completar."), machineUpdateErrorText(RuntimeException("x")))
    }

    @Test fun everyWordOfTheScreenHasATextInEveryLanguage() {
        Tr.useTable("es", emptyMap())
        val codes = listOf("en", "de", "fr", "it", "ja", "zh", "pt")
        val needed = mutableListOf<String>()
        for (reason in listOf("switched_off", "not_configured", "agent_switched_off", "not_answering", "refused", "anything_else")) needed += machineUpdateReasonText(reason)
        for (status in listOf(403, 404, 410, 409, 422, 429, 503, 500)) for (detail in listOf("machine_update_disabled", "confirmation_mismatch", "bad_code", "bad_plan", "")) needed += machineUpdateRefusalText(status, detail)
        for (part in listOf("download", "verify", "install", "stage", "restart", "check")) needed += machineStepName("server.$part")
        assertTrue(needed.size > 40)
        for (code in codes) {
            val table = JSONObject(File("src/main/assets/i18n/$code.json").readText(Charsets.UTF_8))
            val missing = needed.filter { !table.has(it) || table.getString(it).isBlank() }
            assertEquals("$code lacks texts", emptyList<String>(), missing)
        }
    }
}
