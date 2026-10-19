// ARMOR-ANDROID-CONTROL - the "More" menu and what is behind it: recordings, history, connection settings.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.model.MediaItem
import es.electrohobby3d.armor.update.AppUpdateState

/** What the "More" tab can show; null is the menu itself. */
enum class MoreTab { Nodes, Voice, Weather, Solar, Batteries, Electrical, AlarmPanels, Network, Services, NodeHealth, Evidence, History, Security, Updates, Settings }

@Composable
fun MoreMenu(onOpen: (MoreTab) -> Unit, onNodeSetup: () -> Unit, onAbout: () -> Unit, onLogout: () -> Unit, updateAvailable: Boolean = false, isAdmin: Boolean = false) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenTitle(Icons.Filled.GridView, tr("Más"))
        MenuRow(Icons.Filled.Tune, tr("Configurar nodos"), tr("El configurador de un nodo (radar, solar, eléctrico) dentro de la app: todos sus parámetros")) { onOpen(MoreTab.Nodes) }
        MenuRow(Icons.Filled.Mic, tr("Asistente"), tr("Escribe o dí órdenes: armar, desarmar, el estado o silenciar la alarma")) { onOpen(MoreTab.Voice) }
        MenuRow(Icons.Filled.Cloud, tr("Meteorología"), tr("El tiempo del lugar que elijas: ahora, por horas, diez días, aire y polen")) { onOpen(MoreTab.Weather) }
        MenuRow(Icons.Filled.WbSunny, tr("Solar"), tr("Inversores y baterías")) { onOpen(MoreTab.Solar) }
        if (isAdmin) MenuRow(Icons.Filled.BatteryChargingFull, tr("Mantenimiento de baterías"), tr("Reloj, contadores, reinicio, capacidad y salidas de una batería Pylontech")) { onOpen(MoreTab.Batteries) }
        MenuRow(Icons.Filled.ElectricBolt, tr("Eléctrica"), tr("Red, circuitos y buses de continua")) { onOpen(MoreTab.Electrical) }
        MenuRow(Icons.Filled.Shield, tr("Centrales de alarma"), tr("Nodos ARMOR-ALARM: zonas, estado y armado")) { onOpen(MoreTab.AlarmPanels) }
        MenuRow(Icons.Filled.Router, tr("Red"), tr("Internet, dispositivos y cambios de la red local")) { onOpen(MoreTab.Network) }
        MenuRow(Icons.Filled.Dns, tr("Servicios"), tr("Programas del sistema y nodos de campo, activos o no")) { onOpen(MoreTab.Services) }
        MenuRow(Icons.Filled.MonitorHeart, tr("Salud de los nodos"), tr("Memoria, enlace, reinicios y errores de cada nodo")) { onOpen(MoreTab.NodeHealth) }
        MenuRow(Icons.Filled.VideoLibrary, tr("Grabaciones"), tr("Fotos y vídeos guardados")) { onOpen(MoreTab.Evidence) }
        MenuRow(Icons.Filled.History, tr("Historial"), tr("Todo lo que ha pasado")) { onOpen(MoreTab.History) }
        MenuRow(Icons.Filled.Lock, tr("Seguridad"), tr("Segundo factor, sesiones abiertas y acceso desde fuera de casa.")) { onOpen(MoreTab.Security) }
        MenuRow(Icons.Filled.Bluetooth, tr("Configurar un nodo"), tr("Prepara un nodo nuevo por Bluetooth"), onClick = onNodeSetup)
        MenuRow(Icons.Filled.SystemUpdate, tr("Actualizaciones"), if (updateAvailable) tr("Hay una versión nueva") else tr("Buscar una versión nueva en GitHub"), tint = if (updateAvailable) ArmorColors.Cyan else ArmorColors.Cyan) { onOpen(MoreTab.Updates) }
        MenuRow(Icons.Filled.Settings, tr("Ajustes"), tr("Servidor y avisos")) { onOpen(MoreTab.Settings) }
        MenuRow(Icons.Filled.Info, tr("Acerca de"), tr("Versión y créditos"), onClick = onAbout)
        MenuRow(Icons.AutoMirrored.Filled.Logout, tr("Cerrar sesión"), null, tint = ArmorColors.Alert, onClick = onLogout)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String?, tint: Color = ArmorColors.Cyan, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon, tint, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (tint == ArmorColors.Alert) ArmorColors.Alert else ArmorColors.Text)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ArmorColors.Muted)
        }
    }
}

