// ARMOR-ANDROID-CONTROL - the Cameras screen: the live views, the camera pad and the full-screen view, with icons instead of words.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.CameraView
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The stop of a held key goes out from here, not from the scope of the screen: that one is cancelled with the screen. */
private val stopScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

@Composable
fun CamerasScreen(
    cameras: List<CameraView>, grid: Int, onGrid: (Int) -> Unit, origin: String, viewModel: ArmorViewModel, authenticated: Boolean,
    recordingIds: Set<String>, health: Map<String, String>, onExpand: (CameraView) -> Unit, readOnly: Boolean = false,
) {
    // [grid] is how many cameras a page shows (each page opens that many live pictures, no more); the arrows go through the rest
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = if (cameras.isEmpty()) 1 else (cameras.size + grid - 1) / grid
    if (page >= pages) page = pages - 1
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(Icons.Filled.Videocam, tr("Cámaras"), if (cameras.isEmpty()) null else trf("%s en el servidor", cameras.size)) { GridPicker(grid, onGrid) }
        if (origin.isBlank() || cameras.isEmpty()) {
            Panel(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Icons.Filled.VideocamOff, ArmorColors.Muted, size = 64.dp)
                    Text(if (origin.isBlank()) tr("Conecta con el servidor primero") else tr("Aún no hay cámaras"), style = MaterialTheme.typography.titleMedium)
                    if (origin.isNotBlank()) Text(tr("Se añaden desde Studio, en el ordenador."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
                }
            }
        } else LazyVerticalGrid(
            columns = GridCells.Fixed(if (grid <= 1) 1 else 2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cameras.drop(page * grid).take(grid), key = { it.id }) { camera -> CameraTile(camera, origin, viewModel, authenticated && !readOnly, recordingIds.contains(camera.id), health[camera.id], onExpand) }
            if (pages > 1) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Anterior"), tint = ArmorColors.Cyan) }
                    Text(trf("Página %s de %s", page + 1, pages), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { page = (page + 1).coerceAtMost(pages - 1) }, enabled = page < pages - 1) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("Siguiente"), tint = ArmorColors.Cyan) }
                }
            }
        }
    }
}

@Composable
private fun GridPicker(grid: Int, choose: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.GridView, contentDescription = tr("Vistas"), tint = ArmorColors.Cyan) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(1, 2, 4, 6, 8, 9, 12, 16).forEach { amount ->
                DropdownMenuItem(
                    text = { Text(if (amount == 1) tr("1 cámara") else trf("%s cámaras", amount)) }, onClick = { choose(amount); open = false },
                    leadingIcon = { if (amount == grid) Icon(Icons.Filled.Check, null, tint = ArmorColors.Cyan) },
                )
            }
        }
    }
}

@Composable
private fun CameraTile(camera: CameraView, origin: String, viewModel: ArmorViewModel, operatorReady: Boolean, recording: Boolean, health: String?, expand: (CameraView) -> Unit) {
    var ptzOpen by remember { mutableStateOf(false) }
    val usable = operatorReady && camera.configured
    Panel(Modifier.fillMaxWidth().height(236.dp), tint = if (health == "offline") ArmorColors.Amber else null) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            when {
                health == "offline" -> CameraMessage(Icons.Filled.VideocamOff, tr("Sin señal"))
                camera.configured && camera.liveVideoAvailable -> MjpegFeed(viewModel.mjpegUrl(origin, camera), Modifier.fillMaxSize().clickable { expand(camera) }, sampleSize = 2, resolve = { viewModel.streamUrl(origin, camera) })
                else -> CameraMessage(Icons.Filled.VideocamOff, if (camera.configured) tr("Sin vídeo") else tr("Sin configurar"))
            }
            if (recording) Icon(Icons.Filled.FiberManualRecord, null, tint = ArmorColors.Alert, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(14.dp))
            if (ptzOpen) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) { PtzPad(enabled = usable, action = { viewModel.ptz(origin, camera, it) }, key = 34.dp) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(camera.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { ptzOpen = !ptzOpen }, enabled = usable, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.OpenWith, contentDescription = tr("Mover la cámara"), tint = if (ptzOpen) ArmorColors.Cyan else ArmorColors.Muted) }
            IconButton(onClick = { viewModel.snapshot(origin, camera) }, enabled = usable, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.PhotoCamera, contentDescription = tr("Foto"), tint = ArmorColors.Muted) }
            IconButton(onClick = { viewModel.toggleRecording(origin, camera) }, enabled = usable, modifier = Modifier.size(38.dp)) { Icon(if (recording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, contentDescription = if (recording) tr("Parar") else tr("Grabar"), tint = ArmorColors.Alert) }
            IconButton(onClick = { expand(camera) }, enabled = camera.configured, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.OpenInFull, contentDescription = tr("Ampliar"), tint = ArmorColors.Muted) }
        }
    }
}

