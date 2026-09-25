# Changelog

All notable changes to this project are documented here.

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
