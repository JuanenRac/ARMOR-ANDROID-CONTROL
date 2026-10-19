// ARMOR-ANDROID-CONTROL - controlling the nodes from the phone, as the server reports it: the orders given (restart, pause, resume, standby, identify, diagnostics, maintenance), for one node or for
// every node, and the line of each node (sent, acknowledged, applied, back...). The token that moves an order never reaches the phone. No operation erases settings or credentials or puts a node
// to deep sleep: the list below is the whole contract. See ARMOR-SERVER src/node_control.ts.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

/** One node's line of an order: how far it went and, once known, the state the node is in. */
data class ControlLineView(
    val nodeId: String, val family: String, val stage: String, val state: String, val maintenance: Boolean, val refusal: String, val untilS: Int?, val backAfterS: Int?,
    val firmwareBefore: String, val firmwareAfter: String, val diagnostics: Map<String, String>,
) {
    /** Over, one way or the other: the screen stops asking. */
    val finished: Boolean get() = stage in NodeControlParser.FINAL_STAGES
}

data class ControlOperationView(
    val id: String, val scope: String, val operation: String, val mode: String, val durationS: Int?, val actor: String, val createdAt: String, val updatedAt: String, val state: String,
    val nodes: List<ControlLineView>,
) {
    val active: Boolean get() = state != "done"
}

object NodeControlParser {
    /** The eight operations of the contract, in the order the screen lists them. */
    val operations: List<String> = listOf("restart", "pause", "resume", "standby", "identify", "diagnostics", "maintenance_on", "maintenance_off")
    /** The operations that leave the house less guarded or a node out of work: the node's name (or the word, for every node) is typed again. */
    val needsConfirm: Set<String> = setOf("restart", "pause", "standby")
    /** The word typed to give one of those three operations to every node. */
    val confirmWord: Map<String, String> = mapOf("restart" to "RESTART", "pause" to "PAUSE", "standby" to "STANDBY")
    /** Minimum, maximum and default seconds of the operations that take a duration. */
    val durations: Map<String, Triple<Int, Int, Int>> = mapOf("pause" to Triple(60, 3600, 3600), "identify" to Triple(5, 300, 30), "maintenance_on" to Triple(60, 28_800, 3600))
    val FINAL_STAGES: Set<String> = setOf("applied", "back", "refused", "no_answer", "not_back")
    /** The families whose pause or standby leaves the house without protection. */
    val guards: Set<String> = setOf("alarm", "radar")

    /** What has to be typed for this order: the node's name for one node, the word for every node; null when nothing is asked. */
    fun confirmation(operation: String, nodeId: String?): String? = if (operation !in needsConfirm) null else if (nodeId == null) confirmWord[operation] else nodeId

    /** The duration to send: the typed one when it is a number inside the operation's range, null when the operation takes none or nothing was typed (the server uses its default). */
    fun duration(operation: String, typed: String): Int? {
        val span = durations[operation] ?: return null
        return typed.trim().toIntOrNull()?.takeIf { it in span.first..span.second }
    }

    /** True when the typed duration is acceptable for the operation: empty (the default is used) or inside its range, and nothing at all for an operation that takes none. */
    fun durationOk(operation: String, typed: String): Boolean {
        val span = durations[operation] ?: return typed.isBlank()
        return typed.isBlank() || typed.trim().toIntOrNull()?.let { it in span.first..span.second } == true
    }

    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun line(root: JSONObject): ControlLineView = ControlLineView(
        nodeId = root.optStr("node_id"), family = root.optStr("family"), stage = root.optStr("stage"), state = root.optStr("state"), maintenance = root.optBoolean("maintenance"),
        refusal = root.optStr("refusal"), untilS = if (root.has("until_s")) root.optInt("until_s") else null, backAfterS = if (root.has("back_after_s")) root.optInt("back_after_s") else null,
        firmwareBefore = root.optStr("firmware_before"), firmwareAfter = root.optStr("firmware_after"),
        diagnostics = root.optJSONObject("diagnostics")?.let { found -> found.keys().asSequence().associateWith { found.optStr(it) } } ?: emptyMap(),
    )

    fun operation(root: JSONObject): ControlOperationView = ControlOperationView(
        id = root.optStr("id"), scope = root.optStr("scope"), operation = root.optStr("operation"), mode = root.optStr("mode", "now"), durationS = if (root.has("duration_s")) root.optInt("duration_s") else null,
        actor = root.optStr("actor"), createdAt = root.optStr("created_at"), updatedAt = root.optStr("updated_at"), state = root.optStr("state"),
        nodes = root.optJSONArray("nodes").objects().map(::line).filter { it.nodeId.isNotBlank() },
    )

    fun list(root: JSONObject): List<ControlOperationView> = root.optJSONArray("operations").objects().map(::operation).filter { it.id.isNotBlank() }
}
