// ARMOR-ANDROID-CONTROL - a field node's own configuration panel, inside the app: every setting of the node, as if its web page were opened in a browser.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The panel of a node (radar, solar, electrical) is a web page the node itself serves on the local network. Instead of rebuilding its pages one by one - which would always
// be a copy that lags behind the node - this shows the real page in a WebView, so what the node offers is what the phone offers: every menu, every parameter, the login, the
// firmware update, the import and export of the settings. The WebView may only go where the node is (nothing else is opened), the node must be an address of the local network,
// and the two things a browser does that a WebView does not - choosing a file to send, and saving the file the page makes - are done here.
package es.electrohobby3d.armor

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import es.electrohobby3d.armor.model.NetworkOverview
import es.electrohobby3d.armor.model.NodeFinder
import java.io.File

/** The address of a node's panel, checked: an IPv4 address of the local network, with its port when it is not the usual one. */
object NodeAddress {
    private val Private = Regex("""^(10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})$""")

    /** `http://address[:port]/`, or null when [text] is not an address of the local network (a name, a public address, anything with a path or a login in it). */
    fun parse(text: String): String? {
        val typed = text.trim().removePrefix("http://").trimEnd('/')
        val host = typed.substringBefore(':')
        val port = if (':' in typed) typed.substringAfter(':').toIntOrNull() ?: return null else 80
        if (!Private.matches(host) || port !in 1..65535) return null
        if (host.split('.').any { it.toInt() > 255 }) return null
        return "http://$host${if (port == 80) "" else ":$port"}/"
    }
}

/** The list of the nodes the network node has found, and a box to type an address; choosing one opens its panel. */
@Composable
fun NodesScreen(network: NetworkOverview?, recent: List<String>, onOpen: (String) -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    val found = remember(network) { NodeFinder.candidates(network, emptyList()) }
    val problem = typed.isNotBlank() && NodeAddress.parse(typed) == null
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Abre el configurador de un nodo (radar, solar, eléctrico) dentro de la app: es su propia página, con todos sus menús y parámetros. Tu móvil tiene que estar en la misma red que el nodo.",
                style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted,
            )
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dirección del nodo", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(typed, { typed = it.take(24) }, Modifier.weight(1f), singleLine = true, placeholder = { Text("192.168.0.235") }, isError = problem)
                        Button(onClick = { NodeAddress.parse(typed)?.let(onOpen) }, enabled = NodeAddress.parse(typed) != null) { Text("Abrir") }
                    }
                    if (problem) Text("Escribe una dirección de tu red, como 192.168.0.235.", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Amber)
                    recent.forEach { address -> TextButton(onClick = { onOpen(address) }) { Text(address.removePrefix("http://").trimEnd('/')) } }
                }
            }
        }
        item { Text("Encontrados en la red", fontWeight = FontWeight.SemiBold) }
        if (found.isEmpty()) item { Text("El nodo de red no ha encontrado ninguno todavía. Escribe la dirección arriba.", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
        items(found) { node ->
            Panel(Modifier.fillMaxWidth(), onClick = { NodeAddress.parse(node.ip)?.let(onOpen) }) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(node.nodeId ?: node.hostname ?: node.ip, fontWeight = FontWeight.Medium)
                        Text(listOfNotNull(node.ip, node.mac, node.vendor, if (!node.online) "no responde ahora" else null).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                    }
                    Text("Configurar", color = ArmorColors.Cyan)
                }
            }
        }
    }
}

/** What the page of the node can ask of the phone: to save a file it has made (the exported settings), which a WebView does not do by itself. */
private class SaveBridge(private val context: Context) {
    @JavascriptInterface
    fun save(name: String, mime: String, base64: String) {
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "armor-node.json" }
        val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
        val saved = if (bytes == null || bytes.size > 5 * 1024 * 1024) null else runCatching { write(safeName, mime.ifBlank { "application/octet-stream" }, bytes) }.getOrNull()
        android.os.Handler(android.os.Looper.getMainLooper()).post { Toast.makeText(context, if (saved != null) "Guardado en Descargas: $safeName" else "No se pudo guardar el archivo", Toast.LENGTH_LONG).show() }
    }

    private fun write(name: String, mime: String, bytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, mime); put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
            return true
        }
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
        File(folder, name).writeBytes(bytes)
        return true
    }
}

/** The node's own panel, full size. [onClose] when the person leaves it (back, with nothing left to go back to). */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "JavascriptInterface")
@Composable
fun NodePanelView(address: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val host = remember(address) { Uri.parse(address).host.orEmpty() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var failed by remember(address) { mutableStateOf(false) }
    var chooser by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    // The page asks for a file (the settings to import, a firmware): the phone's own file picker answers.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        chooser?.onReceiveValue(uri?.let { arrayOf(it) })
        chooser = null
    }
    BackHandler { val view = webView; if (view != null && view.canGoBack()) view.goBack() else onClose() }
    Column(Modifier.fillMaxSize()) {
        if (failed) Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No se pudo abrir el nodo en $host. Comprueba que el móvil está en la misma red y que el nodo está encendido.", color = ArmorColors.Amber, style = MaterialTheme.typography.bodySmall)
            Button(onClick = { failed = false; webView?.reload() }) { Text("Reintentar") }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    setBackgroundColor(android.graphics.Color.parseColor("#0B1420"))
                    CookieManager.getInstance().setAcceptCookie(true)
                    addJavascriptInterface(SaveBridge(viewContext), "ArmorSave")
                    // Only the node itself: a link to anywhere else is not followed.
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = request.url.host != host
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) { if (request?.isForMainFrame == true) failed = true }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                            chooser?.onReceiveValue(null)
                            chooser = callback
                            picker.launch("*/*")
                            return true
                        }
                    }
                    // What the page saves (the exported settings are a file made in the page, a blob): fetched here and written to the phone's Downloads.
                    setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                        val name = Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""").find(contentDisposition.orEmpty())?.groupValues?.get(1) ?: "armor-node.json"
                        if (url.startsWith("blob:")) {
                            val script = "(function(){var x=new XMLHttpRequest();x.open('GET',${org.json.JSONObject.quote(url)},true);x.responseType='blob';x.onload=function(){var r=new FileReader();" +
                                "r.onloadend=function(){ArmorSave.save(${org.json.JSONObject.quote(name)},${org.json.JSONObject.quote(mimeType.orEmpty())},String(r.result).split(',')[1]||'');};r.readAsDataURL(x.response);};x.send();})();"
                            evaluateJavascript(script, null)
                        }
                    }
                    // A panel inside a scrolling parent: a finger on it moves the panel, it does not scroll what is around it.
                    setOnTouchListener { view, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) view.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                    loadUrl(address)
                    webView = this
                }
            },
        )
    }
}
