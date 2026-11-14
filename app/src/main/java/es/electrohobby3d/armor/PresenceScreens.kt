// ARMOR-ANDROID-CONTROL - the card of "My account" for the reminder on leaving home: the switch, the button that saves the place the phone is in as home, and what the permissions are for.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/** The reminder on leaving home. Off by default; turning it on asks for the place permissions, and nothing leaves the phone. */
@Composable
fun PresenceCard() {
    val context = LocalContext.current
    val settings = remember { PresenceSettings.of(context) }
    var on by remember { mutableStateOf(settings.enabled) }
    var saved by remember { mutableStateOf(settings.home != null) }
    var message by remember { mutableStateOf<String?>(null) }
    var wantsHome by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun saveHome() {
        if (!PresenceGeofence.hasPermission(context)) { message = tr("Se necesita el permiso de ubicación, también con la app cerrada, para avisarte al salir."); return }
        LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                val place = location?.let { HomePlace(it.latitude, it.longitude) }
                if (place == null || !place.valid) { message = tr("No se pudo leer tu ubicación."); return@addOnSuccessListener }
                settings.home = place; saved = true; message = tr("Casa guardada.")
                PresenceGeofence.restore(context)
            }
            .addOnFailureListener { message = tr("No se pudo leer tu ubicación.") }
    }

    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) message = tr("Se necesita el permiso de ubicación, también con la app cerrada, para avisarte al salir.")
        else if (wantsHome) { wantsHome = false; saveHome() } else PresenceGeofence.restore(context)
    }
    val fine = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) message = tr("Se necesita el permiso de ubicación, también con la app cerrada, para avisarte al salir.")
        else if (Build.VERSION.SDK_INT >= 29 && !PresenceGeofence.hasPermission(context)) background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else if (wantsHome) { wantsHome = false; saveHome() } else PresenceGeofence.restore(context)
    }
    fun askPermissions() { if (!PresenceGeofence.hasPermission(context)) fine.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(tr("Recordatorio al salir de casa"))
                Text(tr("Te avisa si sales de casa con la alarma desarmada. Todo se queda en el móvil: tu ubicación no se envía a ningún sitio."), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = on, onCheckedChange = { value ->
                on = value; settings.enabled = value; message = null
                if (value) { if (PresenceGeofence.hasPermission(context)) PresenceGeofence.restore(context) else askPermissions() } else PresenceGeofence.stop(context)
            })
        }
        if (on) FilledTonalButton(onClick = { message = null; if (PresenceGeofence.hasPermission(context)) saveHome() else { wantsHome = true; askPermissions() } }, modifier = Modifier.fillMaxWidth()) {
            Text(if (saved) tr("Cambiar la casa por mi ubicación actual") else tr("Usar mi ubicación actual como casa"))
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
