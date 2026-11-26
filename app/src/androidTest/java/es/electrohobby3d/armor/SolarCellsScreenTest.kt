package es.electrohobby3d.armor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.electrohobby3d.armor.model.SolarOverview
import es.electrohobby3d.armor.model.SolarParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** The cells of a battery on a real layout: on narrow phones and with large fonts, no voltage may be cut off, for 15 cells and for 16. */
@RunWith(AndroidJUnit4::class)
class SolarCellsScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun overview(count: Int): SolarOverview {
        val cells = (1..count).joinToString(",") { if (it == count) "2.600" else "3.3%02d".format(Locale.US, it) }
        val module = """{"n":1,"present":true,"soc_percent":80,"cells_v":[$cells]}"""
        return SolarParser.overview(JSONObject("""{"devices":[{"node_id":"n","device":"b","kind":"battery","registered":{"model":"pylontech-us3000"},"reading":{"modules":1,"stack":[$module]}}]}"""))
    }

    @Test fun everyVoltageOfFifteenAndSixteenCellsIsWholeOnNarrowPhonesAndLargeFonts() {
        var width by mutableStateOf(332)
        var scale by mutableStateOf(1.0f)
        var count by mutableStateOf(15)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                ArmorTheme { Box(Modifier.width(width.dp)) { SolarScreen(overview(count)) } }
            }
        }
        val cut = mutableListOf<String>()
        for (cellsOfModule in listOf(15, 16)) for (screen in listOf(292, 332, 383)) for (font in listOf(1.0f, 1.3f, 2.0f)) {
            compose.runOnIdle { count = cellsOfModule; width = screen; scale = font }
            compose.waitForIdle()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("solar-cell-1-1"))
            for (cell in 1..cellsOfModule) {
                val results = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("solar-cell-1-$cell", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                val layout = results.first()
                if (layout.hasVisualOverflow || layout.lineCount != 1) cut += "$cellsOfModule cells, ${screen}dp, font $font: cell $cell"
            }
        }
        assertEquals(emptyList<String>(), cut)
    }
}
