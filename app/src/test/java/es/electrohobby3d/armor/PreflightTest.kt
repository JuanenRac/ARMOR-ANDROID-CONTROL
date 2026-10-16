package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.PreflightParser
import es.electrohobby3d.armor.model.PreflightText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreflightTest {
    @Test fun aQuietPerimeterIsReady() {
        val check = PreflightParser.parse(JSONObject("""{"ready":true,"issues":[]}"""))
        assertTrue(check.ready)
        assertEquals(0, check.blocking)
    }

    @Test fun blockingFindingsStopAndWarningsOnlyInform() {
        val check = PreflightParser.parse(JSONObject("""{"ready":false,"issues":[
            {"kind":"zone","level":"block","code":"zone_open","id":"alarm-1","detail":"z3"},
            {"kind":"camera","level":"warn","code":"camera_down","id":"cam-02"},
            {"kind":"node","level":"block","code":"node_offline","id":null},
            {"kind":"x","level":"block","code":"","id":"a"}]}"""))
        assertFalse(check.ready)
        assertEquals(2, check.issues.count { it.blocks })
        assertEquals(3, check.issues.size)               // the one without a code is dropped
        assertEquals("La zona z3 de la central alarm-1 está abierta.", PreflightText.describe(check.issues[0]))
        assertEquals("La cámara cam-02 no responde.", PreflightText.describe(check.issues[1]))
    }

    @Test fun aReadyFlagWithABlockingFindingIsNotReady() {
        val check = PreflightParser.parse(JSONObject("""{"ready":true,"issues":[{"kind":"alarm","level":"block","code":"alarm_unseen","id":"a1"}]}"""))
        assertFalse(check.ready)
    }
}
