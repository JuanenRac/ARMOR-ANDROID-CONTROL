# Changelog

All notable changes to this project are documented here.

## [0.4.0] - Weather and Services, and the full list of outages

- **Weather** (More menu): the weather of the place you choose - by name - because guessing it from the network would be wrong. Shows the conditions now, the rain of the next hour, warnings the forecast implies, the next 24 hours, ten days, the air quality and pollen, and the sun and the moon. Nothing is asked of the Internet until a place is chosen, and only its coordinates leave the phone; it never goes through ARMOR-SERVER. The live rain-and-cloud radar map of ARMOR-STUDIO's Weather menu is not in this release.
- **Services** (More menu): every program of the system and every field node, running or not, grouped by family (Core, Network, AI and voice, Field nodes), with its state, systemd unit details, PID, port, memory, restarts and how long it has been up. Read only. Needs ARMOR-SERVER 0.3.9 or later.
- **Network:** the list of outages now shows when each one ended, not only when it started and how long it lasted.

## [0.3.9] - An eye to check what was typed, on the node's own screens

- **Eye on every password of a node's set-up** (the set-up code's secret, the administrator's password, the node's Wi-Fi, the broker): tap it to check what was typed instead of guessing. The app's own sign-in screen already had it; it was missing here.

## [0.3.8] - Configuring a node over Bluetooth no longer ends with the node vanishing

- **Real bug, reported by the user:** after giving a node the Wi-Fi of the house over Bluetooth, the node restarted and neither joined the Wi-Fi nor could be found again, and the app only said "open it by its address". The app now looks for the node again by Bluetooth after it restarts and reads, from its `hello`, whether it joined (its address and network) or why it did not (network not found, wrong password, or only "it did not connect"), with buttons to check again or to change the configuration.
- **Fixed address or DHCP on the Wi-Fi too:** the address block (DHCP or fixed address, mask, gateway, DNS) is offered whichever connection is chosen; before it was only shown for the cable.
- **The form is checked before it is sent:** a Wi-Fi name is needed, a Wi-Fi password has 8 to 63 characters, and a fixed address, mask and gateway must be valid and in the same network.
- Needs the node firmware that reports why it did not join (ARMOR-RADAR 0.3.2, ARMOR-SOLAR 0.1.1, ARMOR-ELECTRICAL 0.0.7); with an older firmware the app says only that it could not tell.
- **Tests:** the patch with a fixed address on Wi-Fi, the form checks and the reading of the outcome from the `hello`.

## [0.3.7] - A GitHub rate limit looked like a broken update check

- **Real bug, reported by the user:** checking for updates failed with "GitHub devolvió el código HTTP 403." - technically accurate, but it gave the operator no way to tell a genuine problem from GitHub's own unauthenticated rate limit (60 requests/hour, shared by every device on the same home network's public IP, not per-app or per-phone) simply being spent for the hour. `describeHttpFailure()` now reads the same `X-RateLimit-Remaining`/`X-RateLimit-Reset` headers GitHub's own response already carries and, only when they say the ceiling is genuinely spent, explains that in plain Spanish with a real estimate of when to try again - any other HTTP failure still reports its real status code plainly, nothing is hidden.
- **Tests:** 6 new JVM tests for the message itself (spent limit with/without a usable reset header, a reset timestamp already in the past, a genuine unrelated 403, budget still remaining, an unrelated HTTP status) - a pure function, no mocked `HttpURLConnection` needed.

## [0.3.6] - Checking GitHub for a new version

