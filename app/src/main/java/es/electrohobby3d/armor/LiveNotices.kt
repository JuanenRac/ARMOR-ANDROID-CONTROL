// ARMOR-ANDROID-CONTROL - the small live notices of the server (WebSocket /api/v1/events).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Besides the whole state, the server tells the consoles that something they may be showing changed: `{"type":"alarm","id":"alm-00001","version":12}` and nothing more. [type] is one of
 * [LiveNotices.TYPES]. A message of any other kind (the state, a kind this app does not know) is ignored, and polling stays as the slow safety net under the channel.
 */
data class LiveNotice(val type: String, val id: String, val version: Long)

object LiveNotices {
    val TYPES = listOf("alarm", "device", "solar", "network", "design")
    private const val MAX_MESSAGE = 1024
    private const val MAX_ID = 64
    const val FIRST_RETRY_MS = 2_000L
    const val LAST_RETRY_MS = 60_000L
    /** One notice within this long means the notices are flowing, and the app asks far less often. */
    const val FRESH_MS = 60_000L
    const val POLL_FAST_MS = 10_000L
    const val POLL_SLOW_MS = 30_000L

    /** The notice a message carries, or null when it is not one. */
    fun parse(text: String?): LiveNotice? {
        if (text == null || text.length > MAX_MESSAGE) return null
        val json = try { JSONObject(text) } catch (_: Exception) { return null }
        val type = json.optString("type", "")
        val id = json.opt("id") as? String ?: return null
        val version = json.opt("version") as? Number ?: return null
        if (type !in TYPES || id.length > MAX_ID) return null
        val number = version.toDouble()
        if (number.isNaN() || number.isInfinite()) return null
        return LiveNotice(type, id, version.toLong())
    }

    /** How long to wait before the next try after [failures] failed ones: two seconds, doubling, one minute at most. */
    fun retryDelay(failures: Int): Long {
        val step = (failures - 1).coerceIn(0, 10)
        return minOf(LAST_RETRY_MS, FIRST_RETRY_MS * (1L shl step))
    }

    /** The address of the channel of a server: the same address with ws for http and wss for https; null when it is not an address. */
    fun url(origin: String): String? {
        val trimmed = origin.trimEnd('/')
        return when {
            trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://") + "/api/v1/events"
            trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://") + "/api/v1/events"
            else -> null
        }
    }

    /** The pause between two passes of the polling: long while a notice arrived lately, as before otherwise. */
    fun pollDelay(lastNoticeAtMs: Long, nowMs: Long): Long =
        if (lastNoticeAtMs > 0 && nowMs - lastNoticeAtMs in 0..FRESH_MS) POLL_SLOW_MS else POLL_FAST_MS
}

/**
 * The open channel to one server. It reconnects by itself (two seconds, doubling, a minute at most), keeps the session cookie in the request, hands every notice to [onNotice] and tells
 * [opened] when it opens or is lost. Nothing is sent to the server: a message from it is read, never answered. The caller stops it with [stop].
 */
class LiveChannel(
    private val origin: String,
    private val cookie: () -> Pair<String, String>?,
    private val onNotice: (LiveNotice) -> Unit,
    private val opened: (Boolean) -> Unit,
    private val client: OkHttpClient = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(30, TimeUnit.SECONDS).build(),
    private val schedule: (Long, () -> Unit) -> Unit = { delayMs, task -> Thread { Thread.sleep(delayMs); task() }.also { it.isDaemon = true }.start() },
) {
    @Volatile private var stopped = false
    @Volatile private var failures = 0
    @Volatile private var socket: WebSocket? = null

    fun start() { stopped = false; connect() }

    fun stop() {
        stopped = true
        socket?.cancel()
        socket = null
        opened(false)
    }

    private fun connect() {
        if (stopped) return
        val address = LiveNotices.url(origin) ?: return
        val builder = Request.Builder().url(address)
        cookie()?.let { (name, value) -> builder.header("Cookie", "$name=$value") }
        socket = client.newWebSocket(builder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { failures = 0; opened(true) }
            override fun onMessage(webSocket: WebSocket, text: String) { LiveNotices.parse(text)?.let { notice -> if (!stopped) onNotice(notice) } }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = lost(webSocket)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = lost(webSocket)
        })
    }

    private fun lost(webSocket: WebSocket) {
        if (socket !== webSocket) return
        socket = null
        opened(false)
        if (stopped) return
        failures += 1
        schedule(LiveNotices.retryDelay(failures)) { connect() }
    }
}
