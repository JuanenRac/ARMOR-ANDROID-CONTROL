// ARMOR-ANDROID-CONTROL - the charts of the Solar screen: the history the server keeps of the bank, each stack, each battery, its cells and each inverter, in sections that fold away
// and stay as the person left them.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.CellLevel
import es.electrohobby3d.armor.model.HistoryRequest
import es.electrohobby3d.armor.model.HistorySample
import es.electrohobby3d.armor.model.SolarCells
import es.electrohobby3d.armor.model.SolarDevice
import es.electrohobby3d.armor.model.SolarHistory
import es.electrohobby3d.armor.model.SolarHistoryMath
import es.electrohobby3d.armor.model.SolarViewSettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Asks the server for a history; null when it could not be read. */
typealias HistoryLoader = suspend (HistoryRequest) -> SolarHistory?

private const val REFRESH_MS = 30_000L
private const val PREFERENCES = "armor-solar-view"

/** A choice of the Solar screen that is remembered on the phone (a section open or folded, a range, a scope). */
internal class Remembered(context: Context) {
    private val store = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    fun flag(key: String, default: Boolean): Boolean = store.getBoolean(key, default)
    fun put(key: String, value: Boolean) { store.edit().putBoolean(key, value).apply() }
    fun number(key: String, default: Int): Int = store.getInt(key, default)
    fun put(key: String, value: Int) { store.edit().putInt(key, value).apply() }
}

@Composable
internal fun rememberFlag(key: String, default: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val state = remember(key) { mutableStateOf(Remembered(context).flag(key, default)) }
    return remember(key) {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = state.value
                set(next) { state.value = next; Remembered(context).put(key, next) }
            override fun component1(): Boolean = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}

@Composable
private fun rememberNumber(key: String, default: Int): Pair<Int, (Int) -> Unit> {
    val context = LocalContext.current
    var value by remember(key) { mutableStateOf(Remembered(context).number(key, default)) }
    return value to { next: Int -> value = next; Remembered(context).put(key, next) }
}

/** A section with a header that folds it away; what is folded is not asked of the server, and the phone remembers it. */
@Composable
internal fun Foldable(key: String, title: String, default: Boolean, tag: String = key, content: @Composable ColumnScope.() -> Unit) {
    var open by rememberFlag("open/$key", default)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = if (open) tr("Ocultar") else tr("Mostrar")) { open = !open }.testTag("fold-$tag").padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = ArmorColors.Cyan)
            Text(title, fontWeight = FontWeight.SemiBold, color = ArmorColors.Text, modifier = Modifier.weight(1f))
        }
        if (open) content()
    }
}

private data class Curve(val key: String, val label: String, val color: Color)

/** A colour for each cell, spread around the wheel so neighbours differ. */
private fun cellColour(index: Int): Color = Color.hsl(((index * 137) % 360).toFloat(), 0.75f, 0.65f)

private fun clock(t: Long, span: Long): String = SimpleDateFormat(if (span > 36 * 3_600_000L) "dd/MM" else "HH:mm", Locale.getDefault()).format(Date(t))

/** An end of the time axis: over a day or more the hour alone would read the same at both ends. */
private fun edge(t: Long, span: Long): String = if (span in 86_400_000L..36 * 3_600_000L) SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(t)) else clock(t, span)

private fun number(value: Double, decimals: Int): String = String.format(Locale.US, "%.${decimals}f", value)

