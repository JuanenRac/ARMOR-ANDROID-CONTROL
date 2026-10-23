// ARMOR-ANDROID-CONTROL - the screen that waits behind the lock of the phone, and the switches that set it (in Security).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The locked screen asks for the fingerprint, the face or the PIN of the phone by itself, and always offers the way out for a person who cannot give it (a hand with a wound, a reset
// phone, a new finger): signing in again with the password, which forgets the session and the remembered sign-in kept on the phone.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** The sentence under the lock: what the phone has to ask with. */
internal fun lockAvailabilityText(availability: LockAvailability): String = when (availability) {
    LockAvailability.Biometric -> tr("Este teléfono tiene huella o rostro, y su PIN como alternativa.")
    LockAvailability.DeviceCredential -> tr("Este teléfono solo tiene PIN, patrón o contraseña: se pedirá eso.")
    LockAvailability.None -> tr("Este teléfono no tiene bloqueo de pantalla: sin él no se puede pedir nada. Pon un PIN, un patrón o una contraseña en los ajustes del teléfono.")
}

/** The screen over the whole app until the person is recognised. */
@Composable
fun LockScreen(gate: LockGate, onUnlocked: () -> Unit, onPasswordInstead: () -> Unit) {
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ask = {
        if (!busy) scope.launch {
            busy = true
            val result = gate.unlockToOpen()
            busy = false
            when (result) {
                AuthResult.Success, AuthResult.Unavailable -> onUnlocked()   // a phone that cannot ask is not shut out
                AuthResult.Cancelled -> message = null
                is AuthResult.Failed -> message = result.reason.ifBlank { tr("No se ha podido comprobar. Inténtalo de nuevo.") }
            }
        }
    }
    LaunchedEffect(Unit) { ask() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(Icons.Filled.Lock, ArmorColors.Cyan, size = 72.dp)
        Text(tr("A.R.M.O.R. está bloqueada"), fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Text(tr("Confirma que eres tú con el bloqueo del teléfono para abrirla."), color = ArmorColors.Muted, textAlign = TextAlign.Center)
        message?.let { Text(it, color = ArmorColors.Alert, textAlign = TextAlign.Center) }
        Button(onClick = { ask() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.Icon(Icons.Filled.Fingerprint, null, Modifier.size(20.dp)); androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp)); Text(tr("Desbloquear"))
        }
        OutlinedButton(onClick = onPasswordInstead, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(tr("No puedo: entrar con la contraseña")) }
        Text(tr("Entrar con la contraseña olvida la sesión guardada en este teléfono; el servidor y sus alarmas no se tocan."), color = ArmorColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

/** What the card of the lock does. */
class LockCardActions(
    val status: LockStatus,
    val onSensitive: (Boolean) -> Unit,
    val onOpen: (Boolean) -> Unit,
    val onGrace: (Int) -> Unit,
    /** The key that seals the session is tied to the unlocking of the phone (off by default). */
    val keyBound: Boolean = false,
    val onKeyBound: (Boolean) -> Unit = {},
)

private fun graceText(seconds: Int): String = when (seconds) {
    0 -> tr("Siempre")
    30 -> tr("30 segundos")
    120 -> tr("2 minutos")
    else -> tr("5 minutos")
}

/** The switches of the lock: before the sensitive actions, to open the app, and for how long one unlocking counts. */
@Composable
fun LockSettingsCard(actions: LockCardActions) {
    val status = actions.status
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Bloqueo con el teléfono"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(tr("Pide la huella, el rostro o el PIN del teléfono antes de las acciones delicadas y, si quieres, para abrir la aplicación. Protege un teléfono que se queda sin vigilar; el servidor sigue comprobando por su cuenta la sesión y el PIN."), color = ArmorColors.Muted, fontSize = 13.sp)
            Text(lockAvailabilityText(status.availability), fontSize = 13.sp, color = if (status.availability == LockAvailability.None) ArmorColors.Amber else ArmorColors.Muted)
            SwitchRow(
                tr("Pedirlo para armar, desarmar, silenciar y desbloquear"), tr("También para las órdenes críticas: nodos, baterías y borrar grabaciones."),
                checked = status.sensitiveOn, enabled = !status.sensitiveForced, onChange = actions.onSensitive,
            )
            if (status.sensitiveForced) Text(tr("Tu administrador lo exige en este servidor."), color = ArmorColors.Amber, fontSize = 12.sp)
            SwitchRow(
                tr("Pedirlo también para abrir la aplicación"), tr("Desactivado por defecto. Siempre queda la salida de entrar con la contraseña."),
                checked = status.openOn, enabled = !status.openForced, onChange = actions.onOpen,
            )
            if (status.openForced) Text(tr("Tu administrador lo exige en este servidor."), color = ArmorColors.Amber, fontSize = 12.sp)
            if (status.unmet) Text(tr("Lo pide el servidor o lo has pedido tú, pero este teléfono no tiene bloqueo de pantalla: de momento no se pide nada y puedes usar la aplicación."), color = ArmorColors.Amber, fontSize = 12.sp)
            SwitchRow(
                tr("Atar la clave de la sesión al desbloqueo del teléfono"), tr("Desactivado por defecto. La sesión guardada y el acceso recordado solo se pueden leer con el teléfono desbloqueado."),
                checked = actions.keyBound, enabled = status.availability != LockAvailability.None, onChange = actions.onKeyBound,
            )
            Text(tr("Aviso: con esto, tras reiniciar el teléfono o si Android cierra la aplicación, la vigilancia de alarmas en segundo plano espera a que desbloquees el teléfono y hasta entonces no recibirás ningún aviso."), color = ArmorColors.Amber, fontSize = 12.sp)
            Text(tr("Cuánto vale una comprobación"), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LockSettings.GRACE_CHOICES.forEach { seconds ->
                    FilterChip(selected = status.graceSeconds == seconds, onClick = { actions.onGrace(seconds) }, label = { Text(graceText(seconds)) })
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, help: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(help, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}
