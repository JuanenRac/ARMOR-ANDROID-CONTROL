// ARMOR-ANDROID-CONTROL - configure a field node over Bluetooth: find it, sign in (or create its administrator), and set its name, Wi-Fi, address and broker.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// For a node that has no Ethernet cable or no address yet. The operations are those of the node's own web panel; the protocol is ARMOR-RADAR's docs/BLE_PROVISIONING.md.
// Compiled, never run against a node.
package es.electrohobby3d.armor

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.electrohobby3d.armor.model.NodeHello
import es.electrohobby3d.armor.model.NodeProtocol
import es.electrohobby3d.armor.model.NodeSettingsPatch
import es.electrohobby3d.armor.model.SetupCode
import es.electrohobby3d.armor.model.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class NodeStep { Search, Connecting, SignIn, Configure }

data class NodeSetupState(
    val step: NodeStep = NodeStep.Search,
    val scanning: Boolean = false,
    val found: List<FoundNode> = emptyList(),
    val hello: NodeHello? = null,
    val busy: Boolean = false,
    val message: String = "",
    val admin: Boolean = false,
    val networks: List<WifiNetwork> = emptyList(),
    val problems: List<String> = emptyList(),
    val saved: Boolean = false,
    val name: String = "",
)

class NodeSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val client = NodeBleClient(application)
    private val mutable = MutableStateFlow(NodeSetupState())
    val state: StateFlow<NodeSetupState> = mutable

    fun scan() {
        if (!client.bluetoothOn) { mutable.update { it.copy(message = "El Bluetooth del teléfono está apagado.") }; return }
        mutable.update { it.copy(scanning = true, found = emptyList(), message = "") }
        val ok = client.startScan { node ->
            mutable.update { s -> s.copy(found = (s.found.filter { it.address != node.address } + node).sortedByDescending { it.rssi }) }
        }
        if (!ok) mutable.update { it.copy(scanning = false, message = "No se pudo empezar la búsqueda.") }
    }

    fun stopScan() { client.stopScan(); mutable.update { it.copy(scanning = false) } }

    fun connect(node: FoundNode) {
        stopScan()
        mutable.update { it.copy(step = NodeStep.Connecting, message = "Conectando con ${node.name}… (Android puede pedir emparejar)") }
        viewModelScope.launch {
            if (!client.connect(node)) { mutable.update { it.copy(step = NodeStep.Search, message = "No se pudo conectar con ${node.name}.") }; return@launch }
            val reply = client.request("hello")
            if (!reply.ok) { client.disconnect(); mutable.update { it.copy(step = NodeStep.Search, message = NodeProtocol.errorText(reply.error)) }; return@launch }
            val hello = NodeHello.from(reply.data)
            mutable.update { it.copy(step = NodeStep.SignIn, hello = hello, message = "", name = hello.name) }
        }
    }

    /** Creates the administrator of a new node with its set-up code, or signs in to a node that has users. */
    fun signIn(code: String, secret: String, user: String, password: String) {
        val hello = mutable.value.hello ?: return
        mutable.update { it.copy(busy = true, message = "") }
        viewModelScope.launch {
            val args = JSONObject().put("user", user).put("password", password)
            val reply = if (hello.setup) {
                val effective = code.ifBlank { SetupCode.derive(secret, hello.mac).orEmpty() }
                client.request("setup", args.put("code", effective.trim().uppercase()))
            } else client.request("login", args)
            if (reply.ok) {
                val isAdmin = hello.setup || reply.data.optString("role") == "admin"
                mutable.update { it.copy(busy = false, step = NodeStep.Configure, admin = isAdmin, message = "") }
            } else mutable.update { it.copy(busy = false, message = NodeProtocol.errorText(reply.error)) }
        }
    }

    fun searchNetworks() {
        mutable.update { it.copy(busy = true, message = "Buscando redes Wi-Fi… (unos segundos)") }
        viewModelScope.launch {
            val reply = client.request("wifi.scan", timeoutMs = 40_000)
            mutable.update {
                if (reply.ok) it.copy(busy = false, networks = WifiNetwork.list(reply.data), message = if (WifiNetwork.list(reply.data).isEmpty()) "No se oyó ninguna red." else "")
                else it.copy(busy = false, message = NodeProtocol.errorText(reply.error))
            }
        }
    }

    fun apply(form: NodeSettingsPatch.Form) {
        mutable.update { it.copy(busy = true, message = "", problems = emptyList(), saved = false) }
        viewModelScope.launch {
            val reply = client.request("config.put", JSONObject().put("config", NodeSettingsPatch.build(form)))
            mutable.update {
                if (reply.ok) it.copy(busy = false, saved = true, message = "Guardado. Reinicia el nodo para aplicarlo.")
                else it.copy(busy = false, message = NodeProtocol.errorText(reply.error), problems = reply.problems())
            }
        }
    }

    fun reboot() {
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            val reply = client.request("reboot")
            client.disconnect()
            mutable.update { NodeSetupState(message = if (reply.ok) "El nodo se reinicia. Cuando vuelva, ábrelo por su dirección o por Studio." else NodeProtocol.errorText(reply.error)) }
        }
    }

    fun close() { client.stopScan(); client.disconnect(); mutable.value = NodeSetupState() }

    override fun onCleared() { client.stopScan(); client.disconnect() }
}

