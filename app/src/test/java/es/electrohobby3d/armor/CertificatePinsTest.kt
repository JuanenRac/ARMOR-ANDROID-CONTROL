package es.electrohobby3d.armor

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.PublicKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/** Trust on first use of the key of the certificate, the pairing with the text of Studio's QR, and the refusal of a plain http after https. */
class CertificatePinsTest {
    private val a = "A".repeat(43) + "="
    private val b = "B".repeat(43) + "="
    private val c = "C".repeat(43) + "="
    private fun store() = PinStore(MapStore())

    @After fun clean() { TrustPins.takeMismatch() }

    @Test fun `a pin is a sha256 in base64`() {
        assertTrue(PinStore.isWellFormed(a))
        assertFalse(PinStore.isWellFormed("short="))
        assertFalse(PinStore.isWellFormed("A".repeat(44)))
        assertFalse(PinStore.isWellFormed("!".repeat(43) + "="))
    }

    @Test fun `first use, then trusted, then a mismatch`() {
        val pins = store()
        assertEquals(PinDecision.FirstUse(b), pins.decide("casa", listOf(a, b), systemTrusts = true))
        pins.add(b, "casa")
        assertEquals(PinDecision.Trusted, pins.decide("casa", listOf(a, b), true))
        assertEquals(PinDecision.Mismatch, pins.decide("CASA", listOf(c), true))
        assertEquals(PinDecision.Mismatch, pins.decide("casa", listOf(c), false))
    }

    @Test fun `a private authority must be paired before it is trusted`() {
        val pins = store()
        assertEquals(PinDecision.UnknownAuthority(b), pins.decide("x", listOf(a, b), systemTrusts = false))
        assertEquals(PinDecision.Mismatch, pins.decide("x", emptyList(), true))
        pins.add(b, "x")
        assertEquals(PinDecision.Trusted, pins.decide("x", listOf(a, b), systemTrusts = false))
    }

    @Test fun `a server that stands alone pins its own key`() {
        assertEquals(PinDecision.FirstUse(a), store().decide("x", listOf(a), true))
    }

    @Test fun `a host keeps four pins, the newest last, and can be cleared`() {
        val pins = store()
        val d = "D".repeat(43) + "="
        val e = "E".repeat(43) + "="
        for (pin in listOf(a, b, c, d, e)) pins.add(pin, "h")
        assertEquals(listOf(b, c, d, e), pins.pins("h"))
        pins.add("garbage", "h")
        assertEquals(4, pins.pins("h").size)
        pins.clear("h")
        assertTrue(pins.pins("h").isEmpty())
    }

    @Test fun `the text of the QR of Studio`() {
        assertEquals(null to a, PinStore.parseQr("armor-pin:$a"))
        assertEquals("casa.duckdns.org" to a, PinStore.parseQr("  armor-pin:casa.duckdns.org:$a "))
        assertNull(PinStore.parseQr("https://evil.example"))
        assertNull(PinStore.parseQr("armor-pin:nope"))
        assertNull(PinStore.parseQr("armor-pin::$a"))
    }

    @Test fun `the pin of a certificate is the sha256 of its public key`() {
        val key = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().public
        val expected = java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(key.encoded))
        assertEquals(expected, PinStore.pinOf(FakeCertificate(key)))
        assertTrue(PinStore.isWellFormed(expected))
    }

    @Test fun `the trust manager keeps a pin the first time and refuses a different key afterwards`() {
        val generator = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val store = store()
        val system = object : javax.net.ssl.X509TrustManager {   // a stand-in for the system's manager that trusts everything: the decision of the pin is what is under test
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val manager = PinningTrustManager("casa", system, store)
        val first = FakeCertificate(generator.generateKeyPair().public)
        manager.checkServerTrusted(arrayOf(first), "RSA")
        assertEquals(1, store.pins("casa").size)
        manager.checkServerTrusted(arrayOf(first), "RSA")   // the same key: fine
        val other = FakeCertificate(generator.generateKeyPair().public)
        try { manager.checkServerTrusted(arrayOf(other), "RSA"); throw AssertionError("a different key was accepted") } catch (_: CertificateException) {}
        assertTrue(TrustPins.takeMismatch())
        assertFalse(TrustPins.takeMismatch())
    }

    @Test fun `after https a plain http to the same host is refused`() {
        val memory = SchemeMemory(MapStore())
        assertTrue(memory.allows("http://192.168.0.180:8080"))
        memory.sawHttps("192.168.0.180")
        assertFalse(memory.allows("http://192.168.0.180:8080"))
        assertTrue(memory.allows("https://192.168.0.180:8443"))
        assertTrue(memory.allows("http://192.168.0.181:8080"))
        memory.forget("192.168.0.180")
        assertTrue(memory.allows("http://192.168.0.180:8080"))
    }
}

/** A certificate that only knows its public key, which is all the pin looks at. */
private class FakeCertificate(private val key: PublicKey) : X509Certificate() {
    override fun getPublicKey(): PublicKey = key
    override fun checkValidity() {}
    override fun checkValidity(date: java.util.Date?) {}
    override fun getVersion() = 3
    override fun getSerialNumber(): java.math.BigInteger = java.math.BigInteger.ONE
    override fun getIssuerDN(): java.security.Principal = throw UnsupportedOperationException()
    override fun getSubjectDN(): java.security.Principal = throw UnsupportedOperationException()
    override fun getNotBefore(): java.util.Date = java.util.Date(0)
    override fun getNotAfter(): java.util.Date = java.util.Date(Long.MAX_VALUE)
    override fun getTBSCertificate(): ByteArray = ByteArray(0)
    override fun getSignature(): ByteArray = ByteArray(0)
    override fun getSigAlgName() = "none"
    override fun getSigAlgOID() = "0"
    override fun getSigAlgParams(): ByteArray? = null
    override fun getIssuerUniqueID(): BooleanArray? = null
    override fun getSubjectUniqueID(): BooleanArray? = null
    override fun getKeyUsage(): BooleanArray? = null
    override fun getBasicConstraints() = -1
    override fun getEncoded(): ByteArray = ByteArray(0)
    override fun verify(key: PublicKey?) {}
    override fun verify(key: PublicKey?, sigProvider: String?) {}
    override fun toString() = "FakeCertificate"
    override fun hasUnsupportedCriticalExtension() = false
    override fun getCriticalExtensionOIDs(): MutableSet<String>? = null
    override fun getNonCriticalExtensionOIDs(): MutableSet<String>? = null
    override fun getExtensionValue(oid: String?): ByteArray? = null
}
