package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRouteTest {
    private fun alarm(source: String, code: String = "x") = Alarm("a", source, "s", "high", code, "", null, false)

    @Test fun `an alarm takes the person to the screen that handles what raised it`() {
        assertEquals(AlarmTarget("Cameras"), AlarmRoute.of(alarm("camera")))
        assertEquals(AlarmTarget("Devices"), AlarmRoute.of(alarm("device")))
        assertEquals(AlarmTarget("More", MoreTab.Network), AlarmRoute.of(alarm("network")))
        assertEquals(AlarmTarget("More", MoreTab.AlarmPanels), AlarmRoute.of(alarm("alarm")))
        assertEquals(AlarmTarget("More", MoreTab.Electrical), AlarmRoute.of(alarm("electrical")))
        assertEquals(AlarmTarget("Radar"), AlarmRoute.of(alarm("node")))
    }

    @Test fun `solar alarms go to the batteries or to the inverters`() {
        assertEquals(AlarmTarget("More", MoreTab.Batteries), AlarmRoute.of(alarm("solar", "battery_low")))
        assertEquals(AlarmTarget("More", MoreTab.Solar), AlarmRoute.of(alarm("solar", "inverter_fault")))
    }

    @Test fun `anything else stays on the alarms`() {
        assertEquals(AlarmTarget("Alarms"), AlarmRoute.of(alarm("moon")))
    }
}
