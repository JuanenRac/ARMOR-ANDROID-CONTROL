// ARMOR-ANDROID-CONTROL - the action "Confirm" of a notice that knows its alarm: it acknowledges the alarm with the session the phone keeps, without opening the app.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ACKNOWLEDGE) return
        val alarm = intent.getStringExtra(EXTRA_ALARM).orEmpty()
        val noticeId = intent.getIntExtra(EXTRA_NOTICE, 0)
        if (!validAlarmId(alarm)) return
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try { if (acknowledge(app, alarm)) app.getSystemService(NotificationManager::class.java).cancel(noticeId) } catch (_: Exception) { /* the notice stays: the person can still open the app */ } finally { pending.finish() }
        }.start()
    }

    companion object {
        const val ACTION_ACKNOWLEDGE = "es.electrohobby3d.armor.ACKNOWLEDGE_ALARM"
        private const val EXTRA_ALARM = "alarm_id"
        private const val EXTRA_NOTICE = "notice_id"

        /** An id the server gave: letters, digits and dashes, short. Anything else is not acted on. */
        fun validAlarmId(id: String): Boolean = id.isNotEmpty() && id.length <= 64 && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }

        fun acknowledgeIntent(context: Context, alarm: String, noticeId: Int): PendingIntent {
            val intent = Intent(context, AlarmActionReceiver::class.java).setAction(ACTION_ACKNOWLEDGE).putExtra(EXTRA_ALARM, alarm).putExtra(EXTRA_NOTICE, noticeId)
            return PendingIntent.getBroadcast(context, noticeId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        /** Acknowledges the alarm with the session the phone keeps; false when there is no address, no session that can be read now, or the server refuses. */
        private fun acknowledge(context: Context, alarm: String): Boolean {
            val preferences = context.getSharedPreferences("armor-control", Context.MODE_PRIVATE)
            val origin = preferences.getString("origin", "").orEmpty()
            if (ServerEndpoint.parse(origin) == null) return false
            TrustPins.install(PreferencesStore(preferences))
            val vault = KeyBoxes.sessionVault(context)
            if (!vault.has() || !KeyBoxes.sessionBox(context).usable()) return false
            val client = SharedFeed.api
            val saved = vault.load(origin) ?: return false
            client.restoreSessionCookie(origin, saved.name, saved.value)
            client.acknowledgeAlarm(origin, alarm)
            return true
        }
    }
}
