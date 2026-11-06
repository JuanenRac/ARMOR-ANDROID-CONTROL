package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Pairing
import es.electrohobby3d.armor.model.PairingCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The pairing code is the same text the shared library writes, and it can never hold a secret. */
class PairingCodeTest {
    @Test fun writesTheSameTextAsTheSharedLibraryAndReadsItBack() {
        val text = PairingCode.build("radar-1", "https://armor.local:8443/")
        assertEquals("armor-provision:{\"node_id\":\"radar-1\",\"server\":\"https://armor.local:8443\",\"v\":1}", text)
        assertEquals(Pairing("radar-1", "https://armor.local:8443"), PairingCode.parse(text))
        assertEquals("http://192.168.0.180:18080", PairingCode.parse(PairingCode.build("a1", "http://192.168.0.180:18080"))?.server)
    }

    @Test fun refusesIdsAndAddressesThatAreNotValidOrCouldHoldASecret() {
        for (id in listOf("", "-x", "Radar", "a b", "x".repeat(65))) assertNull(id, PairingCode.build(id, "https://armor.local"))
        for (server in listOf("https://user:pw@armor.local", "https://user@armor.local", "https://armor.local/?token=x", "https://armor.local/#x", "https://armor.local/api",
            "ftp://armor.local", "armor.local", "https://", "https://armor.local:99999", "https://armor .local", "")) {
            assertNull(server, PairingCode.cleanServer(server))
            assertNull(server, PairingCode.build("radar-1", server))
        }
    }

    @Test fun refusesExtraOrMissingFieldsOtherVersionsAndOtherText() {
        val prefix = "armor-provision:"
        for (bad in listOf(
            "{\"v\":1,\"node_id\":\"radar-1\",\"server\":\"https://armor.local\",\"token\":\"x\"}",
            "{\"v\":1,\"node_id\":\"radar-1\"}",
            "{\"v\":2,\"node_id\":\"radar-1\",\"server\":\"https://armor.local\"}",
            "{\"v\":true,\"node_id\":\"radar-1\",\"server\":\"https://armor.local\"}",
            "{\"v\":1,\"node_id\":5,\"server\":\"https://armor.local\"}",
        )) assertNull(bad, PairingCode.parse(prefix + bad))
        for (bad in listOf(null, "", "armor-provision:", "armor-provision:{", "armor-provision:[]", "other:{}", prefix + "x".repeat(600))) assertNull(bad, PairingCode.parse(bad))
        assertNotNull(PairingCode.parse(prefix + "{\"v\":1,\"node_id\":\"radar-1\",\"server\":\"https:\\/\\/armor.local\"}"))
    }
}
