package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.FieldNode
import es.electrohobby3d.armor.model.NodeCensus
import es.electrohobby3d.armor.model.NodeHealthOverview
import es.electrohobby3d.armor.model.NodeHealthParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The count of the nodes of every kind: the radar stream and the health every node reports, together. */
class NodeCensusTest {
    private fun radar(id: String, online: Boolean) = FieldNode(id = id, online = online, lux = null, tracks = 0, alert = "none")

    private fun health(vararg nodes: Triple<String, String, Boolean>): NodeHealthOverview {
        val items = nodes.joinToString(",") { (id, family, stale) ->
            """{"node_id":"$id","family":"$family","firmware":"1.0.0","received_at":"2026-01-01T10:00:00.000Z","stale":$stale,"status":"${if (stale) "stale" else "ok"}","warnings":[],"restarts_24h":0,
               "report":{"node_id":"$id","uptime_s":10,"reset_reason":"power_on","heap_free":100000,"heap_min":90000,"link":"wifi","link_up":true,"broker_reconnects":0}}"""
        }
        return NodeHealthParser.overview(JSONObject("""{"nodes":[$items],"waiting":[],"totals":{"nodes":${nodes.size},"stale":0,"with_warnings":0}}"""))
    }

    @Test fun onlyTheRadarNodesWhenTheHealthIsNotKnownYet() {
        val census = NodeCensus.of(listOf(radar("r1", true), radar("r2", false)), null)
        assertEquals(1, census.online)
        assertEquals(2, census.total)
        assertEquals(listOf("radar"), census.families.map { it.family })
        assertEquals(listOf("r2"), census.silent.map { it.id })
    }

    @Test fun everyKindOfNodeThatReportsItsHealthIsCounted() {
        val census = NodeCensus.of(
            listOf(radar("r1", true)),
            health(Triple("r1", "radar", false), Triple("s1", "solar", false), Triple("e1", "electrical", false), Triple("a1", "alarm", false), Triple("h1", "hmi", true)),
        )
        assertEquals(4, census.online)
        assertEquals(5, census.total)
        assertEquals(listOf("radar", "solar", "electrical", "alarm", "hmi"), census.families.map { it.family })
        assertEquals(listOf("h1"), census.silent.map { it.id })
    }

    @Test fun aNodeInBothListsIsCountedOnceAndIsOnlineWhenEitherSaysSo() {
        assertEquals(1, NodeCensus.of(listOf(radar("r1", false)), health(Triple("r1", "radar", false))).online)
        assertEquals(1, NodeCensus.of(listOf(radar("r1", true)), health(Triple("r1", "radar", true))).online)
        assertEquals(1, NodeCensus.of(listOf(radar("r1", true)), health(Triple("r1", "radar", true))).total)
    }

    @Test fun nothingIsCountedWithNothing() {
        val census = NodeCensus.of(emptyList(), null)
        assertEquals(0, census.total)
        assertTrue(census.families.isEmpty())
    }
}
