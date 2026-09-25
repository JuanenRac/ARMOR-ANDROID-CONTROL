// ARMOR-ANDROID-CONTROL - alarm notifications and the shared event tracker.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import es.electrohobby3d.armor.network.ArmorApiClient

object AlarmNotifier {
    const val ALARM_CHANNEL = "armor_alarm"
    const val WATCH_CHANNEL = "armor_watch"
    private const val PREFERENCES = "armor-control"
    private const val LAST_EVENT = "last_event"

    @Volatile private var tracker: AlarmTracker? = null

    /** One tracker per process, so the open app and the background service never announce an event twice. */
    fun tracker(context: Context): AlarmTracker = tracker ?: synchronized(this) {
        tracker ?: run {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            AlarmTracker({ preferences.getLong(LAST_EVENT, -1L) }, { preferences.edit().putLong(LAST_EVENT, it).apply() }).also { tracker = it }
        }
    }

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ALARM_CHANNEL, "Alarmas de A.R.M.O.R.", NotificationManager.IMPORTANCE_HIGH).apply { description = "Alertas altas, cámaras sin respuesta y nodos caídos" })
        manager.createNotificationChannel(NotificationChannel(WATCH_CHANNEL, "Vigilancia en segundo plano", NotificationManager.IMPORTANCE_LOW).apply { description = "Aviso permanente mientras la vigilancia está activa" })
    }

    /** From Android 13 a notification needs the user's permission; without it nothing is shown. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, ArmorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun post(context: Context, notice: AlarmNotice) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val notification = Notification.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notice.title).setContentText(notice.text)
            .setCategory(Notification.CATEGORY_ALARM).setAutoCancel(true).setContentIntent(openApp(context))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTICE_BASE + (notice.id % 10_000).toInt(), notification)
    }

    fun watchNotification(context: Context, text: String): Notification {
        ensureChannels(context)
        return Notification.Builder(context, WATCH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("A.R.M.O.R. vigilando").setContentText(text)
            .setOngoing(true).setContentIntent(openApp(context))
            .build()
    }

    /** One polling pass: fetch the newest events and announce the ones that matter. Returns the number announced. */
    fun poll(context: Context, client: ArmorApiClient, origin: String): Int {
        val events = client.history(origin, limit = 50)
        val armed = client.status(origin).mode == "armed"
        val notices = tracker(context).claim(events, armed)
        notices.forEach { post(context, it) }
        return notices.size
    }

    const val WATCH_ID = 4201
    private const val NOTICE_BASE = 5000
}