private fun blePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

private fun hasBlePermissions(context: Context): Boolean = blePermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun NodeSetupScreen(onClose: () -> Unit, model: NodeSetupViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(hasBlePermissions(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> granted = result.values.all { it }; if (granted) model.scan() }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Configurar nodo por Bluetooth", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { model.close(); onClose() }) { Text("Cerrar") }
        }
        if (state.message.isNotBlank()) Text(state.message, color = if (state.saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        when (state.step) {
            NodeStep.Search -> SearchStep(state, granted, onSearch = { if (granted) model.scan() else permission.launch(blePermissions()) }, onStop = model::stopScan, onPick = model::connect)
            NodeStep.Connecting -> Text("Conectando… Si Android pregunta por emparejar, acepta.")
            NodeStep.SignIn -> SignInStep(state, onSignIn = model::signIn, onBack = { model.close() })
            NodeStep.Configure -> ConfigureStep(state, onScanWifi = model::searchNetworks, onApply = model::apply, onReboot = model::reboot)
        }
    }
}

@Composable
private fun SearchStep(state: NodeSetupState, granted: Boolean, onSearch: () -> Unit, onStop: () -> Unit, onPick: (FoundNode) -> Unit) {
    Text("Para un nodo sin cable Ethernet o sin dirección todavía. El nodo tiene que estar encendido y con el Bluetooth activo (un nodo nuevo lo tiene mientras no tenga usuarios).", style = MaterialTheme.typography.bodySmall)
    if (!granted) Text("La app necesita el permiso de Bluetooth cercano para buscar nodos.", style = MaterialTheme.typography.bodySmall)
    Button(onClick = if (state.scanning) onStop else onSearch, modifier = Modifier.fillMaxWidth()) { Text(if (state.scanning) "Parar la búsqueda" else "Buscar nodos") }
    state.found.forEach { node ->
        Card(Modifier.fillMaxWidth().clickable { onPick(node) }) {
            Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(node.name); Text("${node.rssi} dBm", style = MaterialTheme.typography.labelMedium) }
        }
    }
    if (state.scanning && state.found.isEmpty()) Text("Buscando…")
}

