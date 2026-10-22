<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
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
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-228%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.4-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Comprobación de honestidad - qué funciona hoy:** Las reglas de seguridad de la dirección, de decisión de alarmas, del vigilante de alarmas, de traducción y del rol de solo lectura están cubiertas por 257 tests unitarios y la app compila (`gradlew testDebugUnitTest assembleDebug`). Funciona en un móvil real contra el banco de pruebas de la CM5: el acceso, el monitor de cámaras, el armado y las alarmas, el radar, la configuración de un nodo por Bluetooth y las actualizaciones desde GitHub están en uso. Lo que **todavía no** está probado: las pantallas solar, eléctrica y de centrales de alarma con equipos reales (solo existe un nodo radar), el push por Firebase (está en la app y necesita un proyecto Firebase propio; aún no se ha probado en un móvil) y la vigilancia de alarmas en segundo plano bajo las reglas de batería de cada fabricante.

---

## 📋 De un vistazo

| | |
|---|---|
| Versión | `0.7.4` |
| Madurez | Establecido |
| Tests | 257 (`gradlew testDebugUnitTest`) |
| Tecnología | Kotlin / Jetpack Compose |
| En hardware real | Usada en un móvil real contra el banco de pruebas de la CM5; las pantallas solar, eléctrica y de centrales de alarma esperan nodos reales |

## 🎯 Descripción general

* **Acceso con el login del propio servidor:** IP, puerto, usuario y contraseña; la contraseña crea una sesión HttpOnly; solo se guarda en el móvil si marcas **Recordarme** (apagado por defecto: usuario y contraseña sellados con una clave propia del Keystore, solo para ese servidor, y se olvidan en cuanto la desmarcas). Un rol **viewer** solo mira: todo botón que cambia algo está apagado.
* **Monitor de cámaras:** de 1 a 16 teselas, una vista ampliada, MJPEG en directo que conserva la proporción, un mando PTZ acotado, capturas y grabaciones, con visor y confirmación de borrado de las grabaciones.
* **Armar y desarmar,** tras una **comprobación antes de armar** (qué está abierto, mudo o caído) y con el PIN para desarmar; **alarmas** que reconocer, con una insignia para las que esperan; **dispositivos** (humo, gas, inundación, puerta, ventana, movimiento, clima, enchufes, luces, sirenas, cerraduras, persianas, motores de cancela y garaje, calentadores) con su estado y Encender / Apagar / Alternar.
* **Alarmas que te alcanzan:** un sonido para cada gravedad, una **alarma hablada** opcional, avisos de un nodo que llega a ALTA, una cámara muda, una alarma de dispositivo y (armado) un nodo desconectado, y **una central que suena despierta el móvil**; la vigilancia en segundo plano sobrevive a un reinicio y cede cuando el servidor está caído.
* **Radar en vivo, en 2D y 3D:** la pestaña Radar dibuja el sitio tal como se diseñó en Studio y las personas que ven los radares moviéndose sobre él, refrescado cada segundo y medio; arrastra, pellizca y gira la vista 3D.
* **Energía solar y eléctrica:** los totales (sol, consumo, baterías con su carga, si hay red), cada inversor y batería con las celdas a demanda, y los canales y alarmas de los nodos eléctricos.
* **Centrales de alarma:** el estado de cada nodo ARMOR-ALARM, sus zonas y eventos, y comandos de armar / desarmar cuando el servidor los permite.
* **Red:** los dispositivos de la red de la casa, el estado de internet y la **dirección pública** con el proveedor y el lugar, como los informa el nodo de red.
* **Configurar un nodo de campo por Bluetooth** (desde la pantalla de acceso o *Más > Configurar un nodo*): encuentra los nodos que anuncian `ARMOR-xxxxxx`, crea el administrador de uno nuevo o entra (con el PIN del nodo), busca redes Wi-Fi y fija el nombre del nodo, el Wi-Fi de un router o una dirección fija y el broker; un **buscador de nodos** lista los que ya están en la red.
* **El asistente:** comandos escritos y hablados (lista cerrada, en ocho idiomas, con una confirmación ligada a ti) para preguntar el estado de la casa o armar, desarmar, silenciar y encender o apagar luces.
* **Biblioteca de evidencias,** estado del perímetro y de los nodos, un **historial** de cada alerta, nodo, cámara, dispositivo, alarma y cambio de modo, el **mapa del tiempo**, y los **servicios** y actualizaciones de la instalación.
* **Ocho idiomas** (inglés, español, alemán, francés, italiano, japonés, chino y portugués), elegidos en *Más > Ajustes* o siguiendo al móvil; se comprueba que cada frase está en las ocho tablas.
* **Cuidado con la contraseña:** HTTP plano solo se permite a un *literal* IPv4 de LAN privada o loopback; se rechaza un nombre de host que solo empieza como una dirección privada (`10.attacker.example`) o una dirección con cero a la izquierda. El aspecto está hecho de iconos: superficies casi negras, acento cian, ámbar para la atención.
* **Seguridad desde el móvil:** el segundo factor y sus códigos de recuperación, las sesiones abiertas, una comprobación de cómo ve el servidor al móvil, la nueva prueba de identidad para acciones críticas desde fuera y, para un administrador, la VPN (dispositivos con su configuración) y el nombre de DNS dinámico.**Todo el sistema en la pantalla de estado y la entrada desde fuera:** la pantalla de estado resume las centrales de alarma, los servicios, las cámaras que graban y la seguridad y el acceso remoto, y cada tarjeta abre su pantalla; y cuando la dirección escrita está fuera de casa y sin https, el inicio de sesión dice por qué no puede seguir y una guía explica las dos vías seguras (VPN, o dirección pública con https).
* **Salud de los nodos y modos de armado:** una pantalla Salud de los nodos en Más (tiempo encendido, motivo del reinicio, memoria, enlace y señal, conexiones al broker, reinicios, último error y los avisos del servidor de cada nodo) y, en la pantalla de Centrales de alarma, un selector de modo (Fuera, En casa, Noche y los tuyos) que arma una central en el modo elegido cuando su firmware los conoce. Ocho idiomas.
* **Conexiones en el historial:** la pantalla Historial tiene una segunda lista, solo de lectura, con los dispositivos y apps que se conectan al servidor (Studio, esta app, los paneles de los nodos, el servicio de voz), cuándo se vio cada uno por última vez, sus sesiones abiertas y sus inicios de sesión y rechazos; la app ahora le dice al servidor qué es para aparecer como ella misma. Las direcciones solo las ven los administradores.
* **Mantenimiento de baterías (administradores):** las operaciones sencillas en una batería Pylontech - reloj, contadores y registros, reinicio, capacidad, prueba de MOSFET, fallo de fusible, salidas - en los mismos dos pasos que Studio (el servidor hace que el nodo lea la batería, la pantalla muestra exactamente lo que se hará y luego "Hacerlo ahora" con el número de serie escrito otra vez), con el resultado y lo que cambió; los límites, el firmware y la consola de experto se quedan en Studio.
* **Control de los nodos (administradores):** Más > Control de nodos da las mismas operaciones que Studio - reiniciar, pausar, reanudar, poner en espera (trabajo apagado, sigue conectado), identificar, diagnóstico y mantenimiento activado o desactivado - a un nodo o a todos, pidiendo que se escriba de nuevo el nombre del nodo (o la palabra RESTART, PAUSE o STANDBY para todos) y la prueba de identidad nueva, mostrando hasta dónde llegó cada nodo y avisando de que una central de alarma o un radar en pausa o en espera dejan la casa sin proteger. No existe restablecimiento de fábrica, borrado ni sueño profundo.
* **Una ventana de alarma en la app abierta:** mientras una alarma espera a una persona, un aviso sobre todas las pantallas menos la de alarmas late (rojo y rápido si es crítica, ámbar en las demás, quieto si el móvil tiene las animaciones apagadas), dice cuál es la peor alarma y cuántas esperan, y deja usar la app por debajo; puede abrir las alarmas, confirmar desde el aviso o cerrarse hasta que llegue otra alarma.

