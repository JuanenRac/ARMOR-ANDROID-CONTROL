// ARMOR-ANDROID-CONTROL - solar inverters and batteries: what the server reports, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.tr
import es.electrohobby3d.armor.trf
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** The sums the server makes over the equipment that is reporting (a device that has gone quiet is left out of them). */
data class SolarTotals(
    val inverters: Int = 0,
    val batteries: Int = 0,
    val stale: Int = 0,
    val pvW: Double = 0.0,
    val loadW: Double = 0.0,
    /** Positive while the batteries charge, negative while they discharge; null when nothing reports it. */
    val batteryW: Double? = null,
    val socPercent: Int? = null,
    val capacityAh: Double? = null,
    val fullCapacityAh: Double? = null,
    val energyKwh: Double? = null,
    val gridPresent: Boolean = false,
    val mode: String? = null,
)

data class SolarInverterReading(
    val mode: String,
    val gridV: Double?, val gridHz: Double?, val outV: Double?, val outHz: Double?, val outVa: Double?, val outW: Double?, val loadPercent: Double?,
    val batteryV: Double?, val batteryA: Double?, val batteryPercent: Double?, val pvV: Double?, val pvA: Double?, val pvW: Double?, val heatsinkC: Double?,
    val acCharging: Boolean, val pvCharging: Boolean, val loadOn: Boolean, val warnings: List<String>,
    /** A second PV input (pvW is already the sum of both) and, for a parallel system, the units the node reads and the total power of the system. */
    val pv2V: Double? = null, val pv2A: Double? = null, val pv2W: Double? = null, val units: List<SolarUnit> = emptyList(), val totalOutW: Double? = null,
)

/** One inverter of a parallel system, as the one on the node's port reports it. */
data class SolarUnit(val unit: Int, val mode: String, val serial: String, val faultCode: String, val outW: Double?, val loadPercent: Double?, val batteryV: Double?)

/** One battery module of a stack; a BMS is one module. */
data class SolarModule(
    val number: Int, val present: Boolean, val voltageV: Double?, val currentA: Double?, val temperatureC: Double?, val socPercent: Int?, val state: String,
    val cellsV: List<Double>, val temperaturesC: List<Double>, val capacityAh: Double?, val fullCapacityAh: Double?, val cycles: Int?, val healthPercent: Int? = null,
    /** The counters of the module's life by the console's own name (bat_ov_times ...); what they mean is not official. */
    val counters: Map<String, Long> = emptyMap(),
    /** What the module says about itself: its model (a stack may mix models), the versions of its board and software, its cells, the current it allows each way (amperes) and its console's speed. */
    val model: String = "", val boardVersion: String = "", val mainSoftVersion: String = "", val softVersion: String = "", val cellCount: Int? = null,
    val maxChargeA: Double? = null, val maxDischargeA: Double? = null, val consoleBaud: Int? = null,
    /** The state of health of each cell, in cell order, and the cells (0-based) the battery's console does not call Normal. */
    val sohCounts: List<Int> = emptyList(), val sohAbnormal: List<Int> = emptyList(),
)

/** An entry of a battery's event log: the protection fields are the console's own words (a number and a mnemonic), never decoded. */
data class SolarEvent(val index: Int, val time: String, val voltageState: String, val powerEvents: String, val batteryEvents: String, val systemFault: String)

/** An entry of the log with the module it belongs to and the moment (milliseconds since 1970) the server saw it. */
data class LoggedEvent(val module: Int, val seenAt: Long, val event: SolarEvent)

data class SolarBatteryReading(
    val modules: Int, val model: String, val state: String, val voltageV: Double?, val currentA: Double?, val temperatureMinC: Double?, val temperatureMaxC: Double?,
    val cellMinV: Double?, val cellMaxV: Double?, val socPercent: Int?, val alarm: Boolean, val capacityAh: Double?, val fullCapacityAh: Double?, val energyKwh: Double?,
    val cycles: Int?, val stack: List<SolarModule>, val healthPercent: Int? = null,
) {
    /** Watts at the battery: positive while charging. */
    val powerW: Double? get() = if (voltageV != null && currentA != null) voltageV * currentA else null
}

/** The lowest and the highest cell of a module today, with the cell number and the moment (milliseconds since 1970), and the widest gap of the module. */
data class ModuleDayCells(val number: Int, val minV: Double, val minCell: Int, val minAt: Long, val maxV: Double, val maxCell: Int, val maxAt: Long, val deltaV: Double)

