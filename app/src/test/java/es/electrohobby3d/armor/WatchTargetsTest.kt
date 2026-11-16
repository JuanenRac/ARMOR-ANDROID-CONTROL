// ARMOR-ANDROID-CONTROL - which nodes a message to the watch goes to: the ones that declare the capability of the watch app, else the connected ones.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchTargetsTest {
    @Test fun `the watches that declare the capability win over the connected ones`() {
        assertEquals(listOf("watch"), WatchRelay.targets(listOf("watch"), listOf("watch", "tablet")))
    }

    @Test fun `without a node that declares it the connected nodes are used and with nobody there is nobody`() {
        assertEquals(listOf("tablet"), WatchRelay.targets(emptyList<String>(), listOf("tablet")))
        assertEquals(emptyList<String>(), WatchRelay.targets(emptyList<String>(), emptyList()))
    }

    @Test fun `the capability names are the ones the watch app uses`() {
        assertEquals("armor_phone_alerts", WatchRelay.CAPABILITY_PHONE)
        assertEquals("armor_watch_alerts", WatchRelay.CAPABILITY_WATCH)
    }
}
