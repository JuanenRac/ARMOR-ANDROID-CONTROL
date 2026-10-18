package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ConnectionsParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The list of the devices and apps that connect to the server is read tolerantly and never trusts what it does not know. */
class ConnectionsTest {
    private val client = """{"id":"0123456789abcdef","kind":"android","user":"ana","app_version":"0.6.7","first_seen":"2026-01-01T10:00:00.000Z","last_seen":"2026-01-02T10:00:00.000Z","active_sessions":1,"logins_ok":3,"logins_failed":2}"""

    @Test fun aClientIsRead() {
        val list = ConnectionsParser.list(JSONObject("""{"clients":[$client]}"""))
        assertEquals(1, list.size)
        val first = list.single()
        assertEquals("android", first.kind)
        assertEquals("ana", first.user)
        assertEquals("0.6.7", first.appVersion)
        assertEquals(1, first.activeSessions)
        assertEquals(2, first.loginsFailed)
        assertNull("a non-administrator gets no address", first.ip)
        assertNull(first.agent)
    }

    @Test fun anAdministratorAlsoGetsTheAddress() {
        val list = ConnectionsParser.list(JSONObject("""{"clients":[${client.dropLast(1)},"ip":"192.168.0.20","agent":"okhttp/4.12"}]}"""))
        assertEquals("192.168.0.20", list.single().ip)
        assertEquals("okhttp/4.12", list.single().agent)
    }

    @Test fun whatIsNotAClientIsLeftOut() {
        val json = JSONObject("""{"clients":[{"id":"nope","kind":"android"},{"id":"0123456789abcdef","kind":"toaster"},$client,"text",null]}""")
        assertEquals(1, ConnectionsParser.list(json).size)
        assertTrue(ConnectionsParser.list(JSONObject("{}")).isEmpty())
    }

    @Test fun negativeCountsAreNeverShown() {
        val list = ConnectionsParser.list(JSONObject("""{"clients":[${client.replace("\"active_sessions\":1", "\"active_sessions\":-4")}]}"""))
        assertEquals(0, list.single().activeSessions)
    }
}
