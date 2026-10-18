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
  <img src="https://img.shields.io/badge/Tests-130%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.6.5-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Vérification d'honnêteté - ce qui fonctionne aujourd'hui:** Les règles de sécurité de l'adresse, de décision d'alarme, de veilleur d'alarmes, de traduction et de rôle en lecture seule sont couvertes par 130 tests unitaires et l'application se compile (`gradlew testDebugUnitTest assembleDebug`). Elle fonctionne sur un vrai téléphone contre le banc de test CM5 : la connexion, le moniteur de caméras, l'armement et les alarmes, le radar, la configuration d'un nœud en Bluetooth et les mises à jour depuis GitHub sont utilisés. Ce qui n'est **pas** encore prouvé : les écrans solaire, électrique et centrales d'alarme avec du vrai matériel (seul un nœud radar existe), le push via Firebase (il est dans l'application et demande un projet Firebase propre ; il n'a pas encore été essayé sur un téléphone) et la veille d'alarmes en arrière-plan sous les règles de batterie de chaque fabricant.

---

## 📋 En un coup d'œil

| | |
|---|---|
| Version | `0.6.5` |
| Maturité | Établi |
| Tests | 130 (`gradlew testDebugUnitTest`) |
| Technologie | Kotlin / Jetpack Compose |
| Sur matériel réel | Utilisée sur un vrai téléphone contre le banc de test CM5 ; les écrans solaire, électrique et centrales d'alarme attendent de vrais nœuds |

## 🎯 Présentation