- **A real update channel, same design as HYDRA-UMC-ANDROID-CONTROL's own:** on every cold start the app asks GitHub's own release feed (`GET /repos/JuanenRac/ARMOR-ANDROID-CONTROL/releases/latest`) whether a newer *stable* semver tag exists, and offers it through a dialog and through a new *Más > Actualizaciones* screen. Nothing downloads or installs on its own: the operator has to tap Descargar e instalar, and Android's own package installer still makes the final signature check before replacing the app. A draft or prerelease tag is never offered.
- The APK is downloaded straight from that release's own asset (must be named exactly `ARMOR-ANDROID-CONTROL-release.apk`), verified to be a real Android package for this exact `applicationId` and genuinely newer than what is installed before the installer is ever opened; a real, bounded manual-redirect follow keeps GitHub's own required headers on the hop from `api.github.com`/`github.com` to its asset host. Uses `java.net.HttpURLConnection`, the same stdlib-only HTTP the rest of this app already talks to ARMOR-SERVER with - no new networking dependency.
- **Tests:** 7 on the JVM (stable-tag parsing and ordering, the release-metadata trust gate: rejects a non-stable tag, a missing asset, a non-HTTPS URL, a draft/prerelease, or a release that is not actually newer). The debug APK builds. The update flow itself has never run on a phone.

## [0.3.5]

- A GitHub Actions CI baseline (`.github/workflows/ci.yml`): validates the manifest, the version, CHANGELOG.md's heading, the seven README translations' structure and its own local Markdown links, then runs this project's real build/test through `tools/armor_project_tool.py build-test .` (vendored from ARMOR-COMMON, alongside `tools/armor_ci_validate.py` and `tools/_armor_readme_parity.py`, which do the manifest/docs checking).

## [0.3.4] - The local network

- **A *Red* screen** (in *Más*, and a tile on the Status screen): whether the internet is there and, when it is not, whose fault it is (the provider's or this side's), its latency and loss, the outages of the day, the devices of the network with their address, kind and maker (the ones nobody marked as known, and the ones with a port a house rarely wants open, stand out) and what changed. Reading only; it reads what ARMOR-NETWORK nodes report to the server.
- The alarms of the network (the internet or the local network down, a device that was never seen, two machines for one address, a port that opened, a node that went silent) have their own wording and wake the operator, like the solar and electrical ones.

## [0.3.3] - The electrical network

