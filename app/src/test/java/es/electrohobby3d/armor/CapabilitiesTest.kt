// ARMOR-ANDROID-CONTROL - what the session announces the person may do, and what the phone falls back to with a server that announces nothing.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.CapabilityAction
import es.electrohobby3d.armor.network.SessionCapabilities
import es.electrohobby3d.armor.network.SessionFacts
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesTest {
    private fun session(role: String, caps: String? = null): JSONObject {
        val extra = if (caps == null) "" else ",\"capabilities\":" + caps
        return JSONObject("{\"authenticated\":true,\"user\":{\"role\":\"" + role + "\"}" + extra + "}")
    }

    @Test fun `a server that announces nothing leaves the role to decide as before`() {
        val admin = SessionFacts.parse(session("admin")).capabilities
        assertFalse(admin.announced)
        for (action in listOf("arm", "disarm", "acknowledge", "operate", "camera_control", "administer", "manage_users")) assertTrue(action, admin.can(action))
        assertFalse("needs a route only the new server has", admin.can(CapabilityAction.SECURITY_SETTINGS))
        val operator = SessionFacts.parse(session("operator")).capabilities
        assertTrue(operator.can(CapabilityAction.ARM)); assertFalse(operator.can(CapabilityAction.ADMINISTER))
        for (role in listOf("viewer", "panel", "", "other")) assertTrue(role, CapabilityAction.ALL.none { SessionCapabilities.fromRole(role).can(it) })
    }

    @Test fun `an announced field is believed and an action it does not name is not allowed`() {
        val facts = SessionFacts.parse(session("admin", """{"version":1,"actions":{"arm":true,"disarm":false,"operate":true,"future":true},"features":["rtsp_tunnel",7],"security":{"client.device_lock":"sensitive"}}"""))
        val caps = facts.capabilities
        assertTrue(caps.announced)
        assertTrue(caps.can("arm")); assertFalse(caps.can("disarm")); assertFalse(caps.can("acknowledge")); assertFalse("administer is not named", caps.can("administer"))
        assertTrue(caps.has("rtsp_tunnel")); assertFalse(caps.has("zone_feed_key"))
        assertEquals("sensitive", facts.deviceLock)
    }

    @Test fun `a field of another version or shape is ignored and the role decides`() {
        assertNull(SessionCapabilities.parse(JSONObject("""{"version":2,"actions":{"arm":true}}""")))
        assertNull(SessionCapabilities.parse(JSONObject("""{"version":1}""")))
        assertNull(SessionCapabilities.parse(null))
        val caps = SessionCapabilities.of(session("viewer", """{"version":9,"actions":{"arm":true}}"""), "viewer")
        assertFalse(caps.announced); assertFalse(caps.can("arm"))
    }

    @Test fun `the screens follow the capabilities, and the role only when none were announced`() {
        val onlyAck = SessionCapabilities.parse(JSONObject("""{"version":1,"actions":{"operate":true,"acknowledge":true}}"""))
        val state = MonitorUiState(role = "admin", capabilities = onlyAck)
        assertFalse(state.readOnly); assertFalse("the role no longer decides", state.isAdmin)
        assertTrue(state.can(CapabilityAction.ACKNOWLEDGE)); assertFalse(state.can(CapabilityAction.ARM)); assertFalse(state.can(CapabilityAction.CAMERA_CONTROL))
        assertTrue(MonitorUiState(capabilities = SessionCapabilities.parse(JSONObject("""{"version":1,"actions":{"administer":true}}"""))).let { it.readOnly && it.isAdmin })
        // without them: the role, and "not told yet" (empty role) is not read-only
        assertTrue(MonitorUiState(role = "admin").isAdmin)
        assertTrue(MonitorUiState(role = "viewer").readOnly)
        assertFalse(MonitorUiState().readOnly); assertFalse(MonitorUiState().isAdmin); assertTrue(MonitorUiState().can(CapabilityAction.ARM))
    }
}
