// ARMOR-ANDROID-CONTROL - the protocol of a field node's Bluetooth configuration channel (ARMOR-RADAR, docs/BLE_PROVISIONING.md), without the radio.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** The GATT identifiers of a node: one service, a characteristic the app writes and one the node notifies. */
object NodeGatt {
    const val SERVICE = "a5c0de00-5261-4d4f-8000-41524d4f5200"
    const val RX = "a5c0de01-5261-4d4f-8000-41524d4f5200"   // the app writes requests here
    const val TX = "a5c0de02-5261-4d4f-8000-41524d4f5200"   // the node notifies answers here
    const val PREFERRED_MTU = 247
}

/** A message on the byte stream: [length: 2 bytes, big-endian][UTF-8 JSON], at most [MAX_MESSAGE] bytes; the stream may be cut anywhere. */
object BleFrame {
    const val MAX_MESSAGE = 6000

    /** The bytes to send for one message, or null when it is empty or too long. */
    fun frame(json: String): ByteArray? {
        val body = json.toByteArray(Charsets.UTF_8)
        if (body.isEmpty() || body.size > MAX_MESSAGE) return null
        return byteArrayOf((body.size shr 8).toByte(), (body.size and 0xFF).toByte()) + body
    }

    /** The pieces a framed message goes out in: at most [size] bytes each (an ATT payload, MTU - 3). */
    fun chunks(framed: ByteArray, size: Int): List<ByteArray> {
        require(size > 0)
        return framed.toList().chunked(size).map { it.toByteArray() }
    }
}

/** Collects the notifications of the node and hands over whole messages. A length of zero or above the maximum discards what was collected. */
class BleAssembler {
    private var buffer = ByteArray(0)
    var errors = 0
        private set

    /** Adds bytes and returns every message they complete, in order. */
    fun feed(bytes: ByteArray): List<String> {
        buffer += bytes
        val messages = mutableListOf<String>()
        while (buffer.size >= 2) {
            val wanted = ((buffer[0].toInt() and 0xFF) shl 8) or (buffer[1].toInt() and 0xFF)
            if (wanted == 0 || wanted > BleFrame.MAX_MESSAGE) { buffer = ByteArray(0); errors += 1; break }
            if (buffer.size < 2 + wanted) break
            messages += String(buffer, 2, wanted, Charsets.UTF_8)
            buffer = buffer.copyOfRange(2 + wanted, buffer.size)
        }
        return messages
    }

    fun reset() { buffer = ByteArray(0) }
}

/** An answer of the node: [ok] with its [data], or an [error] code (see the protocol's list) and perhaps [data]. */
data class NodeReply(val id: Long, val ok: Boolean, val error: String, val data: JSONObject) {
    private fun problemText(path: String, code: String): String = when {
        path == "uplink" && code == "not_available" -> "uplink: este nodo no tiene puerto Ethernet, usa el Wi-Fi"
        path == "sta.enabled" && code == "required" -> "sta.enabled: sin Wi-Fi ni punto de acceso el nodo quedaría inalcanzable"
        else -> "$path: $code"
    }

    /** The fields the node found wrong in a settings document, as "path: code". */
    fun problems(): List<String> {
        val list: JSONArray = data.optJSONArray("problems") ?: return emptyList()
        return (0 until list.length()).map { val item = list.getJSONObject(it); problemText(item.optString("path"), item.optString("code")) }
    }
}

object NodeProtocol {
    /** A request as JSON text. */
    fun request(id: Long, op: String, args: JSONObject? = null): String =
        JSONObject().put("id", id).put("op", op).apply { if (args != null) put("args", args) }.toString()

    /** Reads an answer; null when the text is not one. */
    fun reply(text: String): NodeReply? = runCatching {
        val json = JSONObject(text)
        if (!json.has("id") || !json.has("ok")) return null
        NodeReply(json.getLong("id"), json.getBoolean("ok"), json.optString("error"), json.optJSONObject("data") ?: JSONObject())
    }.getOrNull()

    /** Why a node could not join the Wi-Fi network it was given, in Spanish. */
    fun staErrorText(code: String): String = when (code) {
        "network_not_found" -> "El nodo no encuentra esa red Wi-Fi: revisa el nombre, que esté al alcance y que sea de 2,4 GHz (el nodo no usa 5 GHz)."
        "wrong_password" -> "La contraseña del Wi-Fi no es correcta (o el router no acepta la conexión)."
        else -> "El nodo no consiguió conectarse al Wi-Fi."
    }

