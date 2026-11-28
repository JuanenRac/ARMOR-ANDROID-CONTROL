package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.SolarCells
import es.electrohobby3d.armor.model.SolarModule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// A module that answers its cells and not its data sheet, with made-up figures.
class SheetUnansweredTest {
    private fun module(number: Int, present: Boolean = true, cells: List<Double> = listOf(3.31, 3.32), cycles: Int? = null, softVersion: String = "") =
        SolarModule(number, present, 49.6, 1.0, 21.0, 70, "Charge", cells, emptyList(), null, null, cycles, softVersion = softVersion)

    @Test
    fun saidOnlyOfASilentModuleBesideOneThatAnswers() {
        val told = module(1, cycles = 12, softVersion = "V2.4")
        val mute = module(2)
        assertTrue(SolarCells.sheetUnanswered(mute, listOf(told, mute)))
        assertFalse(SolarCells.sheetUnanswered(told, listOf(told, mute)))
        assertFalse(SolarCells.sheetUnanswered(mute, listOf(mute)))
        assertFalse(SolarCells.sheetUnanswered(module(2, present = false), listOf(told)))
        assertFalse(SolarCells.sheetUnanswered(module(2, cells = emptyList()), listOf(told, mute)))
        assertFalse(SolarCells.sheetUnanswered(module(2, cycles = 3), listOf(told, mute)))
    }
}
