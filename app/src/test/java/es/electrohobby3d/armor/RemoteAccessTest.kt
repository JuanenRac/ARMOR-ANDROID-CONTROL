package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pieces of reaching the house from outside: a node's panel through the server and the lighter pictures away from home. */
class RemoteAccessTest {
    @Test fun `the panel of a node through the server has the servers address`() {
        assertEquals("https://casa.duckdns.org:443/api/v1/nodes/radar-1/panel/", NodeServerPanel.address("https://casa.duckdns.org:443", "radar-1"))
        assertEquals("http://192.168.0.180:8080/api/v1/nodes/solar_2.a/panel/", NodeServerPanel.address("http://192.168.0.180:8080", "solar_2.a"))
    }

    @Test fun `a bad origin or a node name that could climb out of the path is refused`() {
        assertNull(NodeServerPanel.address("http://8.8.8.8:80", "radar-1"))
        assertNull(NodeServerPanel.address("", "radar-1"))
        assertNull(NodeServerPanel.address("http://192.168.0.180:8080", "../admin"))
        assertNull(NodeServerPanel.address("http://192.168.0.180:8080", "a/b"))
        assertNull(NodeServerPanel.address("http://192.168.0.180:8080", ""))
        assertNull(NodeServerPanel.address("http://192.168.0.180:8080", "x".repeat(65)))
    }

    @Test fun `only an address of the server of the app carries the session cookie`() {
        val origin = "https://casa.duckdns.org:443"
        assertTrue(NodeServerPanel.isViaServer("https://casa.duckdns.org:443/api/v1/nodes/radar-1/panel/", origin))
        assertFalse(NodeServerPanel.isViaServer("http://192.168.0.235/", origin))
        assertFalse(NodeServerPanel.isViaServer("https://casa.duckdns.org.evil.example/api/v1/nodes/x/panel/", origin))
        assertFalse(NodeServerPanel.isViaServer("https://casa.duckdns.org:443/api/v1/nodes/x/panel/", ""))
    }

    @Test fun `away from home the pictures are decoded smaller`() {
        assertEquals(2, CameraQuality.sampleSize(2, away = false))
        assertEquals(1, CameraQuality.sampleSize(1, away = false))
        assertEquals(4, CameraQuality.sampleSize(2, away = true))
        assertEquals(2, CameraQuality.sampleSize(1, away = true))
        assertEquals(8, CameraQuality.sampleSize(8, away = true))
        assertEquals("low", CameraQuality.requested(away = true, tile = true))
        assertEquals("normal", CameraQuality.requested(away = false, tile = true))
        assertEquals("auto", CameraQuality.requested(away = true, tile = false))
    }
}
