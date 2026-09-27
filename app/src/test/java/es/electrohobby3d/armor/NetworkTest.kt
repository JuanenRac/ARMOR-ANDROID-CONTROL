package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.NetworkEvent
import es.electrohobby3d.armor.model.NetworkParser
import es.electrohobby3d.armor.model.NetworkText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Network screen reads what GET /api/v1/network answers; these are the shapes the server produces. */
class NetworkTest {
    private val answer = """
    {"nodes":[
      {"node_id":"network-1","received_at":"2026-09-27T10:00:00.000Z","stale":false,
       "interface":{"name":"Wi-Fi","ip":"192.168.0.10","cidr":"192.168.0.0/24","gateway":"192.168.0.1","rx_bps":1200000,"tx_bps":340000},
       "internet":{"state":"down","since_ms":1790000000000,"gateway_ok":true,"latency_ms":12.5,"loss_percent":100,"outages_24h":2,"downtime_24h_s":900,
         "probes":[{"target":"1.1.1.1:443","kind":"tcp","ok":false},{"target":"8.8.8.8","kind":"dns","ok":true,"latency_ms":14.2}]},
       "devices":[
         {"id":"14:2e:5e:86:d9:62","ip":"192.168.0.1","mac":"14:2e:5e:86:d9:62","vendor":"Sercomm","hostname":"WFADevice","kind":"router","online":true,"first_seen_ms":1,"last_seen_ms":2,
          "ports":[{"port":80,"proto":"tcp","service":"http"}],"note":{"name":"Router del salón","trusted":true,"updated_at":"x"}},
         {"id":"96:b3:ed:0b:1c:18","ip":"192.168.0.12","mac":"96:b3:ed:0b:1c:18","randomized_mac":true,"kind":"phone","online":false,"first_seen_ms":1,"last_seen_ms":2},
         {"id":"00:bc:99:aa:bb:01","ip":"192.168.0.203","kind":"camera","online":true,"first_seen_ms":1,"last_seen_ms":2,"ports":[{"port":554,"proto":"tcp","service":"rtsp"},{"port":23,"proto":"tcp","service":"telnet","banner":"login:"}],"services":["_rtsp._tcp"]},
         {"id":"","ip":"192.168.0.99","online":true},
         {"id":"ip-1","online":true}
       ]},
      {"node_id":"","interface":{"ip":"1.1.1.1"},"internet":{"state":"up"}},
      {"node_id":"network-2"}
    ],
    "totals":{"nodes":1,"stale":0,"devices":3,"online":2,"unknown":2,"internet":"down"},
    "events":[{"id":"e1","node_id":"network-1","kind":"new_device","at_ms":5,"device_id":"96:b3:ed:0b:1c:18"},{"id":"e2","node_id":"network-1","kind":"internet_up","at_ms":6,"outage_s":200},{"id":"","kind":"x"}],
    "outages":[{"node_id":"network-1","kind":"internet","started_ms":100,"ended_ms":400,"duration_s":300}]}
    """.trimIndent()

    @Test fun theOverviewIsReadWithItsNodeDevicesEventsAndOutages() {
        val overview = NetworkParser.overview(JSONObject(answer))
        assertEquals(1, overview.nodes.size)                       // a node with no id and one with no interface are left out
        assertFalse(overview.isEmpty)
        val node = overview.nodes[0]
        assertEquals("192.168.0.1", node.gateway)
        assertEquals(1_200_000L, node.rxBps)
        assertEquals("down", node.internet.state)
        assertEquals(true, node.internet.gatewayOk)
        assertEquals(2, node.internet.probes.size)
        assertNull(node.internet.probes[0].latencyMs)
        assertEquals(3, node.devices.size)                         // a device with no id or no address is left out
        assertEquals(2, overview.events.size)
        assertEquals(1, overview.outages.size)
        assertEquals(2, overview.totals.online)
        assertEquals("down", overview.totals.internet)
    }

