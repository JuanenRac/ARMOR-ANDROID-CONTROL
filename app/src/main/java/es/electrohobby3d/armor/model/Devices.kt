// ARMOR-ANDROID-CONTROL - devices (sensors and actuators of any kind) and alarms: models, parsing and their Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

/** One alarm of the server's alarm centre: raised, then acknowledged by a person, then cleared when its cause ends. */
data class Alarm(
    val id: String,
    val sourceType: String,
    val sourceId: String,
    val severity: String,
    val code: String,
    val raisedAt: String,
    val acknowledgedBy: String?,
    val cleared: Boolean,
) {
    val acknowledged: Boolean get() = acknowledgedBy != null
}

/** A smoke detector, a door contact, a plug, a light... as the server reports it; [state] holds only boolean and numeric fields. */
data class SiteDevice(
    val id: String,
    val name: String,
    val kind: String,
    val protocol: String,
    val location: String,
    val actuator: Boolean,
    val canCommand: Boolean,
    val online: Boolean,
    val expectedIntervalS: Int,
    val state: Map<String, Any>,
)

object DeviceParser {
    fun alarm(json: JSONObject): Alarm? {
        val id = json.optString("id")
        val source = json.optJSONObject("source") ?: return null
        if (id.isBlank() || source.optString("id").isBlank()) return null
        return Alarm(
            id = id, sourceType = source.optString("type"), sourceId = source.optString("id"),
            severity = json.optString("severity", "warning"), code = json.optString("code"), raisedAt = json.optString("raised_at"),
            acknowledgedBy = if (json.has("acknowledged_at") && !json.isNull("acknowledged_at")) json.optString("acknowledged_by", "").ifBlank { "?" } else null,
            cleared = json.has("cleared_at") && !json.isNull("cleared_at"),
        )
    }

    fun device(json: JSONObject): SiteDevice? {
        val id = json.optString("id")
        if (id.isBlank()) return null
        val raw = json.optJSONObject("state")
        val state = buildMap<String, Any> {
            raw?.keys()?.forEach { key ->
                val value = raw.opt(key)
                if (value is Boolean) put(key, value) else if (value is Number) put(key, value.toDouble())
            }
        }
        return SiteDevice(
            id = id, name = json.optString("name", id).ifBlank { id }, kind = json.optString("kind"), protocol = json.optString("protocol"),
            location = json.optString("location"), actuator = json.optString("category") == "actuator", canCommand = json.optBoolean("can_command"),
            online = json.optBoolean("online"), expectedIntervalS = json.optInt("expected_interval_s"), state = state,
        )
    }
}

/** The wording of devices and alarms; the same rules as the Studio's Devices menu. */
object DeviceText {
    /** The state field a kind is mostly about: what the card shows and what an alarm waits for. */
    val mainField: Map<String, String?> = mapOf(
        "smoke" to "triggered", "co" to "triggered", "gas" to "triggered", "water_leak" to "triggered", "panic_button" to "triggered",
        "door" to "open", "window" to "open", "motion" to "triggered", "glass_break" to "triggered", "vibration" to "triggered",
        "climate" to null, "temperature" to null, "humidity" to null, "light_level" to null,
        "smart_plug" to "on", "smart_light" to "on", "smart_switch" to "on", "siren" to "on", "lock" to "locked", "valve" to "open",
    )
    private val alarmKinds = setOf("smoke", "co", "gas", "water_leak", "panic_button", "door", "window", "motion", "glass_break", "vibration")

    fun kindLabel(kind: String) = when (kind) {
        "smoke" -> "Detector de humo"; "co" -> "Detector de CO"; "gas" -> "Detector de gas"; "water_leak" -> "Detector de inundación"; "panic_button" -> "Botón de pánico"
        "door" -> "Contacto de puerta"; "window" -> "Contacto de ventana"; "motion" -> "Sensor de movimiento"; "glass_break" -> "Rotura de cristal"; "vibration" -> "Sensor de vibración"
        "climate" -> "Temperatura y humedad"; "temperature" -> "Temperatura"; "humidity" -> "Humedad"; "light_level" -> "Nivel de luz"
        "smart_plug" -> "Enchufe inteligente"; "smart_light" -> "Luz inteligente"; "smart_switch" -> "Interruptor"; "siren" -> "Sirena"; "lock" -> "Cerradura"; "valve" -> "Válvula"
        else -> kind
    }

