// ARMOR-ANDROID-CONTROL - how the phone says an alarm aloud: the voice (woman, man), the age, the style and the speed, the same choices as the voice of Studio.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context

/** A voice of the phone's speech engine as far as the choice needs it: its name and the language it speaks. */
data class VoiceCandidate(val name: String, val language: String)

/**
 * The voice of the alarms. [gender] is "any", "female" or "male"; [age] is "child", "adult" or "senior"; [style] is "plain", "hero" or "dark"; [speed] is the percent of the normal pace
 * (50 to 200); [warnings] says whether the notices of low severity are spoken too (the critical and the high ones are, once the phone is set to speak).
 * Android does not say the gender of a voice, so it is read from the name when the name says it ("...#female_1") and, when no voice does, a higher or lower pitch stands for it.
 */
data class VoiceSettings(val gender: String = "any", val age: String = "adult", val style: String = "plain", val speed: Int = 100, val warnings: Boolean = false) {
    fun speaks(severity: String): Boolean = severity != "warning" || warnings

    /** The pitch: the age's times the style's (and the gender's, only when no voice of that gender was found). */
    fun pitch(genderFound: Boolean): Float {
        val byGender = if (genderFound) 1f else when (gender) { "female" -> 1.12f; "male" -> 0.82f; else -> 1f }
        return ((AGE_PITCH[age] ?: 1f) * (STYLE_PITCH[style] ?: 1f) * byGender).coerceIn(0.4f, 2f)
    }

    /** The pace: the age's times the style's times the speed that was set. */
    fun rate(): Float = ((AGE_RATE[age] ?: 1f) * (STYLE_RATE[style] ?: 1f) * (speed.coerceIn(MIN_SPEED, MAX_SPEED) / 100f)).coerceIn(0.3f, 3f)

    /** The voice of the language whose name says the wanted gender, else the first of the language, else none (the engine keeps its own). */
    fun pick(voices: List<VoiceCandidate>, language: String): Pair<VoiceCandidate?, Boolean> {
        val base = language.lowercase().split('-', '_')[0]
        val ofLanguage = voices.filter { it.language.lowercase().split('-', '_')[0] == base }
        if (ofLanguage.isEmpty()) return null to false
        if (gender == "any") return (ofLanguage.firstOrNull { it.language.lowercase().replace('_', '-') == language.lowercase() } ?: ofLanguage.first()) to false
        val named = ofLanguage.firstOrNull { hint(it.name) == gender }
        return if (named != null) named to true else ofLanguage.first() to false
    }

    fun encode(): String = listOf(gender, age, style, speed.toString(), warnings.toString()).joinToString("|")

    companion object {
        val GENDERS = listOf("any", "female", "male")
        val AGES = listOf("child", "adult", "senior")
        val STYLES = listOf("plain", "hero", "dark")
        const val MIN_SPEED = 50
        const val MAX_SPEED = 200
        /** What the age and the style do to the pitch and the pace (the same numbers as Studio). */
        private val AGE_PITCH = mapOf("child" to 1.6f, "adult" to 1f, "senior" to 0.7f)
        private val AGE_RATE = mapOf("child" to 1.1f, "adult" to 1f, "senior" to 0.88f)
        private val STYLE_PITCH = mapOf("plain" to 1f, "hero" to 1.12f, "dark" to 0.55f)
        private val STYLE_RATE = mapOf("plain" to 1f, "hero" to 1.22f, "dark" to 0.82f)

        /** "female" or "male" when the name of a voice says it, else "". */
        fun hint(name: String): String {
            val lower = name.lowercase()
            return when {
                "female" in lower || "woman" in lower -> "female"
                "male" in lower || Regex("""(^|[^a-z])man([^a-z]|$)""").containsMatchIn(lower) -> "male"
                else -> ""
            }
        }

        /** Reads what [encode] wrote; anything else (nothing, a damaged text, a value that is not known) is the way it was before. */
        fun decode(text: String?): VoiceSettings {
            val parts = text?.split("|") ?: return VoiceSettings()
            if (parts.size != 5) return VoiceSettings()
            return VoiceSettings(
                gender = parts[0].takeIf { it in GENDERS } ?: "any",
                age = parts[1].takeIf { it in AGES } ?: "adult",
                style = parts[2].takeIf { it in STYLES } ?: "plain",
                speed = (parts[3].toIntOrNull() ?: 100).coerceIn(MIN_SPEED, MAX_SPEED),
                warnings = parts[4] == "true",
            )
        }
    }
}

/** The voice kept in the preferences of the app, read by the open app, the watch and the push messages alike. */
object VoicePreferences {
    private const val PREFERENCES = "armor-control"
    private const val KEY = "voice_settings"

    fun load(context: Context): VoiceSettings = VoiceSettings.decode(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, null))
    fun save(context: Context, settings: VoiceSettings) { context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY, settings.encode()).apply() }
}
