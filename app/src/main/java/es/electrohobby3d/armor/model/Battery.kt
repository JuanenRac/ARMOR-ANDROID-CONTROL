// ARMOR-ANDROID-CONTROL - writing to a Pylontech battery through its gateway node, as the server reports it: the operations that were ordered, how far each one is and how it ended.
// The app offers only the simple operations; the limits, the firmware and the expert console are done in Studio. The service password of the battery is not here and never reaches the phone.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

data class BatteryDiff(val label: String, val before: String, val after: String)

data class BatteryOperationView(
    val id: String, val node: String, val port: Int, val op: String, val actor: String, val updatedAt: String, val stage: String, val summary: String, val risk: String, val atomic: Boolean,
    val canonical: String, val model: String, val serial: String, val softVersion: String, val refusal: String, val outcome: String, val code: String,
    val stepsDone: Int, val stepsTotal: Int, val packetsSent: Int, val packetsTotal: Int, val diff: List<BatteryDiff>, val armedUntil: String, val restorable: Boolean,
) {
    /** Still on its way: the screen asks the server again. */
    val active: Boolean get() = stage in ACTIVE
    val percent: Int get() = when {
        stage == "done" -> 100
        packetsTotal > 0 -> packetsSent * 100 / packetsTotal
        stepsTotal > 0 -> stepsDone * 100 / stepsTotal
        else -> 0
    }
    companion object { val ACTIVE = setOf("arming", "armed", "starting", "running") }
}

data class BatteryOverview(val enabled: Boolean = false, val operations: List<BatteryOperationView> = emptyList())

object BatteryParser {
    /** The operations the phone offers: the ones that need no more than a choice. The rest are done in Studio, where the values can be checked on a larger screen. */
    val phoneOperations: List<String> = listOf("set_clock", "clear_stats", "clear_log", "restart", "shutdown", "soc_capacity", "mosfail_test", "fault_clear", "output_set")
    val outputs: List<String> = listOf("cfet", "dfet", "buzz", "heat")

    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun operation(root: JSONObject): BatteryOperationView {
        val battery = root.optJSONObject("battery") ?: JSONObject()
        return BatteryOperationView(
            id = root.optString("id"), node = root.optString("node"), port = root.optInt("port"), op = root.optString("op"), actor = root.optString("actor"), updatedAt = root.optString("updated_at"),
            stage = root.optString("stage"), summary = root.optString("summary"), risk = root.optString("risk"), atomic = root.optBoolean("atomic"), canonical = root.optString("canonical"),
            model = battery.optString("model"), serial = battery.optString("serial"), softVersion = battery.optString("soft_version"), refusal = root.optString("refusal"),
            outcome = root.optString("outcome"), code = root.optString("code"), stepsDone = root.optInt("steps_done"), stepsTotal = root.optInt("steps_total"),
            packetsSent = root.optInt("packets_sent"), packetsTotal = root.optInt("packets_total"),
            diff = root.optJSONArray("diff").objects().map { BatteryDiff(it.optString("label"), it.optString("before"), it.optString("after")) },
            armedUntil = root.optString("armed_until"), restorable = root.optJSONObject("restore") != null,
        )
    }

    fun overview(root: JSONObject) = BatteryOverview(root.optBoolean("enabled"), root.optJSONArray("operations").objects().map(::operation).filter { it.id.isNotBlank() })

    /** The nodes whose batteries report: where an operation can be ordered. */
    fun nodes(solar: SolarOverview?): List<String> = solar?.devices.orEmpty().filter { it.battery != null }.map { it.nodeId }.distinct().sorted()

    /** A serial number as the node takes it: 10 to 24 letters, digits, - or _ ; NOSERIAL for a battery that has none yet. */
    fun serialLooksRight(text: String): Boolean = text == "NOSERIAL" || Regex("^[A-Za-z0-9_-]{10,24}$").matches(text)
}