/** The lines of one chart on one scale, the alarm levels dashed; a touch reads the nearest moment. */
@Composable
private fun Plot(samples: List<HistorySample>, lines: List<Curve>, from: Long, to: Long, levels: List<Double>, height: Int, description: String, onRead: ((Long?) -> Unit)?, read: Long?) {
    val values = samples.flatMap { sample -> lines.mapNotNull { sample.values[it.key] } }
    val shown = SolarHistoryMath.levelsInView(values, levels)
    val (low, high) = SolarHistoryMath.scale(values + shown)
    val span = (to - from).coerceAtLeast(1)
    val gap = SolarHistoryMath.gapMs(samples)
    var touch = Modifier.fillMaxWidth().height(height.dp).border(1.dp, ArmorColors.Outline, RoundedCornerShape(8.dp)).semantics { contentDescription = description }
    if (onRead != null) {
        touch = touch
            .pointerInput(from, to) { detectTapGestures { at -> onRead(from + (at.x / size.width * span).toLong()) } }
            .pointerInput(from, to) { detectHorizontalDragGestures { change, _ -> onRead(from + (change.position.x.coerceIn(0f, size.width.toFloat()) / size.width * span).toLong()) } }
    }
    Canvas(touch.padding(4.dp)) {
        fun x(t: Long): Float = (t - from).toFloat() / span * size.width
        fun y(value: Double): Float = (size.height - (value - low) / (high - low) * size.height).toFloat()
        for (part in 1..3) drawLine(ArmorColors.Outline.copy(alpha = 0.6f), Offset(0f, size.height * part / 4), Offset(size.width, size.height * part / 4))
        shown.forEach { level -> drawLine(ArmorColors.Alert, Offset(0f, y(level)), Offset(size.width, y(level)), strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))) }
        lines.forEach { line ->
            val path = Path()
            var last = Long.MIN_VALUE
            samples.forEach { sample ->
                val value = sample.values[line.key] ?: return@forEach
                if (last == Long.MIN_VALUE || sample.t - last > gap) path.moveTo(x(sample.t), y(value)) else path.lineTo(x(sample.t), y(value))
                last = sample.t
            }
            drawPath(path, line.color, style = Stroke(width = 1.8.dp.toPx()))
        }
        if (read != null) drawLine(ArmorColors.Text.copy(alpha = 0.7f), Offset(x(read), 0f), Offset(x(read), size.height))
    }
}

