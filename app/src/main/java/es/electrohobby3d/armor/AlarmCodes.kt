// ARMOR-ANDROID-CONTROL - what a person reads for each alarm code, from the table every client shares (assets/alarm_codes.json is a copy of the one in ARMOR-COMMON, kept equal by a test).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import org.json.JSONObject

object AlarmCodes {
    private const val ASSET = "alarm_codes.json"

    @Volatile private var table: Map<String, Map<String, String>> = emptyMap()

    /** Reads the table from the assets. A missing or damaged file leaves the table empty, and the phrases of the app (DeviceText) are used instead. */
    fun init(context: Context) {
        if (table.isNotEmpty()) return
        table = try { parse(context.applicationContext.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }) } catch (_: Exception) { emptyMap() }
    }

    /** code -> language -> text. */
    fun parse(json: String): Map<String, Map<String, String>> {
        val codes = JSONObject(json).getJSONObject("codes")
        return codes.keys().asSequence().associateWith { code ->
            val texts = codes.getJSONObject(code)
            texts.keys().asSequence().associateWith { texts.getString(it) }
        }
    }

    /** For the tests: put a table in place without a context. */
    internal fun use(parsed: Map<String, Map<String, String>>) { table = parsed }

    fun codes(): Set<String> = table.keys

    /** The sentence for [code] in [language]; English when that language lacks it; null when the code is not in the table. */
    fun text(code: String, language: String): String? = table[code]?.let { it[language] ?: it["en"] }
}
