// ARMOR-ANDROID-CONTROL - Security, for an administrator: the VPN into the house (devices that can connect, with their configuration) and the name that follows the house (dynamic DNS).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The same two cards as Studio's Security tab, so that everything can be set up from the phone. The work is done by the maintenance helper of the machine; the configuration of a device holds a
// private key, so it is shown with a warning and closed afterwards, and it is never kept. The panels of the nodes are registered and trusted from Studio.
package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.PeerConfig
import es.electrohobby3d.armor.model.RemoteSetup
import es.electrohobby3d.armor.model.VpnPeer

/** What the two cards can ask for. */
data class RemoteSetupActions(
    val onVpnSetup: (endpoint: String, port: Int, lan: String, routeLan: String) -> Unit,
    val onAddPeer: (name: String, fullTunnel: Boolean) -> Unit,
    val onShowPeer: (VpnPeer) -> Unit,
    val onRemovePeer: (VpnPeer) -> Unit,
    val onDdnsSetup: (provider: String, domain: String, token: String, zone: String, record: String, url: String) -> Unit,
    val onDdnsUpdate: () -> Unit,
    val onDdnsStop: () -> Unit,
    val onCloseConfig: () -> Unit,
)

@Composable
private fun Card(title: String, help: String?, content: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (help != null) Text(help, color = ArmorColors.Muted, fontSize = 13.sp)
            content()
        }
    }
}

@Composable
private fun Field(value: String, onValue: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text, secret: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValue, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
    )
}

@Composable
private fun VpnCard(setup: RemoteSetup, config: PeerConfig?, busy: Boolean, actions: RemoteSetupActions) {
    var endpoint by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("51820") }
    var lan by remember { mutableStateOf("192.168.0.0/24") }
    var route by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<VpnPeer?>(null) }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Card(tr("VPN (WireGuard)"), null) {
        when {
            !setup.vpnScript -> Text(tr("El ayudante de mantenimiento no está instalado en la máquina, así que esto no se puede hacer desde aquí. Instálalo con: install_cm5.sh --with-maintenance."), color = ArmorColors.Muted, fontSize = 13.sp)
            !setup.vpnTool -> Text(tr("WireGuard no está instalado en la máquina. Instálalo con «sudo apt install wireguard-tools» y vuelve."), color = ArmorColors.Amber, fontSize = 13.sp)
            !setup.vpnConfigured -> {
                Field(endpoint, { endpoint = it }, tr("Nombre o dirección a la que se conectará el móvil (por ejemplo casa.duckdns.org)"))
                Field(port, { port = it.filter(Char::isDigit).take(5) }, tr("Puerto de la VPN (UDP)"), KeyboardType.Number)
                Field(lan, { lan = it }, tr("Red de la casa"))
                Field(route, { route = it }, tr("Que el móvil llegue también a los nodos de la casa (nombre de la interfaz de red, por ejemplo eth0)"))
                Button(onClick = { actions.onVpnSetup(endpoint.trim(), port.toIntOrNull() ?: 51820, lan.trim(), route.trim()) }, enabled = endpoint.isNotBlank() && !busy) { Text(tr("Configurar la VPN")) }
            }
            else -> {
                Text((if (setup.vpnRunning) tr("La VPN está funcionando.") else tr("La VPN está configurada pero parada.")) + " " + setup.vpnEndpoint + " · " + setup.vpnSubnet, fontWeight = FontWeight.Bold, color = if (setup.vpnRunning) ArmorColors.Cyan else ArmorColors.Amber)
                Text(tr("En tu router, redirige el puerto UDP {0} a esta máquina. No hace falta abrir nada más.").replace("{0}", setup.vpnEndpoint.substringAfterLast(':', setup.vpnPort.ifBlank { "51820" })), color = ArmorColors.Muted, fontSize = 13.sp)
                Text(tr("Dispositivos que pueden conectarse"), fontWeight = FontWeight.Bold)
                if (setup.peers.isEmpty()) Text(tr("Todavía no hay dispositivos."), color = ArmorColors.Muted)
                setup.peers.forEach { peer ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(peer.name, fontWeight = FontWeight.Bold); Text(peer.address, color = ArmorColors.Muted, fontSize = 12.sp) }
                        TextButton(onClick = { copied = false; actions.onShowPeer(peer) }, enabled = !busy) { Text(tr("Ver configuración")) }
                        TextButton(onClick = { removing = peer }, enabled = !busy) { Text(tr("Quitar")) }
                    }
                }
                Text(tr("Añadir un dispositivo"), fontWeight = FontWeight.Bold)
                Field(name, { name = it.filter { character -> character.isLetterOrDigit() || character == '-' || character == '_' }.take(32) }, tr("Nombre del dispositivo (letras, cifras, - y _; por ejemplo movil-ana)"))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = full, onCheckedChange = { full = it }); Text(tr("Enviar todo el tráfico de este dispositivo por la casa"), fontSize = 13.sp) }
                Button(onClick = { copied = false; actions.onAddPeer(name, full); name = "" }, enabled = name.isNotBlank() && !busy) { Text(tr("Crear")) }
                config?.let { shown ->
                    Text(tr("Configuración de este dispositivo") + ": " + shown.name, fontWeight = FontWeight.Bold, color = ArmorColors.Amber)
                    Text(tr("Contiene la clave privada del dispositivo. Escanea el código con la app WireGuard (o importa el texto), cierra esta ventana y no se lo des a nadie más."), fontSize = 13.sp)
                    SelectionContainer { Text(shown.config, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(shown.config)); copied = true }) { Text(if (copied) tr("Copiado") else tr("Copiar")) }
                        Button(onClick = { copied = false; actions.onCloseConfig() }) { Text(tr("Cerrar")) }
                    }
                }
            }
        }
    }
    removing?.let { peer ->
        AlertDialog(
            onDismissRequest = { removing = null }, containerColor = ArmorColors.SurfaceRaised,
            title = { Text(peer.name) }, text = { Text(tr("¿Quitar este dispositivo? Ya no podrá conectarse.")) },
            confirmButton = { Button(onClick = { removing = null; actions.onRemovePeer(peer) }) { Text(tr("Quitar")) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(tr("Cancelar")) } },
        )
    }
}

