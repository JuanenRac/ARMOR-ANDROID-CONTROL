// ARMOR-ANDROID-CONTROL - the facts an alarm carries (which device, which port, which panel...) with the label each one has in the table every client shares (assets/alarm_facts.json is a copy of the one in ARMOR-COMMON, kept equal by a test).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import org.json.JSONObject

/** One fact of an alarm ready to be read: [label] is the words of the person's language, or the key as it came when the table does not know it ([known] is false then). */
data class AlarmFactRow(val key: String, val label: String, val value: String, val known: Boolean)

object AlarmFacts {
    private const val ASSET = "alarm_facts.json"

    @Volatile private var table: Map<String, Map<String, String>> = emptyMap()
    @Volatile private var help: Map<String, Map<String, String>> = emptyMap()

    /** The order the facts are told in; anything else the server sends follows, by name. */
    private val ORDER = listOf(
        "device", "ip", "mac", "vendor", "hostname", "kind", "os", "online", "open_ports", "port", "risky", "opened_at", "first_seen", "mac_now", "mac_before", "what_happened",
        "arming_mode", "panel_profile", "panels", "panel_answers", "by",
        "latency_ms", "loss_percent", "failing", "router_answers", "since", "router", "interface", "node",
    )
    private val DATES = setOf("since", "first_seen", "opened_at")

    /** Reads the labels from the assets. A missing or damaged file leaves the table empty, and every fact is then shown with its key. */
    fun init(context: Context) {
        if (table.isNotEmpty()) return
        val text = try { context.applicationContext.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() } } catch (_: Exception) { null }
        table = try { if (text == null) emptyMap() else parse(text) } catch (_: Exception) { emptyMap() }
        help = try { if (text == null) emptyMap() else parseHelp(text) } catch (_: Exception) { emptyMap() }
    }

    /** code -> language -> what the alarm means and what to check (the `help` of the shared table); empty when the file has none. */
    fun parseHelp(json: String): Map<String, Map<String, String>> {
        val root = JSONObject(json)
        val texts = root.optJSONObject("help") ?: return emptyMap()
        return texts.keys().asSequence().associateWith { code ->
            val words = texts.getJSONObject(code)
            words.keys().asSequence().associateWith { words.getString(it) }
        }
    }

    /** The help of an alarm code in [language]; English when that language lacks it; null when the code has no help text. */
    fun help(code: String, language: String = Tr.language): String? = help[code]?.let { it[language] ?: it["en"] }?.takeIf { it.isNotBlank() }

    /** fact -> language -> label. */
    fun parse(json: String): Map<String, Map<String, String>> {
        val facts = JSONObject(json).getJSONObject("facts")
        return facts.keys().asSequence().associateWith { key ->
            val labels = facts.getJSONObject(key)
            labels.keys().asSequence().associateWith { labels.getString(it) }
        }
    }

    /** For the tests: put a table in place without a context. */
    internal fun use(parsed: Map<String, Map<String, String>>, helpTable: Map<String, Map<String, String>> = emptyMap()) { table = parsed; help = helpTable }

    fun keys(): Set<String> = table.keys

    /** The label of [key] in [language]; English when that language lacks it; null when the table does not know the fact. */
    fun label(key: String, language: String): String? = table[key]?.let { it[language] ?: it["en"] }

    /** The value as a person reads it: yes/no in words, units for the two measures that have one, a date in the clock of the phone; anything unexpected as it came. */
    fun value(key: String, value: Any): String = when {
        value is Boolean -> if (value) tr("sí") else tr("no")
        key == "latency_ms" && value is Number -> "${plain(value)} ms"
        key == "loss_percent" && value is Number -> "${plain(value)} %"
        key in DATES && value is String -> if (runCatching { java.time.Instant.parse(value) }.isSuccess) Friendly.dayAndClock(value) else value
        value is Number -> plain(value)
        else -> value.toString()
    }

    private fun plain(number: Number): String = if (number is Double && number == Math.rint(number) && Math.abs(number) < 1e15) number.toLong().toString() else number.toString()

    /** Every fact of [detail], ordered, each with its label. Nothing is dropped: a fact the table does not know keeps its key as label. */
    fun rows(detail: Map<String, Any>, language: String = Tr.language): List<AlarmFactRow> =
        detail.entries
            .sortedWith(compareBy<Map.Entry<String, Any>>({ ORDER.indexOf(it.key).let { i -> if (i < 0) ORDER.size else i } }, { it.key }))
            .map { (key, raw) ->
                val label = label(key, language)
                AlarmFactRow(key, label ?: key, value(key, raw), label != null)
            }
}
