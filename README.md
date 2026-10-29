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
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-342%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.7-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Honesty check - what runs today:** The endpoint-safety, alarm-decision, alarm-watcher, translation and read-only-role rules are covered by 342 unit tests and the app builds (`gradlew testDebugUnitTest assembleDebug`). It runs on a real phone against the CM5 test bench: sign-in, the camera monitor, arming and alarms, the radar, the set-up of a node over Bluetooth and the updates from GitHub are in use. What is **not** proven yet: the solar, electrical and alarm-panel screens with real equipment (only a radar node exists), push through Firebase (it is in the app and needs a Firebase project of its own; it has not been tried on a phone yet) and the background alarm watch under the battery rules of every phone maker.

---

## 📋 At a glance

| | |
|---|---|
| Version | `0.7.7` |
| Maturity | Established |
| Role | Android operator client for ARMOR-SERVER (family: Clients, parent: ARMOR-SERVER) |
| Stack | Kotlin / Jetpack Compose, Android 8.0+ |
| Tests | 342 (`gradlew testDebugUnitTest`) |
| Languages | 8 (1345 phrases per language, every phrase checked in all tables) |
| Runs on | An Android 8.0+ phone (minSdk 26) |
| On real hardware | Used on a real phone against the CM5 test bench; the solar, electrical and alarm-panel screens wait for real nodes |
| License | GPL-3.0-or-later |

