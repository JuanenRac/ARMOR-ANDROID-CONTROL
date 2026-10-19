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

/** The extremes of the cells of the day that the server adds to a battery. */
class SolarDayCellsTest {
    private val answer = """
    {"devices":[{"node_id":"casa","device":"us3000-1","kind":"battery","received_at":"2026-01-01T10:00:01.000Z","stale":false,"example":false,
      "day_cells":{"date":"2026-01-01","bank_delta_v":0.06,"modules":[{"n":1,"delta_v":0.04,"min":{"v":3.31,"cell":1,"at":1767261720000},"max":{"v":3.35,"cell":3,"at":1767261780000}},{"n":2,"delta_v":0.06,"min":{"v":3.3,"cell":1,"at":1},"max":5}]},
      "reading":{"kind":"battery","node_id":"casa","device":"us3000-1","timestamp_ms":2,"modules":1,"model":"US3000","state":"idle","stack":[{"n":1,"present":true,"cells_v":[3.31,3.33,3.35]}]}}],
     "waiting":[],"totals":{},"catalog":{}}
    """
    @Test fun theExtremesOfTheDayAreReadAndAModuleWithoutThemIsSkipped() {
        val device = SolarParser.overview(JSONObject(answer)).devices.single()
        val day = device.dayCells!!
        assertEquals(0.06, day.bankDeltaV, 1e-9)
        assertEquals(1, day.modules.size)
        assertEquals(3, day.modules[0].maxCell)
        assertEquals(3.31, day.modules[0].minV, 1e-9)
    }

    @Test fun anInverterOrAnOlderServerHasNoExtremes() {
        val old = JSONObject(answer.replace("\"day_cells\"", "\"other\""))
        assertNull(SolarParser.overview(old).devices.single().dayCells)
    }

    @Test fun aReachIsWrittenWithVoltageCellAndTime() {
        val text = SolarText.reach(3.3121, 5, 0L, "celda")
        assertTrue(text, text.startsWith("3.312 V (celda 5, "))
    }
}

/** The life counters of a battery module. */
class SolarCountersTest {
    private val answer = """
    {"devices":[{"node_id":"casa","device":"us3000-1","kind":"battery","received_at":"x","stale":false,"example":false,
      "reading":{"kind":"battery","node_id":"casa","device":"us3000-1","timestamp_ms":2,"modules":1,"stack":[
        {"n":1,"present":true,"cells_v":[3.3,3.31],"counters":{"bat_ov_times":755,"coc_times":0,"data_items":2411,"bad":-4,"dsg_cap":149287073}},{"n":2,"present":true}]}}],
     "waiting":[],"totals":{},"catalog":{}}
    """
    @Test fun theCountersAreReadAndTheOnesBelowZeroAreLeftOut() {
        val stack = SolarParser.overview(JSONObject(answer)).devices.single().battery!!.stack
        assertEquals(755L, stack[0].counters["bat_ov_times"])
        assertNull(stack[0].counters["bad"])
        assertTrue(stack[1].counters.isEmpty())
    }

    @Test fun onlyTheRaisedProtectionCountersAreShownWithTheConsolesNames() {
        val counters = SolarParser.overview(JSONObject(answer)).devices.single().battery!!.stack[0].counters
        assertEquals("Bat OV Times 755", SolarText.raisedCounters(counters))
        assertEquals("", SolarText.raisedCounters(mapOf("data_items" to 5L, "coc_times" to 0L)))
        assertEquals("COC Times 1 · Dot Times 2".replace("Dot", "DOT"), SolarText.raisedCounters(mapOf("dot_times" to 2L, "coc_times" to 1L)))
        assertEquals("HT Half C Cnt", SolarText.counterLabel("ht_half_c_cnt"))
    }

