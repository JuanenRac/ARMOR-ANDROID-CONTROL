package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Every word the person reads goes through tr(): a Text("...") with words of its own would stay in Spanish in the other seven languages. */
class LiteralsTest {
    private val text = Regex("""\bText\(\s*"((?:[^"\\]|\\.)*)"""")
    private val hole = Regex("""\$\{[^}]*(\}|$)|\$[A-Za-z_][A-Za-z0-9_]*""")
    /** Names and units that are the same in every language. */
    private val sameEverywhere = setOf("Electro", "Hobby", "Wi", "Fi", "ms", "Mbit", "kWh", "dBm", "MHz", "https", "casa", "duckdns", "org", "armor", "pin")

    @Test fun noTextOfTheScreensHasWordsOutsideTr() {
        val found = mutableListOf<String>()
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            text.findAll(file.readText(Charsets.UTF_8)).forEach { match ->
                val words = Regex("""[\p{L}]{3,}""").findAll(hole.replace(match.groupValues[1], " ")).map { it.value }.filter { it !in sameEverywhere }.toList()
                if (words.isNotEmpty()) found += "${file.name}: ${match.groupValues[1].take(60)}"
            }
        }
        assertEquals(emptyList<String>(), found)
    }
}
