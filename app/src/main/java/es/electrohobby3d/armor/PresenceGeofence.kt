// ARMOR-ANDROID-CONTROL - the circle around home that the phone watches for the reminder on leaving (Google Play services geofencing), and the receiver that hears the phone leave it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object PresenceGeofence {
    private fun intent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, 77, Intent(context, PresenceReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

    /** The two permissions the reminder needs: the place, and (from Android 10) the place while the app is closed. */
    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < 29 || context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)

    /** Starts watching the circle around [home]; only for the exit of it. Does nothing without the permissions. */
    @SuppressLint("MissingPermission")
    fun start(context: Context, home: HomePlace) {
        if (!home.valid || !hasPermission(context)) return
        val geofence = Geofence.Builder().setRequestId(PresenceConfig.GEOFENCE_ID)
            .setCircularRegion(home.lat, home.lon, PresenceConfig.RADIUS_M)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        val request = GeofencingRequest.Builder().setInitialTrigger(0).addGeofence(geofence).build()
        LocationServices.getGeofencingClient(context.applicationContext).addGeofences(request, intent(context.applicationContext))
    }

    fun stop(context: Context) { LocationServices.getGeofencingClient(context.applicationContext).removeGeofences(intent(context.applicationContext)) }

    /** After the phone restarts (geofences are forgotten) or the person changes something: watches again when the reminder is on and a place is saved. */
    fun restore(context: Context) {
        val settings = PresenceSettings.of(context)
        val home = settings.home
        if (settings.enabled && home != null) start(context, home) else stop(context)
    }
}

class PresenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return
        val app = context.applicationContext
        if (!PresenceSettings.of(app).enabled) return
        val pending = goAsync()
        Thread {
            try { remind(app) } catch (_: Exception) { /* nothing more to do from a receiver */ } finally { pending.finish() }
        }.start()
    }

    private fun remind(context: Context) {
        Tr.init(context)
        val settings = PresenceSettings.of(context)
        val now = System.currentTimeMillis()
        val kind = PresenceDecision.onLeft(readMode(context), settings.lastReminderMs, now) ?: return
        settings.lastReminderMs = now
        val notice = when (kind) {
            Reminder.ARM_NOW -> AlarmNotice(PresenceConfig.NOTICE_ID, tr("¿Armas la alarma?"), tr("Has salido de casa y el sistema está desarmado."), "warning", NoticeKinds.SYSTEM)
            Reminder.CHECK -> AlarmNotice(PresenceConfig.NOTICE_ID, tr("Comprueba la alarma"), tr("Has salido de casa y no he podido comprobar si la alarma está armada."), "warning", NoticeKinds.SYSTEM)
        }
        AlarmNotifier.post(context, notice)
    }

    /** What the server says about the mode ("armed", "disarmed"), or null when the session or the network does not allow reading it. */
    private fun readMode(context: Context): String? {
        val preferences = context.getSharedPreferences("armor-control", Context.MODE_PRIVATE)
        val origin = preferences.getString("origin", "").orEmpty()
        if (ServerEndpoint.parse(origin) == null) return null
        return try {
            TrustPins.install(PreferencesStore(preferences))
            val vault = KeyBoxes.sessionVault(context)
            if (!vault.has() || !KeyBoxes.sessionBox(context).usable()) return null
            val client = SharedFeed.api
            vault.load(origin)?.let { client.restoreSessionCookie(origin, it.name, it.value) }
            client.status(origin).mode
        } catch (_: Exception) { null }
    }
}
