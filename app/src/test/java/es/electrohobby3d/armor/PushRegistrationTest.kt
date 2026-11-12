package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class PushStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

/** A box that opens what it sealed; [locked] stands for a key tied to the unlocking of a phone that is locked. */
private class PushBox(var locked: Boolean = false) : SecretBox {
    override fun seal(plain: String) = "box:" + plain.reversed()
    override fun open(sealed: String): String? = if (!locked && sealed.startsWith("box:")) sealed.removePrefix("box:").reversed() else null
    override fun usable() = !locked
}

/** A new token reaches the server at once when the phone still holds a session it can read, and waits (without any call) in every other case. */
class PushRegistrationTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val store = PushStore()
    private val box = PushBox()
    private val vault = SessionVault(store, box)

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(server.origin); server.stop() }

    @Test fun `with a readable session the token is told to the server in the language of the phone and the registration is noted`() {
        vault.save(server.origin, "armor_session", "abc123")
        var noted = 0
        assertTrue(PushRegistration.register(server.origin, "tok-1", vault, box, client, "pt") { noted++ })
        val request = server.seen.single()
        assertEquals("POST" to "/api/v1/push/register", request.method to request.path)
        assertTrue(request.body, request.body.contains("tok-1") && request.body.contains("pt"))
        assertTrue(request.headers["cookie"].orEmpty().contains("armor_session=abc123"))   // as the signed-in person
        assertEquals(1, noted)
    }

    @Test fun `with no session kept nothing is sent and nothing is noted`() {
        var noted = 0
        assertFalse(PushRegistration.register(server.origin, "tok", vault, box, client, "en") { noted++ })
        assertTrue(server.seen.isEmpty())
        assertEquals(0, noted)
    }

    @Test fun `a session kept for another server is not used`() {
        vault.save("http://192.168.0.99:18080", "armor_session", "abc123")
        assertFalse(PushRegistration.register(server.origin, "tok", vault, box, client, "en"))
        assertTrue(server.seen.isEmpty())
    }

    @Test fun `while the key is locked behind the unlocking of the phone the token waits`() {
        vault.save(server.origin, "armor_session", "abc123")
        box.locked = true
        assertFalse(PushRegistration.register(server.origin, "tok", vault, box, client, "en"))
        assertTrue(server.seen.isEmpty())
        box.locked = false   // the phone is unlocked: the same session now works
        assertTrue(PushRegistration.register(server.origin, "tok", vault, box, client, "en"))
        assertEquals(1, server.seen.size)
    }

    @Test fun `an address that cannot be read sends nothing`() {
        vault.save("not an address", "armor_session", "abc123")
        for (bad in listOf("", "not an address", "ftp://x")) assertFalse(bad, PushRegistration.register(bad, "tok", vault, box, client, "en"))
        assertTrue(server.seen.isEmpty())
    }

    @Test fun `the server that does not answer is an error for the caller to see and the registration is not noted`() {
        vault.save(server.origin, "armor_session", "abc123")
        server.reply(500)
        var noted = 0
        val failed = runCatching { PushRegistration.register(server.origin, "tok", vault, box, client, "en") { noted++ } }
        assertTrue(failed.isFailure || noted == 0)
        assertEquals(0, noted)
    }
}
