<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  🇺🇸 <b>English</b> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a>
</p>

### Mobile operator client for ARMOR-SERVER

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**Honesty check - what runs today:** The endpoint-safety and alarm-decision rules have unit tests and the app builds. It has **not been run on a phone against the server**, so the notification permission flow and the background service are unverified. Arming, alarms and devices follow the server's routes but have not been tried on a real phone.

---

## 🎯 Overview

* **Sign in with the server's own login:** IP, port, user and password; the password creates an HttpOnly session and is never stored on the phone.
* **Camera monitor:** 1 to 16 tiles, a maximized view, live MJPEG that keeps the picture ratio, a bounded PTZ pad, snapshots and recordings.
* **Arm and disarm**, after a confirmation; **alarms** to acknowledge, with a badge for those waiting; **devices** (smoke, gas, flood, door, window, motion, climate, plugs, lights, sirens, locks) with their state and On / Off / Toggle.
* **Live radar, in 2D and 3D:** the Radar tab draws the site as designed in Studio (ground, buildings, trees, posts, the fields of the radars and cameras) and the people the radars see moving on it, refreshed every second and a half; drag, pinch and turn the 3D view. The placement follows Studio's rules, but it has never shown a real radar.
* **Solar:** an entry in *More* (and a tile on the Status screen) shows the sums (sun, consumption, batteries with their charge and flow, whether the grid is present) and every inverter and battery the server reports: their numbers, the cells of a battery on demand with the highest and the lowest marked, the equipment still waiting for data, and a mark on an example reading or a silent device. Refreshed every five seconds while it is open; tested against the shapes the server answers and tried in an emulator against a local server with example readings, never with real equipment. Solar alarms (an inverter fault, a battery that is low or protecting itself, equipment that went silent) wake the phone like a device alarm.
* **Set up a field node over Bluetooth** (from the login screen or *Más > Configurar un nodo*): finds the nodes that advertise `ARMOR-xxxxxx`, creates the administrator of a new one or signs in, searches for Wi-Fi networks and sets the node's name, a router's Wi-Fi or a fixed address and the broker, for nodes with no Ethernet cable. Compiled and unit-tested, never run against a node or a phone.
* **Evidence library**, the perimeter and node state, and a **history** of every alert, node, camera, device, alarm and mode change.
* **Alarm notifications** for a node reaching HIGH, a camera that stops answering, a device alarm (smoke, gas, flood, panic at any time; a door, window or motion sensor while armed) and, while armed, a node that goes offline; an optional background watch uses the current session and says so when it ends.
* **Careful with the password:** plain HTTP is allowed only to a private-LAN or loopback IPv4 *literal*. A host name that merely starts like a private address (`10.attacker.example`) or an address with a leading zero (some resolvers read `010.0.0.1` as the public `8.0.0.1`) is refused.
* **Electrical network:** an entry in *More* (and a tile on the Status screen) shows the power of the grid input (drawing or feeding the network), every channel the electrical nodes measure (voltage, current, power, energy, frequency, power factor, the state of a switch) and the meters' alarms; the alarms of the electrical nodes (a meter's alarm, the mains out of range, the grid lost, a node that stopped answering) wake the operator like the solar ones. An inverter's card also shows its second PV input and the units of a parallel system.
* **A look made of icons:** near-black surfaces, a cyan accent, amber for attention, big icons with few words, a bottom bar, an About page and a sign-out button.

## 📂 Repository Structure

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt (splash, sign-in, account, About), HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt, SolarScreens.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt   configure a radar, solar or electrical node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmWatcherService.kt
│   ├── ArmorTheme.kt, ServerEndpoint.kt, MjpegFeed.kt
│   ├── network/   model/
├── docs/CLIENT_BOUNDARY.md
└── app/src/test/   endpoint-safety, plain-words, node-protocol and solar-model tests
```

## 🛠️ Development Environment

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug APK is not signed for distribution. See the [client boundary](docs/CLIENT_BOUNDARY.md).

## 🔗 Related Projects

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) is a perimeter-security system made of independent repositories. Each one has its own version, its own tests and its own README; this is the family:

* **[ARMOR-COMMON](../ARMOR-COMMON)** - Message contracts, validators, conformance vectors and generated types
* **[ARMOR-RADAR](../ARMOR-RADAR)** - Field-node firmware for ESP32-S3 with three radars and its own web panel
* **[ARMOR-SOLAR](../ARMOR-SOLAR)** - Solar inverter and battery protocols and the messages of a gateway node
* **[ARMOR-ELECTRICAL](../ARMOR-ELECTRICAL)** - Electrical node: meters, the message of the network's readings and the rules for switching
* **[ARMOR-NETWORK](../ARMOR-NETWORK)** - The local network: its devices, the internet and what changes
* **[ARMOR-SERVER](../ARMOR-SERVER)** - Central coordinator: telemetry, alarms, devices, solar readings and cameras
* **[ARMOR-STUDIO](../ARMOR-STUDIO)** - Web console: cameras, radar, alarms, solar energy and the 2D/3D site designer
* **ARMOR-ANDROID-CONTROL** (this repository) - Android operator client with a live 2D/3D radar
* **[ARMOR-SERVER-AI](../ARMOR-SERVER-AI)** - Visual inference policy that explains its decisions and never actuates
* **[ARMOR-VOICE-AI](../ARMOR-VOICE-AI)** - Offline voice intents with a confirmation that cannot be forged
* **[ARMOR-HARDWARE](../ARMOR-HARDWARE)** - Enclosures, electronics and the bench acceptance matrix
* **[ARMOR-DEVOPS](../ARMOR-DEVOPS)** - Deployment, the CM5 test bench, backup and TLS
* **[ARMOR-SIMULATOR](../ARMOR-SIMULATOR)** - Offline telemetry simulator with repeatable faults
* **[ARMOR-DOCS](../ARMOR-DOCS)** - Architecture, security baseline and the capability matrix

## 📚 Documentation & Community

Where to read more:

* [Capability matrix: what is proven and what is not](../ARMOR-DOCS/docs/CAPABILITY_MATRIX.md)
* [Project catalogue: versions and how the repositories depend on each other](../ARMOR-DOCS/docs/PROJECT_CATALOG.md)
* [Changelog of this repository](CHANGELOG.md)
* [License (GPL-3.0-or-later)](LICENSE)
* Questions, ideas and reports: electrohobby3d@gmail.com

## 👤 AUTHOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENSE

GPL-3.0-or-later - see [LICENSE](LICENSE).
