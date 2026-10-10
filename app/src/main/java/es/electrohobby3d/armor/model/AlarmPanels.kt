// ARMOR-ANDROID-CONTROL - the alarm panels as the alarm nodes (ARMOR-ALARM) report them: what the server says, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

/** One zone of a panel: a door, a window, motion, smoke... `state` is "normal", "triggered" or "tamper". */
data class AlarmZoneView(val id: String, val name: String, val kind: String, val state: String, val bypassed: Boolean) { val title get() = name.ifBlank { id } }

/** One thing that happened, told as seconds ago (the node may have no clock). */
data class AlarmEventView(val agoSeconds: Long, val kind: String, val zone: String?)

data class AlarmPanelView(
    val nodeId: String, val stale: Boolean, val receivedAt: String,
    /** "disarmed", "exit_delay", "armed", "entry_delay" or "alarm". */
    val phase: String, /** "disarmed", "away" or "stay". */ val mode: String,
    val siren: Boolean, val lockedOut: Boolean, val commandsEnabled: Boolean,
    val zones: List<AlarmZoneView>, val openZones: List<String>, val events: List<AlarmEventView>,
) {
    val sounding get() = phase == "alarm"
    val guarding get() = phase == "armed" || phase == "exit_delay" || phase == "entry_delay"
}

data class AlarmTotals(val nodes: Int = 0, val stale: Int = 0, val armed: Int = 0, val sounding: Int = 0)
data class AlarmPanelsOverview(val nodes: List<AlarmPanelView> = emptyList(), val totals: AlarmTotals = AlarmTotals()) { val isEmpty get() = nodes.isEmpty() }

/** What became of a command sent from the app; `refusal` is "none" when the node accepted it and "timeout" when it never answered. */
data class AlarmCommandRecord(val commandId: String, val nodeId: String, val action: String, val mode: String?, val accepted: Boolean, val refusal: String)
/** Whether the server may command the panels at all (off unless the operator turned it on), and the latest results. */
data class AlarmCommandsStatus(val enabled: Boolean = false, val recent: List<AlarmCommandRecord> = emptyList())

object AlarmPanelsParser {
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun overview(root: JSONObject): AlarmPanelsOverview = AlarmPanelsOverview(
        nodes = root.optJSONArray("nodes").objects().mapNotNull(::panel),
        totals = root.optJSONObject("totals")?.let { AlarmTotals(it.optInt("nodes"), it.optInt("stale"), it.optInt("armed"), it.optInt("sounding")) } ?: AlarmTotals(),
    )

    private fun panel(json: JSONObject): AlarmPanelView? {
        val id = json.optString("node_id")
        val state = json.optJSONObject("state") ?: return null
        if (id.isBlank()) return null
        return AlarmPanelView(
            nodeId = id, stale = json.optBoolean("stale"), receivedAt = json.optString("received_at"),
            phase = state.optString("phase", "disarmed"), mode = state.optString("mode", "disarmed"),
            siren = state.optBoolean("siren"), lockedOut = state.optBoolean("locked_out"), commandsEnabled = state.optBoolean("commands_enabled"),
            zones = state.optJSONArray("zones").objects().mapNotNull(::zone),
            openZones = buildList { val array = state.optJSONArray("open_zones"); if (array != null) for (i in 0 until array.length()) array.optString(i).takeIf { it.isNotBlank() }?.let(::add) },
            events = state.optJSONArray("events").objects().map { AlarmEventView(it.optLong("ago_s"), it.optString("kind"), it.optString("zone").takeIf { zone -> zone.isNotBlank() }) },
        )
    }

    private fun zone(json: JSONObject): AlarmZoneView? {
        val id = json.optString("id")
        if (id.isBlank()) return null
        return AlarmZoneView(id, json.optString("name"), json.optString("kind", "instant"), json.optString("state", "normal"), json.optBoolean("bypassed"))
    }

