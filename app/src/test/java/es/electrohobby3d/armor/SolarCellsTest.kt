package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.CellLevel
import es.electrohobby3d.armor.model.SolarCells
import es.electrohobby3d.armor.model.SolarDevice
import es.electrohobby3d.armor.model.SolarParser
import es.electrohobby3d.armor.model.SolarViewSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/** Every cell of a battery reaches the screen and is judged: the fifteen of a Pylontech 48 V module and the sixteen of a 51.2 V one. */
class SolarCellsTest {
    private val settings = SolarViewSettings()

    private fun volts(count: Int, change: Map<Int, Double> = emptyMap()) = (1..count).map { change[it] ?: (3.300 + it / 1000.0) }

    private fun device(model: String, modules: List<String>): SolarDevice = SolarParser.overview(
        JSONObject("""{"devices":[{"node_id":"n","device":"b","kind":"battery","registered":{"model":"$model"},"reading":{"modules":${modules.size},"stack":[${modules.joinToString(",")}]}}]}"""),
    ).devices.single()

    private fun module(n: Int, cells: List<Double>, extra: String = ""): String {
        val list = cells.joinToString(",") { String.format(Locale.US, "%.3f", it) }
        return """{"n":$n,"present":true,"cells_v":[$list]$extra}"""
    }

    @Test fun theFifteenAndSixteenCellsOfEveryModuleOfAFullStackAreAllKept() {
        for (count in listOf(15, 16)) {
            val battery = device("pylontech-us3000", (1..8).map { module(it, volts(count)) }).battery!!
            val bank = SolarCells.bank("pylontech-us3000", battery, settings)
            assertEquals(8, bank.modules.size)
            for (view in bank.modules) {
                assertEquals((1..count).toList(), view.cells.map { it.number })
                assertEquals(3.300 + count / 1000.0, view.cells.last().volts, 0.0001)
                assertTrue(view.cells.first().lowest)
                assertTrue(view.cells.last().highest)
            }
            assertEquals(CellLevel.OK, bank.level)
            assertEquals(count - 1, bank.spreadMv)
        }
    }

    @Test fun aCellOutOfRangeOrMarkedAbnormalIsToldByItsNumber() {
        val battery = device("pylontech-us2000", listOf(module(1, volts(15, mapOf(10 to 2.60, 15 to 3.62)), ",\"soh_abnormal\":[12]"))).battery!!
        val view = SolarCells.bank("pylontech-us2000", battery, settings).modules.single()
        assertEquals(CellLevel.FAULT, view.cells[9].level)
        assertEquals(CellLevel.WARN, view.cells[14].level)
        assertEquals(CellLevel.FAULT, view.cells[12].level)
        assertTrue(view.cells[12].abnormal)
        assertEquals(listOf(13), view.trouble.abnormalCells)
        assertEquals(listOf(10, 15), view.trouble.outOfRangeCells)
        assertEquals(1020, view.trouble.spreadMv)
        assertEquals(CellLevel.FAULT, view.level)
    }

    @Test fun aProtectionOfTheBatteryAFaultStateHeatColdAndMissingCellsAreNotLost() {
        val battery = device(
            "pylontech-us2000",
            listOf(
                module(1, volts(15), ",\"state\":\"Alarm\""),
                module(2, volts(15), ",\"temperature_c\":61"),
                module(3, volts(15), ",\"state\":\"Charge\",\"temperatures_c\":[-3]"),
                module(4, volts(9), ",\"cell_count\":15"),
                module(5, volts(15), ",\"state\":\"Dischg\",\"temperatures_c\":[-3]"),
            ),
        ).battery!!
        val bank = SolarCells.bank("pylontech-us2000", battery, settings)
        assertEquals(listOf(CellLevel.FAULT, CellLevel.WARN, CellLevel.WARN, CellLevel.WARN, CellLevel.OK), bank.modules.map { it.level })
        assertEquals(6, bank.modules[3].trouble.missingCells)
        assertEquals(1, bank.faultModules)
        assertEquals(3, bank.warnModules)
        assertEquals(CellLevel.FAULT, SolarCells.bank("pylontech-us2000", battery.copy(alarm = true, stack = battery.stack.takeLast(1)), settings).level)
    }

    @Test fun theAbsoluteVoltageIsJudgedOnlyForLithiumIronPhosphate() {
        assertTrue(SolarCells.isLfp("pylontech-us5000", ""))
        assertTrue(SolarCells.isLfp("", "US3000C"))
        assertFalse(SolarCells.isLfp("ant-bms-14s-100", "ANT-BMS"))
        assertFalse(SolarCells.isLfp("other", ""))
        val battery = device("ant-bms", listOf(module(1, (1..14).map { 4.10 }))).battery!!
        assertEquals(CellLevel.OK, SolarCells.bank("ant-bms", battery, settings).level)
    }

    @Test fun aTileIsNeverNarrowerThanItsVoltageNeeds() {
        for (width in 150..700) for (scale in listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 2.0f)) {
            val columns = SolarCells.columns(width.toFloat(), scale, 76)
            assertTrue(columns in 1..5)
            val tile = (width - 6f * (columns - 1)) / columns
            assertTrue("$width dp at $scale", columns == 1 || tile >= 76 * maxOf(1f, scale) - 0.01f)
        }
    }

    @Test fun theSettingsFileIsReadAndTheBlinkNeverGoesFasterThanTwiceASecond() {
        assertEquals(SolarViewSettings(), SolarViewSettings.parse(File("src/main/assets/solar_view.json").readText(Charsets.UTF_8)))
        assertEquals(500, SolarViewSettings.parse("{\"blink_period_ms\":50}").blinkPeriodMs)
        assertEquals(SolarViewSettings(), SolarViewSettings.parse("not json"))
    }
}