/** What the server kept of the cells of a battery today (its own day): per module, and the widest gap of the whole bank. */
data class DayCells(val modules: List<ModuleDayCells>, val bankDeltaV: Double)

/** A piece of equipment that has reported: exactly one of [inverter] and [battery] is set. */
data class SolarDevice(
    val nodeId: String, val device: String, val name: String, val model: String, val stale: Boolean, val example: Boolean, val receivedAt: String,
    val inverter: SolarInverterReading?, val battery: SolarBatteryReading?, val dayCells: DayCells? = null, val events: List<LoggedEvent> = emptyList(),
) { val isInverter: Boolean get() = inverter != null }

/** Equipment an operator declared in Studio that no gateway node has reported yet. */
data class SolarWaiting(val nodeId: String, val device: String, val name: String, val kind: String, val model: String)

data class SolarOverview(val devices: List<SolarDevice> = emptyList(), val waiting: List<SolarWaiting> = emptyList(), val totals: SolarTotals = SolarTotals()) {
    val isEmpty: Boolean get() = devices.isEmpty() && waiting.isEmpty()
}

object SolarParser {
    private fun JSONObject.number(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
    private fun JSONObject.whole(key: String): Int? = number(key)?.let { Math.round(it).toInt() }
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }
    private fun JSONArray?.numbers(): List<Double> = buildList { if (this@numbers != null) for (i in 0 until length()) optDouble(i).takeIf { !it.isNaN() }?.let(::add) }
    private fun JSONArray?.strings(): List<String> = buildList { if (this@strings != null) for (i in 0 until length()) optStr(i).takeIf { it.isNotBlank() }?.let(::add) }

    fun overview(root: JSONObject): SolarOverview = SolarOverview(
        devices = root.optJSONArray("devices").objects().mapNotNull(::device),
        waiting = root.optJSONArray("waiting").objects().mapNotNull(::waiting),
        totals = root.optJSONObject("totals")?.let(::totals) ?: SolarTotals(),
    )

    fun totals(json: JSONObject) = SolarTotals(
        inverters = json.optInt("inverters"), batteries = json.optInt("batteries"), stale = json.optInt("stale"),
        pvW = json.number("pv_w") ?: 0.0, loadW = json.number("load_w") ?: 0.0, batteryW = json.number("battery_w"), socPercent = json.whole("soc_percent"),
        capacityAh = json.number("capacity_ah"), fullCapacityAh = json.number("full_capacity_ah"), energyKwh = json.number("energy_kwh"),
        gridPresent = json.optBoolean("grid_present"), mode = if (json.has("mode") && !json.isNull("mode")) json.optStr("mode") else null,
    )

    private fun waiting(json: JSONObject): SolarWaiting? {
        val node = json.optStr("node_id"); val device = json.optStr("device")
        if (node.isBlank() || device.isBlank()) return null
        return SolarWaiting(node, device, json.optStr("name").ifBlank { device }, json.optStr("kind"), json.optStr("model"))
    }

    fun device(json: JSONObject): SolarDevice? {
        val node = json.optStr("node_id"); val device = json.optStr("device")
        val reading = json.optJSONObject("reading") ?: return null
        if (node.isBlank() || device.isBlank()) return null
        val registered = json.optJSONObject("registered")
        val name = registered?.optStr("name").orEmpty().ifBlank { device }
        val model = registered?.optStr("model").orEmpty()
        val common = { inverter: SolarInverterReading?, battery: SolarBatteryReading? ->
            SolarDevice(node, device, name, model, json.optBoolean("stale"), json.optBoolean("example"), json.optStr("received_at"), inverter, battery, if (battery != null) dayCells(json.optJSONObject("day_cells")) else null, if (battery != null) events(json.optJSONArray("events")) else emptyList())
        }
        return when (json.optStr("kind")) {
            "inverter" -> common(inverter(reading), null)
            "battery" -> common(null, battery(reading))
            else -> null
        }
    }

    private fun events(list: JSONArray?): List<LoggedEvent> = list.objects().mapNotNull { item ->
        val e = item.optJSONObject("event") ?: return@mapNotNull null
        val module = item.optInt("n", -1)
        if (module < 1 || e.optStr("time").isBlank()) null
        else LoggedEvent(module, item.optLong("seen_at"), SolarEvent(e.optInt("index"), e.optStr("time"), e.optStr("voltage_state"), e.optStr("power_events"), e.optStr("battery_events"), e.optStr("system_fault")))
    }