@Composable
private fun DdnsCard(setup: RemoteSetup, busy: Boolean, actions: RemoteSetupActions) {
    var provider by remember { mutableStateOf("duckdns") }
    var domain by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var record by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Card(tr("Nombre que sigue a tu casa (DNS dinámico)"), tr("Tu operador cambia de vez en cuando la dirección pública de tu casa. Esto mantiene un nombre apuntando siempre a ella, y el móvil nunca necesita saber la dirección.")) {
        when {
            !setup.ddnsScript -> Text(tr("El ayudante de mantenimiento no está instalado en la máquina, así que esto no se puede hacer desde aquí. Instálalo con: install_cm5.sh --with-maintenance."), color = ArmorColors.Muted, fontSize = 13.sp)
            setup.ddnsConfigured -> {
                Text(tr("Manteniendo {0} al día mediante {1}. Última dirección comunicada: {2}.").replace("{0}", setup.ddnsName.ifBlank { "-" }).replace("{1}", setup.ddnsProvider).replace("{2}", setup.ddnsLast.ifBlank { "-" }))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = actions.onDdnsUpdate, enabled = !busy) { Text(tr("Actualizar ahora")) }
                    OutlinedButton(onClick = actions.onDdnsStop, enabled = !busy) { Text(tr("Dejar de actualizar")) }
                }
            }
            else -> {
                Text(tr("Sin configurar."), color = ArmorColors.Muted)
                Text(tr("Servicio"), fontWeight = FontWeight.Bold)
                listOf("duckdns" to tr("DuckDNS (gratuito)"), "cloudflare" to tr("Cloudflare"), "url" to tr("Otro (una dirección a la que llamar)")).forEach { (id, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = provider == id, onClick = { provider = id }); Text(label, fontSize = 14.sp) }
                }
                if (provider == "duckdns") Field(domain, { domain = it.lowercase() }, tr("Nombre (sin .duckdns.org)"))
                if (provider == "cloudflare") { Field(zone, { zone = it }, tr("ID de la zona (Cloudflare)")); Field(record, { record = it }, tr("Nombre que mantener al día")) }
                if (provider == "url") Field(url, { url = it }, tr("Dirección a la que llamar, con {ip} donde va la dirección")) else Field(token, { token = it }, tr("Token del servicio"), secret = true)
                val ready = when (provider) { "duckdns" -> domain.isNotBlank() && token.isNotBlank(); "cloudflare" -> zone.isNotBlank() && record.isNotBlank() && token.isNotBlank(); else -> url.isNotBlank() }
                Button(onClick = { actions.onDdnsSetup(provider, domain.trim(), token.trim(), zone.trim(), record.trim(), url.trim()); token = "" }, enabled = ready && !busy) { Text(tr("Guardar y comprobar ahora")) }
            }
        }
    }
}

/** The VPN and the dynamic DNS name, for an administrator; the panels of the nodes are said to be in Studio. */
@Composable
fun RemoteSetupCards(setup: RemoteSetup, config: PeerConfig?, busy: Boolean, actions: RemoteSetupActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VpnCard(setup, config, busy, actions)
        DdnsCard(setup, busy, actions)
        Card(tr("Paneles de los nodos a través del servidor"), tr("Para abrir el panel de un nodo desde fuera de casa, usa Studio (Configuración > Seguridad): ahí se registran los nodos y se comprueba y se confía en su certificado.")) {}
    }
}
