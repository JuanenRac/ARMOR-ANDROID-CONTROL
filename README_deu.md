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
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### Mobiler Bediener-Client für ARMOR-SERVER: Alarme, Kameras, Radar, Energie, das Netzwerk und die Einrichtung der Knoten, in acht Sprachen

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-228%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.3-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Ehrlichkeitsprüfung - was heute läuft:** Die Regeln für Adresssicherheit, Alarmentscheidung, Alarmwächter, Übersetzung und die Nur-Lese-Rolle sind von 228 Unit-Tests abgedeckt, und die App baut (`gradlew testDebugUnitTest assembleDebug`). Sie läuft auf einem echten Telefon gegen den CM5-Testaufbau: Anmeldung, Kameramonitor, Scharfschalten und Alarme, Radar, die Einrichtung eines Knotens per Bluetooth und die Updates von GitHub sind im Einsatz. **Noch nicht** bewiesen: die Solar-, Elektro- und Alarmzentralen-Bildschirme mit echten Geräten (es gibt nur einen Radarknoten), Push über Firebase (es ist in der App und braucht ein eigenes Firebase-Projekt; es wurde noch nicht auf einem Handy erprobt) und die Alarmüberwachung im Hintergrund unter den Akkuregeln jedes Herstellers.

---

## 📋 Auf einen Blick

| | |
|---|---|
| Version | `0.7.3` |
| Reifegrad | Etabliert |
| Tests | 228 (`gradlew testDebugUnitTest`) |
| Technik | Kotlin / Jetpack Compose |
| Auf echter Hardware | Auf einem echten Telefon gegen den CM5-Testaufbau im Einsatz; die Solar-, Elektro- und Alarmzentralen-Bildschirme warten auf echte Knoten |

## 🎯 Überblick

