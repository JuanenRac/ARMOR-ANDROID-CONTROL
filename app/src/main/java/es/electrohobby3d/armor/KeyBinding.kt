// ARMOR-ANDROID-CONTROL - the option to tie the key that seals the session and the remembered sign-in to the unlocking of the phone.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// OFF by default. The session and the remembered user and password are sealed with a key of the Android Keystore that cannot leave the phone. With this option on, the key is also one that
// Android lets be used only while the phone is unlocked: a phone that is switched on but locked (and in the wrong hands) gives nothing. The price, which the screen says in plain words:
// the watch for alarms that runs in the background cannot read the session again after the phone restarts, or after Android ended the app, until the person unlocks the phone, so no
// alarm is heard in between. The server cannot turn this on for anybody: it would cut the alarms of every phone without asking, and the server's own checks do not depend on it.
package es.electrohobby3d.armor

import android.app.KeyguardManager
import android.content.Context

/** What the person chose, in the preferences of the app. */
class KeyBinding(private val store: KeyValueStore) {
    var unlockBound: Boolean
        get() = store.get(KEY) == "1"
        set(on) = store.put(KEY, if (on) "1" else "0")

    companion object { const val KEY = "key_unlock" }
}

/** The result of sealing a kept secret again with another box. */
enum class Reseal { Nothing, Moved, Lost }

/** Opens what [key] holds with [from] and seals it again with [to]; what [from] cannot open is dropped (the person signs in again), what is not there stays not there. */
fun resealStore(store: KeyValueStore, key: String, from: SecretBox, to: SecretBox): Reseal {
    val sealed = store.get(key) ?: return Reseal.Nothing
    val plain = if (from.usable()) from.open(sealed) else null
    if (plain == null) { if (from.usable()) store.remove(key); return Reseal.Lost }
    store.put(key, to.seal(plain))
    return Reseal.Moved
}

/** The boxes of the app, in one place so the screen, the watch for alarms and the receiver of the restart all open the same ones. */
object KeyBoxes {
    private const val SESSION = "armor-session"
    private const val CREDENTIALS = "armor-credentials"

    fun bound(context: Context): Boolean = KeyBinding(PreferencesStore(context.getSharedPreferences("armor-control", Context.MODE_PRIVATE))).unlockBound

    fun alias(base: String, bound: Boolean): String = if (bound) "$base-unlock" else base

    private fun box(context: Context, base: String, bound: Boolean): KeystoreBox =
        KeystoreBox(alias(base, bound), bound) { context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true }

    fun sessionBox(context: Context, bound: Boolean = bound(context)): KeystoreBox = box(context, SESSION, bound)
    fun credentialBox(context: Context, bound: Boolean = bound(context)): KeystoreBox = box(context, CREDENTIALS, bound)

    fun sessionVault(context: Context, bound: Boolean = bound(context)) =
        SessionVault(PreferencesStore(context.getSharedPreferences("armor-session", Context.MODE_PRIVATE)), sessionBox(context, bound))

    fun credentialVault(context: Context, bound: Boolean = bound(context)) =
        CredentialVault(PreferencesStore(context.getSharedPreferences("armor-credentials", Context.MODE_PRIVATE)), credentialBox(context, bound))

    /** Moves the kept session and sign-in to the other way of keeping them and forgets the old key. Meant to be called while the phone is unlocked (the person is using the app). */
    fun rebind(context: Context, turnOn: Boolean) {
        resealStore(PreferencesStore(context.getSharedPreferences("armor-session", Context.MODE_PRIVATE)), SessionVault.KEY, sessionBox(context, !turnOn), sessionBox(context, turnOn))
        resealStore(PreferencesStore(context.getSharedPreferences("armor-credentials", Context.MODE_PRIVATE)), CredentialVault.KEY, credentialBox(context, !turnOn), credentialBox(context, turnOn))
        KeystoreBox.deleteKey(alias(SESSION, !turnOn))
        KeystoreBox.deleteKey(alias(CREDENTIALS, !turnOn))
        KeyBinding(PreferencesStore(context.getSharedPreferences("armor-control", Context.MODE_PRIVATE))).unlockBound = turnOn
    }
}
