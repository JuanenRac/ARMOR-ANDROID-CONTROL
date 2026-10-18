// ARMOR-ANDROID-CONTROL - what the parsers do with answers that are not what they expect: a newer server, an older one, a damaged one.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.AlarmPanelsParser
import es.electrohobby3d.armor.model.ConnectionsParser
import es.electrohobby3d.armor.model.DeviceParser
import es.electrohobby3d.armor.model.ElectricalParser
import es.electrohobby3d.armor.model.EventParser
import es.electrohobby3d.armor.model.NetworkParser
import es.electrohobby3d.armor.model.NodeHealthParser
import es.electrohobby3d.armor.model.PreflightParser
import es.electrohobby3d.armor.model.SecurityParser
import es.electrohobby3d.armor.model.ServicesParser
import es.electrohobby3d.armor.model.SiteParser
import es.electrohobby3d.armor.model.SolarParser
import es.electrohobby3d.armor.model.VoiceParser
import es.electrohobby3d.armor.model.WeatherParser
import es.electrohobby3d.armor.update.ReleaseMetadataParser
import es.electrohobby3d.armor.update.UpdateCheckResult
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every field name the app reads from the server (and a few it does not), so that each one can be given a value of the wrong kind. */
private val KEYS = (
    "accepted acknowledged_at acknowledged_by action active activeCameraIds active_sessions address agent ago_s alarm alarms alert_level armed assets at at_ms authenticated available base batteries " +
    "battery_percent battery_v battery_w body broker_reconnects buildings bypassed bytes cameraId cameraPlacements camera_id cameras can_command capacity_ah category challenge channel channels cidr " +
    "cleared_at clients code command_id commands_enabled config configured confirmation_token counted country created createdAt current current_a daily data ddns depth description detail device device_id " +
    "devices dimensions domain downtime_24h_s duration_s enabled endpoint energy_kwh entry_delay_s error events executed exit_delay_s expected_interval_s family fault features file firmware first_seen " +
    "first_seen_ms floors fov frequency_hz gateway_ok grid_hz grid_kwh grid_present grid_v grid_w hasCredentials has_ip heading heap_free height host hourly id intent interface internet inverters ip is_day " +
    "issues items kind label last_address last_error last_seen last_seen_ms latency_ms latitude level link link_up liveVideoAvailable load_on load_percent load_w location locked_out logins_failed " +
    "logins_ok longitude loss_percent lux mac minutely_15 mirror mode model modules name networks node node_id nodes note notify ok online onvifPort open_zones origin outage_s outages outcome path peers " +
    "phase policy port ports power_factor power_w present probes problems profile profiles proto protocol provider public pv_a pv_v pv_w raised_at range reading ready received_at recent recovery_codes " +
    "recovery_left refusal registered report reset_reason restarts_24h results revision risk role roof rotation rssi rssi_dbm rtspPath rtspPort running secret security selected sensor_id sensors serial " +
    "services sessions setup severity since_ms siren site slope snapshotUrl source speech speed_mm_s ssid stale started_ms state status style subnet sunrise sunset switches systemd tag_name target " +
    "target_count targets temperature_2m temperature_c temperatures_c terrain thickness time timezone tool totals totp_policy totp_required track_id trust_proxy trusted type unit units updated_at " +
    "uptime_s uri user voltage_v waiting warnings weather_code width x y z"
    ).split(" ")

private fun variants(): List<Pair<String, () -> Any?>> = listOf(
    "null" to { JSONObject.NULL }, "zero" to { 0 }, "negative" to { -7 }, "huge" to { Long.MAX_VALUE }, "fraction" to { 0.5 }, "text" to { "text" }, "empty text" to { "" }, "number text" to { "12" },
    "true" to { true }, "false" to { false }, "empty list" to { JSONArray() }, "list of junk" to { JSONArray().put(JSONObject.NULL).put(3).put("a").put(JSONObject()).put(JSONArray()) }, "empty object" to { JSONObject() },
)

/** An object with every known field set to [value]. */
private fun filled(value: Any?): JSONObject = JSONObject().also { json -> KEYS.forEach { json.put(it, value) } }

/** The documents to try: every field of the wrong kind, all fields holding objects of wrong-kind fields, and all fields holding lists of such objects and junk. */
private fun documents(): List<Pair<String, JSONObject>> = buildList {
    add("empty" to JSONObject())
    for ((name, make) in variants()) {
        add("flat $name" to filled(make()))
        add("nested $name" to filled(filled(make())))
        add("listed $name" to filled(JSONArray().put(filled(make())).put(JSONObject.NULL).put(7).put("x").put(filled(filled(make())))))
    }
}