@Composable
private fun SignInStep(state: NodeSetupState, onSignIn: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    val hello = state.hello ?: return
    var code by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(hello.name.ifBlank { hello.nodeId }, style = MaterialTheme.typography.titleMedium)
            Text("MAC ${hello.mac} · firmware ${hello.firmware}", style = MaterialTheme.typography.bodySmall)
            Text(if (hello.hasIp) "Red: ${hello.ip}" else "Sin dirección de red todavía", style = MaterialTheme.typography.bodySmall)
            if (hello.staConnected) Text("Wi-Fi: ${hello.staSsid}", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (hello.setup) {
        Text("Este nodo no tiene usuarios. Escribe el código de configuración (el que muestra por su consola USB, o el de la flota) y elige el administrador.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(code, { code = it }, label = { Text("Código de configuración") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(secret, { secret = it }, label = { Text("…o el secreto de la flota (calcula el código desde la MAC)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    } else Text("Inicia sesión con un usuario del nodo.", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(user, { user = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(password, { password = it }, label = { Text(if (hello.setup) "Contraseña nueva (8 caracteres o más)" else "Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Button(onClick = { onSignIn(code, secret, user, password) }, enabled = !state.busy && user.isNotBlank() && password.isNotEmpty() && (!hello.setup || code.isNotBlank() || secret.isNotBlank()), modifier = Modifier.fillMaxWidth()) {
        Text(if (hello.setup) "Crear administrador" else "Entrar")
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Elegir otro nodo") }
}

@Composable
private fun ConfigureStep(state: NodeSetupState, onScanWifi: () -> Unit, onApply: (NodeSettingsPatch.Form) -> Unit, onReboot: () -> Unit) {
    var name by remember(state.name) { mutableStateOf(state.name) }
    var useWifi by remember { mutableStateOf(false) }
    var ssid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var fixed by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var netmask by remember { mutableStateOf("255.255.255.0") }
    var gateway by remember { mutableStateOf("") }
    var dns by remember { mutableStateOf("") }
    var brokerUri by remember { mutableStateOf("") }
    var brokerUser by remember { mutableStateOf("") }
    var brokerPassword by remember { mutableStateOf("") }
    var bluetooth by remember { mutableStateOf("") }
    if (!state.admin) { Text("Este usuario sólo puede mirar: para cambiar la configuración hace falta un administrador."); return }

    Text("Nombre y conexión", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(name, { name = it }, label = { Text("Nombre del nodo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Switch(useWifi, { useWifi = it }); Text("Conectarse al Wi-Fi de un router o punto de acceso") }
    if (useWifi) {
        Button(onClick = onScanWifi, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Buscar redes Wi-Fi") }
        state.networks.forEach { network ->
            Card(Modifier.fillMaxWidth().clickable { ssid = network.ssid }) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(network.ssid); Text("${network.rssi} dBm · canal ${network.channel} · ${network.security}", style = MaterialTheme.typography.labelSmall) }
            }
        }
        OutlinedTextField(ssid, { ssid = it }, label = { Text("Nombre de la red (SSID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(wifiPassword, { wifiPassword = it }, label = { Text("Contraseña del Wi-Fi") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Switch(fixed, { fixed = it }); Text("Dirección fija (si no, DHCP)") }
        if (fixed) {
            OutlinedTextField(address, { address = it }, label = { Text("Dirección IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(netmask, { netmask = it }, label = { Text("Máscara de subred") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(gateway, { gateway = it }, label = { Text("Puerta de enlace") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(dns, { dns = it }, label = { Text("DNS (vacío: la puerta de enlace)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
    HorizontalDivider()
    Text("Broker (opcional)", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(brokerUri, { brokerUri = it }, label = { Text("Dirección del broker (mqtt://host:puerto)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(brokerUser, { brokerUser = it }, label = { Text("Usuario del broker") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(brokerPassword, { brokerPassword = it }, label = { Text("Contraseña del broker") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    HorizontalDivider()
    Text("Bluetooth después del reinicio", style = MaterialTheme.typography.titleMedium)
    listOf("" to "no cambiar", "setup" to "sólo mientras el nodo no tenga usuarios", "always" to "siempre (hace falta iniciar sesión)", "off" to "apagado").forEach { (value, label) ->
        Row(Modifier.clickable { bluetooth = value }, verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = bluetooth == value, onClick = { bluetooth = value }); Text(label) }
    }
    if (state.problems.isNotEmpty()) Text("Valores que el nodo no acepta: " + state.problems.joinToString("; "), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = { onApply(NodeSettingsPatch.Form(name, useWifi, ssid, wifiPassword, fixed, address, netmask, gateway, dns, brokerUri, brokerUser, brokerPassword, bluetooth)) },
        enabled = !state.busy && (!useWifi || ssid.isNotBlank()), modifier = Modifier.fillMaxWidth(),
    ) { Text("Guardar en el nodo") }
    if (state.saved) OutlinedButton(onClick = onReboot, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Reiniciar el nodo ahora") }
}
