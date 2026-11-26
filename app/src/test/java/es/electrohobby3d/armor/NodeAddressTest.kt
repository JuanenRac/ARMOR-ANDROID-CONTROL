package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NodeAddressTest {
    @Test fun `an address of the local network opens the panel of the node`() {
        assertEquals("http://192.168.0.235/", NodeAddress.parse("192.168.0.235"))
        assertEquals("http://192.168.0.235/", NodeAddress.parse(" http://192.168.0.235/ "))
        assertEquals("http://10.0.0.7:8080/", NodeAddress.parse("10.0.0.7:8080"))
        assertEquals("http://172.16.4.2/", NodeAddress.parse("172.16.4.2:80"))
    }

    @Test fun `nothing outside the local network, and nothing that is not an address, is opened`() {
        for (bad in listOf("8.8.8.8", "172.32.0.1", "192.168.0.256", "192.168.000.001", "010.0.0.1", "192.168.0.01", "armor-nodo.local", "example.com", "192.168.0.1/admin", "user@192.168.0.1", "192.168.0.1:0", "192.168.0.1:99999", "192.168.0.1:x", "", "https://192.168.0.1")) {
            assertNull(bad, NodeAddress.parse(bad))
        }
        // a ticket of the server leads to the panel on its own origin; the address that is kept does not carry the ticket
        val ticket = "abcdefghijklmnop_-0123456789"
        val opened = NodeServerPanel.ticketed("https://armor.local", "solar-1", "https://armor.local:8444/enter/$ticket", "https://armor.local:8444")
        assertEquals(OpenedPanel("https://armor.local:8444/enter/$ticket", "https://armor.local:8444/panel/solar-1/"), opened)
        assertNull("not a downgrade to plain http", NodeServerPanel.ticketed("https://armor.local", "solar-1", "http://armor.local:8444/enter/$ticket", "http://armor.local:8444"))
        assertNull("not another host than the origin it states", NodeServerPanel.ticketed("https://armor.local", "solar-1", "https://other.example/enter/$ticket", "https://armor.local:8444"))
        assertNull(NodeServerPanel.ticketed("https://armor.local", "solar-1", "https://armor.local:8444/enter/$ticket?next=x", "https://armor.local:8444"))
        assertNull(NodeServerPanel.ticketed("https://armor.local", "solar 1", "https://armor.local:8444/enter/$ticket", "https://armor.local:8444"))
        assertNull(NodeServerPanel.ticketed("https://armor.local", "solar-1", "https://a@armor.local/enter/$ticket", "https://a@armor.local"))
    }
}
