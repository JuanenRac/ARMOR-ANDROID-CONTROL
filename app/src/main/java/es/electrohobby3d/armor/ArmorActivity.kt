// ARMOR-ANDROID-CONTROL - mobile operator client.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.model.FieldNode
import es.electrohobby3d.armor.model.MediaItem
import java.net.URI

class ArmorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArmorScreen() }
    }
}

private enum class MobileSection(val label: String, val glyph: String) {
    Overview("Estado", "◈"), Alarms("Alarmas", "!"), Devices("Dispositivos", "▤"), Cameras("Cámaras", "◉"), More("Más", "≡")
}

/** What "Más" holds, so the bottom bar keeps five places. */
private enum class MoreTab(val label: String) { Evidence("Grabaciones"), History("Historial"), Settings("Conexión") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArmorScreen(viewModel: ArmorViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("armor-control", Context.MODE_PRIVATE) }
    var origin by rememberSaveable { mutableStateOf(preferences.getString("origin", "") ?: "") }
    val savedUri = remember(origin) { runCatching { URI(origin) }.getOrNull() }
    var serverHost by rememberSaveable { mutableStateOf(savedUri?.host.orEmpty()) }
    var serverPort by rememberSaveable { mutableStateOf(if (savedUri?.port in 1..65535) savedUri?.port.toString() else "8080") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf(MobileSection.Overview) }
    var moreTab by rememberSaveable { mutableStateOf(MoreTab.Evidence) }
    var confirmMode by remember { mutableStateOf<String?>(null) }
    var grid by rememberSaveable { mutableIntStateOf(4) }
    var expanded by remember { mutableStateOf<CameraView?>(null) }
    var about by remember { mutableStateOf(false) }
    var nodeSetup by rememberSaveable { mutableStateOf(false) }   // configuring a field node over Bluetooth, with or without a server
    val state by viewModel.state.collectAsStateWithLifecycle()
    var watching by rememberSaveable { mutableStateOf(preferences.getBoolean("watch", false)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val lifecycleOwner = LocalLifecycleOwner.current
    val validOrigin = ServerEndpoint.parse(origin) != null
    val currentOrigin = ServerEndpoint.parse(origin)?.origin.orEmpty()

    // While the app is on screen everything refreshes every ten seconds and alarms are announced.
    LaunchedEffect(state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank()) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { viewModel.pollOnce(currentOrigin, context.applicationContext); delay(10_000) }
        }
    }
    LaunchedEffect(state.authenticated) {
        if (state.authenticated && android.os.Build.VERSION.SDK_INT >= 33 && !AlarmNotifier.canNotify(context)) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        if (state.authenticated && watching) AlarmWatcherService.start(context) else if (!state.authenticated) AlarmWatcherService.stop(context)
    }

