<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="Bannière d'ARMOR-ANDROID-CONTROL" width="100%">
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
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 ou ultérieure">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Langage">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Langues">
  <img src="https://img.shields.io/badge/Tests-367%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.8-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturité">
</p>

---

**Vérification d'honnêteté - ce qui fonctionne aujourd'hui :** Les règles de sécurité de l'adresse, de décision d'alarme, de veilleur d'alarmes, de traduction et de rôle en lecture seule sont couvertes par 367 tests unitaires et l'application se compile (`gradlew testDebugUnitTest assembleDebug`). Elle fonctionne sur un vrai téléphone contre le banc de test CM5 : la connexion, le moniteur de caméras, l'armement et les alarmes, le radar, la configuration d'un nœud en Bluetooth et les mises à jour depuis GitHub sont utilisés. Ce qui n'est **pas** encore prouvé : les écrans solaire, électrique et centrales d'alarme avec du vrai matériel (seul un nœud radar existe), le push via Firebase (il est dans l'application et demande un projet Firebase propre ; il n'a pas encore été essayé sur un téléphone) et la veille d'alarmes en arrière-plan sous les règles de batterie de chaque fabricant.

---

## 📋 En un coup d'œil

| | |
|---|---|
| Version | `0.7.8` |
| Maturité | Établi |
| Rôle | Client opérateur Android pour ARMOR-SERVER (famille : Clients, parent : ARMOR-SERVER) |
| Technologie | Kotlin / Jetpack Compose, Android 8.0+ |
| Tests | 367 (`gradlew testDebugUnitTest`) |
| Langues | 8 (1345 phrases par langue, chaque phrase vérifiée dans toutes les tables) |
| Fonctionne sur | Un téléphone Android 8.0+ (minSdk 26) |
| Sur matériel réel | Utilisée sur un vrai téléphone contre le banc de test CM5 ; les écrans solaire, électrique et centrales d'alarme attendent de vrais nœuds |
| Licence | GPL-3.0-or-later |

