// ARMOR-ANDROID-CONTROL - the electrical network: the grid input, the circuits and the DC buses the electrical nodes measure. Reading only.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.ElectricalChannel
import es.electrohobby3d.armor.model.ElectricalSwitch
import es.electrohobby3d.armor.model.ElectricalNode
import es.electrohobby3d.armor.model.ElectricalOverview
import es.electrohobby3d.armor.model.ElectricalText
import es.electrohobby3d.armor.model.ElectricalTotals
import es.electrohobby3d.armor.model.NetworkOverview

/** The line the Status screen shows under the Electrical tile: "Consume 2.87 kW de la red". */
fun electricalSummary(totals: ElectricalTotals): String = ElectricalText.gridFlow(totals.gridW)

@Composable
fun ElectricalScreen(overview: ElectricalOverview?, network: NetworkOverview? = null, onScanNetwork: () -> Unit = {}, scanning: Boolean = false) {
    val data = overview ?: ElectricalOverview()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { NodeFinderPanel(network, data.nodes.map { it.nodeId }, onScanNetwork, scanning) }
        item {
            Text(
                when {
                    overview == null -> "Buscando datos…"
                    data.nodes.isEmpty() -> "Sin nodos eléctricos leyendo"
                    data.totals.stale > 0 -> "${data.totals.stale} sin señal de ${data.nodes.size}"
                    else -> "${data.nodes.size} nodos en línea"
                },
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
        }
        if (data.isEmpty) item { EmptyElectrical() }
        if (data.nodes.isNotEmpty()) item { ElectricalTotalsPanel(data.totals) }
        items(data.nodes, key = { it.nodeId }) { node -> ElectricalNodeCard(node) }
    }
}

@Composable
private fun EmptyElectrical() {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Filled.ElectricBolt, ArmorColors.Muted, size = 64.dp)
            Text("Aún no hay nodos eléctricos", style = MaterialTheme.typography.titleMedium)
            Text("Cuando un nodo con contadores publique sus lecturas, aparecerán aquí: la red, los circuitos y los buses de continua.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun ElectricalTotalsPanel(totals: ElectricalTotals) {
    val tint = if (totals.alarms > 0) ArmorColors.Alert else null
    Panel(Modifier.fillMaxWidth(), tint = tint) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.ElectricBolt, tint ?: ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Red eléctrica", fontWeight = FontWeight.SemiBold)
                    Text(electricalSummary(totals), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                totals.gridKwh?.let { Text(ElectricalText.kwh(it), style = MaterialTheme.typography.labelMedium, color = ArmorColors.Muted) }
            }
            if (totals.alarms > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = ArmorColors.Amber, modifier = Modifier.size(16.dp))
                Text(if (totals.alarms == 1) "Un contador avisa de una alarma" else "${totals.alarms} contadores avisan de una alarma", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            }
        }
    }
}

@Composable
private fun ElectricalNodeCard(node: ElectricalNode) {
    Panel(Modifier.fillMaxWidth(), tint = if (node.stale) ArmorColors.Muted else null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(node.nodeId, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (node.stale) "sin señal" else "en línea", style = MaterialTheme.typography.labelSmall, color = if (node.stale) ArmorColors.Amber else ArmorColors.Ok)
            }
            if (node.switchingEnabled == false) Text("Maniobra desactivada en el nodo", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            if (node.channels.isEmpty()) Text("No hay ningún contador leyendo", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            node.channels.forEach { channel -> ChannelRow(channel, node.stale) }
            node.switches.forEach { switchItem -> SwitchRow(switchItem) }
        }
    }
}

@Composable
private fun SwitchRow(switchItem: ElectricalSwitch) {
    val fault = switchItem.fault != "none"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(switchItem.name, fontWeight = FontWeight.Medium, color = if (fault) ArmorColors.Alert else ArmorColors.Text)
        Text(
            "${ElectricalText.switchSource(switchItem)} · A: ${if (switchItem.aClosed) "cerrado" else "abierto"} · B: ${if (switchItem.bClosed) "cerrado" else "abierto"}" + if (switchItem.closing) " · maniobrando" else "",
            style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
        )
        ElectricalText.switchFault(switchItem.fault)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Alert) }
    }
}

@Composable
private fun ChannelRow(channel: ElectricalChannel, stale: Boolean) {
    val tint = if (stale) ArmorColors.Muted else if (channel.alarm) ArmorColors.Alert else null
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(channel.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = tint ?: ArmorColors.Text)
            Text(if (channel.domain == "dc") "CC" else "CA", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            Text(ElectricalText.watts(channel.powerW), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tint ?: ArmorColors.Text)
        }
        val details = buildList {
            add(ElectricalText.volts(channel.voltageV)); add(ElectricalText.amps(channel.currentA))
            channel.frequencyHz?.let { add("${String.format(java.util.Locale.US, "%.1f", it)} Hz") }
            channel.powerFactor?.let { add("cos φ ${String.format(java.util.Locale.US, "%.2f", it)}") }
            channel.energyKwh?.let { add(ElectricalText.kwh(it)) }
            ElectricalText.switchState(channel.state)?.let { add(it) }
        }
        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        if (channel.alarm) Text("El contador avisa de una alarma", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
    }
}
