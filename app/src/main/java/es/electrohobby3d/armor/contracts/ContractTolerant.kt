// ARMOR-ANDROID-CONTROL - reading a message of the shared contract without losing it to a member the app does not know.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.contracts

import org.json.JSONArray
import org.json.JSONObject

/**
 * The document without the members that a closed object of the schema (`additionalProperties: false`) does not name, at every level. The members that an open object may carry
 * (a map of counts, say) and everything the schema does name are left as they are, so a wrong value, a missing member or a bad pattern still fail the check. A newer server that
 * adds a member must not empty a screen of an older app; a document that is really wrong still gets nothing. The document given is never changed. Null when no message has that name.
 */
fun withoutUnknownMembers(name: String, document: JSONObject): JSONObject? {
    val schema = messageSchema(name) ?: return null
    return pruned(document, schema) as? JSONObject
}

/** [parseMessage] after [withoutUnknownMembers]: the data class of the message called [name], or null when the document is not that message even without its unknown members. */
fun parseTolerant(name: String, document: JSONObject): Any? {
    val cleaned = withoutUnknownMembers(name, document) ?: return null
    return parseMessage(name, cleaned)
}

private fun pruned(value: Any?, schema: JSONObject): Any? {
    if (value is JSONObject) {
        val properties = schema.optJSONObject("properties") ?: JSONObject()
        val extra = schema.opt("additionalProperties")
        val closed = extra == false
        val out = JSONObject()
        for (member in value.keys().asSequence().toList()) {
            val rule = properties.optJSONObject(member)
            when {
                rule != null -> out.put(member, pruned(value.get(member), rule))
                closed -> Unit
                extra is JSONObject -> out.put(member, pruned(value.get(member), extra))
                else -> out.put(member, value.get(member))
            }
        }
        return out
    }
    if (value is JSONArray) {
        val items = schema.optJSONObject("items") ?: return value
        val out = JSONArray()
        for (index in 0 until value.length()) out.put(pruned(value.get(index), items))
        return out
    }
    return value
}
