// ARMOR-ANDROID-CONTROL - mobile operator client: the shell (top bar, bottom bar, dialogs) around the screens.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.electrohobby3d.armor.model.CameraView
import es.electrohobby3d.armor.update.AppUpdateState
import es.electrohobby3d.armor.update.AppUpdateViewModel
import kotlinx.coroutines.delay
import java.net.URI

class ArmorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Tr.init(this)
        setContent { ArmorScreen() }
    }
}

/** The six places of the bottom bar: an icon each, and the word only under the one that is open. */
private enum class Section(private val key: String, val icon: ImageVector) {
    Status("Estado", Icons.Filled.Shield), Radar("Radar", Icons.Filled.Radar), Alarms("Alarmas", Icons.Filled.NotificationsActive), Devices("Dispositivos", Icons.Filled.Sensors),
    Cameras("Cámaras", Icons.Filled.Videocam), More("Más", Icons.Filled.GridView);
    val label: String get() = tr(key)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArmorScreen(viewModel: ArmorViewModel = viewModel(), updateViewModel: AppUpdateViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("armor-control", Context.MODE_PRIVATE) }
    // The server's session is kept (sealed in the Keystore) so the app opens signed in; the password never is.
    val vault = remember { SessionVault(PreferencesStore(context.getSharedPreferences("armor-session", Context.MODE_PRIVATE)), KeystoreBox()) }
    var origin by rememberSaveable { mutableStateOf(preferences.getString("origin", "") ?: "") }
    val savedUri = remember(origin) { runCatching { URI(origin) }.getOrNull() }
    var serverHost by rememberSaveable { mutableStateOf(savedUri?.host.orEmpty()) }
    var serverPort by rememberSaveable { mutableStateOf(if (savedUri?.port in 1..65535) savedUri?.port.toString() else "8080") }
    var username by rememberSaveable { mutableStateOf(preferences.getString("user", "") ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf(Section.Status) }
    var moreTab by rememberSaveable { mutableStateOf<MoreTab?>(null) }
    // The weather's place, remembered between launches; nothing is asked of Open-Meteo until one is chosen.
    val savedPlace = remember {
        val name = preferences.getString("wx_name", null)
        val lat = preferences.getString("wx_lat", null)?.toDoubleOrNull()
        val lon = preferences.getString("wx_lon", null)?.toDoubleOrNull()
        if (name != null && lat != null && lon != null) es.electrohobby3d.armor.model.Place(name, preferences.getString("wx_region", null), preferences.getString("wx_country", null), lat, lon) else null
    }
    var weatherRestored by rememberSaveable { mutableStateOf(false) }
    var confirmMode by remember { mutableStateOf<String?>(null) }
    var confirmCommand by remember { mutableStateOf<Pair<es.electrohobby3d.armor.model.SiteDevice, String>?>(null) }   // a command to a device that asks first (a circuit of the board, a critical one)
    var grid by rememberSaveable { mutableIntStateOf(4) }
    var expanded by remember { mutableStateOf<CameraView?>(null) }
    var about by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var splash by rememberSaveable { mutableStateOf(true) }
    var restoring by rememberSaveable { mutableStateOf(true) }   // trying the session kept from last time, before the sign-in is shown
    var nodeSetup by rememberSaveable { mutableStateOf(false) }   // configuring a field node over Bluetooth, with or without a server
    val state by viewModel.state.collectAsStateWithLifecycle()
    val voiceState by viewModel.voice.collectAsStateWithLifecycle()
    val securityState by viewModel.security.collectAsStateWithLifecycle()
    var loginCode by rememberSaveable { mutableStateOf("") }
    // The panel of a node that is open (null: the list), and the addresses opened lately.
    var nodePanel by rememberSaveable { mutableStateOf<String?>(null) }
    var recentNodes by remember { mutableStateOf((preferences.getString("recent_nodes", "") ?: "").split(",").mapNotNull { NodeAddress.parse(it) }) }
    var watching by rememberSaveable { mutableStateOf(preferences.getBoolean("watch", false)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val lifecycleOwner = LocalLifecycleOwner.current
    val validOrigin = ServerEndpoint.parse(origin) != null
    val currentOrigin = ServerEndpoint.parse(origin)?.origin.orEmpty()
    val snackbar = remember { SnackbarHostState() }
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

    // One check per cold start against GitHub's own release feed - never
    // automatic beyond that: download and install both need an explicit tap
    // from the operator (see MoreScreens.kt's UpdateScreen and the dialog
    // below).
    LaunchedEffect(Unit) { updateViewModel.checkForUpdate() }
    LaunchedEffect(state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && Push.available) Push.token(context.applicationContext) { viewModel.registerPush(currentOrigin, it) }
    }
    LaunchedEffect(Unit) {
        if (restoring && !state.authenticated && currentOrigin.isNotBlank()) viewModel.tryRestore(currentOrigin, vault.load(currentOrigin)) { vault.clear() }
        restoring = false
    }

    // Resumes a pending install after the operator grants the one-time
    // "install unknown apps" permission and returns to A.R.M.O.R. Control -
    // no second network download, no re-trusting a stale cache file (see
    // AppUpdateViewModel.resumeInstallAfterPermissionApproval's own doc).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) updateViewModel.resumeInstallAfterPermissionApproval()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // While the app is on screen everything refreshes every ten seconds and alarms are announced.
    var lastSavedCookie by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank()) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.pollOnce(currentOrigin, context.applicationContext)
                // The server renews a session that is in use: keep the renewed cookie.
                // (only when it changed: sealing it with the Keystore and writing it every ten seconds for nothing wears the flash)
                viewModel.sessionCookie(currentOrigin)?.let { cookie -> if (cookie != lastSavedCookie) { vault.save(currentOrigin, cookie.first, cookie.second); lastSavedCookie = cookie } }
                delay(10_000)
            }
        }
    }
    LaunchedEffect(state.authenticated) {
        if (state.authenticated && android.os.Build.VERSION.SDK_INT >= 33 && !AlarmNotifier.canNotify(context)) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        if (state.authenticated && watching) AlarmWatcherService.start(context) else if (!state.authenticated) AlarmWatcherService.stop(context)
    }
    // The Radar tab: the design of the site once, and the nodes' targets every second and a half while it is open and the app is on screen.
    LaunchedEffect(section, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.Radar) {
            if (!state.siteLoaded) viewModel.loadSite(currentOrigin)
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { viewModel.pollRadar(currentOrigin); delay(1_500) } }
        }
    }
    // The status screen summarises the alarm panels, the services and the security: they are asked every ten seconds while it is open and the app is on screen.
    LaunchedEffect(section, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.Status) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) { viewModel.pollAlarmPanels(currentOrigin); viewModel.pollServices(currentOrigin); viewModel.loadSecurity(currentOrigin); delay(10_000) }
            }
        }
    }
    // The Electrical screen (in the More menu): the meters every five seconds while it is open and the app is on screen.
    LaunchedEffect(section, moreTab, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.More && moreTab == MoreTab.Electrical) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { viewModel.pollElectrical(currentOrigin); delay(5_000) } }
        }
    }
    // The Alarm panels screen (in the More menu): the panels every three seconds while it is open and the app is on screen.
    LaunchedEffect(section, moreTab, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.More && moreTab == MoreTab.AlarmPanels) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { viewModel.pollAlarmPanels(currentOrigin); delay(3_000) } }
        }
    }
    // The Network screen (in the More menu): the internet and the devices every five seconds while it is open and the app is on screen.
    LaunchedEffect(section, moreTab, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.More && moreTab == MoreTab.Network) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { viewModel.pollNetwork(currentOrigin); delay(5_000) } }
        }
    }
    // The Solar screen (in the More menu): its equipment every five seconds while it is open and the app is on screen.
    LaunchedEffect(section, moreTab, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.More && moreTab == MoreTab.Solar) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { viewModel.pollSolar(currentOrigin); delay(5_000) } }
        }
    }
    // The Services screen (in the More menu): every service every eight seconds while it is open and the app is on screen.
    LaunchedEffect(section, moreTab, state.authenticated, currentOrigin) {
        if (state.authenticated && currentOrigin.isNotBlank() && section == Section.More && moreTab == MoreTab.Services) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { viewModel.pollServices(currentOrigin); delay(8_000) } }
        }
    }
    // The Weather screen (in the More menu): load the remembered place once, the first time it is opened.
    LaunchedEffect(section, moreTab) {
        if (section == Section.More && moreTab == MoreTab.Weather && !weatherRestored) {
            weatherRestored = true
            savedPlace?.let { viewModel.loadWeather(it) }
        }
    }
    var reloadSite by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadSite) { if (reloadSite > 0 && currentOrigin.isNotBlank()) viewModel.loadSite(currentOrigin) }
    // Messages of the app appear briefly at the bottom, in plain words, once signed in (the sign-in screen shows its own).
    LaunchedEffect(state.message, state.authenticated) {
        val text = Friendly.message(state.message)
        if (text != null && state.authenticated) { snackbar.showSnackbar(text, duration = SnackbarDuration.Short); viewModel.dismissMessage() }
    }
    // The back button goes up one level instead of leaving the app.
    BackHandler(enabled = state.authenticated && (moreTab != null || section != Section.Status)) { if (moreTab != null) moreTab = null else section = Section.Status }

    fun logout() {
        vault.clear()
        Push.forget(context)   // the token is dropped at Firebase too: if the server could not be told, its next message to this phone finds no one and the server forgets it
        AlarmWatcherService.stop(context)
        viewModel.logout(currentOrigin)
        profile = false; confirmLogout = false; section = Section.Status; moreTab = null
    }

    ArmorTheme {
        Surface(Modifier.fillMaxSize(), color = ArmorColors.Background) {
            if (splash) { ArmorSplash { splash = false }; return@Surface }
            if (restoring && !state.authenticated && currentOrigin.isNotBlank()) { RestoringScreen(); return@Surface }
            if (nodeSetup) { NodeSetupScreen(onClose = { nodeSetup = false }); return@Surface }
            if (!state.authenticated) {
                val loginEndpoint = ServerEndpoint.fromHostAndPort(serverHost, serverPort)
                LoginScreen(
                    host = serverHost, port = serverPort, username = username, password = password, busy = state.loading, message = state.message,
                    onHost = { serverHost = it.trim() }, onPort = { serverPort = it }, onUsername = { username = it }, onPassword = { password = it },
                    onLogin = { loginEndpoint?.let { endpoint ->
                        origin = endpoint.origin
                        preferences.edit().putString("origin", endpoint.origin).putString("user", username.trim()).apply()
                        viewModel.login(endpoint.origin, username, password)
                        password = ""
                    } },
                    canLogin = loginEndpoint != null && username.isNotBlank() && password.isNotBlank(),
                    onNodeSetup = { nodeSetup = true }, onAbout = { about = true },
                    challenge = securityState.loginChallenge != null, code = loginCode, onCode = { loginCode = it },
                    onSubmitCode = { loginEndpoint?.let { endpoint -> viewModel.submitCode(endpoint.origin, loginCode); loginCode = "" } },
                    onCancelCode = { loginCode = ""; viewModel.cancelCode() },
                    needsSecure = ServerEndpoint.needsSecureConnection(serverHost), onUseHttps = { serverHost = "https://" + serverHost.removePrefix("http://") },
                )
            } else {
                Scaffold(
                    containerColor = ArmorColors.Background,
                    snackbarHost = { SnackbarHost(snackbar) },
                    topBar = {
                        TopBar(
                            host = savedUri?.host.orEmpty(), armed = state.snapshot.mode == "armed", connected = state.authenticated,
                            onProfile = { profile = true }, onAbout = { about = true }, onLogout = { confirmLogout = true },
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = ArmorColors.Surface, tonalElevation = 0.dp) {
                            Section.entries.forEach { item ->
                                val waiting = if (item == Section.Alarms) state.alarms.count { !it.acknowledged } else 0
                                NavigationBarItem(
                                    selected = section == item, onClick = { section = item; if (item == Section.More) moreTab = null }, alwaysShowLabel = false,
                                    icon = { if (waiting > 0) BadgedBox(badge = { Badge { Text(waiting.toString()) } }) { Icon(item.icon, contentDescription = item.label) } else Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ArmorColors.Cyan, selectedTextColor = ArmorColors.Cyan, indicatorColor = Color0f3540, unselectedIconColor = ArmorColors.Muted),
                                )
                            }
                        }
                    },
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
                        when (section) {
                            Section.Status -> StatusScreen(
                                armed = state.snapshot.mode == "armed", updated = state.snapshot.updatedAt, nodes = state.snapshot.nodes, cameras = state.cameras.size,
                                camerasDown = state.cameraHealth.count { it.value == "offline" }, pendingAlarms = state.alarms.count { !it.acknowledged }, devices = state.devices,
                                onMode = { if (!viewModel.viewerRefused()) confirmMode = if (state.snapshot.mode == "armed") "disarmed" else "armed" }, onAlarms = { section = Section.Alarms },
                                onDevices = { section = Section.Devices }, onCameras = { section = Section.Cameras }, onRadar = { section = Section.Radar },
                                onRefresh = { viewModel.refresh(currentOrigin) }, enabled = validOrigin && !state.loading,
                                solar = state.solar, onSolar = { section = Section.More; moreTab = MoreTab.Solar },
                                electrical = state.electrical, onElectrical = { section = Section.More; moreTab = MoreTab.Electrical },
                                network = state.network, onNetwork = { section = Section.More; moreTab = MoreTab.Network },
                                alarmPanels = state.alarmPanels, onAlarmPanels = { section = Section.More; moreTab = MoreTab.AlarmPanels },
                                services = state.services, onServices = { section = Section.More; moreTab = MoreTab.Services },
                                recording = state.media.activeCameraIds.size, onRecordings = { section = Section.More; moreTab = MoreTab.Evidence; viewModel.loadMedia(currentOrigin) },
                                security = securityState, onSecurity = { section = Section.More; moreTab = MoreTab.Security; viewModel.loadSecurity(currentOrigin) },
                            )
                            Section.Radar -> RadarScreen(state.site, state.siteLoaded, state.snapshot.nodes, onReload = { reloadSite++ })
                            Section.Alarms -> AlarmsScreen(
                                state.alarms, state.closedAlarms, state.devices, state.cameras.associate { it.id to it.name },
                                onAcknowledge = { viewModel.acknowledge(currentOrigin, it) }, onAcknowledgeAll = { viewModel.acknowledgeAll(currentOrigin) }, enabled = validOrigin && !state.loading && !state.readOnly,
                                solarNames = state.solar?.let { s -> s.devices.associate { "${it.nodeId}/${it.device}" to it.name } + s.waiting.associate { "${it.nodeId}/${it.device}" to it.name } }.orEmpty(),
                            )
                            Section.Devices -> DevicesScreen(state.devices, onCommand = { device, command -> if (es.electrohobby3d.armor.model.DeviceText.confirmQuestion(device, command) == null) viewModel.command(currentOrigin, device, command) else confirmCommand = device to command }, enabled = validOrigin && !state.loading && !state.readOnly)
                            Section.Cameras -> CamerasScreen(
                                state.cameras, grid, onGrid = { grid = it }, origin = currentOrigin, viewModel = viewModel, authenticated = state.authenticated,
                                recordingIds = state.media.activeCameraIds, health = state.cameraHealth, onExpand = { expanded = it }, readOnly = state.readOnly,
                            )
                            Section.More -> when (moreTab) {
                                null -> MoreMenu(
                                    onOpen = { tab -> moreTab = tab; if (tab == MoreTab.Evidence) viewModel.loadMedia(currentOrigin); if (tab == MoreTab.Voice) viewModel.checkVoice(currentOrigin); if (tab == MoreTab.Security) viewModel.loadSecurity(currentOrigin) },
                                    onNodeSetup = { nodeSetup = true }, onAbout = { about = true }, onLogout = { confirmLogout = true },
                                    updateAvailable = updateState is AppUpdateState.Available,
                                )
                                MoreTab.Security -> SubScreen(Icons.Filled.Lock, tr("Seguridad"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.loadSecurity(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) {
                                    SecurityScreen(
                                        securityState, onStart = { viewModel.startSecondFactor(currentOrigin, it) }, onEnable = { viewModel.enableSecondFactor(currentOrigin, it) }, onDisable = { viewModel.disableSecondFactor(currentOrigin, it) },
                                        onEndSession = { viewModel.endSession(currentOrigin, it) }, onEndOthers = { viewModel.endOtherSessions(currentOrigin) }, onCheck = { viewModel.checkRemote(currentOrigin) },
                                        remoteActions = RemoteSetupActions(
                                            onVpnSetup = { endpoint, port, lan, route -> viewModel.setupVpn(currentOrigin, endpoint, port, lan, route) },
                                            onAddPeer = { name, full -> viewModel.addVpnDevice(currentOrigin, name, full) },
                                            onShowPeer = { viewModel.showVpnDevice(currentOrigin, it.name) },
                                            onRemovePeer = { viewModel.removeVpnDevice(currentOrigin, it.name) },
                                            onDdnsSetup = { provider, domain, token, zone, record, url -> viewModel.setupDdns(currentOrigin, provider, domain, token, zone, record, url) },
                                            onDdnsUpdate = { viewModel.updateDdns(currentOrigin) },
                                            onDdnsStop = { viewModel.stopDdns(currentOrigin) },
                                            onCloseConfig = { viewModel.closeVpnConfig() },
                                        ),
                                    )
                                }
                                MoreTab.Solar -> SubScreen(Icons.Filled.WbSunny, tr("Solar"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.reloadSolar(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) { SolarScreen(state.solar, state.network, onScanNetwork = { viewModel.scanNetworkNow(currentOrigin) }, scanning = state.loading) }
                                MoreTab.Electrical -> SubScreen(Icons.Filled.ElectricBolt, tr("Eléctrica"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.reloadElectrical(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) { ElectricalScreen(state.electrical, state.network, onScanNetwork = { viewModel.scanNetworkNow(currentOrigin) }, scanning = state.loading) }
                                MoreTab.AlarmPanels -> SubScreen(Icons.Filled.Shield, tr("Centrales de alarma"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.reloadAlarmPanels(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) {
                                    AlarmPanelsScreen(
                                        state.alarmPanels, state.alarmCommands, canCommand = state.authenticated && state.isAdmin, network = state.network, onScanNetwork = { viewModel.scanNetworkNow(currentOrigin) }, scanning = state.loading,
                                        onArm = { panel, mode, force -> viewModel.alarmCommand(currentOrigin, panel.nodeId, mode, force) }, onDisarm = { panel, pin -> viewModel.alarmCommand(currentOrigin, panel.nodeId, null, pin = pin) },
                                    )
                                }
                                MoreTab.Network -> SubScreen(Icons.Filled.Router, tr("Red"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.reloadNetwork(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) { NetworkScreen(state.network) }
                                MoreTab.Services -> SubScreen(Icons.Filled.Dns, tr("Servicios"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.reloadServices(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) { ServicesScreen(state.services, System.currentTimeMillis()) }
                                MoreTab.Nodes -> SubScreen(Icons.Filled.Tune, if (nodePanel == null) tr("Configurar nodos") else nodePanel!!.removePrefix("http://").trimEnd('/'), onBack = { if (nodePanel != null) nodePanel = null else moreTab = null }) {
                                    val panel = nodePanel
                                    if (panel == null) NodesScreen(
                                        state.network, recentNodes,
                                        onOpen = { address ->
                                            recentNodes = (listOf(address) + recentNodes.filter { it != address }).take(5)
                                            preferences.edit().putString("recent_nodes", recentNodes.joinToString(",")).apply()
                                            nodePanel = address
                                        },
                                    ) else NodePanelView(panel, onClose = { nodePanel = null })
                                }
                                MoreTab.Voice -> SubScreen(Icons.Filled.Mic, tr("Asistente"), onBack = { moreTab = null }) {
                                    VoiceScreen(
                                        voiceState, enabled = validOrigin && state.authenticated,
                                        onSend = { text, speak -> viewModel.sendVoice(currentOrigin, text, spoken = speak) },
                                        onConfirm = { pending, speak -> viewModel.sendVoice(currentOrigin, pending.text, confirm = pending, spoken = speak) },
                                        onClear = { viewModel.clearVoice() }, onCancel = { viewModel.cancelVoice() },
                                    )
                                }
                                MoreTab.Weather -> SubScreen(Icons.Filled.Cloud, tr("Meteorología"), onBack = { moreTab = null }) {
                                    WeatherScreen(
                                        state.weather, onSearch = { viewModel.searchPlaces(it) },
                                        onPick = { place ->
                                            preferences.edit().putString("wx_name", place.name).putString("wx_region", place.region).putString("wx_country", place.country)
                                                .putString("wx_lat", place.latitude.toString()).putString("wx_lon", place.longitude.toString()).apply()
                                            viewModel.loadWeather(place)
                                        },
                                        onRefresh = { viewModel.reloadWeather() },
                                        onChangePlace = { preferences.edit().remove("wx_name").remove("wx_region").remove("wx_country").remove("wx_lat").remove("wx_lon").apply(); viewModel.clearWeatherPlace() },
                                    )
                                }
                                MoreTab.Evidence -> SubScreen(Icons.Filled.VideoLibrary, tr("Grabaciones"), onBack = { moreTab = null }, actions = {
                                    IconButton(onClick = { viewModel.loadMedia(currentOrigin) }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                                }) { EvidenceScreen(state.media.items, state.cameras, currentOrigin, viewModel, state.authenticated, canDelete = !state.readOnly) }
                                MoreTab.History -> SubScreen(Icons.Filled.History, tr("Historial"), onBack = { moreTab = null }) { HistoryScreen(state.events, onMore = { viewModel.loadOlderEvents(currentOrigin) }) }
                                MoreTab.Settings -> SubScreen(Icons.Filled.Settings, tr("Ajustes"), onBack = { moreTab = null }) {
                                    SettingsScreen(
                                        origin, state.authenticated,
                                        onConnect = { typed -> ServerEndpoint.parse(typed)?.let { endpoint -> if (state.authenticated && endpoint.origin != currentOrigin) logout(); origin = endpoint.origin; preferences.edit().putString("origin", endpoint.origin).apply(); viewModel.refresh(endpoint.origin) } },
                                        watching = watching, onWatching = { on -> watching = on; setWatching(context, preferences, on) { notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) } },
                                    )
                                }
                                MoreTab.Updates -> SubScreen(Icons.Filled.SystemUpdate, tr("Actualizaciones"), onBack = { moreTab = null }) {
                                    UpdateScreen(
                                        updateState,
                                        onCheck = { updateViewModel.checkForUpdate() },
                                        onDownload = { update -> updateViewModel.downloadAndInstall(update) },
                                        onOpenInstallSettings = { updateViewModel.openInstallPermissionSettings() },
                                    )
                                }
                            }
                        }
                    }
                }
                confirmMode?.let { mode ->
                    val arming = mode == "armed"
                    if (arming) {
                        val check by produceState<es.electrohobby3d.armor.model.Preflight?>(null, mode) { value = viewModel.preflight(currentOrigin) }
                        val blocked = check?.ready == false
                        AlertDialog(
                            onDismissRequest = { confirmMode = null }, containerColor = ArmorColors.SurfaceRaised,
                            icon = { IconBadge(Icons.Filled.Shield, if (blocked) ArmorColors.Amber else ArmorColors.Ok, size = 56.dp) },
                            title = { Text(tr("¿Armar el sistema?")) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(tr("Puertas, ventanas y movimiento harán sonar la alarma."))
                                    when {
                                        check == null -> Text(tr("Comprobando el perímetro…"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
                                        check!!.issues.isEmpty() -> Text(tr("✓ Todo listo: los nodos responden y no hay zonas abiertas."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Ok)
                                        else -> check!!.issues.forEach { issue ->
                                            Text((if (issue.blocks) "⛔ " else "⚠ ") + es.electrohobby3d.armor.model.PreflightText.describe(issue), style = MaterialTheme.typography.bodySmall, color = if (issue.blocks) ArmorColors.Amber else ArmorColors.Muted)
                                        }
                                    }
                                }
                            },
                            confirmButton = { Button(onClick = { viewModel.setMode(currentOrigin, mode); confirmMode = null }) { Text(if (blocked) tr("Armar igualmente") else tr("Armar")) } },
                            dismissButton = { TextButton(onClick = { confirmMode = null }) { Text(tr("Cancelar")) } },
                        )
                    } else {
                        PinAlertDialog(
                            title = tr("¿Desarmar el sistema?"),
                            text = tr("Puertas, ventanas y movimiento dejan de avisar. El humo, el gas, el agua y el pánico siguen avisando."),
                            confirmLabel = tr("Desarmar"), onConfirm = { pin -> viewModel.setMode(currentOrigin, mode, pin); confirmMode = null }, onDismiss = { confirmMode = null },
                        )
                    }
                }
                confirmCommand?.let { (device, command) ->
                    AlertDialog(
                        onDismissRequest = { confirmCommand = null }, containerColor = ArmorColors.SurfaceRaised,
                        icon = { IconBadge(Icons.Filled.Warning, ArmorColors.Amber, size = 56.dp) },
                        title = { Text(trf("¿Ordenar «%s»?", device.name)) },
                        text = { Text(es.electrohobby3d.armor.model.DeviceText.confirmQuestion(device, command).orEmpty()) },
                        confirmButton = { Button(onClick = { viewModel.command(currentOrigin, device, command, confirm = true); confirmCommand = null }) { Text(tr("Sí, ordenar")) } },
                        dismissButton = { TextButton(onClick = { confirmCommand = null }) { Text(tr("Cancelar")) } },
                    )
                }
                if (confirmLogout) AlertDialog(
                    onDismissRequest = { confirmLogout = false }, containerColor = ArmorColors.SurfaceRaised,
                    icon = { IconBadge(Icons.AutoMirrored.Filled.Logout, ArmorColors.Alert, size = 56.dp) },
                    title = { Text(tr("¿Cerrar sesión?")) },
                    text = { Text(tr("Dejarás de recibir avisos en este móvil hasta que vuelvas a entrar.")) },
                    confirmButton = { Button(onClick = ::logout, colors = ButtonDefaults.buttonColors(containerColor = ArmorColors.Alert)) { Text(tr("Cerrar sesión")) } },
                    dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(tr("Cancelar")) } },
                )
                if (profile) ProfileDialog(
                    username = username, server = savedUri?.let { "${it.host}:${it.port}" } ?: origin, connected = state.authenticated, watching = watching,
                    onWatching = { on -> watching = on; setWatching(context, preferences, on) { notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) } },
                    onLogout = { profile = false; confirmLogout = true }, onDismiss = { profile = false },
                )
                expanded?.let { camera -> FullscreenCamera(camera, currentOrigin, viewModel, state.authenticated && !state.readOnly, state.media.activeCameraIds.contains(camera.id), onDismiss = { expanded = null }) }
                // Startup checks only fetch release metadata from GitHub. The
                // actual APK transfer and Android's own installer are both
                // started explicitly by the operator, from this prompt or
                // from Más > Actualizaciones. Suppressed while that same
                // screen is already open, to avoid a dialog over itself.
                val availableUpdate = (updateState as? AppUpdateState.Available)?.update
                if (availableUpdate != null && moreTab != MoreTab.Updates) {
                    AlertDialog(
                        onDismissRequest = { updateViewModel.dismiss() }, containerColor = ArmorColors.SurfaceRaised,
                        icon = { IconBadge(Icons.Filled.SystemUpdate, ArmorColors.Cyan, size = 56.dp) },
                        title = { Text(tr("Nueva versión disponible")) },
                        text = { UpdateAvailableContent(availableUpdate, onDownload = { updateViewModel.downloadAndInstall(availableUpdate); section = Section.More; moreTab = MoreTab.Updates }) },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = { updateViewModel.dismiss() }) { Text(tr("Más tarde")) } },
                    )
                }
            }
            if (securityState.elevationNeeded && state.authenticated) ElevationDialog(securityState.elevationError, securityState.busy, onConfirm = { password, code -> viewModel.elevate(currentOrigin, password, code) }, onDismiss = { viewModel.dismissElevation() })
            if (about) AboutDialog(onDismiss = { about = false })
        }
    }
}

private val Color0f3540 = androidx.compose.ui.graphics.Color(0xFF0F3540)

private fun setWatching(context: Context, preferences: android.content.SharedPreferences, on: Boolean, askPermission: () -> Unit) {
    preferences.edit().putBoolean("watch", on).apply()
    if (on) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && !AlarmNotifier.canNotify(context)) askPermission()
        AlarmWatcherService.start(context)
    } else AlarmWatcherService.stop(context)
}

/** The mark and the name, the state of the system, and the three account icons: profile, about and sign out. */
@Composable
private fun TopBar(host: String, armed: Boolean, connected: Boolean, onProfile: () -> Unit, onAbout: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.background(ArmorColors.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArmorLogo(34.dp)
            Text("A.R.M.O.R.", fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
            Row(
                Modifier.background((if (armed) ArmorColors.Ok else ArmorColors.Amber).copy(alpha = 0.16f), androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(if (armed) Icons.Filled.Shield else Icons.Filled.LockOpen, null, tint = if (armed) ArmorColors.Ok else ArmorColors.Amber, modifier = Modifier.size(16.dp))
                Text(if (armed) tr("Armado") else tr("Desarmado"), color = if (armed) ArmorColors.Ok else ArmorColors.Amber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (connected) ArmorColors.Ok else ArmorColors.Alert)
            Spacer(Modifier.width(8.dp))
            Text(if (connected) host.ifBlank { tr("Conectado") } else tr("Sin conexión"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onProfile) { Icon(Icons.Filled.Person, contentDescription = tr("Mi cuenta"), tint = ArmorColors.Cyan) }
            IconButton(onClick = onAbout) { Icon(Icons.Filled.Info, contentDescription = tr("Acerca de"), tint = ArmorColors.Cyan) }
            IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = tr("Cerrar sesión"), tint = ArmorColors.Alert) }
        }
        HorizontalDivider(color = ArmorColors.Outline)
    }
}
