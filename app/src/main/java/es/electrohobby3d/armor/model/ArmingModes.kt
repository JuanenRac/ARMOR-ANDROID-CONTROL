// ARMOR-ANDROID-CONTROL - the arming modes of the SERVER (Away, Home, Night): what it answers, their parsing and their Spanish wording.
// Not the modes of one alarm panel (ArmProfileView): these decide which zones, sensors, nodes and cameras the server defends.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.tr
import es.electrohobby3d.armor.trf
import org.json.JSONObject

/**
 * GET /api/v1/alarm/modes. [enabled] is false until an administrator turns the modes on in Studio (then the app arms and disarms as it always did); [current] is "disarmed",
 * "away", "home" or "night"; [available] is what can be asked; [unassigned] the modes that defend nothing yet; [defended] how many references Home and Night mark.
 */
data class ArmingModesView(
    val enabled: Boolean = false, val current: String = "disarmed", val available: List<String> = emptyList(), val unassigned: List<String> = emptyList(),
    val downgradePinRequired: Boolean = true, val defended: Map<String, Int> = emptyMap(),
) {
    /** The armed modes that can be asked for, in the order Away, Home, Night. */
    val armable: List<String> get() = ARMED.filter { it in available }
    companion object { val ARMED = listOf("away", "home", "night") }
}

/** A mode refused by the server: [reason] is its code ("no_zones_assigned", "wrong_pin", "pin_locked", "pin_not_set", "modes_disabled", or "failed"). */
class ArmingModeException(val reason: String, message: String, code: Int?) : ArmorApiException(message, code)

object ArmingModesParser {
    fun parse(root: JSONObject): ArmingModesView {
        fun strings(key: String): List<String> = buildList { root.optJSONArray(key)?.let { array -> for (i in 0 until array.length()) array.optString(i).takeIf { it.isNotBlank() }?.let(::add) } }
        val modes = root.optJSONObject("modes")
        val defended = ArmingModesView.ARMED.drop(1).associateWith { mode -> modes?.optJSONObject(mode)?.optJSONArray("defends")?.length() ?: 0 }
        val current = root.optString("current", "disarmed").takeIf { it in setOf("disarmed", "away", "home", "night") } ?: "disarmed"
        return ArmingModesView(
            enabled = root.optBoolean("enabled"), current = current, available = strings("available").filter { it in setOf("disarmed", "away", "home", "night") },
            unassigned = strings("unassigned"), downgradePinRequired = root.optBoolean("downgrade_pin_required", true), defended = defended,
        )
    }
}

object ArmingModeText {
    fun name(mode: String): String = when (mode) {
        "away" -> tr("Modo fuera de casa"); "home" -> tr("Modo en casa"); "night" -> tr("Modo noche"); else -> tr("Desarmado")
    }
    /** "Armado" or, while the modes are on, "Armado · En casa". */
    fun armedTitle(armed: Boolean, mode: String?): String = if (!armed) tr("Desarmado") else if (mode == null || mode == "disarmed") tr("Armado") else trf("Armado · %s", name(mode))
    /** What the picker says under each mode. */
    fun defends(view: ArmingModesView, mode: String): String = when {
        mode == "away" -> tr("defiende todo")
        mode in view.unassigned -> tr("aún nada asignado")
        else -> trf("%s marcados", view.defended[mode] ?: 0)
    }
    /** The refusal of a mode in words. */
    fun refusal(reason: String): String = when (reason) {
        "modes_disabled" -> tr("Los modos de armado no están activados en el servidor.")
        "no_zones_assigned" -> tr("Ese modo no tiene nada asignado: no se armó.")
        "wrong_pin", "pin_locked", "pin_not_set" -> AlarmPanelText.error(reason)
        else -> tr("No se pudo cambiar el modo.")
    }
}
