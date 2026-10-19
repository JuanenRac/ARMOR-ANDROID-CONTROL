// ARMOR-ANDROID-CONTROL - Control of the nodes, for administrators: restart, pause, resume, standby ("work off, still connected"), identify, diagnostics and maintenance, for one node or for all of them,
// and how far each node went. A restart, a pause and a standby ask for the node's name typed again (the word RESTART, PAUSE or STANDBY for all of them); the new proof of identity the server asks for
// is asked by the same dialog as for the other critical actions. Nothing here erases settings or credentials or puts a node to deep sleep. See ARMOR-SERVER src/node_control.ts.
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
import es.electrohobby3d.armor.model.ControlLineView
import es.electrohobby3d.armor.model.ControlOperationView
import es.electrohobby3d.armor.model.NodeControlParser
import es.electrohobby3d.armor.model.NodeHealthText
import es.electrohobby3d.armor.model.NodeHealthView
import es.electrohobby3d.armor.network.ArmorApiException

data class NodeControlUiState(val operations: List<ControlOperationView> = emptyList(), val active: ControlOperationView? = null, val busy: Boolean = false, val message: String? = null)

/** The name of an operation in the words of the app (the source phrase is Spanish; Tr gives it in the language chosen). Standby is never described as a power-off. */
fun controlOperationName(operation: String): String = when (operation) {
    "restart" -> tr("Reiniciar")
    "pause" -> tr("Pausar (temporal)")
    "resume" -> tr("Reanudar")
    "standby" -> tr("Poner en espera (trabajo apagado, sigue conectado)")
    "identify" -> tr("Identificar (parpadeo y aviso)")
    "diagnostics" -> tr("Pedir un diagnóstico")
    "maintenance_on" -> tr("Activar el mantenimiento")
    "maintenance_off" -> tr("Terminar el mantenimiento")
    else -> operation
}

/** The step of a node's line in words. */
fun controlStageName(stage: String): String = when (stage) {
    "pending" -> tr("Pendiente")
    "armed" -> tr("Preparada")
    "sent" -> tr("Enviada")
    "acknowledged" -> tr("El nodo la recibió y se reinicia")
    "applied" -> tr("Hecho")
    "offline" -> tr("Sin conexión (reiniciando)")
    "back" -> tr("De vuelta")
    "refused" -> tr("Rechazada")
    "no_answer" -> tr("No contestó")
    "not_back" -> tr("No volvió a tiempo")
    else -> stage
}

/** The state a node is in after an order, in words. */
fun controlStateName(state: String, maintenance: Boolean): String {
    val base = when (state) {
        "running" -> tr("En marcha")
        "paused" -> tr("En pausa")
        "standby" -> tr("En espera (sigue conectado)")
        else -> ""
    }
    val extra = if (maintenance) tr("en mantenimiento") else ""
    return listOf(base, extra).filter { it.isNotBlank() }.joinToString(" · ")
}

/** What the server or the node refused, in words. */
fun controlRefusalText(code: String): String = when (code) {
    "invalid_request" -> tr("Revisa la orden: la confirmación, la duración o el modo no son válidos.")
    "unknown_node" -> tr("Ese nodo no ha informado todavía.")
    "busy" -> tr("Ya hay una orden en marcha para ese nodo.")
    "node_unavailable" -> tr("El nodo no ha informado hace un momento.")
    "no_nodes" -> tr("Ningún nodo ha informado hace un momento.")
    "already_in_state" -> tr("El nodo ya está en ese estado.")
    "rate_limited" -> tr("Demasiadas órdenes seguidas (un reinicio por minuto y nodo). Espera un poco.")
    "mqtt_unavailable" -> tr("El broker no está conectado, así que no se pudo enviar la orden.")
    "too_many_nodes" -> tr("Un reinicio escalonado admite hasta 64 nodos.")
    "busy_update" -> tr("El nodo está instalando un firmware.")
    "busy_upload" -> tr("El nodo está recibiendo una subida de ficheros.")
    "busy_slot_copy" -> tr("El nodo está copiando una versión de su firmware.")
    "busy_battery" -> tr("El nodo está haciendo una operación en una batería.")
    "already_restarting" -> tr("El nodo ya se está reiniciando.")
    "not_armed", "invalid_token", "token_expired" -> tr("El nodo no aceptó la orden a tiempo. Inténtalo de nuevo.")
    "unavailable" -> tr("El nodo no puede hacerlo ahora.")
    else -> tr("El nodo rechazó la orden.")
}

/** The sentence for a failure of a call: the code the server gave, or a general one. */
fun controlErrorText(error: Throwable): String = if (error is ArmorApiException) controlRefusalText(error.apiError.code) else tr("No se pudo completar.")

