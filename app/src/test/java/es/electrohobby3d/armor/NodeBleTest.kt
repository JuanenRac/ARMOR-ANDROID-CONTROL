package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.BleAssembler
import es.electrohobby3d.armor.model.BleFrame
import es.electrohobby3d.armor.model.NodeGatt
import es.electrohobby3d.armor.model.NodeHello
import es.electrohobby3d.armor.model.NodeKind
import es.electrohobby3d.armor.model.NodeProtocol
import es.electrohobby3d.armor.model.NodeSettingsPatch
import es.electrohobby3d.armor.model.SetupCode
import es.electrohobby3d.armor.model.WifiNetwork
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Bluetooth channel to a field node: framing, requests and answers, the set-up code and the settings the app sends. The radio itself is not tested here. */
class NodeBleTest {
    private val message = """{"id":1,"op":"hello"}"""

    @Test fun `a message is its length in two bytes and its JSON`() {
        val framed = BleFrame.frame(message)!!
        assertEquals(message.length + 2, framed.size)
        assertEquals(0, framed[0].toInt()); assertEquals(message.length, framed[1].toInt() and 0xFF)
        assertNull(BleFrame.frame(""))
        assertNull(BleFrame.frame("x".repeat(BleFrame.MAX_MESSAGE + 1)))
        assertNotNull(BleFrame.frame("x".repeat(BleFrame.MAX_MESSAGE)))
    }

    @Test fun `any way of cutting the stream gives the message back`() {
        val framed = BleFrame.frame(message)!!
        for (size in listOf(1, 2, 3, 7, 20, 244, 500)) {
            val assembler = BleAssembler()
            val got = BleFrame.chunks(framed, size).flatMap { assembler.feed(it) }
            assertEquals("cut in $size", listOf(message), got)
        }
    }

    @Test fun `two messages in one write come out one by one and a bad length is dropped`() {
        val second = """{"id":2,"op":"status"}"""
        val assembler = BleAssembler()
        assertEquals(listOf(message, second), assembler.feed(BleFrame.frame(message)!! + BleFrame.frame(second)!!))
        assertEquals(emptyList<String>(), assembler.feed(byteArrayOf(0, 0)))
        assertEquals(emptyList<String>(), assembler.feed(byteArrayOf(0xFF.toByte(), 0xFF.toByte())))
        assertEquals(2, assembler.errors)
        assertEquals(listOf(message), assembler.feed(BleFrame.frame(message)!!))   // and it understands the next one
    }

    @Test fun `UTF-8 survives the framing`() {
        val text = """{"id":3,"op":"x","args":{"name":"Perímetro norte 日本"}}"""
        assertEquals(listOf(text), BleAssembler().feed(BleFrame.frame(text)!!))
    }

    @Test fun `a request is JSON with an id and an operation`() {
        val json = JSONObject(NodeProtocol.request(7, "login", JSONObject().put("user", "admin").put("password", "x")))
        assertEquals(7, json.getLong("id")); assertEquals("login", json.getString("op")); assertEquals("admin", json.getJSONObject("args").getString("user"))
        assertFalse(JSONObject(NodeProtocol.request(8, "hello")).has("args"))
    }

    @Test fun `answers are read, with their data and their errors`() {
        val ok = NodeProtocol.reply("""{"id":7,"ok":true,"data":{"role":"admin"}}""")!!
        assertTrue(ok.ok); assertEquals(7, ok.id); assertEquals("admin", ok.data.getString("role"))
        val bad = NodeProtocol.reply("""{"id":9,"ok":false,"error":"invalid","data":{"problems":[{"path":"ap.ssid","code":"required"},{"path":"mqtt.uri","code":"invalid"}]}}""")!!
        assertFalse(bad.ok); assertEquals("invalid", bad.error); assertEquals(listOf("ap.ssid: required", "mqtt.uri: invalid"), bad.problems())
        assertNull(NodeProtocol.reply("nonsense")); assertNull(NodeProtocol.reply("""{"id":1}"""))
        assertTrue(NodeProtocol.errorText("wrong_code").contains("código")); assertTrue(NodeProtocol.errorText("zzz").contains("zzz"))
    }

    @Test fun `what the node says about itself and the networks it hears`() {
        val hello = NodeHello.from(JSONObject("""{"node_id":"armor-a1b2c3","name":"Valla norte","mac":"34:85:18:a1:b2:c3","firmware":"0.2.3","setup":true,"has_ip":false,"ip":"","sta_connected":false,"sta_ssid":""}"""))
        assertEquals("armor-a1b2c3", hello.nodeId); assertTrue(hello.setup); assertFalse(hello.hasIp); assertEquals("34:85:18:a1:b2:c3", hello.mac)
        val list = WifiNetwork.list(JSONObject("""{"networks":[{"ssid":"Casa","rssi":-48,"channel":6,"security":"wpa2"},{"ssid":"Abierta","rssi":-80,"channel":1,"security":"open"}]}"""))
        assertEquals(listOf("Casa", "Abierta"), list.map { it.ssid }); assertEquals(-48, list[0].rssi); assertEquals("open", list[1].security)
        assertEquals(emptyList<WifiNetwork>(), WifiNetwork.list(JSONObject("{}")))
    }