* **Anmeldung mit dem Login des Servers selbst:** IP, Port, Benutzer und Passwort; das Passwort erzeugt eine HttpOnly-Sitzung; es wird nur auf dem Telefon gespeichert, wenn Sie **Merken** ankreuzen (standardmäßig aus: Benutzer und Passwort mit einem eigenen Keystore-Schlüssel versiegelt, nur für diesen Server, und sofort vergessen, sobald Sie den Haken entfernen). Eine Rolle **Viewer** sieht nur an: Jede Schaltfläche, die etwas ändert, ist aus.
* **Kameramonitor:** 1 bis 16 Kacheln, eine vergrößerte Ansicht, Live-MJPEG mit erhaltenem Seitenverhältnis, ein begrenztes PTZ-Pad, Schnappschüsse und Aufnahmen, mit Betrachter und Löschbestätigung für Aufnahmen.
* **Scharf- und Unscharfschalten** nach einer **Prüfung vor dem Scharfschalten** (was offen, stumm oder ausgefallen ist) und mit der PIN zum Unscharfschalten; **Alarme** zum Quittieren, mit einem Zähler für wartende; **Geräte** (Rauch, Gas, Wasser, Tür, Fenster, Bewegung, Klima, Steckdosen, Lichter, Sirenen, Schlösser, Rollläden, Tor- und Garagenantriebe, Warmwasserbereiter) mit Zustand und Ein / Aus / Umschalten.
* **Alarme, die Sie erreichen:** ein Ton für jede Stufe, ein optionaler **gesprochener Alarm**, Benachrichtigungen bei einem Knoten, der HOCH erreicht, einer stummen Kamera, einem Gerätealarm und (scharf) einem Offline-Knoten, und **eine Zentrale, die läutet, weckt das Telefon**; die Hintergrundüberwachung übersteht einen Neustart und hält sich zurück, wenn der Server aus ist.
* **Live-Radar in 2D und 3D:** der Reiter Radar zeichnet den Standort, wie er in Studio entworfen wurde, und die Personen, die die Radare darauf sich bewegen sehen, jede anderthalb Sekunden aktualisiert; ziehen, zoomen und drehen Sie die 3D-Ansicht.
* **Solar- und Elektroenergie:** die Summen (Sonne, Verbrauch, Batterien mit ihrer Ladung, ob das Netz da ist), jeder Wechselrichter und jede Batterie mit den Zellen auf Abruf sowie die Kanäle und Alarme der Elektroknoten.
* **Alarmzentralen:** der Zustand jedes ARMOR-ALARM-Knotens, seine Zonen und Ereignisse und Scharf-/Unscharf-Befehle, wenn der Server sie erlaubt.
* **Netzwerk:** die Geräte des Hausnetzes, der Internetzustand und die **öffentliche Adresse** mit Anbieter und Ort, wie der Netzwerkknoten sie meldet.
* **Einen Feldknoten per Bluetooth einrichten** (vom Anmeldebildschirm oder *Mehr > Knoten einrichten*): findet die Knoten, die `ARMOR-xxxxxx` senden, legt den Administrator eines neuen an oder meldet sich an (mit der PIN des Knotens), sucht WLAN-Netze und setzt Namen, das WLAN eines Routers oder eine feste Adresse und den Broker; ein **Knotenfinder** listet die bereits im Netz vorhandenen.
* **Der Assistent:** geschriebene und gesprochene Befehle (geschlossene Liste, in acht Sprachen, mit einer an Sie gebundenen Bestätigung), um den Zustand des Hauses zu erfragen oder scharf, unscharf, stumm und Licht ein oder aus zu schalten.
* **Beweisbibliothek,** Zustand von Perimeter und Knoten, ein **Verlauf** jedes Alarms, Knotens, jeder Kamera, jedes Geräts, Alarms und Moduswechsels, die **Wetterkarte** sowie die **Dienste** und Updates der Anlage.
* **Acht Sprachen** (Englisch, Spanisch, Deutsch, Französisch, Italienisch, Japanisch, Chinesisch und Portugiesisch), gewählt unter *Mehr > Einstellungen* oder dem Telefon folgend; geprüft wird, dass jeder Satz in den acht Tabellen steht.
* **Vorsicht mit dem Passwort:** Klartext-HTTP ist nur zu einem IPv4-*Literal* eines privaten LANs oder Loopbacks erlaubt; ein Hostname, der nur wie eine private Adresse beginnt (`10.attacker.example`), oder eine Adresse mit führender Null wird abgelehnt. Das Aussehen besteht aus Symbolen: fast schwarze Flächen, ein Cyan-Akzent, Bernstein für Aufmerksamkeit.
* **Sicherheit vom Handy aus:** der zweite Faktor und seine Wiederherstellungscodes, die offenen Sitzungen, eine Prüfung, wie der Server das Handy sieht, der neue Identitätsnachweis für kritische Aktionen von außen und, für einen Administrator, das VPN (Geräte mit ihrer Konfiguration) und der dynamische DNS-Name.**Das ganze System auf dem Statusbildschirm und der Weg von außen:** der Statusbildschirm fasst Alarmzentralen, Dienste, aufzeichnende Kameras sowie Sicherheit und Fernzugriff zusammen, jede Karte öffnet ihren Bildschirm; und ist die eingegebene Adresse außerhalb des Hauses und ohne https, sagt die Anmeldung, warum sie nicht weitergeht, und eine Anleitung erklärt die zwei sicheren Wege (VPN oder öffentliche Adresse mit https).
* **Zustand der Knoten und Scharfschaltmodi:** ein Bildschirm Zustand der Knoten unter Mehr (Laufzeit, Neustartgrund, Speicher, Verbindung und Signal, Brokerverbindungen, Neustarts, letzter Fehler und die Warnungen des Servers je Knoten) und auf dem Bildschirm der Alarmzentralen eine Modusauswahl (Abwesend, Zuhause, Nacht und eigene), die eine Zentrale im gewählten Modus scharf schaltet, wenn ihre Firmware die Modi kennt. Acht Sprachen.
* **Verbindungen im Verlauf:** Der Verlaufsbildschirm hat eine zweite Liste, nur lesend, mit den Geräten und Apps, die sich mit dem Server verbinden (Studio, diese App, die Panels der Knoten, der Sprachdienst), wann jedes zuletzt gesehen wurde, seinen offenen Sitzungen sowie Anmeldungen und Ablehnungen; die App sagt dem Server jetzt, was sie ist, und erscheint so als sie selbst. Adressen sehen nur Administratoren.
* **Batteriewartung (Administratoren):** die einfachen Operationen an einer Pylontech-Batterie - Uhr, Zähler und Protokolle, Neustart, Kapazität, MOSFET-Test, Sicherungsfehler, Ausgänge - in denselben zwei Schritten wie in Studio (der Server lässt den Knoten die Batterie lesen, der Bildschirm zeigt genau, was getan wird, dann "Jetzt ausführen" mit nochmals eingetippter Seriennummer), mit Ergebnis und Änderungen; Grenzwerte, Firmware und Expertenkonsole bleiben in Studio.

