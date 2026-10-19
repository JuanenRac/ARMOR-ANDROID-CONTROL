// ARMOR-ANDROID-CONTROL - Battery maintenance: the simple operations on a Pylontech battery (clock, counters, restart, charge capacity, outputs...) through its gateway node, for administrators.
// Two steps with a pause between them: the server has the node read the battery and check everything, the screen shows exactly what will be done, and only then "Do it now" writes (with the
// serial number typed again). The limits, the protections, the firmware and the expert console are done in Studio. See ARMOR-SERVER src/battery_write.ts.
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
import es.electrohobby3d.armor.model.BatteryOperationView
import es.electrohobby3d.armor.model.BatteryOverview
import es.electrohobby3d.armor.model.BatteryParser
import es.electrohobby3d.armor.network.ArmorApiException

data class BatteryUiState(val overview: BatteryOverview? = null, val active: BatteryOperationView? = null, val busy: Boolean = false, val message: String? = null)

/** The name of an operation in the words of the app (the source phrase is Spanish; Tr gives it in the language chosen). */
fun batteryOperationName(op: String): String = when (op) {
    "set_clock" -> tr("Poner en hora el reloj")
    "clear_stats" -> tr("Borrar los contadores de estadísticas")
    "clear_log" -> tr("Borrar el registro")
    "restart" -> tr("Reiniciar el sistema de la batería")
    "shutdown" -> tr("Apagar la batería")
    "soc_capacity" -> tr("Escribir la capacidad total")
    "mosfail_test" -> tr("Activar la prueba de fallo de MOSFET")
    "fault_clear" -> tr("Borrar el fallo de fusible")
    "output_set" -> tr("Conmutar una salida")
    else -> tr("Otra operación (en Studio)")
}

fun batteryOutputName(output: String): String = when (output) {
    "cfet" -> tr("MOSFET de carga")
    "dfet" -> tr("MOSFET de descarga")
    "buzz" -> tr("Zumbador")
    else -> tr("Calefacción")
}

fun batteryStageName(stage: String): String = when (stage) {
    "arming" -> tr("Comprobando")
    "armed" -> tr("Lista para hacer")
    "starting" -> tr("Empezando")
    "running" -> tr("En marcha")
    "done" -> tr("Terminada")
    "rejected" -> tr("Rechazada")
    "expired" -> tr("Caducada")
    "lost" -> tr("Contacto perdido")
    else -> tr("Retirada")
}

fun batteryRiskName(risk: String): String = when (risk) {
    "low" -> tr("bajo")
    "medium" -> tr("medio")
    "high" -> tr("alto")
    else -> tr("no se puede deshacer")
}

/** What the node or the server refused, in words. */
fun batteryRefusalText(code: String): String = when (code) {
    "battery_write_disabled", "disabled" -> tr("La escritura en baterías está desactivada: en el servidor, o en el panel del propio nodo.")
    "unknown_node" -> tr("Ninguna batería de ese nodo está informando.")
    "invalid_request", "invalid_params" -> tr("Revisa el nodo, el puerto, la operación y el número de serie.")
    "busy" -> tr("Otra operación en esta batería sigue en marcha.")
    "serial_mismatch" -> tr("Ese no es el número de serie que informa la batería de este puerto: puerto o batería equivocados.")
    "serial_unreadable" -> tr("La batería no tiene número de serie legible: escribe NOSERIAL solo en las operaciones que lo permiten.")
    "not_armed", "expired", "bad_token" -> tr("Prepara la operación de nuevo.")
    "mqtt_unavailable" -> tr("El broker no está conectado, así que no se pudo enviar la orden.")
    "rate_limited" -> tr("Demasiadas órdenes en un minuto. Espera un poco.")
    "no_password" -> tr("El nodo no tiene guardada la contraseña de servicio de las baterías.")
    "login_failed" -> tr("La batería no aceptó la contraseña de servicio guardada en el nodo.")
    "not_idle" -> tr("La batería está cargando o descargando: espera a que la corriente sea casi cero.")
    "critical_alarm" -> tr("La batería tiene una alarma o protección activa: solo se permiten recuperaciones.")
    "cooldown" -> tr("Se acaba de escribir algo: espera medio minuto.")
    "locked" -> tr("Varios intentos erróneos: el nodo rechaza toda orden durante cinco minutos.")
    "no_battery", "not_pylontech" -> tr("No hay una batería Pylontech respondiendo en ese puerto.")
    "no_answer", "preflight_failed" -> tr("El nodo no pudo leer la batería a tiempo.")
    "clock_not_set" -> tr("El nodo aún no sabe la hora, así que no puede poner en hora la batería.")
    "no_official_value" -> tr("El fabricante no da una capacidad para este modelo.")
    else -> tr("El nodo rechazó la operación.")
}