    ArmorTheme {
        if (nodeSetup) {
            Surface(Modifier.fillMaxSize()) { NodeSetupScreen(onClose = { nodeSetup = false }) }
        } else if (!state.authenticated) {
            val loginEndpoint = ServerEndpoint.fromHostAndPort(serverHost, serverPort)
            LoginPanel(
                host = serverHost, port = serverPort, username = username, password = password,
                busy = state.loading, message = state.message,
                onHost = { serverHost = it }, onPort = { serverPort = it },
                onUsername = { username = it }, onPassword = { password = it },
                onLogin = { loginEndpoint?.let { endpoint ->
                    origin = endpoint.origin
                    preferences.edit().putString("origin", endpoint.origin).apply()
                    viewModel.login(endpoint.origin, username, password)
                    password = ""
                } },
                enabled = loginEndpoint != null && username.isNotBlank() && password.isNotBlank(),
                onNodeSetup = { nodeSetup = true },
            )
        } else {
        Scaffold(
            topBar = { TopAppBar(title = { Column { Text("A.R.M.O.R."); Text("MOBILE CONTROL", style = MaterialTheme.typography.labelSmall) } }, actions = { Text("REV ${state.snapshot.revision}", style = MaterialTheme.typography.labelMedium); IconButton(onClick = { about = true }) { Text("i") } }) },
            bottomBar = { NavigationBar { MobileSection.entries.forEach { item ->
                val waiting = if (item == MobileSection.Alarms) state.alarms.count { !it.acknowledged } else 0
                NavigationBarItem(selected = section == item, onClick = { section = item }, icon = { if (waiting > 0) BadgedBox(badge = { Badge { Text(waiting.toString()) } }) { Text(item.glyph) } else Text(item.glyph) }, label = { Text(item.label, maxLines = 1) })
            } } },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                state.message?.let { message -> AssistChip(onClick = {}, label = { Text(message, maxLines = 2, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                when (section) {
                    MobileSection.Overview -> OverviewPanel(
                        state.snapshot.mode, state.snapshot.revision, state.snapshot.updatedAt, state.snapshot.nodes, state.cameras.size, state.cameraHealth.count { it.value == "offline" },
                        pendingAlarms = state.alarms.count { !it.acknowledged }, devices = state.devices, onMode = { confirmMode = it }, onAlarms = { section = MobileSection.Alarms }, onDevices = { section = MobileSection.Devices },
                        onRefresh = { viewModel.refresh(currentOrigin) }, enabled = validOrigin && !state.loading,
                    )
                    MobileSection.Alarms -> AlarmsPanel(state.alarms, state.closedAlarms, state.devices, state.cameras.associate { it.id to it.name }, onAcknowledge = { viewModel.acknowledge(currentOrigin, it) }, onAcknowledgeAll = { viewModel.acknowledgeAll(currentOrigin) }, enabled = validOrigin && !state.loading)
                    MobileSection.Devices -> DevicesPanel(state.devices, onCommand = { device, command -> viewModel.command(currentOrigin, device, command) }, enabled = validOrigin && !state.loading)
                    MobileSection.Cameras -> CamerasPanel(state.cameras, grid, onGrid = { grid = it }, origin = currentOrigin, viewModel = viewModel, authenticated = state.authenticated, recordingIds = state.media.activeCameraIds, health = state.cameraHealth, onExpand = { expanded = it })
                    MobileSection.More -> Column(Modifier.fillMaxSize()) {
                        PrimaryTabRow(selectedTabIndex = moreTab.ordinal) { MoreTab.entries.forEach { tab -> Tab(selected = moreTab == tab, onClick = { moreTab = tab }, text = { Text(tab.label) }) } }
                        Spacer(Modifier.height(8.dp))
                        when (moreTab) {
                            MoreTab.Evidence -> EvidencePanel(state.media.items, state.cameras, origin = currentOrigin, viewModel = viewModel, unlocked = state.authenticated)
                            MoreTab.History -> HistoryPanel(state.events, onMore = { viewModel.loadOlderEvents(currentOrigin) })
                            MoreTab.Settings -> SessionSettingsPanel(origin, { origin = it }, validOrigin, state.authenticated, onConnect = { preferences.edit().putString("origin", currentOrigin).apply(); viewModel.refresh(currentOrigin) }, onMedia = { viewModel.loadMedia(currentOrigin) }, onLogout = { AlarmWatcherService.stop(context); viewModel.logout(currentOrigin) }, onNodeSetup = { nodeSetup = true },
                                watching = watching,
                                onWatching = { on ->
                                    watching = on
                                    preferences.edit().putBoolean("watch", on).apply()
                                    if (on) {
                                        if (android.os.Build.VERSION.SDK_INT >= 33 && !AlarmNotifier.canNotify(context)) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        AlarmWatcherService.start(context)
                                    } else AlarmWatcherService.stop(context)
                                })
                        }
                    }
                }
            }
        }
        confirmMode?.let { mode -> AlertDialog(
            onDismissRequest = { confirmMode = null },
            title = { Text(if (mode == "armed") "¿Armar el sistema?" else "¿Desarmar el sistema?") },
            text = { Text(if (mode == "armed") "Con el sistema armado, puertas, ventanas y movimiento dispararán alarma." else "Con el sistema desarmado, los contactos y el movimiento dejan de disparar alarma; el humo, el gas, la inundación y el pánico siguen avisando.") },
            confirmButton = { TextButton(onClick = { viewModel.setMode(currentOrigin, mode); confirmMode = null }) { Text(if (mode == "armed") "Armar" else "Desarmar") } },
            dismissButton = { TextButton(onClick = { confirmMode = null }) { Text("Cancelar") } },
        ) }
        expanded?.let { camera -> FullscreenCamera(camera, currentOrigin, viewModel, state.authenticated, state.media.activeCameraIds.contains(camera.id), onDismiss = { expanded = null }) }
        }
        if (about) AlertDialog(onDismissRequest = { about = false }, confirmButton = { TextButton(onClick = { about = false }) { Text("Cerrar") } }, title = { Text("A.R.M.O.R. Mobile Control") }, text = { Text("v${BuildConfig.VERSION_NAME}\nCliente móvil para el estado, armar y desarmar, alarmas, dispositivos, historial de eventos, cámaras, PTZ y biblioteca de evidencias de ARMOR-SERVER. Las contraseñas de cámaras nunca salen del servidor.") })
    }
}

@Composable
private fun LoginPanel(
    host: String, port: String, username: String, password: String, busy: Boolean, message: String?,
    onHost: (String) -> Unit, onPort: (String) -> Unit, onUsername: (String) -> Unit,
    onPassword: (String) -> Unit, onLogin: () -> Unit, enabled: Boolean, onNodeSetup: () -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("A.R.M.O.R.", style = MaterialTheme.typography.displaySmall)
            Text("MOBILE CONTROL", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(28.dp))
            Text("Acceso al servidor", style = MaterialTheme.typography.headlineSmall)
            Text("Introduce la IP privada, puerto y credenciales de ARMOR-SERVER.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = host, onValueChange = onHost, label = { Text("IP o nombre del servidor") }, placeholder = { Text("192.168.0.50") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = port, onValueChange = onPort, label = { Text("Puerto del servidor") }, placeholder = { Text("8080") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = username, onValueChange = onUsername, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = onPassword, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onLogin, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Verificando..." else "Entrar") }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            Text("La contraseña se envía sólo para crear una sesión HttpOnly del servidor; no se guarda en el teléfono.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp))
            OutlinedButton(onClick = onNodeSetup, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) { Text("Configurar un nodo por Bluetooth") }
        }
    }
}

@Composable private fun OverviewPanel(
    mode: String, revision: Long, updated: String, nodes: List<FieldNode>, cameras: Int, camerasDown: Int,
    pendingAlarms: Int, devices: List<es.electrohobby3d.armor.model.SiteDevice>, onMode: (String) -> Unit, onAlarms: () -> Unit, onDevices: () -> Unit,
    onRefresh: () -> Unit, enabled: Boolean,
) {
    val armed = mode == "armed"
    val attention = devices.count { es.electrohobby3d.armor.model.DeviceText.problem(it) != null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Operación del perímetro", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (armed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (armed) "SISTEMA ARMADO" else "SISTEMA DESARMADO", style = MaterialTheme.typography.titleLarge)
                Text("Revisión $revision · ${if (updated.isBlank()) "sin telemetría" else updated}")
                Button(onClick = { onMode(if (armed) "disarmed" else "armed") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(if (armed) "Desarmar el sistema" else "Armar el sistema") }
            }
        }
        Card(Modifier.fillMaxWidth().clickable(onClick = onAlarms), colors = CardDefaults.cardColors(containerColor = if (pendingAlarms > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp)) { Text(if (pendingAlarms == 0) "Sin alarmas pendientes" else "$pendingAlarms alarmas necesitan atención", style = MaterialTheme.typography.titleMedium); Text("Toca para verlas y confirmarlas", style = MaterialTheme.typography.labelSmall) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Nodos", nodes.size.toString(), Modifier.weight(1f))
            Metric("Cámaras", if (camerasDown > 0) "$cameras ($camerasDown sin respuesta)" else cameras.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Dispositivos", devices.size.toString(), Modifier.weight(1f).clickable(onClick = onDevices))
            Metric("Requieren atención", attention.toString(), Modifier.weight(1f).clickable(onClick = onDevices))
        }
        RadarPanel(nodes)
        Button(onClick = onRefresh, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Actualizar estado y cámaras") }
        Text("Armar y desarmar quedan registrados en el servidor con tu nombre de usuario.", style = MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) = Card(modifier) { Column(Modifier.padding(14.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium); Text(label) } }

@Composable private fun CamerasPanel(cameras: List<CameraView>, grid: Int, onGrid: (Int) -> Unit, origin: String, viewModel: ArmorViewModel, authenticated: Boolean, recordingIds: Set<String>, health: Map<String, String>, onExpand: (CameraView) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Monitor IP", style = MaterialTheme.typography.headlineSmall); GridPicker(grid, onGrid) }
        if (origin.isBlank()) Text("Configura y conecta ARMOR-SERVER primero.", Modifier.padding(top = 18.dp))
        else if (cameras.isEmpty()) Text("No hay cámaras configuradas en el servidor.", Modifier.padding(top = 18.dp))
        else LazyVerticalGrid(columns = GridCells.Fixed(if (grid <= 2) grid else 2), modifier = Modifier.fillMaxSize().padding(top = 8.dp), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(cameras.take(grid), key = { it.id }) { camera -> CameraTile(camera, origin, viewModel, authenticated, recordingIds.contains(camera.id), health[camera.id], onExpand) } }
    }
}

@Composable private fun GridPicker(grid: Int, choose: (Int) -> Unit) { var open by remember { mutableStateOf(false) }; Box { AssistChip(onClick = { open = true }, label = { Text("$grid vistas") }); DropdownMenu(expanded = open, onDismissRequest = { open = false }) { listOf(1, 2, 4, 6, 8, 9, 12, 16).forEach { amount -> DropdownMenuItem(text = { Text("$amount vistas") }, onClick = { choose(amount); open = false }) } } } }

@Composable private fun CameraTile(camera: CameraView, origin: String, viewModel: ArmorViewModel, operatorReady: Boolean, recording: Boolean, health: String?, expand: (CameraView) -> Unit) {
    var ptzOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().height(236.dp).clickable(enabled = camera.configured) { expand(camera) }) { Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { if (health == "offline") Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cámara sin respuesta", color = MaterialTheme.colorScheme.error) } else if (camera.configured && camera.liveVideoAvailable) MjpegFeed(viewModel.mjpegUrl(origin, camera), Modifier.fillMaxSize()) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (camera.configured) "Vídeo no disponible" else "Cámara sin configurar") }
            if (ptzOpen) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) { PtzPad(enabled = operatorReady && camera.configured, action = { viewModel.ptz(origin, camera, it) }, key = 36.dp) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(camera.name, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(camera.host, style = MaterialTheme.typography.labelSmall) }; TextButton(onClick = { ptzOpen = !ptzOpen }, enabled = operatorReady && camera.configured) { Text(if (ptzOpen) "PTZ ✕" else "PTZ") }; TextButton(onClick = { expand(camera) }, enabled = camera.configured) { Text("⛶") }; TextButton(onClick = { viewModel.snapshot(origin, camera) }, enabled = operatorReady && camera.configured) { Text("◉") }; TextButton(onClick = { viewModel.toggleRecording(origin, camera) }, enabled = operatorReady && camera.configured) { Text(if (recording) "■" else "●") } }
    } }
}

@Composable private fun FullscreenCamera(camera: CameraView, origin: String, viewModel: ArmorViewModel, unlocked: Boolean, recording: Boolean, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(camera.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.fillMaxWidth().height(300.dp)) { if (camera.liveVideoAvailable) MjpegFeed(viewModel.mjpegUrl(origin, camera), Modifier.fillMaxSize()) else Text("Vídeo no disponible") }; PtzPad(enabled = unlocked, action = { viewModel.ptz(origin, camera, it) }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { viewModel.snapshot(origin, camera) }, enabled = unlocked) { Text("Foto") }; Button(onClick = { viewModel.toggleRecording(origin, camera) }, enabled = unlocked) { Text(if (recording) "Detener" else "Grabar") } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
/**
 * A key that moves the camera while it is held: pressing sends the move and repeats it every second (the server
 * stops a move by itself after a couple of seconds), releasing sends a stop after at least 300 ms, so a tap
 * still moves the camera visibly.
 */
@Composable private fun HoldKey(label: String, command: String, enabled: Boolean, size: Dp, action: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(action)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(8.dp))
            .background(if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .pointerInput(enabled, command) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    val started = System.currentTimeMillis()
                    val repeat = scope.launch { while (true) { current(command); kotlinx.coroutines.delay(1_000) } }
                    try { tryAwaitRelease() } finally {
                        repeat.cancel()
                        scope.launch {
                            val wait = 300 - (System.currentTimeMillis() - started)
                            if (wait > 0) kotlinx.coroutines.delay(wait)
                            current("stop")
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline) }
}

@Composable private fun PtzPad(enabled: Boolean, action: (String) -> Unit, key: Dp = 48.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HoldKey("▲", "up", enabled, key, action)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HoldKey("◀", "left", enabled, key, action)
            Box(Modifier.size(key).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = enabled) { action("stop") }, contentAlignment = Alignment.Center) { Text("■") }
            HoldKey("▶", "right", enabled, key, action)
        }
        HoldKey("▼", "down", enabled, key, action)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { HoldKey("−", "zoomOut", enabled, key, action); HoldKey("+", "zoomIn", enabled, key, action) }
    }
}

