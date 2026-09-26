// ARMOR-ANDROID-CONTROL - the Status, Alarms and Devices screens: icons first, few words.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.DeviceText
import es.electrohobby3d.armor.model.FieldNode
import es.electrohobby3d.armor.model.SiteDevice

/** The title of a screen: an icon in a disc, the words, and whatever buttons the screen has on its right. */
@Composable
fun ScreenTitle(icon: ImageVector, title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
        actions()
    }
}

// ---- Status ----------------------------------------------------------------------------------------------------------------------

@Composable
fun StatusScreen(
    armed: Boolean, updated: String, nodes: List<FieldNode>, cameras: Int, camerasDown: Int, pendingAlarms: Int, devices: List<SiteDevice>,
    onMode: () -> Unit, onAlarms: () -> Unit, onDevices: () -> Unit, onCameras: () -> Unit, onRadar: () -> Unit, onRefresh: () -> Unit, enabled: Boolean,
    solar: es.electrohobby3d.armor.model.SolarOverview? = null, onSolar: () -> Unit = {},
    electrical: es.electrohobby3d.armor.model.ElectricalOverview? = null, onElectrical: () -> Unit = {},
) {
    val attention = devices.count { DeviceText.problem(it) != null }
    val nodesOnline = nodes.count { it.online }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenTitle(Icons.Filled.Shield, "Estado", if (updated.isBlank()) "Esperando datos" else "Actualizado a las ${Friendly.clock(updated)}") {
            IconButton(onClick = onRefresh, enabled = enabled) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = ArmorColors.Cyan) }
        }
        Panel(Modifier.fillMaxWidth(), tint = if (armed) ArmorColors.Ok else ArmorColors.Amber) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(if (armed) Icons.Filled.Shield else Icons.Filled.LockOpen, if (armed) ArmorColors.Ok else ArmorColors.Amber, size = 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(if (armed) "Armado" else "Desarmado", style = MaterialTheme.typography.headlineSmall)
                    Text(if (armed) "Puertas, ventanas y movimiento vigilados." else "Solo humo, gas, agua y pánico avisan.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                FilledIconButton(
                    onClick = onMode, enabled = enabled, modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (armed) ArmorColors.Amber else ArmorColors.Ok, contentColor = Color(0xFF041014)),
                ) { Icon(Icons.Filled.PowerSettingsNew, contentDescription = if (armed) "Desarmar" else "Armar", modifier = Modifier.size(30.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Filled.NotificationsActive, if (pendingAlarms == 0) "0" else pendingAlarms.toString(), if (pendingAlarms == 0) "Sin alarmas" else "Alarmas", if (pendingAlarms > 0) ArmorColors.Alert else ArmorColors.Ok, Modifier.weight(1f), onAlarms)
            StatTile(Icons.Filled.Videocam, cameras.toString(), if (camerasDown > 0) "$camerasDown sin señal" else "Cámaras", if (camerasDown > 0) ArmorColors.Amber else ArmorColors.Cyan, Modifier.weight(1f), onCameras)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Filled.Sensors, devices.size.toString(), if (attention > 0) "$attention con aviso" else "Dispositivos", if (attention > 0) ArmorColors.Amber else ArmorColors.Cyan, Modifier.weight(1f), onDevices)
            StatTile(Icons.Filled.Radar, "$nodesOnline/${nodes.size}", "Nodos en línea", if (nodes.isNotEmpty() && nodesOnline < nodes.size) ArmorColors.Amber else ArmorColors.Cyan, Modifier.weight(1f), onRadar)
        }
        if (solar != null && !solar.isEmpty) Panel(Modifier.fillMaxWidth(), tint = if (solar.totals.stale > 0) ArmorColors.Amber else null, onClick = onSolar) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Filled.WbSunny, if (solar.totals.stale > 0) ArmorColors.Amber else ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Solar", fontWeight = FontWeight.SemiBold)
                    Text(if (solar.devices.isEmpty()) "Esperando datos" else solarSummary(solar.totals), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                solar.totals.socPercent?.let { Text("$it %", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
        if (electrical != null && !electrical.isEmpty) Panel(Modifier.fillMaxWidth(), tint = if (electrical.totals.alarms > 0) ArmorColors.Alert else if (electrical.totals.stale > 0) ArmorColors.Amber else null, onClick = onElectrical) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Filled.ElectricBolt, if (electrical.totals.alarms > 0) ArmorColors.Alert else if (electrical.totals.stale > 0) ArmorColors.Amber else ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Eléctrica", fontWeight = FontWeight.SemiBold)
                    Text(electricalSummary(electrical.totals), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (nodes.isNotEmpty()) {
            Text("Radares", style = MaterialTheme.typography.titleMedium, color = ArmorColors.Muted)
            nodes.forEach { node -> NodeRow(node) }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Panel(modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, tint, size = 40.dp)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NodeRow(node: FieldNode) {
    val high = node.online && node.alert == "high"
    Panel(Modifier.fillMaxWidth(), tint = if (high) ArmorColors.Alert else null) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Filled.Sensors, if (node.online) ArmorColors.Cyan else ArmorColors.Muted, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(node.id, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusDot(if (node.online) ArmorColors.Ok else ArmorColors.Muted, 8.dp)
                    Text(if (node.online) "En línea" else "Sin señal", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
            }
            if (node.online) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Detectados", tint = if (node.tracks > 0) ArmorColors.Amber else ArmorColors.Muted, modifier = Modifier.size(20.dp))
                    Text(node.tracks.toString(), fontWeight = FontWeight.Bold)
                }
                Icon(if (high) Icons.Filled.Warning else Icons.Filled.CheckCircle, contentDescription = null, tint = if (high) ArmorColors.Alert else ArmorColors.Ok)
            }
        }
    }
}

// ---- Alarms ----------------------------------------------------------------------------------------------------------------------

private fun severityColor(severity: String) = when (severity) { "critical" -> ArmorColors.Alert; "high" -> ArmorColors.Amber; else -> ArmorColors.Cyan }

@Composable
fun AlarmsScreen(active: List<Alarm>, closed: List<Alarm>, devices: List<SiteDevice>, cameraNames: Map<String, String>, onAcknowledge: (Alarm) -> Unit, onAcknowledgeAll: () -> Unit, enabled: Boolean, solarNames: Map<String, String> = emptyMap()) {
    val pending = active.count { !it.acknowledged }
    var showClosed by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            ScreenTitle(Icons.Filled.NotificationsActive, "Alarmas", if (pending == 0) "Todo en calma" else if (pending == 1) "1 necesita atención" else "$pending necesitan atención") {
                if (pending > 1) IconButton(onClick = onAcknowledgeAll, enabled = enabled) { Icon(Icons.Filled.DoneAll, contentDescription = "Confirmar todas", tint = ArmorColors.Cyan) }
            }
        }
        if (active.isEmpty()) item {
            Panel(Modifier.fillMaxWidth(), tint = ArmorColors.Ok) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Icons.Filled.CheckCircle, ArmorColors.Ok, size = 64.dp)
                    Text("Sin alarmas", style = MaterialTheme.typography.titleMedium)
                    Text("Cuando algo necesite tu atención aparecerá aquí.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
                }
            }
        }
        items(active, key = { it.id }) { alarm ->
            val tint = if (alarm.acknowledged || alarm.cleared) ArmorColors.Muted else severityColor(alarm.severity)
            Panel(Modifier.fillMaxWidth(), tint = if (alarm.acknowledged) null else tint) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(if (alarm.cleared) Icons.Filled.CheckCircle else Icons.Filled.Warning, tint, size = 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(DeviceText.alarmText(alarm.code), fontWeight = FontWeight.SemiBold)
                        Text("${alarmSourceName(alarm, devices, cameraNames, solarNames)} · ${Friendly.dayAndClock(alarm.raisedAt)}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                        Text(
                            if (alarm.acknowledged) "Vista por ${alarm.acknowledgedBy}${if (alarm.cleared) "" else " · sigue activa"}" else if (alarm.cleared) "Ya terminó · falta confirmarla" else "Activa ahora",
                            style = MaterialTheme.typography.labelSmall, color = if (alarm.acknowledged) ArmorColors.Muted else tint,
                        )
                    }
                    if (!alarm.acknowledged) FilledIconButton(onClick = { onAcknowledge(alarm) }, enabled = enabled) { Icon(Icons.Filled.Done, contentDescription = "Confirmar") }
                }
            }
        }
        item {
            TextButton(onClick = { showClosed = !showClosed }) {
                Icon(Icons.Filled.History, null)
                Spacer(Modifier.width(8.dp))
                Text(if (showClosed) "Ocultar cerradas (${closed.size})" else "Ver cerradas (${closed.size})")
            }
        }
        if (showClosed) items(closed.take(30), key = { "c-" + it.id }) { alarm ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = ArmorColors.Muted, modifier = Modifier.size(18.dp))
                Text("${DeviceText.alarmText(alarm.code)} · ${alarmSourceName(alarm, devices, cameraNames, solarNames)} · ${Friendly.dayAndClock(alarm.raisedAt)}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            }
        }
    }
}

