// ARMOR-ANDROID-CONTROL - the screens around the app: the splash, the sign-in, the profile and the About box.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** The first second and a half: the mark appears, then the app. */
@Composable
fun ArmorSplash(onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    val leave = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(650))
        delay(650)
        leave.animateTo(0f, tween(350))
        onDone()
    }
    Box(
        Modifier.fillMaxSize().alpha(leave.value)
            .background(Brush.radialGradient(listOf(Color(0xFF0B3A47), ArmorColors.Background), radius = 900f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.alpha(appear.value).scale(0.85f + 0.15f * appear.value), horizontalAlignment = Alignment.CenterHorizontally) {
            ArmorLogo(112.dp)
            Spacer(Modifier.height(20.dp))
            Text("A.R.M.O.R.", fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = ArmorColors.Text)
            Spacer(Modifier.height(6.dp))
            Text("Seguridad de tu perímetro", color = ArmorColors.Muted, fontSize = 14.sp)
        }
    }
}

/** While the app tries the session kept from the last time: the sign-in is not shown for a moment and then taken away. */
@Composable
fun RestoringScreen() {
    Box(Modifier.fillMaxSize().background(ArmorColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ArmorLogo(72.dp)
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            Text("Conectando con tu servidor…", color = ArmorColors.Muted)
        }
    }
}

@Composable
fun LoginScreen(
    host: String, port: String, username: String, password: String, busy: Boolean, message: String?,
    onHost: (String) -> Unit, onPort: (String) -> Unit, onUsername: (String) -> Unit, onPassword: (String) -> Unit,
    onLogin: () -> Unit, canLogin: Boolean, onNodeSetup: () -> Unit, onAbout: () -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2A33), ArmorColors.Background, ArmorColors.Background)))) {
        IconButton(onClick = onAbout, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Filled.Info, contentDescription = "Acerca de", tint = ArmorColors.Muted)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            ArmorLogo(84.dp)
            Spacer(Modifier.height(14.dp))
            Text("A.R.M.O.R.", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Text("Seguridad de tu perímetro", color = ArmorColors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = host, onValueChange = onHost, modifier = Modifier.weight(1f), singleLine = true,
                            label = { Text("Servidor") }, placeholder = { Text("192.168.0.50") }, leadingIcon = { Icon(Icons.Filled.Dns, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                        OutlinedTextField(
                            value = port, onValueChange = { onPort(it.filter(Char::isDigit).take(5)) }, modifier = Modifier.width(96.dp), singleLine = true,
                            label = { Text("Puerto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    OutlinedTextField(
                        value = username, onValueChange = onUsername, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Usuario") }, leadingIcon = { Icon(Icons.Filled.Person, null) },
                    )
                    OutlinedTextField(
                        value = password, onValueChange = onPassword, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (visible) "Ocultar" else "Mostrar") } },
                    )
                    Button(onClick = onLogin, enabled = canLogin && !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.AutoMirrored.Filled.Login, null)
                        Spacer(Modifier.width(10.dp))
                        Text(if (busy) "Conectando…" else "Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Friendly.message(message)?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = ArmorColors.Alert, modifier = Modifier.size(20.dp))
                            Text(it, color = ArmorColors.Alert, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            FilledTonalButton(onClick = onNodeSetup, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Bluetooth, null)
                Spacer(Modifier.width(8.dp))
                Text("Configurar un nodo")
            }
            Spacer(Modifier.height(10.dp))
            Text("Tu contraseña no se guarda en el móvil. La sesión sí, cifrada, y se renueva mientras uses la app: no tendrás que volver a entrar salvo que caduque o la cierres.", color = ArmorColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ProfileDialog(username: String, server: String, connected: Boolean, watching: Boolean, onWatching: (Boolean) -> Unit, onLogout: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArmorColors.SurfaceRaised,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconBadge(Icons.Filled.Person, size = 44.dp); Text("Mi cuenta") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                InfoRow(Icons.Filled.Person, "Usuario", username.ifBlank { "—" })
                InfoRow(Icons.Filled.Dns, "Servidor", server.ifBlank { "—" })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusDot(if (connected) ArmorColors.Ok else ArmorColors.Alert, 12.dp)
                    Text(if (connected) "Conectado" else "Sin conexión", fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = ArmorColors.Outline)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = ArmorColors.Cyan)
                    Column(Modifier.weight(1f)) { Text("Avisos con la app cerrada"); Text("Te avisa de alertas aunque no la tengas abierta.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted) }
                    Switch(checked = watching, onCheckedChange = onWatching)
                }
                FilledTonalButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3A1319), contentColor = Color(0xFFFFD9DD))) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }
        },
    )
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = ArmorColors.Cyan)
        Column { Text(label, style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted); Text(value, fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArmorColors.SurfaceRaised,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ArmorLogo(72.dp)
                Text("A.R.M.O.R.", fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("Versión ${BuildConfig.VERSION_NAME}", color = ArmorColors.Cyan, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Vigila tu perímetro desde el móvil: mira quién se acerca, recibe avisos, controla las luces y las sirenas, y ve tus cámaras en directo.",
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = ArmorColors.Muted,
                )
                HorizontalDivider(color = ArmorColors.Outline)
                AboutFeature(Icons.Filled.Shield, "Armar y desarmar", "Con un toque, con confirmación.")
                AboutFeature(Icons.Filled.NotificationsActive, "Alarmas", "Avisos claros que confirmas cuando los has visto.")
                AboutFeature(Icons.Filled.Videocam, "Cámaras", "Directo, foto, grabación y movimiento de cámara.")
                AboutFeature(Icons.Filled.Sensors, "Dispositivos", "Sensores, luces, enchufes, cerraduras y sirenas.")
                AboutFeature(Icons.Filled.Bluetooth, "Nodos", "Configura un nodo nuevo por Bluetooth.")
                HorizontalDivider(color = ArmorColors.Outline)
                Text("Hecho por", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                Text("Electro Hobby 3D", fontWeight = FontWeight.Bold, color = ArmorColors.Cyan)
                Text("Software libre, licencia GPL-3.0.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            }
        },
    )
}

@Composable
private fun AboutFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon, size = 38.dp)
        Column { Text(title, fontWeight = FontWeight.SemiBold); Text(text, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted) }
    }
}
