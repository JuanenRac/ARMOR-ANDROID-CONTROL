package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SpokenMemoryStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

private class SpokenBox(private val tag: String = "A") : SecretBox {
    override fun seal(plain: String) = "$tag:" + plain.reversed()
    override fun open(sealed: String): String? = if (sealed.startsWith("$tag:")) sealed.removePrefix("$tag:").reversed() else null
}

/** A microphone that counts how often it was asked and gives a fixed sound. */
private class CountingRecorder(var granted: Boolean = true, var sound: ByteArray = ByteArray(6_400), var refuseAtStart: Boolean = false) : TurnRecorder {
    var permittedCalls = 0
    var recordCalls = 0
    override fun permitted(): Boolean { permittedCalls++; return granted }
    override fun record(shouldStop: () -> Boolean): ByteArray {
        recordCalls++
        if (refuseAtStart) throw SecurityException("revoked")
        return sound
    }
}

private class ListAudit : TurnAudit {
    val events = mutableListOf<String>()
    override fun record(event: String) { events += event }
}

class SpokenTurnTest {
    private val store = SpokenMemoryStore()
    private val settings = SpokenTurnSettings(store)
    private val tokens = VoiceTokenVault(SpokenMemoryStore(), SpokenBox())
    private val recorder = CountingRecorder()
    private val audit = ListAudit()
    private var sent = 0
    private var sentSize = -1
    private var answer: () -> JSONObject = { JSONObject("""{"heard":"silencia la alarma","reply":"Dime el número del panel"}""") }
    private var now = 1_000L
    private val controller = SpokenTurnController(settings, tokens, recorder, { _, pcm, _, _ -> sent++; sentSize = pcm.size; answer() }, audit, { now })

    private fun ready() { settings.enabled = true; assertTrue(tokens.save("a-voice-token-1234")) }

    @Test fun `the switch is off until the person turns it on`() {
        assertFalse(settings.enabled)
        settings.enabled = true
        assertTrue(SpokenTurnSettings(store).enabled)
        settings.enabled = false
        assertFalse(SpokenTurnSettings(store).enabled)
    }

    @Test fun `with the switch off the microphone is never opened, the permission never asked and nothing is sent`() {
        tokens.save("a-voice-token-1234")
        assertEquals(SpokenTurnOutcome.Off, controller.run("http://h", "es"))
        assertEquals(0, recorder.permittedCalls)
        assertEquals(0, recorder.recordCalls)
        assertEquals(0, sent)
        assertEquals(listOf("blocked_switch_off"), audit.events)
    }

    @Test fun `without a voice token nothing is recorded`() {
        settings.enabled = true
        assertEquals(SpokenTurnOutcome.NoToken, controller.run("http://h", "es"))
        assertEquals(0, recorder.recordCalls)
        assertEquals(0, sent)
        assertEquals(listOf("no_token"), audit.events)
    }

    @Test fun `a denied microphone permission ends the turn before anything is recorded or sent`() {
        ready()
        recorder.granted = false
        assertEquals(SpokenTurnOutcome.PermissionDenied, controller.run("http://h", "es"))
        assertEquals(0, recorder.recordCalls)
        assertEquals(0, sent)
        assertEquals("permission_denied", audit.events.last())
    }

    @Test fun `a permission taken away at the moment of recording is a refusal too`() {
        ready()
        recorder.refuseAtStart = true
        assertEquals(SpokenTurnOutcome.PermissionDenied, controller.run("http://h", "es"))
        assertEquals(0, sent)
    }

    @Test fun `the refusal of the permission dialog is written down and gives the same outcome`() {
        ready()
        assertEquals(SpokenTurnOutcome.PermissionDenied, controller.permissionRefused())
        assertEquals(listOf("permission_denied"), audit.events)
        assertNotNull(SpokenTurnText.notice(SpokenTurnOutcome.PermissionDenied))
    }

    @Test fun `a tap or half a sample is nothing heard and is not sent`() {
        ready()
        for (sound in listOf(ByteArray(0), ByteArray(100), ByteArray(3_199), ByteArray(6_401))) {
            recorder.sound = sound
            assertEquals(SpokenTurnOutcome.NothingHeard, controller.run("http://h", "es"))
        }
        assertEquals(0, sent)
    }

