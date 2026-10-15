// ARMOR-ANDROID-CONTROL - the question asked before disarming: the PIN of the signed-in user. The server checks it (and says if it is wrong or locked); the field is there for everybody,
// because the server only insists for a user who has a PIN (or when the installation requires one).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

/** A PIN is 4 to 10 digits; whatever else is typed is dropped as it is typed. */
internal fun onlyDigits(text: String): String = text.filter { it in '0'..'9' }.take(10)

@Composable
internal fun PinAlertDialog(title: String, text: String, confirmLabel: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ArmorColors.SurfaceRaised,
        icon = { IconBadge(Icons.Filled.LockOpen, ArmorColors.Amber, size = 56.dp) },
        title = { Text(title) },
        text = {
            Column {
                Text(text)
                OutlinedTextField(
                    value = pin, onValueChange = { pin = onlyDigits(it) }, singleLine = true,
                    label = { Text("PIN (si tienes uno)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(pin) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
