// ARMOR-ANDROID-CONTROL - when the app asks for the lock of the phone: the person's choices, what the server may only raise, the time an unlocking counts, and never a phone shut out.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.SessionFacts
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemoryValues : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}

/** A phone that answers as told and counts the questions. */
private class FakePhone(var has: LockAvailability, var answer: AuthResult = AuthResult.Success) : DeviceAuth {
    var asked = 0
    var lastTitle = ""
    override fun availability() = has
    override suspend fun authenticate(title: String, subtitle: String): AuthResult { asked++; lastTitle = title; return answer }
}

class DeviceLockTest {
    private var now = 0L
    private val store = MemoryValues()
    private val settings = LockSettings(store)
    private fun policy(phone: FakePhone) = LockPolicy(settings, phone::availability) { now }

    @Test fun `by default the sensitive actions ask when the phone has a fingerprint or a face, and nothing else does`() {
        assertTrue(policy(FakePhone(LockAvailability.Biometric)).sensitiveRequired())
        assertFalse(policy(FakePhone(LockAvailability.DeviceCredential)).sensitiveRequired())   // the PIN alone is the person's choice
        assertFalse(policy(FakePhone(LockAvailability.None)).sensitiveRequired())
        assertFalse(policy(FakePhone(LockAvailability.Biometric)).openRequired())   // opening the app is off until the person turns it on
    }

    @Test fun `what the person chose wins over the default, and a phone with only a PIN falls back to it`() {
        settings.setSensitive(false)
        assertFalse(policy(FakePhone(LockAvailability.Biometric)).sensitiveRequired())
        settings.setSensitive(true)
        assertTrue(policy(FakePhone(LockAvailability.DeviceCredential)).sensitiveRequired())   // no fingerprint: the phone's PIN is asked
        settings.openApp = true
        assertTrue(policy(FakePhone(LockAvailability.DeviceCredential)).openRequired())
    }

    @Test fun `a phone with no lock is never shut out, whatever the person or the server asked`() {
        settings.setSensitive(true); settings.openApp = true; settings.server = ServerLock.Always
        val rules = policy(FakePhone(LockAvailability.None))
        assertFalse(rules.sensitiveRequired())
        assertFalse(rules.openRequired())
        assertTrue(rules.status().unmet)   // the screen says why nothing is asked
    }

    @Test fun `the server can only raise what the person chose`() {
        settings.setSensitive(false)
        val phone = FakePhone(LockAvailability.Biometric)
        assertFalse(policy(phone).sensitiveRequired())
        settings.server = ServerLock.Sensitive
        assertTrue(policy(phone).sensitiveRequired())
        assertFalse(policy(phone).openRequired())
        settings.server = ServerLock.Always
        assertTrue(policy(phone).openRequired())
        // and what it does not say, or an older server, leaves the person's choice alone
        settings.server = ServerLock.parse("nonsense")
        assertFalse(policy(phone).sensitiveRequired())
        assertEquals(ServerLock.User, ServerLock.parse(null))
        settings.server = ServerLock.Sensitive
        assertTrue(policy(phone).status().sensitiveForced)
        assertFalse(policy(phone).status().openForced)
    }

    @Test fun `one unlocking counts for the time the person chose and then it asks again`() = runBlocking {
        val phone = FakePhone(LockAvailability.Biometric)
        val gate = LockGate(policy(phone), phone)
        assertTrue(gate.allow("Armar el sistema"))
        assertEquals(1, phone.asked)
        now += 29_000
        assertTrue(gate.allow("Desarmar el sistema"))
        assertEquals(1, phone.asked)   // 30 seconds by default
        now += 2_000
        assertTrue(gate.allow("Armar el sistema"))
        assertEquals(2, phone.asked)
        settings.graceSeconds = 0
        assertTrue(gate.allow("Armar el sistema"))
        assertTrue(gate.allow("Armar el sistema"))
        assertEquals(4, phone.asked)   // 0 asks every time
    }

    @Test fun `a refusal stops the action, a closed question stops it too, and a phone that cannot ask lets it through`() = runBlocking {
        val phone = FakePhone(LockAvailability.Biometric, AuthResult.Failed("too many tries"))
        val gate = LockGate(policy(phone), phone)
        assertFalse(gate.allow("Armar el sistema"))
        phone.answer = AuthResult.Cancelled
        assertFalse(gate.allow("Armar el sistema"))
        phone.answer = AuthResult.Unavailable
        assertTrue(gate.allow("Armar el sistema"))
    }

    @Test fun `nothing is asked when the actions do not need the lock`() = runBlocking {
        settings.setSensitive(false)
        val phone = FakePhone(LockAvailability.Biometric)
        assertTrue(LockGate(policy(phone), phone).allow("Armar el sistema"))
        assertEquals(0, phone.asked)
    }

    @Test fun `the app locks again after the time away and not for a short look at another app`() {
        settings.openApp = true
        val rules = policy(FakePhone(LockAvailability.Biometric))
        rules.unlocked()
        rules.left(); now += 10_000
        assertFalse(rules.returned())
        rules.left(); now += 45_000
        assertTrue(rules.returned())
        assertFalse(rules.returned())   // it was said once
        settings.openApp = false
        rules.left(); now += 600_000
        assertFalse(rules.returned())   // not behind the lock: nothing to lock
    }

    @Test fun `leaving the app for the phone's own question is not leaving the app`() = runBlocking {
        settings.openApp = true
        val phone = FakePhone(LockAvailability.Biometric)
        val rules = policy(phone)
        rules.prompting = true
        rules.left(); now += 600_000
        assertFalse(rules.returned())
    }

    @Test fun `the server's choice is kept for the next start and a damaged value is ignored`() {
        settings.server = ServerLock.Always
        assertEquals(ServerLock.Always, LockSettings(store).server)
        store.values["lock_server"] = "???"
        assertEquals(ServerLock.User, LockSettings(store).server)
        store.values["lock_grace"] = "7"
        assertEquals(LockSettings.DEFAULT_GRACE, LockSettings(store).graceSeconds)
    }

    @Test fun `the session tells the role and what the server asks of the lock`() {
        val facts = SessionFacts.parse(JSONObject("""{"authenticated":true,"user":{"role":"operator"},"capabilities":{"version":1,"security":{"client.device_lock":"sensitive"}}}"""))
        assertEquals(SessionFacts("operator", "sensitive"), facts)
        assertEquals(SessionFacts("admin", "user"), SessionFacts.parse(JSONObject("""{"authenticated":true,"user":{"role":"admin"}}""")))   // an older server
        assertEquals(SessionFacts(), SessionFacts.parse(JSONObject("{}")))
    }
}
