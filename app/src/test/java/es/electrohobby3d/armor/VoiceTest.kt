package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.VoiceParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {
    @Test fun `an accepted command that the server carried out`() {
        val reply = VoiceParser.parse(JSONObject("""{"accepted":true,"executed":true,"intent":"arm","outcome":"accepted","speech":"Sistema armado.","result":{"mode":"armed"}}"""))
        assertTrue(reply.accepted && reply.executed)
        assertEquals("arm", reply.intent)
        assertEquals("Sistema armado.", VoiceParser.lineFor(reply))
        assertNull(reply.confirmationToken)
    }

    @Test fun `arming asks for a confirmation first and keeps the token`() {
        val reply = VoiceParser.parse(JSONObject("""{"accepted":false,"executed":false,"intent":"arm","outcome":"confirmation-needed","speech":"Por favor, confirma","confirmation_token":"abc.def"}"""))
        assertFalse(reply.executed)
        assertEquals("abc.def", reply.confirmationToken)
        assertEquals("Por favor, confirma. ¿Armo el sistema?", VoiceParser.lineFor(reply))
        val disarm = reply.copy(intent = "disarm")
        assertEquals("Por favor, confirma. ¿Desarmo el sistema?", VoiceParser.lineFor(disarm))
    }

    @Test fun `silencing in the challenge mode asks for the signed confirmation, never less, because the phone shows no number`() {
        val reply = VoiceParser.parse(JSONObject("""{"accepted":false,"executed":false,"intent":"silence","outcome":"confirmation-needed","speech":"Confirma","confirmation_token":"t.k"}"""))
        assertFalse(reply.accepted || reply.executed)
        assertEquals("t.k", reply.confirmationToken)
        assertEquals("Confirma. ¿Lo hago?", VoiceParser.lineFor(reply))
    }

    @Test fun `the number of the challenge of a spoken turn is read by the contract and kept out of the reply`() {
        val turn = es.electrohobby3d.armor.contracts.parseVoiceTurnResult(JSONObject("""{"heard":"silencia","reply":"Di el numero que ves en el panel","challenge":"48"}"""))
        assertEquals("48", turn?.challenge)
        assertFalse(turn!!.reply.contains("48"))
        assertNull(es.electrohobby3d.armor.contracts.parseVoiceTurnResult(JSONObject("""{"heard":"a","reply":"b","challenge":"4"}""")))
        assertNull(es.electrohobby3d.armor.contracts.parseVoiceTurnResult(JSONObject("""{"heard":"a","reply":"b"}"""))!!.challenge)
    }

    @Test fun `what is not a command says what can be asked`() {
        val reply = VoiceParser.parse(JSONObject("""{"accepted":false,"executed":false,"intent":null,"outcome":"not-understood","speech":"Orden no reconocida"}"""))
        assertNull(reply.intent)
        assertTrue(VoiceParser.lineFor(reply).startsWith("No lo he entendido"))
    }

    @Test fun `the code of an error is read from the text of the exception`() {
        assertEquals("voice_unavailable", VoiceParser.errorCode("""Server operation failed (HTTP 503): {"error":"voice_unavailable"}"""))
        assertNull(VoiceParser.errorCode("no code here"))
        assertNull(VoiceParser.errorCode(null))
    }

    @Test fun `every error is told in words`() {
        assertEquals("Este sistema no tiene instalado el asistente de voz.", VoiceParser.errorText("""HTTP 503: {"error":"voice_unavailable"}"""))
        assertEquals("El asistente de voz no responde ahora mismo.", VoiceParser.errorText("""HTTP 503: {"error":"voice_not_answering"}"""))
        assertEquals("La orden es demasiado larga.", VoiceParser.errorText("""HTTP 422: {"error":"text_too_long"}"""))
        assertTrue(VoiceParser.errorText("Server operation failed (HTTP 401)").startsWith("La sesión ha caducado"))
        assertEquals("No se pudo enviar la orden al servidor.", VoiceParser.errorText("boom"))
    }
}
