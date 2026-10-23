// ARMOR-ANDROID-CONTROL - the lock of the phone, used to protect the app: the fingerprint, the face or the PIN of the phone itself, asked before the sensitive actions and, if wanted, to open the app.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// Everything here is a choice of the person, with the careful default: before arming, disarming, silencing a siren, unlocking and the other critical orders the app asks for the fingerprint or the
// face when the phone has one (and for nothing when it has no lock at all). Opening the app behind the lock is off until the person turns it on. The server can only RAISE this (the security option
// `client.device_lock` of Studio: "sensitive" or "always"); it can never lower what the person chose. A phone with no lock at all is never shut out, and a locked app always offers the way out of
// signing in again with the password. The lock only protects an unattended phone: the server keeps checking the session, the PIN and the proof of identity on its own.
package es.electrohobby3d.armor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the phone can ask: a fingerprint or face (and, as the way out of it, the phone's PIN), the PIN, pattern or password of the phone alone, or nothing. */
enum class LockAvailability { Biometric, DeviceCredential, None }

/** What the server asks of every phone: nothing more than the person chose, also the sensitive actions, or also to open the app. */
enum class ServerLock {
    User, Sensitive, Always;

    val wire: String get() = name.lowercase()

    companion object {
        /** The value the server announced; anything it did not say (an older server) or a word this version does not know is "the person decides". */
        fun parse(text: String?): ServerLock = when (text) { "sensitive" -> Sensitive; "always" -> Always; else -> User }
    }
}

sealed interface AuthResult {
    data object Success : AuthResult
    /** The person closed the question. */
    data object Cancelled : AuthResult
    /** The phone cannot ask (no lock, nothing enrolled, the hardware is not there): the person is not shut out for it. */
    data object Unavailable : AuthResult
    /** A refusal that is not a choice: too many wrong tries, a lock-out. */
    data class Failed(val reason: String) : AuthResult
}

/** The phone's question, behind an interface so the rules can be tested without a phone. */
interface DeviceAuth {
    fun availability(): LockAvailability
    suspend fun authenticate(title: String, subtitle: String): AuthResult
}

/** Asked before an action that must be confirmed with the lock of the phone; true when it may go on. */
fun interface SensitiveGate {
    suspend fun allow(reason: String): Boolean
}

/** The phone that has nothing to ask with (a preview, a test): the app is not locked by it. */
object NoDeviceAuth : DeviceAuth {
    override fun availability() = LockAvailability.None
    override suspend fun authenticate(title: String, subtitle: String): AuthResult = AuthResult.Unavailable
}

/** What the person chose, kept in the phone's preferences. */
class LockSettings(private val store: KeyValueStore) {
    /** Before the sensitive actions: what the person chose, else on when the phone has a fingerprint or a face, else off. */
    fun sensitive(availability: LockAvailability): Boolean = store.get(SENSITIVE)?.let { it == "1" } ?: (availability == LockAvailability.Biometric)
    fun setSensitive(on: Boolean) = store.put(SENSITIVE, if (on) "1" else "0")

    /** Behind the lock to open the app: off until the person turns it on. */
    var openApp: Boolean
        get() = store.get(OPEN) == "1"
        set(on) = store.put(OPEN, if (on) "1" else "0")

    /** For how long one unlocking counts, in seconds: 0 asks every time. */
    var graceSeconds: Int
        get() = store.get(GRACE)?.toIntOrNull()?.takeIf { it in GRACE_CHOICES } ?: DEFAULT_GRACE
        set(seconds) = store.put(GRACE, (if (seconds in GRACE_CHOICES) seconds else DEFAULT_GRACE).toString())

    /** What the server asked the last time the person was signed in, so it also holds when the app opens before the sign-in. */
    var server: ServerLock
        get() = ServerLock.parse(store.get(SERVER))
        set(policy) = store.put(SERVER, policy.wire)

    companion object {
        val GRACE_CHOICES = listOf(0, 30, 120, 300)
        const val DEFAULT_GRACE = 30
        private const val SENSITIVE = "lock_sensitive"
        private const val OPEN = "lock_open"
        private const val GRACE = "lock_grace"
        private const val SERVER = "lock_server"
    }
}

/** What the screen of the settings shows. */
data class LockStatus(val availability: LockAvailability, val sensitiveOn: Boolean, val openOn: Boolean, val graceSeconds: Int, val server: ServerLock, val unmet: Boolean) {
    /** The server asks for it, so the person's switch cannot turn it off. */
    val sensitiveForced: Boolean get() = server != ServerLock.User
    val openForced: Boolean get() = server == ServerLock.Always
}

/** When the lock is asked for: the rules, with the clock and the phone's answer given from outside. */
class LockPolicy(private val settings: LockSettings, private val availability: () -> LockAvailability, private val clock: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private var lastAuthAt: Long? = null
    private var leftAt: Long? = null

    /** True while the phone's own question is on screen: leaving the app for it is not leaving the app. */
    @Volatile var prompting: Boolean = false

    /** The sensitive actions need the lock: the person's choice, or the server's, and only when the phone has a lock to ask. */
    fun sensitiveRequired(): Boolean {
        val phone = availability()
        if (phone == LockAvailability.None) return false
        return settings.sensitive(phone) || settings.server != ServerLock.User
    }

    /** Opening the app needs the lock. */
    fun openRequired(): Boolean {
        if (availability() == LockAvailability.None) return false
        return settings.openApp || settings.server == ServerLock.Always
    }

    /** The last unlocking still counts. */
    fun fresh(): Boolean {
        val at = lastAuthAt ?: return false
        val grace = settings.graceSeconds
        return grace > 0 && clock() - at < grace * 1000L
    }

    /** A sensitive action has to ask now. */
    fun promptNeeded(): Boolean = sensitiveRequired() && !fresh()

    /** The person was recognised just now. */
    fun unlocked() { lastAuthAt = clock() }

    /** The app went out of sight. */
    fun left() { if (!prompting) leftAt = clock() }

    /** The app is back in sight: true when it must be locked again (it was away for as long as an unlocking counts, or longer). */
    fun returned(): Boolean {
        if (prompting) return false
        val at = leftAt ?: return false
        leftAt = null
        return openRequired() && clock() - at >= settings.graceSeconds * 1000L
    }

    fun status(): LockStatus {
        val phone = availability()
        val sensitive = settings.sensitive(phone) || settings.server != ServerLock.User
        val open = settings.openApp || settings.server == ServerLock.Always
        return LockStatus(phone, sensitive, open, settings.graceSeconds, settings.server, unmet = phone == LockAvailability.None && (sensitive || open))
    }
}

/** Puts the question of the phone in front of the actions and of the opening of the app. */
class LockGate(val policy: LockPolicy, private val auth: DeviceAuth) : SensitiveGate {
    private val one = Mutex()   // two orders at once ask once

    override suspend fun allow(reason: String): Boolean = one.withLock {
        if (!policy.promptNeeded()) return@withLock true
        when (ask(tr("Confirma que eres tú"), reason)) {
            AuthResult.Success, AuthResult.Unavailable -> true   // a phone that cannot ask is not a phone that cannot be used
            else -> false
        }
    }

    /** The question to change the lock itself: turning it on proves it works on this phone, turning it off proves it is the person. */
    suspend fun prove(): AuthResult = one.withLock { ask(tr("Confirma que eres tú"), tr("Cambiar el bloqueo de la aplicación")) }

    /** The question to open the app. */
    suspend fun unlockToOpen(): AuthResult = one.withLock { ask(tr("Abre A.R.M.O.R. con el bloqueo del teléfono"), tr("Solo tú puedes abrir la aplicación.")) }

    private suspend fun ask(title: String, subtitle: String): AuthResult {
        policy.prompting = true
        val result = try { auth.authenticate(title, subtitle) } finally { policy.prompting = false }
        if (result == AuthResult.Success || result == AuthResult.Unavailable) policy.unlocked()
        return result
    }
}
