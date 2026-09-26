package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ElectricalParser
import es.electrohobby3d.armor.model.ElectricalText
import es.electrohobby3d.armor.model.SolarParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Electrical screen reads what GET /api/v1/electrical/readings answers; these are the shapes the server produces. */
class ElectricalTest {
    private val answer = """
    {"nodes":[
      {"node_id":"electrical-1","received_at":"2026-01-01T10:00:00.000Z","stale":false,
       "reading":{"kind":"electrical","node_id":"electrical-1","timestamp_ms":5000,"switching_enabled":false,"channels":[
         {"id":"grid","domain":"ac","label":"Red","voltage_v":231.4,"current_a":12.6,"power_w":2810.5,"energy_kwh":5230.4,"frequency_hz":49.98,"power_factor":0.97,"state":"closed","alarm":false},
         {"id":"dc-bus","domain":"dc","voltage_v":52.3,"current_a":18.4,"power_w":962.5,"energy_kwh":96.4,"alarm":true},
         {"id":"oven","domain":"ac","power_w":900}]}},
      {"node_id":"electrical-2","received_at":"2026-01-01T09:00:00.000Z","stale":true,"reading":{"kind":"electrical","node_id":"electrical-2","timestamp_ms":1,"channels":[]}},
      {"node_id":"","stale":false,"reading":{"channels":[]}},
      {"node_id":"electrical-3","stale":false}
    ],
    "totals":{"nodes":2,"channels":3,"stale":1,"grid_w":2810,"grid_kwh":5230.4,"alarms":1}}
    """.trimIndent()

    @Test fun theOverviewIsReadWithItsNodesChannelsAndTotals() {
        val overview = ElectricalParser.overview(JSONObject(answer))
        assertEquals(2, overview.nodes.size)                       // a node with no id and one with no reading are left out
        assertFalse(overview.isEmpty)
        val node = overview.nodes[0]
        assertEquals("electrical-1", node.nodeId)
        assertFalse(node.stale)
        assertEquals(3, node.channels.size)
        val grid = node.channels[0]
        assertEquals("Red", grid.name)
        assertEquals(231.4, grid.voltageV!!, 0.001)
        assertEquals(2810.5, grid.powerW!!, 0.001)
        assertEquals(49.98, grid.frequencyHz!!, 0.001)
        assertEquals("closed", grid.state)
        assertFalse(grid.alarm)
        val bus = node.channels[1]
        assertEquals("dc-bus", bus.name)                            // no label: the identifier
        assertEquals("dc", bus.domain)
        assertTrue(bus.alarm)
        assertNull(bus.frequencyHz)
        assertNull(bus.state)
        assertNull(node.channels[2].voltageV)                       // what a meter did not say is not zero
        assertTrue(overview.nodes[1].stale)
        assertEquals(2810.0, overview.totals.gridW!!, 0.001)
        assertEquals(1, overview.totals.alarms)
        assertEquals(1, overview.totals.stale)
    }

    @Test fun anEmptyAnswerIsAnEmptyOverview() {
        val overview = ElectricalParser.overview(JSONObject("{}"))
        assertTrue(overview.isEmpty)
        assertNull(overview.totals.gridW)
        val none = ElectricalParser.overview(JSONObject("""{"nodes":[],"totals":{"nodes":0,"channels":0,"stale":0,"grid_w":null,"grid_kwh":null,"alarms":0}}"""))
        assertNull(none.totals.gridW)
        assertNull(none.totals.gridKwh)
    }

    @Test fun theWordsAreThoseOfAPerson() {
        assertEquals("Consume 2.81 kW de la red", ElectricalText.gridFlow(2810.0))
        assertEquals("Cede 640 W a la red", ElectricalText.gridFlow(-640.0))
        assertEquals("Sin consumo de red", ElectricalText.gridFlow(3.0))
        assertEquals("Sin dato", ElectricalText.gridFlow(null))
        assertEquals("231.4 V", ElectricalText.volts(231.4))
        assertEquals("—", ElectricalText.volts(null))
        assertEquals("12.60 A", ElectricalText.amps(12.6))
        assertEquals("5230.4 kWh", ElectricalText.kwh(5230.4))
        assertEquals("900 W", ElectricalText.watts(900.0))
        assertEquals("1.20 kW", ElectricalText.watts(1200.0))
        assertEquals("Abierto", ElectricalText.switchState("open"))
        assertNull(ElectricalText.switchState(null))
    }

    @Test fun anInverterKeepsItsSecondInputAndItsParallelUnits() {
        val answer = """
        {"devices":[{"node_id":"casa","device":"axpert-1","kind":"inverter","received_at":"x","stale":false,
         "reading":{"kind":"inverter","node_id":"casa","device":"axpert-1","timestamp_ms":1,"mode":"line","grid_v":231.4,"grid_hz":50,"out_v":230,"out_hz":50,"out_va":1450,"out_w":1180,
           "load_percent":24,"battery_v":52.4,"battery_a":14,"battery_percent":81,"pv_v":180,"pv_a":10.9,"pv_w":2986,"heatsink_c":41,"ac_charging":false,"pv_charging":true,"load_on":true,"warnings":[],
           "pv2_v":327.3,"pv2_a":3.1,"pv2_w":1026,"total_out_w":312,
           "units":[{"unit":0,"mode":"battery","serial":"92931701100510","fault_code":"00","out_w":141,"load_percent":5,"battery_v":51.4},{"unit":1,"mode":"fault","fault_code":"07"}]}}],
         "waiting":[],"totals":{}}
        """.trimIndent()
        val inverter = SolarParser.overview(JSONObject(answer)).devices[0].inverter!!
        assertEquals(1026.0, inverter.pv2W!!, 0.001)
        assertEquals(327.3, inverter.pv2V!!, 0.001)
        assertEquals(312.0, inverter.totalOutW!!, 0.001)
        assertEquals(2, inverter.units.size)
        assertEquals("92931701100510", inverter.units[0].serial)
        assertEquals(141.0, inverter.units[0].outW!!, 0.001)
        assertEquals("07", inverter.units[1].faultCode)
        assertNull(inverter.units[1].outW)
        // an inverter with one input has none of it
        val plainJson = """{"devices":[{"node_id":"casa","device":"axpert-1","kind":"inverter","received_at":"x","stale":false,"reading":{"kind":"inverter","node_id":"casa","device":"axpert-1","timestamp_ms":1,"mode":"line","grid_v":231.4,"grid_hz":50,"out_v":230,"out_hz":50,"out_va":1450,"out_w":1180,"load_percent":24,"battery_v":52.4,"battery_a":14,"battery_percent":81,"pv_v":180,"pv_a":10.9,"pv_w":1960,"heatsink_c":41,"ac_charging":false,"pv_charging":true,"load_on":true,"warnings":[]}}],"waiting":[],"totals":{}}"""
        val plain = SolarParser.overview(JSONObject(plainJson)).devices[0].inverter!!
        assertNull(plain.pv2W)
        assertTrue(plain.units.isEmpty())
    }
}
