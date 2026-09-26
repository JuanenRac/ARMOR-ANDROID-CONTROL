<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  🇩🇪 <b>Deutsch</b> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a>
</p>

### Mobiler Bedienclient für ARMOR-SERVER

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**Ehrlichkeitsprüfung - was heute läuft:** Die Regeln für Adresssicherheit und Alarmentscheidung haben Unit-Tests (53) und die App baut. Sie wurde **nicht auf einem Telefon gegen den Server ausgeführt**, daher sind der Berechtigungsablauf der Benachrichtigungen und der Hintergrunddienst ungeprüft. Scharfschalten, Alarme und Geräte folgen den Routen des Servers, wurden aber auf keinem echten Telefon ausprobiert.

---

## 🎯 Überblick

* **Anmeldung mit dem Login des Servers:** IP, Port, Benutzer und Passwort; das Passwort erzeugt eine HttpOnly-Sitzung und wird nie auf dem Telefon gespeichert.
* **Kameramonitor:** 1 bis 16 Kacheln, eine maximierte Ansicht, MJPEG live mit erhaltenem Bildverhältnis, ein begrenztes PTZ-Pad, Schnappschüsse und Aufnahmen.
* **Scharf- und Unscharfschalten** nach einer Bestätigung; **Alarme** zum Quittieren, mit einem Badge für wartende; **Geräte** (Rauch, Gas, Wasser, Tür, Fenster, Bewegung, Klima, Steckdosen, Lichter, Sirenen, Schlösser) mit ihrem Zustand und Ein / Aus / Umschalten.
* **Live-Radar in 2D und 3D:** der Radar-Tab zeichnet den Standort, wie er in Studio entworfen wurde (Gelände, Gebäude, Bäume, Pfosten, die Felder der Radare und Kameras) und die Personen, die die Radare sich bewegen sehen, alle anderthalb Sekunden aktualisiert; die 3D-Ansicht lässt sich ziehen, zoomen und drehen. Die Platzierung folgt den Regeln von Studio, hat aber nie ein echtes Radar gezeigt.
* **Solar:** ein Eintrag in *Más* (und eine Kachel im Status) zeigt die Summen (Sonne, Verbrauch, Batterien mit Ladung und Fluss, ob das Netz da ist) und jeden Wechselrichter und jede Batterie, die der Server meldet: ihre Werte, auf Wunsch die Zellen einer Batterie mit markierter höchster und niedrigster, die Geräte, die noch auf Daten warten, und eine Markierung bei Beispielwerten oder einem stummen Gerät. Alle fünf Sekunden aktualisiert, solange sie offen ist; gegen die Antwortformen des Servers getestet und in einem Emulator gegen einen lokalen Server mit Beispielwerten ausprobiert, nie mit echten Geräten.
* **Einen Feldknoten per Bluetooth einrichten** (vom Anmeldebildschirm oder *Más > Configurar un nodo*): findet Knoten, die `ARMOR-xxxxxx` senden, legt den Administrator eines neuen an oder meldet sich an, sucht WLAN-Netze und setzt den Namen des Knotens, das WLAN eines Routers oder eine feste Adresse und den Broker, für Knoten ohne Ethernet-Kabel. Gebaut und per Unit-Test geprüft, nie gegen einen Knoten oder ein Telefon ausgeführt.
* **Beweisbibliothek,** Zustand von Perimeter und Knoten und ein **Verlauf** jeder Warnung, jedes Knotens, jeder Kamera, jedes Geräts, jedes Alarms und jedes Moduswechsels.
* **Alarmbenachrichtigungen** für einen Knoten, der HIGH erreicht, eine Kamera, die nicht mehr antwortet, einen Geräte-Alarm (Rauch, Gas, Wasser, Panik jederzeit; ein Tür-, Fenster- oder Bewegungssensor bei Scharfschaltung) und, scharf, einen Knoten, der offline geht; eine optionale Hintergrundüberwachung nutzt die aktuelle Sitzung und meldet, wenn sie endet.
* **Vorsicht mit dem Passwort:** einfaches HTTP ist nur zu einem privaten LAN- oder Loopback-IPv4-*Literal* erlaubt. Ein Hostname, der nur wie eine private Adresse beginnt (`10.attacker.example`), oder eine Adresse mit führender Null (manche Resolver lesen `010.0.0.1` als die öffentliche `8.0.0.1`) wird abgelehnt.
* **Ein Aussehen aus Symbolen:** fast schwarze Flächen, ein Cyan-Akzent, Bernstein für Aufmerksamkeit, große Symbole mit wenigen Worten, eine untere Leiste, eine Info-Seite und eine Abmelde-Schaltfläche.