    @Test fun `a turn is sent once and the answer is read`() {
        ready()
        answer = { JSONObject("""{"heard":"estado","reply":"Todo en orden"}""") }
        val outcome = controller.run("http://h", "es") as SpokenTurnOutcome.Answered
        assertEquals("estado", outcome.heard)
        assertEquals("Todo en orden", outcome.reply)
        assertNull(outcome.challenge)
        assertEquals(1, sent)
        assertEquals(listOf("sent", "answered"), audit.events)
        assertNull(SpokenTurnText.notice(outcome))
    }

    @Test fun `the sound is cut at ten seconds`() {
        ready()
        recorder.sound = ByteArray(400_000)
        controller.run("http://h", "es")
        assertEquals(SpokenTurnController.MAX_BYTES, sentSize)
    }

    @Test fun `the number of the challenge is shown from the moment it arrives`() {
        ready()
        answer = { JSONObject("""{"heard":"silencia","reply":"Di el número que ves en el panel","challenge":"4821"}""") }
        now = 5_000
        val outcome = controller.run("http://h", "es") as SpokenTurnOutcome.Answered
        assertEquals(ShownChallenge("4821", 5_000), outcome.challenge)
        assertEquals("answered_challenge", audit.events.last())
        assertFalse(outcome.reply.contains("4821"))
    }

    @Test fun `the number expires after nineteen seconds and not before`() {
        val shown = ShownChallenge("4821", 10_000)
        assertFalse(shown.expired(10_000))
        assertFalse(shown.expired(28_999))
        assertTrue(shown.expired(29_000))
        assertTrue(shown.expired(60_000))
        assertEquals(19_000L, shown.remainingMs(10_000))
        assertEquals(1L, shown.remainingMs(28_999))
        assertEquals(0L, shown.remainingMs(29_000))
    }

    @Test fun `a clock that goes back makes the number expire instead of live longer`() {
        assertTrue(ShownChallenge("4821", 10_000).expired(9_000))
    }

    @Test fun `a number that is not two to six plain digits fails closed`() {
        ready()
        for (bad in listOf("4", "1234567", "12a", "٣٤", "-12", " 12", "12 ", "12\n", "1.5", "", "０１２")) {
            answer = { JSONObject().put("heard", "silencia").put("reply", "x").put("challenge", bad) }
            assertEquals("challenge '$bad'", SpokenTurnOutcome.Malformed, controller.run("http://h", "es"))
        }
        assertTrue(audit.events.contains("malformed_answer"))
        assertNotNull(SpokenTurnText.notice(SpokenTurnOutcome.Malformed))
    }

    @Test fun `a number that is not a string fails closed`() {
        ready()
        answer = { JSONObject("""{"heard":"a","reply":"b","challenge":4821}""") }
        assertEquals(SpokenTurnOutcome.Malformed, controller.run("http://h", "es"))
        answer = { JSONObject("""{"heard":"a","reply":"b","challenge":null}""") }
        assertEquals(SpokenTurnOutcome.Malformed, controller.run("http://h", "es"))
    }

    @Test fun `an answer that is not a voice reply fails closed`() {
        ready()
        for (text in listOf("{}", """{"heard":"a"}""", """{"heard":1,"reply":"b"}""", """{"heard":"a","reply":"b","extra":1}""", """{"heard":"a","reply":"b","audio_omitted":"always"}""")) {
            answer = { JSONObject(text) }
            assertEquals(text, SpokenTurnOutcome.Malformed, controller.run("http://h", "es"))
        }
    }

    @Test fun `the digits of the number are the only thing ChallengeNumber lets through`() {
        assertEquals("48", ChallengeNumber.parse("48"))
        assertEquals("123456", ChallengeNumber.parse("123456"))
        assertNull(ChallengeNumber.parse(null))
        assertNull(ChallengeNumber.parse("1"))
        assertNull(ChallengeNumber.parse("1234567"))
    }

