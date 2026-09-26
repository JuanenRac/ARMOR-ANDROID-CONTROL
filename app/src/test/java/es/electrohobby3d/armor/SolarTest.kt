package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.SolarParser
import es.electrohobby3d.armor.model.SolarText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Solar tab reads what GET /api/v1/solar answers; these are the shapes the server produces. */
class SolarTest {
    private val answer = """
    {"devices":[
      {"node_id":"casa","device":"axpert-1","kind":"inverter","received_at":"2026-01-01T10:00:00.000Z","stale":false,"example":false,
       "registered":{"name":"Inversor del garaje","model":"voltronic"},
       "reading":{"kind":"inverter","node_id":"casa","device":"axpert-1","timestamp_ms":1,"mode":"line","grid_v":231.4,"grid_hz":50,"out_v":230,"out_hz":50,"out_va":1450,"out_w":1180,
         "load_percent":24,"battery_v":52.4,"battery_a":14,"battery_percent":81,"pv_v":180,"pv_a":10.9,"pv_w":1960,"heatsink_c":41,"ac_charging":false,"pv_charging":true,"load_on":true,
         "warnings":["line_fail"]}},
      {"node_id":"casa","device":"ant-1","kind":"battery","received_at":"2026-01-01T10:00:01.000Z","stale":true,"example":true,
       "reading":{"kind":"battery","node_id":"casa","device":"ant-1","timestamp_ms":2,"modules":1,"model":"ANT-BMS","state":"charging","voltage_v":52.84,"current_a":6.5,
         "temperature_min_c":1,"temperature_max_c":7,"cell_min_v":3.3,"cell_max_v":3.305,"soc_percent":91,"alarm":true,"capacity_ah":252.6,"full_capacity_ah":280,"energy_kwh":13.35,"cycles":17,
         "stack":[{"n":1,"present":true,"voltage_v":52.84,"current_a":6.5,"temperature_c":3,"soc_percent":91,"state":"Charge","cells_v":[3.3,3.301,3.305,3.302],"temperatures_c":[1,2,7],
                   "capacity_ah":252.6,"full_capacity_ah":280,"cycles":17},{"n":2,"present":false}]}}
     ],
     "waiting":[{"node_id":"casa","device":"us3000-1","kind":"battery","name":"Pila del salón","model":"pylontech-us3000","connection":"rs232","notes":"","created_at":"x"}],
     "totals":{"inverters":1,"batteries":1,"stale":1,"pv_w":1960,"load_w":1180,"battery_w":null,"soc_percent":91,"capacity_ah":null,"full_capacity_ah":null,"energy_kwh":null,"grid_present":true,"mode":"line"},
     "catalog":{"inverter_models":[],"battery_models":[],"connections":[]}}
    """.trimIndent()

    @Test fun theOverviewIsReadWithItsInvertersBatteriesAndWaitingEquipment() {
        val overview = SolarParser.overview(JSONObject(answer))
        assertEquals(2, overview.devices.size)
        assertEquals(1, overview.waiting.size)
        assertFalse(overview.isEmpty)
        val inverter = overview.devices[0]
        assertTrue(inverter.isInverter)
        assertEquals("Inversor del garaje", inverter.name)
        assertEquals("line", inverter.inverter!!.mode)
        assertEquals(1960.0, inverter.inverter!!.pvW!!, 0.001)
        assertEquals(listOf("line_fail"), inverter.inverter!!.warnings)
        assertTrue(inverter.inverter!!.pvCharging)
        assertFalse(inverter.stale)
        assertNull(inverter.battery)
    }

    @Test fun aBatteryKeepsItsCellsSensorsAndFlags() {
        val battery = SolarParser.overview(JSONObject(answer)).devices[1]
        assertEquals("ant-1", battery.name)               // not declared: the device name stands in
        assertTrue(battery.stale)
        assertTrue(battery.example)
        val reading = battery.battery!!
        assertEquals(91, reading.socPercent)
        assertTrue(reading.alarm)
        assertEquals(1, reading.stack.size)               // the module that is not there is dropped
        assertEquals(listOf(3.3, 3.301, 3.305, 3.302), reading.stack[0].cellsV)
        assertEquals(listOf(1.0, 2.0, 7.0), reading.stack[0].temperaturesC)
        assertEquals(17, reading.cycles)
        assertEquals(52.84 * 6.5, reading.powerW!!, 0.001)
    }

