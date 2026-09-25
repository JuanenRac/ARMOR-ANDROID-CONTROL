// ARMOR-ANDROID-CONTROL - the Alarms and Devices screens.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.DeviceText
import es.electrohobby3d.armor.model.SiteDevice

/** The words of the two buttons that command an actuator: a lock is locked by "on", a valve is opened by "on". */
internal fun commandLabels(kind: String): Pair<String, String> = when (kind) {
    "lock" -> "Cerrar" to "Abrir"; "valve" -> "Abrir" to "Cerrar"; "siren" -> "Sonar" to "Silenciar"; else -> "Encender" to "Apagar"
}

/** Where an alarm came from, in words: the device's own name when it is one. */
internal fun alarmSourceName(alarm: Alarm, devices: List<SiteDevice>, cameraNames: Map<String, String>): String = when (alarm.sourceType) {
    "device" -> devices.firstOrNull { it.id == alarm.sourceId }?.name ?: alarm.sourceId
    "camera" -> cameraNames[alarm.sourceId] ?: alarm.sourceId
    else -> alarm.sourceId
}

@Composable
internal fun AlarmsPanel(
    active: List<Alarm>, closed: List<Alarm>, devices: List<SiteDevice>, cameraNames: Map<String, String>,
    onAcknowledge: (Alarm) -> Unit, onAcknowledgeAll: () -> Unit, enabled: Boolean,
) {
    val pending = active.count { !it.acknowledged }
    var showClosed by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (pending > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (pending == 0) "Todo en calma" else if (pending == 1) "1 alarma necesita atención" else "$pending alarmas necesitan atención", style = MaterialTheme.typography.titleLarge)
                    Text("Lo que necesita a una persona ahora mismo. Confirmar una alarma no la cierra: se cierra sola cuando su causa termina.", style = MaterialTheme.typography.bodySmall)
                    if (pending > 1) Button(onClick = onAcknowledgeAll, enabled = enabled) { Text("Confirmar todas") }
                }
            }
        }
        items(active, key = { it.id }) { alarm ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(DeviceText.alarmText(alarm.code), style = MaterialTheme.typography.titleMedium)
                        Text("${alarmSourceName(alarm, devices, cameraNames)} · ${DeviceText.severityLabel(alarm.severity)} · ${alarm.raisedAt.replace('T', ' ').removeSuffix("Z").take(19)}", style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (alarm.acknowledged) "Confirmada por ${alarm.acknowledgedBy}${if (alarm.cleared) " · terminada" else " · sigue activa"}"
                            else if (alarm.cleared) "La causa ya terminó · falta confirmarla" else "Activa",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (!alarm.acknowledged) Button(onClick = { onAcknowledge(alarm) }, enabled = enabled) { Text("Confirmar") }
                }
            }
        }
        item {
            TextButton(onClick = { showClosed = !showClosed }) { Text(if (showClosed) "Ocultar el histórico (${closed.size})" else "Ver el histórico (${closed.size})") }
        }
        if (showClosed) {
            if (closed.isEmpty()) item { Text("Todavía no hay alarmas cerradas.", style = MaterialTheme.typography.bodySmall) }
            items(closed.take(30), key = { "c-" + it.id }) { alarm ->
                Text("${DeviceText.alarmText(alarm.code)} · ${alarmSourceName(alarm, devices, cameraNames)} · ${alarm.raisedAt.replace('T', ' ').removeSuffix("Z").take(19)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private enum class DeviceFilter(val label: String) { All("Todos"), Sensors("Sensores"), Actuators("Actuadores"), Problems("Atención") }

@Composable
internal fun DevicesPanel(devices: List<SiteDevice>, onCommand: (SiteDevice, String) -> Unit, enabled: Boolean) {
    var filter by rememberSaveable { mutableStateOf(DeviceFilter.All) }
    val shown = devices.filter {
        when (filter) {
            DeviceFilter.All -> true; DeviceFilter.Sensors -> !it.actuator; DeviceFilter.Actuators -> it.actuator; DeviceFilter.Problems -> DeviceText.problem(it) != null
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dispositivos", style = MaterialTheme.typography.headlineSmall)
        Text("Humo, gas, inundación, puertas y ventanas, movimiento, clima, enchufes, luces, sirenas y cerraduras, por Wi-Fi, Zigbee, Bluetooth o cable. Se añaden y colocan desde Studio.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DeviceFilter.entries.forEach { item -> FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.label) }) }
        }
        if (devices.isEmpty()) Text("El servidor todavía no tiene dispositivos. Añádelos desde Studio.")
        else if (shown.isEmpty()) Text("Ningún dispositivo coincide con el filtro.")
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(shown, key = { it.id }) { device ->
                val problem = DeviceText.problem(device)
                Card(Modifier.fillMaxWidth(), colors = if (problem == "activado" || problem == "manipulado") CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer) else CardDefaults.cardColors()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(listOf(DeviceText.kindLabel(device.kind), DeviceText.protocolLabel(device.protocol), device.location).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(if (device.online) "en línea" else "sin respuesta", style = MaterialTheme.typography.labelSmall, color = if (device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        val state = DeviceText.describeState(device)
                        Text(if (state.isEmpty()) "sin datos todavía" else state.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        problem?.let { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
                        if (device.actuator && device.canCommand) {
                            val (on, off) = commandLabels(device.kind)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onCommand(device, "on") }, enabled = enabled) { Text(on) }
                                OutlinedButton(onClick = { onCommand(device, "off") }, enabled = enabled) { Text(off) }
                                if (device.kind != "siren" && device.kind != "lock" && device.kind != "valve") OutlinedButton(onClick = { onCommand(device, "toggle") }, enabled = enabled) { Text("Alternar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
