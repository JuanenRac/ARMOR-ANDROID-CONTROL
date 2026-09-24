// ARMOR-ANDROID-CONTROL - ARMOR-SERVER HTTP client.
// Camera passwords and RTSP URLs never enter this client or the Android UI.
package es.electrohobby3d.armor.network

import es.electrohobby3d.armor.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class ArmorApiException(message: String) : IllegalStateException(message)

class ArmorApiClient {
    init {
        // Retain only the server's HttpOnly operator-session cookie. Android's
        // HttpURLConnection has no cookie store until one is explicitly set.
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
        }
    }

    fun status(origin: String): ArmorSnapshot = getJson(origin, "/api/v1/status").let { root ->
        val nodeObject = root.optJSONObject("nodes") ?: JSONObject()
        val nodes = nodeObject.keys().asSequence().map { id ->
            nodeObject.getJSONObject(id).let { node ->
                FieldNode(id, node.optBoolean("online"), if (node.isNull("lux")) null else node.optDouble("lux"), node.optInt("target_count"), node.optString("alert_level", "normal"))
            }
        }.toList()
        ArmorSnapshot(root.optString("mode", "disarmed"), root.optLong("revision"), root.optString("updated_at"), nodes)
    }

    fun cameraViews(origin: String): List<CameraView> = getJson(origin, "/api/v1/camera-views").optJSONArray("cameras").asObjects().map { camera ->
        CameraView(camera.getString("id"), camera.optString("name", camera.getString("id")), camera.optString("host"), camera.optString("snapshotUrl"), camera.optString("rtspPath"), camera.optInt("onvifPort", 80), camera.optInt("rtspPort", 554), camera.optBoolean("hasCredentials"), camera.optBoolean("liveVideoAvailable"))
    }

    fun openOperatorSession(origin: String, token: String) {
        request(origin, "/api/v1/operator/session", "POST", mapOf("Authorization" to "Bearer $token"), null, setOf(201)).disconnect()
    }

    fun studioSessionActive(origin: String): Boolean = getJson(origin, "/api/v1/studio/session").optBoolean("authenticated")

    /** Sends the human login only once; the server returns an HttpOnly session cookie. */
    fun openStudioSession(origin: String, username: String, password: String) {
        request(
            origin,
            "/api/v1/studio/session",
            "POST",
            mapOf("Content-Type" to "application/json"),
            JSONObject(mapOf("username" to username, "password" to password)).toString(),
            setOf(201),
        ).disconnect()
    }

    fun closeStudioSession(origin: String) {
        request(origin, "/api/v1/studio/session", "DELETE", emptyMap(), null, setOf(204)).disconnect()
    }

    fun snapshot(origin: String, cameraId: String) { request(origin, "/api/v1/cameras/${part(cameraId)}/snapshot", "POST", emptyMap(), null, setOf(201)).disconnect() }
    fun recording(origin: String, cameraId: String, start: Boolean) {
        request(origin, "/api/v1/cameras/${part(cameraId)}/recordings/${if (start) "start" else "stop"}", "POST", emptyMap(), null, setOf(if (start) 202 else 200)).disconnect()
    }
    fun ptz(origin: String, cameraId: String, command: String) {
        request(origin, "/api/v1/cameras/${part(cameraId)}/ptz", "POST", mapOf("Content-Type" to "application/json"), JSONObject(mapOf("command" to command)).toString(), setOf(200)).disconnect()
    }
    fun media(origin: String): MediaCatalogue = getJson(origin, "/api/v1/media").let { root ->
        val items = root.optJSONArray("items").asObjects().map { item -> MediaItem(item.getString("id"), item.getString("cameraId"), item.getString("kind"), item.getString("file"), item.getString("createdAt"), item.optLong("bytes")) }
        MediaCatalogue(items, root.optJSONArray("activeCameraIds").asStrings().toSet())
    }
    fun deleteMedia(origin: String, item: MediaItem) {
        val kind = if (item.kind == "snapshot") "snapshots" else "recordings"
        request(origin, "/api/v1/media/${part(item.cameraId)}/$kind/${part(item.file)}", "DELETE", emptyMap(), null, setOf(204)).disconnect()
    }
    fun mediaUrl(origin: String, item: MediaItem): String {
        val kind = if (item.kind == "snapshot") "snapshots" else "recordings"
        return "$origin/api/v1/media/${part(item.cameraId)}/$kind/${part(item.file)}"
    }
    fun mjpegUrl(origin: String, id: String): String = "$origin/api/v1/cameras/${part(id)}/mjpeg"

    private fun getJson(origin: String, path: String): JSONObject {
        val connection = request(origin, path, "GET", mapOf("Accept" to "application/json"), null, setOf(200))
        return try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }) } finally { connection.disconnect() }
    }
    private fun request(origin: String, path: String, method: String, headers: Map<String, String>, body: String?, expected: Set<Int>): HttpURLConnection {
        val connection = (URL(origin.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 12_000; instanceFollowRedirects = false
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) { doOutput = true; outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) } }
        }
        if (connection.responseCode !in expected) {
            val stream = connection.errorStream
            val detail = stream?.bufferedReader()?.use { it.readText() }?.take(180).orEmpty()
            connection.disconnect()
            throw ArmorApiException("Server operation failed (HTTP ${connection.responseCode})${if (detail.isBlank()) "" else ": $detail"}")
        }
        return connection
    }
    private fun part(value: String) = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
}

private fun JSONArray?.asObjects(): List<JSONObject> = buildList { if (this@asObjects != null) for (index in 0 until length()) optJSONObject(index)?.let(::add) }
private fun JSONArray?.asStrings(): List<String> = buildList { if (this@asStrings != null) for (index in 0 until length()) add(optString(index)) }
