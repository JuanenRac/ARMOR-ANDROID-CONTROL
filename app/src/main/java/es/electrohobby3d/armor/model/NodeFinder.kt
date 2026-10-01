// ARMOR-ANDROID-CONTROL - finding nodes on the network, for the Solar, Electrical and Radar screens: what the network node has found that looks like an ARMOR
// node (a host name starting with "armor-", which is what a node calls itself, or an Espressif maker) and that is not yet known here. Mirrors ARMOR-STUDIO's
// NodeFinder component.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

private const val HOST_PREFIX = "armor-"

data class NodeCandidate(val ip: String, val mac: String?, val hostname: String?, val vendor: String?, val online: Boolean, val nodeId: String?)

object NodeFinder {
    /** The devices of the network that look like ARMOR nodes and are not among the ones already known (by id). */
    fun candidates(network: NetworkOverview?, knownIds: Collection<String>): List<NodeCandidate> {
        val ids = knownIds.map { it.lowercase() }.toSet()
        val seen = mutableSetOf<String>()
        val found = mutableListOf<NodeCandidate>()
        for (node in network?.nodes.orEmpty()) {
            for (device in node.devices) {
                val hostname = device.hostname?.lowercase()?.removeSuffix(".local")?.removeSuffix(".")
                val named = hostname != null && hostname.startsWith(HOST_PREFIX)
                val espressif = device.vendor?.contains("espressif", ignoreCase = true) == true
                if (!named && !espressif) continue
                val nodeId: String? = if (named) hostname.removePrefix(HOST_PREFIX) else null
                if ((nodeId != null && ids.contains(nodeId)) || seen.contains(device.ip)) continue
                seen.add(device.ip)
                found.add(NodeCandidate(device.ip, device.mac, device.hostname, device.vendor, device.online, nodeId))
            }
        }
        return found.sortedWith(compareByDescending<NodeCandidate> { it.online }.thenBy { it.ip })
    }
}
