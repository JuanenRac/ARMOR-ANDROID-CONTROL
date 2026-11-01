// ARMOR-ANDROID-CONTROL - the reports of the server, for administrators and operators: the summary of a day and the activity heat map of a camera.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** The two reports. Each is off in the server until an administrator turns it on, and a refusal is told in words that say what to do; nothing is invented when a report fails. */
@Composable
fun ReportsScreen(state: ArmorViewModel.ReportsUiState, cameras: List<Pair<String, String>>, onSummary: (String) -> Unit, onHeatmap: (String?) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Para administradores y operadores. Cada informe está apagado hasta que un administrador lo enciende en Seguridad, en Studio; es para que lo lea una persona y no cambia nada."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        SummaryCard(state, onSummary)
        HeatmapCard(state, cameras, onHeatmap)
    }
}

@Composable
private fun SummaryCard(state: ArmorViewModel.ReportsUiState, onSummary: (String) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    Panel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Resumen de un día"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(date, { date = it.trim().take(10) }, label = { Text(tr("Día (AAAA-MM-DD)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onSummary(date) }, enabled = !state.summaryBusy) { Text(if (state.summaryBusy) tr("Cargando…") else tr("Mostrar")) }
            state.summaryRefusal?.let { Text(tr(it), color = ArmorColors.Alert, style = MaterialTheme.typography.bodyMedium) }
            state.summary?.let { summary ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Total(tr("Eventos"), summary.events); Total(tr("Alarmas"), summary.alarms); Total(tr("Alarmas graves"), summary.serious); Total(tr("Falsas alarmas"), summary.falseAlarms)
                }
                Text(summary.text, style = MaterialTheme.typography.bodyMedium)
                if (summary.findings.isNotEmpty()) {
                    Text(tr("Patrones encontrados"), fontWeight = FontWeight.SemiBold)
                    summary.findings.forEach { Text("• ${findingName(it.kind)} - ${it.subject} (${trf("%s veces", it.count)})", style = MaterialTheme.typography.bodySmall) }
                }
                summary.aiNote?.let { Text("${tr("Nota de la IA:")} $it", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted) }
                Text(tr("Hecho con las alarmas que guardó el servidor, los enlaces perdidos, las falsas alarmas y, si el registro diario está activo, las lecturas y el consumo de la casa. Ningún nombre, texto ni dirección de la casa llega a la IA."), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            }
        }
    }
}

@Composable
private fun RowScope.Total(label: String, value: Int) {
    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeatmapCard(state: ArmorViewModel.ReportsUiState, cameras: List<Pair<String, String>>, onHeatmap: (String?) -> Unit) {
    var camera by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Mapa de calor de actividad"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(tr("Dónde se vio actividad en la escena de una cámara: solo posiciones contadas, nunca una imagen de nadie. Viene del servicio de visión en vivo."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Box {
                OutlinedButton(onClick = { open = true }) { Text(cameras.firstOrNull { it.first == camera }?.second ?: tr("La única / la primera")) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text(tr("La única / la primera")) }, onClick = { camera = ""; open = false })
                    cameras.forEach { (id, name) -> DropdownMenuItem(text = { Text(name.ifBlank { id }) }, onClick = { camera = id; open = false }) }
                }
            }
            Button(onClick = { onHeatmap(camera.ifBlank { null }) }, enabled = !state.heatmapBusy) { Text(if (state.heatmapBusy) tr("Cargando…") else tr("Mostrar")) }
            state.heatmapRefusal?.let { Text(tr(it), color = ArmorColors.Alert, style = MaterialTheme.typography.bodyMedium) }
            state.heatmap?.let { map ->
                val bitmap = remember(map) { map.png?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() } }
                if (bitmap != null) Image(bitmap, contentDescription = trf("Mapa de calor de la cámara %s", map.cameraId), contentScale = ContentScale.FillWidth, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxWidth())
                Text(trf("%s: %s posiciones contadas en una rejilla de %s x %s.", map.cameraId, map.positions, map.cols, map.rows), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            }
        }
    }
}

private fun findingName(kind: String): String = when (kind) {
    "hourly_repeat" -> tr("La misma alarma a la misma hora cada hora")
    "intermittent_sensor" -> tr("Un sensor que falla a ratos")
    "intermittent_link" -> tr("Un nodo que pierde el enlace una y otra vez")
    "recurrent_false_alarm" -> tr("Una zona con falsas alarmas repetidas")
    "anomalous_consumption" -> tr("Consumo muy por encima de lo habitual")
    else -> kind
}
