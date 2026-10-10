package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.AlarmPanelText
import es.electrohobby3d.armor.model.AlarmPanelsParser
import es.electrohobby3d.armor.model.AlarmTotals
import es.electrohobby3d.armor.model.DeviceText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Alarm panels screen reads what GET /api/v1/alarm/nodes and /api/v1/alarm/commands answer; these are the shapes the server produces. */
class AlarmPanelsTest {
    private val answer = """
    {"nodes":[
      {"node_id":"alarm-1","received_at":"2026-01-01T10:00:00.000Z","stale":false,
       "state":{"kind":"alarm","node_id":"alarm-1","timestamp_ms":5000,"phase":"alarm","mode":"away","siren":true,"locked_out":false,"commands_enabled":true,
         "zones":[{"id":"front-door","name":"Puerta de entrada","kind":"entry","state":"triggered","bypassed":false},
                  {"id":"window","kind":"instant","state":"normal","bypassed":true},
                  {"id":"smoke","kind":"always","state":"tamper","bypassed":false},
                  {"name":"sin id","kind":"instant","state":"normal","bypassed":false}],
         "open_zones":["front-door"],
         "events":[{"ago_s":40,"kind":"armed"},{"ago_s":3,"kind":"alarm","zone":"front-door"}]}},
      {"node_id":"alarm-2","received_at":"2026-01-01T09:00:00.000Z","stale":true,"state":{"phase":"disarmed","mode":"disarmed","commands_enabled":false,"zones":[],"open_zones":[],"events":[]}},
      {"node_id":"","stale":false,"state":{}},
      {"node_id":"alarm-3","stale":false}
    ],
    "totals":{"nodes":2,"stale":1,"armed":0,"sounding":1}}
    """.trimIndent()

    @Test fun theOverviewIsReadWithItsPanelsZonesAndTotals() {
        val overview = AlarmPanelsParser.overview(JSONObject(answer))
        assertEquals(2, overview.nodes.size)                        // a node with no id and one with no state are left out
        assertFalse(overview.isEmpty)
        assertEquals(AlarmTotals(2, 1, 0, 1), overview.totals)
        val panel = overview.nodes[0]
        assertEquals("alarm-1", panel.nodeId)
        assertTrue(panel.sounding && panel.siren && !panel.guarding && panel.commandsEnabled && !panel.lockedOut)
        assertEquals(3, panel.zones.size)                           // a zone with no id is left out
        assertEquals("Puerta de entrada", panel.zones[0].title)
        assertEquals("window", panel.zones[1].title)                // no name: the identifier
        assertTrue(panel.zones[1].bypassed)
        assertEquals("tamper", panel.zones[2].state)
        assertEquals(listOf("front-door"), panel.openZones)
        assertEquals(2, panel.events.size)
        assertEquals("front-door", panel.events[1].zone)
        assertNull(panel.events[0].zone)
        val quiet = overview.nodes[1]
        assertTrue(quiet.stale && !quiet.commandsEnabled && !quiet.guarding && !quiet.sounding)
        assertTrue(AlarmPanelsParser.overview(JSONObject("{}")).isEmpty)
    }

    @Test fun theStatusOfTheCommandsIsReadAndIsOffByDefault() {
        assertFalse(AlarmPanelsParser.commands(JSONObject("{}")).enabled)
        val status = AlarmPanelsParser.commands(JSONObject("""{"enabled":true,"pending":[],"recent":[
            {"command_id":"c0ffee0123456789","node_id":"alarm-1","action":"arm","mode":"away","accepted":false,"refusal":"zones_open","actor":"admin"},
            {"command_id":"c0ffee0123456780","node_id":"alarm-1","action":"disarm","accepted":true,"refusal":"none"}]}"""))
        assertTrue(status.enabled)
        assertEquals(2, status.recent.size)
        assertEquals("away", status.recent[0].mode)
        assertNull(status.recent[1].mode)
        assertTrue(status.recent[1].accepted)
    }

    @Test fun theWordingIsPlainSpanishAndNeverShowsACode() {
        for (phase in listOf("disarmed", "exit_delay", "armed", "entry_delay", "alarm")) assertNotEquals(phase, AlarmPanelText.phase(phase))
        for (kind in listOf("instant", "entry", "always")) assertNotEquals(kind, AlarmPanelText.zoneKind(kind))
        assertEquals("interior", AlarmPanelText.zoneKind("interior"))   // the same word in Spanish
        for (state in listOf("normal", "triggered", "tamper")) assertNotEquals(state, AlarmPanelText.zoneState(state))
        for (kind in listOf("armed", "exit_delay_started", "entry_delay_started", "alarm", "siren_timed_out", "disarmed", "bad_pin", "locked_out", "zone_bypassed")) assertNotEquals(kind, AlarmPanelText.event(kind))
        for (refusal in listOf("none", "not_disarmed", "zones_open", "not_armed", "bad_pin", "locked_out", "timeout")) assertNotEquals(refusal, AlarmPanelText.refusal(refusal))
        for (code in listOf("commands_disabled", "unknown_node", "node_unavailable", "node_commands_off", "busy", "mqtt_unavailable")) assertNotEquals(code, AlarmPanelText.error(code))
        assertEquals("No se pudo enviar la orden", AlarmPanelText.error("something_new"))
        assertEquals("hace 12 s", AlarmPanelText.ago(12))
        assertEquals("hace 3 min", AlarmPanelText.ago(180))
        assertEquals("hace 2 h", AlarmPanelText.ago(7200))
        assertTrue(AlarmPanelText.confirmDisarm("alarm-1").contains("no lleva PIN") || AlarmPanelText.confirmDisarm("alarm-1").contains("No lleva PIN"))
    }

    @Test fun theSummaryTellsWhatMattersMost() {
        assertEquals("Sin centrales de alarma", AlarmPanelText.summary(AlarmTotals()))
        assertEquals("Centrales desarmadas", AlarmPanelText.summary(AlarmTotals(2, 0, 0, 0)))
        assertEquals("Una central vigilando", AlarmPanelText.summary(AlarmTotals(2, 0, 1, 0)))
        assertEquals("2 centrales vigilando", AlarmPanelText.summary(AlarmTotals(2, 0, 2, 0)))
        assertEquals("¡Una central está en alarma!", AlarmPanelText.summary(AlarmTotals(2, 0, 1, 1)))   // a sounding alarm outranks the rest
    }

    @Test fun theAlarmsTheServerRaisesForPanelsHaveTheirOwnSentences() {
        for (code in listOf("alarm_sounding", "alarm_tamper", "alarm_locked_out", "alarm_offline")) assertNotEquals(code, DeviceText.alarmText(code))
    }
}
