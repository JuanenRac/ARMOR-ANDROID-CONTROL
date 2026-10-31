// ARMOR-ANDROID-CONTROL - the facts an alarm carries, with the label each has in the table every client shares.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.DeviceParser
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AlarmFactsTest {
    private val languages = listOf("en", "es", "de", "fr", "it", "ja", "zh", "pt")
    private val asset = File("src/main/assets/alarm_facts.json")
    private val common = File("../../ARMOR-COMMON/src/armor_common/alarm_facts.json")
    private val table get() = AlarmFacts.parse(asset.readText(Charsets.UTF_8))

    @After fun reset() { AlarmFacts.use(emptyMap()); Tr.useTable("es", emptyMap()) }

    private fun alarm(detail: String) = DeviceParser.alarm(JSONObject("""{"id":"a1","source":{"type":"alarm","id":"mode_panel"},"severity":"high","code":"mode_panel_sync_failed","raised_at":"2026-01-01T00:00:00.000Z","detail":$detail}"""))!!

    @Test fun `every fact has its label in the eight languages`() {
        assertTrue(table.size >= 50)
        for ((key, labels) in table) for (language in languages) assertTrue("$key in $language", !labels[language].isNullOrBlank())
    }

    @Test fun `the copy in the assets is the table of ARMOR-COMMON`() {
        if (!common.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return
        }
        assertEquals(common.readText(Charsets.UTF_8).replace("\r\n", "\n"), asset.readText(Charsets.UTF_8).replace("\r\n", "\n"))
    }

    @Test fun `the facts of a panel that does not confirm the arming mode come with their labels in order`() {
        AlarmFacts.use(table)
        val a = alarm("""{"by":"voice","panel_answers":"nodeA: timeout","panels":"nodeA, nodeB","panel_profile":"away","arming_mode":"away"}""")
        val rows = AlarmFacts.rows(a.detail, "pt")
        assertEquals(listOf("arming_mode", "panel_profile", "panels", "panel_answers", "by"), rows.map { it.key })
        assertEquals(listOf("Modo de armamento escolhido", "Perfil pedido às centrais", "Centrais que não confirmaram", "O que cada central respondeu", "Escolhido por"), rows.map { it.label })
        assertEquals("Modo de armado elegido", AlarmFacts.rows(a.detail, "es").first().label)
        assertEquals(listOf("away", "away", "nodeA, nodeB", "nodeA: timeout", "voice"), rows.map { it.value })
        assertTrue(rows.all { it.known })
    }

    @Test fun `a label comes in the language asked and English when that language lacks it`() {
        AlarmFacts.use(mapOf("ip" to mapOf("en" to "Address (IP)", "de" to "Adresse (IP)")))
        assertEquals("Adresse (IP)", AlarmFacts.label("ip", "de"))
        assertEquals("Address (IP)", AlarmFacts.label("ip", "ja"))
        assertEquals(null, AlarmFacts.label("not_a_fact", "en"))
    }

    @Test fun `a fact the table does not know is shown as it came and no fact is lost`() {
        AlarmFacts.use(table)
        val a = alarm("""{"zzz_new":"x","ip":"10.0.0.2","list":[1,2],"nested":{"a":1},"nothing":null,"count":3,"ratio":2.5,"flag":true}""")
        assertEquals(8, a.detail.size)
        val rows = AlarmFacts.rows(a.detail, "en")
        assertEquals(8, rows.size)
        assertEquals("ip", rows.first().key)
        val unknown = rows.filter { !it.known }.associate { it.key to it.label }
        assertEquals(setOf("zzz_new", "list", "nested", "nothing", "count", "ratio", "flag"), unknown.keys)
        assertTrue(unknown.all { it.key == it.value })
        assertEquals("[1,2]", rows.first { it.key == "list" }.value)
        assertEquals("3", rows.first { it.key == "count" }.value)
        assertEquals("2.5", rows.first { it.key == "ratio" }.value)
    }

    @Test fun `with no table every fact is still listed with its key`() {
        AlarmFacts.use(emptyMap())
        val rows = AlarmFacts.rows(alarm("""{"device":"cam1","port":22}""").detail, "en")
        assertEquals(listOf("device" to "cam1", "port" to "22"), rows.map { it.label to it.value })
        assertFalse(rows.any { it.known })
    }

    @Test fun `yes and no, the units and the dates are told as a person reads them`() {
        Tr.useTable("en", mapOf("sí" to "yes", "no" to "no"))
        assertEquals("yes", AlarmFacts.value("online", true))
        assertEquals("no", AlarmFacts.value("risky", false))
        assertEquals("12 ms", AlarmFacts.value("latency_ms", 12.0))
        assertEquals("5 %", AlarmFacts.value("loss_percent", 5))
        assertEquals("not a date", AlarmFacts.value("since", "not a date"))
        assertEquals(Friendly.dayAndClock("2026-01-01T10:00:00Z"), AlarmFacts.value("opened_at", "2026-01-01T10:00:00Z"))
    }

    @Test fun `an alarm without details has an empty list and keeps being an alarm`() {
        val a = DeviceParser.alarm(JSONObject("""{"id":"a2","source":{"type":"device","id":"d1"},"severity":"critical","code":"smoke","raised_at":"x"}"""))!!
        assertNotNull(a)
        assertTrue(a.detail.isEmpty())
        assertTrue(AlarmFacts.rows(a.detail, "en").isEmpty())
    }
}
