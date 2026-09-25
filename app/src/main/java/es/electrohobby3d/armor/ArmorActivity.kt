// ARMOR-ANDROID-CONTROL - mobile operator client.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
    Overview("Estado", "◈"), Cameras("Cámaras", "◉"), Evidence("Grabaciones", "▣"), History("Historial", "≡"), Settings("Conexión", "⚙")
}

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
    var grid by rememberSaveable { mutableIntStateOf(4) }
    var expanded by remember { mutableStateOf<CameraView?>(null) }
    var about by remember { mutableStateOf(false) }
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
        if (!state.authenticated) {
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
            )
        } else {
        Scaffold(
            topBar = { TopAppBar(title = { Column { Text("A.R.M.O.R."); Text("MOBILE CONTROL", style = MaterialTheme.typography.labelSmall) } }, actions = { Text("REV ${state.snapshot.revision}", style = MaterialTheme.typography.labelMedium); IconButton(onClick = { about = true }) { Text("i") } }) },
            bottomBar = { NavigationBar { MobileSection.entries.forEach { item -> NavigationBarItem(selected = section == item, onClick = { section = item }, icon = { Text(item.glyph) }, label = { Text(item.label) }) } } },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                state.message?.let { message -> AssistChip(onClick = {}, label = { Text(message, maxLines = 2, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                when (section) {
                    MobileSection.Overview -> OverviewPanel(state.snapshot.mode, state.snapshot.revision, state.snapshot.updatedAt, state.snapshot.nodes, state.cameras.size, state.cameraHealth.count { it.value == "offline" }, onRefresh = { viewModel.refresh(currentOrigin) }, enabled = validOrigin && !state.loading)
                    MobileSection.Cameras -> CamerasPanel(state.cameras, grid, onGrid = { grid = it }, origin = currentOrigin, viewModel = viewModel, authenticated = state.authenticated, recordingIds = state.media.activeCameraIds, health = state.cameraHealth, onExpand = { expanded = it })
                    MobileSection.Evidence -> EvidencePanel(state.media.items, state.cameras, origin = currentOrigin, viewModel = viewModel, unlocked = state.authenticated)
                    MobileSection.History -> HistoryPanel(state.events, onMore = { viewModel.loadOlderEvents(currentOrigin) })
                    MobileSection.Settings -> SessionSettingsPanel(origin, { origin = it }, validOrigin, state.authenticated, onConnect = { preferences.edit().putString("origin", currentOrigin).apply(); viewModel.refresh(currentOrigin) }, onMedia = { viewModel.loadMedia(currentOrigin) }, onLogout = { AlarmWatcherService.stop(context); viewModel.logout(currentOrigin) },
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
        expanded?.let { camera -> FullscreenCamera(camera, currentOrigin, viewModel, state.authenticated, state.media.activeCameraIds.contains(camera.id), onDismiss = { expanded = null }) }
        }
        if (about) AlertDialog(onDismissRequest = { about = false }, confirmButton = { TextButton(onClick = { about = false }) { Text("Cerrar") } }, title = { Text("A.R.M.O.R. Mobile Control") }, text = { Text("v${BuildConfig.VERSION_NAME}\nCliente móvil para el estado, historial de eventos, alarmas, cámaras, PTZ y biblioteca de evidencias de ARMOR-SERVER. Las contraseñas de cámaras nunca salen del servidor.") })
    }
}

@Composable
private fun LoginPanel(
    host: String, port: String, username: String, password: String, busy: Boolean, message: String?,
    onHost: (String) -> Unit, onPort: (String) -> Unit, onUsername: (String) -> Unit,
    onPassword: (String) -> Unit, onLogin: () -> Unit, enabled: Boolean,
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
        }
    }
}

@Composable private fun OverviewPanel(mode: String, revision: Long, updated: String, nodes: List<FieldNode>, cameras: Int, camerasDown: Int, onRefresh: () -> Unit, enabled: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Operación del perímetro", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(if (mode == "armed") "SISTEMA ARMADO" else "SISTEMA DESARMADO", style = MaterialTheme.typography.titleLarge); Text("Revisión $revision · ${if (updated.isBlank()) "sin telemetría" else updated}") } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Metric("Nodos", nodes.size.toString(), Modifier.weight(1f)); Metric("Cámaras", if (camerasDown > 0) "$cameras ($camerasDown sin respuesta)" else cameras.toString(), Modifier.weight(1f)) }
        RadarPanel(nodes)
        Button(onClick = onRefresh, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Actualizar estado y cámaras") }
        Text("El armado queda deliberadamente fuera del móvil hasta validar físicamente el flujo de seguridad.", style = MaterialTheme.typography.bodySmall)
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
    Card(Modifier.fillMaxWidth().height(236.dp).clickable(enabled = camera.configured) { expand(camera) }) { Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { if (health == "offline") Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cámara sin respuesta", color = MaterialTheme.colorScheme.error) } else if (camera.configured && camera.liveVideoAvailable) MjpegFeed(viewModel.mjpegUrl(origin, camera), Modifier.fillMaxSize()) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (camera.configured) "Vídeo no disponible" else "Cámara sin configurar") } }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(camera.name, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(camera.host, style = MaterialTheme.typography.labelSmall) }; TextButton(onClick = { expand(camera) }, enabled = camera.configured) { Text("⛶") }; TextButton(onClick = { viewModel.snapshot(origin, camera) }, enabled = operatorReady && camera.configured) { Text("◉") }; TextButton(onClick = { viewModel.toggleRecording(origin, camera) }, enabled = operatorReady && camera.configured) { Text(if (recording) "■" else "●") } }
    } }
}

@Composable private fun FullscreenCamera(camera: CameraView, origin: String, viewModel: ArmorViewModel, unlocked: Boolean, recording: Boolean, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(camera.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.fillMaxWidth().height(300.dp)) { if (camera.liveVideoAvailable) MjpegFeed(viewModel.mjpegUrl(origin, camera), Modifier.fillMaxSize()) else Text("Vídeo no disponible") }; PtzPad(enabled = unlocked, action = { viewModel.ptz(origin, camera, it) }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { viewModel.snapshot(origin, camera) }, enabled = unlocked) { Text("Foto") }; Button(onClick = { viewModel.toggleRecording(origin, camera) }, enabled = unlocked) { Text(if (recording) "Detener" else "Grabar") } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
@Composable private fun PtzPad(enabled: Boolean, action: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("PTZ", style = MaterialTheme.typography.labelLarge); Row { Spacer(Modifier.width(48.dp)); TextButton(onClick = { action("up") }, enabled = enabled) { Text("▲") }; Spacer(Modifier.width(48.dp)) }; Row { TextButton(onClick = { action("left") }, enabled = enabled) { Text("◀") }; TextButton(onClick = { action("stop") }, enabled = enabled) { Text("■") }; TextButton(onClick = { action("right") }, enabled = enabled) { Text("▶") } }; Row { Spacer(Modifier.width(48.dp)); TextButton(onClick = { action("down") }, enabled = enabled) { Text("▼") }; Spacer(Modifier.width(48.dp)) }; Row { TextButton(onClick = { action("zoomOut") }, enabled = enabled) { Text("−") }; TextButton(onClick = { action("zoomIn") }, enabled = enabled) { Text("+") } } } }

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
        Text("Cada cambio de nivel de alerta, de estado de nodo y cámara y de modo, del más reciente al más antiguo.", style = MaterialTheme.typography.bodySmall)
        if (events.isEmpty()) Text("Todavía no hay eventos registrados.")
        else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            listItems(events, key = { it.id }) { event ->
                val high = event.type == "alert" && event.to == "high"
                Card(Modifier.fillMaxWidth(), colors = if (high) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer) else CardDefaults.cardColors()) {
                    Column(Modifier.padding(12.dp)) { Text(event.at.replace('T', ' ').removeSuffix("Z").take(19), style = MaterialTheme.typography.labelSmall); Text(AlarmPolicy.describe(event)) }
                }
            }
            if (events.size >= 50) item { TextButton(onClick = onMore) { Text("Cargar eventos anteriores") } }
        }
    }
}
