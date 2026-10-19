// ARMOR-ANDROID-CONTROL - devices (sensors and actuators of any kind) and alarms: models, parsing and their Spanish wording.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.tr
import es.electrohobby3d.armor.trf
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
    /** How much it matters to switch it from afar: "low" (a click), "circuit" (a circuit of the board: asks first) or "critical" (asks first, and only an administrator may). */
    val risk: String = "low",
)

object DeviceParser {
    fun alarm(json: JSONObject): Alarm? {
        val id = json.optStr("id")
        val source = json.optJSONObject("source") ?: return null
        if (id.isBlank() || source.optStr("id").isBlank()) return null
        if (es.electrohobby3d.armor.AlarmPolicy.isSilent(json.optStr("code"))) return null   // the alarm of a duress PIN is not listed nor counted on a phone
        return Alarm(
            id = id, sourceType = source.optStr("type"), sourceId = source.optStr("id"),
            severity = json.optStr("severity", "warning"), code = json.optStr("code"), raisedAt = json.optStr("raised_at"),
            acknowledgedBy = if (json.has("acknowledged_at") && !json.isNull("acknowledged_at")) json.optStr("acknowledged_by", "").ifBlank { "?" } else null,
            cleared = json.has("cleared_at") && !json.isNull("cleared_at"),
        )
    }

