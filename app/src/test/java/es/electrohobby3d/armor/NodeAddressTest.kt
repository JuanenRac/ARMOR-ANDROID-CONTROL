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
        for (bad in listOf("8.8.8.8", "172.32.0.1", "192.168.0.256", "armor-nodo.local", "example.com", "192.168.0.1/admin", "user@192.168.0.1", "192.168.0.1:0", "192.168.0.1:99999", "192.168.0.1:x", "", "https://192.168.0.1")) {
            assertNull(bad, NodeAddress.parse(bad))
        }
    }
}
