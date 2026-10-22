// ARMOR-ANDROID-CONTROL - the alarm window of the open app: a pulsing banner over the screens while an alarm waits for a person.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.Alarm
import es.electrohobby3d.armor.model.DeviceText

/** What the banner says: the worst alarm that nobody has confirmed, and how many wait. [key] changes whenever the set of waiting alarms changes, so a closed banner comes back for a new alarm. */
data class AlarmBannerModel(val severity: String, val count: Int, val title: String, val detail: String, val key: String, val alarm: Alarm)

object AlarmBannerPolicy {
    private fun rank(severity: String) = when (severity) { "critical" -> 3; "high" -> 2; else -> 1 }

    /** The alarms that wait for a person: not confirmed, and not the silent kind (the alarm of a duress PIN is never shown on a phone). */
    fun waiting(alarms: List<Alarm>): List<Alarm> = alarms.filter { !it.acknowledged && !AlarmPolicy.isSilent(it.code) }

    fun key(alarms: List<Alarm>): String = waiting(alarms).map { it.id }.sorted().joinToString(",")

    /** The banner to show, or null when nothing waits or the person closed this very set of alarms. */
    fun from(alarms: List<Alarm>, closedKey: String?): AlarmBannerModel? {
        val open = waiting(alarms)
        if (open.isEmpty()) return null
        val key = key(alarms)
        if (key == closedKey) return null
        val worst = open.sortedWith(compareByDescending<Alarm> { rank(it.severity) }.thenByDescending { it.raisedAt }).first()
        val title = if (open.size == 1) trf("ALARMA %s", DeviceText.severityLabel(worst.severity)) else trf("%d alarmas sin confirmar", open.size)
        return AlarmBannerModel(worst.severity, open.size, title, "${DeviceText.alarmText(worst.code)} · ${worst.sourceId}", key, worst)
    }

    /** Critical is red and loud; the rest are amber. */
    fun colour(severity: String): Color = if (severity == "critical") ArmorColors.Alert else ArmorColors.Amber

    /** How slow the pulse is: a critical alarm beats faster. */
    fun pulseMillis(severity: String): Int = if (severity == "critical") 600 else 1100
}

/** A banner that pulses while an alarm waits. It lets the app work under it, goes to the alarms, confirms from the banner and can be closed until another alarm comes. */
@Composable
fun AlarmBanner(model: AlarmBannerModel, canConfirm: Boolean, onOpen: () -> Unit, onConfirm: (Alarm) -> Unit, onClose: (String) -> Unit, modifier: Modifier = Modifier) {
    val colour = AlarmBannerPolicy.colour(model.severity)
    // A person who turned the animations off in the phone gets the banner still, never a flashing one.
    val resolver = LocalContext.current.contentResolver
    val still = remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val pulse by rememberInfiniteTransition(label = "alarm-banner").animateFloat(
        initialValue = 1f, targetValue = if (still) 1f else 0.45f,
        animationSpec = infiniteRepeatable(tween(AlarmBannerPolicy.pulseMillis(model.severity)), RepeatMode.Reverse), label = "alarm-banner-pulse",
    )
    Card(
        modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colour.copy(alpha = 0.14f)),
        border = BorderStroke(2.dp, colour.copy(alpha = pulse)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(model.title, style = MaterialTheme.typography.titleSmall, color = colour, modifier = Modifier.alpha(pulse))
            Text(model.detail, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Text)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onClose(model.key) }) { Text(tr("Cerrar")) }
                TextButton(onClick = onOpen) { Text(tr("Ver alarmas")) }
                if (canConfirm) Button(onClick = { onConfirm(model.alarm) }) { Text(tr("Confirmar")) }
            }
        }
    }
}
