<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  🇫🇷 <b>Français</b> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### Client mobile d'opérateur pour ARMOR-SERVER : alarmes, caméras, radar, énergie, réseau et configuration des nœuds, en huit langues

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-324%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.5-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Vérification d'honnêteté - ce qui fonctionne aujourd'hui:** Les règles de sécurité de l'adresse, de décision d'alarme, de veilleur d'alarmes, de traduction et de rôle en lecture seule sont couvertes par 324 tests unitaires et l'application se compile (`gradlew testDebugUnitTest assembleDebug`). Elle fonctionne sur un vrai téléphone contre le banc de test CM5 : la connexion, le moniteur de caméras, l'armement et les alarmes, le radar, la configuration d'un nœud en Bluetooth et les mises à jour depuis GitHub sont utilisés. Ce qui n'est **pas** encore prouvé : les écrans solaire, électrique et centrales d'alarme avec du vrai matériel (seul un nœud radar existe), le push via Firebase (il est dans l'application et demande un projet Firebase propre ; il n'a pas encore été essayé sur un téléphone) et la veille d'alarmes en arrière-plan sous les règles de batterie de chaque fabricant.

---

## 📋 En un coup d'œil

| | |
|---|---|
| Version | `0.7.5` |
| Maturité | Établi |
| Tests | 324 (`gradlew testDebugUnitTest`) |
| Technologie | Kotlin / Jetpack Compose |
| Sur matériel réel | Utilisée sur un vrai téléphone contre le banc de test CM5 ; les écrans solaire, électrique et centrales d'alarme attendent de vrais nœuds |

## 🚦 État du projet

Chaque ligne est une partie de ce dépôt et le feu dit où elle en est aujourd'hui. Elle est écrite d'après ce que le dépôt peut montrer (tests, compilations, exécutions), pas d'après ce qui est prévu.

🟢 Fait et vérifié · 🟡 Fonctionne, avec des limites connues · 🟠 À moitié fait ou pas encore vérifié · 🔴 Pas fait ou ne fonctionne pas

| Domaine | État | Ce que cela veut dire ici |
|---|---|---|
| Logiciel | 🟢 | L'application compile (`gradlew testDebugUnitTest assembleDebug`) et tourne sur un vrai téléphone contre le banc d'essai CM5. |
| Tests | 🟢 | 324 tests unitaires réussissent (`gradlew testDebugUnitTest`) : sécurité des adresses, décision d'alarme, veilleur d'alarmes, traductions et rôle en lecture seule. |
| Interface | 🟡 | La connexion, le moniteur de caméras, l'armement et les alarmes et le radar sont utilisés sur un vrai téléphone ; la configuration d'un nœud par Bluetooth n'a pas été confirmée avec un vrai nœud, et les écrans solaire, électrique et de centrale d'alarme n'ont pas vu de vrais équipements. |
| Matériel | 🟡 | Utilisée sur un vrai téléphone contre le banc d'essai CM5 ; la veille d'alarmes en arrière-plan sous les règles de batterie de chaque fabricant n'est pas prouvée. |
| Sécurité | 🟡 | Les règles de sécurité des adresses et du rôle en lecture seule sont couvertes par des tests unitaires ; il n'y a pas eu de revue indépendante. |
| Mises à jour | 🟡 | Les mises à jour depuis GitHub sont utilisées sur le téléphone. |
| Intégration | 🟠 | Le push via Firebase est dans l'application mais demande un projet Firebase propre et n'a pas été essayé sur un téléphone. |
| Documentation | 🟢 | Huit langues et le journal des modifications sont à jour. |

## 🎯 Présentation

