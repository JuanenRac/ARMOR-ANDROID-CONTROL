// ARMOR-ANDROID-CONTROL - how often the background watch asks the server: slowly while the push messages work, quickly when they do not.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context

/** What the pace depends on, read from the phone's preferences. Times are in milliseconds since 1970; 0 means "never". */
data class PushHealth(
    /** This build has the installation's Firebase file (see [Push.available]) and the phone holds a push token. */
    val available: Boolean,
    /** The server took this phone's token (for the server in use), at this time. */
    val registeredAt: Long,
    /** The last time the watch announced an event that the server's push should have announced first, at this time. */
    val lastMissAt: Long,
)

/**
 * The pace of the watch. Asking the server every few seconds is what makes the battery and the mobile data go, and it is only needed when nothing else tells the phone about an alarm.
 * While the push works the watch only checks now and then ([slowMs], the person's choice, one minute to ten) as a safety net; the push is trusted when this build has it, the server took the
 * token, and the watch has not caught the server in an event that the push failed to bring in the last [MISS_MEMORY_MS]. Otherwise it asks every [FAST_MS], as it always did.
 */
object WatchPace {
    const val FAST_MS = 10_000L
    const val SLOW_DEFAULT_S = 120
    /** The choices offered, in seconds: 10 keeps the fast pace even when the push works. */
    val SLOW_CHOICES_S: List<Int> = listOf(10, 60, 120, 300, 600)
    /** An event the watch announces is a miss of the push only when it is older than this (the push arrives in seconds). */
    const val MISS_AFTER_MS = 90_000L
    /** After a miss the push is not trusted for this long. */
    const val MISS_MEMORY_MS = 6 * 3_600_000L

    private const val PREFERENCES = "armor-control"
    private const val KEY_SLOW = "watch_slow_s"
    private const val KEY_REGISTERED = "push_registered_at"
    private const val KEY_REGISTERED_FOR = "push_registered_for"
    private const val KEY_MISS = "push_miss_at"

    /** The slow interval for a stored choice: one of the offered values, the default for anything else. */
    fun slowSeconds(stored: Int): Int = if (stored in SLOW_CHOICES_S) stored else SLOW_DEFAULT_S

    /** Whether the push can be trusted to bring the alarms by itself at [now]. */
    fun pushWorking(health: PushHealth, now: Long): Boolean =
        health.available && health.registeredAt > 0 && (health.lastMissAt <= 0 || now - health.lastMissAt >= MISS_MEMORY_MS)

    /** How long to wait before the next look at the server when the last one went well. */
    fun intervalMs(health: PushHealth, slowSeconds: Int, now: Long): Long =
        if (pushWorking(health, now)) slowSeconds(slowSeconds) * 1000L else FAST_MS

    /** An event of [ageMs] that the watch is announcing for the first time: the push should have brought it already. */
    fun isMiss(health: PushHealth, ageMs: Long, now: Long): Boolean = pushWorking(health, now) && ageMs > MISS_AFTER_MS

    // ---- what is kept between runs ----------------------------------------------------------------------------------------------------------------

    private fun preferences(context: Context) = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun slowSeconds(context: Context): Int = slowSeconds(preferences(context).getInt(KEY_SLOW, SLOW_DEFAULT_S))
    fun setSlowSeconds(context: Context, seconds: Int) { preferences(context).edit().putInt(KEY_SLOW, slowSeconds(seconds)).apply() }

    /** The health of the push for the server the app is signed in to: a token the server took for another server counts for nothing. */
    fun health(context: Context, origin: String, now: Long = System.currentTimeMillis()): PushHealth {
        val saved = preferences(context)
        val registered = if (saved.getString(KEY_REGISTERED_FOR, null) == origin) saved.getLong(KEY_REGISTERED, 0L) else 0L
        return PushHealth(available = Push.available && Push.stored(context) != null, registeredAt = registered, lastMissAt = saved.getLong(KEY_MISS, 0L).takeIf { it <= now } ?: 0L)
    }

    /** The server took this phone's token. */
    fun markRegistered(context: Context, origin: String, now: Long = System.currentTimeMillis()) {
        preferences(context).edit().putLong(KEY_REGISTERED, now).putString(KEY_REGISTERED_FOR, origin).apply()
    }

    /** The watch caught an event the push did not bring. */
    fun markMiss(context: Context, now: Long = System.currentTimeMillis()) { preferences(context).edit().putLong(KEY_MISS, now).apply() }

    /** Milliseconds until the next look at the server after a good one. */
    fun nextIntervalMs(context: Context, origin: String): Long {
        val now = System.currentTimeMillis()
        return intervalMs(health(context, origin, now), slowSeconds(context), now)
    }
}
