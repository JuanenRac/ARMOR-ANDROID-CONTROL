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
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.HotTub
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
    network: es.electrohobby3d.armor.model.NetworkOverview? = null, onNetwork: () -> Unit = {},
    alarmPanels: es.electrohobby3d.armor.model.AlarmPanelsOverview? = null, onAlarmPanels: () -> Unit = {},
    services: es.electrohobby3d.armor.model.ServicesOverview? = null, onServices: () -> Unit = {},
    recording: Int = 0, onRecordings: () -> Unit = {},
    security: SecurityUiState = SecurityUiState(), onSecurity: () -> Unit = {},
    nodeHealth: es.electrohobby3d.armor.model.NodeHealthOverview? = null, onNodeHealth: () -> Unit = {},
    /** The arming mode in force while the server has the arming modes on ("away", "home", "night"); null otherwise. */
    armingMode: String? = null,
) {
    val attention = devices.count { DeviceText.problem(it) != null }
    // every kind of node (radar, solar, electrical, alarm, wall panel), not only the radar nodes of the live stream
    val census = es.electrohobby3d.armor.model.NodeCensus.of(nodes, nodeHealth)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenTitle(Icons.Filled.Shield, tr("Estado"), if (updated.isBlank()) tr("Esperando datos") else trf("Actualizado a las %s", Friendly.clock(updated))) {
            IconButton(onClick = onRefresh, enabled = enabled) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
        }
        Panel(Modifier.fillMaxWidth(), tint = if (armed) ArmorColors.Ok else ArmorColors.Amber) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(if (armed) Icons.Filled.Shield else Icons.Filled.LockOpen, if (armed) ArmorColors.Ok else ArmorColors.Amber, size = 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(es.electrohobby3d.armor.model.ArmingModeText.armedTitle(armed, armingMode), style = MaterialTheme.typography.headlineSmall)
                    Text(if (armed && armingMode != null && armingMode != "away") tr("Vigila lo marcado para este modo.") else if (armed) tr("Puertas, ventanas y movimiento vigilados.") else tr("Solo humo, gas, agua y pánico avisan."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                FilledIconButton(
                    onClick = onMode, enabled = enabled, modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (armed) ArmorColors.Amber else ArmorColors.Ok, contentColor = Color(0xFF041014)),
                ) { Icon(Icons.Filled.PowerSettingsNew, contentDescription = if (armed) tr("Desarmar") else tr("Armar"), modifier = Modifier.size(30.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Filled.NotificationsActive, if (pendingAlarms == 0) "0" else pendingAlarms.toString(), if (pendingAlarms == 0) tr("Sin alarmas") else tr("Alarmas"), if (pendingAlarms > 0) ArmorColors.Alert else ArmorColors.Ok, Modifier.weight(1f), onAlarms)
            StatTile(Icons.Filled.Videocam, cameras.toString(), if (camerasDown > 0) trf("%s sin señal", camerasDown) else tr("Cámaras"), if (camerasDown > 0) ArmorColors.Amber else ArmorColors.Cyan, Modifier.weight(1f), onCameras)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Filled.Sensors, devices.size.toString(), if (attention > 0) trf("%s con aviso", attention) else tr("Dispositivos"), if (attention > 0) ArmorColors.Amber else ArmorColors.Cyan, Modifier.weight(1f), onDevices)
            StatTile(Icons.Filled.Hub, "${census.online}/${census.total}", tr("Nodos en línea"), if (census.total > 0 && census.online < census.total) ArmorColors.Amber else ArmorColors.Cyan, Modifier.weight(1f), if (nodeHealth != null && !nodeHealth.isEmpty) onNodeHealth else onRadar)
        }
        if (census.families.isNotEmpty()) StatusLink(
            Icons.Filled.Hub, tr("Nodos por tipo"),
            census.families.joinToString(" · ") { entry -> "${es.electrohobby3d.armor.model.NodeHealthText.family(entry.family)} ${entry.online}/${entry.total}" },
            if (census.silent.isNotEmpty()) ArmorColors.Amber else null, if (nodeHealth != null && !nodeHealth.isEmpty) onNodeHealth else onRadar,
        )
        if (solar != null && !solar.isEmpty) Panel(Modifier.fillMaxWidth(), tint = if (solar.totals.stale > 0) ArmorColors.Amber else null, onClick = onSolar) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Filled.WbSunny, if (solar.totals.stale > 0) ArmorColors.Amber else ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(tr("Solar"), fontWeight = FontWeight.SemiBold)
                    Text(if (solar.devices.isEmpty()) tr("Esperando datos") else solarSummary(solar.totals), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                solar.totals.socPercent?.let { Text("${it} %", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
        if (electrical != null && !electrical.isEmpty) Panel(Modifier.fillMaxWidth(), tint = if (electrical.totals.alarms > 0) ArmorColors.Alert else if (electrical.totals.stale > 0) ArmorColors.Amber else null, onClick = onElectrical) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Filled.ElectricBolt, if (electrical.totals.alarms > 0) ArmorColors.Alert else if (electrical.totals.stale > 0) ArmorColors.Amber else ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(tr("Eléctrica"), fontWeight = FontWeight.SemiBold)
                    Text(electricalSummary(electrical.totals), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (network != null && !network.isEmpty) {
            val internetState = network.nodes.first().internet.state
            val networkTint = if (internetState == "down" || internetState == "lan_down") ArmorColors.Alert else if (internetState == "degraded" || network.totals.stale > 0) ArmorColors.Amber else null
            Panel(Modifier.fillMaxWidth(), tint = networkTint, onClick = onNetwork) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconBadge(Icons.Filled.Router, networkTint ?: ArmorColors.Cyan, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(tr("Red"), fontWeight = FontWeight.SemiBold)
                        Text(es.electrohobby3d.armor.model.NetworkText.summary(network), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (alarmPanels != null && !alarmPanels.isEmpty) {
            val totals = alarmPanels.totals
            val panelTint = if (totals.sounding > 0) ArmorColors.Alert else if (totals.stale > 0) ArmorColors.Amber else null
            StatusLink(Icons.Filled.Shield, tr("Centrales de alarma"), trf("%s centrales · %s armadas", totals.nodes, totals.armed) + (if (totals.sounding > 0) " · " + trf("%s sonando", totals.sounding) else "") + (if (totals.stale > 0) " · " + trf("%s sin señal", totals.stale) else ""), panelTint, onAlarmPanels)
        }
        if (services != null && services.services.isNotEmpty()) {
            val running = services.services.count { it.state == "running" || it.state == "online" }
            val failed = services.services.count { it.state == "failed" }
            StatusLink(Icons.Filled.Dns, tr("Servicios"), trf("%s de %s servicios en marcha", running, services.services.size) + (if (failed > 0) " · " + trf("%s con fallo", failed) else ""), if (failed > 0) ArmorColors.Alert else null, onServices)
        }
        if (recording > 0) StatusLink(Icons.Filled.VideoLibrary, tr("Grabaciones"), trf("Grabando %s cámaras", recording), null, onRecordings)
        security.totp?.let { totp ->
            val setup = security.setup
            val vpn = if (setup == null) null else if (!setup.vpnConfigured) tr("VPN sin configurar") else if (setup.vpnRunning) tr("VPN activa") else tr("VPN parada")
            val summary = listOfNotNull(if (totp.enabled) tr("Segundo factor activado") else tr("Segundo factor desactivado"), vpn, security.sessions.count { it.remote }.takeIf { it > 0 }?.let { trf("%s sesiones desde fuera", it) }).joinToString(" · ")
            StatusLink(Icons.Filled.Lock, tr("Seguridad y acceso remoto"), summary, if (!totp.enabled || (setup != null && setup.vpnConfigured && !setup.vpnRunning)) ArmorColors.Amber else null, onSecurity)
        }
        if (nodes.isNotEmpty()) {
            Text(tr("Radares"), style = MaterialTheme.typography.titleMedium, color = ArmorColors.Muted)
            nodes.forEach { node -> NodeRow(node) }
        }
        val others = census.entries.filter { it.family != "radar" }
        if (others.isNotEmpty()) {
            Text(tr("Otros nodos"), style = MaterialTheme.typography.titleMedium, color = ArmorColors.Muted)
            others.forEach { entry -> OtherNodeRow(entry) }
        }
    }
}

/** A one-line card of the status screen that opens the screen it summarises. */
@Composable
private fun StatusLink(icon: ImageVector, title: String, summary: String, tint: Color?, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), tint = tint, onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon, tint ?: ArmorColors.Cyan, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
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

/** A node that is not a radar one (solar, electrical, alarm panel, wall panel): its name, its kind and whether it answers. */
@Composable
private fun OtherNodeRow(entry: es.electrohobby3d.armor.model.CensusEntry) {
    Panel(Modifier.fillMaxWidth(), tint = if (entry.online) null else ArmorColors.Amber) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Filled.Sensors, if (entry.online) ArmorColors.Cyan else ArmorColors.Muted, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(entry.id, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusDot(if (entry.online) ArmorColors.Ok else ArmorColors.Muted, 8.dp)
                    Text(es.electrohobby3d.armor.model.NodeHealthText.family(entry.family) + " · " + (if (entry.online) tr("En línea") else tr("Sin señal")), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
            }
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
                    Text(if (node.online) tr("En línea") else tr("Sin señal"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
            }
            if (node.online) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = tr("Detectados"), tint = if (node.tracks > 0) ArmorColors.Amber else ArmorColors.Muted, modifier = Modifier.size(20.dp))
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
            ScreenTitle(Icons.Filled.NotificationsActive, tr("Alarmas"), if (pending == 0) tr("Todo en calma") else if (pending == 1) tr("1 necesita atención") else trf("%s necesitan atención", pending)) {
                if (pending > 1) IconButton(onClick = onAcknowledgeAll, enabled = enabled) { Icon(Icons.Filled.DoneAll, contentDescription = tr("Confirmar todas"), tint = ArmorColors.Cyan) }
            }
        }
        if (active.isEmpty()) item {
            Panel(Modifier.fillMaxWidth(), tint = ArmorColors.Ok) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Icons.Filled.CheckCircle, ArmorColors.Ok, size = 64.dp)
                    Text(tr("Sin alarmas"), style = MaterialTheme.typography.titleMedium)
                    Text(tr("Cuando algo necesite tu atención aparecerá aquí."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
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
                        DeviceText.alarmHelp(alarm.code)?.let { help -> Text(help, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
                        Text("${alarmSourceName(alarm, devices, cameraNames, solarNames)} · ${Friendly.dayAndClock(alarm.raisedAt)}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                        Text(
                            if (alarm.acknowledged) (if (alarm.cleared) trf("Vista por %s", alarm.acknowledgedBy) else trf("Vista por %s · sigue activa", alarm.acknowledgedBy)) else if (alarm.cleared) tr("Ya terminó · falta confirmarla") else tr("Activa ahora"),
                            style = MaterialTheme.typography.labelSmall, color = if (alarm.acknowledged) ArmorColors.Muted else tint,
                        )
                    }
                    if (!alarm.acknowledged) FilledIconButton(onClick = { onAcknowledge(alarm) }, enabled = enabled) { Icon(Icons.Filled.Done, contentDescription = tr("Confirmar")) }
                }
            }
        }
        item {
            TextButton(onClick = { showClosed = !showClosed }) {
                Icon(Icons.Filled.History, null)
                Spacer(Modifier.width(8.dp))
                Text(if (showClosed) trf("Ocultar cerradas (%s)", closed.size) else trf("Ver cerradas (%s)", closed.size))
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
    "smart_plug" -> Icons.Filled.Power; "smart_light" -> Icons.Filled.Lightbulb; "smart_switch" -> Icons.Filled.ToggleOn; "smart_breaker" -> Icons.Filled.Bolt; "energy_meter" -> Icons.Filled.Speed; "siren" -> Icons.Filled.Campaign; "lock" -> Icons.Filled.Lock; "valve" -> Icons.Filled.Plumbing
    "shutter" -> Icons.Filled.Blinds; "gate_motor" -> Icons.Filled.Fence; "garage_motor" -> Icons.Filled.Garage; "water_heater" -> Icons.Filled.HotTub
    else -> Icons.Filled.Sensors
}

private enum class DeviceFilter(val icon: ImageVector, private val key: String) {
    All(Icons.Filled.GridView, "Todos"), Sensors(Icons.Filled.Sensors, "Sensores"), Actuators(Icons.Filled.Power, "Control"), Problems(Icons.Filled.Warning, "Avisos");
    val label: String get() = tr(key)
}

@Composable
fun DevicesScreen(devices: List<SiteDevice>, onCommand: (SiteDevice, String) -> Unit, enabled: Boolean) {
    var filter by rememberSaveable { mutableStateOf(DeviceFilter.All) }
    val shown = devices.filter {
        when (filter) { DeviceFilter.All -> true; DeviceFilter.Sensors -> !it.actuator; DeviceFilter.Actuators -> it.actuator; DeviceFilter.Problems -> DeviceText.problem(it) != null }
    }
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(2) }) { ScreenTitle(Icons.Filled.Sensors, tr("Dispositivos"), trf("%s en total", devices.size)) }
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
                    Text(if (devices.isEmpty()) tr("Aún no hay dispositivos") else tr("Nada con este filtro"), style = MaterialTheme.typography.titleMedium)
                    if (devices.isEmpty()) Text(tr("Se añaden desde Studio, en el ordenador."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
                }
            }
        }
        gridItems(shown, key = { it.id }) { device -> DeviceTile(device, onCommand, enabled) }
    }
}

@Composable
private fun DeviceTile(device: SiteDevice, onCommand: (SiteDevice, String) -> Unit, enabled: Boolean) {
    val problem = DeviceText.problem(device)
    val danger = problem == tr("activado") || problem == tr("manipulado")
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
            Text(if (state.isEmpty()) tr("sin datos") else state.first(), style = MaterialTheme.typography.bodyMedium, color = if (danger) ArmorColors.Alert else ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (problem != null && !danger) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Warning, null, tint = ArmorColors.Amber, modifier = Modifier.size(16.dp)); Text(problem, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            }
            if (device.actuator && device.canCommand) {
                val (onLabel, offLabel) = commandLabels(device.kind)
                when (device.kind) {
                    "lock", "valve", "shutter", "gate_motor", "garage_motor", "siren" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
