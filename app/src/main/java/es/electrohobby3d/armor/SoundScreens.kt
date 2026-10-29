// ARMOR-ANDROID-CONTROL - the settings of the sound and the voice of the alarms: a sound for each severity and kind (with a button to hear it) and the voice that says them.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The sound of each severity and of each kind of notice, with the button that plays it. */
@Composable
fun AlarmSoundsSection() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SoundPreferences.load(context)) }
    var picking by remember { mutableStateOf<Pair<String, Boolean>?>(null) }   // what is being chosen: the key and whether it is a kind (else a severity)
    var note by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf<String?>(null) }
    fun change(next: SoundSettings) { settings = next; SoundPreferences.save(context, next) }
    DisposableEffect(Unit) { onDispose { AlarmSounds.stop() } }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = picking
        picking = null
        if (uri != null && target != null) {
            val kept = AlarmSounds.keepOwn(context, uri)
            if (kept == null) note = tr("Ese archivo no sirve: tiene que ser un sonido de menos de 3 MB.")
            else { note = null; change(applyChoice(settings, target, kept)) }
        }
    }
    @Composable fun SoundRow(key: String, title: String, kind: Boolean) {
        val severity = if (kind) "high" else key
        val choice = (if (kind) settings.byKind[key] else settings.bySeverity[key]) ?: SoundChoice()
        val id = (if (kind) "K" else "S") + key
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(soundTitle(choice, kind), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                IconButton(onClick = {
                    if (playing == id) { AlarmSounds.stop(); playing = null }
                    else { AlarmSounds.play(context, if (kind) "high" else severity, if (kind && choice.isDefault) settings.bySeverity["high"] ?: SoundChoice() else choice); playing = id }
                }) { Icon(if (playing == id) Icons.Filled.Stop else Icons.Filled.PlayArrow, tr("Probar")) }
                OutlinedButton(onClick = { picking = key to kind }) { Text(tr("Cambiar")) }
            }
        }
    }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Sonido de cada aviso"), fontWeight = FontWeight.SemiBold)
            Text(tr("Elige el sonido de cada gravedad y, si quieres, el de cada tipo de aviso (el del tipo manda sobre el de la gravedad). Puede ser uno del móvil o un archivo tuyo de menos de 3 MB. Pulsa el botón de reproducir para oírlo."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Text(tr("Por gravedad"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            SoundSettings.SEVERITIES.forEach { SoundRow(it, severityTitle(it), kind = false) }
            Text(tr("Por tipo"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            NoticeKinds.CHOICES.forEach { SoundRow(it, noticeKindTitle(it), kind = true) }
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Cyan) }
        }
    }
    picking?.let { target ->
        val system = remember { AlarmSounds.systemSounds(context) }
        AlertDialog(
            onDismissRequest = { picking = null; AlarmSounds.stop() },
            title = { Text(tr("Elige un sonido")) },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    item {
                        TextButton(onClick = { change(applyChoice(settings, target, SoundChoice())); picking = null; AlarmSounds.stop() }, modifier = Modifier.fillMaxWidth()) { Text(tr("Predeterminado")) }
                        TextButton(onClick = { file.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) { Text(tr("Un archivo mío…")) }
                        Text(tr("Sonidos del móvil"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    }
                    items(system) { (title, uri) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { change(applyChoice(settings, target, SoundChoice(SoundChoice.SYSTEM, uri, title))); picking = null; AlarmSounds.stop() }, modifier = Modifier.weight(1f)) { Text(title, maxLines = 1) }
                            IconButton(onClick = { AlarmSounds.play(context, if (target.second) "high" else target.first, SoundChoice(SoundChoice.SYSTEM, uri, title)) }) { Icon(Icons.Filled.PlayArrow, tr("Probar")) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = null; AlarmSounds.stop() }) { Text(tr("Cancelar")) } },
        )
    }
}

/** The settings with the choice put on the severity or the kind that is being chosen (the second of the pair says which of the two it is). */
internal fun applyChoice(current: SoundSettings, target: Pair<String, Boolean>, choice: SoundChoice): SoundSettings =
    if (target.second) current.withKind(target.first, choice) else current.withSeverity(target.first, choice)

private fun soundTitle(choice: SoundChoice, kind: Boolean): String = when {
    choice.isDefault -> if (kind) tr("Como su gravedad") else tr("Predeterminado")
    choice.kind == SoundChoice.OWN -> trf("Mío: %s", choice.name)
    else -> choice.name.ifBlank { tr("Sonido del móvil") }
}

private fun severityTitle(severity: String): String = when (severity) { "critical" -> tr("Alarmas críticas"); "high" -> tr("Altas"); else -> tr("Avisos (gravedad baja)") }

/** The title of a kind of notice (the same words as the filters of the settings). */
fun noticeKindTitle(kind: String): String = when (kind) {
    "alert" -> tr("Alertas del radar")
    "camera" -> tr("Cámaras sin respuesta")
    "node" -> tr("Nodos fuera de línea o en silencio")
    "alarm.device" -> tr("Alarmas de dispositivos")
    "alarm.solar" -> tr("Alarmas de la instalación solar")
    "alarm.electrical" -> tr("Alarmas eléctricas")
    "alarm.network" -> tr("Alarmas de red")
    "alarm.alarm" -> tr("Alarmas de las centrales")
    else -> tr("Otras alarmas")
}

/** The voice that says the alarms: woman or man, age, style and speed, with a button to hear it. */
@Composable
fun AlarmVoiceSection() {
    val context = LocalContext.current
    var speak by rememberSaveable { mutableStateOf(AlarmNotifier.speaksAlarms(context)) }
    var voice by remember { mutableStateOf(VoicePreferences.load(context)) }
    fun change(next: VoiceSettings) { voice = next; VoicePreferences.save(context, next) }
    @Composable fun Choices(label: String, values: List<String>, current: String, title: (String) -> String, set: (String) -> Unit) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            values.chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.forEach { value ->
                        if (value == current) Button(onClick = {}, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text(title(value), maxLines = 2) }
                        else OutlinedButton(onClick = { set(value) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text(title(value), maxLines = 2) }
                    }
                }
            }
        }
    }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Voz de las alarmas"), fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Decir la alarma en voz alta"))
                    Text(tr("Con la voz del móvil, para las críticas y las altas."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                Switch(checked = speak, onCheckedChange = { speak = it; AlarmNotifier.setSpeaksAlarms(context, it) })
            }
            Text(tr("Cómo suena depende de las voces que tenga instaladas tu móvil: si ninguna dice que es de mujer o de hombre, se aproxima con el tono."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Choices(tr("Voz"), VoiceSettings.GENDERS, voice.gender, { when (it) { "female" -> tr("Mujer"); "male" -> tr("Hombre"); else -> tr("Cualquiera") } }) { change(voice.copy(gender = it)) }
            Choices(tr("Edad"), VoiceSettings.AGES, voice.age, { when (it) { "child" -> tr("Niño"); "senior" -> tr("Mayor"); else -> tr("Adulto") } }) { change(voice.copy(age = it)) }
            Choices(tr("Estilo"), VoiceSettings.STYLES, voice.style, { when (it) { "hero" -> tr("Héroe de acción: rápido y brillante"); "dark" -> tr("Señor oscuro: lento y grave"); else -> tr("Normal") } }) { change(voice.copy(style = it)) }
            Text(trf("Velocidad: %s", "${voice.speed} %"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Slider(value = voice.speed.toFloat(), onValueChange = { change(voice.copy(speed = it.toInt())) }, valueRange = VoiceSettings.MIN_SPEED.toFloat()..VoiceSettings.MAX_SPEED.toFloat())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Decir también los avisos de gravedad baja"), Modifier.weight(1f))
                Switch(checked = voice.warnings, onCheckedChange = { change(voice.copy(warnings = it)) })
            }
            Button(onClick = { AlarmSpeaker.stop(); AlarmSpeaker.say(context, tr("Alarma de prueba. Humo detectado en la cocina."), voice) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(tr("Probar la voz")) }
        }
    }
}
