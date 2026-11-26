// ARMOR-ANDROID-CONTROL - the pictures of a battery: its state in words and colour, the numbers of the bank and every cell of every module.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.CellLevel
import es.electrohobby3d.armor.model.CellView
import es.electrohobby3d.armor.model.ModuleView
import es.electrohobby3d.armor.model.SolarCells
import es.electrohobby3d.armor.model.SolarText
import es.electrohobby3d.armor.model.SolarViewSettings

@Composable
internal fun rememberSolarViewSettings(): SolarViewSettings {
    val context = LocalContext.current
    return remember {
        try { SolarViewSettings.parse(context.assets.open("solar_view.json").bufferedReader(Charsets.UTF_8).use { it.readText() }) } catch (_: java.io.IOException) { SolarViewSettings() }
    }
}

internal fun levelColor(level: CellLevel, stale: Boolean = false): Color = when {
    stale -> ArmorColors.Muted
    level == CellLevel.FAULT -> ArmorColors.Alert
    level == CellLevel.WARN -> ArmorColors.Amber
    else -> ArmorColors.Ok
}

internal fun levelIcon(level: CellLevel): ImageVector = when (level) { CellLevel.FAULT -> Icons.Filled.Error; CellLevel.WARN -> Icons.Filled.Warning; else -> Icons.Filled.CheckCircle }

internal fun levelWord(level: CellLevel): String = when (level) { CellLevel.FAULT -> tr("Fallo"); CellLevel.WARN -> tr("Aviso"); else -> tr("Correcto") }

/** A soft pulse (once a second by default) for a fault; steady when there is none or when the person turned the animations off in the phone. */
@Composable
internal fun blinkAlpha(active: Boolean, periodMs: Int): Float {
    val resolver = LocalContext.current.contentResolver
    val still = remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    if (!active || still) return 1f
    val value by rememberInfiniteTransition(label = "solar-fault").animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(periodMs.coerceAtLeast(SolarViewSettings.FASTEST_BLINK_MS) / 2), RepeatMode.Reverse), label = "solar-fault-alpha",
    )
    return value
}

/** Icon, word and colour together: the state is never told by the colour alone. */
@Composable
internal fun LevelChip(level: CellLevel, stale: Boolean, settings: SolarViewSettings) {
    val color = levelColor(level, stale)
    val alpha = blinkAlpha(level == CellLevel.FAULT && !stale, settings.blinkPeriodMs)
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f * alpha)).border(1.dp, color.copy(alpha = alpha), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(levelIcon(level), null, tint = color.copy(alpha = alpha), modifier = Modifier.size(14.dp))
        Text(levelWord(level), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** A number of the bank with its name, in a soft box. */
internal data class Stat(val label: String, val value: String, val color: Color = ArmorColors.Text)

@Composable
internal fun StatGrid(stats: List<Stat>) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = SolarCells.columns(maxWidth.value, scale, 130).coerceAtMost(3)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { stat ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ArmorColors.Cyan.copy(alpha = 0.06f)).semantics(mergeDescendants = true) {}.padding(8.dp)) {
                            Text(stat.label, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                            Text(stat.value, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.sp, color = stat.color)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Every cell of a module, as many to a row as fit without cutting its voltage. */
@Composable
internal fun CellGrid(view: ModuleView, stale: Boolean, settings: SolarViewSettings) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = SolarCells.columns(maxWidth.value, scale, settings.minCellTileDp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            view.cells.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { cell -> CellTile(view.module.number, cell, stale, settings, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CellTile(module: Int, cell: CellView, stale: Boolean, settings: SolarViewSettings, modifier: Modifier) {
    val color = levelColor(cell.level, stale)
    val alpha = blinkAlpha(cell.level == CellLevel.FAULT && !stale, settings.blinkPeriodMs)
    val mark = when { cell.highest -> "▲ " + tr("máx"); cell.lowest -> "▼ " + tr("mín"); else -> "" }
    val markColor = if (cell.highest) ArmorColors.Cyan else ArmorColors.Amber
    val shape = RoundedCornerShape(10.dp)
    val spoken = trf("Celda %s", cell.number) + ": " + SolarText.cell(cell.volts) + " V, " + levelWord(cell.level) + (if (mark.isEmpty()) "" else ", $mark")
    val calm = cell.level == CellLevel.OK
    Column(
        modifier.clip(shape).background(color.copy(alpha = if (calm) 0.10f else 0.25f * alpha))
            .border(BorderStroke(if (calm) 1.dp else 2.dp, color.copy(alpha = if (calm) 0.35f else alpha)), shape)
            .semantics { contentDescription = spoken }.padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(cell.number.toString(), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            if (!calm) Icon(levelIcon(cell.level), null, tint = color.copy(alpha = alpha), modifier = Modifier.size(12.dp))
            Spacer(Modifier.weight(1f))
            if (mark.isNotEmpty()) Text(mark, style = MaterialTheme.typography.labelSmall, color = markColor)
        }
        Text(SolarText.cell(cell.volts) + " V", Modifier.fillMaxWidth().testTag("solar-cell-$module-${cell.number}"), fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 0.sp, color = ArmorColors.Text)
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(ArmorColors.Outline)) {
            Box(Modifier.fillMaxWidth(cell.fill).fillMaxHeight().background(color))
        }
    }
}

/** What is wrong with a module, in words, one line each. */
internal fun troubleLines(view: ModuleView): List<String> {
    val t = view.trouble
    return listOfNotNull(
        t.state.takeIf { it.isNotEmpty() }?.let { trf("El módulo informa el estado %s", it) },
        t.abnormalCells.takeIf { it.isNotEmpty() }?.let { tr("Celda anormal") + ": " + it.joinToString(", ") },
        t.outOfRangeCells.takeIf { it.isNotEmpty() }?.let { trf("Tensión fuera de rango en las celdas %s", it.joinToString(", ")) },
        t.spreadMv?.let { trf("Diferencia alta entre celdas: %s mV", it) },
        if (t.hot) tr("Temperatura alta") else null,
        if (t.cold) tr("Demasiado frío para cargar") else null,
        t.missingCells.takeIf { it > 0 }?.let { trf("Leídas %s de %s celdas", view.cells.size, view.cells.size + it) },
    )
}