- **An *Eléctrica* screen** (in *Más*, and a tile on the Status screen): the power of the grid input, whether the house draws from the network or feeds it, every channel the electrical nodes measure (voltage, current, power, energy, frequency, power factor, the state of a switch) and the alarms of the meters, refreshed every five seconds while it is open. Reading only.
- **The alarms of the electrical nodes** (a meter's alarm, the mains out of range, the grid lost, a node that stopped answering, a fault of a source switch) have their own wording and wake the operator, like the solar ones.
- **An inverter's card** shows its second PV input and, for a parallel system, its units and the total power.
- **Tests:** 62 on the JVM (the electrical answers, the wording, the second input and the units, the new alarm codes); the debug APK builds. Not run on a phone.


## [0.3.2] - Configuring the three kinds of node over Bluetooth

- **One screen for the radar, the solar and the electrical node.** They answer the same Bluetooth protocol (the same service and characteristics), so *Configure a node* finds any of them by its `ARMOR-XXXXXX` name and sets its name, Wi-Fi, address, broker and Bluetooth mode. After connecting, the screen says **which kind of node** it is (the node declares it in its `hello`; a node that predates that is told by its identifier when it says so).
- **Plain words for what a node refuses:** asking a Wi-Fi-only board for the Ethernet cable, or leaving it with neither Wi-Fi nor its own access point, now says why instead of showing the field code.
- **Tests:** the kinds of node and the refusals are checked on the JVM with the rest of the protocol.
- **Not done:** the radio side has never talked to a node of any kind (no phone and no board have met yet).

## [0.3.1] - The names of the new inverter and battery models

- The solar screens name the models of the server's new catalogue (Axpert, PIP, Revo, InfiniSolar, LV5048, SunGoldPower, the Pylontech and Pytes families) and read an ANT-BMS combination (`ant-bms-24s-200a` shows as ANT-BMS 24S · 200 A); a model it does not know is shown by its identifier.
- A battery's screen shows its **health** (its capacity against new).

## [0.3.0] - Solar alarms wake the phone

- **A solar alarm is announced** like a device alarm, once when it is raised and whether or not the system is armed: an inverter fault (high), a battery that is low or that reports a protection acting (warning or high), and equipment that stopped answering (warning). The notice reads like the others (*ALARMA ALTA · Avería en un inversor solar · casa/axpert-1*), in the app and in the background watch. Acknowledging or clearing it announces nothing.
- **The Alarms screen names solar alarms** by the equipment's own name (the one declared in Studio) instead of its `node/device` path, and the four solar codes have their wording (*Avería en un inversor solar*, *Batería solar baja*, *Una batería solar avisa de un problema*, *Un equipo solar ha dejado de responder*).
- Tests: 55 (was 53). Not tried on a phone.

## [0.2.9] - Solar inverters and batteries in the app

- **A Solar screen** (*Más > Solar*, and a tile on the Status screen that shows the sun, the consumption and the batteries' charge): the sums the server makes (sun, consumption, batteries with their charge and whether they charge or discharge, whether the grid is present and the inverter's mode) and, below them, every inverter and every battery stack that has reported, with its numbers. It is refreshed every five seconds while it is open.
- **An inverter** shows the grid, the load, the panels, the battery side, the temperature and its warnings by name; **a battery** shows its charge, voltage, current, power, temperatures, the range of its cells with the spread in millivolts, capacities and cycles, an alarm line when a protection is acting, and, on demand, **every cell** with the highest and the lowest marked and the temperature sensors (an ANT-BMS lists them all).
- **Honest about the state of the data:** equipment declared in Studio that has not reported yet is listed as *waiting for data*; a device that has gone quiet says *sin señal*; a reading the server made up to try the menus says *ejemplo*. A field the server did not send shows a dash, never a zero.
- Tests: 53 (was 48), for the reading of the server's answer (inverter, battery with cells and sensors, totals, the waiting list, a null total, an odd or empty answer) and the wording. Exercised in an Android emulator against a local server with declared equipment and example readings; never against a real inverter or battery.

## [0.2.8] - Live radar, in 2D and in 3D

- **A Radar tab** in the bottom bar (second place): the site as Studio designed it, with the people the radars see moving on it, refreshed every second and a half while the tab is open. The design is read from the server (`GET /api/v1/site`): the ground, the buildings (walls, floors, gable and hip roofs), the objects on the ground, the cameras' and radars' fields, in the colours chosen in Studio.
- **2D**: north up, drag and pinch, double tap to centre, a 5 m grid and a scale bar; each person is a dot with their track number, a pulse when they count for the alarm and grey when an ignore zone hides them.
- **3D**: a real projection with depth: drag to turn, pinch to zoom, double tap to reset; the buildings with their roofs, trees, posts, masts and fences, the radars' fields on the ground and a standing figure for each person.
- The people are placed with the same rules as Studio's live radar map (which radar of the design produces which node's radar number, which side is positive x, the rated sector of each model), tested with the same numbers. A radar of the design that is not wired to a node in Studio shows nothing; a node that reports people no radar is wired to is said so on the screen. Not drawn on the phone yet: the ignore-zone rectangles, the trails, the doors and windows of the buildings and the devices' places.
- Tests: 48 (was 39), for the geometry and the reading of the site document. Exercised in an Android 14 emulator against a private server with a designed site and two simulated people walking (both views). Not run on a phone, and never with a real radar.

## [0.2.7] - A new look: icons first, plain words

- **A redesigned app** in the look of A.R.M.O.R. (the same dark surfaces and cyan as Studio) and with the structure of the other apps of the family: a splash with the mark, a top bar with the name, the state of the system and the connection, and a bottom bar of five icons (Status, Alarms, Devices, Cameras, More) that shows a word only under the one that is open.
- **Icons instead of text everywhere**: arming is one big power button on the status card; alarms, devices and cameras have an icon per kind (smoke, door, motion, plug, light, siren, lock...); the camera pad, photo, record and full-screen are icon buttons; devices are tiles with a switch.
- **Sign out** is an icon in the top bar, in *More* and in the account box, always asking first. New **account box** (user, server, connection, alerts with the app closed), a proper **About** with the mark, what the app does and who made it, and a launcher icon.
- **Fewer technical words**: no revision numbers, ports or ids on the screens; messages of the server and the network are turned into sentences ("Usuario o contraseña incorrectos", "No se puede conectar con el servidor. Comprueba que el móvil está en la misma wifi") and times are shown in the phone's time zone.
- The back button goes up one level. The sign-in screen has a show/hide password eye and keeps the last user.
- Tests: 39 (was 35), for the plain-words rules and the time zone. Nothing of the server's API changed. Checked in an Android 14 emulator against a local server (sign-in, status, alarms, More, settings, About); the cameras and devices screens were not exercised with real cameras or devices, and it was not run on a phone.