@Composable
fun SubScreen(icon: ImageVector, title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(icon, title) {
            actions()
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Volver"), tint = ArmorColors.Cyan) }
        }
        content()
    }
}

/** The history with its two lists: what happened, and the devices and apps that connect to the server (reading only). */
@Composable
fun HistoryWithConnections(events: List<ArmorEvent>, connections: List<es.electrohobby3d.armor.model.ConnectionClient>?, onMore: () -> Unit, poll: suspend () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("Eventos")) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("Conexiones")) })
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) HistoryScreen(events, onMore) else ConnectionsScreen(connections, poll)
    }
}

/** Every device or app the server has seen, with when it was last seen and its open sessions. An administrator also gets the address; nothing secret is ever in the list. */
@Composable
fun ConnectionsScreen(clients: List<es.electrohobby3d.armor.model.ConnectionClient>?, poll: suspend () -> Unit) {
    LaunchedEffect(Unit) { while (true) { poll(); kotlinx.coroutines.delay(10_000) } }
    if (clients == null) { EmptyNote(Icons.Filled.Devices, tr("Conexiones"), tr("No se pudo leer la lista de conexiones.")); return }
    if (clients.isEmpty()) { EmptyNote(Icons.Filled.Devices, tr("Conexiones"), tr("Ningún cliente se ha conectado todavía.")); return }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Text(tr("Dispositivos y apps que se conectan"), style = MaterialTheme.typography.titleSmall, color = ArmorColors.Muted) }
        items(clients, key = { it.id }) { client ->
            Panel(Modifier.fillMaxWidth(), tint = if (client.loginsFailed > 0) ArmorColors.Amber else null) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(clientIcon(client.kind), if (client.activeSessions > 0) ArmorColors.Ok else ArmorColors.Cyan, size = 36.dp)
                    Column {
                        Text(clientTitle(client), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(trf("Última vez: %s", Friendly.dayAndClock(client.lastSeen)), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                        Text(
                            listOf(trf("Sesiones abiertas: %d", client.activeSessions), trf("Inicios de sesión: %d", client.loginsOk), trf("Rechazados: %d", client.loginsFailed)).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted,
                        )
                        client.ip?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
                    }
                }
            }
        }
        if (clients.none { it.ip != null }) item { Text(tr("Las direcciones solo las ven los administradores."), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
    }
}

private fun clientTitle(client: es.electrohobby3d.armor.model.ConnectionClient): String = listOfNotNull(
    when (client.kind) { "studio" -> tr("Studio"); "android" -> tr("App Android"); "node" -> tr("Panel de nodo"); "voice" -> tr("Servicio de voz"); else -> tr("Otro cliente") },
    client.label, client.user, client.appVersion?.let { trf("Versión %s", it) },
).joinToString(" · ")

private fun clientIcon(kind: String): ImageVector = when (kind) { "android" -> Icons.Filled.PhoneAndroid; "studio" -> Icons.Filled.Computer; "node" -> Icons.Filled.Sensors; "voice" -> Icons.Filled.Mic; else -> Icons.Filled.Devices }

@Composable
fun HistoryScreen(events: List<ArmorEvent>, onMore: () -> Unit) {
    if (events.isEmpty()) { EmptyNote(Icons.Filled.History, tr("Todavía no ha pasado nada"), tr("Aquí verás los cambios de estado, las alarmas y las órdenes.")); return }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(events, key = { it.id }) { event ->
            val high = (event.type == "alert" && event.to == "high") || (event.type == "alarm" && event.to == "raised" && event.severity == "critical")
            Panel(Modifier.fillMaxWidth(), tint = if (high) ArmorColors.Alert else null) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(if (high) Icons.Filled.Warning else eventIcon(event.type), if (high) ArmorColors.Alert else ArmorColors.Cyan, size = 36.dp)
                    Column {
                        Text(AlarmPolicy.describe(event), style = MaterialTheme.typography.bodyMedium)
                        Text(Friendly.dayAndClock(event.at), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                    }
                }
            }
        }
        if (events.size >= 50) item { TextButton(onClick = onMore) { Icon(Icons.Filled.ExpandMore, null); Spacer(Modifier.width(8.dp)); Text(tr("Ver anteriores")) } }
    }
}

