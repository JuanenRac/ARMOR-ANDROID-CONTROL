package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.electrohobby3d.armor.model.HistoryRequest
import es.electrohobby3d.armor.model.HistorySample
import es.electrohobby3d.armor.model.SolarHistory
import es.electrohobby3d.armor.model.SolarOverview
import es.electrohobby3d.armor.model.SolarParser
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.sin

/**
 * The charts of the Solar screen on a real layout, with made-up figures: folded until asked, one small chart per cell, and remembered. It also takes pictures of them in
 * /data/local/tmp/shots for the workflow `android-screenshots.yml`.
 */
@RunWith(AndroidJUnit4::class)
class SolarChartsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun preferences() = instrumentation.targetContext.getSharedPreferences("armor-solar-view", android.content.Context.MODE_PRIVATE)

    /** Nothing remembered, the list of cells folded so that the pictures show the charts, and the app in Spanish. */
    @Before fun startClean() {
        preferences().edit().clear().putBoolean("cells/demo/demo", false).commit()
        Tr.choose(instrumentation.targetContext, "es")
    }

    private fun overview(): SolarOverview {
        val cells = (1..15).joinToString(",") { if (it == 7) "3.720" else "3.3%02d".format(Locale.US, it) }
        val module = """{"n":1,"present":true,"soc_percent":76,"voltage_v":50.1,"current_a":4.2,"temperature_c":24,"cells_v":[$cells]}"""
        val battery = """{"node_id":"demo","device":"demo","kind":"battery","registered":{"model":"pylontech-us3000","name":"Demo"},"reading":{"modules":1,"soc_percent":76,"voltage_v":50.1,"current_a":4.2,"stack":[$module]}}"""
        val inverter = """{"node_id":"demo","device":"inv","kind":"inverter","registered":{"name":"Demo inverter"},"reading":{"mode":"battery","pv_w":900,"out_w":600,"battery_v":50.1,"grid_v":230}}"""
        return SolarParser.overview(JSONObject("""{"devices":[$battery,$inverter]}"""))
    }

    private fun history(request: HistoryRequest): SolarHistory {
        val now = System.currentTimeMillis()
        val samples = (0..200).map { i ->
            val wave = sin(i / 18.0)
            val values = linkedMapOf(
                "voltage_v" to 50.0 + wave, "current_a" to 8 * wave, "power_w" to 400 * wave, "soc_percent" to 60 + 25 * wave, "temperature_c" to 23 + 2 * wave, "temperature_max_c" to 24 + 2 * wave,
                "cell_min_v" to 3.29 + 0.02 * wave, "cell_max_v" to 3.34 + 0.03 * wave, "pv_w" to 700 + 500 * wave, "out_w" to 500 + 150 * sin(i / 7.0), "battery_v" to 50 + wave, "battery_a" to 6 * wave,
                "battery_percent" to 60 + 20 * wave, "grid_v" to 230 + 3 * sin(i / 5.0), "delta_mv" to 45 + 10 * wave,
            )
            for (cell in 1..15) values["cell$cell"] = if (cell == 7) 3.36 + 0.36 * i / 200 else 3.30 + 0.03 * wave + cell * 0.001
            HistorySample(now - (200 - i) * 430_000L, values)   // a day of made-up readings
        }
        return SolarHistory(request.module, 60, 30, samples)
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(1200)
        listOf("mkdir -p /data/local/tmp/shots", "screencap -p /data/local/tmp/shots/$name.png").forEach { command ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        }
    }

    private fun reveal(tag: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(tag)) }
    private fun tap(tag: String) { reveal(tag); compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick) }
    /** Brings a node to the top of the screen, for a picture of what follows it. */
    private fun top(tag: String) {
        reveal(tag)
        val y = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
        compose.onNode(hasScrollToNodeAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, y - 60f) }
    }
    private fun present(tag: String): Boolean = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test fun theChartsAreFoldedUntilAskedForThenDrawnAndRemembered() {
        compose.setContent {
            ArmorTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(12.dp)) { SolarScreen(overview(), history = { history(it) }) }
                }
            }
        }
        reveal("fold-bank")
        assertFalse(present("fold-chart-bank/voltage"))
        tap("fold-bank")
        compose.waitUntil(10_000) { present("fold-chart-bank/voltage") }
        top("fold-bank")
        shot("solar-charts-bank")
        tap("fold-bank")

        reveal("fold-charts-demo")
        tap("fold-charts-demo")
        compose.waitUntil(10_000) { present("fold-chart-demo/demo/stack/voltage") }
        top("fold-charts-demo")
        shot("solar-charts-pack")
        top("fold-chart-demo/demo/stack/soc")
        shot("solar-charts-pack-more")
        tap("scope-2")
        compose.waitUntil(10_000) { present("fold-chart-demo/demo/1/cells") }
        top("fold-chart-demo/demo/1/cells")
        shot("solar-charts-cells")
        tap("small-cells")
        compose.waitUntil(10_000) { present("small-chart-7") }
        top("small-cells")
        shot("solar-charts-cells-small")

        reveal("fold-charts-inv")
        tap("fold-charts-inv")
        compose.waitUntil(10_000) { present("fold-chart-demo/inv/power") }
        top("fold-charts-inv")
        shot("solar-charts-inverter")

        // what was chosen is kept on the phone
        assertTrue(preferences().getBoolean("small-cells", false))
        assertFalse(preferences().getBoolean("open/bank", true))
        assertTrue(preferences().getBoolean("open/battery/demo/demo", false))
    }
}