## [0.2.6] - Configure a field node over Bluetooth

- **Configurar un nodo por Bluetooth**, from the login screen and from *Más > Conexión*, so the same app does everything: it finds the nodes that advertise `ARMOR-xxxxxx`, connects (Android pairs when asked), creates the administrator of a new node with its set-up code (or with the fleet secret, from which it computes the code of the node's MAC) or signs in to one that has users, searches for Wi-Fi networks, and sets the node's name, the router's Wi-Fi or a fixed address, the broker and the Bluetooth mode, then restarts it. It is for nodes with no Ethernet cable or no address yet.
- New permissions: Bluetooth scan and connect (never used for location), and location only up to Android 11, where scanning needs it. The Bluetooth link is compiled and its framing, requests, set-up code and settings are unit-tested (35 tests, 10 new); it has **never run against a node or a phone**.

## [0.2.5] - Arm and disarm, alarms, devices and device alarms on the phone

- **Arm and disarm** from the phone, after a confirmation that says what changes; the server records it with the signed-in user's name.
- **Alarms menu:** what needs a person right now with the source (the device's own name), how serious and since when; acknowledge one or all; the closed record. The bottom bar shows how many are waiting.
- **Devices menu:** every sensor and actuator the server holds (smoke, gas, flood, door, window, motion, climate, plugs, lights, sirens, locks, valves) with its state, filters (all, sensors, actuators, needing attention) and On / Off / Toggle for what can be commanded. Devices are added and placed from Studio.
- **A smoke, CO, gas, flood or panic alarm wakes the phone** even while the system is disarmed, and a door, window, motion, glass-break or vibration alarm does while armed; the event is announced once. Node and camera alarms are not announced a second time.
- The overview shows the alarms waiting and the devices needing attention; the history reads device and alarm events. Recordings, history and connection are now under *Más* to keep the bottom bar at five places.
- An older server without alarms or devices keeps working: those screens stay empty instead of failing the refresh.
- 25 unit tests (was 7).

## [0.2.4] - PTZ that stops

- PTZ keys work by holding (press moves, release stops after at least 300 ms, repeated every second), commands go out in order, only failures are shown, and each camera tile has a PTZ toggle that shows the pad over the picture. Not run on a phone.

## [0.2.3] - History, alarms and camera health

- New **Historial** section (every alert, node, camera and mode change) and the radar list moved into **Estado**; camera tiles say when a camera does not answer.
- Alarm notifications: a node reaching HIGH, a camera that stops answering and, while armed, a node that goes offline or silent. An event is never announced twice and the first launch does not replay the history.
- Optional background watch (a foreground service with a permanent notification) that uses the current session; when it expires, or Android ends the service, it says so instead of failing silently.
- Everything refreshes every ten seconds while the app is on screen. 14 unit tests. **Not run on a phone**: the notification permission flow and the service lifecycle are unverified.

## [0.2.2]

- Plain HTTP is now accepted only towards private-LAN or loopback IPv4 literals; host names that
  merely look like a private address and addresses with leading zeros are refused.
- Applied the ARMOR look (near-black surfaces, cyan accent, amber for attention).
- Added unit tests for the endpoint-safety rules and documented the client boundary.

## [0.2.1]

- Added server IP, port, username and password login before the mobile console.
- Reused the ARMOR-SERVER HttpOnly Studio session for live-camera controls,
  PTZ, snapshots, recording and evidence; no operator token is requested or
  stored on the phone.

## [0.2.0]

- Replaced the Android shell with an ARMOR-SERVER mobile operator client.
- Added real MJPEG camera monitoring, PTZ, snapshots, recording control and
  evidence-library operations through the server session boundary.
- Added a project Gradle wrapper and endpoint-validation regression test.
