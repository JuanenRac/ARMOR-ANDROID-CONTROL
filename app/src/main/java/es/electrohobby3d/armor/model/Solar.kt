// ARMOR-ANDROID-CONTROL - solar inverters and batteries: what the server reports, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

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
)

data class SolarBatteryReading(
    val modules: Int, val model: String, val state: String, val voltageV: Double?, val currentA: Double?, val temperatureMinC: Double?, val temperatureMaxC: Double?,
    val cellMinV: Double?, val cellMaxV: Double?, val socPercent: Int?, val alarm: Boolean, val capacityAh: Double?, val fullCapacityAh: Double?, val energyKwh: Double?,
    val cycles: Int?, val stack: List<SolarModule>, val healthPercent: Int? = null,
) {
    /** Watts at the battery: positive while charging. */
    val powerW: Double? get() = if (voltageV != null && currentA != null) voltageV * currentA else null
}

/** A piece of equipment that has reported: exactly one of [inverter] and [battery] is set. */
data class SolarDevice(
    val nodeId: String, val device: String, val name: String, val model: String, val stale: Boolean, val example: Boolean, val receivedAt: String,
    val inverter: SolarInverterReading?, val battery: SolarBatteryReading?,
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
    private fun JSONArray?.strings(): List<String> = buildList { if (this@strings != null) for (i in 0 until length()) optString(i).takeIf { it.isNotBlank() }?.let(::add) }

    fun overview(root: JSONObject): SolarOverview = SolarOverview(
        devices = root.optJSONArray("devices").objects().mapNotNull(::device),
        waiting = root.optJSONArray("waiting").objects().mapNotNull(::waiting),
        totals = root.optJSONObject("totals")?.let(::totals) ?: SolarTotals(),
    )

    fun totals(json: JSONObject) = SolarTotals(
        inverters = json.optInt("inverters"), batteries = json.optInt("batteries"), stale = json.optInt("stale"),
        pvW = json.number("pv_w") ?: 0.0, loadW = json.number("load_w") ?: 0.0, batteryW = json.number("battery_w"), socPercent = json.whole("soc_percent"),
        capacityAh = json.number("capacity_ah"), fullCapacityAh = json.number("full_capacity_ah"), energyKwh = json.number("energy_kwh"),
        gridPresent = json.optBoolean("grid_present"), mode = if (json.has("mode") && !json.isNull("mode")) json.optString("mode") else null,
    )

    private fun waiting(json: JSONObject): SolarWaiting? {
        val node = json.optString("node_id"); val device = json.optString("device")
        if (node.isBlank() || device.isBlank()) return null
        return SolarWaiting(node, device, json.optString("name").ifBlank { device }, json.optString("kind"), json.optString("model"))
    }

    fun device(json: JSONObject): SolarDevice? {
        val node = json.optString("node_id"); val device = json.optString("device")
        val reading = json.optJSONObject("reading") ?: return null
        if (node.isBlank() || device.isBlank()) return null
        val registered = json.optJSONObject("registered")
        val name = registered?.optString("name").orEmpty().ifBlank { device }
        val model = registered?.optString("model").orEmpty()
        val common = { inverter: SolarInverterReading?, battery: SolarBatteryReading? ->
            SolarDevice(node, device, name, model, json.optBoolean("stale"), json.optBoolean("example"), json.optString("received_at"), inverter, battery)
        }
        return when (json.optString("kind")) {
            "inverter" -> common(inverter(reading), null)
            "battery" -> common(null, battery(reading))
            else -> null
        }
    }

    private fun inverter(r: JSONObject) = SolarInverterReading(
        mode = r.optString("mode", "unknown"),
        gridV = r.number("grid_v"), gridHz = r.number("grid_hz"), outV = r.number("out_v"), outHz = r.number("out_hz"), outVa = r.number("out_va"), outW = r.number("out_w"),
        loadPercent = r.number("load_percent"), batteryV = r.number("battery_v"), batteryA = r.number("battery_a"), batteryPercent = r.number("battery_percent"),
        pvV = r.number("pv_v"), pvA = r.number("pv_a"), pvW = r.number("pv_w"), heatsinkC = r.number("heatsink_c"),
        acCharging = r.optBoolean("ac_charging"), pvCharging = r.optBoolean("pv_charging"), loadOn = r.optBoolean("load_on"), warnings = r.optJSONArray("warnings").strings(),
        pv2V = r.number("pv2_v"), pv2A = r.number("pv2_a"), pv2W = r.number("pv2_w"), totalOutW = r.number("total_out_w"),
        units = r.optJSONArray("units").objects().map { SolarUnit(it.optInt("unit"), it.optString("mode", "unknown"), it.optString("serial"), it.optString("fault_code"), it.number("out_w"), it.number("load_percent"), it.number("battery_v")) },
    )

    private fun module(m: JSONObject) = SolarModule(
        number = m.optInt("n"), present = m.optBoolean("present"), voltageV = m.number("voltage_v"), currentA = m.number("current_a"), temperatureC = m.number("temperature_c"),
        socPercent = m.whole("soc_percent"), state = m.optString("state"), cellsV = m.optJSONArray("cells_v").numbers(), temperaturesC = m.optJSONArray("temperatures_c").numbers(),
        capacityAh = m.number("capacity_ah"), fullCapacityAh = m.number("full_capacity_ah"), cycles = m.whole("cycles"), healthPercent = m.whole("health_percent"),
    )

    private fun battery(r: JSONObject) = SolarBatteryReading(
        modules = r.optInt("modules"), model = r.optString("model"), state = r.optString("state"), voltageV = r.number("voltage_v"), currentA = r.number("current_a"),
        temperatureMinC = r.number("temperature_min_c"), temperatureMaxC = r.number("temperature_max_c"), cellMinV = r.number("cell_min_v"), cellMaxV = r.number("cell_max_v"),
        socPercent = r.whole("soc_percent"), alarm = r.optBoolean("alarm"), capacityAh = r.number("capacity_ah"), fullCapacityAh = r.number("full_capacity_ah"),
        energyKwh = r.number("energy_kwh"), cycles = r.whole("cycles"), stack = r.optJSONArray("stack").objects().map(::module).filter { it.present }, healthPercent = r.whole("health_percent"),
    )
}

/** The wording of solar equipment, in plain Spanish. */
object SolarText {
    fun mode(mode: String?) = when (mode) {
        "power_on" -> "Arrancando"; "standby" -> "En espera"; "line" -> "Con la red"; "battery" -> "Con la batería"; "fault" -> "Avería"
        "power_saving" -> "Ahorro de energía"; "shutdown" -> "Apagado"; else -> "Sin dato"
    }

    fun batteryState(state: String?) = when (state) { "charging" -> "Cargando"; "discharging" -> "Descargando"; "idle" -> "En reposo"; else -> "Sin dato" }

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
        watts == null -> "Sin dato"
        watts > 20 -> "Cargando · ${watts(watts)}"
        watts < -20 -> "Descargando · ${watts(-watts)}"
        else -> "En reposo"
    }

    /** The spread between the highest and the lowest cell, in millivolts: the number that tells an unbalanced pack. */
    fun cellSpreadMv(cells: List<Double>): Int? = if (cells.size < 2) null else Math.round((cells.max() - cells.min()) * 1000.0).toInt()
}
