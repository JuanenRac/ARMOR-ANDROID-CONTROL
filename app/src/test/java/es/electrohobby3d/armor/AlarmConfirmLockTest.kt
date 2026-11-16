// ARMOR-ANDROID-CONTROL - the "Confirm" action of an alarm notice asks for the phone to be unlocked, from Android 12.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmConfirmLockTest {
    @Test fun `the unlock is enforced from Android 12 when the person wants it`() {
        assertTrue(AlarmConfirmLock.enforced(required = true, sdk = 31))
        assertTrue(AlarmConfirmLock.enforced(required = true, sdk = 35))
    }

    @Test fun `it is not enforced when the person turned it off or the system cannot`() {
        assertFalse(AlarmConfirmLock.enforced(required = false, sdk = 35))
        assertFalse(AlarmConfirmLock.enforced(required = true, sdk = 30))
    }
}
