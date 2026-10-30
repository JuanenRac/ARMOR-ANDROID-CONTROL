package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The background watch looks at the server slowly while the push alerts work and quickly when they do not. */
class WatchPaceTest {
    private val now = 10_000_000_000L
    private val working = PushHealth(available = true, registeredAt = now - 3_600_000L, lastMissAt = 0L)

    @Test fun asksQuicklyWithoutAPushThatWorks() {
        assertEquals(WatchPace.FAST_MS, WatchPace.intervalMs(PushHealth(false, 0L, 0L), 120, now))
        assertEquals("a build without the Firebase file or a phone without a token", WatchPace.FAST_MS, WatchPace.intervalMs(working.copy(available = false), 120, now))
        assertEquals("the server never took the token", WatchPace.FAST_MS, WatchPace.intervalMs(working.copy(registeredAt = 0L), 120, now))
    }

    @Test fun asksSlowlyWhileThePushWorksAndAsChosen() {
        assertEquals(120_000L, WatchPace.intervalMs(working, 120, now))
        for (seconds in WatchPace.SLOW_CHOICES_S) assertEquals(seconds * 1000L, WatchPace.intervalMs(working, seconds, now))
        assertEquals("a value that is not one of the choices is the default", WatchPace.SLOW_DEFAULT_S * 1000L, WatchPace.intervalMs(working, 7, now))
        assertEquals("choosing 10 s keeps the usual pace", WatchPace.FAST_MS, WatchPace.intervalMs(working, 10, now))
    }

    @Test fun aMissedEventBringsTheFastPaceBackForAWhile() {
        val missed = working.copy(lastMissAt = now - 60_000L)
        assertEquals(WatchPace.FAST_MS, WatchPace.intervalMs(missed, 600, now))
        assertEquals(WatchPace.FAST_MS, WatchPace.intervalMs(missed, 600, now + WatchPace.MISS_MEMORY_MS - 61_000L))
        assertEquals("and the push is trusted again afterwards", 600_000L, WatchPace.intervalMs(missed, 600, now + WatchPace.MISS_MEMORY_MS))
    }

    @Test fun onlyAnOldEventFoundByTheWatchIsAMiss() {
        assertFalse("an event of seconds ago: the push may still be on its way", WatchPace.isMiss(working, 5_000L, now))
        assertTrue(WatchPace.isMiss(working, WatchPace.MISS_AFTER_MS + 1, now))
        assertFalse("with no push that works there is nothing to miss", WatchPace.isMiss(working.copy(registeredAt = 0L), 600_000L, now))
    }

    @Test fun anEventTimeThatCannotBeReadIsNeverAMiss() {
        assertEquals(0L, AlarmNotifier.eventAgeMs("not a time", now))
        assertEquals(90_000L, AlarmNotifier.eventAgeMs(java.time.Instant.ofEpochMilli(now - 90_000L).toString(), now))
    }
}
