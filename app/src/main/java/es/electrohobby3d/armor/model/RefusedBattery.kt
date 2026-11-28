// ARMOR-ANDROID-CONTROL - a battery stack whose readings the server is refusing, from GET /api/v1/system/ingest (the same rule as Studio): the last thing it said of the topic is an
// error, newer than the last reading it took, and the message was a battery's. Without it the stack only looks old or absent.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONObject

data class RefusedBattery(val name: String, val reason: String) {
    companion object {
        private val TOPIC = Regex("^armor/solar/([^/]+)/([^/]+)/state$")
        private val BATTERY = Regex("\"kind\"\\s*:\\s*\"battery\"")

        fun fromIngest(body: JSONObject): List<RefusedBattery> {
            val topics = body.optJSONArray("topics") ?: return emptyList()
            return (0 until topics.length()).mapNotNull { index ->
                val item = topics.optJSONObject(index) ?: return@mapNotNull null
                val named = TOPIC.find(item.optString("topic")) ?: return@mapNotNull null
                val failedAt = item.text("last_error_at") ?: return@mapNotNull null
                val takenAt = item.text("last_ok_at")
                if (!BATTERY.containsMatchIn(item.text("last_payload").orEmpty()) || (takenAt != null && takenAt >= failedAt)) return@mapNotNull null
                RefusedBattery("${named.groupValues[1]} / ${named.groupValues[2]}", item.text("last_error").orEmpty().take(200))
            }
        }

        private fun JSONObject.text(name: String): String? = if (isNull(name)) null else optString(name).ifEmpty { null }
    }
}
