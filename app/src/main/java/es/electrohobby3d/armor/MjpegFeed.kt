// ARMOR-ANDROID-CONTROL - bounded MJPEG renderer for ARMOR-SERVER relays.
package es.electrohobby3d.armor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

private sealed interface MjpegState { data object Loading : MjpegState; data class Frame(val bitmap: Bitmap) : MjpegState; data class Failure(val message: String) : MjpegState }

@Composable
fun MjpegFeed(url: String, modifier: Modifier = Modifier) {
    val feed by produceState<MjpegState>(MjpegState.Loading, url) {
        value = MjpegState.Loading
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("Accept", "multipart/x-mixed-replace")
                if (connection.responseCode != 200) throw IllegalStateException("HTTP ${connection.responseCode}")
                connection.inputStream.use { raw ->
                    val input = BufferedInputStream(raw, 64 * 1024)
                    while (isActive) {
                        val frame = readJpeg(input) ?: break
                        BitmapFactory.decodeByteArray(frame, 0, frame.size)?.let { value = MjpegState.Frame(it) }
                    }
                    if (value is MjpegState.Loading) value = MjpegState.Failure("No video frames received")
                }.also { connection.disconnect() }
            } catch (error: Exception) { value = MjpegState.Failure(error.message ?: "Camera stream unavailable") }
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when (val current = feed) {
            MjpegState.Loading -> CircularProgressIndicator()
            is MjpegState.Failure -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is MjpegState.Frame -> Canvas(Modifier.fillMaxSize()) {
                val image = current.bitmap.asImageBitmap()
                val scale = minOf(size.width / image.width, size.height / image.height)
                val width = image.width * scale; val height = image.height * scale
                drawImage(image, dstOffset = androidx.compose.ui.unit.IntOffset(((size.width - width) / 2).toInt(), ((size.height - height) / 2).toInt()), dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt()))
            }
        }
    }
}

private const val MAX_JPEG_BYTES = 8 * 1024 * 1024

/** One JPEG of the stream, from its start marker to its end marker, or null when the stream ends or the picture is larger than [MAX_JPEG_BYTES]. */
private fun readJpeg(input: BufferedInputStream): ByteArray? {
    val bytes = java.io.ByteArrayOutputStream(64 * 1024)
    var previous = -1
    while (true) {
        val value = input.read()
        if (value < 0) return null
        if (previous == 0xff && value == 0xd8) { bytes.write(0xff); bytes.write(0xd8); break }
        previous = value
    }
    previous = -1
    while (bytes.size() < MAX_JPEG_BYTES) {
        val value = input.read(); if (value < 0) return null
        bytes.write(value)
        if (previous == 0xff && value == 0xd9) return bytes.toByteArray()
        previous = value
    }
    return null
}
