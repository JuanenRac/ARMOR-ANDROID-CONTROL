<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">🇺🇸 <b>English</b> | <a href="README_spa.md">🇪🇸 Español</a></p>

### Mobile operator client for ARMOR-SERVER

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**Honesty check - what runs today:** The endpoint-safety rules have unit tests and the app builds. It has **not been run on a phone against the server**, and it deliberately cannot arm or disarm the system.

---

## 1. 🛠️ OVERVIEW

* **Sign in with the server's own login:** IP, port, user and password; the password creates an HttpOnly session and is never stored on the phone.
* **Camera monitor:** 1 to 16 tiles, a maximized view, live MJPEG that keeps the picture ratio, a bounded PTZ pad, snapshots and recordings.
* **Evidence library** and the perimeter and node state.
* **Careful with the password:** plain HTTP is allowed only to a private-LAN or loopback IPv4 *literal*. A host name that merely starts like a private address (`10.attacker.example`) or an address with a leading zero (some resolvers read `010.0.0.1` as the public `8.0.0.1`) is refused.
* The Hydra look: near-black surfaces, a cyan accent, amber for attention.

---

## 2. 🔧 BUILD & RUN

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug APK is not signed for distribution. See the [client boundary](docs/CLIENT_BOUNDARY.md).

---

## 📂 DIRECTORY STRUCTURE

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt, ArmorTheme.kt, ArmorViewModel.kt, ServerEndpoint.kt, MjpegFeed.kt
│   ├── network/   model/
└── app/src/test/   endpoint-safety tests
```

---

## 👤 AUTHOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENSE

GPL-3.0-or-later - see [LICENSE](LICENSE).
