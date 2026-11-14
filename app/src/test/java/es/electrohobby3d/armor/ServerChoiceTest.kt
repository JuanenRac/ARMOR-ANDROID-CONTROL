package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerChoiceTest {
    @Test fun `a server reads without its scheme and a secure one has a lock`() {
        assertEquals("192.168.0.50:8080", ServerChoice.label("http://192.168.0.50:8080"))
        assertEquals("🔒 casa.example.org:443", ServerChoice.label("https://casa.example.org:443"))
        assertEquals("🔒 casa.example.org", ServerChoice.label("https://casa.example.org"))
    }

    @Test fun `the address fields come from the origin`() {
        assertEquals("192.168.0.50" to "8080", ServerChoice.fields("http://192.168.0.50:8080"))
        assertEquals("https://casa.example.org" to "443", ServerChoice.fields("https://casa.example.org"))
        assertEquals("https://casa.example.org" to "18080", ServerChoice.fields("https://casa.example.org:18080"))
    }

    @Test fun `an origin the app would refuse fills nothing`() {
        assertNull(ServerChoice.fields("http://casa.example.org:8080"))
        assertNull(ServerChoice.fields("ftp://192.168.0.50"))
        assertNull(ServerChoice.fields("not an address"))
    }

    @Test fun `the fields make the same endpoint again`() {
        for (origin in listOf("http://10.0.0.4:8080", "https://armor.example.org:18080", "https://armor.example.org:443")) {
            val (host, port) = ServerChoice.fields(origin)!!
            assertEquals(origin, ServerEndpoint.fromHostAndPort(host, port)?.origin)
        }
    }

    @Test fun `the offer lists each valid server once with the one in use first`() {
        val kept = listOf("http://10.0.0.4:8080", "https://armor.example.org:18080", "http://10.0.0.4:8080", "http://evil.example.org:80")
        assertEquals(listOf("https://armor.example.org:18080", "http://10.0.0.4:8080"), ServerChoice.offered(kept, "https://armor.example.org:18080"))
        assertEquals(listOf("http://10.0.0.4:8080", "https://armor.example.org:18080"), ServerChoice.offered(kept, "http://192.168.1.1:8080"))
        assertEquals(emptyList<String>(), ServerChoice.offered(emptyList(), "http://10.0.0.4:8080"))
    }
}
