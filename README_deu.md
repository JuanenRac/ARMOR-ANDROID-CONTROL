<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL-Banner" width="100%">
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
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 oder neuer">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Sprache">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Sprachen">
  <img src="https://img.shields.io/badge/Tests-376%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.8-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Reifegrad">
</p>

---

**Ehrlichkeitsprüfung - was heute läuft:** Die Regeln für Adresssicherheit, Alarmentscheidung, Alarmwächter, Übersetzung und die Nur-Lese-Rolle sind von 376 Unit-Tests abgedeckt, und die App baut (`gradlew testDebugUnitTest assembleDebug`). Sie läuft auf einem echten Telefon gegen den CM5-Testaufbau: Anmeldung, Kameramonitor, Scharfschalten und Alarme, Radar, die Einrichtung eines Knotens per Bluetooth und die Updates von GitHub sind im Einsatz. **Noch nicht** bewiesen: die Solar-, Elektro- und Alarmzentralen-Bildschirme mit echten Geräten (es gibt nur einen Radarknoten), Push über Firebase (es ist in der App und braucht ein eigenes Firebase-Projekt; es wurde noch nicht auf einem Handy erprobt) und die Alarmüberwachung im Hintergrund unter den Akkuregeln jedes Herstellers.

---

## 📋 Auf einen Blick

| | |
|---|---|
| Version | `0.7.8` |
| Reifegrad | Etabliert |
| Rolle | Android-Bedienclient für ARMOR-SERVER (Familie: Clients, übergeordnet: ARMOR-SERVER) |
| Technik | Kotlin / Jetpack Compose, Android 8.0+ |
| Tests | 376 (`gradlew testDebugUnitTest`) |
| Sprachen | 8 (1345 Sätze je Sprache, jeder Satz in allen Tabellen geprüft) |
| Läuft auf | Ein Telefon mit Android 8.0+ (minSdk 26) |
| Auf echter Hardware | Auf einem echten Telefon gegen den CM5-Testaufbau im Einsatz; die Solar-, Elektro- und Alarmzentralen-Bildschirme warten auf echte Knoten |
| Lizenz | GPL-3.0-or-later |

