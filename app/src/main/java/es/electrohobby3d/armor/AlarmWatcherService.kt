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
import es.electrohobby3d.armor.network.ArmorApiClient
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
        val notification = AlarmNotifier.watchNotification(this, "Comprobando eventos cada ${INTERVAL_MS / 1000} s")
        if (Build.VERSION.SDK_INT >= 29) startForeground(AlarmNotifier.WATCH_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(AlarmNotifier.WATCH_ID, notification)
        if (job?.isActive != true) job = scope.launch { watch() }
        return START_NOT_STICKY
    }

    private suspend fun watch() {
        val origin = getSharedPreferences("armor-control", Context.MODE_PRIVATE).getString("origin", "").orEmpty()
        if (ServerEndpoint.parse(origin) == null) { stopWith("Configura el servidor en la app."); return }
        val client = ArmorApiClient()
        // The phone may have closed the app's process: the watcher carries the session kept by the app.
        SessionVault(PreferencesStore(getSharedPreferences("armor-session", Context.MODE_PRIVATE)), KeystoreBox()).load(origin)?.let { client.restoreSessionCookie(origin, it.name, it.value) }
        var failures = 0
        var firstFailureAt = 0L
        while (scope.isActive) {
            try {
                AlarmNotifier.poll(this, client, origin)
                failures = 0
                firstFailureAt = 0L
            } catch (error: ArmorApiException) {
                if (error.code == 401) { stopWith("La sesión ha caducado: abre la app e inicia sesión."); return }
                failures += 1
                if (failures == 1) firstFailureAt = System.currentTimeMillis()
            } catch (error: Exception) {
                failures += 1
                if (failures == 1) firstFailureAt = System.currentTimeMillis()
            }
            // A server that stays unreachable is itself worth knowing about, once.
            if (failures == UNREACHABLE_AFTER) AlarmNotifier.post(this, AlarmNotice(System.currentTimeMillis() % 1_000_000, "Servidor sin respuesta", "ARMOR-SERVER no responde desde hace ${(System.currentTimeMillis() - firstFailureAt) / 1000} s"))
            delay(nextDelayMs(failures))
        }
    }

    /** The normal interval while all goes well; after failures it doubles (up to a minute) with a little jitter, so a server that is down is not hammered. */
    private fun nextDelayMs(failures: Int): Long {
        if (failures == 0) return INTERVAL_MS
        val backoff = (INTERVAL_MS shl failures.coerceAtMost(3)).coerceAtMost(MAX_BACKOFF_MS)
        return backoff + Random.nextLong(0, 1_000)
    }

    private fun stopWith(reason: String) {
        AlarmNotifier.post(this, AlarmNotice(System.currentTimeMillis() % 1_000_000, "Vigilancia detenida", reason))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Android 15 ends a data-sync foreground service after a few hours; tell the operator instead of dying silently. */
    override fun onTimeout(startId: Int, fgsType: Int) { stopWith("Android detuvo la vigilancia en segundo plano; vuelve a activarla en la app.") }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val INTERVAL_MS = 10_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val UNREACHABLE_AFTER = 6

        fun start(context: Context) {
            AlarmNotifier.ensureChannels(context)
            context.startForegroundService(Intent(context, AlarmWatcherService::class.java))
        }
        fun stop(context: Context) { context.stopService(Intent(context, AlarmWatcherService::class.java)) }
    }
}
