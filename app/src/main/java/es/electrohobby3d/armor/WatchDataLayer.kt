// ARMOR-ANDROID-CONTROL - the Data Layer of Wear OS: carries the messages of WatchRelay to the connected watch and receives its confirmations. Used only while the option of the watch is on.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Sends to every watch that is connected, on a thread of its own: the Data Layer is waited for and the caller may be the main thread. A phone with no watch sends to nobody. */
class DataLayerTransport private constructor(private val context: Context) : WatchTransport {
    override fun send(path: String, payload: String) {
        executor.execute {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes, TIMEOUT_S, TimeUnit.SECONDS)
                for (node in nodes) Tasks.await(Wearable.getMessageClient(context).sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8)), TIMEOUT_S, TimeUnit.SECONDS)
            } catch (_: Exception) { /* no watch in reach, or no Data Layer on this phone: the notice on the phone is what counts */ }
        }
    }

    companion object {
        private const val TIMEOUT_S = 3L
        private val executor = Executors.newSingleThreadExecutor()

        @Volatile
        private var instance: DataLayerTransport? = null

        fun of(context: Context): DataLayerTransport = instance ?: synchronized(this) { instance ?: DataLayerTransport(context.applicationContext).also { instance = it } }
    }
}

/** The confirmation of the watch: the phone acknowledges the alarm with the session it keeps (as the action of a notice does) and tells the watch the alert is over. */
class WatchAckService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchRelay.PATH_TO_PHONE) return
        val (alertId, alarm) = WatchRelay.alarmToAcknowledge(WatchRelay.enabled(this), String(event.data, Charsets.UTF_8)) ?: return
        val done = try { AlarmActionReceiver.acknowledge(applicationContext, alarm) } catch (_: Exception) { false }
        if (done) DataLayerTransport.of(this).send(WatchRelay.PATH_TO_WATCH, WatchRelay.clearMessage(alertId))
        else WatchRelay.sent.remember(alertId, alarm, WatchRelay.OTHER_TTL_S)   // the server did not take it: the watch may ask again
    }
}