    fun device(json: JSONObject): SiteDevice? {
        val id = json.optStr("id")
        if (id.isBlank()) return null
        val raw = json.optJSONObject("state")
        val state = buildMap<String, Any> {
            raw?.keys()?.forEach { key ->
                val value = raw.opt(key)
                if (value is Boolean) put(key, value) else if (value is Number) put(key, value.toDouble())
            }
        }
        return SiteDevice(
            id = id, name = json.optStr("name", id).ifBlank { id }, kind = json.optStr("kind"), protocol = json.optStr("protocol"),
            location = json.optStr("location"), actuator = json.optStr("category") == "actuator", canCommand = json.optBoolean("can_command"),
            online = json.optBoolean("online"), expectedIntervalS = json.optInt("expected_interval_s"), state = state,
            risk = json.optStr("risk", "low").ifBlank { "low" },
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
        "smart_plug" to "on", "smart_light" to "on", "smart_switch" to "on", "smart_breaker" to "on", "energy_meter" to null, "siren" to "on", "lock" to "locked", "valve" to "open",
        "shutter" to "open", "gate_motor" to "open", "garage_motor" to "open", "water_heater" to "on",
    )
    private val alarmKinds = setOf("smoke", "co", "gas", "water_leak", "panic_button", "door", "window", "motion", "glass_break", "vibration")

    fun kindLabel(kind: String) = when (kind) {
        "smoke" -> tr("Detector de humo"); "co" -> tr("Detector de CO"); "gas" -> tr("Detector de gas"); "water_leak" -> tr("Detector de inundación"); "panic_button" -> tr("Botón de pánico")
        "door" -> tr("Contacto de puerta"); "window" -> tr("Contacto de ventana"); "motion" -> tr("Sensor de movimiento"); "glass_break" -> tr("Rotura de cristal"); "vibration" -> tr("Sensor de vibración")
        "climate" -> tr("Temperatura y humedad"); "temperature" -> tr("Temperatura"); "humidity" -> tr("Humedad"); "light_level" -> tr("Nivel de luz")
        "smart_plug" -> tr("Enchufe inteligente"); "smart_light" -> tr("Luz inteligente"); "smart_switch" -> tr("Interruptor"); "smart_breaker" -> tr("Automático inteligente"); "energy_meter" -> tr("Medidor de energía"); "siren" -> tr("Sirena"); "lock" -> tr("Cerradura"); "valve" -> tr("Válvula")
        "shutter" -> tr("Persiana motorizada"); "gate_motor" -> tr("Motor de puerta o cancela"); "garage_motor" -> tr("Motor de puerta de garaje"); "water_heater" -> tr("Calentador de agua")
        else -> kind
    }

    fun protocolLabel(protocol: String) = when (protocol) {
        "wifi" -> "Wi-Fi"; "bluetooth" -> "Bluetooth"; "zigbee" -> "Zigbee"; "zwave" -> "Z-Wave"; "thread" -> "Thread"; "lora" -> "LoRa"; "rf433" -> "433 MHz"; "wired" -> tr("Cable"); else -> protocol
    }

    private fun flag(state: Map<String, Any>, key: String) = state[key] == true
    private fun number(state: Map<String, Any>, key: String) = (state[key] as? Double)

    /** What the device is doing, as short pieces of text ("ACTIVADO", "21.5 °C", "batería 80 %"). */
    fun describeState(device: SiteDevice): List<String> = buildList {
        val main = mainField[device.kind]
        if (main != null && device.state.containsKey(main)) {
            val on = flag(device.state, main)
            add(when (main) {
                "triggered" -> if (on) tr("ACTIVADO") else tr("en reposo")
                "open" -> if (on) tr("abierto") else tr("cerrado")
                "locked" -> if (on) tr("cerrada con llave") else tr("abierta")
                else -> if (on) tr("encendido") else tr("apagado")
            })
        }
        number(device.state, "temperature")?.let { add("%.1f °C".format(java.util.Locale.US, it)) }
        number(device.state, "humidity")?.let { add("${it.toInt()} %") }
        number(device.state, "lux")?.let { add("${it.toInt()} lx") }
        number(device.state, "brightness")?.let { add(trf("brillo %s %%", it.toInt())) }
        number(device.state, "power_w")?.let { add("%.1f W".format(java.util.Locale.US, it)) }
        number(device.state, "voltage_v")?.let { add("%.1f V".format(java.util.Locale.US, it)) }
        number(device.state, "current_a")?.let { add("%.2f A".format(java.util.Locale.US, it)) }
        number(device.state, "energy_kwh")?.let { add("%.2f kWh".format(java.util.Locale.US, it)) }
        number(device.state, "co_ppm")?.let { add("${it.toInt()} ppm") }
        number(device.state, "battery")?.let { add(trf("batería %s %%", it.toInt())) }
        if (flag(device.state, "tamper")) add(tr("manipulado"))
    }

    /** Why a device needs a person: triggered, tampered with, low battery or silent; null when all is well. */
    fun problem(device: SiteDevice): String? {
        val main = mainField[device.kind]
        if (device.kind in alarmKinds && main != null && flag(device.state, main)) return tr("activado")
        if (flag(device.state, "tamper")) return tr("manipulado")
        if ((number(device.state, "battery") ?: 100.0) < 15.0) return tr("batería baja")
        if (!device.online && device.expectedIntervalS > 0) return tr("sin respuesta")
        return null
    }

    /** What an alert of a battery means and what to check, in two lines (from the makers' manuals and what can be verified); null for an alert that has no such text. */
    fun alarmHelp(code: String): String? = when (code) {
        "solar_battery_overvoltage" -> tr("Significa: El pack supera la tensión a la que actúa la propia protección de la batería, así que puede cortar la carga.\nRevisa: La tensión de carga ajustada en el inversor o cargador frente al manual del modelo (modelos de 48 V: 52,5 a 53,5 V) y que la batería y el inversor se comunican de verdad.")
        "solar_battery_low" -> tr("Significa: La carga de la batería es baja. Una batería descargada hasta su límite de tensión baja se desconecta sola para protegerse.\nRevisa: Que los paneles o la red la están cargando y cuánto consume la carga. Tras una desconexión, el manual indica cargarla hasta que se apague la luz roja (despacio, si está muy baja).")
        "solar_battery_alarm" -> tr("Significa: Un módulo informa de una alarma o de una protección (luz roja Alarm parpadeando = alarma, fija = protección).\nRevisa: La luz roja, el registro de eventos del módulo aquí y su temperatura (carga solo entre 0 y 50 °C), corriente y tensión frente al manual. Si la luz roja sigue y nada lo explica, contacta con el distribuidor.")
        "solar_battery_hot" -> tr("Significa: Una batería está más caliente que el límite fijado en los niveles de alarma. Los manuales admiten cargar hasta 50 °C y aconsejan de 10 a 40 °C.\nRevisa: La ventilación (evita espacios cerrados), la distancia a fuentes de calor y a la salida de aire del inversor, y el sol directo.")
        "solar_battery_cold" -> tr("Significa: Una batería se está cargando más fría que el límite bajo. Los manuales solo admiten cargar entre 0 y 50 °C.\nRevisa: Dónde está instalada la batería: muévela o caldea la sala, y limita la corriente de carga del inversor hasta que vuelva al rango.")
        "solar_cells_unbalanced" -> tr("Significa: La diferencia entre la celda más alta y la más baja supera el nivel fijado aquí (unos 100 mV con la carga casi completa es una cifra habitual, no oficial).\nRevisa: Deja que la batería llegue a carga completa: el manual dice que el BMS equilibra las celdas mientras cicla. Si el mismo módulo y celda se repiten en los extremos del día, ese módulo es el que hay que mirar.")
        "solar_counter_rose" -> tr("Significa: Un contador de protección del historial de un módulo ha subido: esa protección ha actuado. Lo que significa cada contador no es oficial.\nRevisa: Qué contador es (en los contadores del módulo) y qué hacía entonces el pack: tensión de carga, corriente y temperatura. El registro de eventos del módulo muestra el momento.")
        "solar_cell_soh_abnormal" -> tr("Significa: La consola de la propia batería llama Abnormal al estado de salud de una celda: esa celda es la débil del módulo.\nRevisa: La celda que nombra la alerta (contada desde 1) en las barras del módulo: si su tensión es baja o se repite en los extremos del día, díselo al distribuidor; los números son de la consola, no un veredicto oficial.")
        "solar_module_overcurrent" -> tr("Significa: Un módulo lleva más corriente que el límite que indica en su info. Los manuales dicen que su protección actúa por encima de su corriente máxima.\nRevisa: Los ajustes de corriente de carga y descarga del inversor o cargador, el consumo y cuántos módulos reparten la corriente (más módulos en paralelo la reparten).")
        "solar_offline" -> tr("Significa: El nodo ya no recibe respuestas de este equipo solar (o el propio nodo está en silencio).\nRevisa: El cable y sus pines (consola RJ45: pin 3 TX de la batería, pin 6 RX de la batería, pin 8 masa), la velocidad (consola 115200; una unidad antigua puede usar 19200; RS485 con el DIP 1: 1 = 9600, 0 = 115200). Una batería puede estar dormida: la ayuda del nodo explica cómo despertarla.")
        "solar_battery_worn" -> tr("Significa: La capacidad que la batería ha aprendido está por debajo del nivel fijado aquí frente a su capacidad nominal: guarda menos que cuando era nueva.\nRevisa: Los ciclos y la salud de cada módulo: si uno está muy por debajo de los demás, ese es el que hay que cambiar; las temperaturas extremas acortan la vida de la batería.")
        else -> null
    }

    fun alarmText(code: String) = when (code) {
        "solar_cell_soh_abnormal" -> tr("Una celda de un módulo de batería está marcada como anormal por la propia batería"); "solar_module_overcurrent" -> tr("Un módulo de batería lleva más corriente de la que dice admitir")
        "intrusion" -> tr("Intrusión detectada por los radares"); "node_down" -> tr("Un nodo de campo no responde"); "camera_motion" -> tr("Una cámara ha visto movimiento"); "camera_down" -> tr("Una cámara no responde")
        "smoke" -> tr("Humo detectado"); "co" -> tr("Monóxido de carbono detectado"); "gas" -> tr("Gas detectado"); "water_leak" -> tr("Inundación detectada"); "panic" -> tr("Botón de pánico pulsado")
        "door_open" -> tr("Puerta abierta con el sistema armado"); "window_open" -> tr("Ventana abierta con el sistema armado"); "motion" -> tr("Movimiento con el sistema armado")
        "glass_break" -> tr("Rotura de cristal con el sistema armado"); "vibration" -> tr("Vibración con el sistema armado"); "tamper" -> tr("Un dispositivo ha sido manipulado")
        "low_battery" -> tr("Batería baja"); "device_offline" -> tr("Un dispositivo ha dejado de responder"); "triggered" -> tr("Un dispositivo se ha activado")
        "solar_fault" -> tr("Avería en un inversor solar"); "solar_battery_low" -> tr("Batería solar baja"); "solar_battery_alarm" -> tr("Una batería solar avisa de un problema")
        "solar_offline" -> tr("Un equipo solar ha dejado de responder"); "solar_cells_unbalanced" -> tr("Las celdas de una batería están desequilibradas")
        "solar_battery_hot" -> tr("Una batería está demasiado caliente"); "solar_battery_cold" -> tr("Una batería se está cargando con demasiado frío"); "solar_battery_worn" -> tr("Una batería está muy desgastada"); "solar_battery_overvoltage" -> tr("Una batería supera la tensión a la que actúa su propia protección"); "solar_counter_rose" -> tr("Ha subido un contador de protección de un módulo de batería")
        "solar_inverter_hot" -> tr("Un inversor está demasiado caliente")
        "electrical_alarm" -> tr("Un contador de la red eléctrica avisa de una alarma"); "electrical_voltage" -> tr("La tensión de red está fuera de rango")
        "electrical_grid_lost" -> tr("Se ha perdido el suministro de la red"); "electrical_offline" -> tr("Un nodo eléctrico ha dejado de responder")
        "electrical_switch_fault" -> tr("Un conmutador de fuentes de la red eléctrica tiene una avería y queda abierto")
        "network_internet_down" -> tr("No hay internet y el router sigue respondiendo: es cosa del operador"); "network_lan_down" -> tr("La red local está caída: el router no responde")
        "network_degraded" -> tr("Internet va lento o pierde paquetes"); "network_new_device" -> tr("Un dispositivo que nunca se había visto se ha unido a la red")
        "network_arp_conflict" -> tr("Dos máquinas responden por una misma dirección de la red"); "network_port_opened" -> tr("Un dispositivo de la red ha abierto un puerto")
        "network_offline" -> tr("Un nodo de red ha dejado de informar"); "network_watched_online" -> tr("Un dispositivo que pediste vigilar ha vuelto a la red")
        "alarm_sounding" -> tr("¡La alarma está sonando!"); "alarm_tamper" -> tr("Una zona de la alarma indica sabotaje (cable cortado o en cortocircuito)")
        "alarm_zone_trouble" -> tr("Una zona de la alarma no se puede leer: no está vigilando")
        "alarm_locked_out" -> tr("La central de alarma está bloqueada tras PIN erróneos"); "alarm_offline" -> tr("Un nodo de alarma ha dejado de responder")
        "duress_disarm" -> tr("El sistema se desarmó con el PIN de coacción: puede que alguien esté siendo obligado")
        else -> code
    }

    /** What to ask before a command to a device of that risk (null: nothing, a click is enough). */
    fun confirmQuestion(device: SiteDevice, command: String): String? = when (device.risk) {
        "circuit" -> trf("«%s» es un circuito del cuadro eléctrico. ¿Seguro que quieres %s su alimentación?", device.name, commandVerb(command))
        "critical" -> trf("«%s» es crítico (un congelador, una bomba, la alarma...). Solo un administrador puede ordenarlo. ¿Seguro que quieres %s su alimentación?", device.name, commandVerb(command))
        else -> null
    }
    private fun commandVerb(command: String) = when (command) { "on" -> tr("conectar"); "off" -> tr("cortar"); else -> tr("cambiar") }

    fun severityLabel(severity: String) = when (severity) { "critical" -> tr("CRÍTICA"); "high" -> tr("ALTA"); else -> tr("AVISO") }
}
