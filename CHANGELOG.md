# Changelog

All notable changes to this project are documented here.

## [0.3.0] - 2026-09-25

- Plain HTTP is now accepted only towards private-LAN or loopback IPv4 literals; host names that
  merely look like a private address and addresses with leading zeros are refused.
- Applied the ARMOR look (near-black surfaces, cyan accent, amber for attention).
- Added unit tests for the endpoint-safety rules and documented the client boundary.

## [0.2.1] - 2026-09-24

- Added server IP, port, username and password login before the mobile console.
- Reused the ARMOR-SERVER HttpOnly Studio session for live-camera controls,
  PTZ, snapshots, recording and evidence; no operator token is requested or
  stored on the phone.

## [0.2.0] - 2026-09-24

- Replaced the Android shell with an ARMOR-SERVER mobile operator client.
- Added real MJPEG camera monitoring, PTZ, snapshots, recording control and
  evidence-library operations through the server session boundary.
- Added a project Gradle wrapper and endpoint-validation regression test.