**Springe zu:** [Überblick](#-überblick) · [Architektur](#-architektur) · [Sicherheit](#-sicherheit-und-schutz) · [Schnittstellen](#-schnittstellen) · [Konfiguration](#-konfiguration) · [Schnellstart](#-schnellstart) · [Tests](#-tests) · [Probleme](#-fehlerbehebung) · [Struktur](#-struktur-des-repositorys) · [Verwandte](#-verwandte-projekte)

## 🚦 Projektstatus

Jede Zeile ist ein Teil dieses Repositorys, und die Ampel sagt, wie weit er heute ist. Sie beruht auf dem, was das Repository zeigen kann (Tests, Builds, Läufe), nicht auf dem, was geplant ist.

🟢 Fertig und überprüft · 🟡 Funktioniert, mit bekannten Grenzen · 🟠 Teilweise fertig oder noch nicht überprüft · 🔴 Nicht gemacht oder funktioniert nicht

| Bereich | Stand | Was das hier bedeutet |
|---|---|---|
| Software | 🟢 | Die App wird gebaut (`gradlew testDebugUnitTest assembleDebug`) und läuft auf einem echten Telefon gegen den CM5-Prüfstand. |
| Tests | 🟢 | 376 Unit-Tests bestehen (`gradlew testDebugUnitTest`): Adresssicherheit, Alarmentscheidung, Alarmüberwachung, Übersetzungen und die Nur-Lese-Rolle. |
| Oberfläche | 🟡 | Anmeldung, Kameramonitor, Scharfschalten und Alarme sowie das Radar sind auf einem echten Telefon im Einsatz; die Bluetooth-Einrichtung eines Knotens ist an einem echten Knoten nicht bestätigt, und die Bildschirme für Solar, Elektrik und Alarmzentrale sind noch keinen echten Geräten begegnet. |
| Hardware | 🟡 | Auf einem echten Telefon gegen den CM5-Prüfstand genutzt; die Alarmüberwachung im Hintergrund unter den Akkuregeln jedes Herstellers ist nicht belegt. |
| Sicherheit | 🟡 | Die Regeln für Adresssicherheit und die Nur-Lese-Rolle sind durch Unit-Tests abgedeckt; es gab keine unabhängige Prüfung. |
| Aktualisierungen | 🟡 | Die Aktualisierungen von GitHub werden auf dem Telefon genutzt. |
| Integration | 🟠 | Push über Firebase ist in der App, braucht aber ein eigenes Firebase-Projekt und wurde auf einem Telefon nicht erprobt. |
| Dokumentation | 🟢 | Acht Sprachen und das Änderungsprotokoll sind aktuell. |

## 🎯 Überblick

* **Anmeldung mit dem Login des Servers selbst:** IP, Port, Benutzer und Passwort; das Passwort erzeugt eine HttpOnly-Sitzung; es wird nur auf dem Telefon gespeichert, wenn Sie **Merken** ankreuzen (standardmäßig aus: Benutzer und Passwort mit einem eigenen Keystore-Schlüssel versiegelt, nur für diesen Server, und sofort vergessen, sobald Sie den Haken entfernen). Eine Rolle **Viewer** sieht nur an: Jede Schaltfläche, die etwas ändert, ist aus.
* **Sperre mit dem Telefon:** Die App verlangt Fingerabdruck, Gesicht oder PIN des Telefons selbst vor dem Scharfschalten, Entschärfen, Stummschalten einer Sirene, Entsperren und den anderen kritischen Befehlen (standardmäßig nur aktiv, wenn das Telefon Fingerabdruck oder Gesicht hat) und, wenn du es einschaltest, zum Öffnen der App. Dein Administrator kann es in Studio verlangen, aber nie unter deine Wahl senken; ein Telefon ohne Bildschirmsperre wird nie ausgesperrt, und der gesperrte Bildschirm bietet immer die erneute Anmeldung mit dem Passwort an. Optional (standardmäßig aus, mit sichtbarem Hinweis) lässt sich der Schlüssel, der die gespeicherte Sitzung versiegelt, an das Entsperren des Telefons binden; dann wartet die Alarmüberwachung im Hintergrund nach einem Neustart, bis Sie das Telefon entsperren.
* **Kameramonitor:** 1 bis 16 Kacheln, eine vergrößerte Ansicht, Live-MJPEG mit erhaltenem Seitenverhältnis, ein begrenztes PTZ-Pad, Schnappschüsse und Aufnahmen, mit Betrachter und Löschbestätigung für Aufnahmen.
* **Scharf- und Unscharfschalten** nach einer **Prüfung vor dem Scharfschalten** (was offen, stumm oder ausgefallen ist) und mit der PIN zum Unscharfschalten; **Alarme** zum Quittieren, mit einem Zähler für wartende; **Geräte** (Rauch, Gas, Wasser, Tür, Fenster, Bewegung, Klima, Steckdosen, Lichter, Sirenen, Schlösser, Rollläden, Tor- und Garagenantriebe, Warmwasserbereiter) mit Zustand und Ein / Aus / Umschalten.
* **Alarme, die Sie erreichen:** ein Ton für jede Stufe, ein optionaler **gesprochener Alarm**, Benachrichtigungen bei einem Knoten, der HOCH erreicht, einer stummen Kamera, einem Gerätealarm und (scharf) einem Offline-Knoten, und **eine Zentrale, die läutet, weckt das Telefon**; die Hintergrundüberwachung übersteht einen Neustart und hält sich zurück, wenn der Server aus ist.
* **Live-Radar in 2D und 3D:** der Reiter Radar zeichnet den Standort, wie er in Studio entworfen wurde, und die Personen, die die Radare darauf sich bewegen sehen, jede anderthalb Sekunden aktualisiert; ziehen, zoomen und drehen Sie die 3D-Ansicht.
* **Solar- und Elektroenergie:** die Summen (Sonne, Verbrauch, Batterien mit ihrer Ladung, ob das Netz da ist), jeder Wechselrichter und jede Batterie mit den Zellen auf Abruf sowie die Kanäle und Alarme der Elektroknoten.
* **Alarmzentralen:** der Zustand jedes ARMOR-NODE-ALARM-Knotens, seine Zonen und Ereignisse und Scharf-/Unscharf-Befehle, wenn der Server sie erlaubt.
* **Netzwerk:** die Geräte des Hausnetzes, der Internetzustand und die **öffentliche Adresse** mit Anbieter und Ort, wie der Netzwerkknoten sie meldet.
* **Einen Feldknoten per Bluetooth einrichten** (vom Anmeldebildschirm oder *Mehr > Knoten einrichten*): findet die Knoten, die `ARMOR-xxxxxx` senden, legt den Administrator eines neuen an oder meldet sich an (mit der PIN des Knotens), sucht WLAN-Netze und setzt Namen, das WLAN eines Routers oder eine feste Adresse und den Broker; ein **Knotenfinder** listet die bereits im Netz vorhandenen.
* **Der Assistent:** geschriebene und gesprochene Befehle (geschlossene Liste, in acht Sprachen, mit einer an Sie gebundenen Bestätigung), um den Zustand des Hauses zu erfragen oder scharf, unscharf, stumm und Licht ein oder aus zu schalten.
* **Beweisbibliothek,** Zustand von Perimeter und Knoten, ein **Verlauf** jedes Alarms, Knotens, jeder Kamera, jedes Geräts, Alarms und Moduswechsels, die **Wetterkarte** sowie die **Dienste** und Updates der Anlage.
* **Acht Sprachen** (Englisch, Spanisch, Deutsch, Französisch, Italienisch, Japanisch, Chinesisch und Portugiesisch), gewählt unter *Mehr > Einstellungen* oder dem Telefon folgend; geprüft wird, dass jeder Satz in den acht Tabellen steht.
* **Vorsicht mit dem Passwort:** Klartext-HTTP ist nur zu einem IPv4-*Literal* eines privaten LANs oder Loopbacks erlaubt; ein Hostname, der nur wie eine private Adresse beginnt (`10.attacker.example`), oder eine Adresse mit führender Null wird abgelehnt. Das Aussehen besteht aus Symbolen: fast schwarze Flächen, ein Cyan-Akzent, Bernstein für Aufmerksamkeit.
* **Sicherheit vom Handy aus:** der zweite Faktor und seine Wiederherstellungscodes, die offenen Sitzungen, eine Prüfung, wie der Server das Handy sieht, der neue Identitätsnachweis für kritische Aktionen von außen und, für einen Administrator, das VPN (Geräte mit ihrer Konfiguration) und der dynamische DNS-Name.
* **Das ganze System auf dem Statusbildschirm und der Weg von außen:** der Statusbildschirm fasst Alarmzentralen, Dienste, aufzeichnende Kameras sowie Sicherheit und Fernzugriff zusammen, jede Karte öffnet ihren Bildschirm; und ist die eingegebene Adresse außerhalb des Hauses und ohne https, sagt die Anmeldung, warum sie nicht weitergeht, und eine Anleitung erklärt die zwei sicheren Wege (VPN oder öffentliche Adresse mit https).
* **Zustand der Knoten und Scharfschaltmodi:** ein Bildschirm Zustand der Knoten unter Mehr (Laufzeit, Neustartgrund, Speicher, Verbindung und Signal, Brokerverbindungen, Neustarts, letzter Fehler und die Warnungen des Servers je Knoten) und auf dem Bildschirm der Alarmzentralen eine Modusauswahl (Abwesend, Zuhause, Nacht und eigene), die eine Zentrale im gewählten Modus scharf schaltet, wenn ihre Firmware die Modi kennt. Acht Sprachen.
* **Verbindungen im Verlauf:** Der Verlaufsbildschirm hat eine zweite Liste, nur lesend, mit den Geräten und Apps, die sich mit dem Server verbinden (Studio, diese App, die Panels der Knoten, der Sprachdienst), wann jedes zuletzt gesehen wurde, seinen offenen Sitzungen sowie Anmeldungen und Ablehnungen; die App sagt dem Server jetzt, was sie ist, und erscheint so als sie selbst. Adressen sehen nur Administratoren.
* **Batteriewartung (Administratoren):** die einfachen Operationen an einer Pylontech-Batterie - Uhr, Zähler und Protokolle, Neustart, Kapazität, MOSFET-Test, Sicherungsfehler, Ausgänge - in denselben zwei Schritten wie in Studio (der Server lässt den Knoten die Batterie lesen, der Bildschirm zeigt genau, was getan wird, dann "Jetzt ausführen" mit nochmals eingetippter Seriennummer), mit Ergebnis und Änderungen; Grenzwerte, Firmware und Expertenkonsole bleiben in Studio.
* **Steuerung der Knoten (Administratoren):** Mehr > Knotensteuerung bietet dieselben Operationen wie Studio - Neustart, Pause, Fortsetzen, Standby (Arbeit aus, bleibt verbunden), Identifizieren, Diagnose und Wartung ein oder aus - für einen Knoten oder alle, verlangt die erneute Eingabe des Knotennamens (oder des Wortes RESTART, PAUSE oder STANDBY für alle) und den frischen Identitätsnachweis, zeigt, wie weit jeder Knoten ist, und warnt, dass eine pausierte oder im Standby befindliche Alarmzentrale oder ein Radar das Haus ungeschützt lässt. Es gibt kein Zurücksetzen auf Werkseinstellungen, kein Löschen und keinen Tiefschlaf.
* **Ein Alarmfenster in der geöffneten App:** Solange ein Alarm auf eine Person wartet, pulsiert über jedem Bildschirm außer dem der Alarme ein Banner (rot und schnell bei kritischen, bernsteinfarben bei den übrigen, ruhig, wenn das Telefon die Animationen abgeschaltet hat), nennt den schlimmsten Alarm und wie viele warten und lässt die App darunter bedienen; es kann die Alarme öffnen, direkt bestätigen oder bis zum nächsten Alarm geschlossen werden.
* **Von außerhalb des Hauses:** zwei Adressen (die private im Heim-WLAN, die für unterwegs mit Mobilfunkdaten oder VPN), gewählt nach dem Aufenthaltsort des Telefons; antwortet eine nicht, wird die andere versucht, und eine Meldung sagt, was fehlschlägt; der Schlüssel des Zertifikats der Installation wird beim ersten Mal gemerkt (oder mit dem Text des QR-Codes von Studio gekoppelt) und ein anderer abgelehnt, und einfaches http wird für einen Server abgelehnt, der schon per https erreicht wurde; das Panel eines Knotens lässt sich über den Server öffnen; Alarme können als reine Daten-Push eintreffen und werden einmal gemeldet, ob per Push oder durch die Überwachung; die geöffnete App und die Überwachung teilen sich eine Abfrage des Servers; die Kamera-Streams stoppen außerhalb des Bildschirms und kommen mit wachsenden Wartezeiten zurück.
* **Meldungen nach Wunsch, mit eigenen Tönen und einer eigenen Stimme:** In den Einstellungen lassen sich alle Meldungen stummschalten, ohne Ton und Vibration zeigen (und nicht vorlesen) und nach Schweregrad (hoch, niedrig) oder Art (Radar-Alarme, Kameras ohne Antwort, Knoten offline oder still sowie die Alarme von Geräten, Solar, Elektro, Netzwerk und Alarmpanels) abschalten; ein kritischer Alarm meldet sich immer, außer wenn alle Meldungen stumm sind. Ein Ton für jeden Schweregrad und jede Art von Meldung, aus den Tönen des Telefons oder aus einer eigenen Datei (unter 3 MB), jeweils mit Abspielknopf; und die Stimme, die die Alarme spricht (Frau, Mann oder egal; Kind, Erwachsener oder Senior; sachlich, Actionheld oder dunkler Lord; Tempo; ob auch Meldungen niedrigen Schweregrads gesprochen werden) mit Testknopf.
* **Ein Banner, das zum richtigen Bildschirm führt, und eine ruhigere Überwachung:** Das Banner eines Alarms öffnet den Bildschirm, der ihn bearbeitet (Kameras, Geräte, Wechselrichter oder Batterien, der Elektro-Bildschirm, das Netzwerk oder die Alarmpanels); das Telefon nennt dem Server beim Push-Registrieren seine Sprache, damit die Meldung darin geschrieben wird, und ein neues Firebase-Token erreicht den Server sofort aus dem Hintergrund. Eine Kamera mit zwei Objektiven in einem Stream sind zwei Fenster der Kameraliste. Solange die Push-Meldungen funktionieren, fragt die Hintergrundüberwachung den Server alle 120 s statt alle 10 s (*Einstellungen > Überwachung mit Push-Meldungen*: 10, 60, 120, 300 oder 600 s).
* **Scharfschaltmodi des Servers (Abwesend, Zuhause, Nacht):** Hat der Server die Scharfschaltmodi eingeschaltet (Studio, Konfiguration > Sicherheit), öffnet die Schaltfläche zum Scharfschalten auf dem Statusbildschirm eine Auswahl, um in einem Modus scharf zu schalten, zu wechseln oder unscharf zu schalten; ein Modus ohne Zuweisung verlangt eine Bestätigung, ein Wechsel zu einem Modus mit weniger Schutz verlangt die PIN wie das Unscharfschalten, und der Status zeigt den aktiven Modus. Bei ausgeschalteten Modi schaltet die App wie bisher scharf und unscharf.

## 🧩 Rolle in A.R.M.O.R.

ARMOR-ANDROID-CONTROL ist das Fenster des Telefons auf denselben [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER), den auch [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) nutzt: Es liest und befiehlt nur über ihn. Hier folgen Ihnen die Alarme (eine läutende Alarmzentrale weckt das Telefon), hier fragen Sie den Assistenten per Sprache, und es ist das Werkzeug, um vor Ort einen neuen Knoten per Bluetooth einzurichten.

| Name | Wie | Wozu |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | Alles, was die App liest und anordnet |
| Feldknoten | Bluetooth (`ARMOR-xxxxxx`) | Richtet einen neuen Knoten direkt neben ihm ein |
| GitHub | HTTPS | Prüft und lädt das signierte Update der App selbst |
| Open-Meteo | HTTPS | Die öffentliche Vorhersage, Luftqualität und Ortssuche der Wetterkarte |

## 🏛️ Architektur

<p align="center">
  <img src="images/architecture.svg" alt="Architektur von ARMOR-ANDROID-CONTROL: wer es aufruft, was es enthält und wovon es abhängt" width="100%">
</p>

## 🔒 Sicherheit und Schutz

| Bedrohung | Abwehr |
|---|---|
| Ein auf dem Telefon gespeichertes Passwort | Nur mit *Angemeldet bleiben* (standardmäßig aus): Benutzer und Passwort mit einem eigenen Keystore-Schlüssel versiegelt, nur für diesen Server, beim Abwählen sofort vergessen; kein Backup (`allowBackup=false`) |
| Klartext-HTTP ins Internet | Nur erlaubt zu einem IPv4-*Literal* des privaten Netzes oder Loopback; ein Host wie `10.attacker.example` oder eine Adresse mit führender Null (`010.0.0.1` wird oktal gelesen) wird abgelehnt; Klartext-http wird für einen Host abgelehnt, der über https erreicht wurde |
| Ein Man-in-the-Middle über https | Vertrauen haben die Stellen des Systems plus die, die du auf dem Telefon installiert hast; der Schlüssel des Installationszertifikats wird beim ersten Mal gemerkt (oder mit dem QR-Text von Studio gekoppelt), ein anderer wird abgelehnt |
| Ein kritischer Befehl mit dem Telefon in fremden Händen | Fingerabdruck, Gesicht oder PIN des Telefons vor Scharf-/Unscharfschalten, Sirene stummschalten, Entriegeln und anderen kritischen Befehlen (standardmäßig nur an, wenn das Telefon Fingerabdruck oder Gesicht hat); der Administrator kann es in Studio verlangen, nie senken, was du gewählt hast; ein Telefon ohne Bildschirmsperre wird nie ausgesperrt |
| Ein Betrachter ändert etwas | Die Rolle *Betrachter* sieht alles, aber jede Schaltfläche, die etwas ändert, ist aus (Rollentests) |
| Eine falsche Entschärfungs-PIN | Die PIN geht nur beim Unscharfschalten an den Server; `wrong_pin`, `pin_locked`, `pin_not_set` und `elevation_required` werden in einfache Worte übersetzt |
| Ein gefälschtes App-Update | Die SHA-256 der Signaturzertifikate (Verlauf) der installierten App und des Kandidaten werden verglichen, bevor Androids Installer aufgerufen wird; teilen sie keines, wird es klar gesagt |
| Ein Alarm, den das Telefon nicht sieht | Ein Vordergrunddienst (`AlarmWatcherService`) fragt alle 10 s ab, wartet 15 s auf das Entsperren, weicht bis 60 s zurück, meldet nach 6 Fehlern *nicht erreichbar* und überlebt einen Neustart; ein Alarm, der älter als 15 Minuten ist, wird nicht angekündigt |
| Ein Kamerapasswort auf dem Telefon | Die Kameras werden über den Strom des Servers gesehen: Die App hat kein Kamerapasswort und keine RTSP-Adresse |

### Berechtigungen

`INTERNET`, `ACCESS_NETWORK_STATE`: der Server. `USE_BIOMETRIC`: die Telefonsperre. `POST_NOTIFICATIONS`: Alarme. `FOREGROUND_SERVICE` (+ `DATA_SYNC`, `SPECIAL_USE`) und `RECEIVE_BOOT_COMPLETED`: die Alarmüberwachung. `REQUEST_INSTALL_PACKAGES`: das Update der App. `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`: die Einrichtung eines Knotens; `BLUETOOTH`, `BLUETOOTH_ADMIN` und `ACCESS_FINE_LOCATION` nur bis Android 11 (`maxSdkVersion=30`), für die Bluetooth-Suche.

Es gibt keine unabhängige Sicherheitsprüfung. Lies [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) und [SECURITY.md](SECURITY.md).

## 🔌 Schnittstellen

| Teil | Was sie festlegt |
|---|---|
| ARMOR-SERVER | 55 verschiedene Pfade von `/api/v1` in `network/ArmorApiClient.kt` (die Schnittstelle ist `network/ArmorApi.kt`); das Radar wird alle 1,5 s gelesen; die Kameras per MJPEG über den Server |
| Zwei Adressen | Die private im Heim-WLAN und die für unterwegs (Mobilfunk oder VPN), je nach Aufenthaltsort des Telefons; antwortet eine nicht, wird die andere versucht, und die Meldung sagt, was fehlschlägt |
| Bluetooth | Findet Knoten, die `ARMOR-xxxxxx` ankündigen, legt den Administrator eines neuen an oder meldet sich mit der Knoten-PIN an, sucht WLANs und setzt Name, WLAN oder feste Adresse und den Broker; das Protokoll ist `model/NodeBle.kt`; ein Knotenfinder listet die bereits im Netz befindlichen auf |
| Sprache | `POST /api/v1/voice/command`: eine geschlossene Befehlsliste in acht Sprachen, mit einer an dich gebundenen Bestätigung (`model/Voice.kt`, `VoiceScreens.kt`) |
| Push | `PushService`: Alarme als reine Datennachrichten, einmal angekündigt, ob per Push oder per Überwachung; die Firebase-Schritte stehen in [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) (acht Sprachen) |

## ⚙️ Konfiguration

| Name | Wert | Was sie festlegt |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | Android 8.0 oder neuer auf dem Telefon |
| `versionName` | = `armor.project.json` | Der Build schlägt fehl, wenn Manifest und Version abweichen |
| JDK / Android SDK | 17+ / 37 | Zum Bauen der App und Ausführen der Tests |

In der App (nicht in Dateien): die Sprache (die acht oder dem Telefon folgen), die zwei Adressen, *Angemeldet bleiben*, die Telefonsperre, der gesprochene Alarm und die Benachrichtigungen, unter *Mehr > Einstellungen*. Grenzen im Code: Die Überwachung fragt alle 10 s ab, weicht bis 60 s zurück und ignoriert Alarme, die älter als 15 Minuten sind.

## 🚀 Schnellstart

**Voraussetzungen:** JDK 17+, das Android SDK (compile SDK 37), ein Telefon mit Android 8.0+ (minSdk 26).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 376 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Die Versionen werden auf GitHub veröffentlicht, von wo die App selbst nach Updates sucht; die APK ist mit dem eigenen Release-Schlüssel des Betreuers signiert, der außerhalb des Repositorys liegt, und die App vergleicht das Signaturzertifikat eines Downloads mit dem eigenen, bevor sie die Datei an das Installationsprogramm von Android übergibt. Siehe die [Client-Grenze](docs/CLIENT_BOUNDARY.md).

## 🧪 Tests

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| Teil | Tests | Was sie abdeckt |
|---|---|---|
| Alarme | 67 | `AlarmPolicy`, `AlarmBanner`, `AlarmPanels`, `AlarmCodes`, `AlarmRoute`, `NoticeFilter`, `PushEvents`, `DevicesAndAlarms` |
| Serververbindung, Auswertung, Testdaten | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| View-Model und Rollen | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| Sperre, Sitzungstresor, Schlüssel | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| Adresssicherheit, Zertifikate | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| Energie und Netzwerk | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| Knoten per Bluetooth, Knotensteuerung | 26 | `NodeBle`, `NodeControl` |
| Updates von GitHub | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| Radar und Kameras | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| Übersetzungen, einfache Worte, Sprache | 14 | `Tr`, `Friendly`, `Voice` |

376 Tests in 47 Dateien, ausgeführt mit `testDebugUnitTest`; ein Fake-Server (`FakeServer.kt`) und von einem echten Server geholte Daten (`tools/capture_fixtures.mjs`) speisen sie. Es wird geprüft, dass jeder Satz in den acht Sprachtabellen steht.

## 🏗️ Build und Veröffentlichung

Releases werden auf GitHub veröffentlicht, von wo die App selbst nach Updates sucht; die App sucht nach dem Asset `ARMOR-ANDROID-CONTROL-release.apk`. Die APK ist mit dem eigenen Release-Schlüssel des Maintainers signiert, der außerhalb des Repositorys liegt, und die App vergleicht das Signaturzertifikat eines Downloads mit ihrem eigenen, bevor sie die Datei an Androids Installer übergibt. Der `versionCode` steigt vor jeder APK, sonst gilt sie für Android nicht als Update.

## 🩺 Fehlerbehebung

| Sie sehen | Weil | Tun Sie |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | Der Server lehnt die Entschärfungs-PIN ab oder verlangt einen frischen Identitätsnachweis | Gib die PIN erneut ein, warte das Ende der Sperre ab oder bestätige deine Identität |
| Die Anmeldung sagt, dass sie nicht fortfahren kann | Die Adresse liegt außerhalb des Hauses und hat kein https (Klartext-http wird abgelehnt) | Folge der Anleitung: ein VPN oder eine öffentliche Adresse mit https |
| Die Verbindung wird verweigert, nachdem sie funktionierte | Der Schlüssel des Installationszertifikats hat sich geändert | Koppele erneut mit dem QR-Text von Studio, wenn du es absichtlich geändert hast |
| Alarme kommen im Hintergrund spät oder gar nicht an | Die Akkuregeln des Herstellers stoppen die Überwachung; Push braucht ein eigenes Firebase-Projekt | Nimm die App vom Energiesparen aus; folge [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| Das Update wird abgelehnt | Das Signaturzertifikat des Downloads teilt keines mit der installierten App | Installiere sie nicht; nimm die APK aus den Releases dieses Repositorys |

## 📂 Struktur des Repositorys

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository-Struktur von ARMOR-ANDROID-CONTROL" width="100%">
</p>

<details>
<summary>Textversion des Baums</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   die App: Bildschirme, Regeln, Netz und Updates
├── app/src/main/assets/                         acht Sprachtabellen, Alarmcodes, Wetterkarte
├── app/src/test/                                376 Tests und ein Fake-Server
├── docs/                                        die Anleitungen dieses Repositorys
├── tools/                                       die gemeinsamen CI-Prüfungen und Projektwerkzeuge
├── .github/                                     CI-Workflow sowie Vorlagen für Issues und Pull Requests
├── images/                                      Banner, Symbol und die zwei Bilder dieses READMEs
├── build.sh build.bat                           Build über den gemeinsamen Projekt-Workflow
├── test.sh test.bat                             Tests über den gemeinsamen Workflow ausführen
├── run.sh run.bat                               Start über den gemeinsamen Projekt-Workflow
├── armor.project.json                           Manifest: Version, Rolle, Reifegrad, Build-Befehl
├── CHANGELOG.md                                 was sich in jeder Version geändert hat
├── SECURITY.md SUPPORT.md LICENSE               Sicherheitsrichtlinie, Support und Lizenz
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           Mitwirkung und Verhaltenskodex
├── build.gradle.kts settings.gradle.kts         der Gradle-Build des Projekts
├── gradle.properties                            Gradle-Einstellungen
├── gradlew gradlew.bat gradle/                  der Gradle-Wrapper
├── build-test.sh build-test.bat                 Tests über den gemeinsamen Workflow ausführen
└── .gitattributes .gitignore                    Git-Einstellungen
```

</details>

## 🗺️ Roadmap und bekannte Grenzen

**Bekannte Grenzen heute**

* Die Bildschirme für Solar, Elektro und Alarmzentrale kennen keine echte Ausrüstung (es gibt nur einen Radarknoten).
* Die Bluetooth-Einrichtung eines Knotens wurde nicht an einem echten Knoten bestätigt, und Push über Firebase wurde auf einem Telefon nicht erprobt.
* Die Alarmüberwachung im Hintergrund unter den Akkuregeln jedes Herstellers ist nicht erwiesen.
* Keine unabhängige Sicherheitsprüfung.

**Als Nächstes**

* Die Bildschirme für Solar, Elektro und Alarmzentrale mit echten Knoten und die Bluetooth-Einrichtung mit einem echten Knoten erproben.
* Push über Firebase mit dem eigenen Projekt einer Installation und die Alarmüberwachung bei mehreren Herstellern erproben.

## 🌍 Sprachen

README und Texte gibt es auf Englisch, Spanisch, Französisch, Italienisch, Deutsch, Chinesisch, Japanisch und europäischem Portugiesisch.

## 🔗 Verwandte Projekte

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) ist ein Perimeter-Sicherheitssystem aus unabhängigen Repositorys. Jedes hat eine eigene Version, eigene Tests und ein eigenes README; hier ist die Familie:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Nachrichtenverträge, Validierer, Konformitätsvektoren und generierte Typen
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Feldknoten-Firmware für ESP32-S3 mit drei Radaren und eigenem Web-Panel
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protokolle für Solar-Wechselrichter und -Batterien und die Nachrichten eines Gateway-Knotens
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Elektroknoten: Zähler, die Nachricht der Netzmesswerte und die Regeln fürs Schalten
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Alarmknoten und Alarmzentrale: Zonen, Scharfschalten, Verzögerungen, Sirene und PIN, mit dem Server oder ohne ihn
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Touch-Panel: der Systemzustand auf einem Wandbildschirm, Scharf- und Quittieren sowie das Zuhause des Sprachassistenten
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - Das lokale Netzwerk: seine Geräte, das Internet und was sich ändert
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Zentraler Koordinator: Telemetrie, Alarme, Geräte, Solarmesswerte und Kameras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web-Konsole: Kameras, Radar, Alarme, Solarenergie und 2D/3D-Standortdesigner
* **ARMOR-ANDROID-CONTROL** (dieses Repository) - Android-Bedienclient mit Live-Radar in 2D/3D
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone-Bedienclient
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Visuelle Inferenzrichtlinie, die ihre Entscheidungen erklärt und nie handelt
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Kamerazonen, Masken und Erkennungsregeln für den Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Offline-Sprachabsichten mit einer nicht fälschbaren Bestätigung
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Gehäuse, Elektronik und die Abnahmematrix am Prüfstand
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Bereitstellung, CM5-Prüfstand, Backup und TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Offline-Telemetriesimulator mit wiederholbaren Fehlern
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Erkennt, installiert und aktualisiert die eigenen Repositorys des Ökosystems
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architektur, Sicherheitsgrundlage und die Fähigkeitsmatrix

## 📚 Dokumentation

* [Client-Grenze](docs/CLIENT_BOUNDARY.md): was der Client tut und nie tut
* [Push-Benachrichtigungen](docs/PUSH_NOTIFICATIONS.md): die Firebase-Schritte in acht Sprachen
* [Prüfstandtests](docs/BENCH_TESTS.md): Prüfungen mit einem echten Telefon
* [Fähigkeitsmatrix: was belegt ist und was nicht](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Projektkatalog: Versionen und wie die Repositorys voneinander abhängen](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Änderungsverlauf dieses Repositorys](CHANGELOG.md)

## 🤝 Mitwirken und Community

* Führe vor einem Pull Request `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py` aus: Es muss bestehen. Siehe [CONTRIBUTING.md](CONTRIBUTING.md) und den [Verhaltenskodex](CODE_OF_CONDUCT.md).
* Das README gibt es in acht Sprachen, und es wird aus den Quellen in ARMOR-DOCS erzeugt: Eine Textänderung erfolgt dort.
* Eine Änderung an einer Nachricht zwischen Komponenten beginnt in ARMOR-COMMON.
* Beschreibe nichts als auf einer Platine funktionierend, was nur gebaut wurde.
* Melde eine Schwachstelle privat: [SECURITY.md](SECURITY.md). Fragen, Ideen und Meldungen: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LIZENZ

GPL-3.0-or-later - siehe [LICENSE](LICENSE).
