# The mobile client's boundary

ARMOR-ANDROID-CONTROL consumes ARMOR-SERVER's API. It does not connect to cameras over RTSP, and it never receives the cameras' passwords, their RTSP paths or the key that encrypts the camera configuration.

## The current flow

1. The operator enters an HTTPS origin, or an HTTP origin limited to a private LAN address (`10/8`, `172.16/12`, `192.168/16`, loopback).
2. The client reads `status` and `camera-views`, both read-only.
3. For sensitive actions, the operator token is exchanged for a temporary HttpOnly cookie through `POST /api/v1/operator/session`.
4. With the session, PTZ, snapshots, recording and the evidence catalogue are enabled. The token is cleared from the field and is never stored in the preferences.
5. Video uses ARMOR-SERVER's MJPEG relay. The phone never builds an RTSP URL and so it cannot reveal a camera password.

## Deliberate limits

- Arming and disarming need an on-screen confirmation and a signed-in session; the server records it with the user's name. Acknowledging an alarm does not close it: it closes by itself when its cause ends.
- HTTP is allowed only for a current home/LAN installation. Exposure outside that network must terminate TLS before the APK is distributed.
- **Updates.** On every cold start the app asks GitHub's releases for a newer stable version (`vMAJOR.MINOR.PATCH`) and, if there is one, offers it; the APK is the release's own asset, named exactly `ARMOR-ANDROID-CONTROL-release.apk`, and it is handed to Android's installer, which checks its signature before replacing the app. Nothing is downloaded or installed without an explicit tap. A release is signed with a stable key and carries a `versionCode` higher than the previous one.

## Setting up a field node over Bluetooth

For a node with no Ethernet cable, or no address yet, the app configures it over Bluetooth Low Energy with the same protocol as its web panel (`docs/BLE_PROVISIONING.md` of ARMOR-RADAR, ARMOR-SOLAR and ARMOR-ELECTRICAL: the three kinds of node speak the same protocol and the app says which one it is). It looks for the nodes that advertise `ARMOR-xxxxxx`, connects, creates the administrator of a new node with its set-up code (or signs in), searches for Wi-Fi networks, sets the name, the connection (cable or the Wi-Fi of a router), DHCP or a fixed address (address, mask, gateway, DNS) on either connection, the broker and the Bluetooth mode, and restarts the node.

- **The outcome is read back.** After the restart the app looks for the node again over Bluetooth and reads its `hello`: whether it joined (its address and network) or why it did not (network not found, wrong password, or only "it did not connect"), with buttons to check again or to change the configuration. A node set up to join a Wi-Fi network keeps advertising until it has an address. This needs the node firmware that reports `sta_error`; with an older one the app says only that it cannot tell.
- **The form is checked before it is sent:** a Wi-Fi name is needed, a Wi-Fi password has 8 to 63 characters, and a fixed address, mask and gateway must be valid and in the same network.
- Permissions: `BLUETOOTH_SCAN` (declared as not used for location) and `BLUETOOTH_CONNECT`; location is asked for only up to Android 11, where scanning requires it.
- The link is encrypted ("just works" pairing): it stops someone listening in, not someone present while the phone pairs. The administrator password and the set-up code travel inside that link.
- The app does not store the code, the fleet secret or the passwords.
- **Verification:** the protocol's format, its operations, the set-up code calculation, the form checks and the reading of the outcome have JVM tests, but the flow has not been run end to end against a node and a phone by the tests; pairing, the MTU and reconnecting are the first things to check on the bench.
