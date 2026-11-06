// ARMOR-ANDROID-CONTROL - the data classes of the shared contracts are the ones ARMOR-COMMON generates.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.contracts.Alarm
import es.electrohobby3d.armor.contracts.AlarmZone
import es.electrohobby3d.armor.contracts.ArmorPorts
import es.electrohobby3d.armor.contracts.Telemetry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArmorContractsTest {
    private val generated = File("../../ARMOR-COMMON/generated")
    private val sources = File("src/main/java/es/electrohobby3d/armor/contracts")

    private fun same(name: String) {
        val common = File(generated, name)
        if (!common.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return
        }
        assertEquals(common.readText(Charsets.UTF_8).replace("\r\n", "\n"), File(sources, name).readText(Charsets.UTF_8).replace("\r\n", "\n"))
    }

    @Test fun `the copies in the app are what ARMOR-COMMON generates now`() {
        same("ArmorContracts.kt")
        same("ArmorPorts.kt")
    }

    @Test fun `a valid message of the shared vectors fits its data class field for field`() {
        val vectors = File("../../ARMOR-COMMON/conformance/telemetry.json")
        if (!vectors.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return
        }
        val list = JSONObject(vectors.readText(Charsets.UTF_8)).getJSONArray("vectors")
        var read = 0
        for (index in 0 until list.length()) {
            val vector = list.getJSONObject(index)
            if (!vector.getBoolean("valid")) continue
            val payload = vector.getJSONObject("payload")
            val targets = payload.getJSONArray("targets")
            val message = Telemetry(
                kind = if (payload.has("kind")) payload.getString("kind") else null,
                node_id = payload.getString("node_id"), timestamp_ms = payload.getLong("timestamp_ms"), lux = payload.getDouble("lux"),
                targets = (0 until targets.length()).map { targets.getJSONObject(it) }.map {
                    es.electrohobby3d.armor.contracts.RadarTrack(it.getLong("sensor_id"), it.getLong("track_id"), it.getDouble("x_mm"), it.getDouble("y_mm"), it.getDouble("speed_mm_s"))
                },
            )
            assertEquals(vector.getString("name"), payload.getString("node_id"), message.node_id)
            assertEquals(vector.getString("name"), targets.length(), message.targets.size)
            read++
        }
        assertTrue("the vectors hold valid telemetry messages", read > 0)
    }

    @Test fun `the alarm data classes and the one numbering of ports exist`() {
        val zone = AlarmZone(id = "front", kind = "entry", state = "normal", bypassed = false)
        assertEquals("front", zone.id)
        assertEquals("alarm", Alarm::class.java.simpleName.lowercase())
        assertTrue(ArmorPorts.SERVER > 0)
    }
}
