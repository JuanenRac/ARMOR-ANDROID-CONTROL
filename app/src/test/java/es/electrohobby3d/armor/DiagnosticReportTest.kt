package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticReportTest {
    @Before fun clean() = DiagnosticLog.clear()

    private fun info(origin: String = "https://casa.example.org:8443/x?a=b", recent: List<DiagnosticLog.Entry> = emptyList()) = DiagnosticInfo(
        appVersion = "0.8.0.2", appCode = 80002, androidRelease = "14", sdk = 34, device = "Acme Phone", language = "es", origin = origin, role = "admin",
        notificationsAllowed = true, batteryUnrestricted = null, watcherOn = false, recent = recent, nowMs = 100_000,
    )

    @Test fun aRouteIsReducedToItsShapeWithNoQueryOrIds() {
        assertEquals("/api/v1/cameras/gate/…", DiagnosticLog.shape("/api/v1/cameras/gate/snapshot?ticket=SECRET"))
        assertEquals("/api/v1/alarms", DiagnosticLog.shape("/api/v1/alarms"))
        assertEquals("/", DiagnosticLog.shape("?x=1"))
    }

    @Test fun onlyTheLastAnswersAreKeptAndTheOldestGoesFirst() {
        for (index in 1..40) DiagnosticLog.record("GET", "/api/v1/state", 200 + index, atMs = index.toLong())
        val kept = DiagnosticLog.recent()
        assertEquals(25, kept.size)
        assertEquals(216, kept.first().status)
        assertEquals(240, kept.last().status)
    }

    @Test fun theAddressIsReducedToSchemeAndHostAndNeverHoldsASignIn() {
        assertEquals("https://casa.example.org", DiagnosticReport.originLabel("https://user:pass@casa.example.org:8443/x?a=b"))
        assertEquals("not set", DiagnosticReport.originLabel("ftp://host"))
        assertEquals("not set", DiagnosticReport.originLabel(""))
    }

    @Test fun theReportSaysWhatIsKnownAndWhatIsNotAndHoldsNoSecret() {
        DiagnosticLog.record("POST", "/api/v1/alarm/command?pin=1234", 403, atMs = 90_000)
        val text = DiagnosticReport.render(info(origin = "https://user:pass@casa.example.org/x", recent = DiagnosticLog.recent()))
        assertTrue(text.contains("App: 0.8.0.2 (code 80002)"))
        assertTrue(text.contains("Server: https://casa.example.org"))
        assertTrue(text.contains("Battery optimisation off for the app: unknown"))
        assertTrue(text.contains("Alarm watcher on: no"))
        assertTrue(text.contains("POST /api/v1/alarm/command -> 403 (10s ago)"))
        for (secret in listOf("user:pass", "1234", "pin=")) assertFalse("$secret leaked", text.contains(secret))
    }
}