private fun eventIcon(type: String): ImageVector = when (type) {
    "alarm" -> Icons.Filled.NotificationsActive; "node" -> Icons.Filled.Sensors; "camera" -> Icons.Filled.Videocam; "device" -> Icons.Filled.Power; "mode" -> Icons.Filled.Shield; else -> Icons.Filled.History
}

@Composable
fun EvidenceScreen(items: List<MediaItem>, cameras: List<CameraView>, origin: String, viewModel: ArmorViewModel, unlocked: Boolean, canDelete: Boolean = true) {
    if (!unlocked) { EmptyNote(Icons.Filled.Lock, tr("Inicia sesión"), tr("Las grabaciones se ven con la sesión abierta.")); return }
    if (items.isEmpty()) { EmptyNote(Icons.Filled.VideoLibrary, tr("No hay grabaciones"), tr("Las fotos y vídeos que hagas con las cámaras aparecerán aquí.")); return }
    var viewing by remember { mutableStateOf<MediaItem?>(null) }
    var deleting by remember { mutableStateOf<MediaItem?>(null) }
    viewing?.let { MediaViewerDialog(it, viewModel.mediaUrl(origin, it), viewModel.sessionCookie(origin)) { viewing = null } }
    deleting?.let { doomed ->
        AlertDialog(
            onDismissRequest = { deleting = null }, containerColor = ArmorColors.SurfaceRaised,
            title = { Text(tr("¿Borrar la grabación?")) }, text = { Text(tr("Se borra del servidor y no se puede deshacer.")) },
            confirmButton = { TextButton(onClick = { viewModel.deleteMedia(origin, doomed); deleting = null }) { Text(tr("Borrar"), color = ArmorColors.Alert) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Cancelar")) } },
        )
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items, key = { it.id }) { item ->
            val name = cameras.firstOrNull { it.id == item.cameraId }?.name ?: item.cameraId
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(if (item.kind == "snapshot") Icons.Filled.PhotoCamera else Icons.Filled.Movie, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text("${if (item.kind == "snapshot") tr("Foto") else tr("Vídeo")} · ${Friendly.dayAndClock(item.createdAt)}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    }
                    IconButton(onClick = { viewing = item }) { Icon(Icons.Filled.PlayArrow, contentDescription = tr("Ver"), tint = ArmorColors.Cyan) }
                    // somebody who may only look is not offered the button (the server would refuse it anyway)
                    if (canDelete) IconButton(onClick = { deleting = item }) { Icon(Icons.Filled.Delete, contentDescription = tr("Borrar"), tint = ArmorColors.Alert) }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(icon: ImageVector, title: String, text: String) {
    Panel(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, ArmorColors.Muted, size = 64.dp)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
        }
    }
}

/** The signed, operator-approved GitHub Release update path: check, review notes, download and hand off to Android's installer. */
@Composable
fun UpdateScreen(state: AppUpdateState, onCheck: () -> Unit, onDownload: (es.electrohobby3d.armor.update.AvailableUpdate) -> Unit, onOpenInstallSettings: () -> Unit, checkOnStart: Boolean = false, onCheckOnStart: (Boolean) -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(trf("Versión instalada: %s", BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = ArmorColors.Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Buscar actualizaciones al iniciar"))
                        Text(tr("Desactivado por defecto. La comprobación consulta GitHub; el tiempo y el radar usan servicios meteorológicos externos."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    }
                    Switch(checked = checkOnStart, onCheckedChange = onCheckOnStart)
                }
                when (state) {
                    is AppUpdateState.Idle, is AppUpdateState.UpToDate -> {
                        if (state is AppUpdateState.UpToDate) Text(tr("Ya tienes la última versión."), color = ArmorColors.Ok, fontWeight = FontWeight.SemiBold)
                        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text(tr("Buscar actualizaciones")) }
                    }
                    is AppUpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp); Text(tr("Buscando en GitHub…"))
                    }
                    is AppUpdateState.Available -> UpdateAvailableContent(state.update, onDownload = { onDownload(state.update) })
                    is AppUpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(trf("Descargando… %s%%", state.progress ?: 0))
                        if (state.progress != null) LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is AppUpdateState.InstallPermissionRequired -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(tr("Para instalar la actualización, Android necesita tu permiso para instalar aplicaciones desde esta app."))
                        Button(onClick = onOpenInstallSettings, modifier = Modifier.fillMaxWidth()) { Text(tr("Conceder permiso")) }
                    }
                    is AppUpdateState.Installing -> Text(tr("Abriendo el instalador de Android…"))
                    is AppUpdateState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.message, color = ArmorColors.Alert)
                        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text(tr("Buscar actualizaciones")) }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableContent(update: es.electrohobby3d.armor.update.AvailableUpdate, onDownload: () -> Unit) {
    var showReleaseNotes by remember { mutableStateOf(false) }
    if (showReleaseNotes) ReleaseNotesDialog(update = update, onDismiss = { showReleaseNotes = false })
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Filled.SystemUpdate, ArmorColors.Cyan, size = 40.dp)
            Text(trf("Nueva versión disponible: %s", update.version), fontWeight = FontWeight.SemiBold, color = ArmorColors.Cyan)
        }
        if (update.notes.isNotBlank()) OutlinedButton(onClick = { showReleaseNotes = true }, modifier = Modifier.fillMaxWidth()) { Text(tr("Ver novedades")) }
        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp)); Text(tr("Descargar e instalar")) }
    }
}

