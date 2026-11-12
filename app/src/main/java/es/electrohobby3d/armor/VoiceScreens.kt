// ARMOR-ANDROID-CONTROL - the Assistant screen: written and spoken commands to the system (arm, disarm, say the state, silence the alarm).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// What is typed goes to the server as it is. What is spoken with the first microphone button is turned into text by the phone's own speech recognition (the system's dialog, so
// no audio leaves the phone through this app) and goes the same way. The second button, the spoken turn, exists only when the person turned it on in Settings: it records one
// phrase and sends the sound to the server (SpokenTurn.kt), and shows the number of the challenge mode when the server asks for one. The server asks the voice gateway what the
// phrase means and carries out what is accepted; arming and disarming ask for a second turn, which is shown here as two buttons. The answer is also said aloud when the voice is on.
package es.electrohobby3d.armor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import es.electrohobby3d.armor.model.PendingVoice
import es.electrohobby3d.armor.model.VoiceMessage
import es.electrohobby3d.armor.model.VoiceUiState
import java.util.Locale

/** The four things it understands, as phrases to tap. */
private val Suggestions: List<String> get() = listOf(
    tr("Estado del sistema"), tr("Armar el sistema"), tr("Desarmar el sistema"), tr("Silenciar la alarma"), tr("Alarmas activas"), tr("Hay alguien"), tr("Estado de los nodos"), tr("Estado de las cámaras"),
    tr("Estado solar"), tr("Consumo eléctrico"), tr("Estado de la red"), tr("Qué hora es"), tr("Enciende las luces"), tr("Apaga las luces"), tr("Ayuda"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceScreen(
    state: VoiceUiState,
    enabled: Boolean,
    onSend: (text: String, spoken: (String) -> Unit) -> Unit,
    onConfirm: (pending: PendingVoice, spoken: (String) -> Unit) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    spokenOn: Boolean = false,
    spoken: SpokenTurnUiState = SpokenTurnUiState(),
    onSpokenStart: (spoken: (String) -> Unit) -> Unit = {},
    onSpokenStop: () -> Unit = {},
    onSpokenRefused: () -> Unit = {},
    onChallengeGone: () -> Unit = {},
) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var speak by rememberSaveable { mutableStateOf(true) }
    var notice by remember { mutableStateOf("") }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val created = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) engine?.language = Locale.forLanguageTag(Tr.language) }
        engine = created
        onDispose { created.stop(); created.shutdown(); engine = null }
    }
    val say: (String) -> Unit = { line -> if (speak) engine?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "armor-assistant") }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) onSend(heard, say)
    }
    fun listen() {
        notice = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Tr.language).putExtra(RecognizerIntent.EXTRA_PROMPT, tr("Di una orden")).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try { recognizer.launch(intent) } catch (error: ActivityNotFoundException) { notice = tr("Este móvil no tiene reconocimiento de voz. Puedes escribir la orden.") }
    }

    // The microphone is asked for only here, only when the switch is on and the person taps the button: a refusal ends the turn without recording anything.
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) onSpokenStart(say) else onSpokenRefused() }
    fun tapSpoken() {
        if (spoken.active) { onSpokenStop(); return }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) onSpokenStart(say) else microphone.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(Unit) { onDispose { onChallengeGone() } }   // the number does not outlive the screen

    val list = rememberLazyListState()
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.size - 1) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("Escribe o di una orden: armar o desarmar el sistema (te pido confirmar), el estado, las alarmas, los nodos, las cámaras, los radares, lo solar, el consumo, la red, la hora, encender o apagar las luces y silenciar la alarma. Di «ayuda» para oírlo."),
                Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
            IconButton(onClick = { speak = !speak; if (!speak) engine?.stop() }) {
                Icon(if (speak) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = if (speak) tr("Silenciar la voz del móvil") else tr("Activar la voz del móvil"), tint = if (speak) ArmorColors.Cyan else ArmorColors.Muted)
            }
            IconButton(onClick = onClear, enabled = state.messages.isNotEmpty()) { Icon(Icons.Filled.DeleteSweep, contentDescription = tr("Borrar la conversación"), tint = ArmorColors.Muted) }
        }
        if (state.available == false) Text(tr("Este sistema no tiene instalado el asistente de voz: las órdenes no funcionarán hasta que se instale."), color = ArmorColors.Amber, style = MaterialTheme.typography.bodySmall)
        if (!enabled) Text(tr("Conecta con el servidor e inicia sesión para dar órdenes."), color = ArmorColors.Amber, style = MaterialTheme.typography.bodySmall)

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.messages.isEmpty()) item {
                Text(tr("Aún no hay órdenes. Toca una de las de abajo, escribe o pulsa el micrófono."), color = ArmorColors.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            }
            items(state.messages) { message -> Bubble(message, enabled && !state.busy, onConfirm = { onConfirm(it, say) }, onCancel = onCancel) }
            if (state.busy) item { Text("…", color = ArmorColors.Muted) }
        }

        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Suggestions.forEach { phrase -> AssistChip(onClick = { onSend(phrase, say) }, label = { Text(phrase) }, enabled = enabled && !state.busy) }
        }
        if (notice.isNotEmpty()) Text(notice, color = ArmorColors.Amber, style = MaterialTheme.typography.bodySmall)
        spoken.challenge?.let { ChallengeCard(it, onChallengeGone) }
        spoken.notice?.let { Text(it, color = ArmorColors.Amber, style = MaterialTheme.typography.bodySmall) }
        if (spoken.listening) Text(tr("Te escucho: toca el botón otra vez cuando acabes."), color = ArmorColors.Cyan, style = MaterialTheme.typography.bodySmall)
        if (spoken.sending) Text(tr("Enviando tu voz al servidor…"), color = ArmorColors.Muted, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(200) }, modifier = Modifier.weight(1f), singleLine = true, enabled = enabled,
                placeholder = { Text(tr("Escribe una orden")) },
            )
            IconButton(onClick = { listen() }, enabled = enabled && !state.busy) { Icon(Icons.Filled.Mic, contentDescription = tr("Decir una orden"), tint = if (enabled && !state.busy) ArmorColors.Cyan else ArmorColors.Muted) }
            if (spokenOn) IconButton(onClick = { tapSpoken() }, enabled = enabled && !state.busy && !spoken.sending) {
                Icon(if (spoken.listening) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver, contentDescription = if (spoken.listening) tr("Terminar de hablar") else tr("Hablar al servidor"), tint = if (spoken.listening) ArmorColors.Amber else if (enabled && !state.busy) ArmorColors.Cyan else ArmorColors.Muted)
            }
            IconButton(onClick = { val phrase = text.trim(); if (phrase.isNotEmpty()) { text = ""; onSend(phrase, say) } }, enabled = enabled && !state.busy && text.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = tr("Enviar la orden"), tint = if (enabled && !state.busy && text.isNotBlank()) ArmorColors.Cyan else ArmorColors.Muted)
            }
        }
    }
}

