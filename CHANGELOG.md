# Changelog

All notable changes to this project are documented here.

## [0.3.1] - The names of the new inverter and battery models

- The solar screens name the models of the server's new catalogue (Axpert, PIP, Revo, InfiniSolar, LV5048, SunGoldPower, the Pylontech and Pytes families) and read an ANT-BMS combination (`ant-bms-24s-200a` shows as ANT-BMS 24S · 200 A); a model it does not know is shown by its identifier.

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
