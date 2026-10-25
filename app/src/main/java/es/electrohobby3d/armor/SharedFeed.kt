// ARMOR-ANDROID-CONTROL - what the open app and the background watch share, so that the server is not asked the same thing twice: one client for the process, a short memory of the latest status
// and history answers, and a note of when the app on screen last read the server (the watch then leaves its turn to it).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.ArmorSnapshot
import es.electrohobby3d.armor.network.ArmorApi
import es.electrohobby3d.armor.network.ArmorApiClient

/** The status and the history are remembered for a moment: two askers within that moment get the same answer from one request. Everything else goes straight to the server. */
class CachingApi(
    private val inner: ArmorApi, private val now: () -> Long = System::currentTimeMillis,
    private val statusTtlMs: Long = 1_200, private val historyTtlMs: Long = 3_000,
) : ArmorApi by inner {
    private data class Held<T>(val at: Long, val value: T)
    private val statuses = HashMap<String, Held<ArmorSnapshot>>()
    private val histories = HashMap<String, Held<List<ArmorEvent>>>()

    /** The number of requests that really went to the server, for the tests. */
    @Volatile var served = 0
        private set

    override fun status(origin: String): ArmorSnapshot = synchronized(statuses) {
        statuses[origin]?.takeIf { now() - it.at in 0 until statusTtlMs }?.value
            ?: inner.status(origin).also { served += 1; statuses[origin] = Held(now(), it) }
    }

    override fun history(origin: String, limit: Int, before: Long?, after: Long?): List<ArmorEvent> = synchronized(histories) {
        val key = "$origin|$limit|$before|$after"
        histories[key]?.takeIf { now() - it.at in 0 until historyTtlMs }?.value
            ?: inner.history(origin, limit, before, after).also { served += 1; histories[key] = Held(now(), it); if (histories.size > 12) histories.keys.drop(8).forEach(histories::remove) }
    }

    // an order changes what the answers say: whatever was remembered before it is stale, so the next reading goes to the server
    override fun setMode(origin: String, mode: String, pin: String?): ArmorSnapshot = try { inner.setMode(origin, mode, pin) } finally { clear() }
    override fun acknowledgeAlarm(origin: String, id: String) = try { inner.acknowledgeAlarm(origin, id) } finally { clear() }
    override fun acknowledgeAllAlarms(origin: String) = try { inner.acknowledgeAllAlarms(origin) } finally { clear() }
    override fun sendAlarmCommand(origin: String, node: String, mode: String?, force: Boolean, pin: String?, profile: String?) = try { inner.sendAlarmCommand(origin, node, mode, force, pin, profile) } finally { clear() }
    override fun silenceAlarmSiren(origin: String, node: String, pin: String?) = try { inner.silenceAlarmSiren(origin, node, pin) } finally { clear() }
    override fun commandDevice(origin: String, id: String, command: String, confirm: Boolean) = try { inner.commandDevice(origin, id, command, confirm) } finally { clear() }
    override fun voiceCommand(origin: String, text: String, language: String, confirmation: String?) = try { inner.voiceCommand(origin, text, language, confirmation) } finally { clear() }
    override fun closeStudioSession(origin: String) = try { inner.closeStudioSession(origin) } finally { clear() }
    override fun forgetNode(origin: String, id: String) = try { inner.forgetNode(origin, id) } finally { clear() }

    /** Forgets what is remembered (the address changed, the person signed out). */
    fun clear() { synchronized(statuses) { statuses.clear() }; synchronized(histories) { histories.clear() } }
}

object SharedFeed {
    /** One client for the whole process: the open app and the background watch use it, and so share its session and its short memory. */
    val api: CachingApi by lazy { CachingApi(ArmorApiClient()) }

    @Volatile private var appReadAt = 0L

    /** The app on screen has just read the server. */
    fun appRead(now: Long = System.currentTimeMillis()) { appReadAt = now }

    /** The app on screen read the server lately, so the watch need not (it would only ask the same again). */
    fun appIsReading(now: Long = System.currentTimeMillis(), windowMs: Long = WINDOW_MS): Boolean = now - appReadAt in 0 until windowMs

    const val WINDOW_MS = 25_000L
}
