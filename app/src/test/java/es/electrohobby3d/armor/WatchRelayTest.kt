// ARMOR-ANDROID-CONTROL - tests of the messages for the Wear OS watch (the contract of ARMOR-WATCH) and of what the phone does with the confirmation of the watch.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchRelayTest {
    private fun notice(id: Long = 7, severity: String = "high", alarm: String? = "al-1", title: String = "Intrusion", text: String = "Radar 1 saw movement") =
        AlarmNotice(id, title, text, severity, NoticeKinds.SYSTEM, alarm)

    @Test
    fun anAlertCarriesTheFieldsOfTheContract() {
        val json = JSONObject(WatchRelay.alertMessage(notice())!!)
        assertEquals("alert", json.getString("type"))
        assertEquals(1, json.getInt("protocolVersion"))
        assertEquals("al-1", json.getString("alertId"))
        assertEquals("warning", json.getString("severity"))
        assertEquals("Intrusion", json.getString("title"))
        assertTrue(json.getBoolean("needsAck"))
        assertEquals(120, json.getInt("ttlSeconds"))
        assertEquals(8, json.length())
    }

    @Test
    fun theSeverityOfThePhoneBecomesOneOfTheWatch() {
        assertEquals("critical", WatchRelay.severityOf("critical"))
        assertEquals("warning", WatchRelay.severityOf("high"))
        assertEquals("info", WatchRelay.severityOf("warning"))
        assertEquals("info", WatchRelay.severityOf("anything else"))
        assertEquals(600, JSONObject(WatchRelay.alertMessage(notice(severity = "critical"))!!).getInt("ttlSeconds"))
    }

    @Test
    fun theTextsAreCutToTheLimitsAndAnEmptyOneIsNotSent() {
        val long = JSONObject(WatchRelay.alertMessage(notice(title = "T".repeat(100), text = "X".repeat(500)))!!)
        assertEquals(WatchRelay.TITLE_MAX, long.getString("title").length)
        assertEquals(WatchRelay.TEXT_MAX, long.getString("text").length)
        assertNull(WatchRelay.alertMessage(notice(title = "  ")))
        assertNull(WatchRelay.alertMessage(notice(text = "")))
    }

    @Test
    fun aNoticeWithoutAnAlarmAsksForNoConfirmation() {
        val json = JSONObject(WatchRelay.alertMessage(notice(id = 41, alarm = null))!!)
        assertFalse(json.getBoolean("needsAck"))
        assertEquals("n41", json.getString("alertId"))
        assertEquals("n0", WatchRelay.alertId(notice(id = -3, alarm = "not valid!")))
    }

    @Test
    fun theRelayAndClearMessagesAreTheOnesOfTheContract() {
        val relay = JSONObject(WatchRelay.relayMessage(true))
        assertEquals("relay", relay.getString("type"))
        assertTrue(relay.getBoolean("enabled"))
        assertFalse(JSONObject(WatchRelay.relayMessage(false)).getBoolean("enabled"))
        val clear = JSONObject(WatchRelay.clearMessage("al-1"))
        assertEquals("alert_clear", clear.getString("type"))
        assertEquals("al-1", clear.getString("alertId"))
    }

    @Test
    fun onlyAStrictConfirmationIsRead() {
        assertEquals("al-1", WatchRelay.parseAck("""{"type":"alert_ack","protocolVersion":1,"alertId":"al-1"}"""))
        assertNull(WatchRelay.parseAck("""{"type":"alert_ack","protocolVersion":2,"alertId":"al-1"}"""))
        assertNull(WatchRelay.parseAck("""{"type":"alert","protocolVersion":1,"alertId":"al-1"}"""))
        assertNull(WatchRelay.parseAck("""{"type":"alert_ack","protocolVersion":1,"alertId":"bad id"}"""))
        assertNull(WatchRelay.parseAck("""{"type":"alert_ack","protocolVersion":1}"""))
        assertNull(WatchRelay.parseAck("not json"))
        assertNull(WatchRelay.parseAck("x".repeat(600)))
    }

    @Test
    fun theConfirmationIsActedOnlyForAnAlertTheWatchWasGivenAndOnlyOnce() {
        WatchRelay.sent.remember("al-9", "alarm-9", 60)
        val ack = """{"type":"alert_ack","protocolVersion":1,"alertId":"al-9"}"""
        assertNull(WatchRelay.alarmToAcknowledge(false, ack))   // the option is off: the entry is not even looked at
        assertNotNull(WatchRelay.alarmToAcknowledge(true, ack))
        assertNull(WatchRelay.alarmToAcknowledge(true, ack))   // used up
        assertNull(WatchRelay.alarmToAcknowledge(true, """{"type":"alert_ack","protocolVersion":1,"alertId":"never-sent"}"""))
    }

    @Test
    fun anAlertThatExpiredCannotBeConfirmedAndTheTableIsBounded() {
        var clock = 100L
        val table = SentAlerts { clock }
        table.remember("a", "alarm-a", 10)
        clock += 10
        assertNull(table.take("a"))
        for (i in 1..SentAlerts.CAPACITY + 5) table.remember("id$i", "alarm$i", 600)
        assertNull(table.take("id1"))
        assertEquals("alarm${SentAlerts.CAPACITY + 5}", table.take("id${SentAlerts.CAPACITY + 5}"))
    }

    // The contract of the watch (docs/watch_alert_contract.json) is one file kept identically in ARMOR-WATCH, here and in the iOS app: the phone's side is held to its limits and examples.
    private fun contract(): JSONObject {
        val file = listOf(java.io.File("../docs/watch_alert_contract.json"), java.io.File("docs/watch_alert_contract.json")).first { it.exists() }
        return JSONObject(file.readText(Charsets.UTF_8))
    }

    private fun expanded(value: Any?): Any? = if (value is JSONObject && value.length() == 2 && value.has("repeat") && value.has("count")) value.getString("repeat").repeat(value.getInt("count")) else value

    @Test
    fun `the limits and the paths of the contract are the ones this app uses`() {
        val contract = contract()
        val limits = contract.getJSONObject("limits")
        assertEquals(WatchRelay.TITLE_MAX, limits.getInt("titleMax"))
        assertEquals(WatchRelay.TEXT_MAX, limits.getInt("textMax"))
        assertEquals(WatchRelay.PROTOCOL_VERSION, contract.getInt("protocolVersion"))
        assertEquals(WatchRelay.PATH_TO_WATCH, contract.getJSONObject("paths").getString("toWatch"))
        assertEquals(WatchRelay.PATH_TO_PHONE, contract.getJSONObject("paths").getString("toPhone"))
        val pattern = Regex(limits.getString("alertIdPattern"))
        for (id in listOf("a", "al-001", "n_2", "a".repeat(64), "", "a b", "a".repeat(65), "a/b")) assertEquals(id, pattern.matches(id), WatchRelay.validId(id))
    }

    @Test
    fun `the confirmations of the contract are read and the wrong ones are refused`() {
        val contract = contract()
        val valid = contract.getJSONArray("valid")
        for (i in 0 until valid.length()) {
            val example = valid.getJSONObject(i)
            if (example.getString("type") == "alert_ack") assertEquals(example.getString("alertId"), WatchRelay.parseAck(example.toString()))
        }
        val invalid = contract.getJSONArray("invalid")
        for (i in 0 until invalid.length()) {
            val example = invalid.getJSONObject(i)
            val why = example.getString("why")
            example.remove("why")
            for (key in example.keys().asSequence().toList()) example.put(key, expanded(example.get(key)))
            if (example.optString("type") == "alert_ack") assertNull(why, WatchRelay.parseAck(example.toString()))
        }
    }

    @Test
    fun `the messages the phone writes are accepted by the contract`() {
        val valid = contract().getJSONArray("valid")
        val relays = (0 until valid.length()).map { valid.getJSONObject(it) }.filter { it.getString("type") == "relay" }.map { it.getBoolean("enabled") }.toSet()
        assertEquals(setOf(true, false), relays)
        for (on in relays) {
            val written = JSONObject(WatchRelay.relayMessage(on))
            assertEquals("relay", written.getString("type"))
            assertEquals(on, written.getBoolean("enabled"))
        }
        val clear = JSONObject(WatchRelay.clearMessage("al-001"))
        assertEquals("alert_clear", clear.getString("type"))
        assertEquals("al-001", clear.getString("alertId"))
    }
}
