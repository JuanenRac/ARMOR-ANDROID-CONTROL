package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.model.NetworkDevice
import es.electrohobby3d.armor.model.NetworkInternet
import es.electrohobby3d.armor.model.NetworkNode
import es.electrohobby3d.armor.model.NetworkOverview
import es.electrohobby3d.armor.model.NodeFinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Finding nodes among what the network node has seen, and what a camera's own flags say about its buttons. */
class NodeFinderTest {
    private fun device(ip: String, hostname: String? = null, vendor: String? = null, online: Boolean = true) = NetworkDevice(
        id = ip, ip = ip, mac = null, randomizedMac = false, vendor = vendor, hostname = hostname, kind = "unknown", os = null, online = online,
        firstSeenMs = 0, lastSeenMs = 0, latencyMs = null, ports = emptyList(), services = emptyList(), note = null,
    )

    private fun overview(vararg devices: NetworkDevice) = NetworkOverview(nodes = listOf(NetworkNode(
        nodeId = "network-1", stale = false, ip = "192.168.1.2", cidr = "192.168.1.0/24", gateway = null, rxBps = null, txBps = null,
        internet = NetworkInternet("up", null, true, null, null, emptyList(), 0, 0), devices = devices.toList(),
    )))

    @Test fun `a host named armor-something is a node and its id is what follows the prefix`() {
        val found = NodeFinder.candidates(overview(device("192.168.1.20", hostname = "ARMOR-Radar-01.local")), emptyList())
        assertEquals(1, found.size)
        assertEquals("radar-01", found[0].nodeId)
    }

    @Test fun `an Espressif board without the name is offered too but has no id`() {
        val found = NodeFinder.candidates(overview(device("192.168.1.21", vendor = "Espressif Inc.")), emptyList())
        assertEquals(1, found.size)
        assertNull(found[0].nodeId)
    }

    @Test fun `what is neither named nor Espressif is left out`() {
        assertTrue(NodeFinder.candidates(overview(device("192.168.1.30", hostname = "laptop", vendor = "Acme")), emptyList()).isEmpty())
    }

    @Test fun `a node already known by id is not offered again, whatever the case`() {
        val found = NodeFinder.candidates(overview(device("192.168.1.20", hostname = "armor-radar-01")), listOf("Radar-01"))
        assertTrue(found.isEmpty())
    }

    @Test fun `the same address is offered once, online ones first and then by address`() {
        val found = NodeFinder.candidates(overview(
            device("192.168.1.40", hostname = "armor-b", online = false), device("192.168.1.9", hostname = "armor-c"),
            device("192.168.1.9", hostname = "armor-c"), device("192.168.1.3", hostname = "armor-a"),
        ), emptyList())
        assertEquals(listOf("192.168.1.3", "192.168.1.9", "192.168.1.40"), found.map { it.ip })
    }

    @Test fun `no network overview gives no candidates`() {
        assertTrue(NodeFinder.candidates(null, emptyList()).isEmpty())
    }

    private fun camera(ptz: Boolean? = null, zoom: Boolean? = null, host: String = "10.0.0.5", creds: Boolean = true, rtsp: String = "", snap: String = "") = CameraView(
        id = "c1", name = "Door", host = host, snapshotUrl = snap, rtspPath = rtsp, onvifPort = 80, rtspPort = 554, hasCredentials = creds, liveVideoAvailable = true, ptz = ptz, zoom = zoom,
    )

    @Test fun `a camera that never said what it can do keeps every button`() {
        val unknown = camera()
        assertTrue(unknown.canPanTilt && unknown.canZoom && unknown.hasMovement)
    }

    @Test fun `a fixed camera with no zoom has no movement button at all`() {
        assertFalse(camera(ptz = false, zoom = false).hasMovement)
        assertTrue(camera(ptz = false, zoom = true).hasMovement)
    }

    @Test fun `a camera is configured only with an address and something to log in or fetch with`() {
        assertTrue(camera().configured)
        assertFalse(camera(host = " ").configured)
        assertFalse(camera(creds = false).configured)
        assertTrue(camera(creds = false, snap = "http://10.0.0.5/snap.jpg").configured)
    }
}
