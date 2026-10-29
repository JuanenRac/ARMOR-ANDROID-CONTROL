<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="Banner di ARMOR-ANDROID-CONTROL" width="100%">
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
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 o successiva">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Linguaggio">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Lingue">
  <img src="https://img.shields.io/badge/Tests-342%20passing-2ea44f.svg" alt="Test">
  <img src="https://img.shields.io/badge/Version-0.7.7-0f6cbd.svg" alt="Versione">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturità">
</p>

---

**Controllo di onestà - cosa funziona oggi:** Le regole di sicurezza dell'indirizzo, di decisione degli allarmi, del guardiano degli allarmi, di traduzione e del ruolo di sola lettura sono coperte da 342 test unitari e l'app si compila (`gradlew testDebugUnitTest assembleDebug`). Funziona su un vero telefono contro il banco di prova CM5: l'accesso, il monitor delle telecamere, l'inserimento e gli allarmi, il radar, la configurazione di un nodo via Bluetooth e gli aggiornamenti da GitHub sono in uso. Ciò che **non** è ancora provato: le schermate solare, elettrica e delle centrali d'allarme con apparecchi reali (esiste solo un nodo radar), il push tramite Firebase (è nell'app e richiede un progetto Firebase proprio; non è ancora stato provato su un telefono) e la sorveglianza degli allarmi in background sotto le regole di batteria di ogni produttore.

---

## 📋 In breve

| | |
|---|---|
| Versione | `0.7.7` |
| Maturità | Consolidato |
| Ruolo | Client operatore Android per ARMOR-SERVER (famiglia: Clients, padre: ARMOR-SERVER) |
| Tecnologia | Kotlin / Jetpack Compose, Android 8.0+ |
| Test | 342 (`gradlew testDebugUnitTest`) |
| Lingue | 8 (1345 frasi per lingua, ogni frase verificata in tutte le tabelle) |
| Gira su | Un telefono con Android 8.0+ (minSdk 26) |
| Su hardware reale | Usata su un vero telefono contro il banco di prova CM5; le schermate solare, elettrica e delle centrali d'allarme attendono nodi reali |
| Licenza | GPL-3.0-or-later |

