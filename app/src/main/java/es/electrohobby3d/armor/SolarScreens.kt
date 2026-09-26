// ARMOR-ANDROID-CONTROL - the Solar screen: the sums, then every inverter and battery with its numbers; cells of a battery on demand.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.SolarBatteryReading
import es.electrohobby3d.armor.model.SolarDevice
import es.electrohobby3d.armor.model.SolarInverterReading
import es.electrohobby3d.armor.model.SolarOverview
import es.electrohobby3d.armor.model.SolarText
import es.electrohobby3d.armor.model.SolarTotals

private fun socColor(percent: Int?) = when { percent == null -> ArmorColors.Muted; percent < 20 -> ArmorColors.Alert; percent < 40 -> ArmorColors.Amber; else -> ArmorColors.Ok }

/** The summary line the Status screen shows under the Solar tile: "2.40 kW de sol · 1.18 kW de consumo". */
fun solarSummary(totals: SolarTotals): String =
    if (totals.inverters == 0) "Baterías: ${SolarText.batteryFlow(totals.batteryW)}"
    else "${SolarText.watts(totals.pvW)} de sol · ${SolarText.watts(totals.loadW)} de consumo"

@Composable
fun SolarScreen(overview: SolarOverview?) {
    val data = overview ?: SolarOverview()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                when {
                    overview == null -> "Buscando datos…"
                    data.devices.isEmpty() -> "Sin equipos leyendo"
                    data.totals.stale > 0 -> "${data.totals.stale} sin señal de ${data.devices.size}"
                    else -> "${data.devices.size} equipos en línea"
                },
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
        }
        if (data.isEmpty) item { EmptySolar() }
        if (data.devices.isNotEmpty()) item { TotalsPanel(data.totals) }
        items(data.devices, key = { "${it.nodeId}/${it.device}" }) { device ->
            if (device.inverter != null) InverterCard(device, device.inverter) else if (device.battery != null) BatteryCard(device, device.battery)
        }
        if (data.waiting.isNotEmpty()) {
            item { Text("Esperando datos", style = MaterialTheme.typography.titleMedium, color = ArmorColors.Muted) }
            items(data.waiting, key = { "w-${it.nodeId}/${it.device}" }) { waiting ->
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(if (waiting.kind == "inverter") Icons.Filled.Bolt else Icons.Filled.BatteryStd, ArmorColors.Muted, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(waiting.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${SolarText.model(waiting.model).ifBlank { if (waiting.kind == "inverter") "Inversor" else "Batería" }} · nodo ${waiting.nodeId}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                        }
                        Text("sin lecturas", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySolar() {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Filled.WbSunny, ArmorColors.Muted, size = 64.dp)
            Text("Aún no hay equipos solares", style = MaterialTheme.typography.titleMedium)
            Text("Los inversores y las baterías se añaden desde Studio, y un nodo solar los lee y envía sus datos aquí.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TotalsPanel(totals: SolarTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (totals.inverters > 0) {
                Tile(Icons.Filled.WbSunny, SolarText.watts(totals.pvW), "Sol", ArmorColors.Amber, Modifier.weight(1f))
                Tile(Icons.Filled.Home, SolarText.watts(totals.loadW), "Consumo", ArmorColors.Cyan, Modifier.weight(1f))
            }
        }
        val soc = totals.socPercent
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(if ((totals.batteryW ?: 0.0) > 20) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd, socColor(soc), size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Baterías", fontWeight = FontWeight.SemiBold)
                        Text(SolarText.batteryFlow(totals.batteryW), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    }
                    Text(SolarText.percent(soc), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = socColor(soc))
                }
                if (soc != null) LinearProgressIndicator(progress = { soc.coerceIn(0, 100) / 100f }, Modifier.fillMaxWidth().height(8.dp), color = socColor(soc), trackColor = ArmorColors.Outline)
                if (totals.capacityAh != null || totals.energyKwh != null) {
                    Text(
                        listOfNotNull(
                            totals.capacityAh?.let { "${SolarText.ah(it)}${totals.fullCapacityAh?.let { full -> " de ${SolarText.ah(full)}" } ?: ""}" },
                            totals.energyKwh?.let { String.format(java.util.Locale.US, "%.2f kWh guardados", it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
                    )
                }
            }
        }
        if (totals.inverters > 0) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (totals.gridPresent) Icons.Filled.Power else Icons.Filled.PowerOff, null, tint = if (totals.gridPresent) ArmorColors.Ok else ArmorColors.Amber, modifier = Modifier.size(18.dp))
            Text(if (totals.gridPresent) "Hay red eléctrica" else "Sin red eléctrica", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            if (totals.mode != null) Text("· ${SolarText.mode(totals.mode)}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, tint, size = 40.dp)
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

/** The small marks a card can carry: no signal, or an example reading made by the server. */
@Composable
private fun Marks(device: SolarDevice) {
    if (device.example) Text("EJEMPLO", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
    if (device.stale) Text("SIN SEÑAL", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Alert)
}

@Composable
private fun Line(label: String, value: String, color: Color = ArmorColors.Text) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color, textAlign = TextAlign.End)
    }
}

@Composable
private fun InverterCard(device: SolarDevice, r: SolarInverterReading) {
    val fault = r.mode == "fault" || r.warnings.isNotEmpty()
    val tint = if (device.stale) ArmorColors.Muted else if (r.mode == "fault") ArmorColors.Alert else if (r.warnings.isNotEmpty() || r.mode == "battery") ArmorColors.Amber else null
    Panel(Modifier.fillMaxWidth(), tint = tint) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.Bolt, if (device.stale) ArmorColors.Muted else tint ?: ArmorColors.Cyan, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(device.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${SolarText.mode(r.mode)}${SolarText.model(device.model).let { if (it.isBlank()) "" else " · $it" }}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                Column(horizontalAlignment = Alignment.End) { Marks(device) }
            }
            Line("Red", "${SolarText.volts(r.gridV)} · ${SolarText.hertz(r.gridHz)}")
            Line("Consumo", "${SolarText.watts(r.outW)} · ${SolarText.percent(r.loadPercent)}")
            Line("Paneles", "${SolarText.watts(r.pvW)} · ${SolarText.volts(r.pvV, 0)}")
            if (r.pv2W != null) Line("Segunda entrada", "${SolarText.watts(r.pv2W)} · ${SolarText.volts(r.pv2V, 0)}")
            Line("Batería", "${SolarText.percent(r.batteryPercent)} · ${SolarText.volts(r.batteryV, 2)} · ${SolarText.amps(r.batteryA)}", socColor(r.batteryPercent?.toInt()))
            Line("Temperatura", SolarText.celsius(r.heatsinkC))
            if (r.units.isNotEmpty()) {
                Line("Sistema en paralelo", "${r.units.size} unidades${r.totalOutW?.let { " · ${SolarText.watts(it)}" } ?: ""}")
                r.units.forEach { unit ->
                    Line("Unidad ${unit.unit + 1}", "${SolarText.mode(unit.mode)}${if (unit.faultCode.isNotEmpty() && unit.faultCode != "00") " · fallo ${unit.faultCode}" else ""} · ${SolarText.watts(unit.outW)}", if (unit.mode == "fault") ArmorColors.Alert else ArmorColors.Text)
                }
            }
            if (fault && r.warnings.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = ArmorColors.Amber, modifier = Modifier.size(16.dp))
                Text(r.warnings.joinToString(", ") { SolarText.warning(it) }, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
            }
        }
    }
}

@Composable
private fun BatteryCard(device: SolarDevice, r: SolarBatteryReading) {
    var showCells by rememberSaveable(device.nodeId, device.device) { mutableStateOf(false) }
    val tint = if (device.stale) ArmorColors.Muted else if (r.alarm) ArmorColors.Alert else null
    val hasCells = r.stack.any { it.cellsV.isNotEmpty() }
    Panel(Modifier.fillMaxWidth(), tint = tint) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(if ((r.currentA ?: 0.0) > 0.5) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd, if (device.stale) ArmorColors.Muted else socColor(r.socPercent), size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(device.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(SolarText.batteryState(r.state), (SolarText.model(device.model).ifBlank { r.model })).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(SolarText.percent(r.socPercent), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = socColor(r.socPercent))
                    Marks(device)
                }
            }
            val soc = r.socPercent
            if (soc != null) LinearProgressIndicator(progress = { soc.coerceIn(0, 100) / 100f }, Modifier.fillMaxWidth().height(6.dp), color = socColor(soc), trackColor = ArmorColors.Outline)
            if (r.modules == 0) Text("La batería no responde a los módulos.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Amber) else {
                Line("Tensión y corriente", "${SolarText.volts(r.voltageV, 2)} · ${SolarText.amps(r.currentA, 2)}")
                Line("Potencia", SolarText.batteryFlow(r.powerW))
                if (r.temperatureMinC != null) Line("Temperatura", if (r.temperatureMinC == r.temperatureMaxC || r.temperatureMaxC == null) SolarText.celsius(r.temperatureMinC) else "${SolarText.celsius(r.temperatureMinC)} – ${SolarText.celsius(r.temperatureMaxC)}")
                if (r.cellMinV != null && r.cellMaxV != null) {
                    val spread = r.stack.firstOrNull { it.cellsV.size > 1 }?.let { SolarText.cellSpreadMv(it.cellsV) }
                    Line("Celdas", "${String.format(java.util.Locale.US, "%.3f", r.cellMinV)} – ${String.format(java.util.Locale.US, "%.3f", r.cellMaxV)} V${if (spread != null) " ($spread mV)" else ""}", if ((spread ?: 0) > 100) ArmorColors.Amber else ArmorColors.Text)
                }
                if (r.capacityAh != null) Line("Capacidad", "${SolarText.ah(r.capacityAh)}${r.fullCapacityAh?.let { " de ${SolarText.ah(it)}" } ?: ""}")
                if (r.cycles != null) Line("Ciclos", r.cycles.toString())
                if (r.healthPercent != null) Line("Salud (capacidad frente a nueva)", "${r.healthPercent} %")
                if (r.modules > 1) Line("Módulos", r.modules.toString())
            }
            if (r.alarm) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = ArmorColors.Alert, modifier = Modifier.size(16.dp))
                Text("La batería avisa de un problema (una protección está actuando).", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Alert)
            }
            if (hasCells) {
                TextButton(onClick = { showCells = !showCells }) {
                    Icon(if (showCells) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (showCells) "Ocultar celdas" else "Ver celdas")
                }
                if (showCells) r.stack.filter { it.cellsV.isNotEmpty() }.forEach { module ->
                    if (r.stack.size > 1) Text("Módulo ${module.number}", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                    val low = module.cellsV.min(); val high = module.cellsV.max()
                    module.cellsV.withIndex().toList().chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (index, volts) ->
                                Text(
                                    "${index + 1}: ${SolarText.cell(volts)}", modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, maxLines = 1,
                                    color = if (module.cellsV.size > 1 && volts == low) ArmorColors.Amber else if (module.cellsV.size > 1 && volts == high) ArmorColors.Cyan else ArmorColors.Muted,
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (module.temperaturesC.isNotEmpty()) Text("Sensores: " + module.temperaturesC.joinToString(" · ") { SolarText.celsius(it) }, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                }
            }
        }
    }
}
