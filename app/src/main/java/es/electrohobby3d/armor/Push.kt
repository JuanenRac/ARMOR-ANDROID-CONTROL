// ARMOR-ANDROID-CONTROL - push messages from the installation's own Firebase project.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

/**
 * Push is on only when the build was made with the installation's own google-services.json (see docs/PUSH_NOTIFICATIONS.md): the build then holds the four values Firebase needs, and
 * the app starts Firebase itself from them. Without them [available] is false and nothing here does anything. The server sends the line of an alarm as a notification message on the
 * channel of its severity, which Android shows by itself whenever the app is not on screen; the watch of the alarms (if the person turned it on) may say the same alarm too, and the server's older channel
 * names are created here as well so that a critical message sounds like an alarm.
 */
object Push {
    private const val PREFERENCES = "armor-control"
    private const val TOKEN = "push_token"

    val available: Boolean get() = BuildConfig.FCM_APP_ID.isNotBlank() && BuildConfig.FCM_API_KEY.isNotBlank() && BuildConfig.FCM_PROJECT_ID.isNotBlank()

    /** Starts Firebase once. False when this build has no Firebase file. */
    fun start(context: Context): Boolean {
        if (!available) return false
        val app = context.applicationContext
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app, FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FCM_APP_ID).setApiKey(BuildConfig.FCM_API_KEY).setProjectId(BuildConfig.FCM_PROJECT_ID).setGcmSenderId(BuildConfig.FCM_SENDER_ID).build())
        }
        return true
    }

    /** The phone's current token, asked from Firebase (and kept), handed to [whenKnown]. Nothing happens when there is no push or Firebase refuses. */
    fun token(context: Context, whenKnown: (String) -> Unit) {
        if (!start(context)) return
        runCatching { FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> if (!token.isNullOrBlank()) { keep(context, token); whenKnown(token) } } }
    }

    /** The phone signs out: its token is dropped at Firebase and here, so the server's messages to it find no one (and the server forgets it). */
    fun forget(context: Context) {
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().remove(TOKEN).apply()
        if (available && start(context)) runCatching { FirebaseMessaging.getInstance().deleteToken() }
    }

    fun stored(context: Context): String? = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(TOKEN, null)
    fun keep(context: Context, token: String) { context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(TOKEN, token).apply() }
}

/** Receives Firebase's calls. A new token is kept and told to the server the next time the person is signed in; a message needs nothing (Android shows it, see [Push]). */
class PushService : FirebaseMessagingService() {
    override fun onCreate() {
        Push.start(this)
        TrustPins.install(PreferencesStore(getSharedPreferences("armor-control", Context.MODE_PRIVATE)))
        super.onCreate()
        AlarmNotifier.ensureChannels(this)   // the channels the server names must exist before a message arrives
    }

    override fun onNewToken(token: String) { Push.keep(this, token) }

    /**
     * A message of data only (the server tells which event happened; this phone says it in its own language) or with a notification part that the system shows by itself when the app is out of
     * sight. The id of the event is what keeps the watch, the open app and the push from saying one event twice. The server this push comes from is the one the app is signed in to.
     */
    override fun onMessageReceived(message: com.google.firebase.messaging.RemoteMessage) {
        Tr.init(this)
        val origin = getSharedPreferences("armor-control", Context.MODE_PRIVATE).getString("origin", "").orEmpty()
        if (ServerEndpoint.parse(origin) == null) return
        val announced = AlarmNotifier.announced(this)
        if (message.notification != null) { PushEvents.noteShown(message.data, origin, announced); return }
        PushEvents.notice(message.data, origin, announced)?.let { AlarmNotifier.post(this, it) }
    }
}