    fun commands(root: JSONObject): AlarmCommandsStatus = AlarmCommandsStatus(
        enabled = root.optBoolean("enabled"),
        recent = root.optJSONArray("recent").objects().map {
            AlarmCommandRecord(it.optString("command_id"), it.optString("node_id"), it.optString("action"), it.optString("mode").takeIf { mode -> mode.isNotBlank() }, it.optBoolean("accepted"), it.optString("refusal"))
        },
    )
}

/** The wording of the alarm panels, in plain Spanish. */
object AlarmPanelText {
    fun phase(phase: String): String = when (phase) {
        "disarmed" -> "Desarmada"; "exit_delay" -> "Saliendo: retardo de salida"; "armed" -> "Armada"; "entry_delay" -> "Retardo de entrada: desarma ya"; "alarm" -> "¡ALARMA!"; else -> phase
    }
    fun mode(mode: String): String = when (mode) { "away" -> "Fuera"; "stay" -> "En casa"; else -> "Sin armar" }
    fun zoneKind(kind: String): String = when (kind) { "instant" -> "instantánea"; "entry" -> "de entrada"; "interior" -> "interior"; "always" -> "siempre"; else -> kind }
    fun zoneState(state: String): String = when (state) { "normal" -> "cerrada"; "triggered" -> "abierta"; "tamper" -> "sabotaje"; else -> state }

    fun event(kind: String): String = when (kind) {
        "armed" -> "armada"; "exit_delay_started" -> "empezó el retardo de salida"; "entry_delay_started" -> "empezó el retardo de entrada"; "alarm" -> "alarma"
        "siren_timed_out" -> "la sirena se paró sola"; "disarmed" -> "desarmada"; "bad_pin" -> "PIN erróneo"; "locked_out" -> "bloqueada"; "zone_bypassed" -> "excluyó una zona"; else -> kind
    }

    /** "hace 12 s", "hace 3 min", "hace 2 h". */
    fun ago(seconds: Long): String = when {
        seconds < 90 -> "hace $seconds s"
        seconds < 5400 -> "hace ${Math.round(seconds / 60.0)} min"
        else -> "hace ${Math.round(seconds / 3600.0)} h"
    }

    /** Why a command was not accepted (or what the node did), in plain Spanish. */
    fun refusal(refusal: String): String = when (refusal) {
        "none" -> "aceptada"; "not_disarmed" -> "ya estaba armada"; "zones_open" -> "hay una zona abierta que importa"; "not_armed" -> "no estaba armada"
        "bad_pin" -> "PIN erróneo"; "locked_out" -> "bloqueada"; "timeout" -> "el nodo no respondió"; else -> refusal
    }

    /** What the server answered when it refused to send a command (its machine-readable code). */
    fun error(code: String): String = when (code) {
        "commands_disabled" -> "Armar desde aquí está apagado en el servidor"
        "unknown_node" -> "El servidor no conoce ese nodo"
        "node_unavailable" -> "El nodo no ha informado últimamente"
        "node_commands_off" -> "El nodo no acepta órdenes del servidor"
        "busy" -> "Otra orden a este nodo aún espera su respuesta"
        "mqtt_unavailable" -> "El servidor no está conectado al broker"
        else -> "No se pudo enviar la orden"
    }

    /** The line the Status screen shows under the alarm panels tile. */
    fun summary(totals: AlarmTotals): String = when {
        totals.nodes == 0 -> "Sin centrales de alarma"
        totals.sounding > 0 -> if (totals.sounding == 1) "¡Una central está en alarma!" else "¡${totals.sounding} centrales están en alarma!"
        totals.armed > 0 -> if (totals.armed == 1) "Una central vigilando" else "${totals.armed} centrales vigilando"
        else -> "Centrales desarmadas"
    }

    fun confirmDisarm(node: String): String = "¿Desarmar «$node» desde el servidor? No lleva PIN: el nodo, el broker y el servidor lo permiten."
}