class ParsersTolerantTest {
    private fun entries(): List<Pair<String, (JSONObject) -> Any?>> = listOf(
        "SiteParser.parse" to { j -> SiteParser.parse(j) },
        "SiteParser.targets" to { j -> SiteParser.targets(j) },
        "EventParser.parse" to { j -> EventParser.parse(j) },
        "ConnectionsParser.list" to { j -> ConnectionsParser.list(j) },
        "ConnectionsParser.client" to { j -> ConnectionsParser.client(j) },
        "DeviceParser.alarm" to { j -> DeviceParser.alarm(j) },
        "DeviceParser.device" to { j -> DeviceParser.device(j) },
        "ElectricalParser.overview" to { j -> ElectricalParser.overview(j) },
        "NetworkParser.overview" to { j -> NetworkParser.overview(j) },
        "NodeHealthParser.overview" to { j -> NodeHealthParser.overview(j) },
        "PreflightParser.parse" to { j -> PreflightParser.parse(j) },
        "SecurityParser.sessions" to { j -> SecurityParser.sessions(j) },
        "SecurityParser.totp" to { j -> SecurityParser.totp(j) },
        "SecurityParser.remote" to { j -> SecurityParser.remote(j) },
        "SecurityParser.remoteSetup" to { j -> SecurityParser.remoteSetup(j) },
        "SecurityParser.peerConfig" to { j -> SecurityParser.peerConfig(j) },
        "SecurityParser.codes" to { j -> SecurityParser.codes(j) },
        "ServicesParser.overview" to { j -> ServicesParser.overview(j) },
        "SolarParser.overview" to { j -> SolarParser.overview(j) },
        "SolarParser.device" to { j -> SolarParser.device(j) },
        "SolarParser.totals" to { j -> SolarParser.totals(j) },
        "VoiceParser.parse" to { j -> VoiceParser.parse(j) },
        "AlarmPanelsParser.overview" to { j -> AlarmPanelsParser.overview(j) },
        "AlarmPanelsParser.profiles" to { j -> AlarmPanelsParser.profiles(j) },
        "AlarmPanelsParser.commands" to { j -> AlarmPanelsParser.commands(j) },
        "WeatherParser.place" to { j -> WeatherParser.place(j) },
        "WeatherParser.places" to { j -> WeatherParser.places(j) },
        "WeatherParser.forecast" to { j -> WeatherParser.forecast(j) },
        "WeatherParser.airQuality" to { j -> WeatherParser.airQuality(j) },
    )

    @Test fun `no parser throws whatever kind of value it finds in a field`() {
        val broken = sortedMapOf<String, String>()
        for ((parser, run) in entries()) for ((document, json) in documents()) {
            try { run(json) } catch (error: Throwable) { broken.putIfAbsent(parser, "${error.javaClass.simpleName} on $document: ${error.message?.take(80)}") }
        }
        assertTrue("these parsers throw on an answer of the wrong shape:\n" + broken.entries.joinToString("\n") { "${it.key}: ${it.value}" }, broken.isEmpty())
    }

    @Test fun `an event that is not a well-formed known one is dropped, and the good ones around it stay`() {
        val events = JSONObject("""{"events":[null,5,"x",[],{"id":3,"type":"mode","at":"2026-01-01T00:00:00Z","mode":"armed"},{"nonsense":true},{"id":0,"type":"mode"},{"id":4,"type":"teleport"},{"id":5,"type":"mode","mode":"disarmed"}]}""")
        val list = events.getJSONArray("events")
        val good = (0 until list.length()).mapNotNull { list.optJSONObject(it)?.let(EventParser::parse) }
        assertEquals(listOf(3L, 5L), good.map { it.id })
        assertEquals(listOf("armed", "disarmed"), good.map { it.mode })
    }

    @Test fun `a release with an unreadable body is a failure to the person, not a crash of the app`() {
        val results = listOf("{}", """{"tag_name":5}""", """{"tag_name":"v9.9.9","assets":"x"}""", """{"tag_name":"v9.9.9","assets":[null,5,{}]}""",
            """{"tag_name":"v9.9.9","assets":[{"name":"ARMOR-ANDROID-CONTROL-release.apk","browser_download_url":"http://not-secure/x.apk"}]}""")
            .map { ReleaseMetadataParser.parseLatestStable(it, "0.1.0") }
        assertTrue(results.none { it is UpdateCheckResult.Available })
        val ok = ReleaseMetadataParser.parseLatestStable("""{"tag_name":"v9.9.9","assets":[{"name":"ARMOR-ANDROID-CONTROL-release.apk","browser_download_url":"https://example.org/x.apk"}]}""", "0.1.0")
        assertEquals(true, ok is UpdateCheckResult.Available)
    }
}