    @Test fun theHealthOfABatteryAndOfItsModulesIsRead() {
        val device = SolarParser.device(JSONObject("""{"node_id":"a","device":"b","kind":"battery","reading":{"kind":"battery","modules":2,"health_percent":78,"stack":[{"n":1,"present":true,"health_percent":100},{"n":2,"present":true,"health_percent":56},{"n":3,"present":true}]}}"""))
        val reading = device!!.battery!!
        assertEquals(78, reading.healthPercent)
        assertEquals(listOf<Int?>(100, 56, null), reading.stack.map { it.healthPercent })
        assertNull(SolarParser.device(JSONObject("""{"node_id":"a","device":"b","kind":"battery","reading":{"kind":"battery","modules":1,"stack":[]}}"""))!!.battery!!.healthPercent)
    }

    @Test fun theTotalsAndTheWaitingListAreRead() {
        val overview = SolarParser.overview(JSONObject(answer))
        assertEquals(1960.0, overview.totals.pvW, 0.001)
        assertNull(overview.totals.batteryW)              // JSON null is "nobody reports it", not zero
        assertEquals(91, overview.totals.socPercent)
        assertTrue(overview.totals.gridPresent)
        assertEquals("line", overview.totals.mode)
        assertEquals("Pila del salón", overview.waiting[0].name)
        assertEquals("pylontech-us3000", overview.waiting[0].model)
    }

    @Test fun anEmptyOrOddAnswerDoesNotBreakAnything() {
        val empty = SolarParser.overview(JSONObject("""{"devices":[],"waiting":[],"totals":{"inverters":0,"batteries":0,"stale":0,"pv_w":0,"load_w":0}}"""))
        assertTrue(empty.isEmpty)
        val odd = SolarParser.overview(JSONObject("""{"devices":[{"kind":"toaster","node_id":"a","device":"b","reading":{}},{"node_id":"","device":"x","kind":"inverter","reading":{}},{"kind":"inverter"}]}"""))
        assertTrue(odd.devices.isEmpty())
        val bare = SolarParser.device(JSONObject("""{"node_id":"a","device":"b","kind":"inverter","reading":{"mode":"standby"}}"""))
        assertNotNull(bare)
        assertNull(bare!!.inverter!!.gridV)               // a field the server did not send is unknown, not zero
    }

    @Test fun theWordsAreThoseOfAPerson() {
        assertEquals("Con la red", SolarText.mode("line"))
        assertEquals("Con la batería", SolarText.mode("battery"))
        assertEquals("Sin dato", SolarText.mode("whatever"))
        assertEquals("Cargando", SolarText.batteryState("charging"))
        assertEquals("ANT-BMS", SolarText.model("ant-bms"))
        assertEquals("Pylontech US3000", SolarText.model("pylontech-us3000"))
        assertEquals("Voltronic Axpert King", SolarText.model("axpert-king"))
        assertEquals("MPP Solar InfiniSolar V", SolarText.model("infinisolar-v"))
        assertEquals("ANT-BMS 24S · 200 A", SolarText.model("ant-bms-24s-200a"))
        assertEquals("ant-bms-16s", SolarText.model("ant-bms-16s"))
        assertEquals("", SolarText.model("other"))
        assertEquals("line fail", SolarText.warning("line_fail"))
        assertEquals("980 W", SolarText.watts(980.0))
        assertEquals("1.96 kW", SolarText.watts(1960.0))
        assertEquals("—", SolarText.watts(null))
        assertEquals("Cargando · 340 W", SolarText.batteryFlow(340.0))
        assertEquals("Descargando · 1.20 kW", SolarText.batteryFlow(-1200.0))
        assertEquals("En reposo", SolarText.batteryFlow(5.0))
        assertEquals("3.305", SolarText.cell(3.305))
        assertEquals(5, SolarText.cellSpreadMv(listOf(3.300, 3.305, 3.302)))
        assertNull(SolarText.cellSpreadMv(listOf(3.3)))
    }
}