/** The number of the challenge mode: big, for the few seconds the wall panel shows it too; then it is gone. */
@Composable
private fun ChallengeCard(challenge: ShownChallenge, onGone: () -> Unit) {
    var now by remember(challenge) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(challenge) {
        while (!challenge.expired(now)) { delay(250); now = System.currentTimeMillis() }
        onGone()
    }
    if (challenge.expired(now)) return
    Column(Modifier.fillMaxWidth().background(ArmorColors.SurfaceRaised, RoundedCornerShape(14.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Di este número en tu próxima orden"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        Text(challenge.number, fontSize = 48.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ArmorColors.Cyan)
        Text(trf("Se oculta en %s s", ((challenge.remainingMs(now) + 999) / 1000).toString()), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
    }
}

@Composable
private fun Bubble(message: VoiceMessage, canAnswer: Boolean, onConfirm: (PendingVoice) -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromPerson) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 320.dp)
                .background(if (message.fromPerson) ArmorColors.CyanDeep.copy(alpha = 0.35f) else ArmorColors.SurfaceRaised, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(message.text, color = if (message.problem) ArmorColors.Amber else ArmorColors.Text, style = MaterialTheme.typography.bodyMedium)
            message.pending?.let { pending ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onConfirm(pending) }, enabled = canAnswer) { Text(tr("Confirmar")) }
                    OutlinedButton(onClick = onCancel, enabled = canAnswer) { Text(tr("Cancelar")) }
                }
            }
        }
    }
}
