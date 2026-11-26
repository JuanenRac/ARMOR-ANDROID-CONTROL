// The history behind the solar charts: what the server sends, the bank added up and the ends of the axis. Made-up figures.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.HistorySample
import es.electrohobby3d.armor.model.SolarHistoryMath
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarHistoryTest {
    @Test fun theNumbersOfASampleAreReadAndTextIsLeftOut() {
        val history = SolarHistoryMath.parse(JSONObject("""{"module":2,"every_s":60,"retention_days":30,"samples":[{"t":1000,"voltage_v":50.5,"cell1":3.3,"mode":"line","soc_percent":null},{"voltage_v":1}]}"""))
        assertEquals(2, history.module)
        assertEquals(60, history.everyS)
        assertEquals(30, history.retentionDays)
        assertEquals(listOf(HistorySample(1000, linkedMapOf("voltage_v" to 50.5, "cell1" to 3.3))), history.samples)
        assertNull(SolarHistoryMath.parse(JSONObject("""{"samples":[]}""")).module)
    }

    @Test fun aStackGetsItsPowerAndTheSpreadOfItsCells() {
        val out = SolarHistoryMath.derived(listOf(HistorySample(1, mapOf("voltage_v" to 50.0, "current_a" to -2.0, "cell_min_v" to 3.30, "cell_max_v" to 3.35)))).first().values
        assertEquals(-100.0, out.getValue("power_w"), 1e-9)
        assertEquals(50.0, out.getValue("delta_mv"), 1e-9)
    }

    @Test fun theBankAddsOnlyTheStepsEveryStackReported() {
        val one = listOf(HistorySample(0, mapOf("voltage_v" to 50.0, "current_a" to 1.0)), HistorySample(60_000, mapOf("voltage_v" to 50.0, "current_a" to 3.0)))
        val two = listOf(HistorySample(10_000, mapOf("voltage_v" to 52.0, "current_a" to 2.0)))
        val bank = SolarHistoryMath.mergeBank(listOf(one, two, emptyList()), 60_000)
        assertEquals(1, bank.size)
        assertEquals(3.0, bank[0].values.getValue("current_a"), 1e-9)
        assertEquals(51.0, bank[0].values.getValue("voltage_v"), 1e-9)
        assertEquals(154.0, bank[0].values.getValue("power_w"), 1e-9)
        assertEquals(emptyList<HistorySample>(), SolarHistoryMath.mergeBank(listOf(emptyList()), 60_000))
    }

    @Test fun theAxisEndsAreRoundAndAFarLevelIsNotDrawn() {
        val (low, high) = SolarHistoryMath.scale(listOf(3.31, 3.34))
        assertTrue(low <= 3.31 && high >= 3.34 && high - low < 0.1)
        assertEquals(0.0 to 1.0, SolarHistoryMath.scale(emptyList()))
        assertEquals(listOf(3.6), SolarHistoryMath.levelsInView(listOf(3.3, 3.5), listOf(3.6, 2.0)))
        assertEquals(60_000L, SolarHistoryMath.bankBucketMs(60))
        assertEquals(4_320_000L, SolarHistoryMath.bankBucketMs(43_200))
    }
}