@Composable private fun EvidencePanel(items: List<MediaItem>, cameras: List<CameraView>, origin: String, viewModel: ArmorViewModel, unlocked: Boolean) {
    Column(Modifier.fillMaxSize()) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Evidencias", style = MaterialTheme.typography.headlineSmall); Button(onClick = { viewModel.loadMedia(origin) }, enabled = unlocked) { Text("Actualizar") } }; if (!unlocked) Text("Desbloquea los controles de operador en Conexión para acceder a fotos y grabaciones.", Modifier.padding(top = 12.dp)) else if (items.isEmpty()) Text("No hay fotos ni grabaciones guardadas.", Modifier.padding(top = 12.dp)) else LazyVerticalGrid(columns = GridCells.Fixed(1), modifier = Modifier.fillMaxSize().padding(top = 8.dp)) { items(items, key = { it.id }) { item -> val name = cameras.firstOrNull { it.id == item.cameraId }?.name ?: item.cameraId; Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(if (item.kind == "snapshot") "FOTO · $name" else "VÍDEO · $name"); Text(item.createdAt, style = MaterialTheme.typography.labelSmall) }; TextButton(onClick = { viewModel.deleteMedia(origin, item) }) { Text("Borrar") } } } } } }
}

@Composable private fun RadarPanel(nodes: List<FieldNode>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Radar y telemetría", style = MaterialTheme.typography.titleMedium)
        if (nodes.isEmpty()) Text("Aún no llega telemetría de nodos.")
        else nodes.forEach { node ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(node.id); Text(if (node.online) "en línea · ${node.tracks} objetivos" else "fuera de línea o en silencio", style = MaterialTheme.typography.labelSmall) }
                    Text(node.alert.uppercase())
                }
            }
        }
        Text("Los datos se proyectan desde ARMOR-SERVER; el teléfono no sustituye sensores ni decisiones de seguridad.", style = MaterialTheme.typography.bodySmall)
    }
}


@Composable private fun HistoryPanel(events: List<ArmorEvent>, onMore: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Historial", style = MaterialTheme.typography.headlineSmall)
        Text("Cada cambio de nivel de alerta, de estado de nodo, cámara, dispositivo y alarma, y de modo, del más reciente al más antiguo.", style = MaterialTheme.typography.bodySmall)
        if (events.isEmpty()) Text("Todavía no hay eventos registrados.")
        else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            listItems(events, key = { it.id }) { event ->
                val high = (event.type == "alert" && event.to == "high") || (event.type == "alarm" && event.to == "raised" && event.severity == "critical")
                Card(Modifier.fillMaxWidth(), colors = if (high) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer) else CardDefaults.cardColors()) {
                    Column(Modifier.padding(12.dp)) { Text(event.at.replace('T', ' ').removeSuffix("Z").take(19), style = MaterialTheme.typography.labelSmall); Text(AlarmPolicy.describe(event)) }
                }
            }
            if (events.size >= 50) item { TextButton(onClick = onMore) { Text("Cargar eventos anteriores") } }
        }
    }
}
