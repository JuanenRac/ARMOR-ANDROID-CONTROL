// ARMOR-ANDROID-CONTROL - the tolerant reader of the shared contract: a member the app does not know is left out, a document that is really wrong still gets nothing.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.contracts.Alarm
import es.electrohobby3d.armor.contracts.MESSAGE_NAMES
import es.electrohobby3d.armor.contracts.checkMessage
import es.electrohobby3d.armor.contracts.messageSchema
import es.electrohobby3d.armor.contracts.parseMessage
import es.electrohobby3d.armor.contracts.parseTolerant
import es.electrohobby3d.armor.contracts.withoutUnknownMembers
import es.electrohobby3d.armor.model.ConnectionsParser
import es.electrohobby3d.armor.model.ReportsParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContractTolerantTest {
    private val file = File("../../ARMOR-COMMON/conformance/wire_cases.tsv")

    private val client = """{"id":"0123456789abcdef","kind":"studio","user":"ana","first_seen":"2026-01-01T10:00:00.000Z","last_seen":"2026-01-02T10:00:00.000Z","active_sessions":1,"logins_ok":3,"logins_failed":2}"""
    private val summary = """{"date":"2026-03-05","language":"es","summary":{"schema":"armor-daily-summary/1","totals":{"events":7,"alarms":5,"serious_alarms":2,"false_alarms":1},
        "by_type":{"alarm":5,"reading":2},"by_zone":{},"by_hour":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]},
        "findings":[{"kind":"hourly_repeat","subject":{"code":"intrusion","zone":"hall"},"count":3,"detail":{},"evidence":[]}],"text":"Un dia tranquilo.","ai_note":"Mira el jardin."}"""
    private val heatmap = """{"schema":"armor.vision.heatmap/v1","camera_id":"cam-front","cols":2,"rows":1,"accepted_positions":12,"rejected_positions":0,"total":12,"max":8,"weights":[[4,8]],"normalized":[[0.5,1]],"contains_images":false}"""

    private fun valid(): List<Pair<String, JSONObject>>? {
        if (!file.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return null
        }
        return file.readLines(Charsets.UTF_8).filter { it.isNotEmpty() }.map { line ->
            val first = line.indexOf('\t')
            val second = line.indexOf('\t', first + 1)
            Triple(line.substring(0, first), line[first + 1] == '1', line.substring(second + 1))
        }.filter { it.second }.map { it.first to JSONObject(it.third) }
    }

    @Test fun `a member the contract does not name is left out of a closed object`() {
        val document = JSONObject(client).put("zz_new_member", 1)
        val cleaned = withoutUnknownMembers("connection_client", document)!!
        assertTrue(!cleaned.has("zz_new_member"))
        assertEquals(JSONObject(client).toString().length, cleaned.toString().length)
        assertNull("the strict check still refuses the member", parseMessage("connection_client", document))
        assertNotNull(parseTolerant("connection_client", document))
    }

    @Test fun `the document given is never changed`() {
        val document = JSONObject(client).put("zz_new_member", JSONObject().put("deep", 1))
        val before = document.toString()
        withoutUnknownMembers("connection_client", document)
        assertEquals(before, document.toString())
    }

    @Test fun `a wrong value a missing member or a bad pattern still fail`() {
        assertNull(parseTolerant("connection_client", JSONObject(client).put("active_sessions", -1)))
        assertNull(parseTolerant("connection_client", JSONObject(client).put("id", "not-hex")))
        assertNull(parseTolerant("connection_client", JSONObject(client).put("kind", "toaster")))
        assertNull(parseTolerant("connection_client", JSONObject(client).also { it.remove("first_seen") }))
        assertNull(parseTolerant("connection_client", JSONObject(client).put("active_sessions", "1")))
        assertNull(parseTolerant("connection_client", JSONObject()))
    }

    @Test fun `an unknown message gives nothing instead of failing`() {
        assertNull(messageSchema("no_such_message"))
        assertNull(withoutUnknownMembers("no_such_message", JSONObject()))
        assertNull(parseTolerant("no_such_message", JSONObject()))
    }

    @Test fun `every message has a schema and its name is in the list`() {
        assertTrue(MESSAGE_NAMES.size > 30)
        for (name in MESSAGE_NAMES) assertNotNull(name, messageSchema(name))
    }

    @Test fun `members unknown at every depth are left out of a nested closed object`() {
        val document = JSONObject(summary)
        document.getJSONArray("findings").getJSONObject(0).getJSONObject("subject").put("zz_new_member", true)
        document.getJSONArray("findings").getJSONObject(0).put("zz_other_member", "x")
        assertNotNull(parseTolerant("daily_summary", document))
    }

    @Test fun `the members an open object may carry are kept`() {
        val document = JSONObject(summary)
        document.getJSONObject("summary").getJSONObject("by_type").put("a_new_type", 9)
        val cleaned = withoutUnknownMembers("daily_summary", document)!!
        assertEquals(9, cleaned.getJSONObject("summary").getJSONObject("by_type").getInt("a_new_type"))
    }

    @Test fun `the connections of a newer server are still read through the contract`() {
        val list = ConnectionsParser.list(JSONObject("""{"clients":[${client.dropLast(1)},"zz_new_member":"x"},${client.replace("0123456789abcdef", "fedcba9876543210")}]}"""))
        assertEquals(listOf("0123456789abcdef", "fedcba9876543210"), list.map { it.id })
        assertEquals("ana", list.first().user)
        assertEquals(3, list.first().loginsOk)
    }

    @Test fun `the summary of a newer server is read through the contract`() {
        val document = JSONObject(summary).put("zz_new_member", 1)
        val read = ReportsParser.summary(document)
        assertEquals(listOf(7, 5, 2, 1), listOf(read.events, read.alarms, read.serious, read.falseAlarms))
        assertEquals("Mira el jardin.", read.aiNote)
        assertEquals(listOf("intrusion / hall"), read.findings.map { it.subject })
        assertEquals(listOf(3), read.findings.map { it.count })
    }

    @Test fun `the heat map of a newer server is read through the contract`() {
        val read = ReportsParser.heatmap(JSONObject(heatmap).put("zz_new_member", 1), byteArrayOf(1))
        assertEquals(listOf("cam-front", 2, 1, 12), listOf(read.cameraId, read.cols, read.rows, read.positions))
    }

    @Test fun `a summary the contract does not describe still shows what it has`() {
        val read = ReportsParser.summary(JSONObject("""{"date":"2026-03-05","language":"en","text":"x","summary":{"totals":{"events":4}}}"""))
        assertEquals(4, read.events)
        assertEquals("x", read.text)
    }

    @Test fun `every valid case of the oracle given a member it does not know is read as it was`() {
        val all = valid() ?: return
        var tried = 0
        for ((name, document) in all) {
            val schema = messageSchema(name) ?: continue
            if (schema.opt("additionalProperties") != false) continue
            val extra = JSONObject(document.toString()).put("zz_new_member", JSONArray().put(1))
            val cleaned = withoutUnknownMembers(name, extra)
            assertNotNull(name, cleaned)
            assertEquals(name, document.toString().length, cleaned.toString().length)
            assertNull(name, checkMessage(name, cleaned))
            assertNotNull(name, parseTolerant(name, extra))
            tried++
        }
        assertTrue("the oracle holds many closed messages", tried > 100)
    }

    @Test fun `a valid alarm read tolerantly is the alarm read strictly`() {
        val all = valid() ?: return
        val (_, document) = all.first { it.first == "alarm" }
        val strict = parseMessage("alarm", document) as Alarm
        assertEquals(strict, parseTolerant("alarm", JSONObject(document.toString()).put("zz_new_member", 1)))
    }
}