/** One chart with its title, which folds it away, its axis ends, its legend and the figures under the finger (or the latest). */
@Composable
private fun HistoryChart(id: String, title: String, unit: String, decimals: Int, samples: List<HistorySample>, lines: List<Curve>, from: Long, to: Long, levels: List<Double> = emptyList()) {
    val inside = samples.filter { it.t in from..to }
    val drawn = lines.filter { line -> inside.any { line.key in it.values } }
    Foldable("chart/$id", title, true, tag = "chart-$id") {
        if (inside.size < 2 || drawn.isEmpty()) {
            Text(tr("Sin historial todavía"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            return@Foldable
        }
        var read by remember(id) { mutableStateOf<Long?>(null) }
        val at = read?.let { SolarHistoryMath.nearest(inside, it) }
        val values = inside.flatMap { sample -> drawn.mapNotNull { sample.values[it.key] } }
        val (low, high) = SolarHistoryMath.scale(values + SolarHistoryMath.levelsInView(values, levels))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${number(high, decimals)} $unit", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            Text(clock(at?.t ?: inside.last().t, to - from), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Text)
        }
        Plot(inside, drawn, from, to, levels, 140, title, { read = it }, at?.t)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${number(low, decimals)} $unit", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            Text("${edge(from, to - from)} – ${edge(to, to - from)}", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
        }
        if (drawn.size <= 4) drawn.forEach { line ->
            val all = inside.mapNotNull { it.values[line.key] }
            val now = at?.values?.get(line.key) ?: all.last()
            Text("● ${line.label}: ${number(now, decimals)} $unit · ${tr("mín")} ${number(all.min(), decimals)} · ${tr("máx")} ${number(all.max(), decimals)}", style = MaterialTheme.typography.labelSmall, color = line.color)
        } else drawn.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { line ->
                    val now = at?.values?.get(line.key) ?: inside.lastOrNull { line.key in it.values }?.values?.get(line.key)
                    Text("● ${line.label} ${now?.let { number(it, decimals) } ?: "–"}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = line.color, maxLines = 2)
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Chips(options: List<Pair<Int, String>>, chosen: Int, tag: String, choose: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) -> FilterChip(selected = value == chosen, onClick = { choose(value) }, label = { Text(label) }, modifier = Modifier.testTag("$tag-$value")) }
    }
}

private data class Loaded(val request: List<HistoryRequest>, val answers: List<SolarHistory?>, val asked: Long)

/** The answers to some requests, asked again every half minute while the charts are open; an answer about something else is never drawn. */
@Composable
private fun rememberHistory(loader: HistoryLoader, requests: List<HistoryRequest>): Loaded? {
    var loaded by remember { mutableStateOf<Loaded?>(null) }
    LaunchedEffect(requests) {
        while (true) {
            val answers = requests.map { loader(it) }
            loaded = Loaded(requests, answers, System.currentTimeMillis())
            delay(REFRESH_MS)
        }
    }
    return loaded?.takeIf { it.request == requests }
}

@Composable
private fun stillWaiting(loaded: Loaded?): Boolean {
    if (loaded == null) { Text(tr("Cargando…"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted); return true }
    if (loaded.answers.all { it == null }) { Text(tr("No se pudo leer el historial"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Amber); return true }
    return false
}

private const val SCOPE_STACK = 0
private const val SCOPE_BATTERY = 1
private const val SCOPE_CELLS = 2

private fun voltage() = Curve("voltage_v", tr("Tensión"), ArmorColors.Cyan)
private fun current() = Curve("current_a", tr("Corriente"), ArmorColors.Amber)
private fun power() = Curve("power_w", tr("Potencia"), ArmorColors.CyanDeep)
private fun charge() = Curve("soc_percent", tr("Nivel de carga"), ArmorColors.Ok)

/** The figures a stack, the bank or one battery have in common. */
@Composable
private fun BatteryFigures(id: String, samples: List<HistorySample>, temperatureKey: String, from: Long, to: Long, cellLevels: List<Double>, settings: SolarViewSettings) {
    HistoryChart("$id/voltage", tr("Tensión"), "V", 2, samples, listOf(voltage()), from, to)
    HistoryChart("$id/current", tr("Corriente"), "A", 1, samples, listOf(current()), from, to)
    HistoryChart("$id/power", tr("Potencia"), "W", 0, samples, listOf(power()), from, to)
    HistoryChart("$id/soc", tr("Nivel de carga"), "%", 0, samples, listOf(charge()), from, to)
    HistoryChart("$id/temperature", tr("Temperatura"), "°C", 1, samples, listOf(Curve(temperatureKey, tr("Temperatura"), ArmorColors.Alert)), from, to, listOf(settings.hotC, settings.coldChargeC))
    HistoryChart("$id/range", tr("Celda más alta y más baja"), "V", 3, samples, listOf(Curve("cell_max_v", tr("máx"), ArmorColors.Cyan), Curve("cell_min_v", tr("mín"), ArmorColors.Amber)), from, to, cellLevels)
    HistoryChart("$id/delta", tr("Diferencia"), "mV", 0, samples, listOf(Curve("delta_mv", tr("Diferencia"), ArmorColors.Amber)), from, to, listOf(settings.spreadWarnMv.toDouble()))
}

/** The whole bank: every stack added up, moment by moment. */
@Composable
internal fun BankCharts(batteries: List<SolarDevice>, loader: HistoryLoader, settings: SolarViewSettings) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Foldable("bank", tr("Gráficas del banco"), false) {
                val (minutes, setMinutes) = rememberNumber("range/bank", 1440)
                Chips(SolarHistoryMath.RANGES, minutes, "range", setMinutes)
                val loaded = rememberHistory(loader, batteries.map { HistoryRequest(it.nodeId, it.device, minutes) })
                if (!stillWaiting(loaded)) {
                    val to = loaded!!.asked
                    val samples = SolarHistoryMath.mergeBank(loaded.answers.map { it?.samples ?: emptyList() }, SolarHistoryMath.bankBucketMs(minutes))
                    BatteryFigures("bank", samples, "temperature_max_c", to - minutes * 60_000L, to, emptyList(), settings)
                }
            }
        }
    }
}

