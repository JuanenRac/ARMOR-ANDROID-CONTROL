// ARMOR-ANDROID-CONTROL - what the person reads: the messages of the server and the network in plain words, with no codes.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

object Friendly {
    /**
     * A message from the app or the server as a sentence a person needs: "Server operation failed (HTTP 401)" becomes "Usuario o contraseña
     * incorrectos". Text that is already a sentence is left alone. Null and blank stay null.
     */
    fun message(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val lower = text.lowercase()
        val code = Regex("""http (\d{3})""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            code == 401 -> tr("Usuario o contraseña incorrectos, o la sesión ha caducado.")
            code == 403 -> tr("No tienes permiso para hacer esto.")
            code == 404 -> tr("El servidor no tiene lo que se pide. Puede que necesite actualizarse.")
            code == 429 -> tr("Demasiados intentos. Espera unos minutos y vuelve a probar.")
            code != null && code >= 500 -> tr("El servidor tiene un problema. Inténtalo de nuevo en un momento.")
            code != null -> tr("El servidor no ha aceptado la petición.")
            "unable to resolve host" in lower || "failed to connect" in lower || "connectexception" in lower || "timeout" in lower || "timed out" in lower ||
                "no route to host" in lower || "connection refused" in lower || "econnrefused" in lower || "unknownhost" in lower ->
                tr("No se puede conectar con el servidor. Comprueba que el móvil está en la misma wifi y que la dirección es correcta.")
            "enter username and password" in lower -> tr("Escribe el usuario y la contraseña.")
            "sign in first" in lower || "sign in to armor-server" in lower -> tr("Inicia sesión primero.")
            "valid https origin" in lower -> tr("La dirección del servidor no es válida.")
            lower == "session established" -> tr("Sesión iniciada.")
            lower == "session restored" -> tr("Sesión recuperada.")
            lower == "session closed" -> tr("Sesión cerrada.")
            lower == "connection refreshed" -> tr("Todo actualizado.")
            lower == "evidence library refreshed" -> tr("Grabaciones actualizadas.")
            lower == "evidence removed" -> tr("Grabación borrada.")
            lower.startsWith("snapshot saved") -> tr("Foto guardada.")
            lower.startsWith("recording started") -> tr("Grabando.")
            lower.startsWith("recording finalized") -> tr("Grabación guardada.")
            lower == "ptz command failed" -> tr("La cámara no ha podido moverse.")
            "server operation failed" in lower -> tr("El servidor no ha podido hacerlo.")
            else -> text
        }
    }

    private fun local(iso: String, zone: java.time.ZoneId): java.time.ZonedDateTime? =
        runCatching { java.time.Instant.parse(iso).atZone(zone) }.getOrNull()

    /** "2026-09-26T01:11:00.123Z" as the clock time in the phone's time zone ("03:11" in Madrid in summer); anything else is returned short. */
    fun clock(iso: String, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
        local(iso, zone)?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: iso.take(16)

    /** "2026-09-26T01:11:00Z" as "26/09 03:11" in the phone's time zone. */
    fun dayAndClock(iso: String, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
        local(iso, zone)?.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")) ?: iso.take(16)
}