* **Connexion avec le login du serveur lui-même :** IP, port, utilisateur et mot de passe ; le mot de passe crée une session HttpOnly ; il n'est stocké sur le téléphone que si vous cochez **Se souvenir de moi** (désactivé par défaut : utilisateur et mot de passe scellés avec une clé propre du Keystore, pour ce serveur seulement, oubliés dès que vous décochez). Un rôle **viewer** ne fait que regarder : tout bouton qui change quelque chose est désactivé.
* **Moniteur de caméras :** de 1 à 16 tuiles, une vue agrandie, MJPEG en direct qui garde le rapport d'image, une télécommande PTZ bornée, captures et enregistrements, avec visionneuse et confirmation de suppression des enregistrements.
* **Armer et désarmer,** après une **vérification avant armement** (ce qui est ouvert, muet ou en panne) et avec le PIN pour désarmer ; **alarmes** à acquitter, avec un badge pour celles en attente ; **appareils** (fumée, gaz, inondation, porte, fenêtre, mouvement, climat, prises, lumières, sirènes, serrures, volets, moteurs de portail et de garage, chauffe-eau) avec leur état et Allumer / Éteindre / Basculer.
* **Des alarmes qui vous atteignent :** un son pour chaque gravité, une **alarme parlée** facultative, des notifications pour un nœud qui passe en HAUT, une caméra muette, une alarme d'appareil et (armé) un nœud hors ligne, et **une centrale qui sonne réveille le téléphone** ; la veille en arrière-plan survit à un redémarrage et lève le pied quand le serveur est en panne.
* **Radar en direct, en 2D et 3D :** l'onglet Radar dessine le site tel que conçu dans Studio et les personnes que voient les radars s'y déplacer, rafraîchi toutes les secondes et demie ; faites glisser, pincez et tournez la vue 3D.
* **Énergie solaire et électrique :** les totaux (soleil, consommation, batteries avec leur charge, présence du réseau), chaque onduleur et batterie avec les cellules à la demande, et les canaux et alarmes des nœuds électriques.
* **Centrales d'alarme :** l'état de chaque nœud ARMOR-ALARM, ses zones et événements, et des commandes d'armement / désarmement quand le serveur les permet.
* **Réseau :** les appareils du réseau de la maison, l'état d'internet et l'**adresse publique** avec le fournisseur et le lieu, comme les rapporte le nœud réseau.
* **Configurer un nœud de terrain en Bluetooth** (depuis l'écran de connexion ou *Plus > Configurer un nœud*) : trouve les nœuds qui annoncent `ARMOR-xxxxxx`, crée l'administrateur d'un nouveau ou se connecte (avec le PIN du nœud), cherche les réseaux Wi-Fi et règle le nom du nœud, le Wi-Fi d'un routeur ou une adresse fixe et le broker ; un **chercheur de nœuds** liste ceux déjà sur le réseau.
* **L'assistant :** commandes écrites et parlées (liste fermée, en huit langues, avec une confirmation liée à vous) pour demander l'état de la maison ou armer, désarmer, faire taire et allumer ou éteindre les lumières.
* **Bibliothèque de preuves,** état du périmètre et des nœuds, un **historique** de chaque alerte, nœud, caméra, appareil, alarme et changement de mode, la **carte météo**, et les **services** et mises à jour de l'installation.
* **Huit langues** (anglais, espagnol, allemand, français, italien, japonais, chinois et portugais), choisies dans *Plus > Réglages* ou suivant le téléphone ; on vérifie que chaque phrase est dans les huit tables.
* **Prudence avec le mot de passe :** le HTTP en clair n'est permis que vers un *littéral* IPv4 de LAN privé ou de boucle locale ; un nom d'hôte qui ressemble seulement à une adresse privée (`10.attacker.example`) ou une adresse avec un zéro initial est refusé. L'apparence est faite d'icônes : surfaces quasi noires, accent cyan, ambre pour l'attention.
* **La sécurité depuis le téléphone :** le second facteur et ses codes de récupération, les sessions ouvertes, une vérification de la façon dont le serveur voit le téléphone, la nouvelle preuve d'identité pour les actions critiques depuis l'extérieur et, pour un administrateur, le VPN (appareils avec leur configuration) et le nom DNS dynamique.**Tout le système sur l'écran d'état et l'entrée depuis l'extérieur :** l'écran d'état résume les centrales d'alarme, les services, les caméras qui enregistrent et la sécurité et l'accès à distance, chaque carte ouvrant son écran ; et quand l'adresse saisie est hors de la maison et sans https, la connexion dit pourquoi elle ne peut pas continuer et un guide explique les deux voies sûres (VPN, ou adresse publique avec https).
* **Santé des nœuds et modes d'armement :** un écran Santé des nœuds dans Plus (durée de marche, motif du redémarrage, mémoire, liaison et signal, connexions au broker, redémarrages, dernière erreur et avertissements du serveur de chaque nœud) et, sur l'écran des Centrales d'alarme, un sélecteur de mode (Absent, Présent, Nuit et les vôtres) qui arme une centrale dans le mode choisi quand son micrologiciel les connaît. Huit langues.

## 🧩 Rôle dans A.R.M.O.R.

ARMOR-ANDROID-CONTROL est la fenêtre du téléphone sur le même [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) qu'utilise [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) : il lit et ordonne par lui et rien d'autre. C'est là que les alarmes vous suivent (une centrale qui sonne réveille le téléphone), que vous interrogez l'assistant à la voix, et l'outil pour configurer un nouveau nœud en Bluetooth sur le terrain, à côté de lui.

## 📂 Structure du dépôt

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

## 🛠️ Environnement de développement

**Prérequis:** JDK 17+, the Android SDK (compile SDK 37), an Android 8.0+ phone (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 130 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Les versions sont publiées sur GitHub, d'où l'application cherche elle-même les mises à jour ; aujourd'hui l'APK est signé avec le certificat de débogage Android du responsable, pas avec une clé de publication. Voir la [frontière du client](docs/CLIENT_BOUNDARY.md).

## 🔗 Projets liés

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) est un système de sécurité périmétrique composé de dépôts indépendants. Chacun a sa propre version, ses propres tests et son propre README ; voici la famille :

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contrats de messages, validateurs, vecteurs de conformité et types générés
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - Firmware du nœud de terrain pour ESP32-S3 avec trois radars et son propre panneau web
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - Protocoles des onduleurs et batteries solaires et messages d'un nœud passerelle
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - Nœud électrique : compteurs, le message des mesures du réseau et les règles de commutation
* **[ARMOR-ALARM](https://github.com/JuanenRac/ARMOR-ALARM)** - Nœud et centrale d'alarme : zones, armement, temporisations, sirène et PIN, avec ou sans le serveur
* **[ARMOR-HMI](https://github.com/JuanenRac/ARMOR-HMI)** - Panneau tactile : l'état du système sur un écran mural, armer et acquitter, et la maison de l'assistant vocal
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - Le réseau local : ses appareils, internet et ce qui change
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinateur central : télémétrie, alarmes, appareils, relevés solaires et caméras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Console web : caméras, radar, alarmes, énergie solaire et concepteur de site 2D/3D
* **ARMOR-ANDROID-CONTROL** (ce dépôt) - Client Android de l'opérateur avec radar 2D/3D en direct
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
