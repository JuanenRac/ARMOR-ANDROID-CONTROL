// ARMOR-ANDROID-CONTROL - the reminder on leaving home: what it says and when it keeps quiet.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceReminderTest {
    private val now = 10_000_000_000L

    @Test fun `with the alarm armed the phone leaving home says nothing`() {
        assertNull(PresenceDecision.onLeft("armed", 0L, now))
    }

    @Test fun `with the alarm disarmed it reminds the person to arm`() {
        assertEquals(Reminder.ARM_NOW, PresenceDecision.onLeft("disarmed", 0L, now))
    }

    @Test fun `when the server cannot be read it asks the person to check rather than staying silent`() {
        assertEquals(Reminder.CHECK, PresenceDecision.onLeft(null, 0L, now))
        assertEquals(Reminder.CHECK, PresenceDecision.onLeft("something else", 0L, now))
    }

    @Test fun `after a reminder the next one waits for the quiet time and then comes again`() {
        val last = now - PresenceConfig.COOLDOWN_MS + 1
        assertNull(PresenceDecision.onLeft("disarmed", last, now))
        assertEquals(Reminder.ARM_NOW, PresenceDecision.onLeft("disarmed", now - PresenceConfig.COOLDOWN_MS, now))
    }

    @Test fun `a clock that went back does not keep the reminder quiet for ever`() {
        assertEquals(Reminder.ARM_NOW, PresenceDecision.onLeft("disarmed", now + 5_000L, now))
    }

    @Test fun `only a real place is a home`() {
        assertTrue(HomePlace(38.0, -1.1).valid)
        assertFalse(HomePlace(0.0, 0.0).valid)
        assertFalse(HomePlace(91.0, 0.5).valid)
        assertFalse(HomePlace(10.0, 181.0).valid)
    }

    @Test fun `the numbers make sense`() {
        assertTrue(PresenceConfig.RADIUS_M >= 50f)
        assertTrue(PresenceConfig.COOLDOWN_MS >= 60_000L)
    }

    @Test fun `the transitions of the circle are the numbers of Google Play services`() {
        assertEquals(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER, PresenceShare.ENTER)
        assertEquals(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT, PresenceShare.EXIT)
    }

    @Test fun `entering the circle says home, leaving says away and nothing else says anything`() {
        assertEquals("home", PresenceShare.wordFor(PresenceShare.ENTER))
        assertEquals("away", PresenceShare.wordFor(PresenceShare.EXIT))
        assertNull(PresenceShare.wordFor(4))
        assertNull(PresenceShare.wordFor(0))
    }

    @Test fun `with the word off the circle is watched only for leaving and only for the reminder`() {
        assertEquals(PresenceShare.EXIT, PresenceShare.transitions(reminder = true, share = false))
        assertEquals(0, PresenceShare.transitions(reminder = false, share = false))
        assertEquals(0, PresenceShare.initialTrigger(share = false))
    }

    @Test fun `with the word on both sides are watched and the side the phone is on is announced at once`() {
        assertEquals(PresenceShare.EXIT or PresenceShare.ENTER, PresenceShare.transitions(reminder = false, share = true))
        assertEquals(PresenceShare.EXIT or PresenceShare.ENTER, PresenceShare.transitions(reminder = true, share = true))
        assertEquals(PresenceShare.EXIT or PresenceShare.ENTER, PresenceShare.initialTrigger(share = true))
    }

    @Test fun `the circle is watched only with something on and a place saved`() {
        assertFalse(PresenceShare.watching(reminder = false, share = false, hasHome = true))
        assertFalse(PresenceShare.watching(reminder = true, share = true, hasHome = false))
        assertTrue(PresenceShare.watching(reminder = true, share = false, hasHome = true))
        assertTrue(PresenceShare.watching(reminder = false, share = true, hasHome = true))
    }
}
