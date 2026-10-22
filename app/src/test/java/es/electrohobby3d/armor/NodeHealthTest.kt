package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.NodeHealthOverview
import es.electrohobby3d.armor.model.NodeHealthParser
import es.electrohobby3d.armor.model.NodeHealthText
import es.electrohobby3d.armor.model.NodeHealthTotals
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The health of the nodes: what GET /api/v1/node-health answers (the shape the server produces), its reading and its wording. */
class NodeHealthTest {
    private val answer = """
    {"nodes":[
      {"node_id":"radar-1","family":"radar","firmware":"0.6.9","board":"s3-wifi","received_at":"2026-01-01T10:00:00.000Z","stale":false,"status":"warning",
       "warnings":[{"code":"node_low_memory","detail":{"heap_free":12000}},{"code":"node_weak_signal","detail":{}}],"restarts_24h":3,
       "report":{"kind":"node_health","node_id":"radar-1","family":"radar","firmware":"0.6.9","board":"s3-wifi","uptime_s":93784,"reset_reason":"task_wdt","heap_free":12000,"heap_min":9000,
                 "link":"wifi","link_up":true,"rssi_dbm":-83,"broker_reconnects":4,"low_stack_tasks":1,"last_error":"armor-mqtt: the broker refused"}},
      {"node_id":"alarm-1","family":"alarm","firmware":"0.3.6","received_at":"2026-01-01T10:00:00.000Z","stale":false,"status":"ok","warnings":[],"restarts_24h":0,
       "report":{"node_id":"alarm-1","uptime_s":3600,"reset_reason":"power_on","heap_free":142000,"heap_min":98000,"link":"ethernet","link_up":true,"broker_reconnects":1}},
      {"node_id":"solar-1","family":"solar","firmware":"0.4.5","received_at":"2026-01-01T09:00:00.000Z","stale":true,"status":"stale","warnings":[],"restarts_24h":1,
       "report":{"uptime_s":60,"reset_reason":"software","heap_free":1500000,"heap_min":1200000,"link":"none","link_up":false,"broker_reconnects":0}},
      {"node_id":"","family":"hmi","report":{}},
      {"node_id":"no-report","family":"hmi"}
    ],
    "waiting":[{"node_id":"north-1","family":"radar"},{"node_id":"","family":"alarm"}],
    "totals":{"nodes":3,"stale":1,"with_warnings":1},
    "levels":{"low_heap_b":30000}}
    """.trimIndent()

    @Test fun theOverviewIsReadWithItsNodesWarningsAndTotals() {
        val overview = NodeHealthParser.overview(JSONObject(answer))
        assertEquals(3, overview.nodes.size)                                   // a node with no id and one with no report are left out
        assertEquals(NodeHealthTotals(3, 1, 1), overview.totals)
        assertEquals(listOf("north-1"), overview.waiting.map { it.nodeId })
        val radar = overview.nodes[0]
        assertTrue(radar.needsLook && !radar.silent)
        assertEquals(listOf("node_low_memory", "node_weak_signal"), radar.warnings.map { it.code })
        assertEquals(93784L, radar.uptimeSeconds); assertEquals("task_wdt", radar.resetReason)
        assertEquals(12000L, radar.heapFree); assertEquals(9000L, radar.heapMin); assertEquals(-83, radar.rssiDbm)
        assertEquals(4L, radar.brokerConnections); assertEquals(1, radar.lowStackTasks); assertEquals("s3-wifi", radar.board)
        assertEquals("armor-mqtt: the broker refused", radar.lastError); assertEquals(3, radar.restarts24h)
        val cable = overview.nodes[1]
        assertNull(cable.rssiDbm); assertNull(cable.lastError); assertNull(cable.board); assertFalse(cable.needsLook)
        assertTrue(overview.nodes[2].silent)
        assertTrue(NodeHealthParser.overview(JSONObject("{}")).isEmpty)
    }

    @Test fun theWordingIsPlainSpanishAndNeverShowsACode() {
        for (family in listOf("radar", "solar", "electrical", "alarm", "hmi")) assertNotEquals(family, NodeHealthText.family(family))
        for (status in listOf("ok", "warning", "stale")) assertNotEquals(status, NodeHealthText.status(status))
        for (code in listOf("node_low_memory", "node_frequent_resets", "node_link_flapping", "node_weak_signal", "node_sensor_bluetooth_open", "node_radar_masked", "node_wifi_jam")) assertNotEquals(code, NodeHealthText.warning(code))
        for (reason in listOf("power_on", "external", "software", "panic", "int_wdt", "task_wdt", "other_wdt", "deep_sleep", "brownout", "sdio", "usb", "jtag", "efuse", "power_glitch", "cpu_lockup"))
            assertNotEquals(reason, NodeHealthText.resetReason(reason))
        assertEquals("desconocido", NodeHealthText.resetReason("cosmic_ray"))
        assertTrue(NodeHealthText.isUnexpected("panic") && NodeHealthText.isUnexpected("brownout") && !NodeHealthText.isUnexpected("software") && !NodeHealthText.isUnexpected("power_on"))
    }

    @Test fun theLinkTheSignalTheMemoryAndTheUptimeAreToldInWords() {
        val nodes = NodeHealthParser.overview(JSONObject(answer)).nodes
        assertEquals("Wi-Fi · activo · -83 dBm (muy débil)", NodeHealthText.link(nodes[0]))
        assertEquals("Cable Ethernet · activo", NodeHealthText.link(nodes[1]))
        assertEquals("sin enlace · caído", NodeHealthText.link(nodes[2]))
        assertEquals(listOf("buena", "aceptable", "débil", "muy débil"), listOf(-55, -65, -75, -90).map { NodeHealthText.signal(it) })
        assertEquals(listOf("500 B", "139 KB", "1.4 MB", "–"), listOf(500L, 142_000L, 1_500_000L, -1L).map { NodeHealthText.bytes(it) })
        assertEquals(listOf("1 d 2 h", "1 h 0 min", "7 min", "0 min"), listOf(93_784L, 3_600L, 420L, 5L).map { NodeHealthText.uptime(it) })
    }

    @Test fun theSummaryOfTheScreenSaysWhatNeedsALook() {
        assertEquals("Ningún nodo ha enviado aún su informe de salud", NodeHealthText.summary(NodeHealthOverview()))
        val overview = NodeHealthParser.overview(JSONObject(answer))
        assertEquals("Un nodo con aviso de salud", NodeHealthText.summary(overview))
        assertEquals("3 nodos con aviso de salud", NodeHealthText.summary(overview.copy(totals = NodeHealthTotals(5, 0, 3))))
        assertEquals("2 nodos sin informe reciente", NodeHealthText.summary(overview.copy(totals = NodeHealthTotals(5, 2, 0))))
        assertEquals("Todos los nodos sanos", NodeHealthText.summary(overview.copy(totals = NodeHealthTotals(5, 0, 0))))
    }

    @Test fun theNodesThatNeedALookComeFirst() {
        val nodes = NodeHealthParser.overview(JSONObject(answer)).nodes
        assertEquals(listOf("radar-1", "solar-1", "alarm-1"), orderedByNeed(nodes.reversed()).map { it.nodeId })
    }
}
