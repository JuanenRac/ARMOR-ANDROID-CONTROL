// ARMOR-ANDROID-CONTROL - how the cells of a battery are judged and laid out: pure rules, with their levels read from assets/solar_view.json.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

/** The order matters: the worst of two levels is the greater one. */
enum class CellLevel { OK, WARN, FAULT }

/** The voltages of a lithium iron phosphate cell: the ends of its bar, the range outside which it is worth a look and the range outside which it is a fault. */
data class CellLimits(val barEmptyV: Double, val barFullV: Double, val lowV: Double, val highV: Double, val faultLowV: Double, val faultHighV: Double)

data class SolarViewSettings(
    val cell: CellLimits = CellLimits(2.8, 3.65, 2.9, 3.6, 2.7, 3.7),
    val spreadWarnMv: Int = 100,
    val hotC: Double = 55.0,
    val coldChargeC: Double = 0.0,
    val blinkPeriodMs: Int = 1000,
    val minCellTileDp: Int = 76,
) {
    companion object {
        /** A blink is never faster than twice a second, whatever the file says. */
        const val FASTEST_BLINK_MS = 500

        /** The settings of the file; a value that is missing or a file that cannot be read keeps the defaults. */
        fun parse(text: String): SolarViewSettings {
            val d = SolarViewSettings()
            val root = try { JSONObject(text) } catch (_: org.json.JSONException) { return d }
            val c = root.optJSONObject("lfp_cell") ?: JSONObject()
            return SolarViewSettings(
                cell = CellLimits(
                    c.optDouble("bar_empty_v", d.cell.barEmptyV), c.optDouble("bar_full_v", d.cell.barFullV), c.optDouble("low_v", d.cell.lowV),
                    c.optDouble("high_v", d.cell.highV), c.optDouble("fault_low_v", d.cell.faultLowV), c.optDouble("fault_high_v", d.cell.faultHighV),
                ),
                spreadWarnMv = root.optInt("spread_warn_mv", d.spreadWarnMv), hotC = root.optDouble("hot_c", d.hotC), coldChargeC = root.optDouble("cold_charge_c", d.coldChargeC),
                blinkPeriodMs = root.optInt("blink_period_ms", d.blinkPeriodMs).coerceAtLeast(FASTEST_BLINK_MS), minCellTileDp = root.optInt("min_cell_tile_dp", d.minCellTileDp).coerceAtLeast(60),
            )
        }
    }
}

/** One cell as the screen draws it; [number] is counted from 1, [fill] is the length of its bar (0 to 1). */
data class CellView(val number: Int, val volts: Double, val level: CellLevel, val lowest: Boolean, val highest: Boolean, val abnormal: Boolean, val fill: Float)

/** Why a module is not well; the cells are counted from 1. */
data class ModuleTrouble(
    val abnormalCells: List<Int> = emptyList(), val outOfRangeCells: List<Int> = emptyList(), val spreadMv: Int? = null,
    val hot: Boolean = false, val cold: Boolean = false, val state: String = "", val missingCells: Int = 0,
    /** A fault of the battery's electronics (SysError, or the cells from 1 that it does not read), its protections or alarms and its notices, each with the battery's own word. */
    val bmsError: Boolean = false, val unreadCells: List<Int> = emptyList(), val protections: List<BatteryState> = emptyList(), val notices: List<BatteryState> = emptyList(),
) {
    val bmsFault: Boolean get() = bmsError || unreadCells.isNotEmpty()
}

data class ModuleView(val module: SolarModule, val cells: List<CellView>, val level: CellLevel, val spreadMv: Int?, val trouble: ModuleTrouble)

data class BankView(val modules: List<ModuleView>, val level: CellLevel, val minV: Double?, val maxV: Double?, val spreadMv: Int?) {
    val faultModules: Int get() = modules.count { it.level == CellLevel.FAULT }
    val warnModules: Int get() = modules.count { it.level == CellLevel.WARN }
}

object SolarCells {
    private val faultWords = listOf("alarm", "protect", "fault", "error", "fail")

    /**
     * The absolute voltage of a cell is only judged for the batteries known to be lithium iron phosphate (Pylontech and Pytes): a BMS of another make may sit on another chemistry,
     * where the same volts are normal. The gap between cells and the state of health the battery reports are judged for all.
     */
    fun isLfp(deviceModel: String, readingModel: String): Boolean {
        val device = deviceModel.lowercase()
        if (device.startsWith("ant") || readingModel.uppercase().startsWith("ANT")) return false
        return device.startsWith("pylontech") || device.startsWith("pytes") || SolarText.generation(readingModel) > 0
    }

