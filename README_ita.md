<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  🇮🇹 <b>Italiano</b> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a>
</p>

### Client mobile dell'operatore per ARMOR-SERVER

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**Controllo di onestà - cosa funziona oggi:** Le regole di sicurezza degli indirizzi e di decisione degli allarmi hanno test unitari (55) e l'app si compila. **Non è stata eseguita su un telefono contro il server**, quindi il flusso di permesso delle notifiche e il servizio in background non sono verificati. Armo, allarmi e dispositivi seguono le rotte del server ma non sono stati provati su un telefono reale.

---

## 🎯 Panoramica

* **Accesso con le credenziali del server:** IP, porta, utente e password; la password crea una sessione HttpOnly e non viene mai salvata sul telefono.
* **Monitor delle telecamere:** da 1 a 16 riquadri, una vista ingrandita, MJPEG dal vivo che mantiene le proporzioni, un joystick PTZ limitato, istantanee e registrazioni.
* **Inserire e disinserire,** dopo una conferma; **allarmi** da riconoscere, con un badge per quelli in attesa; **dispositivi** (fumo, gas, allagamento, porta, finestra, movimento, clima, prese, luci, sirene, serrature) con il loro stato e On / Off / Inverti.
* **Radar dal vivo, in 2D e 3D:** la scheda Radar disegna il sito come progettato in Studio (terreno, edifici, alberi, pali, campi dei radar e delle telecamere) e le persone che i radar vedono muoversi, aggiornato ogni secondo e mezzo; trascina, pizzica e ruota la vista 3D. Il posizionamento segue le regole di Studio, ma non ha mai mostrato un radar reale.
* **Solare:** una voce in *Más* (e un riquadro nello Stato) mostra i totali (sole, consumo, batterie con carica e flusso, se c'è la rete) e ogni inverter e batteria che il server segnala: i loro valori, le celle di una batteria a richiesta con la più alta e la più bassa evidenziate, gli apparecchi che aspettano ancora dati e un contrassegno su una lettura di esempio o un apparecchio muto. Aggiornato ogni cinque secondi quando è aperta; provata sulle forme di risposta del server e in un emulatore contro un server locale con letture di esempio, mai con apparecchi reali. Gli allarmi solari (guasto di un inverter, batteria scarica o in protezione, apparecchio muto) svegliano il telefono come quelli di un dispositivo.
* **Configurare un nodo di campo via Bluetooth** (dalla schermata di accesso o da *Más > Configurar un nodo*): trova i nodi che annunciano `ARMOR-xxxxxx`, crea l'amministratore di uno nuovo o accede, cerca reti Wi-Fi e imposta il nome del nodo, il Wi-Fi di un router o un indirizzo fisso e il broker, per i nodi senza cavo Ethernet. Compilato e testato in unità, mai eseguito contro un nodo né un telefono.
* **Libreria delle prove,** stato del perimetro e dei nodi, e **cronologia** di ogni avviso, nodo, telecamera, dispositivo, allarme e cambio di modo.
* **Notifiche di allarme** per un nodo che raggiunge HIGH, una telecamera che non risponde più, un allarme di dispositivo (fumo, gas, allagamento, panico sempre; una porta, finestra o un sensore di movimento a sistema inserito) e, a sistema inserito, un nodo che va offline; un controllo opzionale in background usa la sessione corrente e lo dice quando finisce.
* **Attenzione con la password:** l'HTTP semplice è ammesso solo verso un *letterale* IPv4 di rete privata o loopback. Un nome host che inizia soltanto come un indirizzo privato (`10.attacker.example`) o un indirizzo con uno zero iniziale (alcuni resolver leggono `010.0.0.1` come il pubblico `8.0.0.1`) viene rifiutato.
* **Rete elettrica:** una voce in *Altro* (e una tessera nella schermata di stato) mostra la potenza dell'ingresso di rete (in prelievo o in immissione), ogni canale che i nodi elettrici misurano (tensione, corrente, potenza, energia, frequenza, fattore di potenza, lo stato di un interruttore) e gli allarmi dei contatori; gli allarmi dei nodi elettrici (quello di un contatore, la tensione di rete fuori intervallo, la rete mancante, un nodo che non risponde più) svegliano l'operatore come quelli solari. La scheda di un inverter mostra anche il secondo ingresso fotovoltaico e le unità di un sistema in parallelo.
* **Un aspetto fatto di icone:** superfici quasi nere, accento ciano, ambra per l'attenzione, icone grandi con poche parole, barra inferiore, una pagina Informazioni e un pulsante di uscita.

## 📂 Struttura del repository

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt (splash, sign-in, account, About), HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt, SolarScreens.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt   configure a radar, solar or electrical node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmWatcherService.kt
│   ├── ArmorTheme.kt, ServerEndpoint.kt, MjpegFeed.kt
│   ├── network/   model/
├── docs/CLIENT_BOUNDARY.md
└── app/src/test/   endpoint-safety, plain-words, node-protocol and solar-model tests
```

## 🛠️ Ambiente di sviluppo

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

L'APK di debug non è firmato per la distribuzione. Vedi il [confine del client](docs/CLIENT_BOUNDARY.md).

## 🔗 Progetti correlati

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) è un sistema di sicurezza perimetrale fatto di repository indipendenti. Ognuno ha la propria versione, i propri test e il proprio README; ecco la famiglia:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratti dei messaggi, validatori, vettori di conformità e tipi generati
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - Firmware del nodo di campo per ESP32-S3 con tre radar e un proprio pannello web
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - Protocolli di inverter e batterie solari e messaggi di un nodo gateway
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - Nodo elettrico: contatori, il messaggio delle letture della rete e le regole di manovra
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - La rete locale: i suoi dispositivi, internet e ciò che cambia
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinatore centrale: telemetria, allarmi, dispositivi, letture solari e telecamere
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Console web: telecamere, radar, allarmi, energia solare e progettista del sito 2D/3D
* **ARMOR-ANDROID-CONTROL** (questo repository) - Client Android dell'operatore con radar 2D/3D in tempo reale
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Politica di inferenza visiva che spiega le sue decisioni e non agisce mai
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenti vocali offline con una conferma impossibile da falsificare
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Contenitori, elettronica e matrice di accettazione da banco
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Distribuzione, banco di prova CM5, backup e TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulatore di telemetria offline con guasti ripetibili
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Rileva, installa e aggiorna i repository stessi dell'ecosistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architettura, base di sicurezza e matrice delle capacità

## 📚 Documentazione e comunità

Dove leggere di più:

* [Matrice delle capacità: cosa è provato e cosa no](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catalogo dei progetti: versioni e dipendenze tra i repository](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Cronologia delle modifiche di questo repository](CHANGELOG.md)
* [Licenza (GPL-3.0-or-later)](LICENSE)
* Domande, idee e segnalazioni: electrohobby3d@gmail.com

## 👤 AUTORE

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENZA

GPL-3.0-or-later - vedi [LICENSE](LICENSE).