## 🧩 Rolle in A.R.M.O.R.

ARMOR-ANDROID-CONTROL ist das Fenster des Telefons auf denselben [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER), den auch [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) nutzt: Es liest und befiehlt nur über ihn. Hier folgen Ihnen die Alarme (eine läutende Alarmzentrale weckt das Telefon), hier fragen Sie den Assistenten per Sprache, und es ist das Werkzeug, um vor Ort einen neuen Knoten per Bluetooth einzurichten.

## 📂 Struktur des Repositorys

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, PinDialog.kt, MjpegFeed.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md   (the Firebase steps, in eight languages)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ Entwicklungsumgebung

**Voraussetzungen:** JDK 17+, the Android SDK (compile SDK 37), an Android 8.0+ phone (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 219 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Die Versionen werden auf GitHub veröffentlicht, von wo die App selbst nach Updates sucht; die APK ist mit dem eigenen Release-Schlüssel des Betreuers signiert, der außerhalb des Repositorys liegt, und die App vergleicht das Signaturzertifikat eines Downloads mit dem eigenen, bevor sie die Datei an das Installationsprogramm von Android übergibt. Siehe die [Client-Grenze](docs/CLIENT_BOUNDARY.md).

## 🔗 Verwandte Projekte

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) ist ein Perimeter-Sicherheitssystem aus unabhängigen Repositorys. Jedes hat eine eigene Version, eigene Tests und ein eigenes README; hier ist die Familie:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Nachrichtenverträge, Validierer, Konformitätsvektoren und generierte Typen
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - Feldknoten-Firmware für ESP32-S3 mit drei Radaren und eigenem Web-Panel
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - Protokolle für Solar-Wechselrichter und -Batterien und die Nachrichten eines Gateway-Knotens
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - Elektroknoten: Zähler, die Nachricht der Netzmesswerte und die Regeln fürs Schalten
* **[ARMOR-ALARM](https://github.com/JuanenRac/ARMOR-ALARM)** - Alarmknoten und Alarmzentrale: Zonen, Scharfschalten, Verzögerungen, Sirene und PIN, mit dem Server oder ohne ihn
* **[ARMOR-HMI](https://github.com/JuanenRac/ARMOR-HMI)** - Touch-Panel: der Systemzustand auf einem Wandbildschirm, Scharf- und Quittieren sowie das Zuhause des Sprachassistenten
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - Das lokale Netzwerk: seine Geräte, das Internet und was sich ändert
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Zentraler Koordinator: Telemetrie, Alarme, Geräte, Solarmesswerte und Kameras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web-Konsole: Kameras, Radar, Alarme, Solarenergie und 2D/3D-Standortdesigner
* **ARMOR-ANDROID-CONTROL** (dieses Repository) - Android-Bedienclient mit Live-Radar in 2D/3D
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Visuelle Inferenzrichtlinie, die ihre Entscheidungen erklärt und nie handelt
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Kamerazonen, Masken und Erkennungsregeln für den Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Offline-Sprachabsichten mit einer nicht fälschbaren Bestätigung
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Gehäuse, Elektronik und die Abnahmematrix am Prüfstand
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Bereitstellung, CM5-Prüfstand, Backup und TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Offline-Telemetriesimulator mit wiederholbaren Fehlern
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Erkennt, installiert und aktualisiert die eigenen Repositories des Ökosystems
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architektur, Sicherheitsgrundlage und die Fähigkeitsmatrix

## 📚 Dokumentation und Community

Hier gibt es mehr zu lesen:

* [Fähigkeitsmatrix: was belegt ist und was nicht](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Projektkatalog: Versionen und wie die Repositorys voneinander abhängen](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Änderungsverlauf dieses Repositorys](CHANGELOG.md)
* [Lizenz (GPL-3.0-or-later)](LICENSE)
* Fragen, Ideen und Meldungen: electrohobby3d@gmail.com

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LIZENZ

GPL-3.0-or-later - siehe [LICENSE](LICENSE).