// ---- Devices ---------------------------------------------------------------------------------------------------------------------

internal fun kindIcon(kind: String): ImageVector = when (kind) {
    "smoke" -> Icons.Filled.LocalFireDepartment; "co" -> Icons.Filled.Air; "gas" -> Icons.Filled.Whatshot; "water_leak" -> Icons.Filled.WaterDrop; "panic_button" -> Icons.Filled.Warning
    "door" -> Icons.Filled.DoorFront; "window" -> Icons.Filled.Window; "motion" -> Icons.AutoMirrored.Filled.DirectionsRun; "glass_break" -> Icons.Filled.GraphicEq; "vibration" -> Icons.Filled.Vibration
    "climate" -> Icons.Filled.Thermostat; "temperature" -> Icons.Filled.DeviceThermostat; "humidity" -> Icons.Filled.Opacity; "light_level" -> Icons.Filled.LightMode
    "smart_plug" -> Icons.Filled.Power; "smart_light" -> Icons.Filled.Lightbulb; "smart_switch" -> Icons.Filled.ToggleOn; "siren" -> Icons.Filled.Campaign; "lock" -> Icons.Filled.Lock; "valve" -> Icons.Filled.Plumbing
    else -> Icons.Filled.Sensors
}

private enum class DeviceFilter(val icon: ImageVector, val label: String) {
    All(Icons.Filled.GridView, "Todos"), Sensors(Icons.Filled.Sensors, "Sensores"), Actuators(Icons.Filled.Power, "Control"), Problems(Icons.Filled.Warning, "Avisos")
}