**Vai a:** [Panoramica](#-panoramica) · [Architettura](#-architettura) · [Sicurezza](#-sicurezza-e-protezione) · [Interfacce](#-interfacce) · [Configurazione](#-configurazione) · [Avvio rapido](#-avvio-rapido) · [Test](#-test) · [Problemi](#-risoluzione-dei-problemi) · [Struttura](#-struttura-del-repository) · [Correlati](#-progetti-correlati)

## 🚦 Stato del progetto

Ogni riga è una parte di questo repository e il semaforo dice a che punto è oggi. È scritta in base a ciò che il repository può mostrare (test, compilazioni, esecuzioni), non a ciò che è previsto.

🟢 Fatto e verificato · 🟡 Funziona, con limiti noti · 🟠 Fatto a metà o non ancora verificato · 🔴 Non fatto o non funzionante

| Area | Stato | Cosa significa qui |
|---|---|---|
| Software | 🟢 | L'app compila (`gradlew testDebugUnitTest assembleDebug`) e gira su un telefono reale contro il banco di prova CM5. |
| Test | 🟢 | Superano 342 test unitari (`gradlew testDebugUnitTest`): sicurezza degli indirizzi, decisione di allarme, sorvegliante degli allarmi, traduzioni e ruolo di sola lettura. |
| Interfaccia | 🟡 | L'accesso, il monitor delle telecamere, l'inserimento e gli allarmi e il radar sono in uso su un telefono reale; la configurazione di un nodo via Bluetooth non è stata confermata con un nodo reale e le schermate solare, elettrica e della centralina d'allarme non hanno incontrato apparecchiature reali. |
| Hardware | 🟡 | Usata su un telefono reale contro il banco di prova CM5; la sorveglianza degli allarmi in background con le regole di batteria di ogni produttore non è provata. |
| Sicurezza | 🟡 | Le regole di sicurezza degli indirizzi e del ruolo di sola lettura sono coperte da test unitari; non c'è stata una revisione indipendente. |
| Aggiornamenti | 🟡 | Gli aggiornamenti da GitHub sono in uso sul telefono. |
| Integrazione | 🟠 | Il push tramite Firebase è nell'app ma richiede un progetto Firebase proprio e non è stato provato su un telefono. |
| Documentazione | 🟢 | Otto lingue e il registro delle modifiche sono aggiornati. |

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
* **La sicurezza dal telefono:** il secondo fattore e i suoi codici di recupero, le sessioni aperte, un controllo di come il server vede il telefono, la nuova prova di identità per le azioni critiche da fuori e, per un amministratore, la VPN (dispositivi con la loro configurazione) e il nome DNS dinamico.
* **Tutto il sistema nella schermata di stato e l'ingresso da fuori:** la schermata di stato riassume le centrali d'allarme, i servizi, le telecamere che registrano e la sicurezza e l'accesso remoto, ogni scheda apre la sua schermata; e quando l'indirizzo scritto è fuori casa e senza https, l'accesso dice perché non può proseguire e una guida spiega le due vie sicure (VPN, o indirizzo pubblico con https).
* **Salute dei nodi e modalità di inserimento:** una schermata Salute dei nodi in Altro (tempo acceso, motivo del riavvio, memoria, collegamento e segnale, connessioni al broker, riavvii, ultimo errore e avvisi del server per ogni nodo) e, nella schermata delle Centrali d'allarme, un selettore di modalità (Fuori casa, In casa, Notte e le tue) che inserisce una centrale nella modalità scelta quando il suo firmware le conosce. Otto lingue.
* **Connessioni nella cronologia:** la schermata Cronologia ha un secondo elenco, di sola lettura, con i dispositivi e le app che si collegano al server (Studio, questa app, i pannelli dei nodi, il servizio vocale), quando ciascuno è stato visto l'ultima volta, le sue sessioni aperte, gli accessi e i rifiuti; l'app ora dice al server cos'è per comparire come se stessa. Gli indirizzi li vedono solo gli amministratori.
* **Manutenzione delle batterie (amministratori):** le operazioni semplici su una batteria Pylontech - orologio, contatori e registri, riavvio, capacità, test MOSFET, guasto al fusibile, uscite - nei due passi di Studio (il server fa leggere la batteria al nodo, la schermata mostra esattamente cosa verrà fatto, poi "Esegui ora" con il numero di serie digitato di nuovo), con il risultato e cosa è cambiato; limiti, firmware e console esperto restano in Studio.
* **Controllo dei nodi (amministratori):** Altro > Controllo dei nodi offre le stesse operazioni di Studio - riavvio, pausa, ripresa, standby (lavoro spento, resta connesso), identificazione, diagnostica e manutenzione attivata o disattivata - a un nodo o a tutti, chiedendo di digitare di nuovo il nome del nodo (o la parola RESTART, PAUSE o STANDBY per tutti) e la nuova prova di identità, mostrando a che punto è ogni nodo e avvisando che una centrale d'allarme o un radar in pausa o in standby lascia la casa senza protezione. Non esistono ripristino di fabbrica, cancellazione né sonno profondo.
* **Una finestra di allarme nell'app aperta:** finché un allarme aspetta una persona, un banner sopra tutte le schermate tranne quella degli allarmi pulsa (rosso e veloce per un critico, ambra per gli altri, fermo se il telefono ha le animazioni spente), dice qual è l'allarme peggiore e quanti aspettano, e lascia usare l'app sotto; può aprire gli allarmi, confermare dal banner o essere chiuso finché non arriva un altro allarme.
* **Da fuori casa:** due indirizzi (quello privato sul Wi-Fi di casa, quello per fuori con i dati mobili o la VPN) scelti secondo dove si trova il telefono, provando l'altro se uno non risponde e con un messaggio che dice cosa non va; la chiave del certificato dell'installazione viene ricordata la prima volta (o associata con il testo del QR di Studio) e una diversa viene rifiutata, e l'http semplice è rifiutato per un server già raggiunto in https; il pannello di un nodo si può aprire tramite il server; gli allarmi possono arrivare come push di soli dati, annunciati una volta sia che arrivino dal push sia dalla sorveglianza; l'app aperta e la sorveglianza condividono una lettura del server; i flussi delle telecamere si fermano fuori schermo e tornano con attese crescenti.

## 🧩 Ruolo in A.R.M.O.R.

ARMOR-ANDROID-CONTROL è la finestra del telefono sullo stesso [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) usato da [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO): legge e ordina attraverso di esso e nient'altro. È dove gli allarmi ti seguono (una centrale che suona sveglia il telefono), dove interroghi l'assistente a voce, e lo strumento per configurare un nuovo nodo via Bluetooth sul campo, accanto ad esso.

| Nome | Come | A cosa serve |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | Tutto ciò che l'app legge e ordina |
| Nodi di campo | Bluetooth (`ARMOR-xxxxxx`) | Configura un nuovo nodo stando accanto a esso |
| GitHub | HTTPS | Controlla e scarica l'aggiornamento firmato dell'app stessa |
| Open-Meteo | HTTPS | Le previsioni, la qualità dell'aria e la ricerca dei luoghi pubbliche della mappa meteo |

## 🏛️ Architettura

<p align="center">
  <img src="images/architecture.svg" alt="Architettura di ARMOR-ANDROID-CONTROL: chi lo chiama, cosa contiene e da cosa dipende" width="100%">
</p>

## 🔒 Sicurezza e protezione

| Minaccia | Difesa |
|---|---|
| Una password salvata sul telefono | Solo con *Ricordami* (spento di default): utente e password sigillati con una chiave Keystore propria, solo per quel server, dimenticati appena lo togli; nessun backup (`allowBackup=false`) |
| HTTP in chiaro verso Internet | Consentito solo verso un *letterale* IPv4 di rete privata o loopback; un host come `10.attacker.example` o un indirizzo con zero iniziale (`010.0.0.1` si legge ottale) è rifiutato; l'http in chiaro è rifiutato per un host raggiunto via https |
| Un uomo nel mezzo su https | La fiducia sono le autorità del sistema più quelle installate sul telefono; la chiave del certificato dell'installazione è ricordata la prima volta (o associata dal testo del QR di Studio) e una diversa è rifiutata |
| Un ordine critico con il telefono in altre mani | L'impronta, il volto o il PIN del telefono prima di inserire, disinserire, silenziare una sirena, sbloccare e gli altri ordini critici (attivo di default solo se il telefono ha impronta o volto); l'amministratore può imporlo da Studio, mai abbassare la tua scelta; un telefono senza blocco schermo non resta mai escluso |
| Un visualizzatore cambia qualcosa | Il ruolo *visualizzatore* vede tutto ma ogni pulsante che cambia qualcosa è spento (test di ruolo) |
| Un PIN di disinserimento errato | Il PIN va al server solo per disinserire; `wrong_pin`, `pin_locked`, `pin_not_set` ed `elevation_required` sono tradotti in parole semplici |
| Un falso aggiornamento dell'app | Gli SHA-256 dei certificati di firma (storico) dell'app installata e del candidato sono confrontati prima di chiamare l'installer di Android; se non ne condividono nessuno, lo dice chiaramente |
| Un allarme che il telefono non vede | Un servizio in primo piano (`AlarmWatcherService`) interroga ogni 10 s, attende 15 s lo sblocco, rallenta fino a 60 s, dice *irraggiungibile* dopo 6 errori, sopravvive al riavvio; un allarme più vecchio di 15 minuti non viene annunciato |
| Una password di telecamera sul telefono | Le telecamere si vedono dal flusso del server: l'app non ha password di telecamera né indirizzo RTSP |

### Permessi

`INTERNET`, `ACCESS_NETWORK_STATE`: il server. `USE_BIOMETRIC`: il blocco del telefono. `POST_NOTIFICATIONS`: gli allarmi. `FOREGROUND_SERVICE` (+ `DATA_SYNC`, `SPECIAL_USE`) e `RECEIVE_BOOT_COMPLETED`: la sorveglianza degli allarmi. `REQUEST_INSTALL_PACKAGES`: l'aggiornamento dell'app. `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`: la configurazione di un nodo; `BLUETOOTH`, `BLUETOOTH_ADMIN` e `ACCESS_FINE_LOCATION` solo fino ad Android 11 (`maxSdkVersion=30`), per la ricerca Bluetooth.

Non c'è stata una revisione di sicurezza indipendente. Leggi [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) e [SECURITY.md](SECURITY.md).

## 🔌 Interfacce

| Parte | Cosa imposta |
|---|---|
| ARMOR-SERVER | 55 percorsi distinti di `/api/v1` in `network/ArmorApiClient.kt` (l'interfaccia è `network/ArmorApi.kt`); il radar si legge ogni 1,5 s; le telecamere in MJPEG tramite il server |
| Due indirizzi | Quella privata sul Wi-Fi di casa e quella per fuori (dati mobili o VPN), scelte in base a dove si trova il telefono; se una tace si prova l'altra e il messaggio dice cosa fallisce |
| Bluetooth | Trova i nodi che annunciano `ARMOR-xxxxxx`, crea l'amministratore di uno nuovo o accede con il PIN del nodo, cerca reti Wi-Fi e imposta il nome, il Wi-Fi o un indirizzo fisso e il broker; il protocollo è `model/NodeBle.kt`; un cercatore di nodi elenca quelli già in rete |
| Voce | `POST /api/v1/voice/command`: una lista chiusa di comandi in otto lingue, con una conferma legata a te (`model/Voice.kt`, `VoiceScreens.kt`) |
| Push | `PushService`: allarmi come messaggi con soli dati, annunciati una volta sia che arrivino via push sia via sorveglianza; i passi di Firebase sono in [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) (otto lingue) |

## ⚙️ Configurazione

| Nome | Valore | Cosa imposta |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | Android 8.0 o successivo sul telefono |
| `versionName` | = `armor.project.json` | La build fallisce se manifesto e versione differiscono |
| JDK / Android SDK | 17+ / 37 | Per compilare l'app ed eseguire i test |

Nell'app (non nei file): la lingua (le otto o seguire il telefono), i due indirizzi, *Ricordami*, il blocco del telefono, l'allarme parlato e le notifiche, in *Altro > Impostazioni*. Limiti nel codice: la sorveglianza interroga ogni 10 s, rallenta fino a 60 s e ignora gli allarmi più vecchi di 15 minuti.

## 🚀 Avvio rapido

**Requisiti:** JDK 17+, l'SDK Android (compile SDK 37), un telefono con Android 8.0+ (minSdk 26).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 342 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Le versioni sono pubblicate su GitHub, da dove l'app stessa cerca gli aggiornamenti; l'APK è firmato con la chiave di rilascio propria del responsabile, conservata fuori dal repository, e l'app confronta il certificato di firma di un download con il proprio prima di passare il file al programma di installazione di Android. Vedi il [confine del client](docs/CLIENT_BOUNDARY.md).

## 🧪 Test

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| Parte | Test | Cosa copre |
|---|---|---|
| Allarmi | 67 | `AlarmPolicy`, `AlarmBanner`, `AlarmPanels`, `AlarmCodes`, `AlarmRoute`, `NoticeFilter`, `PushEvents`, `DevicesAndAlarms` |
| Collegamento al server, lettura, dati di prova | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| View model e ruoli | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| Blocco, deposito sessione, chiavi | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| Sicurezza degli indirizzi, certificati | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| Energia e rete | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| Nodi via Bluetooth, controllo dei nodi | 26 | `NodeBle`, `NodeControl` |
| Aggiornamenti da GitHub | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| Radar e telecamere | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| Traduzioni, parole semplici, voce | 14 | `Tr`, `Friendly`, `Voice` |

342 test in 46 file, eseguiti con `testDebugUnitTest`; un server finto (`FakeServer.kt`) e dati presi da un server reale (`tools/capture_fixtures.mjs`) li alimentano. Si verifica che ogni frase sia nelle otto tabelle di lingua.

## 🏗️ Compilazione e rilascio

Le versioni sono pubblicate su GitHub, da dove l'app stessa controlla gli aggiornamenti; l'app cerca l'asset `ARMOR-ANDROID-CONTROL-release.apk`. L'APK è firmato con la chiave di rilascio del manutentore, tenuta fuori dal repository, e l'app confronta il certificato di firma di un download con il proprio prima di passare il file all'installer di Android. Il `versionCode` sale prima di ogni APK, altrimenti Android non lo tratta come aggiornamento.

## 🩺 Risoluzione dei problemi

| Vedi | Perché | Fai |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | Il server rifiuta il PIN di disinserimento o chiede una nuova prova d'identità | Digita di nuovo il PIN, attendi la fine del blocco o conferma la tua identità |
| L'accesso dice che non può proseguire | L'indirizzo è fuori casa e non ha https (l'http in chiaro è rifiutato) | Segui la guida: una VPN, o un indirizzo pubblico con https |
| La connessione è rifiutata dopo aver funzionato | La chiave del certificato dell'installazione è cambiata | Associa di nuovo con il testo del QR di Studio se l'hai cambiato di proposito |
| Gli allarmi arrivano in ritardo o non arrivano in background | Le regole di batteria del produttore fermano la sorveglianza; il push richiede un progetto Firebase proprio | Escludi l'app dal risparmio batteria; segui [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| L'aggiornamento è rifiutato | Il certificato di firma del download non ne condivide nessuno con l'app installata | Non installarlo; prendi l'APK dalle versioni di questo repository |

## 📂 Struttura del repository

<p align="center">
  <img src="images/repository-structure.svg" alt="Struttura del repository di ARMOR-ANDROID-CONTROL" width="100%">
</p>

<details>
<summary>Versione testuale dell'albero</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   l'app: schermate, regole, rete e aggiornamenti
├── app/src/main/assets/                         otto tabelle di lingua, codici di allarme, mappa meteo
├── app/src/test/                                342 test e un server finto
├── docs/                                        le guide di questo repository
├── tools/                                       i controlli CI condivisi e gli strumenti del progetto
├── .github/                                     workflow CI e modelli per issue e pull request
├── images/                                      banner, icona e le due immagini di questo README
├── build.sh build.bat                           build tramite il flusso di progetto condiviso
├── test.sh test.bat                             eseguire i test tramite il flusso condiviso
├── run.sh run.bat                               avvio tramite il flusso di progetto condiviso
├── armor.project.json                           manifesto: versione, ruolo, maturità, comando di build
├── CHANGELOG.md                                 cosa è cambiato in ogni versione
├── SECURITY.md SUPPORT.md LICENSE               politica di sicurezza, supporto e licenza
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           come contribuire e il codice di condotta
├── build.gradle.kts settings.gradle.kts         la build Gradle del progetto
├── gradle.properties                            impostazioni di Gradle
├── gradlew gradlew.bat gradle/                  il wrapper di Gradle
├── build-test.sh build-test.bat                 eseguire i test tramite il flusso condiviso
└── .gitattributes .gitignore                    impostazioni di Git
```

</details>

## 🗺️ Roadmap e limiti noti

**Limiti noti oggi**

* Le schermate di solare, elettrico e centrale di allarme non hanno incontrato apparecchiature reali (esiste solo un nodo radar).
* La configurazione Bluetooth di un nodo non è stata confermata con un nodo reale, e il push via Firebase non è stato provato su un telefono.
* La sorveglianza degli allarmi in background con le regole di batteria di ogni produttore non è dimostrata.
* Nessuna revisione di sicurezza indipendente.

**Prossimi passi**

* Provare le schermate di solare, elettrico e centrale di allarme con nodi reali, e la configurazione Bluetooth con un nodo reale.
* Provare il push via Firebase con il progetto proprio di un'installazione, e la sorveglianza degli allarmi su più produttori.

## 🌍 Lingue

Il README e i testi esistono in inglese, spagnolo, francese, italiano, tedesco, cinese, giapponese e portoghese europeo.

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
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - Client operatore per iPhone
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Politica di inferenza visiva che spiega le sue decisioni e non agisce mai
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zone delle telecamere, maschere e regole di rilevamento per la Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenti vocali offline con una conferma impossibile da falsificare
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Contenitori, elettronica e matrice di accettazione da banco
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Distribuzione, banco di prova CM5, backup e TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulatore di telemetria offline con guasti ripetibili
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Rileva, installa e aggiorna i repository stessi dell'ecosistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architettura, base di sicurezza e matrice delle capacità

## 📚 Documentazione

* [Confini del client](docs/CLIENT_BOUNDARY.md): cosa fa e non fa mai il client
* [Notifiche push](docs/PUSH_NOTIFICATIONS.md): i passi di Firebase, in otto lingue
* [Prove al banco](docs/BENCH_TESTS.md): verifiche con un telefono reale
* [Matrice delle capacità: cosa è provato e cosa no](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catalogo dei progetti: versioni e dipendenze tra i repository](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Cronologia delle modifiche di questo repository](CHANGELOG.md)

## 🤝 Contribuire e comunità

* Prima di una pull request esegui `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py`: deve passare. Vedi [CONTRIBUTING.md](CONTRIBUTING.md) e il [codice di condotta](CODE_OF_CONDUCT.md).
* Il README esiste in otto lingue ed è generato dalle sorgenti in ARMOR-DOCS: una modifica al testo si fa lì.
* Una modifica a un messaggio tra componenti parte da ARMOR-COMMON.
* Non descrivere come funzionante su una scheda ciò che è stato solo compilato.
* Segnala una vulnerabilità in privato: [SECURITY.md](SECURITY.md). Domande, idee e segnalazioni: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTORE

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENZA

GPL-3.0-or-later - vedi [LICENSE](LICENSE).
