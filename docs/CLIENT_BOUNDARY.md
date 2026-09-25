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
