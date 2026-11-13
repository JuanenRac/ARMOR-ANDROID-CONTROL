package es.electrohobby3d.armor

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Takes the pictures of the app for the README in the emulator of the workflow `android-screenshots.yml`, against the REAL ARMOR-SERVER that the workflow starts (the emulator reaches it on 10.0.2.2).
 * The pictures are made with `screencap` in /data/local/tmp/shots, where the workflow pulls them with adb. Without the instrumentation argument `armorServer` every test here is skipped.
 * Nothing here touches a real installation or a phone.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {
    @get:Rule val rule = createAndroidComposeRule<ArmorActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val server: String? = InstrumentationRegistry.getArguments().getString("armorServer")

    private fun shell(command: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }   // read to the end: the command has finished then
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(1500)   // the animations of the screen end
        shell("mkdir -p /data/local/tmp/shots")
        shell("screencap -p /data/local/tmp/shots/$name.png")   // the shell of the device keeps it where adb can pull it
    }

    private fun present(tag: String): Boolean = try { rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() } catch (error: IllegalStateException) { false }   // no screen yet

    @Test
    fun theLoginAndTheMainPanel() {
        assumeTrue("no server was given", server != null)
        shell("pm grant es.electrohobby3d.armor android.permission.POST_NOTIFICATIONS")   // no question about notifications in the pictures
        val uri = java.net.URI(server!!)
        Thread.sleep(8_000)   // the app starts
        shot("start")
        rule.waitUntil(timeoutMillis = 60_000) { present("login-server") }
        shot("login")
        rule.onNodeWithTag("login-server").performTextInput(uri.host)
        rule.onNodeWithTag("login-port").performTextInput(uri.port.toString())
        rule.onNodeWithTag("login-user").performTextInput("admin")
        rule.onNodeWithTag("login-password").performTextInput("correct-horse-battery")
        shot("login-filled")
        rule.onNodeWithTag("login-submit").performClick()
        rule.waitUntil(timeoutMillis = 60_000) { !present("login-server") }
        Thread.sleep(6_000)   // the panel reads the state of the system
        shot("home")
        // the sections of the bottom bar, one after the other
        val tabs = rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        val names = listOf("status", "radar", "alarms", "devices", "cameras", "more")
        val count = minOf(tabs.fetchSemanticsNodes().size, names.size)
        for (index in 1 until count) {
            try {
                tabs[index].performClick()
                Thread.sleep(5_000)
                shot("section-" + names[index])
            } catch (error: Throwable) { android.util.Log.w("Screenshots", "section " + names[index] + ": " + error) }
        }
    }
}