    @Test fun `the three kinds of node are told apart by what they declare`() {
        fun hello(json: String) = NodeHello.from(JSONObject(json))
        assertEquals(NodeKind.Radar, hello("""{"kind":"radar","node_id":"armor-a1b2c3"}""").kind)
        assertEquals(NodeKind.Solar, hello("""{"kind":"solar","node_id":"casa-solar"}""").kind)
        assertEquals(NodeKind.Electrical, hello("""{"kind":"electrical","node_id":"cuadro"}""").kind)
        // a node that predates the field: told by its identifier when it says so, otherwise an ARMOR node
        assertEquals(NodeKind.Solar, hello("""{"node_id":"solar-a1b2c3"}""").kind)
        assertEquals(NodeKind.Electrical, hello("""{"node_id":"electrical-a1b2c3"}""").kind)
        assertEquals(NodeKind.Unknown, hello("""{"node_id":"armor-a1b2c3"}""").kind)
        assertEquals(NodeKind.Unknown, hello("""{"kind":"other","node_id":"solar-x"}""").kind)
        assertEquals("Nodo eléctrico", NodeKind.Electrical.label)
    }

    @Test fun `the fields a node refuses are told in plain words`() {
        val reply = NodeProtocol.reply("""{"id":3,"ok":false,"error":"invalid","data":{"problems":[{"path":"uplink","code":"not_available"},{"path":"sta.enabled","code":"required"},{"path":"mqtt.uri","code":"invalid"}]}}""")!!
        val problems = reply.problems()
        assertTrue(problems[0].contains("Ethernet") && problems[1].contains("inalcanzable") && problems[2] == "mqtt.uri: invalid")
    }

    @Test fun `the set-up code of a board is the one the firmware and adopt_node make`() {
        // The vector of ARMOR-RADAR's tests: openssl, the firmware's mapping and adopt_node.py all give GD8VZH4HBM.
        val secret = "fleet-secret-for-tests-0123456789"
        assertEquals("GD8VZH4HBM", SetupCode.derive(secret, "a1b2c3d4e5f6"))
        assertEquals("GD8VZH4HBM", SetupCode.derive(secret, "A1:B2:C3:D4:E5:F6"))
        assertEquals(10, SetupCode.derive(secret, "a1b2c3d4e5f7")!!.length)
        assertNull(SetupCode.derive(secret, "a1b2c3"))       // not a MAC
        assertNull(SetupCode.derive("short", "a1b2c3d4e5f6")) // a fleet secret has 16 characters or more
    }

    @Test fun `the settings the app sends change only what was filled in`() {
        val wired = NodeSettingsPatch.build(NodeSettingsPatch.Form(name = " Valla norte "))
        assertEquals("ethernet", wired.getString("uplink")); assertEquals("Valla norte", wired.getJSONObject("node").getString("name"))
        assertTrue(wired.getJSONObject("ip").getBoolean("dhcp")); assertFalse(wired.has("sta")); assertFalse(wired.has("mqtt")); assertFalse(wired.has("ble"))

        val fixed = NodeSettingsPatch.build(NodeSettingsPatch.Form(fixedAddress = true, address = "192.168.0.60", gateway = "192.168.0.1"))
        val ip = fixed.getJSONObject("ip")
        assertFalse(ip.getBoolean("dhcp")); assertEquals("192.168.0.60", ip.getString("address")); assertEquals("255.255.255.0", ip.getString("netmask")); assertEquals("192.168.0.1", ip.getString("dns1"))

        val wifi = NodeSettingsPatch.build(NodeSettingsPatch.Form(useWifi = true, wifiSsid = "Casa", wifiPassword = "clave del router", fixedAddress = true, address = "1.2.3.4"))
        assertEquals("wifi", wifi.getString("uplink")); assertFalse(wifi.has("ip"))   // a node on Wi-Fi always asks for its address
        val sta = wifi.getJSONObject("sta")
        assertTrue(sta.getBoolean("enabled")); assertEquals("Casa", sta.getString("ssid")); assertEquals("clave del router", sta.getString("password"))
        assertFalse(NodeSettingsPatch.build(NodeSettingsPatch.Form(useWifi = true, wifiSsid = "Abierta")).getJSONObject("sta").has("password"))   // an empty password keeps the stored one

        val broker = NodeSettingsPatch.build(NodeSettingsPatch.Form(brokerUri = "mqtt://192.168.0.180:18883", brokerUser = "field-node-x", brokerPassword = "p", bluetoothMode = "always"))
        assertEquals("mqtt://192.168.0.180:18883", broker.getJSONObject("mqtt").getString("uri")); assertEquals("field-node-x", broker.getJSONObject("mqtt").getString("username"))
        assertEquals("always", broker.getJSONObject("ble").getString("mode"))
        assertFalse(NodeSettingsPatch.build(NodeSettingsPatch.Form(bluetoothMode = "loud")).has("ble"))
    }

    @Test fun `the identifiers match the firmware`() {
        assertEquals("a5c0de00-5261-4d4f-8000-41524d4f5200", NodeGatt.SERVICE)
        assertEquals("a5c0de01-5261-4d4f-8000-41524d4f5200", NodeGatt.RX)
        assertEquals("a5c0de02-5261-4d4f-8000-41524d4f5200", NodeGatt.TX)
    }
}
