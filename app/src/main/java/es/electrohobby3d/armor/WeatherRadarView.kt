// ARMOR-ANDROID-CONTROL - the live rain-and-cloud radar of the Weather screen: a WebView over assets/weather_radar.html (Leaflet), ported from ARMOR-STUDIO's RadarMap.tsx.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The page used to be opened as a file (file:///android_asset/...) and was given permission to fetch from other sites. Recent Android and WebView versions ignore or refuse
// that permission, and the radar - its frames, its map - never loaded, with no message at all. The page is now served from a made-up https address that this app answers by
// itself from its own assets (nothing is fetched from there), so it is an ordinary https page whose fetches follow the ordinary rules the weather services already allow, and
// no file permission is asked for. If it still cannot load, the screen says so and offers to try again, instead of showing an empty box.
package es.electrohobby3d.armor

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException
import java.net.URLEncoder

/** The address the radar page is served from; the app answers it from its assets and nothing leaves the phone for it. */
internal const val RADAR_HOST = "appassets.armor.local"

/** The file type of an asset of the radar page. */
internal fun radarMimeType(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
    "html" -> "text/html"
    "js" -> "application/javascript"
    "css" -> "text/css"
    "png" -> "image/png"
    "svg" -> "image/svg+xml"
    else -> "application/octet-stream"
}

/** The page's own address for a place. */
internal fun radarPageUrl(lat: Double, lon: Double, name: String): String =
    "https://$RADAR_HOST/weather_radar.html?lat=$lat&lon=$lon&name=${URLEncoder.encode(name, "UTF-8")}"

/** What the WebView reports about the page, shown under the map in plain text (it stays readable when the page itself paints nothing). */
internal class RadarReport {
    var phase = "sin empezar"
    var progress = 0
    val problems = ArrayDeque<String>()
    var networkErrors = 0
    var httpErrors = 0
    fun problem(text: String) { if (problems.size >= 3) problems.removeFirst(); problems.addLast(text.take(140)) }
    fun text(webView: String): String = buildString {
        append("Radar: ").append(phase).append(" ").append(progress).append("%")
        if (networkErrors > 0) append(" · ").append(networkErrors).append(" fallos de red")
        if (httpErrors > 0) append(" · ").append(httpErrors).append(" respuestas con error")
        append(" · WebView ").append(webView)
        problems.forEach { append("\n").append(it) }
    }
}

private fun webViewVersion(): String = runCatching { android.webkit.WebView.getCurrentWebViewPackage()?.versionName }.getOrNull() ?: "?"

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun RadarMapView(lat: Double, lon: Double, name: String) {
    val url = radarPageUrl(lat, lon, name)
    var failed by remember(url) { mutableStateOf(false) }
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf("") }
    val report = remember(url) { RadarReport() }
    var reportText by remember(url) { mutableStateOf("") }
    val version = remember { webViewVersion() }
    fun refresh() { reportText = report.text(version) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(340.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // A page that never paints would look like a black box; with the page's own colour behind it, it is told apart from a map that is dark.
                    setBackgroundColor(android.graphics.Color.parseColor("#0B1A22"))
                    // A map inside a scrolling list: while a finger is on the map it pans the map, it does not scroll the list.
                    setOnTouchListener { view, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) view.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) { report.phase = "cargando"; refresh() }
                        override fun onPageFinished(view: WebView?, pageUrl: String?) { report.phase = "página cargada"; refresh() }
                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            report.httpErrors += 1
                            report.problem("HTTP ${errorResponse?.statusCode} en ${request?.url?.host}${request?.url?.path?.takeLast(24)}")
                            refresh()
                        }
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            if (request.url.host != RADAR_HOST) return null
                            val path = request.url.path.orEmpty().trimStart('/')
                            return try {
                                WebResourceResponse(radarMimeType(path), "utf-8", context.assets.open(path))
                            } catch (error: IOException) {
                                WebResourceResponse("text/plain", "utf-8", 404, "Not Found", emptyMap(), "".byteInputStream())
                            }
                        }
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            Log.e("ArmorWeatherRadar", "load error on ${request?.url}: ${error?.description}")
                            report.networkErrors += 1
                            report.problem("${error?.description} en ${request?.url?.host}")
                            refresh()
                            if (request?.isForMainFrame == true) failed = true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Log.d("ArmorWeatherRadar", "${message.messageLevel()} ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                            if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR || message.message().startsWith("map size")) { report.problem(message.message()); refresh() }
                            return true
                        }
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { report.progress = newProgress; refresh() }
                    }
                }
            },
            // Loads when the place changes or when the person asks again - not on every redraw of the screen, which would restart the animation.
            update = { view ->
                val wanted = "$url#$attempt"
                if (loaded != wanted) { loaded = wanted; failed = false; view.loadUrl(url) }
            },
        )
        if (failed) Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("No se pudo cargar el radar. Comprueba la conexión del móvil.")
            Button(onClick = { attempt += 1 }) { Text("Reintentar") }
        }
    }
    if (reportText.isNotEmpty()) Text(reportText, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF86A8B3))
    }
}
