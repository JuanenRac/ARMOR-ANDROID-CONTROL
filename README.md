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
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### Mobile operator client for ARMOR-SERVER: alarms, cameras, radar, energy, the network and the set-up of the nodes, in eight languages

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-324%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.5-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Honesty check - what runs today:** The endpoint-safety, alarm-decision, alarm-watcher, translation and read-only-role rules are covered by 324 unit tests and the app builds (`gradlew testDebugUnitTest assembleDebug`). It runs on a real phone against the CM5 test bench: sign-in, the camera monitor, arming and alarms, the radar, the set-up of a node over Bluetooth and the updates from GitHub are in use. What is **not** proven yet: the solar, electrical and alarm-panel screens with real equipment (only a radar node exists), push through Firebase (it is in the app and needs a Firebase project of its own; it has not been tried on a phone yet) and the background alarm watch under the battery rules of every phone maker.

---

## 📋 At a glance

| | |
|---|---|
| Version | `0.7.5` |
| Maturity | Established |
| Tests | 324 (`gradlew testDebugUnitTest`) |
| Stack | Kotlin / Jetpack Compose |
| On real hardware | Used on a real phone against the CM5 test bench; the solar, electrical and alarm-panel screens wait for real nodes |

## 🚦 Project status

Each line is one part of this repository and the light says how far it is today. It is written from what the repository can show (tests, builds, runs), not from what is planned.

🟢 Done and verified · 🟡 Works, with known limits · 🟠 Partly done or not verified yet · 🔴 Not done or not working

| Area | State | What it means here |
|---|---|---|
| Software | 🟢 | The app builds (`gradlew testDebugUnitTest assembleDebug`) and runs on a real phone against the CM5 test bench. |
| Tests | 🟢 | 324 unit tests pass (`gradlew testDebugUnitTest`): endpoint safety, alarm decision, alarm watcher, translations and the read-only role. |
| Interface | 🟡 | Sign-in, the camera monitor, arming and alarms and the radar are in use on a real phone; the Bluetooth set-up of a node has not been confirmed against a real node, and the solar, electrical and alarm-panel screens have not met real equipment. |
| Hardware | 🟡 | Used on a real phone against the CM5 test bench; the background alarm watch under the battery rules of every phone maker is not proven. |
| Security | 🟡 | The endpoint-safety and read-only-role rules are covered by unit tests; there has been no independent review. |
| Updates | 🟡 | The updates from GitHub are in use on the phone. |
| Integration | 🟠 | Push through Firebase is in the app but needs a Firebase project of its own and has not been tried on a phone. |
| Documentation | 🟢 | Eight languages and the changelog are up to date. |

## 🎯 Overview