    private fun JSONArray?.ints(): List<Int> = buildList { if (this@ints != null) for (i in 0 until length()) optInt(i, -1).takeIf { it >= 0 }?.let(::add) }

    private fun dayCells(d: JSONObject?): DayCells? {
        if (d == null) return null
        val modules = d.optJSONArray("modules").objects().mapNotNull { m ->
            val low = m.optJSONObject("min"); val high = m.optJSONObject("max")
            val minV = low?.number("v"); val maxV = high?.number("v")
            if (low == null || high == null || minV == null || maxV == null) null
            else ModuleDayCells(m.optInt("n"), minV, low.optInt("cell"), low.optLong("at"), maxV, high.optInt("cell"), high.optLong("at"), m.number("delta_v") ?: (maxV - minV))
        }
        return DayCells(modules, d.number("bank_delta_v") ?: 0.0)
    }

    private fun inverter(r: JSONObject) = SolarInverterReading(
        mode = r.optStr("mode", "unknown"),
        gridV = r.number("grid_v"), gridHz = r.number("grid_hz"), outV = r.number("out_v"), outHz = r.number("out_hz"), outVa = r.number("out_va"), outW = r.number("out_w"),
        loadPercent = r.number("load_percent"), batteryV = r.number("battery_v"), batteryA = r.number("battery_a"), batteryPercent = r.number("battery_percent"),
        pvV = r.number("pv_v"), pvA = r.number("pv_a"), pvW = r.number("pv_w"), heatsinkC = r.number("heatsink_c"),
        acCharging = r.optBoolean("ac_charging"), pvCharging = r.optBoolean("pv_charging"), loadOn = r.optBoolean("load_on"), warnings = r.optJSONArray("warnings").strings(),
        pv2V = r.number("pv2_v"), pv2A = r.number("pv2_a"), pv2W = r.number("pv2_w"), totalOutW = r.number("total_out_w"),
        units = r.optJSONArray("units").objects().map { SolarUnit(it.optInt("unit"), it.optStr("mode", "unknown"), it.optStr("serial"), it.optStr("fault_code"), it.number("out_w"), it.number("load_percent"), it.number("battery_v")) },
    )

    private fun module(m: JSONObject) = SolarModule(
        number = m.optInt("n"), present = m.optBoolean("present"), voltageV = m.number("voltage_v"), currentA = m.number("current_a"), temperatureC = m.number("temperature_c"),
        socPercent = m.whole("soc_percent"), state = m.optStr("state"), cellsV = m.optJSONArray("cells_v").numbers(), temperaturesC = m.optJSONArray("temperatures_c").numbers(),
        capacityAh = m.number("capacity_ah"), fullCapacityAh = m.number("full_capacity_ah"), cycles = m.whole("cycles"), healthPercent = m.whole("health_percent"),
        counters = m.optJSONObject("counters")?.let { c -> c.keys().asSequence().mapNotNull { k -> c.optLong(k, -1L).takeIf { v -> v >= 0 }?.let { v -> k to v } }.toMap() } ?: emptyMap(),
        model = m.optStr("model"), boardVersion = m.optStr("board_version"), mainSoftVersion = m.optStr("main_soft_version"), softVersion = m.optStr("soft_version"), cellCount = m.whole("cell_count"),
        maxChargeA = m.number("max_charge_a"), maxDischargeA = m.number("max_discharge_a"), consoleBaud = m.whole("console_baud"),
        sohCounts = m.optJSONArray("soh_counts").ints(), sohAbnormal = m.optJSONArray("soh_abnormal").ints(),
    )

    private fun battery(r: JSONObject) = SolarBatteryReading(
        modules = r.optInt("modules"), model = r.optStr("model"), state = r.optStr("state"), voltageV = r.number("voltage_v"), currentA = r.number("current_a"),
        temperatureMinC = r.number("temperature_min_c"), temperatureMaxC = r.number("temperature_max_c"), cellMinV = r.number("cell_min_v"), cellMaxV = r.number("cell_max_v"),
        socPercent = r.whole("soc_percent"), alarm = r.optBoolean("alarm"), capacityAh = r.number("capacity_ah"), fullCapacityAh = r.number("full_capacity_ah"),
        energyKwh = r.number("energy_kwh"), cycles = r.whole("cycles"), stack = r.optJSONArray("stack").objects().map(::module).filter { it.present }, healthPercent = r.whole("health_percent"),
    )
}

