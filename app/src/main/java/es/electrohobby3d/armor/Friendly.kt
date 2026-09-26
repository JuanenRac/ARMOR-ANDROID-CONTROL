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
            code == 401 -> "Usuario o contraseña incorrectos, o la sesión ha caducado."
            code == 403 -> "No tienes permiso para hacer esto."
            code == 404 -> "El servidor no tiene lo que se pide. Puede que necesite actualizarse."
            code == 429 -> "Demasiados intentos. Espera unos minutos y vuelve a probar."
            code != null && code >= 500 -> "El servidor tiene un problema. Inténtalo de nuevo en un momento."
            code != null -> "El servidor no ha aceptado la petición."
            "unable to resolve host" in lower || "failed to connect" in lower || "connectexception" in lower || "timeout" in lower || "timed out" in lower ||
                "no route to host" in lower || "connection refused" in lower || "econnrefused" in lower || "unknownhost" in lower ->
                "No se puede conectar con el servidor. Comprueba que el móvil está en la misma wifi y que la dirección es correcta."
            "enter username and password" in lower -> "Escribe el usuario y la contraseña."
            "sign in first" in lower || "sign in to armor-server" in lower -> "Inicia sesión primero."
            "valid https origin" in lower -> "La dirección del servidor no es válida."
            lower == "session established" -> "Sesión iniciada."
            lower == "session restored" -> "Sesión recuperada."
            lower == "session closed" -> "Sesión cerrada."
            lower == "connection refreshed" -> "Todo actualizado."
            lower == "evidence library refreshed" -> "Grabaciones actualizadas."
            lower == "evidence removed" -> "Grabación borrada."
            lower.startsWith("snapshot saved") -> "Foto guardada."
            lower.startsWith("recording started") -> "Grabando."
            lower.startsWith("recording finalized") -> "Grabación guardada."
            lower == "ptz command failed" -> "La cámara no ha podido moverse."
            "server operation failed" in lower -> "El servidor no ha podido hacerlo."
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