    fun module(m: SolarModule, lfp: Boolean, settings: SolarViewSettings): ModuleView {
        val read = m.cellsV.filterIndexed { index, _ -> index !in m.cellsUnread }
        val low = read.minOrNull()
        val high = read.maxOrNull()
        val spread = SolarText.cellSpreadMv(read)
        val limits = settings.cell
        val cells = m.cellsV.mapIndexed { index, volts ->
            val abnormal = index in m.sohAbnormal
            val lowest = spread != null && spread > 0 && volts == low
            val highest = spread != null && spread > 0 && volts == high
            val level = when {
                index in m.cellsUnread || abnormal || (lfp && (volts < limits.faultLowV || volts > limits.faultHighV)) -> CellLevel.FAULT
                lfp && (volts < limits.lowV || volts > limits.highV) -> CellLevel.WARN
                else -> CellLevel.OK
            }
            val fill = ((volts - limits.barEmptyV) / (limits.barFullV - limits.barEmptyV)).coerceIn(0.0, 1.0).toFloat()
            CellView(index + 1, volts, level, lowest, highest, abnormal, fill)
        }
        val temperatures = m.temperaturesC + listOfNotNull(m.temperatureC)
        val charging = m.state.lowercase().startsWith("charg")
        val trouble = ModuleTrouble(
            abnormalCells = m.sohAbnormal.map { it + 1 },
            outOfRangeCells = cells.filter { !it.abnormal && it.level != CellLevel.OK }.map { it.number },
            spreadMv = spread?.takeIf { it >= settings.spreadWarnMv },
            hot = temperatures.any { it > settings.hotC },
            cold = charging && temperatures.any { it < settings.coldChargeC },
            state = m.state.takeIf { state -> faultWords.any { it in state.lowercase() } } ?: "",
            missingCells = if (m.cellsV.isEmpty()) 0 else ((m.cellCount ?: 0) - m.cellsV.size).coerceAtLeast(0),
            bmsError = m.state == "SysError" || m.states.any { it.level == 3 }, unreadCells = m.cellsUnread.map { it + 1 },
            protections = m.states.filter { it.level == 2 }, notices = m.states.filter { it.level == 1 },
        )
        val fromCells = cells.maxOfOrNull { it.level } ?: CellLevel.OK
        val level = when {
            fromCells == CellLevel.FAULT || trouble.abnormalCells.isNotEmpty() || trouble.state.isNotEmpty() || trouble.bmsFault || trouble.protections.isNotEmpty() -> CellLevel.FAULT
            fromCells == CellLevel.WARN || trouble.notices.isNotEmpty() || trouble.spreadMv != null || trouble.hot || trouble.cold || trouble.missingCells > 0 -> CellLevel.WARN
            else -> CellLevel.OK
        }
        return ModuleView(m, cells, level, spread, trouble)
    }

    /** The whole bank: its worst module, a fault when the battery itself says a protection is acting, and the extremes of all its cells. */
    fun bank(deviceModel: String, reading: SolarBatteryReading, settings: SolarViewSettings): BankView {
        val modules = reading.stack.map { module(it, isLfp(deviceModel, it.model.ifBlank { reading.model }), settings) }
        val level = if (reading.alarm) CellLevel.FAULT else modules.maxOfOrNull { it.level } ?: CellLevel.OK
        val all = reading.stack.flatMap { module -> module.cellsV.filterIndexed { index, _ -> index !in module.cellsUnread } }
        val minV = all.minOrNull() ?: reading.cellMinV
        val maxV = all.maxOrNull() ?: reading.cellMaxV
        return BankView(modules, level, minV, maxV, if (minV != null && maxV != null) Math.round((maxV - minV) * 1000.0).toInt() else null)
    }

    /** How many cells fit in a row without cutting a voltage: the tile grows with the font scale of the phone. */
    fun columns(widthDp: Float, fontScale: Float, minTileDp: Int, gapDp: Float = 6f): Int =
        ((widthDp + gapDp) / (minTileDp * fontScale.coerceAtLeast(1f) + gapDp)).toInt().coerceIn(1, 5)
}
