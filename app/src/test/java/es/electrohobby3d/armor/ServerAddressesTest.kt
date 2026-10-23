package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MapStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

/** The two addresses of the house: which one is used where the phone is, what happens when it is silent, and what the person is told. */
class ServerAddressesTest {
    private val home = ServerEndpoint.parse("http://192.168.0.180:8080")!!
    private val away = ServerEndpoint.parse("https://casa.duckdns.org:443")!!
    private val both = ServerAddresses(home, away)

    @Test fun `the kind of network says where the phone is`() {
        assertEquals(NetworkPlace.Home, NetworkPlace.of(wifi = true, ethernet = false, cellular = false, vpn = false))
        assertEquals(NetworkPlace.Home, NetworkPlace.of(wifi = false, ethernet = true, cellular = false, vpn = false))
        assertEquals(NetworkPlace.Away, NetworkPlace.of(wifi = false, ethernet = false, cellular = true, vpn = false))
        assertEquals(NetworkPlace.Away, NetworkPlace.of(wifi = true, ethernet = false, cellular = false, vpn = true))
        assertEquals(NetworkPlace.Unknown, NetworkPlace.of(wifi = false, ethernet = false, cellular = false, vpn = false))
    }

    @Test fun `at home the private address, away the other, and one alone is always used`() {
        assertEquals(home, both.chosen(NetworkPlace.Home))
        assertEquals(away, both.chosen(NetworkPlace.Away))
        assertEquals(home, both.chosen(NetworkPlace.Unknown))
        assertEquals(home, ServerAddresses(home, null).chosen(NetworkPlace.Away))
        assertEquals(away, ServerAddresses(null, away).chosen(NetworkPlace.Home))
        assertNull(ServerAddresses(null, null).chosen(NetworkPlace.Home))
    }

    @Test fun `the other address is the alternative`() {
        assertEquals(away, both.alternativeTo(home))
        assertEquals(home, both.alternativeTo(away))
        assertNull(ServerAddresses(home, null).alternativeTo(home))
        assertTrue(both.isAway(away))
        assertFalse(both.isAway(home))
    }

    @Test fun `the addresses are saved and the single address of an older version is the one for home`() {
        val store = MapStore()
        both.save(store)
        assertEquals(both, ServerAddresses.load(store))
        val old = MapStore().apply { put("origin", home.origin) }
        assertEquals(ServerAddresses(home, null), ServerAddresses.load(old))
        assertTrue(ServerAddresses.load(MapStore()).isEmpty)
    }

    private fun select(place: NetworkPlace, silent: Set<ServerEndpoint> = emptySet(), available: Boolean = true, addresses: ServerAddresses = both, mismatch: Boolean = false) =
        Connector.select(addresses, place, available, { mismatch }) { it !in silent }

    @Test fun `the chosen address is used when it answers`() {
        assertEquals(Selection.Reached(home), select(NetworkPlace.Home))
        assertEquals(Selection.Reached(away), select(NetworkPlace.Away))
    }

    @Test fun `when the chosen one is silent the other is tried`() {
        assertEquals(Selection.Reached(away), select(NetworkPlace.Home, silent = setOf(home)))
        assertEquals(Selection.Reached(home), select(NetworkPlace.Away, silent = setOf(away)))
    }

    @Test fun `the message says exactly what failed`() {
        assertEquals(Selection.Failed(ReachProblem.BothUnreachable), select(NetworkPlace.Home, silent = setOf(home, away)))
        assertEquals(Selection.Failed(ReachProblem.HomeUnreachableNoRemote), select(NetworkPlace.Away, silent = setOf(home), addresses = ServerAddresses(home, null)))
        assertEquals(Selection.Failed(ReachProblem.RemoteUnreachable), select(NetworkPlace.Away, silent = setOf(away), addresses = ServerAddresses(null, away)))
        assertEquals(Selection.Failed(ReachProblem.Offline), select(NetworkPlace.Home, silent = setOf(home, away), available = false))
        assertEquals(Selection.Failed(ReachProblem.NoAddress), select(NetworkPlace.Home, addresses = ServerAddresses(null, null)))
        assertEquals(Selection.Failed(ReachProblem.CertificateChanged), select(NetworkPlace.Away, silent = setOf(away), mismatch = true))
    }

    @Test fun `every problem has words`() {
        Tr.useTable("es", emptyMap())
        for (problem in ReachProblem.entries) assertTrue(problem.message.isNotBlank())
        assertTrue(ReachProblem.HomeUnreachableNoRemote.message.contains("No se puede llegar a casa"))
    }
}
