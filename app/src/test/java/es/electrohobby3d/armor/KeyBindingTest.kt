package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class BindingStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

/** A box that only opens what it sealed itself; [locked] stands for a key tied to the unlocking of a phone that is locked. */
private class BindingBox(private val tag: String, var locked: Boolean = false) : SecretBox {
    override fun seal(plain: String) = "$tag:" + plain.reversed()
    override fun open(sealed: String): String? = if (!locked && sealed.startsWith("$tag:")) sealed.removePrefix("$tag:").reversed() else null
    override fun usable() = !locked
}

class KeyBindingTest {
    private val origin = "http://192.168.0.180:18080"

    @Test fun `the key is not tied to the unlocking until the person says so`() {
        val binding = KeyBinding(BindingStore())
        assertFalse(binding.unlockBound)
        binding.unlockBound = true
        assertTrue(binding.unlockBound)
        binding.unlockBound = false
        assertFalse(binding.unlockBound)
    }

    @Test fun `the two ways of keeping the secrets use two keys, and the old one is the one that already existed`() {
        assertEquals("armor-session", KeyBoxes.alias("armor-session", false))
        assertEquals("armor-credentials", KeyBoxes.alias("armor-credentials", false))
        assertEquals("armor-session-unlock", KeyBoxes.alias("armor-session", true))
        assertEquals("armor-credentials-unlock", KeyBoxes.alias("armor-credentials", true))
    }

    @Test fun `a session sealed the old way is sealed again the new way and read with it`() {
        val store = BindingStore()
        val plain = BindingBox("plain"); val bound = BindingBox("bound")
        SessionVault(store, plain).save(origin, "armor_session", "abc123")
        assertEquals(Reseal.Moved, resealStore(store, SessionVault.KEY, plain, bound))
        assertEquals(StoredSession(origin, "armor_session", "abc123"), SessionVault(store, bound).load(origin))
        assertNull(SessionVault(store, plain).load(origin))   // the old box no longer opens it
    }

    @Test fun `the remembered sign-in goes the same way`() {
        val store = BindingStore()
        val plain = BindingBox("plain"); val bound = BindingBox("bound")
        CredentialVault(store, plain).save(origin, "ana", "a\nb c")
        assertEquals(Reseal.Moved, resealStore(store, CredentialVault.KEY, plain, bound))
        assertEquals(StoredCredentials(origin, "ana", "a\nb c"), CredentialVault(store, bound).load(origin))
    }

    @Test fun `nothing kept means nothing to move`() {
        assertEquals(Reseal.Nothing, resealStore(BindingStore(), SessionVault.KEY, BindingBox("a"), BindingBox("b")))
    }

    @Test fun `what the old box cannot open is dropped, so the person signs in again`() {
        val store = BindingStore()
        SessionVault(store, BindingBox("someone else")).save(origin, "armor_session", "abc123")
        assertEquals(Reseal.Lost, resealStore(store, SessionVault.KEY, BindingBox("plain"), BindingBox("bound")))
        assertFalse(SessionVault(store, BindingBox("bound")).has())
    }

    @Test fun `a locked phone leaves what is kept alone instead of taking it for unreadable`() {
        val store = BindingStore()
        val box = BindingBox("bound")
        val vault = SessionVault(store, box)
        vault.save(origin, "armor_session", "abc123")
        box.locked = true                              // the phone restarted: the watch tries to read the session before anybody unlocked it
        assertNull(vault.load(origin))
        assertTrue("the session is still there", vault.has())
        box.locked = false                             // the person unlocked the phone
        assertEquals("abc123", vault.load(origin)?.value)
    }

    @Test fun `the same for the remembered sign-in`() {
        val store = BindingStore()
        val box = BindingBox("bound")
        val vault = CredentialVault(store, box)
        vault.save(origin, "ana", "s3cret")
        box.locked = true
        assertNull(vault.load(origin))
        assertTrue(vault.has())
        box.locked = false
        assertEquals("s3cret", vault.load(origin)?.password)
    }

    @Test fun `moving the secrets while the phone is locked loses nothing`() {
        val store = BindingStore()
        val bound = BindingBox("bound")
        SessionVault(store, bound).save(origin, "armor_session", "abc123")
        bound.locked = true
        assertEquals(Reseal.Lost, resealStore(store, SessionVault.KEY, bound, BindingBox("plain")))
        assertTrue("it could not be read, not unreadable: it is kept", SessionVault(store, bound).has())
    }
}
