// ARMOR-ANDROID-CONTROL - the Bluetooth Low Energy link to a field node: finds it, connects, and carries the framed JSON of its configuration channel.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// The protocol is in ARMOR-RADAR's docs/BLE_PROVISIONING.md and its framing in model/NodeBle.kt (tested on the JVM). This file is the part that needs a radio:
// it has been compiled but never run against a node or a phone.
package es.electrohobby3d.armor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import es.electrohobby3d.armor.model.BleAssembler
import es.electrohobby3d.armor.model.BleFrame
import es.electrohobby3d.armor.model.NodeGatt
import es.electrohobby3d.armor.model.NodeProtocol
import es.electrohobby3d.armor.model.NodeReply
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** A node heard while scanning. */
data class FoundNode(val address: String, val name: String, val rssi: Int, val device: BluetoothDevice)

@SuppressLint("MissingPermission")   // the screen asks for the Bluetooth permissions before it uses this class
class NodeBleClient(private val context: Context) {
    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var rx: BluetoothGattCharacteristic? = null
    private var tx: BluetoothGattCharacteristic? = null
    private val assembler = BleAssembler()
    private val waiting = ConcurrentHashMap<Long, CompletableDeferred<NodeReply>>()
    private var nextId = 1L
    private var mtu = 23
    private var stepDone: CompletableDeferred<Int>? = null   // the result of the GATT operation in progress (0 = success)
    private var connected: CompletableDeferred<Boolean>? = null

    val bluetoothOn: Boolean get() = adapter?.isEnabled == true

    // ---- finding nodes ---------------------------------------------------------------------------------------------------------------

    fun startScan(onFound: (FoundNode) -> Unit): Boolean {
        val scanner = adapter?.bluetoothLeScanner ?: return false
        stopScan()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: result.device.name ?: return
                if (name.startsWith("ARMOR-")) onFound(FoundNode(result.device.address, name, result.rssi, result.device))
            }
        }
        scanCallback = callback
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(NodeGatt.SERVICE))).build()
        scanner.startScan(listOf(filter), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        return true
    }

    fun stopScan() {
        val callback = scanCallback ?: return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        scanCallback = null
    }

    // ---- the connection --------------------------------------------------------------------------------------------------------------

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                g.requestMtu(NodeGatt.PREFERRED_MTU)   // the services are discovered once the MTU answer arrives
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                connected?.complete(false)
                stepDone?.complete(-1)
                failAll("disconnected")
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, negotiated: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) mtu = negotiated
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(UUID.fromString(NodeGatt.SERVICE))
            rx = service?.getCharacteristic(UUID.fromString(NodeGatt.RX))
            tx = service?.getCharacteristic(UUID.fromString(NodeGatt.TX))
            val notifier = tx
            if (status != BluetoothGatt.GATT_SUCCESS || rx == null || notifier == null) { connected?.complete(false); return }
            g.setCharacteristicNotification(notifier, true)
            val descriptor = notifier.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor == null) { connected?.complete(false); return }
            writeDescriptor(g, descriptor)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) { connected?.complete(status == BluetoothGatt.GATT_SUCCESS) }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) { stepDone?.complete(status) }

        // Android 13 and later call this one; older versions call the one below with the value inside the characteristic.
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) { received(value) }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) @Suppress("DEPRECATION") received(characteristic.value ?: return)
        }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptor(g: BluetoothGatt, descriptor: BluetoothGattDescriptor) {
        if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        else { descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; g.writeDescriptor(descriptor) }
    }

    private fun received(bytes: ByteArray) {
        for (text in assembler.feed(bytes)) {
            val reply = NodeProtocol.reply(text) ?: continue
            waiting.remove(reply.id)?.complete(reply)
        }
    }

    private fun failAll(code: String) {
        val it = waiting.entries.iterator()
        while (it.hasNext()) { val entry = it.next(); entry.value.complete(NodeReply(entry.key, false, code, JSONObject())); it.remove() }
    }

    /** Connects and gets the channel ready (MTU, services, notifications). False when the node cannot be reached or is not an A.R.M.O.R. node. */
    suspend fun connect(node: FoundNode, timeoutMs: Long = 20_000): Boolean {
        stopScan()
        disconnect()
        assembler.reset()
        val ready = CompletableDeferred<Boolean>()
        connected = ready
        gatt = node.device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        val ok = withTimeoutOrNull(timeoutMs) { ready.await() } ?: false
        if (!ok) disconnect()
        return ok
    }

    fun disconnect() {
        runCatching { gatt?.disconnect(); gatt?.close() }
        gatt = null; rx = null; tx = null; connected = null
        failAll("disconnected")
    }

    // ---- requests -----------------------------------------------------------------------------------------------------------------------

    /**
     * Sends one request and waits for its answer. The first write can meet the pairing: Android pairs when the node asks, and until it is done a write
     * comes back as "insufficient authentication or encryption", so that is retried for a few seconds.
     */
    suspend fun request(op: String, args: JSONObject? = null, timeoutMs: Long = 30_000): NodeReply {
        val g = gatt; val target = rx
        if (g == null || target == null) return NodeReply(0, false, "disconnected", JSONObject())
        val id = nextId++
        val answer = CompletableDeferred<NodeReply>()
        waiting[id] = answer
        val framed = BleFrame.frame(NodeProtocol.request(id, op, args)) ?: run { waiting.remove(id); return NodeReply(id, false, "invalid", JSONObject()) }
        for (piece in BleFrame.chunks(framed, (mtu - 3).coerceAtLeast(20))) {
            var written = false
            for (attempt in 0 until 12) {
                val done = CompletableDeferred<Int>()
                stepDone = done
                if (!writeChunk(g, target, piece)) { delay(500); continue }
                val status = withTimeoutOrNull(5_000) { done.await() } ?: -2
                if (status == BluetoothGatt.GATT_SUCCESS) { written = true; break }
                if (status == -1) break   // disconnected
                delay(1_000)              // pairing in progress (status 5, 8, 15) or a busy stack: try again
            }
            if (!written) { waiting.remove(id); return NodeReply(id, false, "disconnected", JSONObject()) }
        }
        return withTimeoutOrNull(timeoutMs) { answer.await() } ?: run { waiting.remove(id); NodeReply(id, false, "timeout", JSONObject()) }
    }

    @Suppress("DEPRECATION")
    private fun writeChunk(g: BluetoothGatt, target: BluetoothGattCharacteristic, bytes: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= 33) g.writeCharacteristic(target, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
        else { target.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT; target.value = bytes; g.writeCharacteristic(target) }
}
