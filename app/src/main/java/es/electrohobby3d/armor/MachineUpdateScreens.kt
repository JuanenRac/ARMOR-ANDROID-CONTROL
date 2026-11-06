// ARMOR-ANDROID-CONTROL - Update the programs of the machine that runs the server, for administrators: choose a program, see what is installed against the newest release (nothing changes), and
// apply the update with the program's name typed again, a new proof of identity and the one-time code. Only a signed release is put in place; the switch is off at the server and on the
// maintenance helper until the owner turns it on, and then this screen offers nothing. See ARMOR-SERVER src/routes/machine_update.ts.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.MachineJobView
import es.electrohobby3d.armor.model.MachineUpdateParser
import es.electrohobby3d.armor.model.MachineUpdatePreview
import es.electrohobby3d.armor.model.MachineUpdateState
import es.electrohobby3d.armor.network.ArmorApiException
import kotlinx.coroutines.delay

data class MachineUpdateUiState(
    val state: MachineUpdateState? = null, val preview: MachineUpdatePreview? = null, val previewAtMs: Long = 0L,
    val job: MachineJobView? = null, val jobLog: List<String> = emptyList(), val busy: Boolean = false, val message: String? = null,
)

/** Why the update is not on, in words. The switch is the owner's: off at the server (ARMOR_CM5_UPDATE) and on the maintenance helper until they turn it on. */
fun machineUpdateReasonText(reason: String): String = when (reason) {
    "switched_off" -> tr("La actualización desde el móvil está apagada en el servidor. Se enciende en el servidor, no desde la app.")
    "not_configured" -> tr("El servidor no tiene configurado el ayudante de mantenimiento.")
    "agent_switched_off" -> tr("El ayudante de mantenimiento tiene la actualización apagada.")
    "not_answering" -> tr("El ayudante de mantenimiento no contesta.")
    "refused" -> tr("El ayudante de mantenimiento rechazó la consulta.")
    else -> tr("La actualización desde el móvil no está disponible.")
}

/** The sentence for a refusal of the server on this screen. The 202 of the apply is not a refusal; everything else the server can say has its words. */
fun machineUpdateRefusalText(status: Int?, detail: String): String = when {
    status == 403 && detail == "machine_update_disabled" -> tr("La actualización desde el móvil está apagada en el servidor o en el ayudante de mantenimiento.")
    status == 403 -> tr("No se aceptó: puede faltar el código de un solo uso, o la actualización está apagada.")
    status == 404 -> tr("El plan ya no existe o ya se usó. Comprueba las versiones otra vez.")
    status == 410 -> tr("El plan ha caducado. Comprueba las versiones otra vez.")
    status == 409 -> tr("Ya hay otro trabajo en marcha en el equipo.")
    status == 422 && detail == "confirmation_mismatch" -> tr("El nombre escrito no coincide con el del programa.")
    status == 422 && detail == "bad_code" -> tr("El código de un solo uso tiene seis cifras.")
    status == 422 && detail == "bad_plan" -> tr("El plan no es válido. Comprueba las versiones otra vez.")
    status == 422 -> tr("Ese programa no está en la lista de los que se pueden actualizar.")
    status == 429 -> tr("Se actualizó otro programa hace un momento. Espera unos minutos.")
    status == 503 -> tr("El ayudante de mantenimiento no está instalado o no contesta.")
    else -> tr("No se pudo completar.")
}

fun machineUpdateErrorText(error: Throwable): String =
    if (error is ArmorApiException) machineUpdateRefusalText(error.apiError.status ?: error.code, error.apiError.detail) else tr("No se pudo completar.")

/** A step of the job in words: the part after the program ("server.download" is "download"). */
fun machineStepName(id: String): String = when (MachineUpdateParser.stepPart(id)) {
    "download" -> tr("Descargar")
    "verify" -> tr("Comprobar la firma y el resumen")
    "install" -> tr("Instalar")
    "stage" -> tr("Probar antes de cambiar")
    "restart" -> tr("Reiniciar el servicio")
    "check" -> tr("Comprobar que funciona")
    else -> id
}

