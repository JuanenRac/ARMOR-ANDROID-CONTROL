package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemoryStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

/** A box that only opens what it sealed itself, like the Keystore's. */
private class ReversingBox(private val tag: String = "A") : SecretBox {
    override fun seal(plain: String) = "$tag:" + plain.reversed()
    override fun open(sealed: String): String? = if (sealed.startsWith("$tag:")) sealed.removePrefix("$tag:").reversed() else null
}

class SessionVaultTest {
    private val origin = "http://192.168.0.180:18080"

    @Test fun `keeps the session of the server and gives it back`() {
        val vault = SessionVault(MemoryStore(), ReversingBox())
        vault.save(origin, "armor_studio_sid", "abc123")
        assertEquals(StoredSession(origin, "armor_studio_sid", "abc123"), vault.load(origin))
    }

    @Test fun `the sessions of several servers are kept at once and each is given back to its own server`() {
        val vault = SessionVault(MemoryStore(), ReversingBox())
        val workshop = "https://taller.example:18080"
        vault.save(origin, "armor_studio_sid", "house1")
        vault.save(workshop, "armor_studio_sid", "shop1")
        assertEquals("house1", vault.load(origin)?.value)
        assertEquals("shop1", vault.load(workshop)?.value)
        assertEquals(listOf(origin, workshop), vault.origins())
        vault.save(origin, "armor_studio_sid", "house2")
        assertEquals("house2", vault.load(origin)?.value)
        assertEquals("a server is listed once", listOf(origin, workshop), vault.origins())
    }

    @Test fun `forgetting one server leaves the others, and signing out forgets the one in use`() {
        val vault = SessionVault(MemoryStore(), ReversingBox())
        val workshop = "https://taller.example:18080"
        vault.save(origin, "armor_studio_sid", "house1")
        vault.save(workshop, "armor_studio_sid", "shop1")
        vault.clear()
        assertNull(vault.load(workshop))
        assertEquals("house1", vault.load(origin)?.value)
        assertEquals(listOf(origin), vault.origins())
        vault.forget(origin)
        assertTrue(vault.origins().isEmpty())
        assertTrue(!vault.has())
    }

    @Test fun `forgetting all leaves nothing`() {
        val store = MemoryStore()
        val vault = SessionVault(store, ReversingBox())
        vault.save(origin, "armor_studio_sid", "house1")
        vault.save("https://taller.example:18080", "armor_studio_sid", "shop1")
        vault.clearAll()
        assertTrue(store.values.isEmpty())
    }

    @Test fun `a session of the old single place is still read for its server`() {
        val store = MemoryStore()
        store.put(SessionVault.KEY, ReversingBox().seal("$origin\narmor_studio_sid\nold"))
        assertEquals("old", SessionVault(store, ReversingBox()).load(origin)?.value)
    }

    @Test fun `writes nothing readable and never the password`() {
        val store = MemoryStore()
        SessionVault(store, ReversingBox()).save(origin, "armor_studio_sid", "abc123")
        assertTrue(store.values.values.none { "abc123" in it })
    }

    @Test fun `a session kept for another server is not given to this one`() {
        val vault = SessionVault(MemoryStore(), ReversingBox())
        vault.save(origin, "armor_studio_sid", "abc123")
        assertNull(vault.load("http://10.0.0.5:18080"))
    }

    @Test fun `what another key sealed cannot be read and is dropped`() {
        val store = MemoryStore()
        SessionVault(store, ReversingBox("A")).save(origin, "armor_studio_sid", "abc123")
        assertNull(SessionVault(store, ReversingBox("B")).load(origin))
        assertTrue(store.values.isEmpty())
    }

    @Test fun `clear forgets the session and an empty one is not kept`() {
        val store = MemoryStore()
        val vault = SessionVault(store, ReversingBox())
        vault.save(origin, "armor_studio_sid", "")
        assertTrue(store.values.isEmpty())
        vault.save(origin, "armor_studio_sid", "abc123")
        vault.clear()
        assertNull(vault.load(origin))
    }

    @Test fun `the cookie put back is the one the next request carries`() {
        val client = ArmorApiClient()
        client.restoreSessionCookie(origin, "armor_studio_sid", "abc123")
        assertEquals("armor_studio_sid" to "abc123", client.sessionCookie(origin))
        assertNull(client.sessionCookie("http://10.9.9.9:18080"))
    }
}