## 🧩 Papel en A.R.M.O.R.

ARMOR-ANDROID-CONTROL es la ventana del móvil al mismo [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) que usa [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO): lee y ordena a través de él y de nada más. Es donde las alarmas te siguen (una central que suena despierta el móvil), donde preguntas al asistente por voz, y la herramienta para configurar un nodo nuevo por Bluetooth sobre el terreno, a su lado.

## 📂 Estructura del repositorio

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, NodeControlScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmBanner.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, PinDialog.kt, MjpegFeed.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md   (the Firebase steps, in eight languages)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ Entorno de desarrollo

**Requisitos:** JDK 17+, el SDK de Android (compile SDK 37), un móvil con Android 8.0+ (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 257 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Las versiones se publican en GitHub, desde donde la propia app busca actualizaciones; el APK se firma con la clave de publicación propia del responsable, guardada fuera del repositorio, y la app compara el certificado de firma de una descarga con el suyo antes de pasar el archivo al instalador de Android. Véase el [límite del cliente](docs/CLIENT_BOUNDARY.md).

## 🔗 Proyectos relacionados

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) es un sistema de seguridad perimetral hecho de repositorios independientes. Cada uno tiene su propia versión, sus propias pruebas y su propio README; esta es la familia:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratos de mensajes, validadores, vectores de conformidad y tipos generados
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - Firmware del nodo de campo para ESP32-S3 con tres radares y su propio panel web
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - Protocolos de inversores y baterías solares y los mensajes de un nodo pasarela
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - Nodo eléctrico: contadores, el mensaje de las lecturas de la red y las reglas para maniobrar
* **[ARMOR-ALARM](https://github.com/JuanenRac/ARMOR-ALARM)** - Nodo y central de alarma: zonas, armado, retardos, sirena y PIN, con el servidor o sin él
* **[ARMOR-HMI](https://github.com/JuanenRac/ARMOR-HMI)** - Panel táctil: el estado del sistema en una pantalla de pared, armar y reconocer alarmas, y el hogar del asistente de voz
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - La red local: sus dispositivos, internet y lo que cambia
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinador central: telemetría, alarmas, dispositivos, lecturas solares y cámaras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Consola web: cámaras, radar, alarmas, energía solar y el diseñador de sitio 2D/3D
* **ARMOR-ANDROID-CONTROL** (este repositorio) - Cliente Android del operador con radar 2D/3D en vivo
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Política de inferencia visual que explica sus decisiones y nunca actúa
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zonas de cámara, máscaras y reglas de detección para la Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenciones de voz sin conexión con una confirmación imposible de falsificar
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Cajas, electrónica y la matriz de aceptación en banco
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Despliegue, el banco de pruebas de la CM5, copias de seguridad y TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulador de telemetría sin conexión con fallos repetibles
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Detecta, instala y actualiza los propios repositorios del ecosistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Arquitectura, base de seguridad y la matriz de capacidades

## 📚 Documentación y comunidad

Dónde leer más:

* [Matriz de capacidades: qué está probado y qué no](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catálogo de proyectos: versiones y cómo dependen unos de otros](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Historial de cambios de este repositorio](CHANGELOG.md)
* [Licencia (GPL-3.0-or-later)](LICENSE)
* Preguntas, ideas e informes: electrohobby3d@gmail.com

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCIA

GPL-3.0-or-later - véase [LICENSE](LICENSE).
