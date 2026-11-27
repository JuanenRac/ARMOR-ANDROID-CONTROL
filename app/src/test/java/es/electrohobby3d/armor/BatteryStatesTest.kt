package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.BatteryState
import es.electrohobby3d.armor.model.CellLevel
import es.electrohobby3d.armor.model.SolarCells
import es.electrohobby3d.armor.model.SolarParser
import es.electrohobby3d.armor.model.SolarViewSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The three levels of a battery's states, and a module that does not read three of its cells (invented figures). */
class BatteryStatesTest {
    @Test fun aNoticeAProtectionAndAFault() {
        assertEquals(1, BatteryState("voltage", "High").level)
        assertEquals(2, BatteryState("system", "High").level)
        assertEquals(2, BatteryState("voltage", "NeverSeen").level)
        assertEquals(3, BatteryState("base", "SysError").level)
    }

    @Test fun cellsThatAreNotReadAreAFaultAndNotTheLowestCell() {
        val cells = (0 until 15).joinToString(",") { if (it in 3..5) "0" else "3.33" }
        val module = """{"n":1,"present":true,"cells_v":[$cells],"cells_unread":[3,4,5],"manufacturer":"Pylon","manufacturer_source":"reported","flags":[{"column":"voltage","word":"UV","cell":3}]}"""
        val device = SolarParser.overview(JSONObject("""{"devices":[{"node_id":"n","device":"b","kind":"battery","reading":{"modules":1,"stack":[$module]}}]}""")).devices.single()
        val read = device.battery!!.stack.single()
        assertEquals("Pylon", read.manufacturer)
        val bank = SolarCells.bank("pylontech-us2000", device.battery!!, SolarViewSettings())
        val view = bank.modules.single()
        assertEquals(listOf(4, 5, 6), view.trouble.unreadCells)
        assertTrue(view.trouble.bmsFault)
        assertEquals(CellLevel.FAULT, view.level)
        assertEquals(3.33, bank.minV!!, 0.001)
    }
}
