package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.SessionFacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

private class TrustedStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

/** A key pair made in the JVM, standing for the one of the Keystore; [locked] stands for a phone that is locked. */
private class JvmSigner(var locked: Boolean = false, private val present: Boolean = true) : DeviceSigner {
    private val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    var forgotten = false
    override fun available() = present
    override fun publicKey(): String? = Base64.getEncoder().encodeToString(pair.public.encoded)
    override fun sign(data: ByteArray): ByteArray? = if (locked) null else Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(data) }.sign()
    override fun forget() { forgotten = true }
    fun verifies(data: ByteArray, signature: ByteArray, key: PublicKey = pair.public) = Signature.getInstance("SHA256withECDSA").apply { initVerify(key); update(data) }.verify(signature)
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrustedPhoneTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient
    private val origin get() = server.origin
    private val id = "0123456789abcdef01234567"

    @Before fun start() { Dispatchers.setMain(UnconfinedTestDispatcher()); server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { server.stop(); Dispatchers.resetMain() }

    private fun phone(settings: TrustedPhoneSettings, signer: DeviceSigner = JvmSigner()) = TrustedPhone(client, signer, settings) { "Pixel 9" }
    private fun settings(on: Boolean = true) = TrustedPhoneSettings(TrustedStore()).also { it.enabled = on }

    @Test fun `it is off until the person turns it on, and nothing is asked of the server meanwhile`() {
        val s = settings(on = false)
        assertFalse(TrustedPhoneSettings(TrustedStore()).enabled)
        assertEquals(TrustResult.Off, phone(s).register(origin, offered = true))
        assertFalse(phone(s).renew(origin))
        assertTrue(server.seen.isEmpty())
    }

    @Test fun `a server that does not offer it, or a phone that cannot hold a key, is not asked`() {
        assertEquals(TrustResult.Unavailable, phone(settings()).register(origin, offered = false))
        assertEquals(TrustResult.Unavailable, phone(settings(), JvmSigner(present = false)).register(origin, offered = true))
        assertEquals(TrustResult.Unavailable, phone(settings(), NoDeviceSigner).register(origin, offered = true))
        assertTrue(server.seen.isEmpty())
    }

    @Test fun `it registers with the name and the public key, keeps the id, and does not register twice`() {
        val signer = JvmSigner(); val s = settings()
        server.reply(201, """{"id":"$id","name":"Pixel 9"}""")
        assertEquals(TrustResult.Done, phone(s, signer).register(origin, true))
        assertEquals(id, s.id(origin))
        val sent = server.seen.single()
        assertEquals("POST", sent.method); assertEquals("/api/v1/devices/trust", sent.path)
        assertTrue(sent.body, sent.body.contains("\"name\":\"Pixel 9\"") && sent.body.contains(signer.publicKey()!!))
        assertEquals(TrustResult.Done, phone(s, signer).register(origin, true))
        assertEquals("the second time nothing is sent", 1, server.seen.size)
    }

    @Test fun `a server that wants a new proof first, or that refuses, leaves no id`() {
        val s = settings()
        server.reply(403, """{"error":"prove yourself","code":"elevation_required"}""")
        assertEquals(TrustResult.NeedsProof, phone(s).register(origin, true))
        server.reply(409, """{"error":"too many","code":"too_many_devices"}""")
        assertEquals(TrustResult.Failed, phone(s).register(origin, true))
        assertNull(s.id(origin))
    }

    @Test fun `the session is renewed by signing the challenge the server gave, tied to this phone and this challenge`() {
        val signer = JvmSigner(); val s = settings(); s.setId(origin, id)
        server.respond = { request ->
            when (request.path) {
                "/api/v1/devices/trust/challenge" -> Triple(200, """{"challenge":"chal-1","expires_in_s":60}""", emptyMap())
                "/api/v1/devices/trust/session" -> Triple(201, """{"expiresAt":"x"}""", mapOf("Set-Cookie" to "armor_studio_sid=fresh; Path=/; HttpOnly"))
                else -> Triple(404, "{}", emptyMap())
            }
        }
        assertTrue(phone(s, signer).renew(origin))
        val challenge = server.seen[0]; val session = server.seen[1]
        assertTrue(challenge.body.contains(id))
        val body = org.json.JSONObject(session.body)
        assertEquals(id, body.getString("id")); assertEquals("chal-1", body.getString("challenge"))
        assertTrue("the server checks this signature", signer.verifies(trustedSignedText(id, "chal-1").toByteArray(Charsets.UTF_8), Base64.getDecoder().decode(body.getString("signature"))))
        assertFalse("not valid for another challenge", signer.verifies(trustedSignedText(id, "chal-2").toByteArray(Charsets.UTF_8), Base64.getDecoder().decode(body.getString("signature"))))
    }

    @Test fun `a locked phone cannot sign, so nothing is sent and the id stays`() {
        val s = settings(); s.setId(origin, id)
        server.reply(200, """{"challenge":"c"}""")
        assertFalse(phone(s, JvmSigner(locked = true)).renew(origin))
        assertEquals(id, s.id(origin))
        assertEquals("only the challenge was asked", 1, server.seen.size)
    }

    @Test fun `a server that no longer knows the phone makes the app forget the id, a server that is down does not`() {
        val s = settings(); s.setId(origin, id)
        server.reply(404, """{"error":"no such trusted phone","code":"unknown_device"}""")
        assertFalse(phone(s).renew(origin))
        assertNull(s.id(origin))
        s.setId(origin, id)
        val closed = java.net.ServerSocket(0).use { it.localPort }
        s.setId("http://127.0.0.1:$closed", id)
        assertFalse(phone(s).renew("http://127.0.0.1:$closed"))
        assertEquals(id, s.id("http://127.0.0.1:$closed"))
    }

    @Test fun `turning it off ends the phone at the server and forgets the id, and the key only when asked`() {
        val signer = JvmSigner(); val s = settings(); s.setId(origin, id)
        server.reply(204, "")
        phone(s, signer).end(origin)
        val sent = server.seen.single()
        assertEquals("DELETE", sent.method); assertEquals("/api/v1/devices/trust/$id", sent.path)
        assertNull(s.id(origin)); assertFalse(signer.forgotten)
        phone(s, signer).end(origin, dropKey = true)
        assertTrue(signer.forgotten)
    }

    @Test fun `an id that is not the one the server makes is not believed`() {
        val s = settings()
        s.setId(origin, "../../etc/passwd")
        assertNull(s.id(origin))
    }

    @Test fun `the session announces whether the server offers trusted phones`() {
        val on = org.json.JSONObject("""{"user":{"role":"admin"},"capabilities":{"version":1,"security":{"client.device_lock":"user","client.trusted_devices":true}}}""")
        val off = org.json.JSONObject("""{"user":{"role":"admin"},"capabilities":{"version":1,"security":{"client.device_lock":"user"}}}""")
        assertTrue(SessionFacts.parse(on).trustedDevices)
        assertFalse(SessionFacts.parse(off).trustedDevices)
        assertFalse(SessionFacts.parse(org.json.JSONObject("""{"user":{"role":"viewer"}}""")).trustedDevices)
    }

    @Test fun `a session the server ended is renewed with the trusted phone before the person is sent to the password`() {
        val s = settings(); s.setId(origin, id)
        var renewed = false
        server.respond = { request ->
            when {
                request.path == "/api/v1/studio/session" && request.method == "GET" ->
                    if (renewed) Triple(200, """{"authenticated":true,"user":{"role":"operator"},"capabilities":{"version":1,"security":{"client.trusted_devices":true}}}""", emptyMap())
                    else Triple(200, """{"authenticated":false}""", emptyMap())
                request.path == "/api/v1/devices/trust/challenge" -> Triple(200, """{"challenge":"c1"}""", emptyMap())
                request.path == "/api/v1/devices/trust/session" -> { renewed = true; Triple(201, """{"expiresAt":"x"}""", mapOf("Set-Cookie" to "armor_studio_sid=fresh; Path=/; HttpOnly")) }
                else -> Triple(200, "{}", emptyMap())
            }
        }
        val model = ArmorViewModel(client)
        model.trusted = phone(s)
        var refused = 0
        runBlocking {
            assertTrue(model.tryRestore(origin, StoredSession(origin, "armor_studio_sid", "old")) { refused += 1 })
            assertEquals("the person was not sent to the password", 0, refused)
            assertTrue(model.state.value.authenticated)
            assertTrue(model.state.value.trustedDevices)
            // with nothing kept at all, the trusted phone alone can open the session
            renewed = false
            assertTrue(model.tryRestore(origin, null) { refused += 1 })
        }
        assertEquals(0, refused)
    }

    @Test fun `without a trusted phone, a refused session is refused as it always was`() {
        server.reply(200, """{"authenticated":false}""")
        val model = ArmorViewModel(client)
        var refused = 0
        runBlocking { assertFalse(model.tryRestore(origin, StoredSession(origin, "armor_studio_sid", "old")) { refused += 1 }) }
        assertEquals(1, refused)
    }
}