/** The warning to read before an order: what it leaves undone. [guards] says the node (or one of the nodes) is an alarm panel or a radar. */
fun controlWarning(operation: String, all: Boolean, mode: String, guards: Boolean): String? {
    val text = when (operation) {
        "restart" -> if (all && mode == "now") tr("Con todos a la vez, la casa queda sin vigilar cerca de un minuto. El reinicio escalonado deja la central de alarma la última y es el recomendable.")
            else if (all) tr("El reinicio escalonado va nodo a nodo, con la central de alarma la última. Cada nodo tarda cerca de un minuto en volver.")
            else if (guards) tr("El nodo estará fuera de servicio cerca de un minuto: una central de alarma o un radar reiniciado deja la casa sin vigilar mientras tanto.")
            else tr("El nodo estará fuera de servicio cerca de un minuto.")
        "pause" -> tr("Mientras está en pausa el nodo no hace su trabajo, y se reanuda solo al terminar el tiempo. Sigue conectado.")
        "standby" -> tr("En espera el nodo no hace su trabajo pero sigue conectado y contesta. No se reanuda solo, sigue así aunque se reinicie y hay que reanudarlo desde aquí o desde Studio. No es apagarlo.")
        "maintenance_on" -> tr("En mantenimiento el servidor deja de avisar de que el nodo no responde. No cambia lo que hace el nodo.")
        "identify" -> tr("El nodo parpadea o muestra un aviso. Nunca suena la sirena.")
        "diagnostics" -> tr("Solo lee: no cambia nada en el nodo.")
        "resume" -> tr("El nodo vuelve a su trabajo normal.")
        else -> return null
    }
    return if (guards && (operation == "pause" || operation == "standby")) tr("Atención: con una central de alarma o un radar así, la casa NO está protegida.") + " " + text else text
}

@Composable
private fun Choice(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 14.sp)
    }
}

private fun lineColour(line: ControlLineView) = when (line.stage) {
    "refused", "no_answer", "not_back" -> ArmorColors.Alert
    "applied", "back" -> ArmorColors.Ok
    else -> ArmorColors.Cyan
}

