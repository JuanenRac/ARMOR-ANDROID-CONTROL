// ARMOR-ANDROID-CONTROL - the watch for alarms starts again when the phone does.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * After a restart of the phone the watch for alarms was dead until somebody opened the app, even though the person had switched it on. This starts it again, but only when it was
 * on and the app still holds a session for the server (without one it would only say, at once, that the session expired).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val preferences = context.getSharedPreferences("armor-control", Context.MODE_PRIVATE)
        if (!preferences.getBoolean("watch", false)) return
        val origin = preferences.getString("origin", "").orEmpty()
        if (ServerEndpoint.parse(origin) == null) return
        if (SessionVault(PreferencesStore(context.getSharedPreferences("armor-session", Context.MODE_PRIVATE)), KeystoreBox()).load(origin) == null) return
        runCatching { AlarmWatcherService.start(context) }
    }
}