/** The wording of solar equipment, in plain Spanish. */
object SolarText {
    /** The counters of protections and errors: above zero they are worth a look (the same list the server tells a rise of). */
    val severeCounters: Set<String> = setOf(
        "lifealarm_times", "bat_ov_times", "bat_hv_times", "bat_uv_times", "input_ov_times", "pwr_ov_times", "pwr_hv_times", "pwr_uv_times", "coc_times", "coc2_times", "doc_times", "doc2_times",
        "sc_times", "cot_times", "dot_times", "bmicerr_times", "system_error_times",
    )
    private val acronyms = setOf("ov", "hv", "lv", "uv", "coc", "coc2", "doc", "doc2", "sc", "coca", "doca", "cot", "cut", "dot", "dut", "cht", "clt", "dht", "dlt", "ht", "lt", "slp", "rv", "bmicerr")

    /** "bat_ov_times" -> "Bat OV Times": the name as the battery's console prints it. */
    fun counterLabel(key: String): String = key.split("_").joinToString(" ") { word -> if (word in acronyms) word.uppercase() else word.replaceFirstChar { it.uppercase() } }

    /** The protection counters of a module that are above zero, as "Bat OV Times 755 · COC Times 1"; empty when there are none. */
    fun raisedCounters(counters: Map<String, Long>): String =
        counters.filter { it.key in severeCounters && it.value > 0 }.toSortedMap().entries.joinToString(" · ") { counterLabel(it.key) + " " + it.value }

    /** How new a model is, from the model a module reports: 1 the older ones, 2 the C, UP and US5000 models, 0 a model nobody knows. */
    fun generation(model: String): Int {
        val name = model.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
        return when {
            name in setOf("US2000", "US2000B", "US2000BPLUS", "US2000PLUS", "US2KBPL", "US3000") -> 1
            name in setOf("US2000C", "US3000C", "US5000", "US5000B", "UP2500", "UP5000") -> 2
            else -> 0
        }
    }

    /**
     * What the makers' manuals say about mixing models in a group: the master (module 1) is the newest model, from the 9th module on only the C, UP and US5000 models are accepted, and a 24 V model
     * (UP2500) does not share a group with 48 V ones. A warning (code to the modules it is about), never a block; a model nobody knows is not judged.
     */
    fun composition(stack: List<SolarModule>): List<Pair<String, List<Int>>> {
        val known = stack.filter { it.present && it.model.isNotBlank() }
        val out = mutableListOf<Pair<String, List<Int>>>()
        known.filter { it.number >= 9 && generation(it.model) == 1 }.map { it.number }.takeIf { it.isNotEmpty() }?.let { out += "beyond8" to it }
        val master = known.firstOrNull { it.number == 1 }
        if (master != null && generation(master.model) > 0) known.filter { it.number != 1 && generation(it.model) > generation(master.model) }.map { it.number }.takeIf { it.isNotEmpty() }?.let { out += "master" to it }
        val small = known.filter { it.model.uppercase().filter { c -> c in 'A'..'Z' || c in '0'..'9' } == "UP2500" }
        val big = known.filter { it !in small && generation(it.model) > 0 }
        if (small.isNotEmpty() && big.isNotEmpty()) out += "voltage" to small.map { it.number }
        return out
    }

    /** The warning of [composition] in words. */
    fun compositionText(code: String): String = when (code) {
        "beyond8" -> tr("A partir del 9.º módulo solo se admiten modelos C, UP y US5000; estos son modelos antiguos:")
        "master" -> tr("El maestro (módulo 1) debería ser el modelo más nuevo del grupo, pero hay un modelo más nuevo entre los esclavos:")
        else -> tr("Un modelo de 24 V (UP2500) no puede compartir grupo con modelos de 48 V:")
    }

    /** The cells (counted from 1, as a person counts them) the battery's console calls abnormal in its state of health. */
    fun abnormalCells(module: SolarModule): List<Int> = module.sohAbnormal.map { it + 1 }

