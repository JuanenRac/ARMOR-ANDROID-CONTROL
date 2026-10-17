// ARMOR-ANDROID-CONTROL - the alarm panels as the alarm nodes (ARMOR-ALARM) report them: what the server says, its parsing and its Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.tr
import es.electrohobby3d.armor.trf
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
        val id = json.optStr("node_id")
        val state = json.optJSONObject("state") ?: return null
        if (id.isBlank()) return null
        return AlarmPanelView(
            nodeId = id, stale = json.optBoolean("stale"), receivedAt = json.optStr("received_at"),
            phase = state.optStr("phase", "disarmed"), mode = state.optStr("mode", "disarmed"),
            siren = state.optBoolean("siren"), lockedOut = state.optBoolean("locked_out"), commandsEnabled = state.optBoolean("commands_enabled"),
            zones = state.optJSONArray("zones").objects().mapNotNull(::zone),
            openZones = buildList { val array = state.optJSONArray("open_zones"); if (array != null) for (i in 0 until array.length()) array.optStr(i).takeIf { it.isNotBlank() }?.let(::add) },
            events = state.optJSONArray("events").objects().map { AlarmEventView(it.optLong("ago_s"), it.optStr("kind"), it.optStr("zone").takeIf { zone -> zone.isNotBlank() }) },
        )
    }

    private fun zone(json: JSONObject): AlarmZoneView? {
        val id = json.optStr("id")
        if (id.isBlank()) return null
        return AlarmZoneView(id, json.optStr("name"), json.optStr("kind", "instant"), json.optStr("state", "normal"), json.optBoolean("bypassed"))
    }

    fun commands(root: JSONObject): AlarmCommandsStatus = AlarmCommandsStatus(
        enabled = root.optBoolean("enabled"),
        recent = root.optJSONArray("recent").objects().map {
            AlarmCommandRecord(it.optStr("command_id"), it.optStr("node_id"), it.optStr("action"), it.optStr("mode").takeIf { mode -> mode.isNotBlank() }, it.optBoolean("accepted"), it.optStr("refusal"))
        },
    )
}

/** The wording of the alarm panels, in plain Spanish. */
object AlarmPanelText {
    fun phase(phase: String): String = when (phase) {
        "disarmed" -> tr("Desarmada"); "exit_delay" -> tr("Saliendo: retardo de salida"); "armed" -> tr("Armada"); "entry_delay" -> tr("Retardo de entrada: desarma ya"); "alarm" -> tr("¡ALARMA!"); else -> phase
    }
    fun mode(mode: String): String = when (mode) { "away" -> tr("Fuera"); "stay" -> tr("En casa"); else -> tr("Sin armar") }
    fun zoneKind(kind: String): String = when (kind) { "instant" -> tr("instantánea"); "entry" -> tr("de entrada"); "interior" -> tr("interior"); "always" -> tr("siempre"); else -> kind }
    fun zoneState(state: String): String = when (state) { "normal" -> tr("cerrada"); "triggered" -> tr("abierta"); "tamper" -> tr("sabotaje"); "trouble" -> tr("no se puede leer"); else -> state }

    fun event(kind: String): String = when (kind) {
        "armed" -> tr("armada"); "exit_delay_started" -> tr("empezó el retardo de salida"); "entry_delay_started" -> tr("empezó el retardo de entrada"); "alarm" -> tr("alarma")
        "siren_timed_out" -> tr("la sirena se paró sola"); "disarmed" -> tr("desarmada"); "bad_pin" -> tr("PIN erróneo"); "locked_out" -> tr("bloqueada"); "zone_bypassed" -> tr("excluyó una zona"); "siren_silenced" -> tr("la sirena se silenció con el PIN"); else -> kind
    }

    /** "hace 12 s", "hace 3 min", "hace 2 h". */
    fun ago(seconds: Long): String = when {
        seconds < 90 -> trf("hace %s s", seconds)
        seconds < 5400 -> trf("hace %s min", Math.round(seconds / 60.0))
        else -> trf("hace %s h", Math.round(seconds / 3600.0))
    }

    /** Why a command was not accepted (or what the node did), in plain Spanish. */
    fun refusal(refusal: String): String = when (refusal) {
        "none" -> tr("aceptada"); "not_disarmed" -> tr("ya estaba armada"); "zones_open" -> tr("hay una zona abierta que importa"); "not_armed" -> tr("no estaba armada")
        "bad_pin" -> tr("PIN erróneo"); "locked_out" -> tr("bloqueada"); "timeout" -> tr("el nodo no respondió"); else -> refusal
    }

    /** What the server answered when it refused to send a command (its machine-readable code). */
    fun error(code: String): String = when (code) {
        "commands_disabled" -> tr("Armar desde aquí está apagado en el servidor")
        "unknown_node" -> tr("El servidor no conoce ese nodo")
        "node_unavailable" -> tr("El nodo no ha informado últimamente")
        "node_commands_off" -> tr("El nodo no acepta órdenes del servidor")
        "busy" -> tr("Otra orden a este nodo aún espera su respuesta")
        "mqtt_unavailable" -> tr("El servidor no está conectado al broker")
        "wrong_pin" -> tr("El PIN no es correcto")
        "pin_locked" -> tr("Demasiados PIN erróneos: espera un poco")
        "pin_not_set" -> tr("Este usuario no tiene PIN: lo pone un administrador")
        else -> tr("No se pudo enviar la orden")
    }

    /** The line the Status screen shows under the alarm panels tile. */
    fun summary(totals: AlarmTotals): String = when {
        totals.nodes == 0 -> tr("Sin centrales de alarma")
        totals.sounding > 0 -> if (totals.sounding == 1) tr("¡Una central está en alarma!") else trf("¡%s centrales están en alarma!", totals.sounding)
        totals.armed > 0 -> if (totals.armed == 1) tr("Una central vigilando") else trf("%s centrales vigilando", totals.armed)
        else -> tr("Centrales desarmadas")
    }

    fun confirmSilence(node: String): String = trf("¿Silenciar la sirena de «%s» sin desarmar? Escribe tu PIN: el servidor lo comprueba y nunca lo envía al nodo. El sistema sigue armado y la alarma queda registrada.", node)

    fun confirmDisarm(node: String): String = trf("¿Desarmar «%s» desde el servidor? Escribe tu PIN: el servidor lo comprueba y nunca lo envía al nodo.", node)
}
