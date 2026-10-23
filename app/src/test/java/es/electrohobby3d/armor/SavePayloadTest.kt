package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The file a node's page sends to be saved arrives as one text through a message port; what is not exactly that is thrown away. */
class SavePayloadTest {
    private fun payload(name: String, mime: String, data: String) = org.json.JSONObject().put("name", name).put("mime", mime).put("data", data).toString()
    private fun base64(text: String) = java.util.Base64.getEncoder().encodeToString(text.toByteArray())

    @Test fun `a file is read from its text`() {
        val file = SavePayload.parse(payload("settings.json", "application/json", base64("{\"a\":1}")))!!
        assertEquals("settings.json", file.name)
        assertEquals("application/json", file.mime)
        assertEquals("{\"a\":1}", String(file.bytes))
    }

    @Test fun `the name is cleaned and given a default, and the type has a default`() {
        assertEquals(".._.._etc_passwd", SavePayload.parse(payload("../../etc/passwd", "", base64("x")))!!.name)
        assertEquals("armor-node.json", SavePayload.parse(payload("", "text/plain", base64("x")))!!.name)
        assertEquals("application/octet-stream", SavePayload.parse(payload("a.bin", "", base64("x")))!!.mime)
        assertEquals(80, SavePayload.parse(payload("n".repeat(200), "", base64("x")))!!.name.length)
    }

    @Test fun `what is not exactly that is refused`() {
        assertNull(SavePayload.parse("not json"))
        assertNull(SavePayload.parse(""))
        assertNull(SavePayload.parse(payload("a.json", "", "!!! not base64 !!!")))
        assertNotNull(SavePayload.parse(payload("a.json", "", "")))   // an empty file is a file
    }

    @Test fun `a file larger than the limit is refused`() {
        val huge = "A".repeat(SavePayload.MAX_BYTES * 4 / 3 + 100)
        assertNull(SavePayload.parse(payload("big.bin", "", huge)))
        val justOver = base64("x".repeat(SavePayload.MAX_BYTES + 1))
        assertNull(SavePayload.parse(payload("big.bin", "", justOver)))
    }
}
