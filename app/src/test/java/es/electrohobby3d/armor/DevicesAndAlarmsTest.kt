package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ArmorEvent
import es.electrohobby3d.armor.model.DeviceParser
import es.electrohobby3d.armor.model.DeviceText
import es.electrohobby3d.armor.model.EventParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicesAndAlarmsTest {
    private fun alarmEvent(id: Long, state: String, sourceType: String, code: String, severity: String = "critical", source: String = "smoke-kitchen") =
        ArmorEvent(id, "t", "alarm", source, null, state, null, null, severity = severity, code = code, sourceType = sourceType)

    @Test fun aDeviceAlarmWhenItIsRaisedWakesTheOperatorEvenWhileDisarmed() {
        val notices = AlarmPolicy.notices(listOf(alarmEvent(1, "raised", "device", "smoke")), armed = false)
        assertEquals(1, notices.size)
        assertEquals("ALARMA CRÍTICA", notices[0].title)
        assertEquals("Humo detectado · smoke-kitchen", notices[0].text)
    }

    @Test fun anAlarmIsNotAnnouncedAgainWhenAcknowledgedOrCleared() {
        val events = listOf(alarmEvent(2, "acknowledged", "device", "smoke"), alarmEvent(3, "cleared", "device", "smoke"))
        assertTrue(AlarmPolicy.notices(events, armed = true).isEmpty())
    }

    @Test fun nodeAndCameraAlarmsAreNotAnnouncedTwice() {
        // Their own alert and camera events already are.
        val events = listOf(alarmEvent(4, "raised", "node", "intrusion", "high", "north-1"), alarmEvent(5, "raised", "camera", "camera_down", "warning", "cam-01"))
        assertTrue(AlarmPolicy.notices(events, armed = true).isEmpty())
    }

    @Test fun solarAlarmsWakeTheOperatorOnceWhenRaisedAndNotWhenAcknowledgedOrCleared() {
        val fault = alarmEvent(20, "raised", "solar", "solar_fault", "high", "casa/axpert-1")
        val low = alarmEvent(21, "raised", "solar", "solar_battery_low", "warning", "casa/ant-1")
        val notices = AlarmPolicy.notices(listOf(fault, low), armed = false)   // a solar fault matters whether or not the perimeter is armed
        assertEquals(2, notices.size)
        assertEquals("ALARMA ALTA", notices[0].title)
        assertEquals("Avería en un inversor solar · casa/axpert-1", notices[0].text)
        assertEquals("ALARMA AVISO", notices[1].title)
        assertEquals("Batería solar baja · casa/ant-1", notices[1].text)
        val later = listOf(alarmEvent(22, "acknowledged", "solar", "solar_fault", "high", "casa/axpert-1"), alarmEvent(23, "cleared", "solar", "solar_fault", "high", "casa/axpert-1"))
        assertTrue(AlarmPolicy.notices(later, armed = true).isEmpty())
    }

    @Test fun aSolarAlarmIsNamedByItsEquipmentNotByItsPath() {
        val alarm = es.electrohobby3d.armor.model.Alarm("alm-1", "solar", "casa/axpert-1", "high", "solar_fault", "2026-01-01T10:00:00Z", null, false)
        assertEquals("Inversor del garaje", alarmSourceName(alarm, emptyList(), emptyMap(), mapOf("casa/axpert-1" to "Inversor del garaje")))
        assertEquals("axpert-1", alarmSourceName(alarm, emptyList(), emptyMap()))   // not known (yet): the device part of the path
    }

    @Test fun everyCodeTheServerRaisesHasItsOwnWording() {
        val codes = listOf("intrusion", "node_down", "camera_down", "smoke", "co", "gas", "water_leak", "panic", "door_open", "window_open", "motion", "glass_break", "vibration", "triggered", "tamper", "low_battery", "device_offline",
            "solar_fault", "solar_battery_low", "solar_battery_alarm", "solar_offline", "electrical_alarm", "electrical_voltage", "electrical_grid_lost", "electrical_offline", "electrical_switch_fault")
        for (code in codes) assertNotEquals(code, DeviceText.alarmText(code))
    }

    private fun assertNotEquals(unexpected: String, actual: String) = assertFalse("no wording for $unexpected", unexpected == actual)

    @Test fun parsesAlarmAndDeviceEventsAndRefusesMalformedOnes() {
        val alarm = EventParser.parse(JSONObject("""{"id":10,"at":"x","type":"alarm","alarm_id":"alm-00001","state":"raised","severity":"critical","source":"smoke-kitchen","source_type":"device","code":"smoke"}"""))
        assertNotNull(alarm)
        assertEquals("smoke-kitchen", alarm!!.subject)
        assertEquals("raised", alarm.to)
        assertEquals("device", alarm.sourceType)
        assertEquals("smoke", alarm.code)
        val device = EventParser.parse(JSONObject("""{"id":11,"at":"x","type":"device","device_id":"front-door","kind":"door","field":"open","from":false,"to":true}"""))
        assertEquals("front-door: abierto no → sí", AlarmPolicy.describe(device!!))
        assertNull(EventParser.parse(JSONObject("""{"id":12,"type":"alarm","state":"raised"}""")))
        assertNull(EventParser.parse(JSONObject("""{"id":13,"type":"device"}""")))
    }

    @Test fun theHistoryReadsAnAlarmInSpanish() {
        assertEquals("Humo detectado · smoke-kitchen · generada", AlarmPolicy.describe(alarmEvent(1, "raised", "device", "smoke")))
        assertEquals("Humo detectado · smoke-kitchen · cerrada", AlarmPolicy.describe(alarmEvent(2, "cleared", "device", "smoke")))
    }

    @Test fun parsesAnAlarmOfTheAlarmCentre() {
        val raised = DeviceParser.alarm(JSONObject("""{"id":"alm-1","key":"k","source":{"type":"device","id":"d1"},"severity":"high","code":"door_open","raised_at":"2026-01-01T00:00:00.000Z"}"""))!!
        assertFalse(raised.acknowledged)
        assertFalse(raised.cleared)
        val done = DeviceParser.alarm(JSONObject("""{"id":"alm-2","source":{"type":"node","id":"n1"},"severity":"warning","code":"node_down","raised_at":"x","acknowledged_at":"y","acknowledged_by":"admin","cleared_at":"z"}"""))!!
        assertEquals("admin", done.acknowledgedBy)
        assertTrue(done.cleared)
        assertNull(DeviceParser.alarm(JSONObject("""{"id":"alm-3","code":"smoke"}""")))
    }

    private fun device(json: String) = DeviceParser.device(JSONObject(json))!!

    @Test fun describesTheStateOfEachKindOfDevice() {
        val smoke = device("""{"id":"s","name":"Cocina","kind":"smoke","protocol":"zigbee","category":"sensor","online":true,"state":{"triggered":true,"battery":80}}""")
        assertEquals(listOf("ACTIVADO", "batería 80 %"), DeviceText.describeState(smoke))
        val climate = device("""{"id":"c","kind":"climate","category":"sensor","online":true,"state":{"temperature":21.54,"humidity":48}}""")
        assertEquals(listOf("21.5 °C", "48 %"), DeviceText.describeState(climate))
        val lock = device("""{"id":"l","kind":"lock","category":"actuator","can_command":true,"online":true,"state":{"locked":true}}""")
        assertEquals(listOf("cerrada con llave"), DeviceText.describeState(lock))
        assertEquals("c", climate.name)
    }

    @Test fun tellsWhenADeviceNeedsAPerson() {
        assertEquals("activado", DeviceText.problem(device("""{"id":"a","kind":"smoke","online":true,"state":{"triggered":true}}""")))
        assertEquals("activado", DeviceText.problem(device("""{"id":"b","kind":"door","online":true,"state":{"open":true}}""")))
        assertNull(DeviceText.problem(device("""{"id":"c","kind":"smart_plug","category":"actuator","online":true,"state":{"on":true}}""")))
        assertEquals("manipulado", DeviceText.problem(device("""{"id":"d","kind":"door","online":true,"state":{"open":false,"tamper":true}}""")))
        assertEquals("batería baja", DeviceText.problem(device("""{"id":"e","kind":"climate","online":true,"state":{"battery":9}}""")))
        assertEquals("sin respuesta", DeviceText.problem(device("""{"id":"f","kind":"motion","online":false,"expected_interval_s":300,"state":{}}""")))
        assertNull(DeviceText.problem(device("""{"id":"g","kind":"motion","online":false,"expected_interval_s":0,"state":{}}""")))
    }

    @Test fun onlyBooleanAndNumericFieldsSurviveParsing() {
        val parsed = device("""{"id":"x","kind":"smart_plug","state":{"on":true,"power_w":42,"note":"hola","nested":{"a":1}}}""")
        assertEquals(setOf("on", "power_w"), parsed.state.keys)
        assertEquals(42.0, parsed.state["power_w"])
        assertNull(DeviceParser.device(JSONObject("""{"name":"sin id"}""")))
    }

    @Test fun aLockIsLockedByOnAndAValveOpenedByOn() {
        assertEquals("Cerrar" to "Abrir", commandLabels("lock"))
        assertEquals("Abrir" to "Cerrar", commandLabels("valve"))
        assertEquals("Encender" to "Apagar", commandLabels("smart_plug"))
    }
}
