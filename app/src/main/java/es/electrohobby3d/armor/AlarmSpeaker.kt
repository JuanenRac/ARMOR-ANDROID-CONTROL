// ARMOR-ANDROID-CONTROL - says an alarm aloud with the phone's own voice (Android's text to speech), in the language of the app, when the person asked for it, with the voice they chose.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object AlarmSpeaker {
    @Volatile private var engine: TextToSpeech? = null
    @Volatile private var ready = false
    private val waiting = ArrayDeque<Pair<String, VoiceSettings>>()

    /** Queue a sentence; the voice engine is started the first time and the queue is spoken once it is ready. The voice is the one of the settings of the person unless one is given (the test of the voice). */
    fun say(context: Context, text: String, voice: VoiceSettings = VoicePreferences.load(context)) {
        synchronized(this) {
            if (engine == null) {
                engine = TextToSpeech(context.applicationContext) { status ->
                    synchronized(this) {
                        ready = status == TextToSpeech.SUCCESS
                        if (ready) {
                            while (waiting.isNotEmpty()) { val (sentence, wanted) = waiting.removeFirst(); speak(sentence, wanted) }
                        } else { waiting.clear(); engine?.shutdown(); engine = null }   // the next alarm tries again (the voice engine may have been missing or updating)
                    }
                }
            }
            if (ready) speak(text.take(240), voice) else if (waiting.size < 5) waiting.addLast(text.take(240) to voice)
        }
    }

    /** Stops what is being said (the person tests another voice). */
    fun stop() { synchronized(this) { if (ready) engine?.stop() } }

    private fun speak(text: String, voice: VoiceSettings) {
        val tts = engine ?: return
        val language = Locale.forLanguageTag(Tr.language)
        tts.language = language
        val candidates = runCatching { tts.voices.orEmpty().map { VoiceCandidate(it.name, it.locale.toLanguageTag()) } }.getOrDefault(emptyList())
        val (picked, genderFound) = voice.pick(candidates, Tr.language)
        if (picked != null) runCatching { tts.voices.firstOrNull { it.name == picked.name }?.let { tts.voice = it } }
        tts.setPitch(voice.pitch(genderFound))
        tts.setSpeechRate(voice.rate())
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "armor-alarm")
    }
}
