# Límite del cliente móvil

ARMOR-ANDROID-CONTROL consume la API de ARMOR-SERVER; no se conecta a cámaras
por RTSP ni recibe las contraseñas, rutas RTSP ni la clave que cifra la
configuración de cámara.

## Flujo actual

1. El operador introduce un origen HTTPS, o un origen HTTP limitado a una IP
   privada de LAN (`10/8`, `172.16/12`, `192.168/16`, loopback).
2. El cliente consulta `status` y `camera-views`, ambos de sólo lectura.
3. Para acciones sensibles, el token de operador se canjea por una cookie
   HttpOnly temporal mediante `POST /api/v1/operator/session`.
4. Con la sesión se habilitan PTZ, snapshot, grabación y catálogo de evidencia.
   El token se borra del campo y no se almacena en preferencias.
5. El vídeo usa el relay MJPEG de ARMOR-SERVER. El teléfono nunca construye una
   URL RTSP y no puede revelar una contraseña de cámara.

## Límites deliberados

- Armar y desarmar exige una confirmación en pantalla y una sesión iniciada; el
  servidor lo registra con el nombre del usuario. Confirmar una alarma no la cierra:
  se cierra sola cuando su causa termina.
- HTTP está permitido sólo para una instalación doméstica/LAN actual. Una
  exposición fuera de esa red debe terminar TLS antes de distribuir el APK.
- El APK `debug` no es un canal de actualización. La publicación requiere una
  clave de firma estable, versionCode monótono y una política de releases.

## Configurar un nodo de campo por Bluetooth

Para un nodo sin cable Ethernet, o sin dirección todavía, la app lo configura por Bluetooth Low Energy con el mismo protocolo que su panel web
(`docs/BLE_PROVISIONING.md` de ARMOR-RADAR, ARMOR-SOLAR y ARMOR-ELECTRICAL: los tres tipos de nodo hablan el mismo protocolo y la app dice de cuál se trata): busca los nodos que anuncian `ARMOR-xxxxxx`, se conecta, crea el administrador de un nodo nuevo con su código de
configuración (o inicia sesión), busca redes Wi-Fi, fija nombre, Wi-Fi de un router o dirección fija, broker y modo Bluetooth, y reinicia el nodo.

- Permisos: `BLUETOOTH_SCAN` (declarado sin uso para ubicación) y `BLUETOOTH_CONNECT`; la ubicación sólo se pide hasta Android 11, donde el escaneo la exige.
- El enlace va cifrado (emparejamiento «just works»): impide que alguien escuche, no que alguien presente mientras el móvil se empareja. La contraseña de
  administrador y el código de configuración viajan dentro de ese enlace.
- La app no guarda el código, el secreto de la flota ni las contraseñas.
- **No verificado**: el enlace está compilado y su formato, sus operaciones y el cálculo del código tienen tests en la JVM (12 tests del protocolo), pero no se ha
  ejecutado contra un nodo ni un teléfono; el emparejamiento, el MTU y la reconexión son lo primero a comprobar en el banco.
