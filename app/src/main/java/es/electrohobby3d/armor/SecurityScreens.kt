// ARMOR-ANDROID-CONTROL - Security: the second factor of the account, the open sessions of the person, how the server sees this phone, and the sign-in and proof dialogs that go with them.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The same things Studio's Security tab does, in the app: everything can be set up from the phone without a file. The password is typed here only to be sent to the server (it is never kept),
// the recovery codes are shown once and kept nowhere, and a critical action refused from outside the house asks for the password again in a dialog and is tried again by itself.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.PeerConfig
import es.electrohobby3d.armor.model.RemoteInfo
import es.electrohobby3d.armor.model.RemoteSetup
import es.electrohobby3d.armor.model.SessionInfo
import es.electrohobby3d.armor.model.TotpInfo

/** What the Security screen shows and what is in the middle of being done. */
data class SecurityUiState(
    val totp: TotpInfo? = null,
    val sessions: List<SessionInfo> = emptyList(),
    val remote: RemoteInfo? = null,
    /** For an administrator: the VPN and the dynamic DNS name, and the configuration of the device just added or asked for (it holds a private key and is shown once). */
    val setup: RemoteSetup? = null,
    val peerConfig: PeerConfig? = null,
    /** The key and the address of the second factor that is being set up, until the first code confirms it. */
    val setupSecret: String? = null,
    val setupUri: String? = null,
    /** The ten recovery codes, shown once. */
    val recoveryCodes: List<String> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    /** When the account has the second factor on, the sign-in waits here for the code. */
    val loginChallenge: String? = null,
    /** A critical action was refused from outside the house: the dialog asks for the password again. */
    val elevationNeeded: Boolean = false,
    val elevationError: String? = null,
)

/** The phrase for a code the server gave. */
internal fun securityError(code: String): String = when (code) {
    "bad_proof" -> tr("Eso no es correcto.")
    "bad_code" -> tr("Ese código no es correcto.")
    "totp_locked" -> tr("Demasiados códigos erróneos: espera un poco y vuelve a intentarlo.")
    "challenge_expired" -> tr("El acceso ha tardado demasiado: empieza de nuevo.")
    "remote_policy" -> tr("Esto solo se puede hacer desde casa o con la VPN.")
    "already_enabled" -> tr("El segundo factor ya está activado: desactívalo primero.")
    "totp_setup_required" -> tr("Este servidor pide el segundo factor desde fuera de casa. Configúralo antes desde dentro de casa (Configuración > Seguridad).")
    else -> tr("No ha funcionado. No se ha cambiado nada.")
}

private fun whenText(ms: Long): String = if (ms <= 0) "-" else java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(ms))

@Composable
private fun SectionCard(title: String, help: String, content: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(help, color = ArmorColors.Muted, fontSize = 13.sp)
            content()
        }
    }
}

