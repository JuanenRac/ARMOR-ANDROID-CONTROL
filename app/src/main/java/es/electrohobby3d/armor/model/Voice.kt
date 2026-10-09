// ARMOR-ANDROID-CONTROL - written and spoken commands: what the server answers, and what the Assistant screen shows.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

/** What the server answered to one command: the voice gateway's verdict and, when it was accepted, what the server did. */
data class VoiceReply(
    val accepted: Boolean,
    /** The server carried the command out (it armed, disarmed, read the state or silenced the alarms). */
    val executed: Boolean,
    /** `arm`, `disarm`, `status` or `silence`; null when the phrase is not one of them. */
    val intent: String?,
    /** `accepted`, `confirmation-needed`, `confirmation-refused` or `not-understood`. */
    val outcome: String,
    /** What to say back, in the language that was asked for. */
    val speech: String,
    /** Present when the command changes the security state and must be confirmed: the phrase is sent again with it. */
    val confirmationToken: String?,
)

/** A command waiting for its second turn: the same words, sent again with the token the server gave. */
data class PendingVoice(val text: String, val token: String)

/** One line of the conversation. */
data class VoiceMessage(val fromPerson: Boolean, val text: String, val pending: PendingVoice? = null, val problem: Boolean = false)

data class VoiceUiState(val messages: List<VoiceMessage> = emptyList(), val busy: Boolean = false, val available: Boolean? = null)

object VoiceParser {
    fun parse(root: JSONObject): VoiceReply = VoiceReply(
        accepted = root.optBoolean("accepted"), executed = root.optBoolean("executed"),
        intent = if (root.isNull("intent")) null else root.optString("intent").ifBlank { null },
        outcome = root.optString("outcome"), speech = root.optString("speech"),
        confirmationToken = root.optString("confirmation_token").ifBlank { null },
    )

    /** The code an error answer carries (`{"error":"voice_unavailable"}`), taken out of the text of the exception the client raises. */
    fun errorCode(message: String?): String? = message?.let { Regex("\"error\"\\s*:\\s*\"([a-z_]+)\"").find(it)?.groupValues?.get(1) }

    /** What to tell the person when the server could not even take the command. */
    fun errorText(message: String?): String = when (errorCode(message)) {
        "voice_unavailable" -> "Este sistema no tiene instalado el asistente de voz."
        "voice_not_answering" -> "El asistente de voz no responde ahora mismo."
        "voice_refused_the_token" -> "El servidor y el asistente de voz no se ponen de acuerdo (la clave no coincide)."
        "text_too_long" -> "La orden es demasiado larga."
        "no_text" -> "Escribe o di una orden."
        else -> if (message?.contains("HTTP 401") == true) "La sesión ha caducado: abre la app e inicia sesión." else "No se pudo enviar la orden al servidor."
    }

    /** The sentence the person sees, with a note when the phrase was understood but not carried out. */
    fun lineFor(reply: VoiceReply): String = when {
        reply.outcome == "confirmation-needed" -> "${reply.speech}. ${confirmationQuestion(reply.intent)}"
        reply.outcome == "not-understood" -> "No lo he entendido. Di «ayuda» para saber qué puedo hacer."
        else -> reply.speech
    }

    private fun confirmationQuestion(intent: String?) = when (intent) {
        "arm" -> "¿Armo el sistema?"
        "disarm" -> "¿Desarmo el sistema?"
        else -> "¿Lo hago?"
    }
}
