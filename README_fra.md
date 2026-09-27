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
  <a href="README_jpn.md">🇯🇵 日本語</a>
</p>

### Client mobile de l'opérateur pour ARMOR-SERVER

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**Vérification d'honnêteté - ce qui fonctionne aujourd'hui:** Les règles de sûreté des adresses et de décision d'alarme ont des tests unitaires (55) et l'application se compile. Elle **n'a pas été exécutée sur un téléphone face au serveur** : le flux d'autorisation des notifications et le service en arrière-plan ne sont donc pas vérifiés. Armement, alarmes et appareils suivent les routes du serveur mais n'ont pas été essayés sur un vrai téléphone.

---

## 🎯 Présentation

* **Connexion avec l'identifiant du serveur :** IP, port, utilisateur et mot de passe ; le mot de passe crée une session HttpOnly et n'est jamais conservé sur le téléphone.
* **Moniteur de caméras :** 1 à 16 vignettes, une vue agrandie, MJPEG en direct qui garde le rapport d'image, une manette PTZ bornée, captures et enregistrements.
* **Armer et désarmer,** après confirmation ; **alarmes** à acquitter, avec un badge pour celles en attente ; **appareils** (fumée, gaz, inondation, porte, fenêtre, mouvement, climat, prises, lumières, sirènes, serrures) avec leur état et On / Off / Basculer.
* **Radar en direct, en 2D et en 3D :** l'onglet Radar dessine le site tel que conçu dans Studio (sol, bâtiments, arbres, poteaux, champs des radars et des caméras) et les personnes que les radars voient bouger, rafraîchi toutes les secondes et demie ; glisser, pincer et tourner la vue 3D. Le placement suit les règles de Studio, mais n'a jamais montré un vrai radar.
* **Solaire :** une entrée dans *Más* (et une tuile dans l'écran d'état) montre les totaux (soleil, consommation, batteries avec leur charge et leur flux, présence du réseau) et chaque onduleur et batterie que le serveur signale : leurs valeurs, les cellules d'une batterie à la demande avec la plus haute et la plus basse marquées, les équipements qui attendent encore des données, et une marque sur une mesure d'exemple ou un équipement muet. Actualisé toutes les cinq secondes tant qu'il est ouvert ; testé sur les formes de réponse du serveur et essayé dans un émulateur face à un serveur local avec des mesures d'exemple, jamais avec de vrais équipements. Les alarmes solaires (panne d'onduleur, batterie faible ou en protection, équipement muet) réveillent le téléphone comme celles d'un appareil.
* **Configurer un nœud de terrain en Bluetooth** (depuis l'écran de connexion ou *Más > Configurar un nodo*) : trouve les nœuds qui annoncent `ARMOR-xxxxxx`, crée l'administrateur d'un nouveau ou se connecte, cherche des réseaux Wi-Fi et règle le nom du nœud, le Wi-Fi d'un routeur ou une adresse fixe et le broker, pour les nœuds sans câble Ethernet. Compilé et testé unitairement, jamais exécuté face à un nœud ni un téléphone.
* **Bibliothèque de preuves,** état du périmètre et des nœuds, et **historique** de chaque alerte, nœud, caméra, appareil, alarme et changement de mode.
* **Notifications d'alarme** pour un nœud qui atteint HIGH, une caméra qui ne répond plus, une alarme d'appareil (fumée, gaz, inondation, panique à tout moment ; une porte, fenêtre ou un capteur de mouvement pendant l'armement) et, armé, un nœud qui se déconnecte ; une veille facultative en arrière-plan utilise la session en cours et le signale quand elle se termine.
* **Prudence avec le mot de passe :** le HTTP simple n'est permis que vers un *littéral* IPv4 de réseau local privé ou de bouclage. Un nom d'hôte qui commence seulement comme une adresse privée (`10.attacker.example`) ou une adresse avec un zéro initial (certains résolveurs lisent `010.0.0.1` comme la publique `8.0.0.1`) est refusé.
* **Réseau électrique :** une entrée dans *Plus* (et une tuile sur l'écran d'état) montre la puissance de l'entrée du réseau (en soutirage ou en injection), chaque canal que mesurent les nœuds électriques (tension, courant, puissance, énergie, fréquence, facteur de puissance, l'état d'un interrupteur) et les alarmes des compteurs ; les alarmes des nœuds électriques (celle d'un compteur, la tension du secteur hors plage, le réseau perdu, un nœud qui ne répond plus) réveillent l'opérateur comme les alarmes solaires. La carte d'un onduleur montre aussi sa seconde entrée photovoltaïque et les unités d'un système en parallèle.
* **Un aspect fait d'icônes :** surfaces presque noires, accent cyan, ambre pour l'attention, grandes icônes avec peu de mots, barre inférieure, page À propos et bouton de déconnexion.

## 📂 Structure du dépôt

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

## 🛠️ Environnement de développement

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

L'APK de débogage n'est pas signé pour la distribution. Voir la [frontière du client](docs/CLIENT_BOUNDARY.md).

## 🔗 Projets liés

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) est un système de sécurité périmétrique composé de dépôts indépendants. Chacun a sa propre version, ses propres tests et son propre README ; voici la famille :

* **[ARMOR-COMMON](../ARMOR-COMMON)** - Contrats de messages, validateurs, vecteurs de conformité et types générés
* **[ARMOR-RADAR](../ARMOR-RADAR)** - Firmware du nœud de terrain pour ESP32-S3 avec trois radars et son propre panneau web
* **[ARMOR-SOLAR](../ARMOR-SOLAR)** - Protocoles des onduleurs et batteries solaires et messages d'un nœud passerelle
* **[ARMOR-ELECTRICAL](../ARMOR-ELECTRICAL)** - Nœud électrique : compteurs, le message des mesures du réseau et les règles de commutation
* **[ARMOR-NETWORK](../ARMOR-NETWORK)** - Le réseau local : ses appareils, internet et ce qui change
* **[ARMOR-SERVER](../ARMOR-SERVER)** - Coordinateur central : télémétrie, alarmes, appareils, relevés solaires et caméras
* **[ARMOR-STUDIO](../ARMOR-STUDIO)** - Console web : caméras, radar, alarmes, énergie solaire et concepteur de site 2D/3D
* **ARMOR-ANDROID-CONTROL** (ce dépôt) - Client Android de l'opérateur avec radar 2D/3D en direct
* **[ARMOR-SERVER-AI](../ARMOR-SERVER-AI)** - Politique d'inférence visuelle qui explique ses décisions et n'agit jamais
* **[ARMOR-VOICE-AI](../ARMOR-VOICE-AI)** - Intentions vocales hors ligne avec une confirmation impossible à falsifier
* **[ARMOR-HARDWARE](../ARMOR-HARDWARE)** - Boîtiers, électronique et matrice d'acceptation sur banc
* **[ARMOR-DEVOPS](../ARMOR-DEVOPS)** - Déploiement, banc d'essai CM5, sauvegarde et TLS
* **[ARMOR-SIMULATOR](../ARMOR-SIMULATOR)** - Simulateur de télémétrie hors ligne avec des pannes reproductibles
* **[ARMOR-UPDATER](../ARMOR-UPDATER)** - Détecte, installe et met à jour les propres dépôts de l'écosystème
* **[ARMOR-DOCS](../ARMOR-DOCS)** - Architecture, base de sécurité et matrice des capacités

## 📚 Documentation et communauté

Pour en savoir plus :

* [Matrice des capacités : ce qui est prouvé et ce qui ne l'est pas](../ARMOR-DOCS/docs/CAPABILITY_MATRIX.md)
* [Catalogue des projets : versions et dépendances entre les dépôts](../ARMOR-DOCS/docs/PROJECT_CATALOG.md)
* [Historique des modifications de ce dépôt](CHANGELOG.md)
* [Licence (GPL-3.0-or-later)](LICENSE)
* Questions, idées et rapports : electrohobby3d@gmail.com

## 👤 AUTEUR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCE

GPL-3.0-or-later - voir [LICENSE](LICENSE).
