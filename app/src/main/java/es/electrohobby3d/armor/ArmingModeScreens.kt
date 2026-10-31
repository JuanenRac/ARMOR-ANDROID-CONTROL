// ARMOR-ANDROID-CONTROL - the picker that arms the SERVER in a mode (Away, Home, Night), shown only while the server has the arming modes on. A mode that defends nothing asks to be
// confirmed; a change to a mode that defends less than the one in force asks for the PIN, like a disarm (the server decides and says so; the picker never guesses which change lowers the
// defence). Disarming keeps its own PIN question.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.ArmingModeText
import es.electrohobby3d.armor.model.ArmingModesView
import es.electrohobby3d.armor.model.Preflight
import es.electrohobby3d.armor.model.PreflightText
import kotlinx.coroutines.launch

/** Where the picker is: choosing, showing what the check before arming found, asking to confirm a mode with nothing assigned, or asking for the PIN. */
internal sealed interface ArmingStage {
    data object Pick : ArmingStage
    data class Check(val mode: String, val check: Preflight) : ArmingStage
    data class Confirm(val mode: String) : ArmingStage
    data class Pin(val mode: String, val problem: String? = null) : ArmingStage
}

/**
 * The check before arming applies to an arm from disarmed, as to a plain arm (a switch between modes is not an arm): the picker shows what it found when it found something and goes straight
 * to the server when the perimeter is fine or the check could not be had. Null means: go on and arm.
 */
internal fun checkStage(mode: String, systemArmed: Boolean, check: Preflight?): ArmingStage.Check? =
    if (!systemArmed && check != null && check.issues.isNotEmpty()) ArmingStage.Check(mode, check) else null

/**
 * The next stage after the server refused with [reason] while arming [mode]; null when the picker has nothing more to ask (the message is shown). [pinTyped] says that a PIN was already
 * sent: a wrong one is then told, while the first refusal for a missing PIN only asks for it.
 */
internal fun nextArmingStage(mode: String, reason: String, pinTyped: Boolean): ArmingStage? = when (reason) {
    "no_zones_assigned" -> ArmingStage.Confirm(mode)
    "wrong_pin", "pin_locked", "pin_not_set" -> if (reason == "wrong_pin" && !pinTyped) ArmingStage.Pin(mode) else ArmingStage.Pin(mode, reason)
    else -> null
}

@Composable
internal fun ArmingModeDialog(
    modes: ArmingModesView, armed: Boolean,
    arm: suspend (mode: String, pin: String?, confirmUnassigned: Boolean) -> Pair<String, String>?,
    preflight: suspend () -> Preflight?,
    onDisarm: () -> Unit, onDismiss: () -> Unit,
) {
    var stage by remember { mutableStateOf<ArmingStage>(ArmingStage.Pick) }
    var pin by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var needsConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun attempt(mode: String, typed: String?, confirm: Boolean) {
        scope.launch {
            busy = true; problem = null
            val refusal = arm(mode, typed, confirm)
            busy = false
            if (refusal == null || refusal.first == "elevation") { onDismiss(); return@launch }
            if (refusal.first == "no_zones_assigned") needsConfirm = true
            val next = nextArmingStage(mode, refusal.first, typed != null)
            if (next != null) { stage = next; if (next is ArmingStage.Pin) pin = "" } else problem = refusal.second
        }
    }
    fun begin(mode: String) {
        scope.launch {
            busy = true; problem = null
            val found = if (armed) null else preflight()
            busy = false
            val check = checkStage(mode, armed, found)
            if (check != null) stage = check else attempt(mode, null, false)
        }
    }
    when (val shown = stage) {
        is ArmingStage.Check -> AlertDialog(
            onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
            icon = { IconBadge(Icons.Filled.Shield, if (shown.check.ready) ArmorColors.Ok else ArmorColors.Amber, size = 56.dp) },
            title = { Text(trf("Armar: %s", ArmingModeText.name(shown.mode))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shown.check.issues.forEach { issue ->
                        Text((if (issue.blocks) "⛔ " else "⚠ ") + PreflightText.describe(issue), style = MaterialTheme.typography.bodySmall, color = if (issue.blocks) ArmorColors.Amber else ArmorColors.Muted)
                    }
                    problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Alert) }
                }
            },
            confirmButton = { Button(enabled = !busy, onClick = { attempt(shown.mode, null, false) }) { Text(if (shown.check.ready) tr("Armar") else tr("Armar igualmente")) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        )
        is ArmingStage.Pin -> AlertDialog(
            onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
            icon = { IconBadge(Icons.Filled.Shield, ArmorColors.Amber, size = 56.dp) },
            title = { Text(tr("Hace falta el PIN")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trf("%s defiende menos que el modo actual: pide tu PIN, como desarmar.", ArmingModeText.name(shown.mode)))
                    OutlinedTextField(
                        value = pin, onValueChange = { pin = onlyDigits(it) }, singleLine = true, label = { Text(tr("PIN (si tienes uno)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(),
                    )
                    shown.problem?.let { Text(ArmingModeText.refusal(it), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Alert) }
                    problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Alert) }
                }
            },
            confirmButton = { Button(enabled = !busy && pin.isNotEmpty(), onClick = { attempt(shown.mode, pin, needsConfirm) }) { Text(trf("Armar: %s", ArmingModeText.name(shown.mode))) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        )
        is ArmingStage.Confirm -> AlertDialog(
            onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
            icon = { IconBadge(Icons.Filled.Shield, ArmorColors.Amber, size = 56.dp) },
            title = { Text(trf("Nada asignado a %s", ArmingModeText.name(shown.mode))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trf("%s no tiene zonas ni sensores: armarlo no defendería nada. Asígnalos en Studio o arma igualmente.", ArmingModeText.name(shown.mode)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                        Text(tr("Armar igualmente"))
                    }
                    problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Alert) }
                }
            },
            confirmButton = { Button(enabled = !busy && confirmed, onClick = { attempt(shown.mode, null, true) }) { Text(tr("Armar igualmente")) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        )
        ArmingStage.Pick -> AlertDialog(
            onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
            icon = { IconBadge(Icons.Filled.Shield, ArmorColors.Ok, size = 56.dp) },
            title = { Text(tr("Modo de armado")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trf("Ahora: %s", ArmingModeText.name(modes.current)), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    Text(tr("Fuera de casa defiende todo. En casa y Noche, solo lo que marcaste en el diseñador de alarma de Studio."))
                    modes.armable.forEach { mode ->
                        val here = mode == modes.current
                        OutlinedButton(onClick = { begin(mode) }, enabled = !busy && !here, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(if (here) ArmingModeText.name(mode) else if (armed) trf("Cambiar a %s", ArmingModeText.name(mode)) else trf("Armar: %s", ArmingModeText.name(mode)))
                                Text(if (here) tr("en vigor") else ArmingModeText.defends(modes, mode), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                            }
                        }
                    }
                    problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Alert) }
                }
            },
            confirmButton = { if (armed) Button(onClick = onDisarm) { Text(tr("Desarmar")) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        )
    }
}