@Composable
private fun CameraMessage(icon: ImageVector, text: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = ArmorColors.Muted, modifier = Modifier.size(34.dp))
        Text(text, color = ArmorColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FullscreenCamera(camera: CameraView, origin: String, viewModel: ArmorViewModel, unlocked: Boolean, recording: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
        title = { Text(camera.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                    if (camera.liveVideoAvailable) MjpegFeed(viewModel.mjpegUrl(origin, camera), Modifier.fillMaxSize(), resolve = { viewModel.streamUrl(origin, camera) }) else CameraMessage(Icons.Filled.VideocamOff, tr("Sin vídeo"))
                }
                PtzPad(enabled = unlocked, action = { viewModel.ptz(origin, camera, it) })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { viewModel.snapshot(origin, camera) }, enabled = unlocked) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text(tr("Foto")) }
                    FilledTonalButton(onClick = { viewModel.toggleRecording(origin, camera) }, enabled = unlocked) {
                        Icon(if (recording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, null, tint = ArmorColors.Alert); Spacer(Modifier.width(8.dp)); Text(if (recording) tr("Parar") else tr("Grabar"))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
    )
}

/**
 * A key that moves the camera while it is held: pressing sends the move and repeats it every second (the server
 * stops a move by itself after a couple of seconds), releasing sends a stop after at least 300 ms, so a tap
 * still moves the camera visibly.
 */
@Composable
private fun HoldKey(icon: ImageVector, description: String, command: String, enabled: Boolean, size: Dp, action: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(action)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(10.dp))
            .background(if (enabled) ArmorColors.Outline else ArmorColors.Surface)
            .pointerInput(enabled, command) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    val started = System.currentTimeMillis()
                    val repeat = scope.launch { while (true) { current(command); delay(1_000) } }
                    try { tryAwaitRelease() } finally {
                        repeat.cancel()
                        // a stop must go out even when the screen is gone (a dialog closed with the finger down): it is not launched in the scope of the screen
                        val wait = 300 - (System.currentTimeMillis() - started)
                        stopScope.launch { if (wait > 0) delay(wait); current("stop") }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = if (enabled) ArmorColors.Cyan else ArmorColors.Muted, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
private fun PtzPad(enabled: Boolean, action: (String) -> Unit, key: Dp = 48.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HoldKey(Icons.Filled.KeyboardArrowUp, tr("Arriba"), "up", enabled, key, action)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HoldKey(Icons.AutoMirrored.Filled.ArrowBack, tr("Izquierda"), "left", enabled, key, action)
            Box(Modifier.size(key).clip(RoundedCornerShape(10.dp)).background(ArmorColors.Surface).clickable(enabled = enabled) { action("stop") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Stop, contentDescription = tr("Parar"), tint = ArmorColors.Muted, modifier = Modifier.size(key * 0.5f))
            }
            HoldKey(Icons.AutoMirrored.Filled.ArrowForward, tr("Derecha"), "right", enabled, key, action)
        }
        HoldKey(Icons.Filled.KeyboardArrowDown, tr("Abajo"), "down", enabled, key, action)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { HoldKey(Icons.Filled.ZoomOut, tr("Alejar"), "zoomOut", enabled, key, action); HoldKey(Icons.Filled.ZoomIn, tr("Acercar"), "zoomIn", enabled, key, action) }
    }
}
