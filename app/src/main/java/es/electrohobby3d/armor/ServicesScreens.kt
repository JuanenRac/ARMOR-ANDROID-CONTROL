// ARMOR-ANDROID-CONTROL - every service of the system, running or not: the programs of the machine and the field nodes. Reading only.
// Mirrors ARMOR-STUDIO's Services menu.
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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.ServiceInfo
import es.electrohobby3d.armor.model.ServicesOverview
import es.electrohobby3d.armor.model.ServicesText

private fun stateColour(state: String): Color = when (state) {
    "running", "online" -> ArmorColors.Ok
    "starting" -> ArmorColors.Amber
    "failed", "offline" -> ArmorColors.Alert
    "not_installed" -> ArmorColors.Muted
    else -> ArmorColors.Muted
}

@Composable
fun ServicesScreen(overview: ServicesOverview?, nowMs: Long) {
    val data = overview ?: ServicesOverview()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                if (overview == null) "Buscando servicios…" else ServicesText.summary(data),
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
        }
        if (overview != null && !data.systemd) item { Text("Esta máquina no usa systemd: los programas aparecen como desconocidos; los nodos de campo sí se leen.", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
        ServicesText.familyOrder.forEach { family ->
            val shown = data.services.filter { it.family == family }
            if (shown.isNotEmpty()) {
                item { Text(ServicesText.family(family), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(shown, key = { it.id }) { ServiceRow(it, nowMs) }
            }
        }
        val other = data.services.filter { it.family !in ServicesText.familyOrder }
        if (other.isNotEmpty()) {
            item { Text(other.first().family, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(other, key = { it.id }) { ServiceRow(it, nowMs) }
        }
        if (overview != null && data.services.isEmpty()) item { Text("No se ha leído ningún servicio.", style = MaterialTheme.typography.bodyMedium, color = ArmorColors.Muted) }
    }
}

@Composable
private fun ServiceRow(service: ServiceInfo, nowMs: Long) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Circle, contentDescription = null, tint = stateColour(service.state), modifier = Modifier.size(10.dp))
                Text(service.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(ServicesText.state(service.state), style = MaterialTheme.typography.labelMedium, color = stateColour(service.state))
            }
            Text(service.description, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Text(
                buildString {
                    append(if (service.kind == "field-node") "Última vez: ${ServicesText.since(service.sinceMs, nowMs)}" else "En marcha ${ServicesText.since(service.sinceMs, nowMs)}")
                    if (service.pid != null) append(" · PID ${service.pid}")
                    if (service.port != null) append(" · puerto ${service.port}")
                    if (service.memoryBytes != null) append(" · ${ServicesText.memory(service.memoryBytes)}")
                    if ((service.restarts ?: 0) > 0) append(" · ${service.restarts} reinicios")
                    if (service.enabled == true) append(" · arranca solo")
                },
                style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted,
            )
        }
    }
}
