<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="Banner de ARMOR-ANDROID-CONTROL" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  🇪🇸 <b>Español</b> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### Cliente móvil de operador para ARMOR-SERVER: alarmas, cámaras, radar, energía, la red y la configuración de los nodos, en ocho idiomas

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 o posterior">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Lenguaje">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Idiomas">
  <img src="https://img.shields.io/badge/Tests-376%20passing-2ea44f.svg" alt="Pruebas">
  <img src="https://img.shields.io/badge/Version-0.7.8-0f6cbd.svg" alt="Versión">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Madurez">
</p>

---

**Comprobación de honestidad - qué funciona hoy:** Las reglas de seguridad de la dirección, de decisión de alarmas, del vigilante de alarmas, de traducción y del rol de solo lectura están cubiertas por 376 tests unitarios y la app compila (`gradlew testDebugUnitTest assembleDebug`). Funciona en un móvil real contra el banco de pruebas de la CM5: el acceso, el monitor de cámaras, el armado y las alarmas, el radar, la configuración de un nodo por Bluetooth y las actualizaciones desde GitHub están en uso. Lo que **todavía no** está probado: las pantallas solar, eléctrica y de centrales de alarma con equipos reales (solo existe un nodo radar), el push por Firebase (está en la app y necesita un proyecto Firebase propio; aún no se ha probado en un móvil) y la vigilancia de alarmas en segundo plano bajo las reglas de batería de cada fabricante.

---

## 📋 De un vistazo

| | |
|---|---|
| Versión | `0.7.8` |
| Madurez | Establecido |
| Función | Cliente de operador Android para ARMOR-SERVER (familia: Clients, padre: ARMOR-SERVER) |
| Tecnología | Kotlin / Jetpack Compose, Android 8.0+ |
| Pruebas | 376 (`gradlew testDebugUnitTest`) |
| Idiomas | 8 (1345 frases por idioma, cada frase comprobada en todas las tablas) |
| Funciona en | Un móvil con Android 8.0+ (minSdk 26) |
| En hardware real | Usada en un móvil real contra el banco de pruebas de la CM5; las pantallas solar, eléctrica y de centrales de alarma esperan nodos reales |
| Licencia | GPL-3.0-or-later |