@Composable
fun DevicesScreen(devices: List<SiteDevice>, onCommand: (SiteDevice, String) -> Unit, enabled: Boolean) {
    var filter by rememberSaveable { mutableStateOf(DeviceFilter.All) }
    val shown = devices.filter {
        when (filter) { DeviceFilter.All -> true; DeviceFilter.Sensors -> !it.actuator; DeviceFilter.Actuators -> it.actuator; DeviceFilter.Problems -> DeviceText.problem(it) != null }
    }
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(2) }) { ScreenTitle(Icons.Filled.Sensors, "Dispositivos", "${devices.size} en total") }
        item(span = { GridItemSpan(2) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceFilter.entries.forEach { item ->
                    FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.label, maxLines = 1) }, leadingIcon = { Icon(item.icon, null, Modifier.size(18.dp)) })
                }
            }
        }
        if (shown.isEmpty()) item(span = { GridItemSpan(2) }) {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Icons.Filled.Sensors, ArmorColors.Muted, size = 64.dp)
                    Text(if (devices.isEmpty()) "Aún no hay dispositivos" else "Nada con este filtro", style = MaterialTheme.typography.titleMedium)
                    if (devices.isEmpty()) Text("Se añaden desde Studio, en el ordenador.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
                }
            }
        }
        gridItems(shown, key = { it.id }) { device -> DeviceTile(device, onCommand, enabled) }
    }
}

@Composable
private fun DeviceTile(device: SiteDevice, onCommand: (SiteDevice, String) -> Unit, enabled: Boolean) {
    val problem = DeviceText.problem(device)
    val danger = problem == "activado" || problem == "manipulado"
    val main = DeviceText.mainField[device.kind]
    val isOn = main != null && device.state[main] == true
    val tint = if (danger) ArmorColors.Alert else if (problem != null) ArmorColors.Amber else if (isOn && device.actuator) ArmorColors.Cyan else ArmorColors.Muted
    Panel(Modifier.fillMaxWidth(), tint = if (problem != null) tint else null) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(kindIcon(device.kind), if (device.online || device.expectedIntervalS == 0) (if (danger) ArmorColors.Alert else ArmorColors.Cyan) else ArmorColors.Muted, size = 40.dp)
                Spacer(Modifier.weight(1f))
                StatusDot(if (device.online) ArmorColors.Ok else ArmorColors.Muted, 10.dp)
            }
            Text(device.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val state = DeviceText.describeState(device)
            Text(if (state.isEmpty()) "sin datos" else state.first(), style = MaterialTheme.typography.bodyMedium, color = if (danger) ArmorColors.Alert else ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (problem != null && !danger) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Warning, null, tint = ArmorColors.Amber, modifier = Modifier.size(16.dp)); Text(problem, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            }
            if (device.actuator && device.canCommand) {
                val (onLabel, offLabel) = commandLabels(device.kind)
                when (device.kind) {
                    "lock", "valve", "siren" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val onIcon = when (device.kind) { "lock" -> Icons.Filled.Lock; "siren" -> Icons.AutoMirrored.Filled.VolumeUp; else -> Icons.Filled.PlayArrow }
                        val offIcon = when (device.kind) { "lock" -> Icons.Filled.LockOpen; "siren" -> Icons.AutoMirrored.Filled.VolumeOff; else -> Icons.Filled.Stop }
                        FilledTonalIconButton(onClick = { onCommand(device, "on") }, enabled = enabled) { Icon(onIcon, contentDescription = onLabel) }
                        FilledTonalIconButton(onClick = { onCommand(device, "off") }, enabled = enabled) { Icon(offIcon, contentDescription = offLabel) }
                    }
                    else -> Switch(checked = isOn, onCheckedChange = { onCommand(device, if (it) "on" else "off") }, enabled = enabled)
                }
            }
        }
    }
}