**Jump to:** [Overview](#-overview) · [Architecture](#-architecture) · [Security](#-security--safety) · [Interfaces](#-interfaces) · [Configuration](#-configuration) · [Quick start](#-quick-start) · [Testing](#-testing) · [Troubleshooting](#-troubleshooting) · [Structure](#-repository-structure) · [Related](#-related-projects)

## 🚦 Project status

Each line is one part of this repository and the light says how far it is today. It is written from what the repository can show (tests, builds, runs), not from what is planned.

🟢 Done and verified · 🟡 Works, with known limits · 🟠 Partly done or not verified yet · 🔴 Not done or not working

| Area | State | What it means here |
|---|---|---|
| Software | 🟢 | The app builds (`gradlew testDebugUnitTest assembleDebug`) and runs on a real phone against the CM5 test bench. |
| Tests | 🟢 | 342 unit tests pass (`gradlew testDebugUnitTest`): endpoint safety, alarm decision, alarm watcher, translations and the read-only role. |
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
* **Security from the phone:** the second factor and its recovery codes, the open sessions, a check of how the server sees the phone, the new proof of identity for critical actions from outside, and, for an administrator, the VPN (devices with their configuration) and the dynamic DNS name.
* **The whole system on the status screen, and the way in from outside:** the status screen summarises the alarm panels, the services, the cameras that are recording and the security and remote access, each card opening its screen; and when the address typed is outside the house without https, the sign-in says why it cannot go on and a guide explains the two safe ways (VPN, or a public address with https).
* **Node health and arming modes:** a Node health screen in More (every node's uptime, why it restarted, memory, link and signal, broker connections, restarts, last error and the warnings the server raised) and, on the Alarm panels screen, a mode picker (Away, Home, Night and your own) that arms a panel in the chosen mode when its firmware knows the modes. Eight languages.
* **Connections in the history:** the History screen has a second list, read-only, with the devices and apps that connect to the server (Studio, this app, the panels of the nodes, the voice service), when each was last seen, its open sessions and its sign-ins and refusals; the app now says what it is to the server so it appears as itself. Addresses are shown to administrators only.
* **Battery maintenance (administrators):** the simple operations on a Pylontech battery - clock, counters and logs, restart, capacity, MOSFET test, fuse fault, outputs - in the same two steps as Studio (the server has the node read the battery, the screen shows exactly what will be done, then "Do it now" with the serial number typed again), with the result and what changed; limits, firmware and the expert console stay in Studio.
* **Control of the nodes (administrators):** More > Node control gives the same operations as Studio - restart, pause, resume, standby (work off, still connected), identify, diagnostics and maintenance on or off - to one node or to all of them, asking for the node's name (or the word RESTART, PAUSE or STANDBY for all) typed again and for the fresh proof of identity, showing how far each node went, and warning that an alarm panel or a radar paused or on standby leaves the house unprotected. There is no factory reset, no erase and no deep sleep.
* **An alarm window in the open app:** while an alarm waits for a person, a banner over every screen but the alarms pulses (red and fast for a critical one, amber for the rest, still when the phone has its animations off), says which alarm is the worst and how many wait, and lets the app be used under it; it can open the alarms, confirm from the banner or be closed until another alarm arrives.
* **From outside the house:** two addresses (the private one on the home Wi-Fi, the one for away on mobile data or the VPN) chosen by where the phone is, with the other tried if one is silent and a message that says what fails; the key of the installation's certificate is remembered the first time (or paired from the text of Studio's QR) and a different one is refused, plain http is refused for a host that was reached over https; a node's panel can be opened through the server; alarms can arrive as push messages with data only, announced once whether they come by push or by the watch; the open app and the watch share one reading of the server; the camera streams stop out of sight and come back with growing waits.

## 🧩 Role in A.R.M.O.R.

ARMOR-ANDROID-CONTROL is the phone's window on the same [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) that [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) uses: it reads and orders through it and nothing else. It is where the alarms follow you (a sounding alarm panel wakes the phone), where you ask the assistant by voice, and the tool to set up a new node over Bluetooth in the field, standing next to it.

| Name | How | What for |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | Everything the app reads and orders |
| Field nodes | Bluetooth (`ARMOR-xxxxxx`) | Sets up a new node standing next to it |
| GitHub | HTTPS | Checks and downloads the signed update of the app itself |
| Open-Meteo | HTTPS | The public forecast, air quality and place search of the weather map |

## 🏛️ Architecture

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-ANDROID-CONTROL architecture: who calls it, what it holds and what it depends on" width="100%">
</p>

## 🔒 Security & Safety

| Threat | Defence |
|---|---|
| A password kept on the phone | Only with *Remember me* (off by default): user and password sealed with a Keystore key of their own, for that server only, forgotten the moment you untick it; no backup (`allowBackup=false`) |
| Plain HTTP towards the internet | Allowed only to a private-LAN or loopback IPv4 *literal*; a host like `10.attacker.example` or an address with a leading zero (`010.0.0.1` reads as octal) is refused; plain http is refused for a host that was reached over https |
| A man in the middle over https | Trust is the system's authorities plus those you installed on the phone; the key of the installation's certificate is remembered the first time (or paired from the text of Studio's QR) and a different one is refused |
| A critical order with the phone in other hands | The fingerprint, the face or the PIN of the phone before arming, disarming, silencing a siren, unlocking and the other critical orders (on by default only when the phone has a fingerprint or a face); the administrator can require it from Studio but never lower what you chose; a phone with no screen lock is never shut out |
| A viewer changes something | The *viewer* role sees everything but every button that changes something is off (role tests) |
| A wrong disarm PIN | The PIN goes to the server only when disarming; `wrong_pin`, `pin_locked`, `pin_not_set` and `elevation_required` are translated into plain words |
| A fake update of the app | The SHA-256 of the signing certificates (history) of the installed app and of the candidate are compared before Android's installer is called; if they share none, it is said plainly |
| An alarm the phone does not see | A foreground service (`AlarmWatcherService`) polls every 10 s, waits 15 s for the phone to unlock, backs off up to 60 s, says *unreachable* after 6 failures, survives a reboot; an alarm older than 15 minutes is not announced |
| A camera password on the phone | The cameras are seen through the server's stream: the app has no camera password and no RTSP address |

### Permissions

`INTERNET`, `ACCESS_NETWORK_STATE`: the server. `USE_BIOMETRIC`: the lock of the phone. `POST_NOTIFICATIONS`: alarms. `FOREGROUND_SERVICE` (+ `DATA_SYNC`, `SPECIAL_USE`) and `RECEIVE_BOOT_COMPLETED`: the alarm watch. `REQUEST_INSTALL_PACKAGES`: the update of the app. `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`: the set-up of a node; `BLUETOOTH`, `BLUETOOTH_ADMIN` and `ACCESS_FINE_LOCATION` only up to Android 11 (`maxSdkVersion=30`), for the Bluetooth scan.

No independent security review. Read [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) and [SECURITY.md](SECURITY.md).

## 🔌 Interfaces

| Part | What it sets |
|---|---|
| ARMOR-SERVER | 55 distinct paths of `/api/v1` in `network/ArmorApiClient.kt` (the interface is `network/ArmorApi.kt`); the radar is read every 1.5 s; the cameras by MJPEG through the server |
| Two addresses | The private one on the home Wi-Fi and the one for away (mobile data or the VPN), chosen by where the phone is; the other is tried if one is silent and the message says what fails |
| Bluetooth | Finds nodes that advertise `ARMOR-xxxxxx`, creates the administrator of a new one or signs in with the node's PIN, searches Wi-Fi networks and sets the name, the Wi-Fi or a fixed address and the broker; the protocol is `model/NodeBle.kt`; a node finder lists the ones already on the network |
| Voice | `POST /api/v1/voice/command`: a closed list of commands in eight languages, with a confirmation bound to you (`model/Voice.kt`, `VoiceScreens.kt`) |
| Push | `PushService`: alarms as messages with data only, announced once whether they come by push or by the watch; the Firebase steps are in [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) (eight languages) |

## ⚙️ Configuration

| Name | Value | What it sets |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | Android 8.0 or later on the phone |
| `versionName` | = `armor.project.json` | The build fails when the manifest and the version differ |
| JDK / Android SDK | 17+ / 37 | To build the app and run the tests |

In the app (not in files): the language (the eight or following the phone), the two addresses, *Remember me*, the lock of the phone, the spoken alarm and the notifications, in *More > Settings*. Limits in the code: the watch polls every 10 s, backs off to 60 s and ignores alarms older than 15 minutes.

## 🚀 Quick start

**Requirements:** JDK 17+, the Android SDK (compile SDK 37), an Android 8.0+ phone (minSdk 26).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 342 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Releases are published on GitHub, from where the app itself checks for updates; the APK is signed with the maintainer's own release key, kept outside the repository, and the app compares the signing certificate of a download with its own before it hands the file to Android's installer. See the [client boundary](docs/CLIENT_BOUNDARY.md).

## 🧪 Testing

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| Part | Tests | What it covers |
|---|---|---|
| Alarms | 67 | `AlarmPolicy`, `AlarmBanner`, `AlarmPanels`, `AlarmCodes`, `AlarmRoute`, `NoticeFilter`, `PushEvents`, `DevicesAndAlarms` |
| Server link, parsing, fixtures | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| View model and roles | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| Lock, session vault, keys | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| Endpoint safety, addresses, certificates | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| Energy and network | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| Nodes over Bluetooth, node control | 26 | `NodeBle`, `NodeControl` |
| Updates from GitHub | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| Radar and cameras | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| Translations, plain words, voice | 14 | `Tr`, `Friendly`, `Voice` |

342 tests in 46 files, run with `testDebugUnitTest`; a fake server (`FakeServer.kt`) and fixtures taken from a real server (`tools/capture_fixtures.mjs`) feed them. Every phrase is checked to be in the eight language tables.

## 🏗️ Build & Release

Releases are published on GitHub, from where the app itself checks for updates; the app looks for the asset `ARMOR-ANDROID-CONTROL-release.apk`. The APK is signed with the maintainer's own release key, kept outside the repository, and the app compares the signing certificate of a download with its own before it hands the file to Android's installer. The `versionCode` goes up before each APK, or Android does not treat it as an update.

## 🩺 Troubleshooting

| You see | Because | Do |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | The server refuses the disarm PIN or asks for a fresh proof of identity | Type the PIN again, wait for the lock to end, or confirm your identity |
| The sign-in says it cannot go on | The address is outside the house and has no https (plain http is refused) | Follow the guide: a VPN, or a public address with https |
| The connection is refused after it worked | The key of the installation's certificate changed | Pair again with the text of Studio's QR if you changed it on purpose |
| Alarms arrive late or not at all in the background | The battery rules of the phone maker stop the watch; push needs a Firebase project of its own | Exempt the app from battery saving; follow [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| The update is refused | The signing certificate of the download shares none with the installed app | Do not install it; take the APK from the releases of this repository |

## 📂 Repository Structure

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository structure of ARMOR-ANDROID-CONTROL" width="100%">
</p>

<details>
<summary>Text version of the tree</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   the app: screens, rules, network and updates
├── app/src/main/assets/                         eight language tables, alarm codes, weather map
├── app/src/test/                                342 tests and a fake server
├── docs/                                        the guides of this repository
├── tools/                                       the shared CI checks and project tooling
├── .github/                                     CI workflow, issue and pull-request templates
├── images/                                      banner, icon and the two pictures of this README
├── build.sh build.bat                           build through the shared project workflow
├── test.sh test.bat                             run the tests through the shared workflow
├── run.sh run.bat                               start through the shared project workflow
├── armor.project.json                           manifest: version, role, maturity, build command
├── CHANGELOG.md                                 what changed in each version
├── SECURITY.md SUPPORT.md LICENSE               security policy, support and license
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           how to contribute and the code of conduct
├── build.gradle.kts settings.gradle.kts         the Gradle build of the project
├── gradle.properties                            Gradle settings
├── gradlew gradlew.bat gradle/                  the Gradle wrapper
├── build-test.sh build-test.bat                 run the tests through the shared workflow
└── .gitattributes .gitignore                    Git settings
```

</details>

## 🗺️ Roadmap & Known limits

**Known limits today**

* The solar, electrical and alarm-panel screens have not met real equipment (only a radar node exists).
* The Bluetooth set-up of a node has not been confirmed against a real node, and push through Firebase has not been tried on a phone.
* The background alarm watch under the battery rules of every phone maker is not proven.
* No independent security review.

**Next**

* Try the solar, electrical and alarm-panel screens with real nodes, and the Bluetooth set-up with a real node.
* Try push through Firebase with an installation's own project, and the alarm watch on several phone makers.

## 🌍 Languages

README and texts exist in English, Spanish, French, Italian, German, Chinese, Japanese and European Portuguese.

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

## 📚 Documentation

* [Client boundary](docs/CLIENT_BOUNDARY.md): what the client does and never does
* [Push notifications](docs/PUSH_NOTIFICATIONS.md): the Firebase steps, in eight languages
* [Bench tests](docs/BENCH_TESTS.md): checks with a real phone
* [Capability matrix: what is proven and what is not](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Project catalogue: versions and how the repositories depend on each other](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Changelog of this repository](CHANGELOG.md)

## 🤝 Contributing & Community

* Before a pull request run `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py`: it must pass. See [CONTRIBUTING.md](CONTRIBUTING.md) and the [code of conduct](CODE_OF_CONDUCT.md).
* The README exists in eight languages and is generated from the sources in ARMOR-DOCS: a change to the text is made there.
* A change to a message between components starts in ARMOR-COMMON.
* Do not describe as working on a board what has only been built.
* Report a vulnerability privately: [SECURITY.md](SECURITY.md). Questions, ideas and reports: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTHOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENSE

GPL-3.0-or-later - see [LICENSE](LICENSE).
