package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.VoiceParser
import es.electrohobby3d.armor.model.optStr
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonTextTest {
    @Test fun aJsonNullIsEmptyAndNeverTheWordNull() {
        val json = JSONObject("""{"a":null,"b":"text","c":5}""")
        assertEquals("", json.optStr("a"))
        assertEquals("fallback", json.optStr("a", "fallback"))
        assertEquals("", json.optStr("missing"))
        assertEquals("text", json.optStr("b"))
        assertEquals("5", json.optStr("c"))
        val array = JSONArray("""[null,"x"]""")
        assertEquals("", array.optStr(0)); assertEquals("x", array.optStr(1)); assertEquals("", array.optStr(9))
    }

    @Test fun aVoiceAnswerWithANullTokenAsksForNoConfirmation() {
        val reply = VoiceParser.parse(JSONObject("""{"intent":null,"outcome":"answered","speech":"hecho","confirmation_token":null,"executed":false}"""))
        assertNull(reply.confirmationToken)
        assertNull(reply.intent)
    }
}