* **Sign in with the server's own login:** IP, port, user and password; the password creates an HttpOnly session; it is stored on the phone only if you tick **Remember me** (off by default: user and password sealed with a Keystore key of their own, for that server only, forgotten the moment you untick it). A **viewer** role only looks: every button that changes something is off.
* **Lock with the phone:** the app asks for the fingerprint, the face or the PIN of the phone itself before arming, disarming, silencing a siren, unlocking and the other critical orders (on by default only when the phone has a fingerprint or a face), and, if you turn it on, to open the app. Your administrator can require it from Studio but never lower what you chose, a phone with no screen lock is never shut out, and the locked screen always offers signing in again with the password. Optionally (off by default, with a visible warning) the key that seals the saved session can be tied to unlocking the phone; then the alarm watch in the background waits for you to unlock the phone after a restart.
* **Camera monitor:** 1 to 16 tiles, a maximized view, live MJPEG that keeps the picture ratio, a bounded PTZ pad, snapshots and recordings, with a viewer and a delete confirmation for the recordings.
* **Arm and disarm,** after a **check before arming** (what is open, silent or down) and with the PIN to disarm; **alarms** to acknowledge, with a badge for those waiting; **devices** (smoke, gas, flood, door, window, motion, climate, plugs, lights, sirens, locks, shutters, gate and garage-door motors, water heaters) with their state and On / Off / Toggle.
* **Alarms that reach you:** a sound for every severity, an optional **spoken alarm**, notifications for a node reaching HIGH, a silent camera, a device alarm and (while armed) an offline node, and a **sounding alarm panel wakes the phone**; the background watch survives a reboot and backs off when the server is down.
* **Live radar, in 2D and 3D:** the Radar tab draws the site as designed in Studio and the people the radars see moving on it, refreshed every second and a half; drag, pinch and turn the 3D view.
* **Solar and electrical energy:** the sums (sun, consumption, batteries with their charge, whether the grid is present), every inverter and battery with the cells on demand, and the channels and alarms of the electrical nodes.
* **Alarm panels:** the state of every ARMOR-NODE-ALARM node, its zones and events, and arm / disarm commands when the server allows them.
* **Network:** the devices of the house's network, the internet's state and the **public address** with the provider and the place, as the network node reports them.
* **Set up a field node over Bluetooth** (from the login screen or *More > Set up a node*): finds the nodes that advertise `ARMOR-xxxxxx`, creates the administrator of a new one or signs in (with the node's PIN), searches for Wi-Fi networks and sets the node's name, a router's Wi-Fi or a fixed address and the broker; a **node finder** lists the ones already on the network.
* **The assistant:** written and spoken commands (a closed list, in eight languages, with a confirmation bound to you) to ask for the state of the house or to arm, disarm, silence and switch the lights.
* **Evidence library,** the perimeter and node state, a **history** of every alert, node, camera, device, alarm and mode change, the **weather map**, and the **services** and updates of the installation.
* **Eight languages** (English, Spanish, German, French, Italian, Japanese, Chinese and Portuguese), chosen in *More > Settings* or following the phone; every phrase is checked to be in the eight tables.
* **Careful with the password:** plain HTTP is allowed only to a private-LAN or loopback IPv4 *literal*; a host name that merely starts like a private address (`10.attacker.example`) or an address with a leading zero is refused. The look is made of icons: near-black surfaces, a cyan accent, amber for attention.
* **Security from the phone:** the second factor and its recovery codes, the open sessions, a check of how the server sees the phone, the new proof of identity for critical actions from outside, and, for an administrator, the VPN (devices with their configuration) and the dynamic DNS name.**The whole system on the status screen, and the way in from outside:** the status screen summarises the alarm panels, the services, the cameras that are recording and the security and remote access, each card opening its screen; and when the address typed is outside the house without https, the sign-in says why it cannot go on and a guide explains the two safe ways (VPN, or a public address with https).
* **Node health and arming modes:** a Node health screen in More (every node's uptime, why it restarted, memory, link and signal, broker connections, restarts, last error and the warnings the server raised) and, on the Alarm panels screen, a mode picker (Away, Home, Night and your own) that arms a panel in the chosen mode when its firmware knows the modes. Eight languages.
* **Connections in the history:** the History screen has a second list, read-only, with the devices and apps that connect to the server (Studio, this app, the panels of the nodes, the voice service), when each was last seen, its open sessions and its sign-ins and refusals; the app now says what it is to the server so it appears as itself. Addresses are shown to administrators only.
* **Battery maintenance (administrators):** the simple operations on a Pylontech battery - clock, counters and logs, restart, capacity, MOSFET test, fuse fault, outputs - in the same two steps as Studio (the server has the node read the battery, the screen shows exactly what will be done, then "Do it now" with the serial number typed again), with the result and what changed; limits, firmware and the expert console stay in Studio.
* **Control of the nodes (administrators):** More > Node control gives the same operations as Studio - restart, pause, resume, standby (work off, still connected), identify, diagnostics and maintenance on or off - to one node or to all of them, asking for the node's name (or the word RESTART, PAUSE or STANDBY for all) typed again and for the fresh proof of identity, showing how far each node went, and warning that an alarm panel or a radar paused or on standby leaves the house unprotected. There is no factory reset, no erase and no deep sleep.
* **An alarm window in the open app:** while an alarm waits for a person, a banner over every screen but the alarms pulses (red and fast for a critical one, amber for the rest, still when the phone has its animations off), says which alarm is the worst and how many wait, and lets the app be used under it; it can open the alarms, confirm from the banner or be closed until another alarm arrives.
* **From outside the house:** two addresses (the private one on the home Wi-Fi, the one for away on mobile data or the VPN) chosen by where the phone is, with the other tried if one is silent and a message that says what fails; the key of the installation's certificate is remembered the first time (or paired from the text of Studio's QR) and a different one is refused, plain http is refused for a host that was reached over https; a node's panel can be opened through the server; alarms can arrive as push messages with data only, announced once whether they come by push or by the watch; the open app and the watch share one reading of the server; the camera streams stop out of sight and come back with growing waits.

## 🧩 Role in A.R.M.O.R.

ARMOR-ANDROID-CONTROL is the phone's window on the same [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) that [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) uses: it reads and orders through it and nothing else. It is where the alarms follow you (a sounding alarm panel wakes the phone), where you ask the assistant by voice, and the tool to set up a new node over Bluetooth in the field, standing next to it.

## 📂 Repository Structure

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, NodeControlScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmBanner.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, DeviceLock.kt, PhoneLock.kt, LockScreens.kt, PinDialog.kt, MjpegFeed.kt, AlarmCodes.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md (the Firebase steps, in eight languages), BENCH_TESTS.md
├── tools/   capture_fixtures.mjs (takes fixtures from a real server), the shared CI checks (armor_ci_validate.py, armor_project_tool.py, _armor_readme_parity.py)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ Development Environment

**Requirements:** JDK 17+, the Android SDK (compile SDK 37), an Android 8.0+ phone (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 324 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Releases are published on GitHub, from where the app itself checks for updates; the APK is signed with the maintainer's own release key, kept outside the repository, and the app compares the signing certificate of a download with its own before it hands the file to Android's installer. See the [client boundary](docs/CLIENT_BOUNDARY.md).

## 🔗 Related Projects

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) is a perimeter-security system made of independent repositories. Each one has its own version, its own tests and its own README; this is the family:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Message contracts, validators, conformance vectors and generated types
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Field-node firmware for ESP32-S3 with three radars and its own web panel
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Solar inverter and battery protocols and the messages of a gateway node
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Electrical node: meters, the message of the network's readings and the rules for switching
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Alarm node and panel: zones, arming, delays, siren and PIN, with the server or without it
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Touch panel: the state of the system on a wall screen, arming and acknowledging, and the home of the voice assistant
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - The local network: its devices, the internet and what changes
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Central coordinator: telemetry, alarms, devices, solar readings and cameras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web console: cameras, radar, alarms, solar energy and the 2D/3D site designer
* **ARMOR-ANDROID-CONTROL** (this repository) - Android operator client with a live 2D/3D radar
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone operator client
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Visual inference policy that explains its decisions and never actuates
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Camera zones, masks and detection rules for the Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Offline voice intents with a confirmation that cannot be forged
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Enclosures, electronics and the bench acceptance matrix
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Deployment, the CM5 test bench, backup and TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Offline telemetry simulator with repeatable faults
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Detects, installs and updates the ecosystem's own repositories
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architecture, security baseline and the capability matrix

## 📚 Documentation & Community

Where to read more:

* [Capability matrix: what is proven and what is not](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Project catalogue: versions and how the repositories depend on each other](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Changelog of this repository](CHANGELOG.md)
* [License (GPL-3.0-or-later)](LICENSE)
* Questions, ideas and reports: electrohobby3d@gmail.com

## 👤 AUTHOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENSE

GPL-3.0-or-later - see [LICENSE](LICENSE).
