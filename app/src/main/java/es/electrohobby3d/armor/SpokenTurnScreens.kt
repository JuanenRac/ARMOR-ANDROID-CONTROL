// ARMOR-ANDROID-CONTROL - the spoken turn on the phone: the microphone, the pieces it is built from, and the section of Settings where the person turns it on.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The rules (the switch, the token, the permission, the number of the challenge) are in SpokenTurn.kt and are tested without a phone. This file is what needs one: AudioRecord, the Keystore
// and the screen. The microphone is touched only by AudioRecordTurnRecorder.record, which the controller calls after the switch, the token and the permission have been checked.
package es.electrohobby3d.armor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Records one phrase with AudioRecord: 16 kHz, mono, 16 bits, the source meant for speech recognition. */
class AudioRecordTurnRecorder(private val context: Context) : TurnRecorder {
    override fun permitted(): Boolean = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")   // checked by permitted() just before, and a refusal at this moment is a SecurityException that the controller turns into "permission refused"
    override fun record(shouldStop: () -> Boolean): ByteArray {
        if (!permitted()) return ByteArray(0)
        val size = maxOf(AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 8192)
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
        } catch (_: IllegalArgumentException) { return ByteArray(0) }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); return ByteArray(0) }
        val sound = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        try {
            recorder.startRecording()
            while (sound.size() < SpokenTurnController.MAX_BYTES && !shouldStop()) {
                val count = recorder.read(buffer, 0, buffer.size)
                if (count < 0) break
                sound.write(buffer, 0, count)
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        return sound.toByteArray()
    }

    private companion object { const val RATE = 16_000 }
}

/** The pieces of the spoken turn, built from the same files wherever they are asked for. */
object SpokenTurnKit {
    private fun files(context: Context, name: String) = PreferencesStore(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))

    fun settings(context: Context) = SpokenTurnSettings(files(context, "armor-control"))
    fun audit(context: Context) = StoreTurnAudit(files(context, "armor-spoken-turn"))
    fun tokens(context: Context) = VoiceTokenVault(files(context, "armor-voice-token"), KeystoreBox("armor-voice-token"))

    fun controller(context: Context) = SpokenTurnController(
        settings(context), tokens(context), AudioRecordTurnRecorder(context.applicationContext),
        { origin, pcm, language, token -> SharedFeed.api.voiceTurn(origin, pcm, language, token) }, audit(context),
        sendWithSession = { origin, pcm, language -> SharedFeed.api.mobileVoiceTurn(origin, pcm, language) },
    )
}

/** The switch of the spoken turn and the voice token. Off until the person turns it on; turning it off, or forgetting the token, takes effect at once. */
@Composable
fun SpokenTurnSection() {
    val context = LocalContext.current
    val settings = remember { SpokenTurnKit.settings(context) }
    val tokens = remember { SpokenTurnKit.tokens(context) }
    val audit = remember { SpokenTurnKit.audit(context) }
    var on by remember { mutableStateOf(settings.enabled) }
    var hasToken by remember { mutableStateOf(tokens.has()) }
    var draft by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Orden hablada al servidor"), fontWeight = FontWeight.SemiBold)
            Text(
                tr("Apagada por defecto. Encendida, el botón de hablar del Asistente graba tu voz (hasta diez segundos), la envía al servidor con la clave de voz y la descarta: el móvil no la guarda. Es lo único de la app que usa el micrófono, y solo cuando tocas el botón. Si el servidor pide un número para silenciar la alarma, lo verás en pantalla unos segundos."),
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Permitir la orden hablada"), Modifier.weight(1f))
                Switch(checked = on, onCheckedChange = { next -> on = next; settings.enabled = next; audit.record(if (next) "switch_on" else "switch_off") })
            }
            if (on) {
                Text(
                    if (hasToken) tr("La clave de voz está guardada, cifrada en este móvil.") else tr("Escribe la clave de voz de la instalación (la del servicio de voz). Quien la tenga puede dar órdenes por voz: guárdala solo en móviles tuyos."),
                    style = MaterialTheme.typography.bodySmall, color = if (hasToken) ArmorColors.Muted else ArmorColors.Amber,
                )
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it.take(VoiceTokenVault.MAX_LENGTH + 1); problem = false }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(tr("Clave de voz")) }, visualTransformation = PasswordVisualTransformation(),
                    isError = problem, supportingText = { if (problem) Text(tr("La clave no es válida: sin espacios, de 8 a 200 caracteres.")) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (tokens.save(draft)) { hasToken = true; draft = ""; problem = false; audit.record("token_saved") } else problem = true
                    }, enabled = draft.isNotBlank()) { Text(tr("Guardar la clave")) }
                    OutlinedButton(onClick = { tokens.clear(); hasToken = false; audit.record("token_cleared") }, enabled = hasToken) { Text(tr("Olvidar la clave")) }
                }
            }
        }
    }
}
