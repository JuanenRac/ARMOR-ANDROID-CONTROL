// ARMOR-ANDROID-CONTROL - the local network: whether the internet is there, the devices on it and what changed. Reading only.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.NetworkDevice
import es.electrohobby3d.armor.model.NetworkEvent
import es.electrohobby3d.armor.model.NetworkNode
import es.electrohobby3d.armor.model.NetworkOverview
import es.electrohobby3d.armor.model.NetworkText
import java.text.DateFormat
import java.util.Date

private fun stateColour(state: String): Color = when (state) { "up" -> ArmorColors.Ok; "degraded" -> ArmorColors.Amber; "unknown" -> ArmorColors.Muted; else -> ArmorColors.Alert }
private fun when_(atMs: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(atMs))

@Composable
fun NetworkScreen(overview: NetworkOverview?) {
    val data = overview ?: NetworkOverview()
    var showOnlyNew by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                when {
                    overview == null -> tr("Buscando datos…")
                    data.nodes.isEmpty() -> tr("Sin nodos de red leyendo")
                    data.totals.stale > 0 -> trf("%s sin señal de %s", data.totals.stale, data.nodes.size)
                    else -> trf("%s nodos en línea", data.nodes.size)
                },
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
        }
        if (data.isEmpty) item { EmptyNetwork() }
        data.nodes.firstOrNull()?.let { node ->
            item { InternetPanel(node, data) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Dispositivos"), style = MaterialTheme.typography.titleMedium, color = ArmorColors.Muted, modifier = Modifier.weight(1f))
                    Text(if (showOnlyNew) tr("Solo sin conocer") else tr("Todos"), style = MaterialTheme.typography.labelMedium, color = ArmorColors.Cyan, modifier = Modifier.clickable { showOnlyNew = !showOnlyNew }.padding(4.dp))
                }
            }
            val shown = node.devices.sortedBy { ipKey(it.ip) }.filter { !showOnlyNew || !it.known }
            items(shown, key = { it.id }) { device -> DeviceRow(device, node.ip) }
            if (data.outages.isNotEmpty()) item { OutagesPanel(data) }
            if (data.events.isNotEmpty()) item { EventsPanel(data, node.devices) }
        }
    }
}

private fun ipKey(ip: String): Long = ip.split('.').fold(0L) { sum, part -> sum * 256 + (part.toLongOrNull() ?: 0L) }

@Composable
private fun EmptyNetwork() {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Filled.Router, ArmorColors.Muted, size = 64.dp)
            Text(tr("Aún no hay nodos de red"), style = MaterialTheme.typography.titleMedium)
            Text(tr("Cuando un nodo ARMOR-NETWORK vigile la red, aparecerán aquí: si hay internet, los dispositivos y lo que cambia."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun InternetPanel(node: NetworkNode, data: NetworkOverview) {
    val internet = node.internet
    val colour = stateColour(internet.state)
    Panel(Modifier.fillMaxWidth(), tint = if (internet.state == "up") null else colour) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.Router, colour, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(NetworkText.state(internet.state), fontWeight = FontWeight.SemiBold, color = colour)
                    NetworkText.hint(internet.state)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted) }
                    internet.sinceMs?.let { Text(trf("Desde %s", when_(it)), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
                }
                internet.latencyMs?.let { Text("${Math.round(it)} ms", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
            Text(
                trf("Cortes en 24 h: %s · sin internet %s", internet.outages24h, NetworkText.duration(internet.downtime24hS.toLong())) + (internet.lossPercent?.let { trf(" · pérdida %s %%", Math.round(it)) } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
            Text(trf("%s · red %s · router %s · ↓ %s ↑ %s", node.ip, node.cidr, node.gateway ?: "—", NetworkText.bps(node.rxBps), NetworkText.bps(node.txBps)), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            node.publicInfo?.let { info ->
                Text(trf("IP pública: %s", info.ip), fontWeight = FontWeight.SemiBold)
                val country = info.country?.let { code -> java.util.Locale("", code).getDisplayCountry(java.util.Locale.forLanguageTag(Tr.language)).ifBlank { code } }
                val place = listOfNotNull(info.city, info.region, country).joinToString(", ")
                val line = listOfNotNull(info.org, place.ifBlank { null }).joinToString(" · ")
                if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            }
            val unknown = data.totals.unknown
            if (unknown > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = ArmorColors.Amber, modifier = Modifier.size(16.dp))
                Text(if (unknown == 1) tr("Un dispositivo sin marcar como conocido") else trf("%s dispositivos sin marcar como conocidos", unknown), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            }
        }
    }
}

@Composable
private fun DeviceRow(device: NetworkDevice, ownIp: String) {
    val risky = device.ports.filter { it.port in NetworkText.riskyPorts }
    Panel(Modifier.fillMaxWidth(), tint = if (!device.online) ArmorColors.Muted else if (risky.isNotEmpty()) ArmorColors.Alert else null) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(device.name + if (device.ip == ownIp) tr(" (este nodo)") else "", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (device.online) tr("en línea") else tr("sin conexión"), style = MaterialTheme.typography.labelSmall, color = if (device.online) ArmorColors.Ok else ArmorColors.Muted)
            }
            Text("${device.ip} · ${NetworkText.kind(device.shownKind)}${device.vendor?.let { " · ${it}" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (device.ports.isNotEmpty()) Text(tr("Puertos: ") + device.ports.joinToString(", ") { it.port.toString() }, style = MaterialTheme.typography.labelSmall, color = if (risky.isNotEmpty()) ArmorColors.Alert else ArmorColors.Muted)
            if (risky.isNotEmpty()) Text(trf("Puerto que una casa rara vez quiere abierto: %s", risky.joinToString(", ") { "${it.port}${it.service?.let { s -> " (${s})" } ?: ""}" }), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Alert)
            if (!device.known) Text(tr("Sin marcar como conocido"), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
        }
    }
}

@Composable
private fun OutagesPanel(data: NetworkOverview) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr("Cortes"), fontWeight = FontWeight.SemiBold)
            data.outages.take(8).forEach { outage ->
                Text(
                    "${when_(outage.startedMs)} → ${when_(outage.endedMs)} · ${NetworkText.duration(outage.durationS.toLong())} · ${if (outage.kind == "gateway") "el router" else "internet"}",
                    style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun EventsPanel(data: NetworkOverview, devices: List<NetworkDevice>) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Qué ha cambiado"), fontWeight = FontWeight.SemiBold)
            data.events.take(20).forEach { event: NetworkEvent ->
                Column {
                    Text(NetworkText.event(event, devices), style = MaterialTheme.typography.bodySmall)
                    Text(when_(event.atMs), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                }
            }
        }
    }
}
