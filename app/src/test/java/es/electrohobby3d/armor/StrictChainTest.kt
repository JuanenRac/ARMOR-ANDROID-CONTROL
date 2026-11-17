package es.electrohobby3d.armor

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** pins.strict_chain: a server paired from now on must present a chain that leads to the certificate kept at pairing time. Real X.509 certificates are built here, with no extra library. */
class StrictChainTest {
    @After fun clean() { TrustPins.takeMismatch() }

    private val ec = KeyPairGenerator.getInstance("EC").apply { initialize(256) }

    private fun tlv(tag: Int, body: ByteArray): ByteArray {
        val length = when {
            body.size < 128 -> byteArrayOf(body.size.toByte())
            body.size < 256 -> byteArrayOf(0x81.toByte(), body.size.toByte())
            else -> byteArrayOf(0x82.toByte(), (body.size shr 8).toByte(), body.size.toByte())
        }
        return byteArrayOf(tag.toByte()) + length + body
    }
    private fun seq(vararg parts: ByteArray) = tlv(0x30, parts.fold(ByteArray(0)) { a, b -> a + b })
    private fun name(common: String) = seq(tlv(0x31, seq(tlv(0x06, byteArrayOf(0x55, 0x04, 0x03)), tlv(0x0c, common.toByteArray()))))
    private fun time(millis: Long): ByteArray {
        val format = java.text.SimpleDateFormat("yyMMddHHmmss'Z'", java.util.Locale.ROOT).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        return tlv(0x17, format.format(java.util.Date(millis)).toByteArray())
    }

    /** A certificate for [subject]'s key, signed by [issuer]; a self-signed one when both are the same pair. */
    private fun certificate(subjectName: String, subject: KeyPair, issuerName: String, issuer: KeyPair, serial: Int): X509Certificate {
        val algorithm = seq(tlv(0x06, byteArrayOf(0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x04, 0x03, 0x02)))
        val now = System.currentTimeMillis()
        val tbs = seq(
            tlv(0xa0, tlv(0x02, byteArrayOf(2))), tlv(0x02, byteArrayOf(serial.toByte())), algorithm, name(issuerName),
            seq(time(now - 86_400_000L), time(now + 86_400_000L * 365)), name(subjectName), subject.public.encoded,
        )
        val signature = Signature.getInstance("SHA256withECDSA").apply { initSign(issuer.private); update(tbs) }.sign()
        val der = seq(tbs, algorithm, tlv(0x03, byteArrayOf(0) + signature))
        return CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate
    }

    private class Chain(val root: X509Certificate, val leaf: X509Certificate) { val both: Array<X509Certificate> get() = arrayOf(leaf, root) }

    private fun chain(rootName: String = "Test authority"): Chain {
        val rootKeys = ec.generateKeyPair()
        val leafKeys = ec.generateKeyPair()
        return Chain(certificate(rootName, rootKeys, rootName, rootKeys, 1), certificate("casa", leafKeys, rootName, rootKeys, 2))
    }

    private val trustsNothing = object : javax.net.ssl.X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) { throw CertificateException("unknown authority") }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private fun accepts(manager: PinningTrustManager, chain: Array<X509Certificate>): Boolean =
        try { manager.checkServerTrusted(chain, "ECDSA"); true } catch (_: CertificateException) { false }

    @Test fun `generated certificates form a real chain`() {
        val c = chain()
        assertEquals(2, linkedCount(c.both))
        val forged = chain()
        assertEquals(1, linkedCount(arrayOf(forged.leaf, c.root)))
    }

    @Test fun `the first use keeps the certificate and the same chain keeps being accepted`() {
        val values = MapStore()
        val store = PinStore(values)
        val c = chain()
        val manager = PinningTrustManager("casa", object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }, store)
        assertTrue(accepts(manager, c.both))
        assertNotNull(store.certificate("casa"))
        assertFalse(store.isLegacy("casa"))
        assertTrue(accepts(manager, c.both))
        assertEquals(StrictVerdict.Verified, store.strictVerdict("casa", c.both.map(PinStore::fingerprintOf), 2))
    }

    @Test fun `a forged chain that reuses the pinned key is refused`() {
        val store = PinStore(MapStore())
        val c = chain()
        // pair with the authority: its key is the pin, as a QR code of Studio would carry
        store.add(PinStore.pinOf(c.root), "casa")
        val manager = PinningTrustManager("casa", trustsNothing, store)
        assertTrue(accepts(manager, c.both))   // the first connection after a pasted QR keeps the matching certificate
        assertNotNull(store.certificate("casa"))
        // another authority that happens to carry the same public key but is a different certificate
        val sameKeyDifferentCertificate = certificate("Test authority", java.security.KeyPair(c.root.publicKey, ec.generateKeyPair().private), "other", ec.generateKeyPair(), 9)
        val forgedChain = arrayOf(c.leaf, sameKeyDifferentCertificate)
        assertFalse(accepts(manager, forgedChain))
        assertTrue(TrustPins.takeMismatch())
    }

    @Test fun `an old pin without a certificate keeps working and is reported as old`() {
        val values = MapStore()
        val c = chain()
        values.put("pin_casa", PinStore.pinOf(c.root))   // written before certificates were kept: no marker, no certificate
        val store = PinStore(values)
        assertTrue(store.isLegacy("casa"))
        val manager = PinningTrustManager("casa", trustsNothing, store)
        assertTrue(accepts(manager, c.both))
        assertNull(store.certificate("casa"))
        assertEquals(StrictVerdict.Legacy, store.strictVerdict("casa", c.both.map(PinStore::fingerprintOf), 2))
    }

    @Test fun `with the switch off only the pin decides and clearing forgets the certificate`() {
        val store = PinStore(MapStore())
        assertTrue(store.strictChain())   // on by default
        store.setStrictChain(false)
        assertFalse(store.strictChain())
        assertEquals(StrictVerdict.Off, store.strictVerdict("casa", emptyList(), 0))
        store.setStrictChain(true)
        store.add(PinStore.pinOf(chain().root), "casa")
        store.setCertificate("casa", chain().root.encoded)
        store.clear("casa")
        assertNull(store.certificate("casa"))
        assertTrue(store.pins("casa").isEmpty())
        assertFalse(store.isLegacy("casa"))
    }
}
