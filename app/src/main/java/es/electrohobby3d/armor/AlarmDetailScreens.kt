// ARMOR-ANDROID-CONTROL - the detail of one alarm: what happened, where, when, and every fact it carries with its label in the language of the person.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.DeviceText

/** What the card and the detail say about the state of an alarm. */
internal fun alarmStateText(alarm: Alarm): String =
    if (alarm.acknowledged) (if (alarm.cleared) trf("Vista por %s", alarm.acknowledgedBy) else trf("Vista por %s · sigue activa", alarm.acknowledgedBy))
    else if (alarm.cleared) tr("Ya terminó · falta confirmarla") else tr("Activa ahora")

/** The window with the whole alarm. Every fact is listed: one the table does not know is shown with its key as it came, never hidden. */
@Composable
fun AlarmDetailDialog(alarm: Alarm, sourceName: String, canConfirm: Boolean, onConfirm: (Alarm) -> Unit, onDismiss: () -> Unit) {
    val tint = if (alarm.acknowledged || alarm.cleared) ArmorColors.Muted else AlarmBannerPolicy.colour(alarm.severity)
    val facts = AlarmFacts.rows(alarm.detail)
    val help = AlarmFacts.help(alarm.code)   // what the alarm means and what to check, from the table every client shares; nothing when the code has none
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
        icon = { IconBadge(if (alarm.cleared) Icons.Filled.CheckCircle else Icons.Filled.Warning, tint, size = 56.dp) },
        title = { Text(DeviceText.alarmText(alarm.code)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine(tr("Gravedad"), DeviceText.severityLabel(alarm.severity))
                DetailLine(tr("Origen"), sourceName)
                DetailLine(tr("Hora"), Friendly.dayAndClock(alarm.raisedAt))
                DetailLine(tr("Estado"), alarmStateText(alarm))
                if (alarm.falseAlarm == true) DetailLine(tr("Falsa alarma"), tr("sí"))
                if (help != null) Text(help, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Text)
                HorizontalDivider(color = ArmorColors.Muted.copy(alpha = 0.3f))
                Text(tr("Detalles"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                if (facts.isEmpty()) Text(tr("Esta alarma no trae más detalles."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                facts.forEach { DetailLine(it.label, it.value) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        confirmButton = { if (canConfirm && !alarm.acknowledged) Button(onClick = { onConfirm(alarm); onDismiss() }) { Text(tr("Confirmar")) } },
    )
}

/** A label above its value, read by a screen reader as one phrase. */
@Composable
private fun DetailLine(label: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}.padding(vertical = 1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ArmorColors.Text)
    }
}