@Composable
private fun ReleaseNotesDialog(update: es.electrohobby3d.armor.update.AvailableUpdate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArmorColors.SurfaceRaised,
        title = { Text(update.releaseName.ifBlank { update.version.toString() }) },
        text = { Text(update.notes, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
    )
}

@Composable
fun SettingsScreen(
    origin: String, authenticated: Boolean, onConnect: (String) -> Unit,
    watching: Boolean, onWatching: (Boolean) -> Unit,
) {
    // What is typed here is a draft: the address the app talks to changes only when the person taps "Conectar" (it used to change with every key, restarting the polling against a half-typed address).
    var draft by rememberSaveable(origin) { mutableStateOf(origin) }
    val valid = ServerEndpoint.parse(draft) != null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Filled.Dns, null, tint = ArmorColors.Cyan); Text(tr("Servidor"), fontWeight = FontWeight.SemiBold) }
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Dirección")) },
                    isError = draft.isNotBlank() && !valid, supportingText = { Text(if (draft.isBlank() || valid) tr("Por ejemplo http://192.168.0.50:8080") else tr("La dirección no es válida")) },
                )
                Button(onClick = { onConnect(draft) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text(tr("Conectar")) }
            }
        }
        val soundContext = androidx.compose.ui.platform.LocalContext.current
        var speak by rememberSaveable { mutableStateOf(AlarmNotifier.speaksAlarms(soundContext)) }
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Sonidos y voz de las alarmas"), fontWeight = FontWeight.SemiBold)
                Text(tr("Las alarmas críticas suenan como una alarma, las altas como un aviso fuerte y los avisos como una notificación. Cada gravedad tiene su propio canal: puedes elegir su sonido, su vibración o silenciarla en los ajustes del móvil."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                Button(onClick = {
                    AlarmNotifier.ensureChannels(soundContext)
                    soundContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, soundContext.packageName).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Text(tr("Elegir el sonido de cada gravedad")) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Decir la alarma en voz alta"))
                        Text(tr("Con la voz del móvil, para las críticas y las altas."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    }
                    Switch(checked = speak, onCheckedChange = { speak = it; AlarmNotifier.setSpeaksAlarms(soundContext, it) })
                }
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            val languageContext = androidx.compose.ui.platform.LocalContext.current
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Idioma"), fontWeight = FontWeight.SemiBold)
                Text(tr("El idioma de la app. Si no eliges uno, usa el del móvil cuando es uno de estos ocho."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                Tr.languages.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (code, name) ->
                            if (Tr.language == code) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(name) }
                            else androidx.compose.material3.OutlinedButton(onClick = { Tr.choose(languageContext, code) }, modifier = Modifier.weight(1f)) { Text(name) }
                        }
                    }
                }
            }
        }
        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.NotificationsActive, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(tr("Avisos con la app cerrada"), fontWeight = FontWeight.SemiBold)
                    Text(tr("Te avisa de alertas altas, cámaras sin señal y alarmas siempre; de nodos caídos o en silencio, solo con el sistema armado."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                Switch(checked = watching, onCheckedChange = onWatching, enabled = authenticated)
            }
        }
    }
}
