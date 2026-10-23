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
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### Client mobile da operatore per ARMOR-SERVER: allarmi, telecamere, radar, energia, la rete e la configurazione dei nodi, in otto lingue

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-319%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.4-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Controllo di onestà - cosa funziona oggi:** Le regole di sicurezza dell'indirizzo, di decisione degli allarmi, del guardiano degli allarmi, di traduzione e del ruolo di sola lettura sono coperte da 257 test unitari e l'app si compila (`gradlew testDebugUnitTest assembleDebug`). Funziona su un vero telefono contro il banco di prova CM5: l'accesso, il monitor delle telecamere, l'inserimento e gli allarmi, il radar, la configurazione di un nodo via Bluetooth e gli aggiornamenti da GitHub sono in uso. Ciò che **non** è ancora provato: le schermate solare, elettrica e delle centrali d'allarme con apparecchi reali (esiste solo un nodo radar), il push tramite Firebase (è nell'app e richiede un progetto Firebase proprio; non è ancora stato provato su un telefono) e la sorveglianza degli allarmi in background sotto le regole di batteria di ogni produttore.

---

## 📋 In breve

| | |
|---|---|
| Versione | `0.7.4` |
| Maturità | Consolidato |
| Test | 319 (`gradlew testDebugUnitTest`) |
| Tecnologia | Kotlin / Jetpack Compose |
| Su hardware reale | Usata su un vero telefono contro il banco di prova CM5; le schermate solare, elettrica e delle centrali d'allarme attendono nodi reali |

## 🎯 Panoramica

