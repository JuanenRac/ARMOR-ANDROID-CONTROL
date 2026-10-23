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
import es.electrohobby3d.armor.network.ArmorApi

object AlarmNotifier {
    const val ALARM_CHANNEL = "armor_alarm"
    const val CRITICAL_CHANNEL = "armor_alarm_critical"
    const val WARNING_CHANNEL = "armor_alarm_warning"
    private const val SERVER_CRITICAL_CHANNEL = "armor_critical"
    private const val SERVER_WARNING_CHANNEL = "armor_warning"
    const val WATCH_CHANNEL = "armor_watch"
    private const val SPEAK = "speak_alarms"
    private const val PREFERENCES = "armor-control"
    private const val LAST_EVENT = "last_event|"

    @Volatile private var tracker: AlarmTracker? = null
    @Volatile private var announced: AnnouncedIds? = null

    /** The ids of the events already announced, shared by the open app, the watch and the push messages of this process. */
    fun announced(context: Context): AnnouncedIds = announced ?: synchronized(this) {
        announced ?: run {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            AnnouncedIds({ key -> preferences.getString(key, null) }, { key, value -> preferences.edit().putString(key, value).apply() }).also { announced = it }
        }
    }

    /** One tracker per process, so the open app and the background service never announce an event twice. */
    fun tracker(context: Context): AlarmTracker = tracker ?: synchronized(this) {
        tracker ?: run {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            AlarmTracker({ origin -> preferences.getLong(LAST_EVENT + origin, -1L) }, { origin, id -> preferences.edit().putLong(LAST_EVENT + origin, id).apply() }, announced = announced(context)).also { tracker = it }
        }
    }

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ALARM_CHANNEL, tr("Alarmas de A.R.M.O.R."), NotificationManager.IMPORTANCE_HIGH).apply { description = tr("Alertas altas, cámaras sin respuesta y nodos caídos") })
        // One channel per severity, so each can have its own sound, vibration and priority in the phone's own settings (a critical one uses the alarm sound and the alarm volume).
        // The channels of the server's push messages: if an older server names its own ("armor_critical", "armor_warning") they exist here with the same sound, so a critical alarm that
        // arrives with the app closed still sounds like an alarm.
        for (id in listOf(CRITICAL_CHANNEL, SERVER_CRITICAL_CHANNEL)) manager.createNotificationChannel(NotificationChannel(id, tr("Alarmas críticas"), NotificationManager.IMPORTANCE_HIGH).apply {
            description = tr("Humo, gas, agua, pánico y lo más grave: suena como una alarma.")
            setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM), android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true); vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 900)
            setBypassDnd(true)
        })
        for (id in listOf(WARNING_CHANNEL, SERVER_WARNING_CHANNEL)) manager.createNotificationChannel(NotificationChannel(id, tr("Avisos"), NotificationManager.IMPORTANCE_DEFAULT).apply { description = tr("Avisos menos graves: batería baja, un dispositivo sin respuesta.") })
        manager.createNotificationChannel(NotificationChannel(WATCH_CHANNEL, tr("Vigilancia en segundo plano"), NotificationManager.IMPORTANCE_LOW).apply { description = tr("Aviso permanente mientras la vigilancia está activa") })
    }

    /** From Android 13 a notification needs the user's permission; without it nothing is shown. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, ArmorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun post(context: Context, notice: AlarmNotice) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val channel = when (notice.severity) { "critical" -> CRITICAL_CHANNEL; "warning" -> WARNING_CHANNEL; else -> ALARM_CHANNEL }
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notice.title).setContentText(notice.text)
            .setCategory(Notification.CATEGORY_ALARM).setAutoCancel(true).setContentIntent(openApp(context))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTICE_BASE + (notice.id % 10_000).toInt(), notification)
        if (notice.severity != "warning" && speaksAlarms(context)) AlarmSpeaker.say(context, "${notice.title}. ${notice.text}")
    }

    /** Whether the phone says an alarm aloud as well as showing it (off by default). */
    fun speaksAlarms(context: Context): Boolean = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(SPEAK, false)
    fun setSpeaksAlarms(context: Context, on: Boolean) { context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(SPEAK, on).apply() }

    fun watchNotification(context: Context, text: String): Notification {
        ensureChannels(context)
        return Notification.Builder(context, WATCH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(tr("A.R.M.O.R. vigilando")).setContentText(text)
            .setOngoing(true).setContentIntent(openApp(context))
            .build()
    }

    /** One polling pass: fetch the newest events and announce the ones that matter. Returns the number announced. */
    fun poll(context: Context, client: ArmorApi, origin: String): Int {
        if (SharedFeed.appIsReading()) return 0   // the app on screen reads the server and announces; asking the same again from the background would only double the requests
        val tracker = tracker(context)
        val window = client.history(origin, limit = 50)
        val events = freshEvents(tracker.last(origin), window, { after -> client.history(origin, limit = 200, after = after) })
        val armed = client.status(origin).mode == "armed"
        val notices = tracker.claim(origin, events, armed)
        notices.forEach { post(context, it) }
        return notices.size
    }

    const val WATCH_ID = 4201
    private const val NOTICE_BASE = 5000
}