**Ir a:** [Resumen](#-resumen) · [Arquitectura](#-arquitectura) · [Seguridad](#-seguridad-y-protección) · [Interfaces](#-interfaces) · [Configuración](#-configuración) · [Inicio rápido](#-inicio-rápido) · [Pruebas](#-pruebas) · [Problemas](#-resolución-de-problemas) · [Estructura](#-estructura-del-repositorio) · [Relacionados](#-proyectos-relacionados)

## 🚦 Estado del proyecto

Cada línea es una parte de este repositorio y la luz dice hasta dónde llega hoy. Está escrito con lo que el repositorio puede demostrar (pruebas, compilaciones, ejecuciones), no con lo que está planeado.

🟢 Hecho y verificado · 🟡 Funciona, con límites conocidos · 🟠 Parcial o sin verificar todavía · 🔴 No hecho o no funciona

| Área | Estado | Qué significa aquí |
|---|---|---|
| Software | 🟢 | La app compila (`gradlew testDebugUnitTest assembleDebug`) y funciona en un móvil real contra el banco de pruebas CM5. |
| Pruebas | 🟢 | Pasan 376 pruebas unitarias (`gradlew testDebugUnitTest`): seguridad de las direcciones, decisión de alarma, vigilante de alarmas, traducciones y rol de solo lectura. |
| Interfaz | 🟡 | El inicio de sesión, el monitor de cámaras, el armado y las alarmas y el radar se usan en un móvil real; la configuración de un nodo por Bluetooth no se ha confirmado con un nodo real, y las pantallas solar, eléctrica y de panel de alarma no han visto equipos reales. |
| Hardware | 🟡 | Se usa en un móvil real contra el banco de pruebas CM5; la vigilancia de alarmas en segundo plano bajo las reglas de batería de cada fabricante no está probada. |
| Seguridad | 🟡 | Las reglas de seguridad de direcciones y de rol de solo lectura están cubiertas por pruebas unitarias; no ha habido una revisión independiente. |
| Actualizaciones | 🟡 | Las actualizaciones desde GitHub se usan en el móvil. |
| Integración | 🟠 | El push mediante Firebase está en la app pero necesita un proyecto Firebase propio y no se ha probado en un móvil. |
| Documentación | 🟢 | Ocho idiomas y el historial de cambios están al día. |

## 🎯 Resumen

* **Acceso con el login del propio servidor:** IP, puerto, usuario y contraseña; la contraseña crea una sesión HttpOnly; solo se guarda en el móvil si marcas **Recordarme** (apagado por defecto: usuario y contraseña sellados con una clave propia del Keystore, solo para ese servidor, y se olvidan en cuanto la desmarcas). Un rol **viewer** solo mira: todo botón que cambia algo está apagado.
* **Bloqueo con el teléfono:** la app pide la huella, el rostro o el PIN del propio teléfono antes de armar, desarmar, silenciar una sirena, desbloquear y las demás órdenes críticas (activado por defecto solo si el teléfono tiene huella o rostro) y, si lo activas, para abrir la app. Tu administrador puede exigirlo desde Studio pero nunca bajar lo que elegiste, un teléfono sin bloqueo de pantalla nunca se queda fuera y la pantalla bloqueada siempre ofrece entrar de nuevo con la contraseña. Opcionalmente (desactivado por defecto, con un aviso visible) la clave que sella la sesión guardada puede atarse al desbloqueo del teléfono; entonces la vigilancia de alarmas en segundo plano espera a que desbloquees el teléfono tras un reinicio.
* **Monitor de cámaras:** de 1 a 16 teselas, una vista ampliada, MJPEG en directo que conserva la proporción, un mando PTZ acotado, capturas y grabaciones, con visor y confirmación de borrado de las grabaciones.
* **Armar y desarmar,** tras una **comprobación antes de armar** (qué está abierto, mudo o caído) y con el PIN para desarmar; **alarmas** que reconocer, con una insignia para las que esperan; **dispositivos** (humo, gas, inundación, puerta, ventana, movimiento, clima, enchufes, luces, sirenas, cerraduras, persianas, motores de cancela y garaje, calentadores) con su estado y Encender / Apagar / Alternar.
* **Alarmas que te alcanzan:** un sonido para cada gravedad, una **alarma hablada** opcional, avisos de un nodo que llega a ALTA, una cámara muda, una alarma de dispositivo y (armado) un nodo desconectado, y **una central que suena despierta el móvil**; la vigilancia en segundo plano sobrevive a un reinicio y cede cuando el servidor está caído.
* **Radar en vivo, en 2D y 3D:** la pestaña Radar dibuja el sitio tal como se diseñó en Studio y las personas que ven los radares moviéndose sobre él, refrescado cada segundo y medio; arrastra, pellizca y gira la vista 3D.
* **Energía solar y eléctrica:** los totales (sol, consumo, baterías con su carga, si hay red), cada inversor y batería con las celdas a demanda, y los canales y alarmas de los nodos eléctricos.
* **Centrales de alarma:** el estado de cada nodo ARMOR-NODE-ALARM, sus zonas y eventos, y comandos de armar / desarmar cuando el servidor los permite.
* **Red:** los dispositivos de la red de la casa, el estado de internet y la **dirección pública** con el proveedor y el lugar, como los informa el nodo de red.
* **Configurar un nodo de campo por Bluetooth** (desde la pantalla de acceso o *Más > Configurar un nodo*): encuentra los nodos que anuncian `ARMOR-xxxxxx`, crea el administrador de uno nuevo o entra (con el PIN del nodo), busca redes Wi-Fi y fija el nombre del nodo, el Wi-Fi de un router o una dirección fija y el broker; un **buscador de nodos** lista los que ya están en la red.
* **El asistente:** comandos escritos y hablados (lista cerrada, en ocho idiomas, con una confirmación ligada a ti) para preguntar el estado de la casa o armar, desarmar, silenciar y encender o apagar luces.
* **Biblioteca de evidencias,** estado del perímetro y de los nodos, un **historial** de cada alerta, nodo, cámara, dispositivo, alarma y cambio de modo, el **mapa del tiempo**, y los **servicios** y actualizaciones de la instalación.
* **Ocho idiomas** (inglés, español, alemán, francés, italiano, japonés, chino y portugués), elegidos en *Más > Ajustes* o siguiendo al móvil; se comprueba que cada frase está en las ocho tablas.
* **Cuidado con la contraseña:** HTTP plano solo se permite a un *literal* IPv4 de LAN privada o loopback; se rechaza un nombre de host que solo empieza como una dirección privada (`10.attacker.example`) o una dirección con cero a la izquierda. El aspecto está hecho de iconos: superficies casi negras, acento cian, ámbar para la atención.
* **Seguridad desde el móvil:** el segundo factor y sus códigos de recuperación, las sesiones abiertas, una comprobación de cómo ve el servidor al móvil, la nueva prueba de identidad para acciones críticas desde fuera y, para un administrador, la VPN (dispositivos con su configuración) y el nombre de DNS dinámico.
* **Todo el sistema en la pantalla de estado y la entrada desde fuera:** la pantalla de estado resume las centrales de alarma, los servicios, las cámaras que graban y la seguridad y el acceso remoto, y cada tarjeta abre su pantalla; y cuando la dirección escrita está fuera de casa y sin https, el inicio de sesión dice por qué no puede seguir y una guía explica las dos vías seguras (VPN, o dirección pública con https).
* **Salud de los nodos y modos de armado:** una pantalla Salud de los nodos en Más (tiempo encendido, motivo del reinicio, memoria, enlace y señal, conexiones al broker, reinicios, último error y los avisos del servidor de cada nodo) y, en la pantalla de Centrales de alarma, un selector de modo (Fuera, En casa, Noche y los tuyos) que arma una central en el modo elegido cuando su firmware los conoce. Ocho idiomas.
* **Conexiones en el historial:** la pantalla Historial tiene una segunda lista, solo de lectura, con los dispositivos y apps que se conectan al servidor (Studio, esta app, los paneles de los nodos, el servicio de voz), cuándo se vio cada uno por última vez, sus sesiones abiertas y sus inicios de sesión y rechazos; la app ahora le dice al servidor qué es para aparecer como ella misma. Las direcciones solo las ven los administradores.
* **Mantenimiento de baterías (administradores):** las operaciones sencillas en una batería Pylontech - reloj, contadores y registros, reinicio, capacidad, prueba de MOSFET, fallo de fusible, salidas - en los mismos dos pasos que Studio (el servidor hace que el nodo lea la batería, la pantalla muestra exactamente lo que se hará y luego "Hacerlo ahora" con el número de serie escrito otra vez), con el resultado y lo que cambió; los límites, el firmware y la consola de experto se quedan en Studio.
* **Control de los nodos (administradores):** Más > Control de nodos da las mismas operaciones que Studio - reiniciar, pausar, reanudar, poner en espera (trabajo apagado, sigue conectado), identificar, diagnóstico y mantenimiento activado o desactivado - a un nodo o a todos, pidiendo que se escriba de nuevo el nombre del nodo (o la palabra RESTART, PAUSE o STANDBY para todos) y la prueba de identidad nueva, mostrando hasta dónde llegó cada nodo y avisando de que una central de alarma o un radar en pausa o en espera dejan la casa sin proteger. No existe restablecimiento de fábrica, borrado ni sueño profundo.
* **Una ventana de alarma en la app abierta:** mientras una alarma espera a una persona, un aviso sobre todas las pantallas menos la de alarmas late (rojo y rápido si es crítica, ámbar en las demás, quieto si el móvil tiene las animaciones apagadas), dice cuál es la peor alarma y cuántas esperan, y deja usar la app por debajo; puede abrir las alarmas, confirmar desde el aviso o cerrarse hasta que llegue otra alarma.
* **Desde fuera de casa:** dos direcciones (la privada en la wifi de casa, la de fuera con datos móviles o la VPN) elegidas según dónde esté el teléfono, probando la otra si una no responde y con un mensaje que dice qué falla; la clave del certificado de la instalación se recuerda la primera vez (o se empareja con el texto del QR de Studio) y una distinta se rechaza, y se rechaza el http plano a un servidor al que ya se llegó por https; el panel de un nodo puede abrirse a través del servidor; las alarmas pueden llegar como push solo con datos, anunciadas una vez lleguen por push o por la vigilancia; la app abierta y la vigilancia comparten una lectura del servidor; las cámaras se paran fuera de pantalla y vuelven con esperas crecientes.
* **Avisos a tu medida, con sonidos y una voz propios:** en los ajustes, silenciar todos los avisos, mostrarlos sin sonido ni vibración (y sin decirlos en voz alta) y desactivarlos por gravedad (alta, baja) o por tipo (alertas del radar, cámaras que no responden, nodos desconectados o callados, y las alarmas de dispositivos, solar, eléctrico, red y paneles de alarma); una alarma crítica siempre avisa salvo que se silencien todos. Un sonido para cada gravedad y cada tipo de aviso, de los sonidos del móvil o de un fichero propio (menos de 3 MB), cada uno con un botón de reproducir; y la voz que dice las alarmas (mujer, hombre o cualquiera; niño, adulto o mayor; normal, héroe de acción o señor oscuro; velocidad; y si también se dicen los avisos de gravedad baja) con un botón de prueba.
* **Un banner que te lleva a la pantalla correcta y una vigilancia más tranquila:** el banner de una alarma abre la pantalla que la atiende (cámaras, dispositivos, inversores o baterías, la pantalla eléctrica, la red o los paneles de alarma); el móvil dice su idioma al registrarse para push y el servidor escribe el aviso en él, y un token nuevo de Firebase llega al servidor enseguida desde segundo plano. Una cámara con dos lentes en una sola transmisión son dos ventanas de la lista de cámaras. Mientras los avisos push funcionan, la vigilancia en segundo plano consulta al servidor cada 120 s en lugar de cada 10 (*Ajustes > Vigilancia con avisos push*: 10, 60, 120, 300 o 600 s).
* **Modos de armado del servidor (Fuera, En casa, Noche):** mientras el servidor tiene los modos de armado activados (Studio, Configuración > Seguridad), el botón de armar de la pantalla de estado abre un selector para armar en un modo, cambiar de uno a otro o desarmar; un modo sin nada asignado pide confirmación, un cambio a un modo que defiende menos pide el PIN como desarmar, y el estado muestra el modo en vigor. Con los modos apagados la app arma y desarma como antes.

## 🧩 Papel en A.R.M.O.R.

ARMOR-ANDROID-CONTROL es la ventana del móvil al mismo [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) que usa [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO): lee y ordena a través de él y de nada más. Es donde las alarmas te siguen (una central que suena despierta el móvil), donde preguntas al asistente por voz, y la herramienta para configurar un nodo nuevo por Bluetooth sobre el terreno, a su lado.

| Nombre | Cómo | Para qué |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | Todo lo que la app lee y ordena |
| Nodos de campo | Bluetooth (`ARMOR-xxxxxx`) | Configura un nodo nuevo estando a su lado |
| GitHub | HTTPS | Comprueba y descarga la actualización firmada de la propia app |
| Open-Meteo | HTTPS | La previsión, la calidad del aire y la búsqueda de lugares públicas del mapa del tiempo |

## 🏛️ Arquitectura

<p align="center">
  <img src="images/architecture.svg" alt="Arquitectura de ARMOR-ANDROID-CONTROL: quién lo llama, qué contiene y de qué depende" width="100%">
</p>

## 🔒 Seguridad y protección

| Amenaza | Defensa |
|---|---|
| Una contraseña guardada en el móvil | Solo con *Recordarme* (apagado por defecto): usuario y contraseña sellados con una clave propia del Keystore, solo para ese servidor, olvidados al desmarcar; sin copia de seguridad (`allowBackup=false`) |
| HTTP plano hacia internet | Solo se permite a un *literal* IPv4 de red privada o loopback; un host como `10.attacker.example` o una dirección con cero inicial (`010.0.0.1` se lee octal) se rechaza; el http plano se rechaza para un host al que se llegó por https |
| Un intermediario en https | La confianza son las autoridades del sistema más las que instalaste en el móvil; la clave del certificado de la instalación se recuerda la primera vez (o se empareja con el texto del QR de Studio) y una distinta se rechaza |
| Una orden crítica con el móvil en otras manos | La huella, la cara o el PIN del móvil antes de armar, desarmar, silenciar una sirena, desbloquear y otras órdenes críticas (activo por defecto solo si el móvil tiene huella o cara); el administrador puede exigirlo desde Studio pero nunca bajar lo que elegiste; un móvil sin bloqueo nunca se queda fuera |
| Un visor cambia algo | El rol *visor* lo ve todo pero todos los botones que cambian algo están apagados (pruebas de rol) |
| Un PIN de desarmado incorrecto | El PIN solo se manda al servidor al desarmar; `wrong_pin`, `pin_locked`, `pin_not_set` y `elevation_required` se traducen a texto llano |
| Una falsa actualización de la app | Se comparan los SHA-256 de los certificados de firma (historial) de la app instalada y del candidato antes de llamar al instalador de Android; si no comparten ninguno, se dice claramente |
| Una alarma que el móvil no ve | Un servicio en primer plano (`AlarmWatcherService`) sondea cada 10 s, espera 15 s a que el móvil se desbloquee, retrocede hasta 60 s, dice *inalcanzable* tras 6 fallos y sobrevive al reinicio; una alarma de más de 15 minutos no se anuncia |
| Una contraseña de cámara en el móvil | Las cámaras se ven por el flujo del servidor: la app no tiene contraseña de cámara ni dirección RTSP |

### Permisos

`INTERNET`, `ACCESS_NETWORK_STATE`: el servidor. `USE_BIOMETRIC`: el bloqueo del móvil. `POST_NOTIFICATIONS`: las alarmas. `FOREGROUND_SERVICE` (+ `DATA_SYNC`, `SPECIAL_USE`) y `RECEIVE_BOOT_COMPLETED`: la vigilancia de alarmas. `REQUEST_INSTALL_PACKAGES`: la actualización de la app. `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`: la configuración de un nodo; `BLUETOOTH`, `BLUETOOTH_ADMIN` y `ACCESS_FINE_LOCATION` solo hasta Android 11 (`maxSdkVersion=30`), para el escaneo Bluetooth.

No hay revisión de seguridad independiente. Lee [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) y [SECURITY.md](SECURITY.md).

## 🔌 Interfaces

| Parte | Qué define |
|---|---|
| ARMOR-SERVER | 55 rutas distintas de `/api/v1` en `network/ArmorApiClient.kt` (la interfaz es `network/ArmorApi.kt`); el radar se lee cada 1,5 s; las cámaras por MJPEG a través del servidor |
| Dos direcciones | La privada en el Wi-Fi de casa y la de fuera (datos móviles o la VPN), elegidas según dónde esté el móvil; si una no responde se prueba la otra y el mensaje dice qué falla |
| Bluetooth | Encuentra los nodos que anuncian `ARMOR-xxxxxx`, crea el administrador de uno nuevo o entra con el PIN del nodo, busca redes Wi-Fi y define el nombre, el Wi-Fi o una dirección fija y el broker; el protocolo es `model/NodeBle.kt`; un buscador de nodos lista los que ya están en la red |
| Voz | `POST /api/v1/voice/command`: una lista cerrada de órdenes en ocho idiomas, con una confirmación atada a ti (`model/Voice.kt`, `VoiceScreens.kt`) |
| Push | `PushService`: alarmas como mensajes solo con datos, anunciadas una vez lleguen por push o por la vigilancia; los pasos de Firebase están en [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) (ocho idiomas) |

## ⚙️ Configuración

| Nombre | Valor | Qué define |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | Android 8.0 o posterior en el móvil |
| `versionName` | = `armor.project.json` | La compilación falla si el manifiesto y la versión difieren |
| JDK / Android SDK | 17+ / 37 | Para compilar la app y ejecutar las pruebas |

En la app (no en ficheros): el idioma (los ocho o seguir al móvil), las dos direcciones, *Recordarme*, el bloqueo del móvil, la alarma hablada y las notificaciones, en *Más > Ajustes*. Límites en el código: la vigilancia sondea cada 10 s, retrocede hasta 60 s e ignora alarmas de más de 15 minutos.

## 🚀 Inicio rápido

**Requisitos:** JDK 17+, el SDK de Android (compile SDK 37), un móvil con Android 8.0+ (minSdk 26).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 376 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Las versiones se publican en GitHub, desde donde la propia app busca actualizaciones; el APK se firma con la clave de publicación propia del responsable, guardada fuera del repositorio, y la app compara el certificado de firma de una descarga con el suyo antes de pasar el archivo al instalador de Android. Véase el [límite del cliente](docs/CLIENT_BOUNDARY.md).

## 🧪 Pruebas

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| Parte | Pruebas | Qué cubre |
|---|---|---|
| Alarmas | 67 | `AlarmPolicy`, `AlarmBanner`, `AlarmPanels`, `AlarmCodes`, `AlarmRoute`, `NoticeFilter`, `PushEvents`, `DevicesAndAlarms` |
| Enlace con el servidor, lectura, datos de prueba | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| Modelo de vista y roles | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| Bloqueo, almacén de sesión, claves | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| Seguridad de direcciones, certificados | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| Energía y red | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| Nodos por Bluetooth, control de nodos | 26 | `NodeBle`, `NodeControl` |
| Actualizaciones desde GitHub | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| Radar y cámaras | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| Traducciones, palabras sencillas, voz | 14 | `Tr`, `Friendly`, `Voice` |

376 pruebas en 47 ficheros, con `testDebugUnitTest`; un servidor falso (`FakeServer.kt`) y datos tomados de un servidor real (`tools/capture_fixtures.mjs`) las alimentan. Se comprueba que cada frase esté en las ocho tablas de idioma.

## 🏗️ Compilación y publicación

Las versiones se publican en GitHub, desde donde la propia app comprueba las actualizaciones; la app busca el recurso `ARMOR-ANDROID-CONTROL-release.apk`. El APK se firma con la clave de versión del mantenedor, guardada fuera del repositorio, y la app compara el certificado de firma de una descarga con el suyo antes de pasar el fichero al instalador de Android. El `versionCode` sube antes de cada APK, o Android no lo trata como actualización.

## 🩺 Resolución de problemas

| Ves | Porque | Haz |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | El servidor rechaza el PIN de desarmado o pide una prueba de identidad nueva | Escribe el PIN de nuevo, espera a que acabe el bloqueo o confirma tu identidad |
| El inicio de sesión dice que no puede continuar | La dirección está fuera de casa y no tiene https (el http plano se rechaza) | Sigue la guía: una VPN, o una dirección pública con https |
| La conexión se rechaza tras haber funcionado | Cambió la clave del certificado de la instalación | Vuelve a emparejar con el texto del QR de Studio si lo cambiaste a propósito |
| Las alarmas llegan tarde o no llegan en segundo plano | Las reglas de batería del fabricante detienen la vigilancia; el push necesita un proyecto Firebase propio | Excluye la app del ahorro de batería; sigue [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| La actualización se rechaza | El certificado de firma de la descarga no comparte ninguno con la app instalada | No lo instales; toma el APK de las versiones de este repositorio |

## 📂 Estructura del repositorio

<p align="center">
  <img src="images/repository-structure.svg" alt="Estructura del repositorio de ARMOR-ANDROID-CONTROL" width="100%">
</p>

<details>
<summary>Versión en texto del árbol</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   la app: pantallas, reglas, red y actualizaciones
├── app/src/main/assets/                         ocho tablas de idioma, códigos de alarma, mapa del tiempo
├── app/src/test/                                376 pruebas y un servidor falso
├── docs/                                        las guías de este repositorio
├── tools/                                       las comprobaciones de CI compartidas y las herramientas del proyecto
├── .github/                                     flujo de CI y plantillas de issues y pull requests
├── images/                                      banner, icono y las dos imágenes de este README
├── build.sh build.bat                           compilar con el flujo de proyecto compartido
├── test.sh test.bat                             ejecutar las pruebas con el flujo compartido
├── run.sh run.bat                               arrancar con el flujo de proyecto compartido
├── armor.project.json                           manifiesto: versión, rol, madurez, comando de build
├── CHANGELOG.md                                 qué cambió en cada versión
├── SECURITY.md SUPPORT.md LICENSE               política de seguridad, soporte y licencia
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           cómo contribuir y el código de conducta
├── build.gradle.kts settings.gradle.kts         la compilación Gradle del proyecto
├── gradle.properties                            ajustes de Gradle
├── gradlew gradlew.bat gradle/                  el envoltorio de Gradle
├── build-test.sh build-test.bat                 ejecutar las pruebas con el flujo compartido
└── .gitattributes .gitignore                    ajustes de Git
```

</details>

## 🗺️ Hoja de ruta y límites conocidos

**Límites conocidos hoy**

* Las pantallas de solar, eléctrico y panel de alarma no han conocido equipo real (solo existe un nodo radar).
* La configuración Bluetooth de un nodo no se ha confirmado con un nodo real, y el push de Firebase no se ha probado en un móvil.
* La vigilancia de alarmas en segundo plano bajo las reglas de batería de cada fabricante no está probada.
* No hay revisión de seguridad independiente.

**Lo siguiente**

* Probar las pantallas de solar, eléctrico y panel de alarma con nodos reales, y la configuración Bluetooth con un nodo real.
* Probar el push de Firebase con un proyecto propio de una instalación, y la vigilancia de alarmas en varios fabricantes.

## 🌍 Idiomas

El README y los textos existen en inglés, español, francés, italiano, alemán, chino, japonés y portugués de Portugal.

## 🔗 Proyectos relacionados

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) es un sistema de seguridad perimetral hecho de repositorios independientes. Cada uno tiene su propia versión, sus propias pruebas y su propio README; esta es la familia:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratos de mensajes, validadores, vectores de conformidad y tipos generados
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Firmware de nodo de campo para ESP32-S3 con tres radares y su propio panel web
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protocolos de inversores y baterías solares y los mensajes de un nodo pasarela
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Nodo eléctrico: medidores, el mensaje de las lecturas de la red y las reglas de maniobra
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Nodo y panel de alarma: zonas, armado, retardos, sirena y PIN, con el servidor o sin él
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Panel táctil: el estado del sistema en una pantalla de pared, armar y reconocer, y el hogar del asistente de voz
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - La red local: sus dispositivos, internet y lo que cambia
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinador central: telemetría, alarmas, dispositivos, lecturas solares y cámaras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Consola web: cámaras, radar, alarmas, energía solar y el diseñador 2D/3D del emplazamiento
* **ARMOR-ANDROID-CONTROL** (este repositorio) - Cliente de operador Android con radar 2D/3D en vivo
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - Cliente de operador para iPhone
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Política de inferencia visual que explica sus decisiones y nunca actúa
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zonas de cámara, máscaras y reglas de detección para la Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenciones de voz sin conexión con una confirmación que no se puede falsificar
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Cajas, electrónica y la matriz de aceptación en banco
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Despliegue, el banco de pruebas de la CM5, copias de seguridad y TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulador de telemetría sin conexión con fallos repetibles
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Detecta, instala y actualiza los repositorios del propio ecosistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Arquitectura, base de seguridad y la matriz de capacidades

## 📚 Documentación

* [Límites del cliente](docs/CLIENT_BOUNDARY.md): qué hace y qué nunca hace el cliente
* [Notificaciones push](docs/PUSH_NOTIFICATIONS.md): los pasos de Firebase, en ocho idiomas
* [Pruebas de banco](docs/BENCH_TESTS.md): comprobaciones con un móvil real
* [Matriz de capacidades: qué está demostrado y qué no](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catálogo de proyectos: versiones y cómo dependen unos repositorios de otros](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Registro de cambios de este repositorio](CHANGELOG.md)

## 🤝 Contribuir y comunidad

* Antes de un pull request ejecuta `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py`: debe pasar. Consulta [CONTRIBUTING.md](CONTRIBUTING.md) y el [código de conducta](CODE_OF_CONDUCT.md).
* El README existe en ocho idiomas y se genera desde las fuentes de ARMOR-DOCS: un cambio de texto se hace allí.
* Un cambio en un mensaje entre componentes empieza en ARMOR-COMMON.
* No describas como funcionando en una placa lo que solo se ha compilado.
* Informa de una vulnerabilidad en privado: [SECURITY.md](SECURITY.md). Preguntas, ideas e informes: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCIA

GPL-3.0-or-later - véase [LICENSE](LICENSE).
