// ARMOR-ANDROID-CONTROL - says an alarm aloud with the phone's own voice (Android's text to speech), in Spanish, when the person asked for it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object AlarmSpeaker {
    @Volatile private var engine: TextToSpeech? = null
    @Volatile private var ready = false
    private val waiting = ArrayDeque<String>()

    /** Queue a sentence; the voice engine is started the first time and the queue is spoken once it is ready. */
    fun say(context: Context, text: String) {
        synchronized(this) {
            if (engine == null) {
                engine = TextToSpeech(context.applicationContext) { status ->
                    synchronized(this) {
                        ready = status == TextToSpeech.SUCCESS
                        if (ready) {
                            engine?.language = Locale("es", "ES")
                            while (waiting.isNotEmpty()) engine?.speak(waiting.removeFirst(), TextToSpeech.QUEUE_ADD, null, "armor-alarm")
                        } else waiting.clear()
                    }
                }
            }
            if (ready) engine?.speak(text.take(240), TextToSpeech.QUEUE_ADD, null, "armor-alarm") else if (waiting.size < 5) waiting.addLast(text.take(240))
        }
    }
}