/** The history of one socket: the stack as a whole, one of its batteries, or the cells of one of its batteries. */
@Composable
internal fun BatteryHistoryCharts(device: SolarDevice, loader: HistoryLoader, settings: SolarViewSettings) {
    val reading = device.battery ?: return
    val key = "${device.nodeId}/${device.device}"
    Foldable("battery/$key", tr("Gráficas"), false, tag = "charts-${device.device}") {
        val (scope, setScope) = rememberNumber("scope/$key", SCOPE_STACK)
        val (minutes, setMinutes) = rememberNumber("range/$key", 1440)
        val (chosen, setChosen) = rememberNumber("module/$key", 0)
        Chips(listOf(SCOPE_STACK to tr("Pack"), SCOPE_BATTERY to tr("Batería"), SCOPE_CELLS to tr("Celdas")), scope, "scope", setScope)
        Chips(SolarHistoryMath.RANGES, minutes, "range", setMinutes)
        val modules = reading.stack.filter { it.present }
        val module = modules.firstOrNull { it.number == chosen } ?: modules.firstOrNull()
        if (scope != SCOPE_STACK && modules.size > 1) Chips(modules.map { it.number to trf("Módulo %s", it.number) }, module?.number ?: 0, "module", setChosen)
        val wanted = if (scope == SCOPE_STACK) null else module?.number
        val loaded = rememberHistory(loader, listOf(HistoryRequest(device.nodeId, device.device, minutes, wanted)))
        Text(tr("Toca una gráfica para leer un momento."), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
        if (stillWaiting(loaded)) return@Foldable
        val answer = loaded!!.answers.first()!!
        val to = loaded.asked
        val from = to - minutes * 60_000L
        if (wanted != null && answer.module == null) {
            Text(tr("Este servidor todavía no guarda el histórico por batería y celda: actualiza ARMOR-SERVER."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Amber)
            return@Foldable
        }
        if (wanted != null) Text(
            if (answer.retentionDays == 0) tr("Este servidor no registra histórico por batería y celda.")
            else trf("El servidor registra cada batería y celda: un punto cada %s segundos durante el último día y medias de cinco minutos durante %s días.", answer.everyS ?: 0, answer.retentionDays ?: 0),
            style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted,
        )
        val lfp = module != null && SolarCells.isLfp(device.model, module.model.ifBlank { reading.model })
        val cellLevels = if (lfp) listOf(settings.cell.faultHighV, settings.cell.faultLowV) else emptyList()
        val samples = if (wanted == null) SolarHistoryMath.derived(answer.samples) else answer.samples
        when (scope) {
            SCOPE_CELLS -> CellCharts("$key/${module?.number}", device, module?.let { SolarCells.module(it, lfp, settings) }, samples, from, to, cellLevels, settings)
            SCOPE_BATTERY -> BatteryFigures("$key/battery", samples, "temperature_c", from, to, cellLevels, settings)
            else -> BatteryFigures("$key/stack", samples, "temperature_max_c", from, to, cellLevels, settings)
        }
    }
}

/** The cells of one battery together, their spread and, if the person asks, one small chart for each cell on a common scale. */
@Composable
private fun CellCharts(id: String, device: SolarDevice, view: es.electrohobby3d.armor.model.ModuleView?, samples: List<HistorySample>, from: Long, to: Long, levels: List<Double>, settings: SolarViewSettings) {
    val cells = view?.cells ?: emptyList()
    var small by rememberFlag("small-cells", false)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = small, onCheckedChange = { small = it }, modifier = Modifier.testTag("small-cells"))
        Text(tr("Una gráfica pequeña por cada celda"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
    if (small) {
        Text(tr("Todas las gráficas pequeñas usan la misma escala; las líneas discontinuas son los niveles de alarma de las celdas."), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
        val inside = samples.filter { it.t in from..to }
        val all = inside.flatMap { sample -> cells.mapNotNull { sample.values["cell${it.number}"] } }
        val drawn = SolarHistoryMath.levelsInView(all, levels)
        val (low, high) = SolarHistoryMath.scale(all + drawn)
        cells.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { cell ->
                    val key = "cell${cell.number}"
                    val faulty = cell.level == CellLevel.FAULT && !device.stale
                    val color = levelColor(cell.level, device.stale)
                    val alpha = blinkAlpha(faulty, settings.blinkPeriodMs)
                    Column(
                        Modifier.weight(1f).border(if (cell.level == CellLevel.OK) 1.dp else 2.dp, if (cell.level == CellLevel.OK) ArmorColors.Outline else color.copy(alpha = alpha), RoundedCornerShape(10.dp))
                            .testTag("small-chart-${cell.number}").padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (cell.level != CellLevel.OK) Icon(levelIcon(cell.level), levelWord(cell.level), tint = color.copy(alpha = alpha), modifier = Modifier.size(12.dp))
                            Text(trf("Celda %s", cell.number) + " · " + number(cell.volts, 3) + " V", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Text)
                        }
                        val own = inside.mapNotNull { it.values[key] }
                        if (own.size < 2) Text(tr("Sin historial todavía"), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) else {
                            SmallPlot(inside, key, from, to, low, high, drawn, if (cell.level == CellLevel.OK) ArmorColors.Cyan else color, trf("Celda %s", cell.number))
                            Text("${tr("mín")} ${number(own.min(), 3)} · ${tr("máx")} ${number(own.max(), 3)}", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                        }
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    HistoryChart("$id/cells", tr("Celdas"), "V", 3, samples, cells.map { Curve("cell${it.number}", it.number.toString(), cellColour(it.number - 1)) }, from, to, levels)
    HistoryChart("$id/cells-delta", tr("Diferencia"), "mV", 0, samples, listOf(Curve("delta_mv", tr("Diferencia"), ArmorColors.Amber)), from, to, listOf(settings.spreadWarnMv.toDouble()))
}

@Composable
private fun SmallPlot(samples: List<HistorySample>, key: String, from: Long, to: Long, low: Double, high: Double, levels: List<Double>, color: Color, description: String) {
    val span = (to - from).coerceAtLeast(1)
    val gap = SolarHistoryMath.gapMs(samples)
    Canvas(Modifier.fillMaxWidth().height(44.dp).semantics { contentDescription = description }) {
        fun x(t: Long): Float = (t - from).toFloat() / span * size.width
        fun y(value: Double): Float = (size.height - (value - low) / (high - low) * size.height).toFloat()
        levels.forEach { level -> drawLine(ArmorColors.Alert, Offset(0f, y(level)), Offset(size.width, y(level)), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))) }
        val path = Path()
        var last = Long.MIN_VALUE
        samples.forEach { sample ->
            val value = sample.values[key] ?: return@forEach
            if (last == Long.MIN_VALUE || sample.t - last > gap) path.moveTo(x(sample.t), y(value)) else path.lineTo(x(sample.t), y(value))
            last = sample.t
        }
        drawPath(path, color, style = Stroke(width = 1.4.dp.toPx()))
    }
}

/** The history of an inverter: the sun, the consumption, its battery and the grid. */
@Composable
internal fun InverterHistoryCharts(device: SolarDevice, loader: HistoryLoader) {
    val key = "${device.nodeId}/${device.device}"
    Foldable("inverter/$key", tr("Gráficas"), false, tag = "charts-${device.device}") {
        val (minutes, setMinutes) = rememberNumber("range/$key", 1440)
        Chips(SolarHistoryMath.RANGES, minutes, "range", setMinutes)
        val loaded = rememberHistory(loader, listOf(HistoryRequest(device.nodeId, device.device, minutes)))
        if (stillWaiting(loaded)) return@Foldable
        val samples = loaded!!.answers.first()!!.samples
        val to = loaded.asked
        val from = to - minutes * 60_000L
        HistoryChart("$key/power", tr("Sol y consumo"), "W", 0, samples, listOf(Curve("pv_w", tr("Sol"), ArmorColors.Amber), Curve("out_w", tr("Consumo"), ArmorColors.Cyan)), from, to)
        HistoryChart("$key/battery-v", tr("Tensión de la batería"), "V", 2, samples, listOf(Curve("battery_v", tr("Tensión"), ArmorColors.Cyan)), from, to)
        HistoryChart("$key/battery-a", tr("Corriente de la batería"), "A", 1, samples, listOf(Curve("battery_a", tr("Corriente"), ArmorColors.Amber)), from, to)
        HistoryChart("$key/battery-soc", tr("Nivel de carga"), "%", 0, samples, listOf(Curve("battery_percent", tr("Nivel de carga"), ArmorColors.Ok)), from, to)
        HistoryChart("$key/grid", tr("Tensión de la red"), "V", 1, samples, listOf(Curve("grid_v", tr("Red"), ArmorColors.CyanDeep)), from, to)
    }
}
