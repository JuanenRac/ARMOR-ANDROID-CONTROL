// ARMOR-ANDROID-CONTROL - the history of the solar equipment the server keeps (GET /api/v1/solar/history): its parsing and the arithmetic of the charts, the same as Studio's.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** One moment of a history: the numbers it carries by the server's own names (voltage_v, soc_percent, cell1...). A number the server did not record is simply not there. */
data class HistorySample(val t: Long, val values: Map<String, Double>)

/** What the server answered: [module] is null for a stack or an inverter, and also from a server that predates the history by battery. */
data class SolarHistory(val module: Int?, val everyS: Int?, val retentionDays: Int?, val samples: List<HistorySample>)

/** What is asked: the equipment, how far back and, for one battery and its cells, its module. */
data class HistoryRequest(val node: String, val device: String, val minutes: Int, val module: Int? = null)

object SolarHistoryMath {
    /** The ranges of Studio, in minutes: 1 h, 6 h, 24 h, 7 days and 30 days. */
    val RANGES = listOf(60 to "1 h", 360 to "6 h", 1440 to "24 h", 10_080 to "7 d", 43_200 to "30 d")

    fun parse(root: JSONObject): SolarHistory {
        val list = root.optJSONArray("samples")
        val samples = buildList {
            if (list != null) for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                val t = item.optLong("t", -1L)
                if (t < 0) continue
                val values = LinkedHashMap<String, Double>()
                for (key in item.keys()) if (key != "t") (item.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() }?.let { values[key] = it }
                add(HistorySample(t, values))
            }
        }
        fun whole(key: String): Int? = if (root.has(key) && !root.isNull(key)) root.optInt(key) else null
        return SolarHistory(whole("module"), whole("every_s"), whole("retention_days"), samples)
    }

    /** A stack's samples with the power and the spread of the cells worked out from what they carry. */
    fun derived(samples: List<HistorySample>): List<HistorySample> = samples.map { sample ->
        val extra = LinkedHashMap(sample.values)
        val voltage = sample.values["voltage_v"]
        val current = sample.values["current_a"]
        val low = sample.values["cell_min_v"]
        val high = sample.values["cell_max_v"]
        if (voltage != null && current != null) extra["power_w"] = voltage * current
        if (low != null && high != null) extra["delta_mv"] = Math.round((high - low) * 1000).toDouble()
        HistorySample(sample.t, extra)
    }

    fun bankBucketMs(minutes: Int): Long = max(60_000L, (minutes * 60_000L + 599) / 600)

    /**
     * The history of the whole bank from the histories of its stacks, in steps of [bucketMs]. A step is given only when every stack that has a history said something in it: a sum with a
     * stack missing would look like a drop. Currents, powers and energies add up; the voltage and the charge are the mean; the extremes are the extremes.
     */
    fun mergeBank(histories: List<List<HistorySample>>, bucketMs: Long): List<HistorySample> {
        val used = histories.filter { it.isNotEmpty() }
        if (used.isEmpty()) return emptyList()
        val buckets = sortedMapOf<Long, Array<HistorySample?>>()
        used.forEachIndexed { index, history ->
            for (sample in derived(history)) buckets.getOrPut(Math.floorDiv(sample.t, bucketMs) * bucketMs) { arrayOfNulls(used.size) }[index] = sample   // the last of the step
        }
        return buckets.mapNotNull { (t, row) ->
            val filled = row.filterNotNull()
            if (filled.size < used.size) return@mapNotNull null
            val merged = LinkedHashMap<String, Double>()
            fun put(key: String, everyStack: Boolean, how: (List<Double>) -> Double) {
                val values = filled.mapNotNull { it.values[key] }
                if (values.isNotEmpty() && (!everyStack || values.size == used.size)) merged[key] = how(values)
            }
            put("current_a", true) { it.sum() }
            put("power_w", true) { it.sum() }
            put("energy_kwh", true) { it.sum() }
            put("voltage_v", false) { it.average() }
            put("soc_percent", false) { it.average() }
            put("temperature_max_c", false) { values -> values.max() }
            put("cell_min_v", false) { values -> values.min() }
            put("cell_max_v", false) { values -> values.max() }
            val low = merged["cell_min_v"]
            val high = merged["cell_max_v"]
            if (low != null && high != null) merged["delta_mv"] = Math.round((high - low) * 1000).toDouble()
            HistorySample(t, merged)
        }
    }

    /** The ends of the vertical axis: round numbers around the values; a flat line gets room above and below. */
    fun scale(values: List<Double>, ticks: Int = 4): Pair<Double, Double> {
        if (values.isEmpty()) return 0.0 to 1.0
        var low = values.min()
        var high = values.max()
        if (low == high) { low -= 1.0; high += 1.0 }
        if (low > 0 && low < (high - low) * 0.5) low = 0.0
        val raw = (high - low) / max(1, ticks)
        val magnitude = 10.0.pow(floor(log10(raw)))
        val norm = raw / magnitude
        val step = (if (norm < 1.5) 1.0 else if (norm < 3) 2.0 else if (norm < 7) 5.0 else 10.0) * magnitude
        return floor(low / step) * step to kotlin.math.ceil(high / step) * step
    }

    /** The levels worth drawing: those no further from the data than the data is wide, so a far one never flattens the curves. */
    fun levelsInView(values: List<Double>, levels: List<Double>): List<Double> {
        if (values.isEmpty()) return emptyList()
        val low = values.min()
        val high = values.max()
        val reach = max(max(high - low, kotlin.math.abs(high) * 0.1), 0.1)
        return levels.filter { it >= low - reach && it <= high + reach }
    }

    /** The sample nearest to a moment; null when there is none. */
    fun nearest(samples: List<HistorySample>, t: Long): HistorySample? = samples.minByOrNull { kotlin.math.abs(it.t - t) }

    /** A step longer than this between two samples is a gap: the line is broken there instead of being drawn across it. */
    fun gapMs(samples: List<HistorySample>): Long {
        if (samples.size < 3) return Long.MAX_VALUE
        val steps = samples.zipWithNext { a, b -> b.t - a.t }.sorted()
        return min(Long.MAX_VALUE / 8, max(1L, steps[steps.size / 2])) * 4
    }
}