## 📂 Struktur des Repositorys

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt (splash, sign-in, account, About), HomeScreens.kt, CameraScreens.kt, MoreScreens.kt, SolarScreens.kt
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmWatcherService.kt
│   ├── ArmorTheme.kt, ServerEndpoint.kt, MjpegFeed.kt
│   ├── network/   model/
└── app/src/test/   endpoint-safety and plain-words tests
```

## 🛠️ Entwicklungsumgebung

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Das Debug-APK ist nicht für die Verteilung signiert. Siehe die [Client-Grenze](docs/CLIENT_BOUNDARY.md).

## 🔗 Verwandte Projekte

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) ist ein Perimeter-Sicherheitssystem aus unabhängigen Repositorys. Jedes hat eine eigene Version, eigene Tests und ein eigenes README; hier ist die Familie:

* **[ARMOR-COMMON](../ARMOR-COMMON)** - Nachrichtenverträge, Validierer, Konformitätsvektoren und generierte Typen
* **[ARMOR-RADAR](../ARMOR-RADAR)** - Feldknoten-Firmware für ESP32-S3 mit drei Radaren und eigenem Web-Panel
* **[ARMOR-SOLAR](../ARMOR-SOLAR)** - Protokolle für Solar-Wechselrichter und -Batterien und die Nachrichten eines Gateway-Knotens
* **[ARMOR-SERVER](../ARMOR-SERVER)** - Zentraler Koordinator: Telemetrie, Alarme, Geräte, Solarmesswerte und Kameras
* **[ARMOR-STUDIO](../ARMOR-STUDIO)** - Web-Konsole: Kameras, Radar, Alarme, Solarenergie und 2D/3D-Standortdesigner
* **ARMOR-ANDROID-CONTROL** (dieses Repository) - Android-Bedienclient mit Live-Radar in 2D/3D
* **[ARMOR-SERVER-AI](../ARMOR-SERVER-AI)** - Visuelle Inferenzrichtlinie, die ihre Entscheidungen erklärt und nie handelt
* **[ARMOR-VOICE-AI](../ARMOR-VOICE-AI)** - Offline-Sprachabsichten mit einer nicht fälschbaren Bestätigung
* **[ARMOR-HARDWARE](../ARMOR-HARDWARE)** - Gehäuse, Elektronik und die Abnahmematrix am Prüfstand
* **[ARMOR-DEVOPS](../ARMOR-DEVOPS)** - Bereitstellung, CM5-Prüfstand, Backup und TLS
* **[ARMOR-SIMULATOR](../ARMOR-SIMULATOR)** - Offline-Telemetriesimulator mit wiederholbaren Fehlern
* **[ARMOR-DOCS](../ARMOR-DOCS)** - Architektur, Sicherheitsgrundlage und die Fähigkeitsmatrix

## 📚 Dokumentation und Community

Hier gibt es mehr zu lesen:

* [Fähigkeitsmatrix: was belegt ist und was nicht](../ARMOR-DOCS/docs/CAPABILITY_MATRIX.md)
* [Projektkatalog: Versionen und wie die Repositorys voneinander abhängen](../ARMOR-DOCS/docs/PROJECT_CATALOG.md)
* [Änderungsverlauf dieses Repositorys](CHANGELOG.md)
* [Lizenz (GPL-3.0-or-later)](LICENSE)
* Fragen, Ideen und Meldungen: electrohobby3d@gmail.com

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LIZENZ

GPL-3.0-or-later - siehe [LICENSE](LICENSE).
