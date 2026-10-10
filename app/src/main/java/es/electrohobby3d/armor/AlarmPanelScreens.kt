// ARMOR-ANDROID-CONTROL - the alarm panels: whether each alarm node is guarding, its zones and its last events, and (for an administrator, when the server allows it) arming and disarming.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.AlarmCommandsStatus
import es.electrohobby3d.armor.model.AlarmPanelText
import es.electrohobby3d.armor.model.AlarmPanelView
import es.electrohobby3d.armor.model.AlarmPanelsOverview
import es.electrohobby3d.armor.model.NetworkOverview

@Composable
fun AlarmPanelsScreen(
    overview: AlarmPanelsOverview?, commands: AlarmCommandsStatus, canCommand: Boolean, network: NetworkOverview? = null,
    onScanNetwork: () -> Unit = {}, scanning: Boolean = false, onArm: (AlarmPanelView, String, Boolean) -> Unit = { _, _, _ -> }, onDisarm: (AlarmPanelView) -> Unit = {},
) {
    val data = overview ?: AlarmPanelsOverview()
    var confirmDisarm by remember { mutableStateOf<AlarmPanelView?>(null) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { NodeFinderPanel(network, data.nodes.map { it.nodeId }, onScanNetwork, scanning) }
        item {
            Text(
                when {
                    overview == null -> "Buscando datos…"
                    data.nodes.isEmpty() -> "Sin centrales de alarma leyendo"
                    data.totals.stale > 0 -> "${data.totals.stale} sin señal de ${data.nodes.size}"
                    else -> AlarmPanelText.summary(data.totals)
                },
                style = MaterialTheme.typography.bodySmall, color = if (data.totals.sounding > 0) ArmorColors.Alert else ArmorColors.Muted,
            )
        }
        if (canCommand && !commands.enabled) item { Text("Armar desde aquí está apagado en el servidor (ARMOR_ALARM_COMMANDS).", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber) }
        if (data.isEmpty) item { EmptyAlarmPanels() }
        items(data.nodes, key = { it.nodeId }) { panel ->
            AlarmPanelCard(panel, allowed = canCommand && commands.enabled && !panel.stale && panel.commandsEnabled, recent = commands.recent.filter { it.nodeId == panel.nodeId }.take(3), onArm = onArm, onDisarm = { confirmDisarm = panel })
        }
    }
    confirmDisarm?.let { panel ->
        AlertDialog(
            onDismissRequest = { confirmDisarm = null }, containerColor = ArmorColors.SurfaceRaised,
            icon = { IconBadge(Icons.Filled.Shield, ArmorColors.Amber, size = 56.dp) },
            title = { Text("¿Desarmar «${panel.nodeId}»?") },
            text = { Text(AlarmPanelText.confirmDisarm(panel.nodeId)) },
            confirmButton = { Button(onClick = { onDisarm(panel); confirmDisarm = null }) { Text("Sí, desarmar") } },
            dismissButton = { TextButton(onClick = { confirmDisarm = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EmptyAlarmPanels() {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Filled.Shield, ArmorColors.Muted, size = 64.dp)
            Text("Aún no hay centrales de alarma", style = MaterialTheme.typography.titleMedium)
            Text("Cuando un nodo ARMOR-ALARM publique su estado, aparecerá aquí con sus zonas y sus últimos eventos.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun AlarmPanelCard(panel: AlarmPanelView, allowed: Boolean, recent: List<es.electrohobby3d.armor.model.AlarmCommandRecord>, onArm: (AlarmPanelView, String, Boolean) -> Unit, onDisarm: () -> Unit) {
    var force by remember { mutableStateOf(false) }
    val tint = if (panel.stale) ArmorColors.Muted else if (panel.sounding) ArmorColors.Alert else null
    Panel(Modifier.fillMaxWidth(), tint = tint) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.Shield, tint ?: if (panel.guarding) ArmorColors.Ok else ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(panel.nodeId, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(AlarmPanelText.phase(panel.phase) + if (panel.mode != "disarmed") " · ${AlarmPanelText.mode(panel.mode)}" else "", style = MaterialTheme.typography.bodySmall, color = tint ?: ArmorColors.Muted)
                }
                Text(if (panel.stale) "sin señal" else "en línea", style = MaterialTheme.typography.labelSmall, color = if (panel.stale) ArmorColors.Amber else ArmorColors.Ok)
            }
            if (panel.siren) Text("La sirena está sonando", style = MaterialTheme.typography.labelMedium, color = ArmorColors.Alert)
            if (panel.lockedOut) Text("Bloqueada tras PIN erróneos", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            if (panel.zones.isEmpty()) Text("El nodo aún no vigila ninguna zona", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            panel.zones.forEach { zone ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(zone.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(AlarmPanelText.zoneKind(zone.kind), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                    Text(
                        AlarmPanelText.zoneState(zone.state) + if (zone.bypassed) " · excluida" else "", style = MaterialTheme.typography.labelMedium,
                        color = when (zone.state) { "tamper" -> ArmorColors.Alert; "triggered" -> ArmorColors.Amber; else -> ArmorColors.Ok },
                    )
                }
            }
            if (panel.events.isNotEmpty()) {
                Text("Últimos eventos", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                panel.events.takeLast(5).reversed().forEach { event ->
                    val zone = event.zone?.let { id -> panel.zones.firstOrNull { it.id == id }?.title ?: id }
                    Text("${AlarmPanelText.ago(event.agoSeconds)} · ${AlarmPanelText.event(event.kind)}" + (zone?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
            }
            if (!panel.commandsEnabled) Text("El nodo no acepta órdenes del servidor", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            if (allowed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onArm(panel, "away", force) }, enabled = panel.phase == "disarmed") { Text("Armar: fuera") }
                    Button(onClick = { onArm(panel, "stay", force) }, enabled = panel.phase == "disarmed") { Text("Armar: en casa") }
                    OutlinedButton(onClick = onDisarm, enabled = panel.phase != "disarmed") { Text("Desarmar") }
                }
                if (panel.openZones.isNotEmpty() && panel.phase == "disarmed") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(checked = force, onCheckedChange = { force = it })
                        Text("Excluir las zonas abiertas y armar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            recent.forEach { item ->
                Text("Orden ${item.action}${item.mode?.let { " ${AlarmPanelText.mode(it)}" } ?: ""}: ${AlarmPanelText.refusal(item.refusal)}", style = MaterialTheme.typography.labelSmall, color = if (item.accepted) ArmorColors.Ok else ArmorColors.Amber)
            }
        }
    }
}
