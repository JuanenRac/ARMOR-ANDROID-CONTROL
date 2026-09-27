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
enum class MoreTab { Solar, Electrical, Network, Evidence, History, Updates, Settings }

@Composable
fun MoreMenu(onOpen: (MoreTab) -> Unit, onNodeSetup: () -> Unit, onAbout: () -> Unit, onLogout: () -> Unit, updateAvailable: Boolean = false) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenTitle(Icons.Filled.GridView, "Más")
        MenuRow(Icons.Filled.WbSunny, "Solar", "Inversores y baterías") { onOpen(MoreTab.Solar) }
        MenuRow(Icons.Filled.ElectricBolt, "Eléctrica", "Red, circuitos y buses de continua") { onOpen(MoreTab.Electrical) }
        MenuRow(Icons.Filled.Router, "Red", "Internet, dispositivos y cambios de la red local") { onOpen(MoreTab.Network) }
        MenuRow(Icons.Filled.VideoLibrary, "Grabaciones", "Fotos y vídeos guardados") { onOpen(MoreTab.Evidence) }
        MenuRow(Icons.Filled.History, "Historial", "Todo lo que ha pasado") { onOpen(MoreTab.History) }
        MenuRow(Icons.Filled.Bluetooth, "Configurar un nodo", "Prepara un nodo nuevo por Bluetooth", onClick = onNodeSetup)
        MenuRow(Icons.Filled.SystemUpdate, "Actualizaciones", if (updateAvailable) "Hay una versión nueva" else "Buscar una versión nueva en GitHub", tint = if (updateAvailable) ArmorColors.Cyan else ArmorColors.Cyan) { onOpen(MoreTab.Updates) }
        MenuRow(Icons.Filled.Settings, "Ajustes", "Servidor y avisos") { onOpen(MoreTab.Settings) }
        MenuRow(Icons.Filled.Info, "Acerca de", "Versión y créditos", onClick = onAbout)
        MenuRow(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", null, tint = ArmorColors.Alert, onClick = onLogout)
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ArmorColors.Cyan) }
        }
        content()
    }
}

@Composable
fun HistoryScreen(events: List<ArmorEvent>, onMore: () -> Unit) {
    if (events.isEmpty()) { EmptyNote(Icons.Filled.History, "Todavía no ha pasado nada", "Aquí verás los cambios de estado, las alarmas y las órdenes."); return }
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
        if (events.size >= 50) item { TextButton(onClick = onMore) { Icon(Icons.Filled.ExpandMore, null); Spacer(Modifier.width(8.dp)); Text("Ver anteriores") } }
    }
}

private fun eventIcon(type: String): ImageVector = when (type) {
    "alarm" -> Icons.Filled.NotificationsActive; "node" -> Icons.Filled.Sensors; "camera" -> Icons.Filled.Videocam; "device" -> Icons.Filled.Power; "mode" -> Icons.Filled.Shield; else -> Icons.Filled.History
}

@Composable
fun EvidenceScreen(items: List<MediaItem>, cameras: List<CameraView>, origin: String, viewModel: ArmorViewModel, unlocked: Boolean) {
    if (!unlocked) { EmptyNote(Icons.Filled.Lock, "Inicia sesión", "Las grabaciones se ven con la sesión abierta."); return }
    if (items.isEmpty()) { EmptyNote(Icons.Filled.VideoLibrary, "No hay grabaciones", "Las fotos y vídeos que hagas con las cámaras aparecerán aquí."); return }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items, key = { it.id }) { item ->
            val name = cameras.firstOrNull { it.id == item.cameraId }?.name ?: item.cameraId
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(if (item.kind == "snapshot") Icons.Filled.PhotoCamera else Icons.Filled.Movie, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text("${if (item.kind == "snapshot") "Foto" else "Vídeo"} · ${Friendly.dayAndClock(item.createdAt)}", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                    }
                    IconButton(onClick = { viewModel.deleteMedia(origin, item) }) { Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = ArmorColors.Alert) }
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
fun UpdateScreen(state: AppUpdateState, onCheck: () -> Unit, onDownload: (es.electrohobby3d.armor.update.AvailableUpdate) -> Unit, onOpenInstallSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Versión instalada: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = ArmorColors.Muted)
                when (state) {
                    is AppUpdateState.Idle, is AppUpdateState.UpToDate -> {
                        if (state is AppUpdateState.UpToDate) Text("Ya tienes la última versión.", color = ArmorColors.Ok, fontWeight = FontWeight.SemiBold)
                        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Buscar actualizaciones") }
                    }
                    is AppUpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp); Text("Buscando en GitHub…")
                    }
                    is AppUpdateState.Available -> UpdateAvailableContent(state.update, onDownload = { onDownload(state.update) })
                    is AppUpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Descargando… ${state.progress ?: 0}%")
                        if (state.progress != null) LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is AppUpdateState.InstallPermissionRequired -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Para instalar la actualización, Android necesita tu permiso para instalar aplicaciones desde esta app.")
                        Button(onClick = onOpenInstallSettings, modifier = Modifier.fillMaxWidth()) { Text("Conceder permiso") }
                    }
                    is AppUpdateState.Installing -> Text("Abriendo el instalador de Android…")
                    is AppUpdateState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.message, color = ArmorColors.Alert)
                        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Buscar actualizaciones") }
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
            Text("Nueva versión disponible: ${update.version}", fontWeight = FontWeight.SemiBold, color = ArmorColors.Cyan)
        }
        if (update.notes.isNotBlank()) OutlinedButton(onClick = { showReleaseNotes = true }, modifier = Modifier.fillMaxWidth()) { Text("Ver novedades") }
        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp)); Text("Descargar e instalar") }
    }
}

@Composable
private fun ReleaseNotesDialog(update: es.electrohobby3d.armor.update.AvailableUpdate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArmorColors.SurfaceRaised,
        title = { Text(update.releaseName.ifBlank { update.version.toString() }) },
        text = { Text(update.notes, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
fun SettingsScreen(
    origin: String, changeOrigin: (String) -> Unit, valid: Boolean, authenticated: Boolean, onConnect: () -> Unit,
    watching: Boolean, onWatching: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Filled.Dns, null, tint = ArmorColors.Cyan); Text("Servidor", fontWeight = FontWeight.SemiBold) }
                OutlinedTextField(
                    value = origin, onValueChange = changeOrigin, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Dirección") },
                    isError = origin.isNotBlank() && !valid, supportingText = { Text(if (origin.isBlank() || valid) "Por ejemplo http://192.168.0.50:8080" else "La dirección no es válida") },
                )
                Button(onClick = onConnect, enabled = valid, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Conectar") }
            }
        }
        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.NotificationsActive, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text("Avisos con la app cerrada", fontWeight = FontWeight.SemiBold)
                    Text("Te avisa de alertas, cámaras sin señal y nodos caídos mientras el sistema está armado.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                }
                Switch(checked = watching, onCheckedChange = onWatching, enabled = authenticated)
            }
        }
    }
}