    fun protocolLabel(protocol: String) = when (protocol) {
        "wifi" -> "Wi-Fi"; "bluetooth" -> "Bluetooth"; "zigbee" -> "Zigbee"; "zwave" -> "Z-Wave"; "thread" -> "Thread"; "lora" -> "LoRa"; "rf433" -> "433 MHz"; "wired" -> "Cable"; else -> protocol
    }

    private fun flag(state: Map<String, Any>, key: String) = state[key] == true
    private fun number(state: Map<String, Any>, key: String) = (state[key] as? Double)

    /** What the device is doing, as short pieces of text ("ACTIVADO", "21.5 °C", "batería 80 %"). */
    fun describeState(device: SiteDevice): List<String> = buildList {
        val main = mainField[device.kind]
        if (main != null && device.state.containsKey(main)) {
            val on = flag(device.state, main)
            add(when (main) {
                "triggered" -> if (on) "ACTIVADO" else "en reposo"
                "open" -> if (on) "abierto" else "cerrado"
                "locked" -> if (on) "cerrada con llave" else "abierta"
                else -> if (on) "encendido" else "apagado"
            })
        }
        number(device.state, "temperature")?.let { add("%.1f °C".format(java.util.Locale.US, it)) }
        number(device.state, "humidity")?.let { add("${it.toInt()} %") }
        number(device.state, "lux")?.let { add("${it.toInt()} lx") }
        number(device.state, "brightness")?.let { add("brillo ${it.toInt()} %") }
        number(device.state, "power_w")?.let { add("%.1f W".format(java.util.Locale.US, it)) }
        number(device.state, "co_ppm")?.let { add("${it.toInt()} ppm") }
        number(device.state, "battery")?.let { add("batería ${it.toInt()} %") }
        if (flag(device.state, "tamper")) add("manipulado")
    }

    /** Why a device needs a person: triggered, tampered with, low battery or silent; null when all is well. */
    fun problem(device: SiteDevice): String? {
        val main = mainField[device.kind]
        if (device.kind in alarmKinds && main != null && flag(device.state, main)) return "activado"
        if (flag(device.state, "tamper")) return "manipulado"
        if ((number(device.state, "battery") ?: 100.0) < 15.0) return "batería baja"
        if (!device.online && device.expectedIntervalS > 0) return "sin respuesta"
        return null
    }

    fun alarmText(code: String) = when (code) {
        "intrusion" -> "Intrusión detectada por los radares"; "node_down" -> "Un nodo de campo no responde"; "camera_down" -> "Una cámara no responde"
        "smoke" -> "Humo detectado"; "co" -> "Monóxido de carbono detectado"; "gas" -> "Gas detectado"; "water_leak" -> "Inundación detectada"; "panic" -> "Botón de pánico pulsado"
        "door_open" -> "Puerta abierta con el sistema armado"; "window_open" -> "Ventana abierta con el sistema armado"; "motion" -> "Movimiento con el sistema armado"
        "glass_break" -> "Rotura de cristal con el sistema armado"; "vibration" -> "Vibración con el sistema armado"; "tamper" -> "Un dispositivo ha sido manipulado"
        "low_battery" -> "Batería baja"; "device_offline" -> "Un dispositivo ha dejado de responder"; "triggered" -> "Un dispositivo se ha activado"
        else -> code
    }

    fun severityLabel(severity: String) = when (severity) { "critical" -> "CRÍTICA"; "high" -> "ALTA"; else -> "AVISO" }
}
