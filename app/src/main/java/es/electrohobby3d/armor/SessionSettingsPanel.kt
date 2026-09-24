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
        Text("Sesión", style = MaterialTheme.typography.titleMedium)
        Text(if (authenticated) "Sesión de cámaras activa." else "No hay una sesión activa.")
        if (authenticated) TextButton(onClick = onMedia) { Text("Actualizar biblioteca de evidencias") }
        OutlinedButton(onClick = onLogout, enabled = authenticated, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        Text(
            "La app conserva sólo la dirección del servidor. La contraseña no se guarda; ARMOR-SERVER emite una cookie HttpOnly temporal y mantiene las credenciales de cámaras cifradas en su propio almacenamiento.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