    @Test fun `a refusal of the server is told by its code and keeps the person out of a wrong shown number`() {
        ready()
        answer = { throw ArmorApiException("Server operation failed (HTTP 401): {\"error\":\"unauthorized\"}", 401) }
        val failed = controller.run("http://h", "es") as SpokenTurnOutcome.Failed
        assertEquals("unauthorized", failed.code)
        assertEquals("El servidor no acepta la clave de voz.", SpokenTurnText.notice(failed))
        assertEquals("failed:unauthorized", audit.events.last())
        answer = { throw java.io.IOException("timeout") }
        val lost = controller.run("http://h", "es") as SpokenTurnOutcome.Failed
        assertNull(lost.code)
        assertEquals("failed:network", audit.events.last())
        assertNotNull(SpokenTurnText.notice(lost))
        answer = { throw ArmorApiException("HTTP 503: {\"error\":\"voice_unavailable\"}", 503) }
        assertEquals("Este sistema no tiene instalado el asistente de voz.", SpokenTurnText.notice(controller.run("http://h", "es") as SpokenTurnOutcome.Failed))
    }

    @Test fun `every outcome that did not work has words`() {
        for (outcome in listOf(SpokenTurnOutcome.Off, SpokenTurnOutcome.NoToken, SpokenTurnOutcome.PermissionDenied, SpokenTurnOutcome.NothingHeard, SpokenTurnOutcome.Malformed, SpokenTurnOutcome.Failed("x"))) {
            assertTrue(outcome.toString(), !SpokenTurnText.notice(outcome).isNullOrBlank())
        }
    }

    @Test fun `the ui state knows when the microphone or the line is busy`() {
        assertFalse(SpokenTurnUiState().active)
        assertTrue(SpokenTurnUiState(listening = true).active)
        assertTrue(SpokenTurnUiState(sending = true).active)
    }

    // ---- the voice token

    @Test fun `a voice token is kept sealed and read back`() {
        val backing = SpokenMemoryStore()
        val vault = VoiceTokenVault(backing, SpokenBox())
        assertFalse(vault.has())
        assertTrue(vault.save("  a-voice-token-1234  "))
        assertEquals("a-voice-token-1234", vault.load())
        assertTrue(backing.values.values.none { "voice-token" in it })
        vault.clear()
        assertFalse(vault.has())
        assertNull(vault.load())
    }

    @Test fun `a token that could not be a header value is not kept`() {
        val vault = VoiceTokenVault(SpokenMemoryStore(), SpokenBox())
        for (bad in listOf("", "short", "has space inside1", "tab\tinside1234", "new\nline12345", "ñandú-token-123", "x".repeat(201))) assertFalse(bad, vault.save(bad))
        assertFalse(vault.has())
        assertTrue(vault.save("x".repeat(200)))
    }

    @Test fun `a token sealed by another key is dropped and a locked phone leaves it alone`() {
        val backing = SpokenMemoryStore()
        VoiceTokenVault(backing, SpokenBox("A")).save("a-voice-token-1234")
        assertNull(VoiceTokenVault(backing, SpokenBox("B")).load())
        assertFalse(backing.values.containsKey(VoiceTokenVault.KEY))
        VoiceTokenVault(backing, SpokenBox("A")).save("a-voice-token-1234")
        val locked = object : SecretBox {
            override fun seal(plain: String) = plain
            override fun open(sealed: String): String? = null
            override fun usable() = false
        }
        assertNull(VoiceTokenVault(backing, locked).load())
        assertTrue(backing.values.containsKey(VoiceTokenVault.KEY))
    }

    // ---- the audit trail

    @Test fun `the audit trail keeps the last fifty events and no words`() {
        var clock = 0L
        val trail = StoreTurnAudit(SpokenMemoryStore(), { ++clock })
        repeat(60) { trail.record("event_$it") }
        val entries = trail.entries()
        assertEquals(StoreTurnAudit.MAX_ENTRIES, entries.size)
        assertEquals("11|event_10", entries.first())
        assertEquals("60|event_59", entries.last())
        trail.record("heard: silencia\nla alarma, 1234")
        assertEquals("61|heard:silencialaalarma1234".take(40), trail.entries().last())
        assertFalse(trail.entries().last().contains(" "))
    }

    // ---- the client: what goes on the wire

