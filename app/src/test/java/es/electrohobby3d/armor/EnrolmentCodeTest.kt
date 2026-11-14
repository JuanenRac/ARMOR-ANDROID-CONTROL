package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Enrolment
import es.electrohobby3d.armor.model.EnrolmentCode
import es.electrohobby3d.armor.network.EnrolmentReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The QR that sets up the app is the text Studio writes, it can never hold a secret, and the server's facts must agree with it. */
class EnrolmentCodeTest {
    private val code = "Q2hpbGRyZW5PZkFyY2hpdGVjdHM"
    private val fingerprint = "79ca85e16340bbc46012defe4cfdff30287c1f0fa5f8dc26b395d15e498e8a7a"
    private val origin = "https://armor.example.org:18080"
    private fun text(ca: String = fingerprint, extra: String = "", version: Int = 1, address: String = origin) =
        "armor-enrol:{\"ca\":\"$ca\",\"code\":\"$code\",\"origin\":\"$address\"$extra,\"v\":$version}"

    @Test fun readsTheTextStudioWrites() {
        assertEquals(Enrolment(origin, code, fingerprint), EnrolmentCode.parse(text()))
        assertEquals("", EnrolmentCode.parse(text(ca = ""))?.caFingerprint)
        assertEquals("https://armor.example.org", EnrolmentCode.parse(text(address = "https://armor.example.org/"))?.origin)
    }

    @Test fun refusesAnythingThatIsNotExactlyThatText() {
        assertNull(EnrolmentCode.parse(null))
        assertNull(EnrolmentCode.parse(text(extra = ",\"password\":\"x\"")))
        assertNull(EnrolmentCode.parse(text(version = 2)))
        assertNull(EnrolmentCode.parse(text(address = "http://armor.local")))
        assertNull(EnrolmentCode.parse(text(address = "https://user:pw@armor.local")))
        assertNull(EnrolmentCode.parse(text(address = "https://armor.local/api")))
        assertNull(EnrolmentCode.parse(text(address = "https://armor.local/?x=1")))
        assertNull(EnrolmentCode.parse(text(ca = "abcd")))
        assertNull(EnrolmentCode.parse(text(ca = fingerprint.uppercase())))
        assertNull(EnrolmentCode.parse("armor-provision:{\"node_id\":\"a\",\"server\":\"$origin\",\"v\":1}"))
        assertNull(EnrolmentCode.parse("armor-enrol:[1]"))
        assertNull(EnrolmentCode.parse("armor-enrol:{broken"))
        assertNull(EnrolmentCode.parse("armor-enrol:" + "x".repeat(400)))
        assertNull(EnrolmentCode.parse(text().replace(code, "short")))
    }

    @Test fun theServerAgreesOnlyWithTheSameAddressAndTheSameAuthority() {
        val read = EnrolmentCode.parse(text())!!
        assertTrue(EnrolmentReader.agrees(read, EnrolmentReader.Facts(origin, fingerprint)))
        assertFalse(EnrolmentReader.agrees(read, EnrolmentReader.Facts("https://other.example.org", fingerprint)))
        assertFalse(EnrolmentReader.agrees(read, EnrolmentReader.Facts(origin, "")))
        assertFalse(EnrolmentReader.agrees(read, EnrolmentReader.Facts(origin, "0".repeat(64))))
    }

    @Test fun readsTheFactsOfTheServerAndRefusesWhatIsNotThem() {
        assertEquals(EnrolmentReader.Facts(origin, fingerprint), EnrolmentReader.parseFacts("{\"origin\":\"$origin\",\"ca_fingerprint\":\"$fingerprint\"}"))
        assertEquals(EnrolmentReader.Facts(origin, ""), EnrolmentReader.parseFacts("{\"origin\":\"$origin\",\"ca_fingerprint\":null}"))
        assertNull(EnrolmentReader.parseFacts("{\"origin\":\"http://armor.local\",\"ca_fingerprint\":null}"))
        assertNull(EnrolmentReader.parseFacts("{\"ca_fingerprint\":null}"))
        assertNull(EnrolmentReader.parseFacts("{\"origin\":\"$origin\",\"ca_fingerprint\":5}"))
        assertNull(EnrolmentReader.parseFacts("not json"))
    }

    @Test fun anAuthorityNamedByTheCodePairsTheKeyOfThatCertificateOnly() {
        val pins = PinStore(MapStore())
        val a = "A".repeat(43) + "="
        val b = "B".repeat(43) + "="
        val c = "C".repeat(43) + "="
        val leaf = "1".repeat(64)
        val authority = "2".repeat(64)
        // nothing named: a private authority the system does not know is refused, as before
        assertEquals(PinDecision.UnknownAuthority(b), pins.decide("armor.example.org", listOf(a, b), false, listOf(leaf, authority)))
        pins.expectAuthority("armor.example.org", authority)
        assertEquals(authority, pins.expectedAuthority("armor.example.org"))
        // the chain holds that certificate: its key is the one to pair, even if the system does not know the authority
        assertEquals(PinDecision.FirstUse(b), pins.decide("armor.example.org", listOf(a, b), false, listOf(leaf, authority)))
        // the chain does not hold it: the usual rules decide, nothing is refused by the expectation
        assertEquals(PinDecision.UnknownAuthority(c), pins.decide("armor.example.org", listOf(a, c), false, listOf(leaf, "3".repeat(64))))
        assertEquals(PinDecision.FirstUse(c), pins.decide("armor.example.org", listOf(a, c), true, listOf(leaf, "3".repeat(64))))
        // once a key is paired the expectation is not needed and a different chain is a mismatch
        pins.add(b, "armor.example.org")
        assertEquals(PinDecision.Mismatch, pins.decide("armor.example.org", listOf(a, c), true, listOf(leaf, "3".repeat(64))))
        pins.clearExpected("armor.example.org")
        assertNull(pins.expectedAuthority("armor.example.org"))
    }

    @Test fun anExpectedAuthorityThatIsNotAFingerprintIsIgnored() {
        val pins = PinStore(MapStore())
        pins.expectAuthority("armor.example.org", "short")
        pins.expectAuthority("armor.example.org", "G".repeat(64))
        pins.expectAuthority("armor.example.org", "A".repeat(64))
        assertNull(pins.expectedAuthority("armor.example.org"))
    }
}
