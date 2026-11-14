// ARMOR-ANDROID-CONTROL - the reminder on leaving home: when the phone leaves the place the person saved as home and the alarm is not armed, a notice says so. Off until the person turns it on.
// Everything stays on the phone: the place is kept in the app's own preferences and is never sent to the server; the only thing asked of the server is whether the system is armed.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.content.SharedPreferences

/** The numbers of the reminder, in one place. */
object PresenceConfig {
    /** The radius of home, in metres: the phone has left when it is farther than this from the saved place. */
    const val RADIUS_M = 150f
    /** After a reminder, the next one waits this long (a phone at the edge of the radius goes in and out). */
    const val COOLDOWN_MS = 30 * 60_000L
    const val GEOFENCE_ID = "armor-home"
    /** Where the reminder shows in the list of notices; one place, so a second reminder replaces the first. */
    const val NOTICE_ID = 9_100L
}

/** What the reminder says. */
enum class Reminder { ARM_NOW, CHECK }

object PresenceDecision {
    /**
     * The phone left home. [mode] is what the server says ("armed", "disarmed"), or null when it could not be read. Armed: nothing to say. Disarmed: remind to arm. Unknown: ask the person to check,
     * because a reminder that stays silent when the server cannot be reached is the case that matters. Inside the quiet time after a reminder: nothing.
     */
    fun onLeft(mode: String?, lastReminderMs: Long, nowMs: Long): Reminder? {
        if (lastReminderMs > 0 && nowMs - lastReminderMs in 0 until PresenceConfig.COOLDOWN_MS) return null
        return when (mode) { "armed" -> null; "disarmed" -> Reminder.ARM_NOW; else -> Reminder.CHECK }
    }
}

/** The place saved as home. */
data class HomePlace(val lat: Double, val lon: Double) {
    val valid: Boolean get() = lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)
}

/** What the person chose, kept in the preferences of the app. Off by default. */
class PresenceSettings(private val preferences: SharedPreferences) {
    var enabled: Boolean
        get() = preferences.getBoolean(ON, false)
        set(value) { preferences.edit().putBoolean(ON, value).apply() }

    var home: HomePlace?
        get() = if (preferences.contains(LAT) && preferences.contains(LON)) HomePlace(Double.fromBits(preferences.getLong(LAT, 0L)), Double.fromBits(preferences.getLong(LON, 0L))).takeIf { it.valid } else null
        set(value) {
            val edit = preferences.edit()
            if (value == null || !value.valid) edit.remove(LAT).remove(LON) else edit.putLong(LAT, value.lat.toBits()).putLong(LON, value.lon.toBits())
            edit.apply()
        }

    var lastReminderMs: Long
        get() = preferences.getLong(LAST, 0L)
        set(value) { preferences.edit().putLong(LAST, value).apply() }

    companion object {
        private const val ON = "presence_on"
        private const val LAT = "presence_lat"
        private const val LON = "presence_lon"
        private const val LAST = "presence_last"
        fun of(context: Context) = PresenceSettings(context.applicationContext.getSharedPreferences("armor-control", Context.MODE_PRIVATE))
    }
}
