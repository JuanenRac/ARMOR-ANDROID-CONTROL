// ARMOR-ANDROID-CONTROL - which battery stacks the server is refusing. Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.RefusedBattery
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RefusedBatteryTest {
    private fun topic(name: String, errorAt: String?, okAt: String?, kind: String = "battery") = JSONObject()
        .put("topic", name).put("last_error", "balancing too high").put("last_error_at", errorAt ?: JSONObject.NULL)
        .put("last_ok_at", okAt ?: JSONObject.NULL).put("last_payload", "{\"kind\": \"$kind\"}")

    @Test fun onlyTheStacksRefusedSinceTheLastReading() {
        val topics = JSONArray()
            .put(topic("armor/solar/n1/stack/state", "2026-01-02T10:00:00Z", "2026-01-02T09:00:00Z"))
            .put(topic("armor/solar/n1/old/state", "2026-01-02T08:00:00Z", "2026-01-02T09:00:00Z"))
            .put(topic("armor/solar/n1/inv/state", "2026-01-02T10:00:00Z", null, "inverter"))
            .put(topic("armor/solar/n1/fine/state", null, "2026-01-02T09:00:00Z"))
            .put(topic("armor/alarm/n2/state", "2026-01-02T10:00:00Z", null))
        assertEquals(listOf(RefusedBattery("n1 / stack", "balancing too high")), RefusedBattery.fromIngest(JSONObject().put("topics", topics)))
        assertEquals(emptyList<RefusedBattery>(), RefusedBattery.fromIngest(JSONObject()))
    }
}