    /** The Spanish words for an error code of the node. */
    fun errorText(code: String): String = when (code) {
        "wrong_code" -> "El código de configuración no es correcto."
        "wrong_credentials" -> "Usuario o contraseña incorrectos."
        "too_many_attempts" -> "Demasiados intentos: espera un momento."
        "invalid_name" -> "El nombre de usuario no es válido (3 a 32 caracteres: a-z, 0-9, . _ -)."
        "weak_password" -> "La contraseña debe tener de 8 a 64 caracteres."
        "unauthorized" -> "Hay que iniciar sesión."
        "forbidden" -> "No tienes permiso para eso."
        "setup_required" -> "El nodo aún no tiene usuarios: crea el administrador."
        "invalid" -> "Algunos valores no son válidos."
        "storage" -> "El nodo no pudo escribir en su memoria."
        "wifi_busy" -> "La radio del nodo está ocupada: inténtalo de nuevo."
        "scan_failed" -> "La búsqueda de redes ha fallado."
        "wifi_unavailable" -> "No se pudo arrancar el Wi-Fi del nodo."
        "timeout" -> "El nodo no respondió a tiempo."
        "network_not_found" -> staErrorText(code)
        "disconnected" -> "Se perdió la conexión con el nodo."
        else -> "El nodo respondió con un error ($code)."
    }
}

/** What a node has done with the connection it was given, read from its `hello` after it restarted. */
sealed interface NodeOutcome {
    /** It has an address: [ssid] is the Wi-Fi network it joined (empty on a cable). */
    data class Online(val ip: String, val ssid: String) : NodeOutcome
    /** It tried and could not: [reason] is in plain words. */
    data class Failed(val reason: String) : NodeOutcome
    /** It is on, but has neither an address nor a reason yet. */
    object Waiting : NodeOutcome

    companion object {
        fun of(hello: NodeHello): NodeOutcome = when {
            hello.hasIp -> Online(hello.ip, if (hello.staConnected) hello.staSsid else "")
            hello.staError.isNotBlank() -> Failed(NodeProtocol.staErrorText(hello.staError))
            else -> Waiting
        }
    }
}

/** A Wi-Fi network the node heard. */
data class WifiNetwork(val ssid: String, val rssi: Int, val channel: Int, val security: String) {
    companion object {
        fun list(data: JSONObject): List<WifiNetwork> {
            val array = data.optJSONArray("networks") ?: return emptyList()
            return (0 until array.length()).map { val n = array.getJSONObject(it); WifiNetwork(n.optString("ssid"), n.optInt("rssi"), n.optInt("channel"), n.optString("security")) }
        }
    }
}

/** The kinds of field node that answer on the configuration channel; they all speak the same protocol, so the app configures each of them the same way. */
enum class NodeKind(val label: String) {
    Radar("Nodo radar"), Solar("Nodo solar"), Electrical("Nodo eléctrico"), Unknown("Nodo ARMOR");

    companion object {
        /** From the `kind` a node declares in its `hello`; a node that predates it is told by its identifier when that says so, and is otherwise just an ARMOR node. */
        fun of(declared: String, nodeId: String = ""): NodeKind = when {
            declared.equals("radar", true) -> Radar
            declared.equals("solar", true) -> Solar
            declared.equals("electrical", true) -> Electrical
            declared.isBlank() && nodeId.startsWith("solar-", true) -> Solar
            declared.isBlank() && nodeId.startsWith("electrical-", true) -> Electrical
            else -> Unknown
        }
    }
}

/** What the node says about itself before anyone signs in. */
data class NodeHello(val nodeId: String, val name: String, val mac: String, val firmware: String, val setup: Boolean, val hasIp: Boolean, val ip: String, val staConnected: Boolean, val staSsid: String,
                     val kind: NodeKind = NodeKind.Unknown, val staError: String = "", val apActive: Boolean = false) {
    companion object {
        fun from(data: JSONObject) = NodeHello(data.optString("node_id"), data.optString("name"), data.optString("mac"), data.optString("firmware"), data.optBoolean("setup"),
            data.optBoolean("has_ip"), data.optString("ip"), data.optBoolean("sta_connected"), data.optString("sta_ssid"), NodeKind.of(data.optString("kind"), data.optString("node_id")),
            data.optString("sta_error"), data.optBoolean("ap_active"))
    }
}

