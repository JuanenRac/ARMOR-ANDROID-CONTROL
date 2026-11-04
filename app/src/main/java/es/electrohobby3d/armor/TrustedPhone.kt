// ARMOR-ANDROID-CONTROL - a trusted phone: a key pair that never leaves the phone, registered with the server, so that the session can be renewed by signing a challenge instead of typing the password
// and the code again.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// OFF by default, and only offered when the administrator turned on `client.trusted_devices` in Studio (announced in `capabilities.security` of the session). The private half is made inside the Android
// Keystore, cannot be exported and can be used only while the phone is unlocked (from Android 9). The server keeps the public half, hands out a one-time challenge, and opens a session of the length the
// administrator chose only to the phone that signs it. The administrator (or the person) can end the phone from Studio at any time, and turning the switch off here ends it too. What this does not do, and
// is left to be tried on a real phone: asking the fingerprint for every renewal (a BiometricPrompt tied to the key); the phone's own lock (the option of the lock of the phone) already protects the app.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApi
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.ElevationRequiredException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/** The key pair of the phone, behind an interface so the rules can be tested without a phone. */
interface DeviceSigner {
    /** Whether this phone can hold the key (a preview or a test cannot). */
    fun available(): Boolean
    /** The public key as base64 of its DER form (SubjectPublicKeyInfo), made on the first call; null when it cannot be made. */
    fun publicKey(): String?
    /** The ECDSA signature with SHA-256, in DER, of [data]; null when the key cannot be used now (the phone is locked) or there is none. */
    fun sign(data: ByteArray): ByteArray?
    /** Forgets the key pair. */
    fun forget()
}

/** No key at all: a preview, a test. */
object NoDeviceSigner : DeviceSigner {
    override fun available() = false
    override fun publicKey(): String? = null
    override fun sign(data: ByteArray): ByteArray? = null
    override fun forget() {}
}

/** An ECDSA P-256 key made inside the Android Keystore; usable only while the phone is unlocked (from Android 9). */
class KeystoreSigner(private val alias: String = "armor-trusted-phone") : DeviceSigner {
    override fun available() = true

    @Synchronized private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized private fun ensure(): Boolean = runCatching {
        val store = keyStore()
        if (store.containsAlias(alias)) return true
        val generator = KeyPairGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(alias, android.security.keystore.KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256)
        if (android.os.Build.VERSION.SDK_INT >= 28) spec.setUnlockedDeviceRequired(true)
        generator.initialize(spec.build())
        generator.generateKeyPair()
        true
    }.getOrDefault(false)

    override fun publicKey(): String? = if (!ensure()) null else runCatching { Base64.getEncoder().encodeToString(keyStore().getCertificate(alias).publicKey.encoded) }.getOrNull()

    override fun sign(data: ByteArray): ByteArray? = runCatching {
        val key = keyStore().getKey(alias, null) as? PrivateKey ?: return null
        Signature.getInstance("SHA256withECDSA").apply { initSign(key); update(data) }.sign()
    }.getOrNull()

    override fun forget() { runCatching { keyStore().deleteEntry(alias) } }
}

/** What the person chose and what the server gave this phone, in the preferences of the app. */
class TrustedPhoneSettings(private val store: KeyValueStore) {
    /** The person's switch; off until they turn it on. */
    var enabled: Boolean
        get() = store.get(ENABLED) == "1"
        set(on) = store.put(ENABLED, if (on) "1" else "0")

    /** The id the server gave this phone on [origin], or null. */
    fun id(origin: String): String? = store.get(ID_PREFIX + origin)?.takeIf { it.matches(ID) }
    fun setId(origin: String, id: String) { if (id.matches(ID)) store.put(ID_PREFIX + origin, id) }
    fun clear(origin: String) { store.remove(ID_PREFIX + origin) }

    companion object {
        private const val ENABLED = "trusted_phone"
        private const val ID_PREFIX = "trusted_phone_id:"
        private val ID = Regex("^[0-9a-f]{24}$")
    }
}

/** What came of asking the server to trust this phone. */
enum class TrustResult {
    /** Registered (or it already was). */
    Done,
    /** The person's switch is off. */
    Off,
    /** The server does not offer it (an older one, or the administrator left it off) or this phone cannot hold a key. */
    Unavailable,
    /** The server wants a new proof of identity first. */
    NeedsProof,
    /** The server refused (too many phones, a bad name) or could not be reached. */
    Failed,
}

/** The text the phone signs: the same the server checks, tied to one phone and one challenge. */
fun trustedSignedText(id: String, challenge: String): String = "armor-device-trust-v1|$id|$challenge"

class TrustedPhone(private val api: ArmorApi, private val signer: DeviceSigner, private val settings: TrustedPhoneSettings, private val phoneName: () -> String) {
    /** Whether the person asked for it and this phone can do it. */
    fun wanted(): Boolean = settings.enabled && signer.available()

    /** Registers this phone with the server once signed in, when the person asked for it and the server offers it ([offered]: `capabilities.security` says so). */
    fun register(origin: String, offered: Boolean): TrustResult {
        if (!settings.enabled) return TrustResult.Off
        if (!offered || !signer.available()) return TrustResult.Unavailable
        if (settings.id(origin) != null) return TrustResult.Done
        val key = signer.publicKey() ?: return TrustResult.Unavailable
        return try {
            settings.setId(origin, api.trustRegister(origin, phoneName().take(60).ifBlank { "Android" }, key))
            if (settings.id(origin) != null) TrustResult.Done else TrustResult.Failed
        } catch (error: ElevationRequiredException) { TrustResult.NeedsProof }
        catch (error: ArmorApiException) { TrustResult.Failed }
        catch (error: java.io.IOException) { TrustResult.Failed }
    }

    /**
     * Opens a new session by signing a challenge, when this phone was registered on [origin]. True when the server opened one (its cookie is then the session of the client). A refusal that says the
     * server no longer knows this phone (ended from Studio, the option turned off) forgets the id, so the person signs in the ordinary way and is not asked again for nothing; a server that could not
     * be reached leaves everything as it is.
     */
    fun renew(origin: String): Boolean {
        if (!settings.enabled) return false
        val id = settings.id(origin) ?: return false
        return try {
            val challenge = api.trustChallenge(origin, id)
            val signature = signer.sign(trustedSignedText(id, challenge).toByteArray(Charsets.UTF_8)) ?: return false   // the phone is locked: try again later, keep the id
            api.trustSession(origin, id, challenge, Base64.getEncoder().encodeToString(signature))
            true
        } catch (error: ArmorApiException) {
            if (error.code == 404 || error.code == 401) settings.clear(origin)
            false
        } catch (error: java.io.IOException) { false }
    }

    /** The person turned it off, or signed out of this server: the server forgets the phone (when it can be asked) and so does the app. The key is kept for the other servers unless [dropKey]. */
    fun end(origin: String, dropKey: Boolean = false) {
        val id = settings.id(origin)
        if (id != null) runCatching { api.trustRemove(origin, id) }
        settings.clear(origin)
        if (dropKey) signer.forget()
    }
}