* **Connexion avec le login du serveur lui-même :** IP, port, utilisateur et mot de passe ; le mot de passe crée une session HttpOnly ; il n'est stocké sur le téléphone que si vous cochez **Se souvenir de moi** (désactivé par défaut : utilisateur et mot de passe scellés avec une clé propre du Keystore, pour ce serveur seulement, oubliés dès que vous décochez). Un rôle **viewer** ne fait que regarder : tout bouton qui change quelque chose est désactivé.
* **Verrouillage avec le téléphone :** l'application demande l'empreinte, le visage ou le code du téléphone lui-même avant d'armer, de désarmer, de couper une sirène, de déverrouiller et pour les autres ordres critiques (activé par défaut seulement si le téléphone a une empreinte ou un visage) et, si tu l'actives, pour ouvrir l'application. Ton administrateur peut l'exiger depuis Studio mais jamais abaisser ton choix ; un téléphone sans verrouillage d'écran n'est jamais bloqué et l'écran verrouillé propose toujours de se reconnecter avec le mot de passe. En option (désactivé par défaut, avec un avertissement visible), la clé qui scelle la session enregistrée peut être liée au déverrouillage du téléphone ; la surveillance des alarmes en arrière-plan attend alors que vous déverrouilliez le téléphone après un redémarrage.
* **Moniteur de caméras :** de 1 à 16 tuiles, une vue agrandie, MJPEG en direct qui garde le rapport d'image, une télécommande PTZ bornée, captures et enregistrements, avec visionneuse et confirmation de suppression des enregistrements.
* **Armer et désarmer,** après une **vérification avant armement** (ce qui est ouvert, muet ou en panne) et avec le PIN pour désarmer ; **alarmes** à acquitter, avec un badge pour celles en attente ; **appareils** (fumée, gaz, inondation, porte, fenêtre, mouvement, climat, prises, lumières, sirènes, serrures, volets, moteurs de portail et de garage, chauffe-eau) avec leur état et Allumer / Éteindre / Basculer.
* **Des alarmes qui vous atteignent :** un son pour chaque gravité, une **alarme parlée** facultative, des notifications pour un nœud qui passe en HAUT, une caméra muette, une alarme d'appareil et (armé) un nœud hors ligne, et **une centrale qui sonne réveille le téléphone** ; la veille en arrière-plan survit à un redémarrage et lève le pied quand le serveur est en panne.
* **Radar en direct, en 2D et 3D :** l'onglet Radar dessine le site tel que conçu dans Studio et les personnes que voient les radars s'y déplacer, rafraîchi toutes les secondes et demie ; faites glisser, pincez et tournez la vue 3D.
* **Énergie solaire et électrique :** les totaux (soleil, consommation, batteries avec leur charge, présence du réseau), chaque onduleur et batterie avec les cellules à la demande, et les canaux et alarmes des nœuds électriques.
* **Centrales d'alarme :** l'état de chaque nœud ARMOR-NODE-ALARM, ses zones et événements, et des commandes d'armement / désarmement quand le serveur les permet.
* **Réseau :** les appareils du réseau de la maison, l'état d'internet et l'**adresse publique** avec le fournisseur et le lieu, comme les rapporte le nœud réseau.
* **Configurer un nœud de terrain en Bluetooth** (depuis l'écran de connexion ou *Plus > Configurer un nœud*) : trouve les nœuds qui annoncent `ARMOR-xxxxxx`, crée l'administrateur d'un nouveau ou se connecte (avec le PIN du nœud), cherche les réseaux Wi-Fi et règle le nom du nœud, le Wi-Fi d'un routeur ou une adresse fixe et le broker ; un **chercheur de nœuds** liste ceux déjà sur le réseau.
* **L'assistant :** commandes écrites et parlées (liste fermée, en huit langues, avec une confirmation liée à vous) pour demander l'état de la maison ou armer, désarmer, faire taire et allumer ou éteindre les lumières.
* **Bibliothèque de preuves,** état du périmètre et des nœuds, un **historique** de chaque alerte, nœud, caméra, appareil, alarme et changement de mode, la **carte météo**, et les **services** et mises à jour de l'installation.
* **Huit langues** (anglais, espagnol, allemand, français, italien, japonais, chinois et portugais), choisies dans *Plus > Réglages* ou suivant le téléphone ; on vérifie que chaque phrase est dans les huit tables.
* **Prudence avec le mot de passe :** le HTTP en clair n'est permis que vers un *littéral* IPv4 de LAN privé ou de boucle locale ; un nom d'hôte qui ressemble seulement à une adresse privée (`10.attacker.example`) ou une adresse avec un zéro initial est refusé. L'apparence est faite d'icônes : surfaces quasi noires, accent cyan, ambre pour l'attention.
* **La sécurité depuis le téléphone :** le second facteur et ses codes de récupération, les sessions ouvertes, une vérification de la façon dont le serveur voit le téléphone, la nouvelle preuve d'identité pour les actions critiques depuis l'extérieur et, pour un administrateur, le VPN (appareils avec leur configuration) et le nom DNS dynamique.**Tout le système sur l'écran d'état et l'entrée depuis l'extérieur :** l'écran d'état résume les centrales d'alarme, les services, les caméras qui enregistrent et la sécurité et l'accès à distance, chaque carte ouvrant son écran ; et quand l'adresse saisie est hors de la maison et sans https, la connexion dit pourquoi elle ne peut pas continuer et un guide explique les deux voies sûres (VPN, ou adresse publique avec https).
* **Santé des nœuds et modes d'armement :** un écran Santé des nœuds dans Plus (durée de marche, motif du redémarrage, mémoire, liaison et signal, connexions au broker, redémarrages, dernière erreur et avertissements du serveur de chaque nœud) et, sur l'écran des Centrales d'alarme, un sélecteur de mode (Absent, Présent, Nuit et les vôtres) qui arme une centrale dans le mode choisi quand son micrologiciel les connaît. Huit langues.
* **Connexions dans l'historique :** l'écran Historique a une seconde liste, en lecture seule, avec les appareils et applications qui se connectent au serveur (Studio, cette appli, les panneaux des nœuds, le service vocal), la dernière fois que chacun a été vu, ses sessions ouvertes, ses connexions et ses refus ; l'appli dit maintenant au serveur ce qu'elle est pour apparaître sous son nom. Les adresses ne sont visibles que des administrateurs.
* **Maintenance des batteries (administrateurs) :** les opérations simples sur une batterie Pylontech - horloge, compteurs et journaux, redémarrage, capacité, test MOSFET, défaut de fusible, sorties - en deux étapes comme dans Studio (le serveur fait lire la batterie par le nœud, l'écran montre exactement ce qui sera fait, puis "Le faire maintenant" avec le numéro de série saisi de nouveau), avec le résultat et ce qui a changé ; les limites, le firmware et la console expert restent dans Studio.
* **Commande des nœuds (administrateurs) :** Plus > Contrôle des nœuds offre les mêmes opérations que Studio - redémarrer, mettre en pause, reprendre, mettre en veille (travail arrêté, reste connecté), identifier, diagnostic et maintenance activée ou désactivée - à un nœud ou à tous, en demandant de saisir de nouveau le nom du nœud (ou le mot RESTART, PAUSE ou STANDBY pour tous) et la nouvelle preuve d'identité, en montrant où en est chaque nœud et en avertissant qu'une centrale d'alarme ou un radar en pause ou en veille laisse la maison sans protection. Il n'existe ni réinitialisation d'usine, ni effacement, ni sommeil profond.
* **Une fenêtre d'alarme dans l'appli ouverte :** tant qu'une alarme attend quelqu'un, une bannière pulse au-dessus de tous les écrans sauf celui des alarmes (rouge et rapide pour une critique, ambre pour les autres, fixe si le téléphone a les animations coupées), dit quelle alarme est la pire et combien attendent, et laisse utiliser l'appli en dessous ; elle peut ouvrir les alarmes, confirmer depuis la bannière ou être fermée jusqu'à l'arrivée d'une autre alarme.
* **Depuis l'extérieur :** deux adresses (la privée sur le Wi-Fi de la maison, celle de l'extérieur avec les données mobiles ou le VPN) choisies selon où est le téléphone, l'autre étant essayée si l'une ne répond pas, avec un message qui dit ce qui échoue ; la clé du certificat de l'installation est retenue la première fois (ou appairée avec le texte du QR de Studio) et une autre est refusée, et le http simple est refusé pour un serveur déjà joint en https ; le panneau d'un nœud peut s'ouvrir via le serveur ; les alarmes peuvent arriver en push de données seules, annoncées une fois qu'elles viennent du push ou de la surveillance ; l'app ouverte et la surveillance partagent une lecture du serveur ; les flux des caméras s'arrêtent hors de l'écran et reviennent avec des attentes croissantes.

## 🧩 Rôle dans A.R.M.O.R.

ARMOR-ANDROID-CONTROL est la fenêtre du téléphone sur le même [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) qu'utilise [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) : il lit et ordonne par lui et rien d'autre. C'est là que les alarmes vous suivent (une centrale qui sonne réveille le téléphone), que vous interrogez l'assistant à la voix, et l'outil pour configurer un nouveau nœud en Bluetooth sur le terrain, à côté de lui.

## 📂 Structure du dépôt

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, NodeControlScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmBanner.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, DeviceLock.kt, PhoneLock.kt, LockScreens.kt, PinDialog.kt, MjpegFeed.kt, AlarmCodes.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md (the Firebase steps, in eight languages), BENCH_TESTS.md
├── tools/   capture_fixtures.mjs (takes fixtures from a real server), the shared CI checks (armor_ci_validate.py, armor_project_tool.py, _armor_readme_parity.py)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ Environnement de développement

**Prérequis:** JDK 17+, le SDK Android (compile SDK 37), un téléphone Android 8.0+ (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 324 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Les versions sont publiées sur GitHub, d'où l'application cherche elle-même les mises à jour ; l'APK est signé avec la clé de publication propre du responsable, conservée hors du dépôt, et l'application compare le certificat de signature d'un téléchargement avec le sien avant de passer le fichier à l'installateur d'Android. Voir la [frontière du client](docs/CLIENT_BOUNDARY.md).

## 🔗 Projets liés

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) est un système de sécurité périmétrique composé de dépôts indépendants. Chacun a sa propre version, ses propres tests et son propre README ; voici la famille :

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contrats de messages, validateurs, vecteurs de conformité et types générés
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Firmware du nœud de terrain pour ESP32-S3 avec trois radars et son propre panneau web
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protocoles des onduleurs et batteries solaires et messages d'un nœud passerelle
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Nœud électrique : compteurs, le message des mesures du réseau et les règles de commutation
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Nœud et centrale d'alarme : zones, armement, temporisations, sirène et PIN, avec ou sans le serveur
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Panneau tactile : l'état du système sur un écran mural, armer et acquitter, et la maison de l'assistant vocal
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - Le réseau local : ses appareils, internet et ce qui change
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinateur central : télémétrie, alarmes, appareils, relevés solaires et caméras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Console web : caméras, radar, alarmes, énergie solaire et concepteur de site 2D/3D
* **ARMOR-ANDROID-CONTROL** (ce dépôt) - Client Android de l'opérateur avec radar 2D/3D en direct
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - Client opérateur iPhone
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Politique d'inférence visuelle qui explique ses décisions et n'agit jamais
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zones de caméra, masques et règles de détection pour la Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intentions vocales hors ligne avec une confirmation impossible à falsifier
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Boîtiers, électronique et matrice d'acceptation sur banc
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Déploiement, banc d'essai CM5, sauvegarde et TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulateur de télémétrie hors ligne avec des pannes reproductibles
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Détecte, installe et met à jour les propres dépôts de l'écosystème
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architecture, base de sécurité et matrice des capacités

## 📚 Documentation et communauté

Pour en savoir plus :

* [Matrice des capacités : ce qui est prouvé et ce qui ne l'est pas](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catalogue des projets : versions et dépendances entre les dépôts](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Historique des modifications de ce dépôt](CHANGELOG.md)
* [Licence (GPL-3.0-or-later)](LICENSE)
* Questions, idées et rapports : electrohobby3d@gmail.com

## 👤 AUTEUR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCE

GPL-3.0-or-later - voir [LICENSE](LICENSE).
