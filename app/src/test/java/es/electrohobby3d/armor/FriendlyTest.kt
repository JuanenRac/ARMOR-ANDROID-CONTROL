package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FriendlyTest {
    @Test fun `codes become sentences`() {
        assertEquals("Usuario o contraseña incorrectos, o la sesión ha caducado.", Friendly.message("Server operation failed (HTTP 401)"))
        assertEquals("Demasiados intentos. Espera unos minutos y vuelve a probar.", Friendly.message("Server operation failed (HTTP 429): {\"error\":\"rate\"}"))
        assertEquals("El servidor tiene un problema. Inténtalo de nuevo en un momento.", Friendly.message("Server operation failed (HTTP 503)"))
        assertEquals("El servidor no ha podido hacerlo.", Friendly.message("Server operation failed"))
    }

    @Test fun `network failures say what to check`() {
        val text = "No se puede conectar con el servidor. Comprueba que el móvil está en la misma wifi y que la dirección es correcta."
        assertEquals(text, Friendly.message("failed to connect to /192.168.0.10 (port 18080) from /192.168.0.20"))
        assertEquals(text, Friendly.message("java.net.SocketTimeoutException: timeout"))
        assertEquals(text, Friendly.message("Unable to resolve host \"armor.local\""))
    }

    @Test fun `the app's own English messages are translated and sentences are kept`() {
        assertEquals("Escribe el usuario y la contraseña.", Friendly.message("Enter username and password"))
        assertEquals("Todo actualizado.", Friendly.message("Connection refreshed"))
        assertEquals("Foto guardada.", Friendly.message("Snapshot saved: Entrada"))
        assertEquals("Sistema ARMADO", Friendly.message("Sistema ARMADO"))
        assertNull(Friendly.message("  "))
        assertNull(Friendly.message(null))
    }

    @Test fun `times are shown in the phone's time zone`() {
        val utc = java.time.ZoneId.of("UTC")
        val madrid = java.time.ZoneId.of("Europe/Madrid")
        assertEquals("01:11", Friendly.clock("2026-09-26T01:11:22.500Z", utc))
        assertEquals("03:11", Friendly.clock("2026-09-26T01:11:22.500Z", madrid))
        assertEquals("26/09 03:11", Friendly.dayAndClock("2026-09-26T01:11:22Z", madrid))
        assertEquals("sin datos", Friendly.clock("sin datos", utc))
    }
}