**Aller à :** [Présentation](#-présentation) · [Architecture](#-architecture) · [Sécurité](#-sécurité-et-protection) · [Interfaces](#-interfaces) · [Configuration](#-configuration) · [Démarrage rapide](#-démarrage-rapide) · [Tests](#-tests) · [Dépannage](#-dépannage) · [Structure](#-structure-du-dépôt) · [Liés](#-projets-liés)

## 🚦 État du projet

Chaque ligne est une partie de ce dépôt et le feu dit où elle en est aujourd'hui. Elle est écrite d'après ce que le dépôt peut montrer (tests, compilations, exécutions), pas d'après ce qui est prévu.

🟢 Fait et vérifié · 🟡 Fonctionne, avec des limites connues · 🟠 À moitié fait ou pas encore vérifié · 🔴 Pas fait ou ne fonctionne pas

| Domaine | État | Ce que cela veut dire ici |
|---|---|---|
| Logiciel | 🟢 | L'application compile (`gradlew testDebugUnitTest assembleDebug`) et tourne sur un vrai téléphone contre le banc d'essai CM5. |
| Tests | 🟢 | 367 tests unitaires réussissent (`gradlew testDebugUnitTest`) : sécurité des adresses, décision d'alarme, veilleur d'alarmes, traductions et rôle en lecture seule. |
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
* **La sécurité depuis le téléphone :** le second facteur et ses codes de récupération, les sessions ouvertes, une vérification de la façon dont le serveur voit le téléphone, la nouvelle preuve d'identité pour les actions critiques depuis l'extérieur et, pour un administrateur, le VPN (appareils avec leur configuration) et le nom DNS dynamique.
* **Tout le système sur l'écran d'état et l'entrée depuis l'extérieur :** l'écran d'état résume les centrales d'alarme, les services, les caméras qui enregistrent et la sécurité et l'accès à distance, chaque carte ouvrant son écran ; et quand l'adresse saisie est hors de la maison et sans https, la connexion dit pourquoi elle ne peut pas continuer et un guide explique les deux voies sûres (VPN, ou adresse publique avec https).
* **Santé des nœuds et modes d'armement :** un écran Santé des nœuds dans Plus (durée de marche, motif du redémarrage, mémoire, liaison et signal, connexions au broker, redémarrages, dernière erreur et avertissements du serveur de chaque nœud) et, sur l'écran des Centrales d'alarme, un sélecteur de mode (Absent, Présent, Nuit et les vôtres) qui arme une centrale dans le mode choisi quand son micrologiciel les connaît. Huit langues.
* **Connexions dans l'historique :** l'écran Historique a une seconde liste, en lecture seule, avec les appareils et applications qui se connectent au serveur (Studio, cette appli, les panneaux des nœuds, le service vocal), la dernière fois que chacun a été vu, ses sessions ouvertes, ses connexions et ses refus ; l'appli dit maintenant au serveur ce qu'elle est pour apparaître sous son nom. Les adresses ne sont visibles que des administrateurs.
* **Maintenance des batteries (administrateurs) :** les opérations simples sur une batterie Pylontech - horloge, compteurs et journaux, redémarrage, capacité, test MOSFET, défaut de fusible, sorties - en deux étapes comme dans Studio (le serveur fait lire la batterie par le nœud, l'écran montre exactement ce qui sera fait, puis "Le faire maintenant" avec le numéro de série saisi de nouveau), avec le résultat et ce qui a changé ; les limites, le firmware et la console expert restent dans Studio.
* **Commande des nœuds (administrateurs) :** Plus > Contrôle des nœuds offre les mêmes opérations que Studio - redémarrer, mettre en pause, reprendre, mettre en veille (travail arrêté, reste connecté), identifier, diagnostic et maintenance activée ou désactivée - à un nœud ou à tous, en demandant de saisir de nouveau le nom du nœud (ou le mot RESTART, PAUSE ou STANDBY pour tous) et la nouvelle preuve d'identité, en montrant où en est chaque nœud et en avertissant qu'une centrale d'alarme ou un radar en pause ou en veille laisse la maison sans protection. Il n'existe ni réinitialisation d'usine, ni effacement, ni sommeil profond.
* **Une fenêtre d'alarme dans l'appli ouverte :** tant qu'une alarme attend quelqu'un, une bannière pulse au-dessus de tous les écrans sauf celui des alarmes (rouge et rapide pour une critique, ambre pour les autres, fixe si le téléphone a les animations coupées), dit quelle alarme est la pire et combien attendent, et laisse utiliser l'appli en dessous ; elle peut ouvrir les alarmes, confirmer depuis la bannière ou être fermée jusqu'à l'arrivée d'une autre alarme.
* **Depuis l'extérieur :** deux adresses (la privée sur le Wi-Fi de la maison, celle de l'extérieur avec les données mobiles ou le VPN) choisies selon où est le téléphone, l'autre étant essayée si l'une ne répond pas, avec un message qui dit ce qui échoue ; la clé du certificat de l'installation est retenue la première fois (ou appairée avec le texte du QR de Studio) et une autre est refusée, et le http simple est refusé pour un serveur déjà joint en https ; le panneau d'un nœud peut s'ouvrir via le serveur ; les alarmes peuvent arriver en push de données seules, annoncées une fois qu'elles viennent du push ou de la surveillance ; l'app ouverte et la surveillance partagent une lecture du serveur ; les flux des caméras s'arrêtent hors de l'écran et reviennent avec des attentes croissantes.

## 🧩 Rôle dans A.R.M.O.R.

ARMOR-ANDROID-CONTROL est la fenêtre du téléphone sur le même [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) qu'utilise [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) : il lit et ordonne par lui et rien d'autre. C'est là que les alarmes vous suivent (une centrale qui sonne réveille le téléphone), que vous interrogez l'assistant à la voix, et l'outil pour configurer un nouveau nœud en Bluetooth sur le terrain, à côté de lui.

| Nom | Comment | À quoi ça sert |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | Tout ce que l'app lit et commande |
| Nœuds de terrain | Bluetooth (`ARMOR-xxxxxx`) | Configure un nouveau nœud à côté de lui |
| GitHub | HTTPS | Vérifie et télécharge la mise à jour signée de l'app elle-même |
| Open-Meteo | HTTPS | Les prévisions, la qualité de l'air et la recherche de lieu publiques de la carte météo |

## 🏛️ Architecture

<p align="center">
  <img src="images/architecture.svg" alt="Architecture de ARMOR-ANDROID-CONTROL : qui l'appelle, ce qu'il contient et de quoi il dépend" width="100%">
</p>

## 🔒 Sécurité et protection

| Menace | Défense |
|---|---|
| Un mot de passe gardé sur le téléphone | Seulement avec *Se souvenir de moi* (désactivé par défaut) : utilisateur et mot de passe scellés par une clé Keystore propre, pour ce serveur seulement, oubliés dès que vous décochez ; pas de sauvegarde (`allowBackup=false`) |
| HTTP en clair vers Internet | Autorisé seulement vers un *littéral* IPv4 de réseau privé ou loopback ; un hôte comme `10.attacker.example` ou une adresse avec zéro initial (`010.0.0.1` se lit en octal) est refusé ; le http en clair est refusé pour un hôte atteint en https |
| Un homme du milieu en https | La confiance, ce sont les autorités du système plus celles que vous avez installées ; la clé du certificat de l'installation est mémorisée la première fois (ou appariée avec le texte du QR de Studio) et une autre est refusée |
| Un ordre critique avec le téléphone en d'autres mains | L'empreinte, le visage ou le PIN du téléphone avant d'armer, désarmer, couper une sirène, déverrouiller et les autres ordres critiques (actif par défaut seulement si le téléphone a une empreinte ou un visage) ; l'administrateur peut l'exiger depuis Studio, jamais abaisser votre choix ; un téléphone sans verrouillage n'est jamais exclu |
| Un lecteur change quelque chose | Le rôle *lecteur* voit tout mais tout bouton qui change quelque chose est désactivé (tests de rôle) |
| Un mauvais PIN de désarmement | Le PIN n'est envoyé au serveur que pour désarmer ; `wrong_pin`, `pin_locked`, `pin_not_set` et `elevation_required` sont traduits en mots simples |
| Une fausse mise à jour de l'app | Les SHA-256 des certificats de signature (historique) de l'app installée et du candidat sont comparés avant d'appeler l'installateur d'Android ; s'ils n'en partagent aucun, c'est dit clairement |
| Une alarme que le téléphone ne voit pas | Un service au premier plan (`AlarmWatcherService`) interroge toutes les 10 s, attend 15 s le déverrouillage, espace jusqu'à 60 s, dit *injoignable* après 6 échecs, survit au redémarrage ; une alarme de plus de 15 minutes n'est pas annoncée |
| Un mot de passe de caméra sur le téléphone | Les caméras se voient par le flux du serveur : l'app n'a ni mot de passe de caméra ni adresse RTSP |

### Autorisations

`INTERNET`, `ACCESS_NETWORK_STATE` : le serveur. `USE_BIOMETRIC` : le verrouillage du téléphone. `POST_NOTIFICATIONS` : les alarmes. `FOREGROUND_SERVICE` (+ `DATA_SYNC`, `SPECIAL_USE`) et `RECEIVE_BOOT_COMPLETED` : la surveillance des alarmes. `REQUEST_INSTALL_PACKAGES` : la mise à jour de l'app. `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` : la configuration d'un nœud ; `BLUETOOTH`, `BLUETOOTH_ADMIN` et `ACCESS_FINE_LOCATION` seulement jusqu'à Android 11 (`maxSdkVersion=30`), pour la recherche Bluetooth.

Il n'y a pas eu de revue de sécurité indépendante. Lisez [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) et [SECURITY.md](SECURITY.md).

## 🔌 Interfaces

| Partie | Ce qu'elle règle |
|---|---|
| ARMOR-SERVER | 55 chemins distincts de `/api/v1` dans `network/ArmorApiClient.kt` (l'interface est `network/ArmorApi.kt`) ; le radar est lu toutes les 1,5 s ; les caméras en MJPEG via le serveur |
| Deux adresses | La privée sur le Wi-Fi de la maison et celle pour l'extérieur (données mobiles ou VPN), choisies selon l'endroit où se trouve le téléphone ; si l'une est muette l'autre est essayée et le message dit ce qui échoue |
| Bluetooth | Trouve les nœuds qui annoncent `ARMOR-xxxxxx`, crée l'administrateur d'un nouveau ou se connecte avec le PIN du nœud, cherche les réseaux Wi-Fi et règle le nom, le Wi-Fi ou une adresse fixe et le broker ; le protocole est `model/NodeBle.kt` ; un chercheur de nœuds liste ceux déjà sur le réseau |
| Voix | `POST /api/v1/voice/command` : une liste fermée de commandes en huit langues, avec une confirmation liée à vous (`model/Voice.kt`, `VoiceScreens.kt`) |
| Push | `PushService` : alarmes en messages de données seules, annoncées une fois qu'elles arrivent par push ou par la surveillance ; les étapes Firebase sont dans [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) (huit langues) |

## ⚙️ Configuration

| Nom | Valeur | Ce qu'elle règle |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | Android 8.0 ou plus récent sur le téléphone |
| `versionName` | = `armor.project.json` | Le build échoue si le manifeste et la version diffèrent |
| JDK / Android SDK | 17+ / 37 | Pour compiler l'app et lancer les tests |

Dans l'app (pas dans des fichiers) : la langue (les huit ou suivre le téléphone), les deux adresses, *Se souvenir de moi*, le verrouillage du téléphone, l'alarme parlée et les notifications, dans *Plus > Réglages*. Limites dans le code : la surveillance interroge toutes les 10 s, s'espace jusqu'à 60 s et ignore les alarmes de plus de 15 minutes.

## 🚀 Démarrage rapide

**Prérequis:** JDK 17+, le SDK Android (compile SDK 37), un téléphone Android 8.0+ (minSdk 26).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 367 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Les versions sont publiées sur GitHub, d'où l'application cherche elle-même les mises à jour ; l'APK est signé avec la clé de publication propre du responsable, conservée hors du dépôt, et l'application compare le certificat de signature d'un téléchargement avec le sien avant de passer le fichier à l'installateur d'Android. Voir la [frontière du client](docs/CLIENT_BOUNDARY.md).

## 🧪 Tests

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| Partie | Tests | Ce qu'il couvre |
|---|---|---|
| Alarmes | 67 | `AlarmPolicy`, `AlarmBanner`, `AlarmPanels`, `AlarmCodes`, `AlarmRoute`, `NoticeFilter`, `PushEvents`, `DevicesAndAlarms` |
| Lien avec le serveur, analyse, données de test | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| Modèle de vue et rôles | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| Verrouillage, coffre de session, clés | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| Sécurité des adresses, certificats | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| Énergie et réseau | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| Nœuds par Bluetooth, contrôle des nœuds | 26 | `NodeBle`, `NodeControl` |
| Mises à jour depuis GitHub | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| Radar et caméras | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| Traductions, mots simples, voix | 14 | `Tr`, `Friendly`, `Voice` |

367 tests dans 47 fichiers, lancés avec `testDebugUnitTest` ; un faux serveur (`FakeServer.kt`) et des données prises sur un vrai serveur (`tools/capture_fixtures.mjs`) les alimentent. On vérifie que chaque phrase figure dans les huit tables de langue.

## 🏗️ Compilation et publication

Les versions sont publiées sur GitHub, d'où l'app vérifie elle-même les mises à jour ; l'app cherche l'élément `ARMOR-ANDROID-CONTROL-release.apk`. L'APK est signé avec la clé de version propre au mainteneur, gardée hors du dépôt, et l'app compare le certificat de signature d'un téléchargement au sien avant de passer le fichier à l'installateur d'Android. Le `versionCode` monte avant chaque APK, sinon Android ne le traite pas comme une mise à jour.

## 🩺 Dépannage

| Vous voyez | Parce que | Faites |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | Le serveur refuse le PIN de désarmement ou demande une preuve d'identité récente | Saisissez de nouveau le PIN, attendez la fin du blocage ou confirmez votre identité |
| La connexion dit qu'elle ne peut pas continuer | L'adresse est hors de la maison et n'a pas de https (le http en clair est refusé) | Suivez le guide : un VPN, ou une adresse publique avec https |
| La connexion est refusée alors qu'elle marchait | La clé du certificat de l'installation a changé | Appariez de nouveau avec le texte du QR de Studio si vous l'avez changé exprès |
| Les alarmes arrivent en retard ou pas du tout en arrière-plan | Les règles de batterie du fabricant arrêtent la surveillance ; le push exige un projet Firebase propre | Exemptez l'app des économies de batterie ; suivez [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| La mise à jour est refusée | Le certificat de signature du téléchargement n'en partage aucun avec l'app installée | Ne l'installez pas ; prenez l'APK dans les versions de ce dépôt |

## 📂 Structure du dépôt

<p align="center">
  <img src="images/repository-structure.svg" alt="Structure du dépôt de ARMOR-ANDROID-CONTROL" width="100%">
</p>

<details>
<summary>Version texte de l'arborescence</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   l'app : écrans, règles, réseau et mises à jour
├── app/src/main/assets/                         huit tables de langue, codes d'alarme, carte météo
├── app/src/test/                                367 tests et un faux serveur
├── docs/                                        les guides de ce dépôt
├── tools/                                       les contrôles CI partagés et les outils du projet
├── .github/                                     workflow CI et modèles d'issues et de pull requests
├── images/                                      bannière, icône et les deux images de ce README
├── build.sh build.bat                           build par le flux de projet partagé
├── test.sh test.bat                             lancer les tests par le flux partagé
├── run.sh run.bat                               démarrer par le flux de projet partagé
├── armor.project.json                           manifeste : version, rôle, maturité, commande de build
├── CHANGELOG.md                                 ce qui a changé à chaque version
├── SECURITY.md SUPPORT.md LICENSE               politique de sécurité, support et licence
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           comment contribuer et le code de conduite
├── build.gradle.kts settings.gradle.kts         le build Gradle du projet
├── gradle.properties                            réglages Gradle
├── gradlew gradlew.bat gradle/                  l'enveloppe Gradle
├── build-test.sh build-test.bat                 lancer les tests par le flux partagé
└── .gitattributes .gitignore                    réglages Git
```

</details>

## 🗺️ Feuille de route et limites connues

**Limites connues aujourd'hui**

* Les écrans solaire, électrique et centrale d'alarme n'ont pas connu de vrai matériel (seul un nœud radar existe).
* La configuration Bluetooth d'un nœud n'a pas été confirmée avec un vrai nœud, et le push via Firebase n'a pas été essayé sur un téléphone.
* La surveillance des alarmes en arrière-plan sous les règles de batterie de chaque fabricant n'est pas prouvée.
* Pas de revue de sécurité indépendante.

**Ensuite**

* Essayer les écrans solaire, électrique et centrale d'alarme avec de vrais nœuds, et la configuration Bluetooth avec un vrai nœud.
* Essayer le push via Firebase avec le projet propre d'une installation, et la surveillance des alarmes chez plusieurs fabricants.

## 🌍 Langues

Le README et les textes existent en anglais, espagnol, français, italien, allemand, chinois, japonais et portugais européen.

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

## 📚 Documentation

* [Limites du client](docs/CLIENT_BOUNDARY.md) : ce que le client fait et ne fait jamais
* [Notifications push](docs/PUSH_NOTIFICATIONS.md) : les étapes Firebase, en huit langues
* [Essais de banc](docs/BENCH_TESTS.md) : contrôles avec un vrai téléphone
* [Matrice des capacités : ce qui est prouvé et ce qui ne l'est pas](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catalogue des projets : versions et dépendances entre les dépôts](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Historique des modifications de ce dépôt](CHANGELOG.md)

## 🤝 Contribuer et communauté

* Avant une pull request, lancez `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py` : il doit réussir. Voir [CONTRIBUTING.md](CONTRIBUTING.md) et le [code de conduite](CODE_OF_CONDUCT.md).
* Le README existe en huit langues et est généré à partir des sources d'ARMOR-DOCS : une modification du texte se fait là-bas.
* Une modification d'un message entre composants commence dans ARMOR-COMMON.
* Ne décrivez pas comme fonctionnant sur une carte ce qui a seulement été compilé.
* Signalez une vulnérabilité en privé : [SECURITY.md](SECURITY.md). Questions, idées et rapports : electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTEUR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCE

GPL-3.0-or-later - voir [LICENSE](LICENSE).
