// ARMOR-ANDROID-CONTROL - the spoken turn: the phone listens to one phrase, sends it to the server as audio and shows the number of the challenge mode when the server asks for it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// OFF by default, and the only part of the app that ever opens the microphone. Until the person turns the switch on (Settings) nothing here records, asks for the permission or sends a
// sound: the Assistant keeps working with written commands and with the speech recognition of the phone, which sends only text. With the switch on, one tap records up to ten seconds of 16 kHz
// mono sound, sends it with the voice token to `POST /api/v1/voice/turn` of the server and drops it; the sound is kept nowhere. When the security option `voice.silence_confirmation` is
// `challenge` the server answers a number that the person must say in the next turn; the phone shows it for nineteen seconds (as the wall panel does) and then removes it. Anything that is not
// exactly what the contract says - an answer that is not a voice reply, a number that is not two to six digits - shows nothing: the turn fails closed. Every decision is written to a short
// audit trail on the phone (no words, no sound). The voice token is sealed with a key of the Android Keystore; it is the shared secret of the voice route, so it is entered by the person.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.contracts.parseVoiceTurnResult
import es.electrohobby3d.armor.model.VoiceParser
import org.json.JSONObject

/** The person's switch. Off until they turn it on. */
class SpokenTurnSettings(private val store: KeyValueStore) {
    var enabled: Boolean
        get() = store.get(ENABLED) == "1"
        set(on) = store.put(ENABLED, if (on) "1" else "0")

    companion object { private const val ENABLED = "spoken_turn" }
}

/** The voice token of the installation, sealed. A token with spaces, control characters or more than 200 characters is not kept (it could not be a header value). */
class VoiceTokenVault(private val store: KeyValueStore, private val box: SecretBox) {
    /** True when the token was kept. */
    fun save(token: String): Boolean {
        val clean = token.trim()
        if (!valid(clean)) return false
        store.put(KEY, box.seal(clean))
        return true
    }

    /** The token, or null when there is none, the phone is locked or what is kept cannot be read (then it is dropped). */
    fun load(): String? {
        val sealed = store.get(KEY) ?: return null
        if (!box.usable()) return null
        val plain = box.open(sealed)
        if (plain == null) { store.remove(KEY); return null }
        return plain.takeIf { valid(it) }
    }

    fun has(): Boolean = store.get(KEY) != null
    fun clear() { store.remove(KEY) }

    companion object {
        const val KEY = "voice_token"
        const val MAX_LENGTH = 200
        fun valid(token: String): Boolean = token.length in 8..MAX_LENGTH && token.all { it in '!'..'~' }
    }
}

/** The number of the challenge mode: two to six digits, nothing else. */
object ChallengeNumber {
    /** The nineteen seconds the wall panel shows it for. */
    const val SHOWN_FOR_MS = 19_000L

    /** The number when [raw] is exactly two to six digits 0-9 (no sign, no space, no other script's digits), else null. */
    fun parse(raw: String?): String? = raw?.takeIf { it.length in 2..6 && it.all { c -> c in '0'..'9' } }
}

/** A number on the screen: it was shown at [shownAtMs] and is gone [ChallengeNumber.SHOWN_FOR_MS] later. */
data class ShownChallenge(val number: String, val shownAtMs: Long) {
    fun expired(nowMs: Long): Boolean = nowMs - shownAtMs >= ChallengeNumber.SHOWN_FOR_MS || nowMs < shownAtMs
    fun remainingMs(nowMs: Long): Long = if (expired(nowMs)) 0 else ChallengeNumber.SHOWN_FOR_MS - (nowMs - shownAtMs)
}

/** What came of one spoken turn. */
sealed interface SpokenTurnOutcome {
    /** The switch is off: nothing was recorded or sent. */
    object Off : SpokenTurnOutcome
    /** No voice token was entered. */
    object NoToken : SpokenTurnOutcome
    /** The person refused the microphone permission (or the system did): nothing was recorded. */
    object PermissionDenied : SpokenTurnOutcome
    /** Nothing, or half a sample, was recorded. */
    object NothingHeard : SpokenTurnOutcome
    /** The server answered something that is not a voice reply, or a number that is not two to six digits: nothing is shown. */
    object Malformed : SpokenTurnOutcome
    /** The server refused or did not answer; [code] is its error code when it gave one. */
    data class Failed(val code: String?) : SpokenTurnOutcome
    data class Answered(val heard: String, val reply: String, val challenge: ShownChallenge?) : SpokenTurnOutcome
}

/** The microphone, behind an interface so the rules can be tested without a phone. */
interface TurnRecorder {
    /** Whether the permission to record is granted now. */
    fun permitted(): Boolean
    /** Records 16 kHz mono 16-bit little-endian sound until [shouldStop] is true or the longest turn is reached; empty when it could not start. */
    fun record(shouldStop: () -> Boolean): ByteArray
}

/** The short audit trail of the spoken turn on the phone: the last events, with their time and nothing that was said. */
interface TurnAudit {
    fun record(event: String)
}

