// ARMOR-ANDROID-CONTROL - the readers of the screens against the readers generated from the shared contracts: they must not drift apart.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.contracts.NodeHealth
import es.electrohobby3d.armor.contracts.parseMessage
import es.electrohobby3d.armor.model.NodeHealthParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The screens read the views the server builds with readers that are TOLERANT on purpose (a field they do not know is not an error, a missing optional one is a default): the server has
 * already checked the data, and a phone that refused what the server accepted would show nothing. The readers generated from the contracts are strict. This test keeps the two from
 * drifting: the same message of the shared oracle goes through both, and the fields the screen shows must be the ones the strict reader found. A field renamed in the contract, or a
 * screen that reads another one, fails here.
 */
class ModelDriftTest {
    private val file = File("../../ARMOR-COMMON/conformance/wire_cases.tsv")

    private fun valid(schema: String): List<JSONObject>? {
        if (!file.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return null
        }
        return file.readLines(Charsets.UTF_8).filter { it.isNotEmpty() }.mapNotNull { line ->
            val first = line.indexOf('\t')
            val second = line.indexOf('\t', first + 1)
            if (line.substring(0, first) == schema && line[first + 1] == '1') JSONObject(line.substring(second + 1)) else null
        }
    }

    @Test fun `the health screen reads what the strict reader read on every valid node health message`() {
        val cases = valid("node_health") ?: return
        assertTrue("the oracle has several valid node health messages", cases.size >= 5)
        for ((index, report) in cases.withIndex()) {
            val strict = parseMessage("node_health", report) as NodeHealth
            val view = NodeHealthParser.overview(JSONObject().put("nodes", JSONArray().put(JSONObject()
                .put("node_id", strict.node_id).put("family", strict.family).put("firmware", strict.firmware).put("status", "ok").put("received_at", "2026-01-01T00:00:00Z")
                .put("report", report)))).nodes
            assertEquals("case $index is shown", 1, view.size)
            val shown = view[0]
            val where = "case $index of node_health"
            assertEquals(where, strict.node_id, shown.nodeId)
            assertEquals(where, strict.family, shown.family)
            assertEquals(where, strict.firmware, shown.firmware)
            assertEquals(where, strict.uptime_s, shown.uptimeSeconds)
            assertEquals(where, strict.reset_reason, shown.resetReason)
            assertEquals(where, strict.heap_free, shown.heapFree)
            assertEquals(where, strict.heap_min, shown.heapMin)
            assertEquals(where, strict.link, shown.link)
            assertEquals(where, strict.link_up, shown.linkUp)
            assertEquals(where, strict.rssi_dbm?.toInt(), shown.rssiDbm)
            assertEquals(where, strict.broker_reconnects, shown.brokerConnections)
            assertEquals(where, (strict.low_stack_tasks ?: 0L).toInt(), shown.lowStackTasks)
            assertEquals(where, strict.last_error?.takeIf { it.isNotBlank() }, shown.lastError)
        }
    }

    @Test fun `the health screen never fails on a message the strict reader refuses`() {
        if (!file.isFile) return
        val refused = file.readLines(Charsets.UTF_8).filter { it.isNotEmpty() }.mapNotNull { line ->
            val first = line.indexOf('\t')
            val second = line.indexOf('\t', first + 1)
            if (line.substring(0, first) == "node_health" && line[first + 1] == '0') line.substring(second + 1) else null
        }
        assertTrue(refused.isNotEmpty())
        for (text in refused) {
            val report = runCatching { JSONObject(text) }.getOrNull() ?: continue
            assertNotNull(NodeHealthParser.overview(JSONObject().put("nodes", JSONArray().put(JSONObject().put("node_id", "n1").put("report", report)))))   // tolerant: no exception, whatever it shows
        }
    }
}