    @Test fun aDeviceIsNamedTheWayTheOperatorWouldCallIt() {
        val devices = NetworkParser.overview(JSONObject(answer)).nodes[0].devices
        assertEquals("Router del salón", devices[0].name)
        assertTrue(devices[0].known)
        assertEquals("192.168.0.12", devices[1].name)              // nothing better than its address
        assertFalse(devices[1].known)
        assertTrue(devices[1].randomizedMac)
        assertEquals(listOf(554, 23), devices[2].ports.map { it.port })
        assertEquals("login:", devices[2].ports[1].banner)
        assertEquals(listOf("_rtsp._tcp"), devices[2].services)
        assertTrue(devices[2].ports.any { it.port in NetworkText.riskyPorts })
    }

    @Test fun theStateOfTheInternetIsWordedAndWhoseFaultItIs() {
        assertEquals("Hay internet", NetworkText.state("up"))
        assertTrue(NetworkText.state("down").contains("router responde"))
        assertTrue(NetworkText.state("lan_down").contains("router no responde"))
        assertNull(NetworkText.hint("up"))
        assertTrue(NetworkText.hint("down")!!.contains("más allá del router"))
        assertTrue(NetworkText.hint("lan_down")!!.contains("de este lado"))
        assertEquals("Comprobando internet…", NetworkText.state("nonsense"))
    }

    @Test fun figuresAreWrittenShort() {
        assertEquals(listOf("45 s", "3 min", "1 h", "2 h 5 min", "2 d 7 h"), listOf(45L, 200L, 3600L, 7500L, 200_000L).map(NetworkText::duration))
        assertEquals(listOf("—", "800 bit/s", "45 kbit/s", "1.2 Mbit/s", "93 Mbit/s", "2.50 Gbit/s"), listOf(null, 800L, 45_000L, 1_200_000L, 93_000_000L, 2_500_000_000L).map(NetworkText::bps))
    }

    @Test fun anEventIsSaidNamingTheDevice() {
        val overview = NetworkParser.overview(JSONObject(answer))
        val devices = overview.nodes[0].devices
        fun event(kind: String, deviceId: String? = null, port: Int? = null, outage: Int? = null, detail: String? = null) = NetworkEvent("e", "network-1", kind, 1, deviceId, port, outage, detail)
        assertEquals("Ha aparecido un dispositivo que nunca se había visto: 192.168.0.12", NetworkText.event(event("new_device", "96:b3:ed:0b:1c:18"), devices))
        assertEquals("Router del salón (192.168.0.1) ha abierto el puerto 23", NetworkText.event(event("port_opened", "14:2e:5e:86:d9:62", 23), devices))
        assertEquals("Vuelve internet tras 3 min", NetworkText.event(event("internet_up", outage = 200), devices))
        assertEquals("Se cayó internet", NetworkText.event(event("internet_down"), devices))
        assertEquals("gone ha dejado de responder", NetworkText.event(event("device_offline", "gone"), devices))
        assertTrue(NetworkText.event(event("arp_conflict", "x", detail = "el router responde otro"), devices).contains("el router responde otro"))
    }

    @Test fun theStatusLineSaysTheInternetAndTheDevices() {
        val overview = NetworkParser.overview(JSONObject(answer))
        assertEquals("Sin internet: el router responde y nada más allá · 2/3 dispositivos · 2 sin conocer", NetworkText.summary(overview))
        assertEquals("", NetworkText.summary(es.electrohobby3d.armor.model.NetworkOverview()))
    }

    @Test fun aNetworkAlarmWakesTheOperator() {
        val raised = es.electrohobby3d.armor.model.ArmorEvent(id = 1, at = "2026-09-27T10:00:00Z", type = "alarm", subject = "network-1", from = null, to = "raised", targets = null, mode = null, severity = "high", code = "network_internet_down", sourceType = "network")
        val notices = AlarmPolicy.notices(listOf(raised), armed = false)
        assertEquals(1, notices.size)
        assertTrue(notices[0].text.contains("No hay internet"))
    }
}
