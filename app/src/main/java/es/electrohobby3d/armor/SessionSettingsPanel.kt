// ARMOR-ANDROID-CONTROL - authenticated connection settings.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun SessionSettingsPanel(
    origin: String,
    changeOrigin: (String) -> Unit,
    valid: Boolean,
    authenticated: Boolean,
    onConnect: () -> Unit,
    onMedia: () -> Unit,
    onLogout: () -> Unit,
    watching: Boolean,
    onWatching: (Boolean) -> Unit,
    onNodeSetup: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Conexión", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = origin,
            onValueChange = changeOrigin,
            label = { Text("Origen ARMOR-SERVER") },
            supportingText = { Text(if (valid) "HTTPS o HTTP de LAN privada aceptado" else "Ejemplo LAN: http://192.168.0.50:8080") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = onConnect, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Conectar y actualizar") }
        HorizontalDivider()
        Text("Avisos", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(12.dp)) {
            Switch(checked = watching, onCheckedChange = onWatching, enabled = authenticated)
            Text("Vigilar alarmas en segundo plano")
        }
        Text(
            "Muestra una notificación permanente y avisa de alertas altas, cámaras sin respuesta y nodos caídos mientras el sistema está armado. " +
                "Usa la sesión actual: si caduca, la vigilancia se detiene y lo avisa. Con la app abierta los avisos funcionan siempre.",
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()
        Text("Sesión", style = MaterialTheme.typography.titleMedium)
        Text(if (authenticated) "Sesión de cámaras activa." else "No hay una sesión activa.")
        if (authenticated) TextButton(onClick = onMedia) { Text("Actualizar biblioteca de evidencias") }
        OutlinedButton(onClick = onLogout, enabled = authenticated, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        HorizontalDivider()
        Text("Nodos de campo", style = MaterialTheme.typography.titleMedium)
        Text("Para un nodo sin cable Ethernet, o sin dirección todavía: se configura desde aquí por Bluetooth (nombre, Wi-Fi de un router, dirección, broker).", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onNodeSetup, modifier = Modifier.fillMaxWidth()) { Text("Configurar un nodo por Bluetooth") }
        Text(
            "La app conserva sólo la dirección del servidor. La contraseña no se guarda; ARMOR-SERVER emite una cookie HttpOnly temporal y mantiene las credenciales de cámaras cifradas en su propio almacenamiento.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
