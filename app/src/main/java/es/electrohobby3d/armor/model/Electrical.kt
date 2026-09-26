// ARMOR-ANDROID-CONTROL - the electrical network as the electrical nodes measure it: what the server reports, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** One measured channel of a node (a circuit, a line, the grid input, a DC bus); what the meter did not say is null. */
data class ElectricalChannel(
    val id: String, val domain: String, val label: String,
    val voltageV: Double?, val currentA: Double?, val powerW: Double?, val energyKwh: Double?, val frequencyHz: Double?, val powerFactor: Double?,
    /** "closed", "open" or "unknown" for a channel with a switch, or null. */
    val state: String?, val alarm: Boolean,
) { val name get() = label.ifBlank { id } }

data class ElectricalNode(val nodeId: String, val stale: Boolean, val receivedAt: String, val channels: List<ElectricalChannel>)

/** The sums the server makes over the nodes that are reporting. */
data class ElectricalTotals(
    val nodes: Int = 0, val channels: Int = 0, val stale: Int = 0,
    /** Watts from the grid (negative while the house feeds it); null when no node reports the channel called grid. */
    val gridW: Double? = null, val gridKwh: Double? = null, val alarms: Int = 0,
)

data class ElectricalOverview(val nodes: List<ElectricalNode> = emptyList(), val totals: ElectricalTotals = ElectricalTotals()) { val isEmpty get() = nodes.isEmpty() }

object ElectricalParser {
    private fun JSONObject.number(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun overview(root: JSONObject): ElectricalOverview = ElectricalOverview(
        nodes = root.optJSONArray("nodes").objects().mapNotNull(::node),
        totals = root.optJSONObject("totals")?.let(::totals) ?: ElectricalTotals(),
    )

    fun totals(json: JSONObject) = ElectricalTotals(json.optInt("nodes"), json.optInt("channels"), json.optInt("stale"), json.number("grid_w"), json.number("grid_kwh"), json.optInt("alarms"))

    private fun node(json: JSONObject): ElectricalNode? {
        val id = json.optString("node_id")
        val reading = json.optJSONObject("reading") ?: return null
        if (id.isBlank()) return null
        return ElectricalNode(id, json.optBoolean("stale"), json.optString("received_at"), reading.optJSONArray("channels").objects().mapNotNull(::channel))
    }

    private fun channel(c: JSONObject): ElectricalChannel? {
        val id = c.optString("id")
        if (id.isBlank()) return null
        return ElectricalChannel(
            id = id, domain = c.optString("domain", "ac"), label = c.optString("label"),
            voltageV = c.number("voltage_v"), currentA = c.number("current_a"), powerW = c.number("power_w"), energyKwh = c.number("energy_kwh"),
            frequencyHz = c.number("frequency_hz"), powerFactor = c.number("power_factor"),
            state = if (c.has("state") && !c.isNull("state")) c.optString("state") else null, alarm = c.optBoolean("alarm"),
        )
    }
}

/** The wording of the electrical network, in plain Spanish. */
object ElectricalText {
    private fun oneDecimal(value: Double) = String.format(Locale.US, "%.1f", value)

    fun watts(value: Double?): String = when {
        value == null -> "—"
        Math.abs(value) >= 1000 -> "${String.format(Locale.US, "%.2f", value / 1000)} kW"
        else -> "${Math.round(value)} W"
    }
    fun volts(value: Double?): String = if (value == null) "—" else "${oneDecimal(value)} V"
    fun amps(value: Double?): String = if (value == null) "—" else "${String.format(Locale.US, "%.2f", value)} A"
    fun kwh(value: Double?): String = if (value == null) "—" else "${String.format(Locale.US, "%.1f", value)} kWh"

    /** Which way the power goes on the grid input: drawing, feeding the network, or nothing. */
    fun gridFlow(watts: Double?): String = when {
        watts == null -> "Sin dato"
        Math.abs(watts) < 5 -> "Sin consumo de red"
        watts > 0 -> "Consume ${watts(watts)} de la red"
        else -> "Cede ${watts(-watts)} a la red"
    }

    fun switchState(state: String?): String? = when (state) { "closed" -> "Cerrado"; "open" -> "Abierto"; "unknown" -> "Sin confirmar"; else -> null }
}