@Composable
private fun PasswordField(value: String, onValue: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value, onValueChange = onValue, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

@Composable
fun SecurityScreen(
    state: SecurityUiState, onStart: (String) -> Unit, onEnable: (String) -> Unit, onDisable: (String) -> Unit, onEndSession: (SessionInfo) -> Unit, onEndOthers: () -> Unit, onCheck: () -> Unit,
    remoteActions: RemoteSetupActions? = null, lock: LockCardActions? = null,
) {
    val uri = LocalUriHandler.current
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        lock?.let { LockSettingsCard(it) }
        SectionCard(tr("Segundo factor (aplicación autenticadora)"), tr("Con él, una contraseña robada no basta para entrar: la aplicación del móvil da un código de seis cifras que cambia cada treinta segundos. Sirve Google Authenticator, Aegis, Authy o cualquier similar.")) {
            state.totp?.let { totp ->
                Text(
                    if (totp.enabled) tr("El segundo factor está ACTIVADO en tu cuenta.") + " " + totp.recoveryLeft + " " + tr("códigos de recuperación restantes") else tr("El segundo factor está desactivado."),
                    fontWeight = FontWeight.Bold, color = if (totp.enabled) ArmorColors.Cyan else ArmorColors.Muted,
                )
            }
            if (state.setupSecret == null && state.totp?.enabled == false) {
                PasswordField(password, { password = it }, tr("Tu contraseña (para confirmar que eres tú)"))
                Button(onClick = { onStart(password); password = "" }, enabled = password.isNotEmpty() && !state.busy) { Text(tr("Configurarlo")) }
            }
            state.setupSecret?.let { secret ->
                Text(tr("Añade esta clave a tu aplicación: abre el enlace en el móvil que tiene la aplicación, o escribe la clave a mano (el tipo es «basado en tiempo»)."), fontSize = 13.sp)
                Text(tr("Clave"), fontWeight = FontWeight.Bold)
                SelectionContainer { Text(secret.chunked(4).joinToString(" "), fontFamily = FontFamily.Monospace, fontSize = 15.sp) }
                state.setupUri?.let { link -> OutlinedButton(onClick = { runCatching { uri.openUri(link) } }) { Text(tr("Abrir en la aplicación")) } }
                OutlinedTextField(
                    value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Código de la aplicación (6 cifras)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                Button(onClick = { onEnable(code); code = "" }, enabled = code.length == 6 && !state.busy) { Text(tr("Activarlo")) }
            }
            if (state.recoveryCodes.isNotEmpty()) {
                Text(tr("Tus códigos de recuperación: guárdalos ahora"), fontWeight = FontWeight.Bold, color = ArmorColors.Amber)
                Text(tr("Cada uno vale una vez, si pierdes el móvil. Solo se muestran ahora y el servidor guarda solo una huella de ellos. Guárdalos en un lugar seguro, no en el mismo móvil."), fontSize = 13.sp)
                SelectionContainer { Text(state.recoveryCodes.joinToString("\n"), fontFamily = FontFamily.Monospace, fontSize = 15.sp) }
            }
            if (state.totp?.enabled == true) {
                PasswordField(password, { password = it }, tr("Tu contraseña (para confirmar que eres tú)"))
                OutlinedButton(onClick = { onDisable(password); password = "" }, enabled = password.isNotEmpty() && !state.busy) { Text(tr("Desactivarlo")) }
            }
        }
        SectionCard(tr("Tus sesiones abiertas"), tr("Cada navegador o móvil con la sesión abierta. Si no reconoces alguno, ciérralo: necesitará tu contraseña de nuevo.")) {
            if (state.sessions.isEmpty()) Text(tr("No hay sesiones que mostrar."), color = ArmorColors.Muted)
            state.sessions.forEach { session ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (session.current) tr("esta sesión") else session.agent.ifBlank { "-" }, fontWeight = FontWeight.Bold)
                    Text(
                        (if (session.remote) tr("desde fuera") else tr("desde casa")) + " · " + session.ip.ifBlank { "-" } + " · " + tr("último uso") + " " + whenText(session.lastSeenMs),
                        color = ArmorColors.Muted, fontSize = 12.sp,
                    )
                    if (!session.current) TextButton(onClick = { onEndSession(session) }) { Text(tr("Cerrarla")) }
                }
            }
            if (state.sessions.size > 1) OutlinedButton(onClick = onEndOthers) { Text(tr("Cerrar todas las demás")) }
        }
        SectionCard(tr("Usar el servidor desde fuera de casa"), tr("Abre el servidor a Internet solo por una puerta: un proxy inverso con certificado válido (el perfil TLS). El broker y los nodos nunca se publican. Pulsa el botón desde un móvil con datos para ver cómo te ve el servidor.")) {
            OutlinedButton(onClick = onCheck, enabled = !state.busy) { Text(tr("Comprobar cómo me ve el servidor")) }
            state.remote?.let { info ->
                Text(tr("Dirección que ve el servidor") + ": " + info.ip)
                Text(tr("Cuenta como") + ": " + if (info.remote) tr("desde fuera") else tr("desde casa"))
                Text(tr("Cookie segura") + ": " + if (info.cookieSecure) tr("sí") else tr("no"))
                Text(tr("Proxy de confianza") + ": " + info.trustProxy)
                Text(tr("Política del segundo factor") + ": " + info.policy)
                if (!info.remote) Text(tr("El servidor te ve dentro de casa. Si estás con datos móviles, el proxy no está pasando tu dirección: indica abajo el proxy de confianza."), color = ArmorColors.Amber, fontSize = 13.sp)
            }
        }
        val setup = state.setup
        if (setup != null && remoteActions != null) RemoteSetupCards(setup, state.peerConfig, state.busy, remoteActions)
        state.message?.let { Text(it, color = ArmorColors.Muted, fontSize = 13.sp) }
    }
}

/** The second step of the sign-in: the code of the app, or a recovery code. */
@Composable
fun SignInCodeStep(code: String, onCode: (String) -> Unit, busy: Boolean, message: String?, onSubmit: () -> Unit, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Segundo paso"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(tr("Escribe el código de seis cifras de tu aplicación autenticadora, o uno de tus códigos de recuperación."), color = ArmorColors.Muted, fontSize = 13.sp)
        OutlinedTextField(
            value = code, onValueChange = onCode, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Código")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel) { Text(tr("Cancelar")) }
            Button(onClick = onSubmit, enabled = code.isNotBlank() && !busy) { Text(tr("Verificar y entrar")) }
        }
        message?.let { Text(it, color = ArmorColors.Alert, fontSize = 13.sp) }
    }
}

/** A critical action was refused from outside the house: the password again (or a code of the app), then the action is tried again by itself. */
@Composable
fun ElevationDialog(error: String?, busy: Boolean, onConfirm: (password: String, code: String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
        title = { Text(tr("Confirma que eres tú")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Estás usando el servidor desde fuera de casa y esta acción es crítica. Escribe tu contraseña de nuevo (o un código de la aplicación si tienes el segundo factor). Vale unos minutos."), fontSize = 13.sp)
                PasswordField(password, { password = it }, tr("Contraseña"))
                OutlinedTextField(
                    value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("o código de la aplicación")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                error?.let { Text(it, color = ArmorColors.Alert, fontSize = 13.sp) }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(password, code) }, enabled = (password.isNotEmpty() || code.length == 6) && !busy) { Text(tr("Confirmar")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
    )
}