/** The sentence for a failure of a call: the code the server gave, or a general one. */
fun batteryErrorText(error: Throwable): String = if (error is ArmorApiException) batteryRefusalText(error.apiError.code) else tr("No se pudo completar.")

private fun resultText(operation: BatteryOperationView): String = when (operation.outcome) {
    "ok" -> tr("Funcionó: lo escrito se volvió a leer y coincide.")
    "partial" -> tr("Se detuvo a medias: la batería puede haber quedado a medio cambiar. Mira la transcripción en Studio antes de hacer nada más.")
    "retry" -> tr("La batería no respondió y no se escribió nada: puedes volver a intentarlo.")
    "rejected" -> tr("No se hizo nada: el nodo lo rechazó.")
    else -> tr("La lectura posterior no coincide, o falló un paso antes de escribir nada. Mira el estado real en Studio antes de hacer nada más.")
}

@Composable
private fun Choice(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun OperationPanel(operation: BatteryOperationView, busy: Boolean, serial: String, onExecute: () -> Unit, onCancel: () -> Unit, onNew: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(batteryOperationName(operation.op) + " · " + operation.node + "/" + operation.port, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(batteryStageName(operation.stage), fontWeight = FontWeight.SemiBold, color = ArmorColors.Cyan)
            if (operation.summary.isNotBlank()) Text(operation.summary, fontSize = 13.sp)
            if (operation.model.isNotBlank()) Text(trf("Batería: %s · serie %s · firmware %s", operation.model, operation.serial.ifBlank { "-" }, operation.softVersion), color = ArmorColors.Muted, fontSize = 12.sp)
            if (operation.risk.isNotBlank()) Text(trf("Riesgo: %s", batteryRiskName(operation.risk)), color = if (operation.risk == "high" || operation.risk == "irreversible") ArmorColors.Amber else ArmorColors.Muted, fontSize = 13.sp)
            if (operation.atomic) Text(tr("Una vez que empieza no se puede detener: no apagues nada hasta que termine."), color = ArmorColors.Amber, fontSize = 13.sp)
            when (operation.stage) {
                "arming" -> Text(tr("El nodo está leyendo la batería y comprobándolo todo…"), color = ArmorColors.Muted)
                "armed" -> {
                    Text(tr("Listo. Esto es lo que se hará:"), fontWeight = FontWeight.Bold)
                    Text(operation.canonical, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onExecute, enabled = !busy && serial.isNotBlank()) { Text(tr("Hacerlo ahora")) }
                        OutlinedButton(onClick = onCancel, enabled = !busy) { Text(tr("Retirar")) }
                    }
                }
                "starting", "running" -> {
                    LinearProgressIndicator(progress = { operation.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    if (operation.stepsTotal > 0) Text(trf("Paso %s de %s", operation.stepsDone, operation.stepsTotal), color = ArmorColors.Muted, fontSize = 12.sp)
                    if (!operation.atomic) OutlinedButton(onClick = onCancel, enabled = !busy) { Text(tr("Detener")) }
                }
                "rejected" -> if (operation.refusal.isNotBlank()) Text(batteryRefusalText(operation.refusal), color = ArmorColors.Alert, fontSize = 13.sp)
                "expired", "lost" -> Text(if (operation.stage == "expired") batteryRefusalText("expired") else batteryRefusalText("no_answer"), color = ArmorColors.Alert, fontSize = 13.sp)
                "done" -> {
                    Text(resultText(operation), color = if (operation.outcome == "ok") ArmorColors.Ok else ArmorColors.Alert, fontSize = 13.sp)
                    if (operation.code.isNotBlank()) Text(trf("Detalle: %s", operation.code), color = ArmorColors.Muted, fontSize = 12.sp)
                    Text(tr("Qué cambió"), fontWeight = FontWeight.Bold)
                    if (operation.diff.isEmpty()) Text(tr("Sin diferencias leídas."), color = ArmorColors.Muted)
                    operation.diff.forEach { Text(trf("%s: antes %s · después %s", it.label, it.before.ifBlank { "-" }, it.after.ifBlank { "-" }), fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
            }
            if (!operation.active) OutlinedButton(onClick = onNew) { Text(tr("Nueva operación")) }
        }
    }
}

@Composable
fun BatteryScreen(
    state: BatteryUiState, nodes: List<String>, isAdmin: Boolean,
    onArm: (node: String, port: Int, op: String, output: String, state: String, serial: String) -> Unit, onExecute: (serial: String) -> Unit, onCancel: () -> Unit, onNew: () -> Unit, onOpen: (BatteryOperationView) -> Unit,
) {
    var node by rememberSaveable(nodes) { mutableStateOf(nodes.firstOrNull().orEmpty()) }
    var port by rememberSaveable { mutableStateOf("1") }
    var op by rememberSaveable { mutableStateOf("set_clock") }
    var output by rememberSaveable { mutableStateOf("cfet") }
    var onOff by rememberSaveable { mutableStateOf("off") }
    var serial by rememberSaveable { mutableStateOf("") }
    val active = state.active
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Mantenimiento de baterías"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(tr("Cambia lo que guarda una batería Pylontech por la consola del nodo pasarela. Algunos cambios no se pueden deshacer y el fabricante puede negar la garantía. Los límites, el firmware y la consola de experto se hacen en Studio."), color = ArmorColors.Muted, fontSize = 13.sp)
                if (!isAdmin) Text(tr("Solo un administrador puede usar esta pantalla."), color = ArmorColors.Alert)
                else if (state.overview != null && !state.overview.enabled) Text(tr("La escritura en baterías está desactivada: en el servidor, o en el panel del propio nodo."), color = ArmorColors.Alert)
                state.message?.let { Text(it, color = ArmorColors.Alert, fontSize = 13.sp) }
            }
        }
        if (isAdmin) {
            if (active != null) OperationPanel(active, state.busy, serial, onExecute = { onExecute(serial.trim()) }, onCancel = onCancel, onNew = onNew)
            else Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Nodo pasarela"), fontWeight = FontWeight.SemiBold)
                    if (nodes.isEmpty()) Text(tr("Ningún nodo pasarela con batería está informando."), color = ArmorColors.Muted)
                    nodes.forEach { Choice(node == it, it) { node = it } }
                    OutlinedTextField(
                        value = port, onValueChange = { port = it.filter(Char::isDigit).take(2) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Puerto del nodo (1 a 10)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(tr("Operación"), fontWeight = FontWeight.SemiBold)
                    BatteryParser.phoneOperations.forEach { Choice(op == it, batteryOperationName(it)) { op = it } }
                    if (op == "output_set") {
                        Text(tr("Salida"), fontWeight = FontWeight.SemiBold)
                        BatteryParser.outputs.forEach { Choice(output == it, batteryOutputName(it)) { output = it } }
                        Choice(onOff == "on", tr("encendida")) { onOff = "on" }
                        Choice(onOff == "off", tr("apagada")) { onOff = "off" }
                    }
                    OutlinedTextField(
                        value = serial, onValueChange = { serial = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Número de serie de la batería (o NOSERIAL)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    )
                    val valid = node.isNotBlank() && port.toIntOrNull() in 1..10 && BatteryParser.serialLooksRight(serial.trim())
                    Button(onClick = { onArm(node, port.toInt(), op, output, onOff, serial.trim()) }, enabled = valid && !state.busy && state.overview?.enabled == true) { Text(tr("Preparar")) }
                }
            }
            val history = state.overview?.operations.orEmpty()
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("Operaciones"), fontWeight = FontWeight.Bold)
                    if (history.isEmpty()) Text(tr("Todavía no se ha hecho ninguna operación en baterías."), color = ArmorColors.Muted, fontSize = 13.sp)
                    history.take(10).forEach { item ->
                        TextButton(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                            Text(batteryOperationName(item.op) + " · " + item.node + "/" + item.port + " · " + batteryStageName(item.stage), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
