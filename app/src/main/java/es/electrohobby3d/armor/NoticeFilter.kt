// ARMOR-ANDROID-CONTROL - which notices the person wants to be told about, and how.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context

/** What the phone does with a notice: show it as usual, show it without sound or vibration, or not show it at all (the history keeps everything either way). */
enum class NoticeDecision { SHOW, SILENT, HIDE }

/** The kinds of notice the person can switch off, in the order of the settings. A notice of the app itself (the server does not answer, the watch stopped) is of kind [SYSTEM]. */
object NoticeKinds {
    const val SYSTEM = "system"
    val CHOICES = listOf("alert", "camera", "node", "alarm.device", "alarm.solar", "alarm.electrical", "alarm.network", "alarm.alarm", "alarm.other")

    /** The kind of an alarm by where it comes from. */
    fun ofAlarm(sourceType: String?): String = when (sourceType) {
        "device", "solar", "electrical", "network", "alarm" -> "alarm.$sourceType"
        else -> "alarm.other"
    }
}

/**
 * The choices of the person, which come before the sound: [muted] shows nothing at all; [silent] shows everything without sound or vibration and does not say it aloud; the rest takes some
 * notices away by severity or by kind. A critical notice is never taken away by severity or by kind (smoke, gas, water, panic: the one that matters most), only by [muted], which asks for it
 * on its own; and it is the only one that [silent] leaves with its sound.
 */
data class NoticeSettings(
    val muted: Boolean = false,
    val silent: Boolean = false,
    val mutedSeverities: Set<String> = emptySet(),
    val mutedKinds: Set<String> = emptySet(),
) {
    fun decide(notice: AlarmNotice): NoticeDecision {
        if (muted) return NoticeDecision.HIDE
        val critical = notice.severity == "critical"
        if (!critical && (notice.severity in mutedSeverities || notice.kind in mutedKinds)) return NoticeDecision.HIDE
        if (silent && !critical) return NoticeDecision.SILENT
        return NoticeDecision.SHOW
    }

    fun withSeverity(severity: String, off: Boolean) = copy(mutedSeverities = if (off) mutedSeverities + severity else mutedSeverities - severity)
    fun withKind(kind: String, off: Boolean) = copy(mutedKinds = if (off) mutedKinds + kind else mutedKinds - kind)

    /** One line per value, so that the store needs no more than text. */
    fun encode(): String = listOf(muted.toString(), silent.toString(), mutedSeverities.sorted().joinToString(","), mutedKinds.sorted().joinToString(",")).joinToString("|")

    companion object {
        val SEVERITIES = listOf("warning", "high")

        /** Reads what [encode] wrote; anything else (nothing, a damaged text, a value that is not known) is the way it was before: everything shows. */
        fun decode(text: String?): NoticeSettings {
            val parts = text?.split("|") ?: return NoticeSettings()
            if (parts.size != 4) return NoticeSettings()
            return NoticeSettings(
                muted = parts[0] == "true", silent = parts[1] == "true",
                mutedSeverities = parts[2].split(",").filter { it in SEVERITIES }.toSet(),
                mutedKinds = parts[3].split(",").filter { it in NoticeKinds.CHOICES }.toSet(),
            )
        }
    }
}

/** The choices kept in the preferences of the app, read by the open app, the watch and the push messages alike. */
object NoticePreferences {
    private const val PREFERENCES = "armor-control"
    private const val KEY = "notice_settings"

    fun load(context: Context): NoticeSettings = NoticeSettings.decode(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, null))
    fun save(context: Context, settings: NoticeSettings) { context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY, settings.encode()).apply() }
}