* **Accesso con il login del server stesso:** IP, porta, utente e password; la password crea una sessione HttpOnly; viene memorizzata sul telefono solo se spunti **Ricordami** (disattivato per impostazione predefinita: utente e password sigillati con una chiave propria del Keystore, solo per quel server, dimenticati appena togli la spunta). Un ruolo **viewer** guarda soltanto: ogni pulsante che cambia qualcosa è spento.
* **Blocco con il telefono:** l'app chiede impronta, volto o PIN del telefono stesso prima di inserire, disinserire, silenziare una sirena, sbloccare e per gli altri ordini critici (attivo per impostazione predefinita solo se il telefono ha impronta o volto) e, se lo attivi, per aprire l'app. Il tuo amministratore può richiederlo da Studio ma mai abbassare ciò che hai scelto; un telefono senza blocco schermo non viene mai lasciato fuori e la schermata bloccata offre sempre di accedere di nuovo con la password. In via facoltativa (disattivata per impostazione predefinita, con un avviso ben visibile) la chiave che sigilla la sessione salvata può essere legata allo sblocco del telefono; in tal caso la sorveglianza degli allarmi in background, dopo un riavvio, attende che tu sblocchi il telefono.
* **Monitor delle telecamere:** da 1 a 16 riquadri, una vista ingrandita, MJPEG dal vivo che mantiene il rapporto dell'immagine, una pulsantiera PTZ limitata, istantanee e registrazioni, con visualizzatore e conferma di cancellazione delle registrazioni.
* **Inserire e disinserire,** dopo un **controllo prima di inserire** (cosa è aperto, muto o fermo) e con il PIN per disinserire; **allarmi** da riconoscere, con un badge per quelli in attesa; **dispositivi** (fumo, gas, allagamento, porta, finestra, movimento, clima, prese, luci, sirene, serrature, tapparelle, motori di cancello e garage, scaldabagni) con il loro stato e Accendi / Spegni / Alterna.
* **Allarmi che ti raggiungono:** un suono per ogni gravità, un **allarme parlato** facoltativo, notifiche per un nodo che arriva ad ALTO, una telecamera muta, un allarme di dispositivo e (a impianto inserito) un nodo offline, e **una centrale che suona sveglia il telefono**; la sorveglianza in background sopravvive a un riavvio e rallenta quando il server è fermo.
* **Radar dal vivo, in 2D e 3D:** la scheda Radar disegna il sito come progettato in Studio e le persone che i radar vedono muoversi su di esso, aggiornato ogni secondo e mezzo; trascina, pizzica e ruota la vista 3D.
* **Energia solare ed elettrica:** i totali (sole, consumo, batterie con la loro carica, se c'è la rete), ogni inverter e batteria con le celle a richiesta, e i canali e gli allarmi dei nodi elettrici.
* **Centrali d'allarme:** lo stato di ogni nodo ARMOR-NODE-ALARM, le sue zone ed eventi, e comandi di inserimento / disinserimento quando il server li consente.
* **Rete:** i dispositivi della rete della casa, lo stato di internet e l'**indirizzo pubblico** con il fornitore e il luogo, come li riferisce il nodo di rete.
* **Configurare un nodo di campo via Bluetooth** (dalla schermata di accesso o *Altro > Configura un nodo*): trova i nodi che annunciano `ARMOR-xxxxxx`, crea l'amministratore di uno nuovo o entra (con il PIN del nodo), cerca reti Wi-Fi e imposta il nome del nodo, il Wi-Fi di un router o un indirizzo fisso e il broker; un **cercatore di nodi** elenca quelli già in rete.
* **L'assistente:** comandi scritti e parlati (elenco chiuso, in otto lingue, con una conferma legata a te) per chiedere lo stato della casa o inserire, disinserire, tacitare e accendere o spegnere le luci.
* **Libreria delle prove,** stato del perimetro e dei nodi, uno **storico** di ogni allarme, nodo, telecamera, dispositivo, allarme e cambio di modalità, la **mappa meteo**, e i **servizi** e gli aggiornamenti dell'impianto.
* **Otto lingue** (inglese, spagnolo, tedesco, francese, italiano, giapponese, cinese e portoghese), scelte in *Altro > Impostazioni* o seguendo il telefono; si controlla che ogni frase sia nelle otto tabelle.
* **Attenzione alla password:** l'HTTP in chiaro è ammesso solo verso un *letterale* IPv4 di LAN privata o loopback; un nome host che inizia soltanto come un indirizzo privato (`10.attacker.example`) o un indirizzo con zero iniziale è rifiutato. L'aspetto è fatto di icone: superfici quasi nere, accento ciano, ambra per l'attenzione.
* **La sicurezza dal telefono:** il secondo fattore e i suoi codici di recupero, le sessioni aperte, un controllo di come il server vede il telefono, la nuova prova di identità per le azioni critiche da fuori e, per un amministratore, la VPN (dispositivi con la loro configurazione) e il nome DNS dinamico.**Tutto il sistema nella schermata di stato e l'ingresso da fuori:** la schermata di stato riassume le centrali d'allarme, i servizi, le telecamere che registrano e la sicurezza e l'accesso remoto, ogni scheda apre la sua schermata; e quando l'indirizzo scritto è fuori casa e senza https, l'accesso dice perché non può proseguire e una guida spiega le due vie sicure (VPN, o indirizzo pubblico con https).
* **Salute dei nodi e modalità di inserimento:** una schermata Salute dei nodi in Altro (tempo acceso, motivo del riavvio, memoria, collegamento e segnale, connessioni al broker, riavvii, ultimo errore e avvisi del server per ogni nodo) e, nella schermata delle Centrali d'allarme, un selettore di modalità (Fuori casa, In casa, Notte e le tue) che inserisce una centrale nella modalità scelta quando il suo firmware le conosce. Otto lingue.
* **Connessioni nella cronologia:** la schermata Cronologia ha un secondo elenco, di sola lettura, con i dispositivi e le app che si collegano al server (Studio, questa app, i pannelli dei nodi, il servizio vocale), quando ciascuno è stato visto l'ultima volta, le sue sessioni aperte, gli accessi e i rifiuti; l'app ora dice al server cos'è per comparire come se stessa. Gli indirizzi li vedono solo gli amministratori.
* **Manutenzione delle batterie (amministratori):** le operazioni semplici su una batteria Pylontech - orologio, contatori e registri, riavvio, capacità, test MOSFET, guasto al fusibile, uscite - nei due passi di Studio (il server fa leggere la batteria al nodo, la schermata mostra esattamente cosa verrà fatto, poi "Esegui ora" con il numero di serie digitato di nuovo), con il risultato e cosa è cambiato; limiti, firmware e console esperto restano in Studio.
* **Controllo dei nodi (amministratori):** Altro > Controllo dei nodi offre le stesse operazioni di Studio - riavvio, pausa, ripresa, standby (lavoro spento, resta connesso), identificazione, diagnostica e manutenzione attivata o disattivata - a un nodo o a tutti, chiedendo di digitare di nuovo il nome del nodo (o la parola RESTART, PAUSE o STANDBY per tutti) e la nuova prova di identità, mostrando a che punto è ogni nodo e avvisando che una centrale d'allarme o un radar in pausa o in standby lascia la casa senza protezione. Non esistono ripristino di fabbrica, cancellazione né sonno profondo.
* **Una finestra di allarme nell'app aperta:** finché un allarme aspetta una persona, un banner sopra tutte le schermate tranne quella degli allarmi pulsa (rosso e veloce per un critico, ambra per gli altri, fermo se il telefono ha le animazioni spente), dice qual è l'allarme peggiore e quanti aspettano, e lascia usare l'app sotto; può aprire gli allarmi, confermare dal banner o essere chiuso finché non arriva un altro allarme.
* **Da fuori casa:** due indirizzi (quello privato sul Wi-Fi di casa, quello per fuori con i dati mobili o la VPN) scelti secondo dove si trova il telefono, provando l'altro se uno non risponde e con un messaggio che dice cosa non va; la chiave del certificato dell'installazione viene ricordata la prima volta (o associata con il testo del QR di Studio) e una diversa viene rifiutata, e l'http semplice è rifiutato per un server già raggiunto in https; il pannello di un nodo si può aprire tramite il server; gli allarmi possono arrivare come push di soli dati, annunciati una volta sia che arrivino dal push sia dalla sorveglianza; l'app aperta e la sorveglianza condividono una lettura del server; i flussi delle telecamere si fermano fuori schermo e tornano con attese crescenti.

## 🧩 Ruolo in A.R.M.O.R.

ARMOR-ANDROID-CONTROL è la finestra del telefono sullo stesso [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) usato da [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO): legge e ordina attraverso di esso e nient'altro. È dove gli allarmi ti seguono (una centrale che suona sveglia il telefono), dove interroghi l'assistente a voce, e lo strumento per configurare un nuovo nodo via Bluetooth sul campo, accanto ad esso.

## 📂 Struttura del repository

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, NodeControlScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmBanner.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, DeviceLock.kt, PhoneLock.kt, LockScreens.kt, PinDialog.kt, MjpegFeed.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md   (the Firebase steps, in eight languages)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ Ambiente di sviluppo

**Requisiti:** JDK 17+, l'SDK Android (compile SDK 37), un telefono Android 8.0+ (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 319 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Le versioni sono pubblicate su GitHub, da dove l'app stessa cerca gli aggiornamenti; l'APK è firmato con la chiave di rilascio propria del responsabile, conservata fuori dal repository, e l'app confronta il certificato di firma di un download con il proprio prima di passare il file al programma di installazione di Android. Vedi il [confine del client](docs/CLIENT_BOUNDARY.md).

## 🔗 Progetti correlati

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) è un sistema di sicurezza perimetrale fatto di repository indipendenti. Ognuno ha la propria versione, i propri test e il proprio README; ecco la famiglia:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratti dei messaggi, validatori, vettori di conformità e tipi generati
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Firmware del nodo di campo per ESP32-S3 con tre radar e un proprio pannello web
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protocolli di inverter e batterie solari e messaggi di un nodo gateway
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Nodo elettrico: contatori, il messaggio delle letture della rete e le regole di manovra
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Nodo e centrale d'allarme: zone, inserimento, ritardi, sirena e PIN, con il server o senza
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Pannello touch: lo stato del sistema su uno schermo a parete, attivare e riconoscere gli allarmi, e la casa dell'assistente vocale
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - La rete locale: i suoi dispositivi, internet e ciò che cambia
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinatore centrale: telemetria, allarmi, dispositivi, letture solari e telecamere
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Console web: telecamere, radar, allarmi, energia solare e progettista del sito 2D/3D
* **ARMOR-ANDROID-CONTROL** (questo repository) - Client Android dell'operatore con radar 2D/3D in tempo reale
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Politica di inferenza visiva che spiega le sue decisioni e non agisce mai
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zone delle telecamere, maschere e regole di rilevamento per la Jetson Orin NX
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
