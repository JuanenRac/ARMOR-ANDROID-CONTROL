// ARMOR-ANDROID-CONTROL - the readers of the messages that ARMOR-COMMON generates, against the oracle of the shared contracts.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.contracts.Alarm
import es.electrohobby3d.armor.contracts.Telemetry
import es.electrohobby3d.armor.contracts.checkMessage
import es.electrohobby3d.armor.contracts.parseMessage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * conformance/wire_cases.tsv of ARMOR-COMMON: one case per line (schema, 1 or 0, the document) with the verdict of the Python validator, made from the valid vectors of the contracts
 * and seeded mutations of them. The Kotlin readers must say the same on every one.
 */
class ArmorParsersOracleTest {
    private val file = File("../../ARMOR-COMMON/conformance/wire_cases.tsv")
    private val generated = File("../../ARMOR-COMMON/generated/ArmorParsers.kt")
    private val sources = File("src/main/java/es/electrohobby3d/armor/contracts")

    private class Case(val number: Int, val schema: String, val valid: Boolean, val text: String)

    private fun cases(): List<Case>? {
        if (!file.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return null
        }
        return file.readLines(Charsets.UTF_8).filter { it.isNotEmpty() }.mapIndexed { index, line ->
            val first = line.indexOf('\t')
            val second = line.indexOf('\t', first + 1)
            Case(index + 1, line.substring(0, first), line[first + 1] == '1', line.substring(second + 1))
        }
    }

    @Test fun `the copy in the app is what ARMOR-COMMON generates now`() {
        if (!generated.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return
        }
        assertEquals(generated.readText(Charsets.UTF_8).replace("\r\n", "\n"), File(sources, "ArmorParsers.kt").readText(Charsets.UTF_8).replace("\r\n", "\n"))
    }

    @Test fun `the readers say what the Python validator says on every seeded case`() {
        val all = cases() ?: return
        assertTrue("the oracle has a few hundred cases", all.size > 500)
        val disagreements = all.filter { (checkMessage(it.schema, JSONObject(it.text)) == null) != it.valid }.map { "${it.number} ${it.schema}" }
        assertEquals(emptyList<String>(), disagreements)
    }

    @Test fun `a valid case becomes its data class and an invalid one becomes nothing`() {
        val all = cases() ?: return
        val missing = all.filter { it.valid && parseMessage(it.schema, JSONObject(it.text)) == null }.map { "${it.number} ${it.schema}" }
        assertEquals(emptyList<String>(), missing)
        val extra = all.filter { !it.valid && parseMessage(it.schema, JSONObject(it.text)) != null }.map { "${it.number} ${it.schema}" }
        assertEquals(emptyList<String>(), extra)
    }

    @Test fun `the data class has what the document said`() {
        val all = cases() ?: return
        val telemetry = all.first { it.valid && it.schema == "telemetry" }
        val document = JSONObject(telemetry.text)
        val message = parseMessage("telemetry", document) as Telemetry
        assertEquals(document.getString("node_id"), message.node_id)
        assertEquals(document.getJSONArray("targets").length(), message.targets.size)
        val alarm = all.first { it.valid && it.schema == "alarm" }
        val state = parseMessage("alarm", JSONObject(alarm.text)) as Alarm
        assertEquals(JSONObject(alarm.text).getString("phase"), state.phase)
        assertEquals(JSONObject(alarm.text).getJSONArray("zones").length(), state.zones.size)
    }

    @Test fun `an unknown name is nothing and a missing member is said`() {
        assertNull(parseMessage("not_a_message", JSONObject("{}")))
        val error = checkMessage("telemetry", JSONObject("{}"))
        assertNotNull(error)
        assertTrue(error!!.path.startsWith("/"))
        assertEquals("is required", error.detail)
    }
}
