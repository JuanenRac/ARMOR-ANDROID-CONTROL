<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center"><a href="README.md">🇺🇸 English</a> | 🇪🇸 <b>Español</b></p>

### Cliente móvil de operador para ARMOR-SERVER

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**Comprobación de honestidad - qué funciona hoy:** Las reglas de seguridad de la dirección y de decisión de alarmas tienen tests unitarios y la app compila. **No se ha ejecutado en un teléfono contra el servidor**, así que el permiso de notificaciones y el servicio en segundo plano no están verificados; armar, alarmas y dispositivos siguen las rutas del servidor pero no se han probado en un teléfono real.

---

## 1. 🛠️ DESCRIPCIÓN

* **Acceso con el login del propio servidor:** IP, puerto, usuario y contraseña; la contraseña crea una sesión HttpOnly y nunca se guarda en el teléfono.
* **Monitor de cámaras:** de 1 a 16 mosaicos, vista ampliada, MJPEG en directo que respeta la proporción, mando PTZ acotado, capturas y grabaciones.
* **Armar y desarmar**, con confirmación; **alarmas** para confirmar, con un aviso de las pendientes; **dispositivos** (humo, gas, inundación, puerta, ventana, movimiento, clima, enchufes, luces, sirenas, cerraduras) con su estado y Encender / Apagar / Alternar.
* **Biblioteca de evidencias**, estado del perímetro y de los nodos, y un **historial** de cada cambio de alerta, nodo, cámara, dispositivo, alarma y modo.
* **Notificaciones de alarma** cuando un nodo llega a ALTA, una cámara deja de responder, un dispositivo da alarma (humo, gas, inundación o pánico siempre; puerta, ventana o movimiento con el sistema armado) o, con el sistema armado, un nodo se cae; una vigilancia opcional en segundo plano usa la sesión actual y avisa cuando termina.
* **Cuidado con la contraseña:** el HTTP plano solo se permite hacia un *literal* IPv4 de LAN privada o loopback. Se rechaza un nombre de host que solo empieza como una dirección privada (`10.atacante.ejemplo`) o una dirección con cero a la izquierda (algunos resolvedores leen `010.0.0.1` como la pública `8.0.0.1`).
* El aspecto Hydra: superficies casi negras, acento cian, ámbar para lo que requiere atención.

---

## 2. 🔧 COMPILAR Y EJECUTAR

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

El APK de depuración no está firmado para distribución. Véase el [límite del cliente](docs/CLIENT_BOUNDARY.md).

---

## 📂 ESTRUCTURA DE DIRECTORIOS

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt, ArmorViewModel.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmWatcherService.kt
│   ├── ArmorTheme.kt, ServerEndpoint.kt, MjpegFeed.kt
│   ├── network/   model/
└── app/src/test/   tests de seguridad de la dirección
```

---

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCIA

GPL-3.0-or-later - véase [LICENSE](LICENSE).
