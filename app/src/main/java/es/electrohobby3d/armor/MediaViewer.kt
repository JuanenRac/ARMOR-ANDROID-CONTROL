// ARMOR-ANDROID-CONTROL - looking at a photo or a video kept on the server.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import es.electrohobby3d.armor.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** A photo or a video of the evidence library, in a window: the photo is drawn, the video plays with the controls of the phone. Both are asked for with the session the app holds. */
@Composable
fun MediaViewerDialog(item: MediaItem, url: String, cookie: Pair<String, String>?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
        title = { Text(if (item.kind == "snapshot") "Foto" else "Vídeo") },
        text = {
            Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                if (item.kind == "snapshot") StillPicture(url, cookie)
                else AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        VideoView(context).apply {
                            setMediaController(MediaController(context).also { it.setAnchorView(this) })
                            setVideoURI(Uri.parse(url), cookie?.let { mapOf("Cookie" to "${it.first}=${it.second}") } ?: emptyMap())
                            setOnPreparedListener { start() }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

private const val MAX_PICTURE_BYTES = 12 * 1024 * 1024

@Composable
private fun StillPicture(url: String, cookie: Pair<String, String>?) {
    val picture by produceState<Result<android.graphics.Bitmap>?>(null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 8_000; connection.readTimeout = 20_000
                    cookie?.let { connection.setRequestProperty("Cookie", "${it.first}=${it.second}") }
                    if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
                    val bytes = connection.inputStream.use { it.readNBytes(MAX_PICTURE_BYTES) }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("La imagen no se pudo leer")
                } finally { connection.disconnect() }
            }
        }
    }
    when (val shown = picture) {
        null -> CircularProgressIndicator()
        else -> shown.fold(
            onSuccess = { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) },
            onFailure = { Text(it.message ?: "No se pudo abrir", color = MaterialTheme.colorScheme.error) },
        )
    }
}
