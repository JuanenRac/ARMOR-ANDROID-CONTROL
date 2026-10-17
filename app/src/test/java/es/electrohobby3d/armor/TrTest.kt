package es.electrohobby3d.armor

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The app is in eight languages: every phrase of the source has its text in the seven tables, with the same holes, and the choice of language falls back to English. */
class TrTest {
    private val codes = listOf("en", "de", "fr", "it", "ja", "zh", "pt")
    private fun tables(): Map<String, JSONObject> = codes.associateWith { JSONObject(File("src/main/assets/i18n/$it.json").readText(Charsets.UTF_8)) }
    private val call = Regex("""\btrf?\(\s*"((?:[^"\\]|\\.)*)"""")
    private val format = Regex("""%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]""")

    private fun decode(source: String): String = Regex("""\\(u[0-9a-fA-F]{4}|.)""").replace(source) { m ->
        val c = m.groupValues[1]
        when {
            c.startsWith("u") && c.length == 5 -> c.substring(1).toInt(16).toChar().toString()
            c == "n" -> "\n"; c == "t" -> "\t"
            else -> c
        }
    }

    private fun usedPhrases(): Set<String> {
        val out = linkedSetOf<String>()
        File("src/main/java").walkTopDown().filter { it.extension == "kt" && it.name != "Tr.kt" }.forEach { file ->
            call.findAll(file.readText(Charsets.UTF_8)).forEach { out += decode(it.groupValues[1]) }
        }
        return out
    }

    @Test fun everyPhraseOfTheSourceIsInEverySpanishTable() {
        val all = tables()
        val phrases = usedPhrases() + listOf("Estado", "Radar", "Alarmas", "Dispositivos", "Cámaras", "Más", "Todos", "Sensores", "Control", "Avisos", "Nodo radar", "Nodo solar", "Nodo eléctrico", "Nodo de alarma", "Panel táctil", "Nodo ARMOR")
        assertTrue("the source has phrases", phrases.size > 500)
        val missing = mutableListOf<String>()
        for (phrase in phrases) for ((code, table) in all) if (!table.has(phrase) || table.getString(phrase).isBlank()) missing += "$code: $phrase"
        assertEquals(emptyList<String>(), missing.take(20))
    }

    @Test fun aTranslationKeepsTheHolesOfTheSpanishPhrase() {
        val wrong = mutableListOf<String>()
        for ((code, table) in tables()) for (key in table.keys()) {
            val want = format.findAll(key.replace("%%", "")).map { it.value.replace(Regex("""\d+\$"""), "") }.sorted().toList()
            val got = format.findAll(table.getString(key).replace("%%", "")).map { it.value.replace(Regex("""\d+\$"""), "") }.sorted().toList()
            if (want != got) wrong += "$code: $key"
        }
        assertEquals(emptyList<String>(), wrong.take(20))
    }

    @Test fun theSeventablesHaveTheSamePhrases() {
        val all = tables()
        val first = all.getValue("en").keys().asSequence().toSet()
        for ((code, table) in all) assertEquals(code, first, table.keys().asSequence().toSet())
    }

    @Test fun theLanguageIsTheSavedOneThenThePhonesThenEnglish() {
        assertEquals("de", Tr.pick("de", "fr"))
        assertEquals("fr", Tr.pick(null, "fr"))
        assertEquals("pt", Tr.pick(null, "pt"))
        assertEquals("en", Tr.pick(null, "ru"))
        assertEquals("en", Tr.pick("xx", "ru"))
    }

    @Test fun aPhraseIsSpokenInTheLanguageOfTheAppAndFallsBackToSpanish() {
        Tr.useTable("en", mapOf("Armar" to "Arm", "%s cámaras" to "%s cameras"))
        assertEquals("Arm", tr("Armar"))
        assertEquals("3 cameras", trf("%s cámaras", 3))
        assertEquals("Sin traducción", tr("Sin traducción"))
        Tr.useTable("es", emptyMap())
        assertEquals("Armar", tr("Armar"))
        assertEquals("3 cámaras", trf("%s cámaras", 3))
    }
}