    /** "3.312 V (celda 5, 14:02)"
: a cell extreme of the day as a line; the time is the phone's own clock. */
    fun reach(volts: Double, cell: Int, atMs: Long, cellWord: String): String =
        String.format(java.util.Locale.US, "%.3f V (%s %d, %s)", volts, cellWord, cell, java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(atMs)))

    fun mode(mode: String?) = when (mode) {
        "power_on" -> tr("Arrancando"); "standby" -> tr("En espera"); "line" -> tr("Con la red"); "battery" -> tr("Con la batería"); "fault" -> tr("Avería")
        "power_saving" -> tr("Ahorro de energía"); "shutdown" -> tr("Apagado"); else -> tr("Sin dato")
    }

    fun batteryState(state: String?) = when (state) { "charging" -> tr("Cargando"); "discharging" -> tr("Descargando"); "idle" -> tr("En reposo"); else -> tr("Sin dato") }

    /** The names of the models the server's catalogue lists (brands are the same in every language). An unknown model is its own identifier. */
    private val models = mapOf(
        "voltronic" to "Voltronic", "mpp-solar" to "MPP Solar", "axpert-vm-ii" to "Voltronic Axpert VM II", "axpert-vm-iii" to "Voltronic Axpert VM III",
        "axpert-mks" to "Voltronic Axpert MKS / MKS II", "axpert-mks-iv" to "Voltronic Axpert MKS IV / MKS V", "axpert-king" to "Voltronic Axpert King",
        "pip-ms" to "MPP Solar PIP MS / MSD / MSE / MSX", "pip-hs" to "MPP Solar PIP HS / HSE / LV", "pip-gk" to "MPP Solar PIP-GK / MK",
        "easun-isolar" to "EASun iSolar SMG II / SMH II", "must-ph18" to "Must PV18 / PH18", "revo-vm-iii" to "Revo VM III / Revo II",
        "infinisolar-v" to "MPP Solar InfiniSolar V", "lv5048-hybrid" to "MPP Solar LV5048 Hybrid / LV6048", "sungoldpower" to "SunGoldPower SPH / SPF",
        "pylontech-us2000" to "Pylontech US2000", "pylontech-us2000c" to "Pylontech US2000C", "pylontech-us2000b-plus" to "Pylontech US2000B Plus",
        "pylontech-us2kbpl" to "Pylontech US2KBPL", "pylontech-us3000" to "Pylontech US3000", "pylontech-us3000c" to "Pylontech US3000C",
        "pylontech-us5000" to "Pylontech US5000", "pylontech-up2500" to "Pylontech UP2500", "pylontech-up5000" to "Pylontech UP5000",
        "pylontech-force-l1" to "Pylontech Force L1", "pylontech-force-l2" to "Pylontech Force L2", "pytes-e-box" to "Pytes E-Box 48100R", "ant-bms" to "ANT-BMS",
    )
    private val antVariant = Regex("""^ant-bms-(\d{1,2})s-(\d{2,3})a${'$'}""")

    fun model(model: String): String {
        if (model == "other" || model.isEmpty()) return ""
        models[model]?.let { return it }
        antVariant.find(model)?.let { return "ANT-BMS ${it.groupValues[1].toInt()}S · ${it.groupValues[2].toInt()} A" }
        return model
    }

    /** A warning name of the inverter ("line_fail") as words. */
    fun warning(name: String) = name.replace('_', ' ')

    fun watts(value: Double?): String = when {
        value == null -> "—"
        Math.abs(value) >= 1000 -> String.format(Locale.US, "%.2f kW", value / 1000.0)
        else -> String.format(Locale.US, "%.0f W", value)
    }
    fun volts(value: Double?, digits: Int = 1): String = if (value == null) "—" else String.format(Locale.US, "%.${digits}f V", value)
    fun amps(value: Double?, digits: Int = 1): String = if (value == null) "—" else String.format(Locale.US, "%.${digits}f A", value)
    fun percent(value: Number?): String = if (value == null) "—" else "${Math.round(value.toDouble())} %"
    fun hertz(value: Double?): String = if (value == null) "—" else String.format(Locale.US, "%.1f Hz", value)
    fun celsius(value: Double?): String = if (value == null) "—" else String.format(Locale.US, "%.0f °C", value)
    fun ah(value: Double?): String = if (value == null) "—" else String.format(Locale.US, "%.1f Ah", value)
    fun cell(value: Double): String = String.format(Locale.US, "%.3f", value)

    /** What the batteries do as one short phrase: "Cargando · 340 W". */
    fun batteryFlow(watts: Double?): String = when {
        watts == null -> tr("Sin dato")
        watts > 20 -> trf("Cargando · %s", watts(watts))
        watts < -20 -> trf("Descargando · %s", watts(-watts))
        else -> tr("En reposo")
    }

    /** The spread between the highest and the lowest cell, in millivolts: the number that tells an unbalanced pack. */
    fun cellSpreadMv(cells: List<Double>): Int? = if (cells.size < 2) null else Math.round((cells.max() - cells.min()) * 1000.0).toInt()
}
