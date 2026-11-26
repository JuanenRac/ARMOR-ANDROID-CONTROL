package es.electrohobby3d.armor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The emulator tests must use the Compose rules of `ui.test.junit4.v2`. The older ones resume a collector on the thread that publishes, so a state published by a worker thread is
 * applied to the views from that thread; the app then fails while it closes, with an error that hides the first one. A phone does not do that.
 */
class ComposeTestRuleTest {
    private val older = Regex("""androidx\.compose\.ui\.test(\.junit4)?\.(create\w*ComposeRule|runComposeUiTest|runAndroidComposeUiTest|\*)""")

    @Test fun noEmulatorTestUsesTheOlderComposeRules() {
        val sources = File("src/androidTest/java").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("the emulator tests were not found", sources.size >= 4)
        val found = sources.flatMap { file -> older.findAll(file.readText(Charsets.UTF_8)).map { "${file.name}: ${it.value}" }.toList() }
        assertEquals(emptyList<String>(), found)
        assertTrue(older.containsMatchIn("import androidx.compose.ui.test.junit4.createComposeRule"))
        assertTrue(!older.containsMatchIn("import androidx.compose.ui.test.junit4.v2.createComposeRule"))
    }
}