@Composable
private fun LineRow(line: ControlLineView) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(line.nodeId + " · " + NodeHealthText.family(line.family), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(controlStageName(line.stage), color = lineColour(line), fontSize = 13.sp)
        if (line.state.isNotBlank()) Text(trf("Estado: %s", controlStateName(line.state, line.maintenance)), fontSize = 12.sp)
        line.untilS?.takeIf { it > 0 }?.let { Text(trf("Quedan %s s", it), color = ArmorColors.Muted, fontSize = 12.sp) }
        line.backAfterS?.let { Text(trf("Volvió a los %s s", it), color = ArmorColors.Muted, fontSize = 12.sp) }
        if (line.firmwareAfter.isNotBlank()) Text(trf("Firmware %s", line.firmwareAfter), color = ArmorColors.Muted, fontSize = 12.sp)
        if (line.stage == "refused" && line.refusal.isNotBlank()) Text(controlRefusalText(line.refusal), color = ArmorColors.Alert, fontSize = 12.sp)
        if (line.diagnostics.isNotEmpty()) {
            val d = line.diagnostics
            Text(
                trf("Diagnóstico: firmware %s · encendido %s s · memoria libre %s · mínima %s", d["firmware"].orEmpty().ifBlank { "-" }, d["uptime_s"].orEmpty().ifBlank { "-" }, d["heap_free"].orEmpty().ifBlank { "-" }, d["heap_min"].orEmpty().ifBlank { "-" }),
                fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun OperationPanel(operation: ControlOperationView, onClose: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val target = if (operation.scope == "all") tr("Todos los nodos") else operation.nodes.firstOrNull()?.nodeId.orEmpty()
            Text(controlOperationName(operation.operation) + " · " + target, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(if (operation.active) tr("En curso") else tr("Terminada"), color = ArmorColors.Cyan, fontWeight = FontWeight.SemiBold)
            if (operation.active) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            operation.nodes.forEach { LineRow(it) }
            if (!operation.active) OutlinedButton(onClick = onClose) { Text(tr("Nueva orden")) }
        }
    }
}

@Composable
fun NodeControlScreen(
    state: NodeControlUiState, nodes: List<NodeHealthView>, isAdmin: Boolean,
    onOrderNode: (nodeId: String, operation: String, confirm: String?, durationS: Int?) -> Unit,
    onOrderAll: (operation: String, mode: String, confirm: String?, durationS: Int?) -> Unit,
    onClose: () -> Unit, onOpen: (ControlOperationView) -> Unit,
) {
    var target by rememberSaveable { mutableStateOf("") }   // "" is every node
    var operation by rememberSaveable { mutableStateOf("identify") }
    var mode by rememberSaveable { mutableStateOf("rolling") }
    var seconds by rememberSaveable { mutableStateOf("") }
    var typed by rememberSaveable { mutableStateOf("") }
    val all = target.isEmpty()
    val active = state.active
    val reporting = nodes.filter { !it.silent }
    val effectiveMode = if (all && operation == "restart") mode else "now"
    val needed = NodeControlParser.confirmation(operation, if (all) null else target)
    val guards = if (all) reporting.any { it.family in NodeControlParser.guards } else nodes.firstOrNull { it.nodeId == target }?.family.orEmpty() in NodeControlParser.guards
    val ready = isAdmin && !state.busy && (if (all) reporting.isNotEmpty() else nodes.any { it.nodeId == target }) && NodeControlParser.durationOk(operation, seconds) && (needed == null || typed == needed)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Control de nodos"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(tr("Ordena a un nodo, o a todos, reiniciarse, pausar su trabajo, ponerse en espera, identificarse, dar un diagnóstico o entrar en mantenimiento. Nunca borra ajustes ni credenciales y nunca apaga el nodo: sigue conectado."), color = ArmorColors.Muted, fontSize = 13.sp)
                if (!isAdmin) Text(tr("Solo un administrador puede usar esta pantalla."), color = ArmorColors.Alert)
                state.message?.let { Text(it, color = ArmorColors.Alert, fontSize = 13.sp) }
            }
        }
        if (isAdmin) {
            if (active != null) OperationPanel(active, onClose)
            else Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Nodo"), fontWeight = FontWeight.SemiBold)
                    if (nodes.isEmpty()) Text(tr("Ningún nodo está informando."), color = ArmorColors.Muted)
                    Choice(all, tr("Todos los nodos")) { target = ""; typed = "" }
                    nodes.sortedBy { it.nodeId }.forEach { node ->
                        val where = controlStateName(node.controlState, node.maintenance)
                        Choice(target == node.nodeId, node.nodeId + " · " + NodeHealthText.family(node.family) + if (where.isNotBlank()) " · $where" else "") { target = node.nodeId; typed = "" }
                    }
                    Text(tr("Operación"), fontWeight = FontWeight.SemiBold)
                    NodeControlParser.operations.forEach { Choice(operation == it, controlOperationName(it)) { operation = it; typed = ""; seconds = "" } }
                    if (all && operation == "restart") {
                        Text(tr("Modo"), fontWeight = FontWeight.SemiBold)
                        Choice(mode == "rolling", tr("Escalonado: uno tras otro, la central de alarma la última (recomendado)")) { mode = "rolling" }
                        Choice(mode == "now", tr("Todos a la vez")) { mode = "now" }
                    }
                    NodeControlParser.durations[operation]?.let { span ->
                        OutlinedTextField(
                            value = seconds, onValueChange = { seconds = it.filter(Char::isDigit).take(5) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            label = { Text(trf("Duración en segundos (%s a %s, vacío: %s)", span.first, span.second, span.third)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    controlWarning(operation, all, effectiveMode, guards)?.let { Text(it, color = if (operation == "diagnostics" || operation == "identify" || operation == "resume") ArmorColors.Muted else ArmorColors.Amber, fontSize = 13.sp) }
                    if (needed != null) {
                        OutlinedTextField(
                            value = typed, onValueChange = { typed = it.take(64) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (all) trf("Escribe la palabra %s para confirmar", needed) else tr("Escribe el nombre del nodo para confirmar")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        )
                    }
                    Button(
                        onClick = {
                            val duration = NodeControlParser.duration(operation, seconds)
                            val confirm = if (needed != null) typed else null
                            if (all) onOrderAll(operation, effectiveMode, confirm, duration) else onOrderNode(target, operation, confirm, duration)
                            typed = ""
                        },
                        enabled = ready,
                    ) { Text(tr("Enviar la orden")) }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("Órdenes recientes"), fontWeight = FontWeight.Bold)
                    if (state.operations.isEmpty()) Text(tr("Todavía no se ha dado ninguna orden a los nodos."), color = ArmorColors.Muted, fontSize = 13.sp)
                    state.operations.take(10).forEach { item ->
                        TextButton(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                            val who = if (item.scope == "all") tr("Todos los nodos") else item.nodes.firstOrNull()?.nodeId.orEmpty()
                            Text(controlOperationName(item.operation) + " · " + who + " · " + if (item.active) tr("En curso") else tr("Terminada"), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
