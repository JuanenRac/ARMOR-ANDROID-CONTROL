// ARMOR-ANDROID-CONTROL - trust in the certificate of the server: the key of the installation's certificate authority (or of the server's own certificate) is remembered the first time (trust on first
// use) or paired with the text of the QR code of Studio, and a different key is refused. The system's own checks still come first; the pin is added on top of them, never instead of the host name check.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

sealed interface PinDecision {
    /** The chain holds a key already pinned for this server. */
    data object Trusted : PinDecision
    /** Nothing is pinned yet and the system already trusts the chain: this pin is kept for next time. */
    data class FirstUse(val pin: String) : PinDecision
    /** A pin exists and nothing in the chain matches it. */
    data object Mismatch : PinDecision
    /** Nothing is pinned and the system does not trust the chain either (a private authority): the person must pair first. */
    data class UnknownAuthority(val pin: String) : PinDecision
}

/** The pins kept for each server: the base64 SHA-256 of the public key (SubjectPublicKeyInfo) of a certificate. More than one may be kept (a renewal, a backup key). */
class PinStore(private val store: KeyValueStore) {
    private fun key(host: String) = "pin_" + host.lowercase()

    fun pins(host: String): List<String> = store.get(key(host)).orEmpty().split(',').filter { it.isNotBlank() }

    /** Adds a pin; a host keeps at most four, the newest last. A text that is not a pin is ignored. */
    fun add(pin: String, host: String) {
        val clean = pin.trim()
        if (!isWellFormed(clean)) return
        val list = pins(host).filter { it != clean } + clean
        store.put(key(host), list.takeLast(4).joinToString(","))
    }

    fun clear(host: String) = store.remove(key(host))

    /** `chainPins` are the pins of every certificate of the chain, the leaf first; `systemTrusts` is the answer of the system's own evaluation. */
    fun decide(host: String, chainPins: List<String>, systemTrusts: Boolean): PinDecision {
        val kept = pins(host)
        if (kept.isNotEmpty()) return if (chainPins.any { it in kept }) PinDecision.Trusted else PinDecision.Mismatch
        if (chainPins.isEmpty()) return PinDecision.Mismatch
        // the key worth pinning is the one of the authority when there is one above the leaf, and the leaf's own when the server stands alone
        val candidate = if (chainPins.size > 1) chainPins.last() else chainPins.first()
        return if (systemTrusts) PinDecision.FirstUse(candidate) else PinDecision.UnknownAuthority(candidate)
    }

    companion object {
        /** A SHA-256 in base64 is 44 characters ending in one "=". */
        fun isWellFormed(pin: String): Boolean = pin.length == 44 && pin.endsWith("=") && pin.dropLast(1).all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' }

        /** The pin that a QR code of Studio carries: `armor-pin:<base64>` or `armor-pin:<host>:<base64>`; null for anything else. */
        fun parseQr(text: String): Pair<String?, String>? {
            val source = text.trim()
            val prefix = "armor-pin:"
            if (!source.startsWith(prefix)) return null
            val rest = source.removePrefix(prefix)
            if (isWellFormed(rest)) return null to rest
            val split = rest.lastIndexOf(':')
            if (split <= 0) return null
            val host = rest.substring(0, split)
            val pin = rest.substring(split + 1)
            return if (isWellFormed(pin)) host to pin else null
        }

        /** The pin of a certificate: the SHA-256 of its public key in X.509 form, in base64. */
        fun pinOf(certificate: X509Certificate): String = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded))
    }
}

/**
 * The process-wide rules for https: set once by the app with the store of pins, used by the HTTP client of the server and by the camera stream. With no store (a test) nothing is added to
 * the system's checks.
 */
object TrustPins {
    @Volatile private var pins: PinStore? = null
    @Volatile private var mismatchHost: String? = null

    @Volatile private var schemes: SchemeMemory? = null

    fun install(store: KeyValueStore) { pins = PinStore(store); schemes = SchemeMemory(store) }
    fun store(): PinStore? = pins
    fun schemeMemory(): SchemeMemory? = schemes

    /** Refuses plain http to a host that has been reached over https; no-op until [install]. */
    fun checkScheme(origin: String) {
        val memory = schemes ?: return
        if (!memory.allows(origin)) throw es.electrohobby3d.armor.network.ArmorApiException("This server was reached over https before: plain http to it is refused (it could be someone else answering). Use https again, or forget the server in the settings.", null)
    }

    /** Remembers that [origin] answered over https. */
    fun reached(origin: String) {
        val memory = schemes ?: return
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return
        if (uri.scheme == "https") memory.sawHttps(uri.host.orEmpty())
    }

    /** True once if a certificate was refused because it did not match the pin (or no pin was paired for a private authority). */
    fun takeMismatch(): Boolean { val had = mismatchHost != null; mismatchHost = null; return had }
    internal fun noteMismatch(host: String) { mismatchHost = host }

    /** Puts the pin rules on an https connection to [host]; a plain http one is left alone. */
    fun apply(connection: HttpURLConnection, host: String) {
        val store = pins ?: return
        if (connection !is HttpsURLConnection) return
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(null as java.security.KeyStore?) }
        val system = factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull() ?: return
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(PinningTrustManager(host, system, store)), null) }
        connection.sslSocketFactory = context.socketFactory
    }
}

/** The system's trust manager with the pin on top: a chain the system trusts is also checked against the pin, and a chain it does not trust is accepted only if a paired pin is in it. */
class PinningTrustManager(private val host: String, private val system: X509TrustManager, private val store: PinStore) : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = system.acceptedIssuers

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = system.checkClientTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val systemTrusts = try { system.checkServerTrusted(chain, authType); true } catch (error: CertificateException) { false }
        val decision = store.decide(host, chain.map(PinStore::pinOf), systemTrusts)
        when (decision) {
            PinDecision.Trusted -> Unit
            is PinDecision.FirstUse -> store.add(decision.pin, host)
            PinDecision.Mismatch, is PinDecision.UnknownAuthority -> { TrustPins.noteMismatch(host); throw CertificateException("The certificate of $host does not match the one that was paired") }
        }
    }
}

/**
 * Once a server has been reached over https, plain http to the same host is refused (a downgrade would send the password in the clear to whoever answers at that address). Forgotten when the
 * person changes the server's address in the settings.
 */
class SchemeMemory(private val store: KeyValueStore) {
    private fun key(host: String) = "https_seen_" + host.lowercase()
    fun sawHttps(host: String) = store.put(key(host), "1")
    fun forget(host: String) = store.remove(key(host))

    /** False when [origin] is plain http to a host that has been seen over https. */
    fun allows(origin: String): Boolean {
        val endpoint = ServerEndpoint.parse(origin) ?: return false
        val uri = runCatching { java.net.URI(endpoint.origin) }.getOrNull() ?: return false
        return !(uri.scheme == "http" && store.get(key(uri.host.orEmpty())) == "1")
    }
}