@Composable
private fun Choice(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun JobPanel(job: MachineJobView, log: List<String>, onClose: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Actualización en curso"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                when (job.state) { "done" -> tr("Terminada"); "failed" -> tr("Falló"); else -> tr("En curso") },
                color = when (job.state) { "done" -> ArmorColors.Ok; "failed" -> ArmorColors.Alert; else -> ArmorColors.Cyan }, fontWeight = FontWeight.SemiBold,
            )
            if (job.running) LinearProgressIndicator(progress = { (job.percent / 100.0).toFloat() }, modifier = Modifier.fillMaxWidth())
            job.steps.forEach { step ->
                Text(machineStepName(step.id) + " · " + when (step.state) { "done" -> tr("Hecho"); "running" -> "${step.percent.toInt()} %"; "skipped" -> tr("Omitido"); "failed" -> tr("Falló"); else -> tr("Pendiente") }, fontSize = 13.sp)
            }
            if (job.failed && job.error.isNotBlank()) Text(job.error.take(300), color = ArmorColors.Alert, fontSize = 12.sp)
            if (log.isNotEmpty()) {
                Text(tr("Registro"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(log.takeLast(MachineUpdateParser.LOG_LINES).joinToString("\n"), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ArmorColors.Muted)
            }
            if (!job.running) OutlinedButton(onClick = onClose) { Text(tr("Nueva comprobación")) }
        }
    }
}

@Composable
fun MachineUpdateScreen(
    ui: MachineUpdateUiState, isAdmin: Boolean,
    onCheck: (project: String) -> Unit, onApply: (project: String, planId: String, confirm: String, code: String) -> Unit, onClose: () -> Unit,
) {
    var project by rememberSaveable { mutableStateOf("") }
    var typed by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ui.preview?.planId) { while (ui.preview?.planId?.isNotEmpty() == true) { now = System.currentTimeMillis(); delay(1_000) } }
    val state = ui.state
    val preview = ui.preview
    val job = ui.job
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Actualizar el equipo"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(tr("Actualiza desde el móvil los programas del equipo donde corre el servidor. Primero se ve qué hay instalado y qué hay publicado, sin cambiar nada; después se confirma escribiendo el nombre del programa. Solo se instala una versión firmada."), color = ArmorColors.Muted, fontSize = 13.sp)
                if (!isAdmin) Text(tr("Solo un administrador puede usar esta pantalla."), color = ArmorColors.Alert)
                ui.message?.let { Text(it, color = ArmorColors.Alert, fontSize = 13.sp) }
            }
        }
        if (isAdmin) {
            if (job != null) JobPanel(job, ui.jobLog, onClose)
            else if (state == null) Text(if (ui.busy) tr("Cargando…") else tr("No se pudo consultar el estado de la actualización."), color = ArmorColors.Muted, fontSize = 13.sp)
            else if (!state.enabled) Panel(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(machineUpdateReasonText(state.reason), color = ArmorColors.Amber, fontSize = 13.sp) } }
            else {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Programa"), fontWeight = FontWeight.SemiBold)
                        if (state.projects.isEmpty()) Text(tr("El ayudante no ofrece ningún programa."), color = ArmorColors.Muted)
                        state.projects.sorted().forEach { name -> Choice(project == name, name) { project = name; typed = ""; code = "" } }
                        Button(onClick = { onCheck(project) }, enabled = !ui.busy && MachineUpdateParser.isProject(project)) { Text(tr("Comprobar versiones")) }
                    }
                }
                if (preview != null) Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(preview.project, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(trf("Instalada: %s · Publicada: %s", preview.localVersion.ifBlank { "-" }, preview.githubVersion.ifBlank { "-" }), fontSize = 13.sp)
                        if (!preview.updateAvailable || preview.planId.isEmpty()) Text(tr("Ya está al día."), color = ArmorColors.Ok)
                        else {
                            val left = MachineUpdateParser.secondsLeft(preview.expiresInS, now - ui.previewAtMs)
                            Text(trf("Hay una versión nueva: %s → %s", preview.localVersion.ifBlank { "-" }, preview.githubVersion.ifBlank { "-" }), color = ArmorColors.Cyan, fontWeight = FontWeight.SemiBold)
                            Text(if (left > 0) trf("El plan vale una sola vez y caduca en %s s.", left) else tr("El plan ha caducado. Comprueba las versiones otra vez."), color = if (left > 0) ArmorColors.Muted else ArmorColors.Amber, fontSize = 12.sp)
                            OutlinedTextField(
                                value = typed, onValueChange = { typed = it.take(64) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("Escribe el nombre del programa para confirmar")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            )
                            OutlinedTextField(
                                value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("Código de un solo uso (si tu cuenta tiene segundo factor)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            Text(tr("Desde fuera de casa se pedirá la contraseña otra vez. Si la versión no está firmada, no se toca nada."), color = ArmorColors.Muted, fontSize = 12.sp)
                            Button(
                                onClick = { onApply(preview.project, preview.planId, typed, code); typed = ""; code = "" },
                                enabled = !ui.busy && left > 0 && MachineUpdateParser.confirmationOk(preview.project, typed) && MachineUpdateParser.codeOk(code),
                            ) { Text(tr("Actualizar ahora")) }
                        }
                    }
                }
            }
        }
    }
}