class StoreTurnAudit(private val store: KeyValueStore, private val clock: () -> Long = System::currentTimeMillis) : TurnAudit {
    override fun record(event: String) {
        val line = "${clock()}|${event.filter { it.isLetterOrDigit() || it == '_' || it == ':' }.take(40)}"
        store.put(KEY, (entries() + line).takeLast(MAX_ENTRIES).joinToString("\n"))
    }

    /** The trail, oldest first. */
    fun entries(): List<String> = store.get(KEY)?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

    companion object { const val KEY = "spoken_turn_audit"; const val MAX_ENTRIES = 50 }
}

/** What a turn needs from the server: the audio, the language and the voice token in, the JSON of the answer out. */
typealias TurnSender = (origin: String, pcm: ByteArray, language: String, voiceToken: String) -> JSONObject

/** What a turn needs when the phone has no voice token: the audio and the language in, the JSON of the answer out; the person's session does the rest. */
typealias SessionTurnSender = (origin: String, pcm: ByteArray, language: String) -> JSONObject

class SpokenTurnController(
    private val settings: SpokenTurnSettings,
    private val tokens: VoiceTokenVault,
    private val recorder: TurnRecorder,
    private val send: TurnSender,
    private val audit: TurnAudit,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Used when no voice token was entered: the turn goes with the session through the route of the phone, which the server keeps closed until an administrator opens it in Studio. */
    private val sendWithSession: SessionTurnSender? = null,
) {
    /** One turn: checks the switch, the token and the permission - in that order, and the microphone is touched only after all three - records, sends and reads the answer. */
    fun run(origin: String, language: String, shouldStop: () -> Boolean = { false }): SpokenTurnOutcome {
        if (!settings.enabled) { audit.record("blocked_switch_off"); return SpokenTurnOutcome.Off }
        val token = tokens.load()
        if (token == null && sendWithSession == null) { audit.record("no_token"); return SpokenTurnOutcome.NoToken }
        if (!recorder.permitted()) return denied()
        val sound = try { recorder.record(shouldStop) } catch (_: SecurityException) { return denied() }
        if (sound.size < MIN_BYTES || sound.size % 2 != 0) { audit.record("nothing_heard"); return SpokenTurnOutcome.NothingHeard }
        val pcm = if (sound.size > MAX_BYTES) sound.copyOf(MAX_BYTES) else sound
        audit.record("sent")
        val json = try { if (token != null) send(origin, pcm, language, token) else sendWithSession!!(origin, pcm, language) } catch (error: Exception) {
            val code = VoiceParser.errorCode(error.message)
            audit.record("failed:" + (code ?: "network").take(24))
            return SpokenTurnOutcome.Failed(code)
        }
        val result = try { parseVoiceTurnResult(json) } catch (_: Exception) { null }
        if (result == null) { audit.record("malformed_answer"); return SpokenTurnOutcome.Malformed }
        val challenge = if (json.has("challenge")) ChallengeNumber.parse(result.challenge) ?: run { audit.record("malformed_challenge"); return SpokenTurnOutcome.Malformed } else null
        audit.record(if (challenge != null) "answered_challenge" else "answered")
        return SpokenTurnOutcome.Answered(result.heard, result.reply, challenge?.let { ShownChallenge(it, clock()) })
    }

    /** The system or the person refused the microphone after the person asked for a turn. */
    fun permissionRefused(): SpokenTurnOutcome = denied()

    private fun denied(): SpokenTurnOutcome { audit.record("permission_denied"); return SpokenTurnOutcome.PermissionDenied }

    companion object {
        /** A tenth of a second: less than that is a tap, not a phrase. */
        const val MIN_BYTES = 3_200
        /** Ten seconds of 16 kHz 16-bit mono, well under the 15 seconds the server takes by default. */
        const val MAX_BYTES = 320_000
    }
}

/** What the Assistant shows of the spoken turn: whether the microphone is open, whether the sound is on its way, and the number to say, if the server asked for one. */
data class SpokenTurnUiState(val listening: Boolean = false, val sending: Boolean = false, val challenge: ShownChallenge? = null, val notice: String? = null) {
    val active: Boolean get() = listening || sending
}

/** The words for what came of a turn (the conversation shows what was heard and said; this is for what did not work). */
object SpokenTurnText {
    fun notice(outcome: SpokenTurnOutcome): String? = when (outcome) {
        SpokenTurnOutcome.Off -> tr("La orden hablada está apagada: enciéndela en Ajustes.")
        SpokenTurnOutcome.NoToken -> tr("Falta la clave de voz: escríbela en Ajustes.")
        SpokenTurnOutcome.PermissionDenied -> tr("Sin permiso para el micrófono no puedo escucharte. Puedes escribir la orden.")
        SpokenTurnOutcome.NothingHeard -> tr("No he oído nada. Prueba otra vez.")
        SpokenTurnOutcome.Malformed -> tr("La respuesta del servidor no es válida: no la muestro.")
        is SpokenTurnOutcome.Failed -> if (outcome.code == "mobile_voice_disabled") tr("El servidor no permite la voz desde el móvil: un administrador debe activarla en Studio.") else if (outcome.code == "unauthorized") tr("El servidor no acepta la clave de voz.") else VoiceParser.errorText(outcome.code?.let { "{\"error\":\"$it\"}" })
        is SpokenTurnOutcome.Answered -> null
    }
}