/** The set-up code of a board on a fleet: HMAC-SHA256 of its MAC (twelve lowercase hexadecimal digits) with the fleet secret, ten symbols of it. The node and tools/adopt_node.py make the same code. */
object SetupCode {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun derive(secret: String, mac: String): String? {
        val digits = mac.lowercase().filter { it in '0'..'9' || it in 'a'..'f' }
        if (digits.length != 12 || secret.length < 16) return null
        val mac256 = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")) }
        val digest = mac256.doFinal(digits.toByteArray(Charsets.US_ASCII))
        return (0 until 10).map { ALPHABET[(digest[it].toInt() and 0xFF) % 31] }.joinToString("")
    }
}

/** The partial settings document the app sends, built from what the operator filled in; an empty field is left out, so it changes nothing. */
object NodeSettingsPatch {
    data class Form(
        val name: String = "",
        val useWifi: Boolean = false,
        val wifiSsid: String = "",
        val wifiPassword: String = "",
        val fixedAddress: Boolean = false,
        val address: String = "",
        val netmask: String = "255.255.255.0",
        val gateway: String = "",
        val dns: String = "",
        val brokerUri: String = "",
        val brokerUser: String = "",
        val brokerPassword: String = "",
        val bluetoothMode: String = "",   // "", "setup", "always" or "off"
    )

    private fun octets(text: String): List<Int>? {
        val parts = text.trim().split(".")
        if (parts.size != 4 || parts.any { it.length !in 1..3 }) return null
        val values = parts.map { it.toIntOrNull() ?: return null }
        return if (values.all { it in 0..255 }) values else null
    }
    private fun asInt(o: List<Int>): Long = o.fold(0L) { acc, v -> (acc shl 8) or v.toLong() }

    /** What is wrong with the form before it is sent, in Spanish; empty when it can be sent. The node checks it again and says which field it refuses. */
    fun problems(form: Form): List<String> {
        val found = mutableListOf<String>()
        if (form.useWifi) {
            if (form.wifiSsid.isBlank()) found += "Falta el nombre de la red Wi-Fi."
            if (form.wifiPassword.isNotEmpty() && form.wifiPassword.length !in 8..63) found += "La contraseña del Wi-Fi tiene de 8 a 63 caracteres."
        }
        if (form.fixedAddress) {
            val address = octets(form.address); val mask = octets(form.netmask); val gateway = octets(form.gateway)
            if (address == null) found += "La dirección IP no es válida (por ejemplo 192.168.0.60)."
            if (mask == null) found += "La máscara no es válida (por ejemplo 255.255.255.0)."
            if (gateway == null) found += "La puerta de enlace no es válida (por ejemplo 192.168.0.1)."
            if (form.dns.isNotBlank() && octets(form.dns) == null) found += "El DNS no es válido."
            if (address != null && mask != null && gateway != null) {
                val m = asInt(mask)
                if ((asInt(address) and m) != (asInt(gateway) and m)) found += "La puerta de enlace tiene que estar en la misma red que la dirección."
                if (asInt(address) == asInt(gateway)) found += "La dirección del nodo y la puerta de enlace no pueden ser la misma."
            }
        }
        return found
    }

    fun build(form: Form): JSONObject {
        val patch = JSONObject()
        if (form.name.isNotBlank()) patch.put("node", JSONObject().put("name", form.name.trim()))
        patch.put("uplink", if (form.useWifi) "wifi" else "ethernet")
        if (form.useWifi) {
            patch.put("sta", JSONObject().put("enabled", true).put("ssid", form.wifiSsid).apply { if (form.wifiPassword.isNotEmpty()) put("password", form.wifiPassword) })
        }
        // DHCP or a fixed address, on whichever connection the node uses: the cable or the Wi-Fi station
        val ip = JSONObject().put("dhcp", !form.fixedAddress)
        if (form.fixedAddress) ip.put("address", form.address.trim()).put("netmask", form.netmask.trim()).put("gateway", form.gateway.trim()).put("dns1", form.dns.trim().ifEmpty { form.gateway.trim() })
        patch.put("ip", ip)
        if (form.brokerUri.isNotBlank()) {
            patch.put("mqtt", JSONObject().put("enabled", true).put("uri", form.brokerUri.trim()).apply {
                if (form.brokerUser.isNotBlank()) put("username", form.brokerUser.trim())
                if (form.brokerPassword.isNotEmpty()) put("password", form.brokerPassword)
            })
        }
        if (form.bluetoothMode in setOf("setup", "always", "off")) patch.put("ble", JSONObject().put("mode", form.bluetoothMode))
        return patch
    }
}