    private val details = """
    {"devices":[{"node_id":"casa","device":"us2000-1","kind":"battery","received_at":"x","stale":false,"example":false,
      "reading":{"kind":"battery","node_id":"casa","device":"us2000-1","timestamp_ms":2,"modules":2,"stack":[
        {"n":1,"present":true,"model":"US2000","board_version":"V10R04","main_soft_version":"B68.2.0.0","soft_version":"V2.6","cell_count":15,"max_charge_a":53,"max_discharge_a":55,"console_baud":19200,
         "soh_counts":[0,0,0,0,0,0,13,0],"soh_abnormal":[6]},
        {"n":2,"present":true,"model":"US3000C"}]},
      "events":[{"n":1,"seen_at":1700000000000,"event":{"index":754,"time":"25-03-09 19:01:42","voltage_state":"OV","power_events":"0x2 PHV","battery_events":"0x4 BVNOR","system_fault":"0x0"}},
                {"n":1,"seen_at":1,"event":{"index":1}},{"n":0,"seen_at":1,"event":{"index":2,"time":"t"}}]}],
     "waiting":[],"totals":{},"catalog":{}}
    """
    @Test fun theDetailsOfAModuleAndTheEventLogAreRead() {
        val device = SolarParser.overview(JSONObject(details)).devices.single()
        val first = device.battery!!.stack[0]
        assertEquals("US2000", first.model)
        assertEquals("V10R04", first.boardVersion)
        assertEquals("B68.2.0.0", first.mainSoftVersion)
        assertEquals("V2.6", first.softVersion)
        assertEquals(15, first.cellCount)
        assertEquals(53.0, first.maxChargeA!!, 0.001)
        assertEquals(55.0, first.maxDischargeA!!, 0.001)
        assertEquals(19200, first.consoleBaud)
        assertEquals(listOf(7), SolarText.abnormalCells(first))   // counted from 1
        assertEquals(13, first.sohCounts[6])
        assertEquals(1, device.events.size)   // the entries with no time or no module are left out
        assertEquals("0x4 BVNOR", device.events[0].event.batteryEvents)
        assertEquals(754, device.events[0].event.index)
        assertTrue(SolarParser.overview(JSONObject(answer)).devices.all { it.events.isEmpty() })
    }

    @Test fun theModelsAGroupMixesAreJudgedByTheManualsRules() {
        val stack = SolarParser.overview(JSONObject(details)).devices.single().battery!!.stack
        assertEquals(listOf("master" to listOf(2)), SolarText.composition(stack))   // the master is older than a slave
        assertEquals(emptyList<Pair<String, List<Int>>>(), SolarText.composition(stack.reversed().mapIndexed { i, m -> m.copy(number = i + 1) }))
        val eight = (1..8).map { stack[1].copy(number = it) }
        assertEquals(listOf("beyond8" to listOf(9)), SolarText.composition(eight + stack[0].copy(number = 9)))
        assertEquals(emptyList<Pair<String, List<Int>>>(), SolarText.composition(eight + stack[1].copy(number = 9)))
        assertEquals(listOf("voltage" to listOf(1)), SolarText.composition(listOf(stack[1].copy(number = 1, model = "UP2500"), stack[1].copy(number = 2))))
        assertEquals(emptyList<Pair<String, List<Int>>>(), SolarText.composition(listOf(stack[0].copy(model = ""), stack[1].copy(model = "Fidus", number = 2))))
        for (code in listOf("beyond8", "master", "voltage")) assertTrue(SolarText.compositionText(code).isNotBlank())
    }

    @Test fun theBatteryAlertsTellWhatTheyMeanAndWhatToCheck() {
        val codes = listOf("solar_battery_overvoltage", "solar_battery_low", "solar_battery_alarm", "solar_battery_hot", "solar_battery_cold", "solar_cells_unbalanced", "solar_counter_rose",
            "solar_cell_soh_abnormal", "solar_module_overcurrent", "solar_offline", "solar_battery_worn")
        for (code in codes) {
            val help = es.electrohobby3d.armor.model.DeviceText.alarmHelp(code)
            assertNotNull(code, help)
            assertEquals(code, 2, help!!.split("\n").size)
        }
        assertNull(es.electrohobby3d.armor.model.DeviceText.alarmHelp("smoke"))
        for (code in listOf("solar_cell_soh_abnormal", "solar_module_overcurrent")) assertTrue(es.electrohobby3d.armor.model.DeviceText.alarmText(code) != code)
    }
}
