package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.NetworkParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerTest {
    @Test fun onlyAViewerIsReadOnly() {
        assertTrue(MonitorUiState(role = "viewer").readOnly)
        assertFalse(MonitorUiState(role = "operator").readOnly)
        assertFalse(MonitorUiState(role = "admin").readOnly)
        assertFalse(MonitorUiState().readOnly)   // a server that does not say the role leaves the person all the buttons: the server refuses what is not allowed
    }

    @Test fun theNetworkNodeTellsThePublicAddress() {
        val node = """{"node_id":"net-1","stale":false,"interface":{"name":"eth0","ip":"192.168.0.10","cidr":"192.168.0.0/24"},"internet":{"state":"up"},"devices":[],
            "public":{"ip":"203.0.113.9","city":"Exampletown","region":"Murcia","country":"ES","org":"AS12430 VODAFONE ESPANA S.A.U.","timezone":"Europe/Madrid","checked_ms":1}}"""
        val info = NetworkParser.overview(JSONObject("""{"nodes":[$node],"totals":{},"events":[],"outages":[]}""")).nodes.single().publicInfo!!
        assertEquals("203.0.113.9", info.ip)
        assertEquals("Exampletown", info.city)
        assertEquals("ES", info.country)
        assertEquals("AS12430 VODAFONE ESPANA S.A.U.", info.org)
        val without = node.replace(Regex(""",\s*"public":\{.*\}\}\s*$""", RegexOption.DOT_MATCHES_ALL), "}")
        assertNull(NetworkParser.overview(JSONObject("""{"nodes":[$without],"totals":{},"events":[],"outages":[]}""")).nodes.single().publicInfo)
    }
}
