// ARMOR-ANDROID-CONTROL - how many nodes of every kind there are and how many answer: the count of the status screen.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

/** One node, whatever its kind ("radar", "solar", "electrical", "alarm" or "hmi"), and whether it answers. */
data class CensusEntry(val id: String, val family: String, val online: Boolean)

/** The nodes of one kind: how many answer out of how many there are. */
data class CensusFamily(val family: String, val online: Int, val total: Int)

/**
 * The radar nodes arrive on the live status stream; every kind of node reports its health to the server. The census takes both: a node that is on the stream is counted with what the
 * stream says, one that only reports its health with what the health says, and one that is in both is online when either says so (and is counted once).
 */
data class NodeCensus(val entries: List<CensusEntry>) {
    val total: Int get() = entries.size
    val online: Int get() = entries.count { it.online }
    val silent: List<CensusEntry> get() = entries.filter { !it.online }
    val families: List<CensusFamily>
        get() = ORDER.mapNotNull { family ->
            val members = entries.filter { it.family == family }
            if (members.isEmpty()) null else CensusFamily(family, members.count { it.online }, members.size)
        }

    companion object {
        private val ORDER = listOf("radar", "solar", "electrical", "alarm", "hmi")

        fun of(radar: List<FieldNode>, health: NodeHealthOverview?): NodeCensus {
            val known = linkedMapOf<String, CensusEntry>()
            health?.nodes?.forEach { known[it.nodeId] = CensusEntry(it.nodeId, it.family, !it.silent) }
            radar.forEach { node ->
                val live = node.online && !node.stale
                val before = known[node.id]
                known[node.id] = CensusEntry(node.id, before?.family ?: "radar", live || (before?.online ?: false))
            }
            return NodeCensus(known.values.sortedBy { it.id })
        }
    }
}
