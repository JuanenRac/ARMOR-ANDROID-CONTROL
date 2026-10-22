// ARMOR-ANDROID-CONTROL - the app against what a real ARMOR-SERVER answered (captured with tools/capture_fixtures.mjs from a server fed by ARMOR-SIMULATOR).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApi
import es.electrohobby3d.armor.network.ArmorApiClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** A server that answers each route with the file captured from the real one: when the contract changes, the captured files change and these tests say what broke. */
class ServerFixturesTest {
    private lateinit var server: FakeServer
    private lateinit var api: ArmorApi
    private val origin get() = server.origin

    private fun fixture(name: String): String = File("src/test/resources/fixtures/$name.json").readText(Charsets.UTF_8)

    @Before fun start() {
        server = FakeServer()
        val routes = mapOf("/api/v1/status" to "status", "/api/v1/alarms" to "alarms", "/api/v1/history" to "history", "/api/v1/camera-views" to "camera_views", "/api/v1/media" to "media", "/api/v1/solar" to "solar")
        server.respond = { seen -> routes[seen.path]?.let { Triple(200, fixture(it), mapOf("Content-Type" to "application/json")) } ?: Triple(404, "{}", emptyMap()) }
        api = ArmorApiClient()
    }
    @After fun stop() { server.stop() }

    @Test fun `the fixtures are the answers of a server and none of them holds a real date`() {
        for (name in listOf("status", "alarms", "history", "camera_views", "media", "solar", "node_health")) {
            val text = fixture(name)
            assertTrue(name, text.trim().startsWith("{"))
            assertFalse("$name has a date of today", Regex("\"20(2[0-9])-\\d\\d-\\d\\dT").containsMatchIn(text))
        }
    }

    @Test fun `the status carries the node that the simulator fed, in high alert with its two targets and silent since`() {
        val snapshot = api.status(origin)
        assertEquals("armed", snapshot.mode)
        assertTrue(snapshot.revision > 0)
        val node = snapshot.nodes.single { it.id == "simulator-1" }
        assertFalse(node.online)   // the capture was taken after the simulator stopped: the server had already marked the node silent, as the alarm of the node says
        assertTrue(node.stale)
        assertEquals("high", node.alert)
        assertEquals(2, node.targets.size)
    }

    @Test fun `the alarms are the ones the server raised, active and not yet confirmed`() {
        val (active, recent) = api.alarms(origin)
        assertEquals(listOf("intrusion", "node_down"), active.map { it.code }.sorted())
        assertTrue(active.all { !it.acknowledged && !it.cleared })
        assertTrue(recent.isEmpty())
        assertEquals("high", active.single { it.code == "intrusion" }.severity)
    }

    @Test fun `the banner of the open app picks the intrusion out of the alarms of the server`() {
        val (active, _) = api.alarms(origin)
        val banner = AlarmBannerPolicy.from(active, null)
        assertNotNull(banner)
        assertEquals("intrusion", banner!!.alarm.code)
        assertEquals(2, banner.count)
    }

    @Test fun `the history reads every kind of event the server wrote, newest first`() {
        val events = api.history(origin)
        assertEquals(listOf("alarm", "node", "alarm", "alert", "alert", "node", "mode"), events.map { it.type })
        assertTrue(events.zipWithNext().all { (a, b) -> a.id > b.id })
        assertEquals("high", events.first { it.type == "alert" }.to)
    }

    @Test fun `a house without cameras or recordings reads as empty and not as an error`() {
        assertTrue(api.cameraViews(origin).isEmpty())
        assertTrue(api.media(origin).items.isEmpty())
    }

    @Test fun `the solar overview reads the inverter and the battery stack of the simulator`() {
        val solar = api.solar(origin)
        assertTrue(solar.devices.any { it.isInverter })
        val battery = solar.devices.first { it.battery != null }.battery!!
        assertEquals(2, battery.modules)
        assertEquals(2, battery.stack.size)
    }

    @Test fun `every call of the app is one that the fixtures know`() {
        api.status(origin); api.alarms(origin); api.history(origin); api.cameraViews(origin); api.media(origin); api.solar(origin)
        assertEquals(6, server.seen.size)
        assertTrue(server.seen.all { it.path.startsWith("/api/v1/") })
    }
}
