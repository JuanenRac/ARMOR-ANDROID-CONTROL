// ARMOR-ANDROID-CONTROL - the health of the nodes: the same report from every radar, solar, electrical, alarm and wall-panel node (uptime, why it restarted, memory, link, how often it lost the
// broker, the last error) and the warnings the server raised about each. Reading only. Mirrors the health section of ARMOR-STUDIO's System menu.
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
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.NodeHealthOverview
import es.electrohobby3d.armor.model.NodeHealthText
import es.electrohobby3d.armor.model.NodeHealthView

private fun statusColour(view: NodeHealthView): Color = when (view.status) { "ok" -> ArmorColors.Ok; "warning" -> ArmorColors.Amber; else -> ArmorColors.Muted }

/** The nodes that need a look first, then the silent ones, then the healthy, each in order of name. */
internal fun orderedByNeed(nodes: List<NodeHealthView>): List<NodeHealthView> =
    nodes.sortedWith(compareBy<NodeHealthView> { when (it.status) { "warning" -> 0; "stale" -> 1; else -> 2 } }.thenBy { it.nodeId })

@Composable
fun NodeHealthScreen(overview: NodeHealthOverview?) {
    val data = overview ?: NodeHealthOverview()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                if (overview == null) tr("Buscando datos…") else NodeHealthText.summary(data),
                style = MaterialTheme.typography.bodySmall, color = if (data.totals.withWarnings > 0) ArmorColors.Amber else ArmorColors.Muted,
            )
        }
        if (overview != null && data.isEmpty) item { EmptyNodeHealth() }
        items(orderedByNeed(data.nodes), key = { it.nodeId }) { node -> NodeHealthCard(node) }
        if (data.waiting.isNotEmpty()) item {
            Text(
                tr("Nodos conocidos que aún no han enviado informe (firmware antiguo, o el broker no les deja escribirlo):") + " " + data.waiting.joinToString(", ") { "${it.nodeId} (${NodeHealthText.family(it.family)})" },
                style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted,
            )
        }
    }
}

@Composable
private fun EmptyNodeHealth() {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Filled.MonitorHeart, ArmorColors.Muted, size = 64.dp)
            Text(tr("Ningún nodo ha enviado aún un informe de salud"), style = MaterialTheme.typography.titleMedium)
            Text(tr("Un nodo con firmware reciente envía uno cada minuto, y el broker debe dejarle escribirlo."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun Fact(label: String, value: String, colour: Color = ArmorColors.Muted) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = colour, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun NodeHealthCard(node: NodeHealthView) {
    val tint = statusColour(node)
    Panel(Modifier.fillMaxWidth(), tint = if (node.needsLook) ArmorColors.Amber else if (node.silent) ArmorColors.Muted else null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.MonitorHeart, tint, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(node.nodeId, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(NodeHealthText.family(node.family) + " · " + node.firmware + (node.board?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                Text(NodeHealthText.status(node.status), style = MaterialTheme.typography.labelSmall, color = tint)
            }
            node.warnings.forEach { Text("• " + NodeHealthText.warning(it.code), style = MaterialTheme.typography.labelMedium, color = ArmorColors.Amber) }
            Fact(tr("Encendido desde hace"), NodeHealthText.uptime(node.uptimeSeconds))
            Fact(tr("Último arranque"), NodeHealthText.resetReason(node.resetReason) + if (NodeHealthText.isUnexpected(node.resetReason)) " · " + tr("inesperado") else "", if (NodeHealthText.isUnexpected(node.resetReason)) ArmorColors.Alert else ArmorColors.Muted)
            Fact(tr("Memoria libre"), NodeHealthText.bytes(node.heapFree) + " · " + trf("mínimo %s", NodeHealthText.bytes(node.heapMin)), if (node.warnings.any { it.code == "node_low_memory" }) ArmorColors.Amber else ArmorColors.Muted)
            Fact(tr("Enlace"), NodeHealthText.link(node))
            Fact(tr("Conexiones al broker"), node.brokerConnections.toString())
            Fact(tr("Reinicios en 24 h"), node.restarts24h.toString(), if (node.restarts24h >= 3) ArmorColors.Amber else ArmorColors.Muted)
            if (node.lowStackTasks > 0) Fact(tr("Tareas cortas de pila"), node.lowStackTasks.toString(), ArmorColors.Amber)
            Fact(tr("Último error"), node.lastError ?: tr("ninguno anotado"))
        }
    }
}