    @Test fun `the client sends the sound raw with the voice token and not the session`() {
        val server = FakeServer()
        val client = ArmorApiClient()
        try {
            server.reply(200, """{"heard":"estado","reply":"Todo en orden","challenge":"73"}""")
            val pcm = ByteArray(1_000) { 'A'.code.toByte() }
            val json = client.voiceTurn(server.origin, pcm, "es", "a-voice-token-1234")
            val seen = server.seen.single()
            assertEquals("POST", seen.method)
            assertEquals("/api/v1/voice/turn", seen.path)
            assertEquals("a-voice-token-1234", seen.headers["x-armor-voice-token"])
            assertEquals("es", seen.headers["x-armor-language"])
            assertEquals("audio/L16;rate=16000", seen.headers["content-type"])
            assertEquals(1_000, seen.body.length)
            assertEquals("73", json.getString("challenge"))
        } finally { client.forgetSession(server.origin); server.stop() }
    }

    @Test fun `the client raises the refusal of the server with its code`() {
        val server = FakeServer()
        val client = ArmorApiClient()
        try {
            server.reply(401, """{"error":"unauthorized"}""")
            val error = try { client.voiceTurn(server.origin, ByteArray(10), "es", "a-voice-token-1234"); null } catch (e: ArmorApiException) { e }
            assertNotNull(error)
            assertEquals("unauthorized", es.electrohobby3d.armor.model.VoiceParser.errorCode(error!!.message))
        } finally { client.forgetSession(server.origin); server.stop() }
    }

    // ---- with the session of the person, when no voice token was entered

    private fun sessionController(send: SessionTurnSender) =
        SpokenTurnController(settings, tokens, recorder, { _, _, _, _ -> sent++; answer() }, audit, { now }, send)

    @Test fun `without a voice token the turn goes with the session when the route of the phone is given`() {
        settings.enabled = true
        var viaSession = 0
        val withSession = sessionController { _, pcm, language -> viaSession++; sentSize = pcm.size; assertEquals("es", language); answer() }
        val outcome = withSession.run("http://h", "es")
        assertTrue(outcome is SpokenTurnOutcome.Answered)
        assertEquals(1, viaSession)
        assertEquals(0, sent)
        assertEquals(listOf("sent", "answered"), audit.events)
    }

    @Test fun `with a voice token the token is still used and the session is not`() {
        ready()
        var viaSession = 0
        val withSession = sessionController { _, _, _ -> viaSession++; answer() }
        assertTrue(withSession.run("http://h", "es") is SpokenTurnOutcome.Answered)
        assertEquals(1, sent)
        assertEquals(0, viaSession)
    }

    @Test fun `the switch and the permission are still checked before the session is used`() {
        var viaSession = 0
        val withSession = sessionController { _, _, _ -> viaSession++; answer() }
        assertEquals(SpokenTurnOutcome.Off, withSession.run("http://h", "es"))
        settings.enabled = true
        recorder.granted = false
        assertEquals(SpokenTurnOutcome.PermissionDenied, withSession.run("http://h", "es"))
        assertEquals(0, viaSession)
        assertEquals(0, recorder.recordCalls)
    }

    @Test fun `a server that keeps the voice of the phone closed is told in words and fails closed`() {
        settings.enabled = true
        val refused = sessionController { _, _, _ -> throw ArmorApiException("403 {\"error\":\"mobile_voice_disabled\"}") }
        val outcome = refused.run("http://h", "es")
        assertTrue(outcome is SpokenTurnOutcome.Failed)
        assertEquals("mobile_voice_disabled", (outcome as SpokenTurnOutcome.Failed).code)
        assertTrue(SpokenTurnText.notice(outcome)!!.contains("Studio"))
        assertEquals(listOf("sent", "failed:mobile_voice_disabled"), audit.events)
    }

    @Test fun `the client sends the sound raw with the session and without a voice token to the route of the phone`() {
        val server = FakeServer()
        val client = ArmorApiClient()
        try {
            server.reply(200, """{"heard":"estado","reply":"Todo en orden"}""")
            val json = client.mobileVoiceTurn(server.origin, ByteArray(1_000) { 'A'.code.toByte() }, "es")
            val seen = server.seen.single()
            assertEquals("POST", seen.method)
            assertEquals("/api/v1/voice/mobile-turn", seen.path)
            assertNull(seen.headers["x-armor-voice-token"])
            assertEquals("es", seen.headers["x-armor-language"])
            assertEquals("audio/L16;rate=16000", seen.headers["content-type"])
            assertEquals(1_000, seen.body.length)
            assertEquals("estado", json.getString("heard"))
        } finally { client.forgetSession(server.origin); server.stop() }
    }
}
