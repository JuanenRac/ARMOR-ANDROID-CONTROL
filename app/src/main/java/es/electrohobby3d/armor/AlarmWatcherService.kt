// ARMOR-ANDROID-CONTROL - optional background watch for alarms.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import es.electrohobby3d.armor.network.ArmorApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls the server for events while the operator's session is valid and turns the ones that
 * matter into notifications. It never stores the password: when the session expires (or Android
 * ends a data-sync service after its time limit) it says so in a notification and stops, and the
 * operator signs in again from the app.
 */
class AlarmWatcherService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Tr.init(this)
        val savedOrigin = getSharedPreferences("armor-control", Context.MODE_PRIVATE).getString("origin", "").orEmpty()
        val notification = AlarmNotifier.watchNotification(this, trf("Comprobando eventos cada %s s", WatchPace.nextIntervalMs(this, savedOrigin) / 1000))
        // From Android 14 the watch is a "special use" service, which Android 15 does not stop after six hours as it does a data-sync one.
        when {
            Build.VERSION.SDK_INT >= 34 -> startForeground(AlarmNotifier.WATCH_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            Build.VERSION.SDK_INT >= 29 -> startForeground(AlarmNotifier.WATCH_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else -> startForeground(AlarmNotifier.WATCH_ID, notification)
        }
        if (job?.isActive != true) job = scope.launch { watch() }
        // If Android ends the process to get memory back, the watch comes back by itself (the session is read again from the vault).
        return START_STICKY
    }

    private suspend fun watch() {
        // The address is read again on every pass: the app moves it between the address of the house and the one for outside as the phone changes place.
        fun savedOrigin() = getSharedPreferences("armor-control", Context.MODE_PRIVATE).getString("origin", "").orEmpty()
        var origin = savedOrigin()
        if (ServerEndpoint.parse(origin) == null) { stopWith(tr("Configura el servidor en la app.")); return }
        TrustPins.install(PreferencesStore(getSharedPreferences("armor-control", Context.MODE_PRIVATE)))
        val client = SharedFeed.api
        // The phone may have closed the app's process: the watcher carries the session kept by the app.
        val vault = KeyBoxes.sessionVault(this)
        // With the key tied to the unlocking of the phone, the session can be read only after the person has unlocked it: until then the watch waits (and says so by hearing nothing).
        while (scope.isActive && vault.has() && !KeyBoxes.sessionBox(this).usable()) delay(WAIT_FOR_UNLOCK_MS)
        vault.load(origin)?.let { client.restoreSessionCookie(origin, it.name, it.value) }
        var failures = 0
        var firstFailureAt = 0L
        while (scope.isActive) {
            try {
                savedOrigin().takeIf { ServerEndpoint.parse(it) != null }?.let { origin = it }
                AlarmNotifier.poll(this, client, origin)
                failures = 0
                firstFailureAt = 0L
            } catch (error: ArmorApiException) {
                if (error.code == 401) { stopWith(tr("La sesión ha caducado: abre la app e inicia sesión.")); return }
                failures += 1
                if (failures == 1) firstFailureAt = System.currentTimeMillis()
            } catch (error: Exception) {
                failures += 1
                if (failures == 1) firstFailureAt = System.currentTimeMillis()
            }
            // A server that stays unreachable is itself worth knowing about, once.
            if (failures == UNREACHABLE_AFTER) AlarmNotifier.post(this, AlarmNotice(System.currentTimeMillis() % 1_000_000, tr("Servidor sin respuesta"), trf("ARMOR-SERVER no responde desde hace %s s", (System.currentTimeMillis() - firstFailureAt) / 1000)))
            delay(nextDelayMs(failures, origin))
        }
    }

    /**
     * The normal interval while all goes well: a slow one (the person's choice) while the push messages work, the fast one when they do not (see [WatchPace]). After failures it doubles
     * (up to a minute) with a little jitter, so a server that is down is not hammered.
     */
    private fun nextDelayMs(failures: Int, origin: String): Long {
        if (failures == 0) return WatchPace.nextIntervalMs(this, origin)
        val backoff = (WatchPace.FAST_MS shl failures.coerceAtMost(3)).coerceAtMost(MAX_BACKOFF_MS)
        return backoff + Random.nextLong(0, 1_000)
    }

    private fun stopWith(reason: String) {
        AlarmNotifier.post(this, AlarmNotice(System.currentTimeMillis() % 1_000_000, tr("Vigilancia detenida"), reason))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Android 15 ends a data-sync foreground service after a few hours; tell the operator instead of dying silently. */
    override fun onTimeout(startId: Int, fgsType: Int) { stopWith(tr("Android detuvo la vigilancia en segundo plano; vuelve a activarla en la app.")) }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val WAIT_FOR_UNLOCK_MS = 15_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val UNREACHABLE_AFTER = 6

        fun start(context: Context) {
            AlarmNotifier.ensureChannels(context)
            context.startForegroundService(Intent(context, AlarmWatcherService::class.java))
        }
        fun stop(context: Context) { context.stopService(Intent(context, AlarmWatcherService::class.java)) }
    }
}
