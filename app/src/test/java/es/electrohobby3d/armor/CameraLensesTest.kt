package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.CameraView
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraLensesTest {
    private fun camera(id: String, layout: String? = null) = CameraView(id, id, "h", "", "", 80, 554, true, true, layout = layout)

    @Test fun `a camera with two lenses in one stream is two windows, one after the other`() {
        val windows = withLensWindows(listOf(camera("a"), camera("yard", "split"), camera("b")))
        assertEquals(listOf("a", "yard.upper", "yard.lower", "b"), windows.map { it.id })
        assertEquals(listOf(LensHalf.WHOLE, LensHalf.UPPER, LensHalf.LOWER, LensHalf.WHOLE), windows.map { it.lens })
    }

    @Test fun `what the buttons ask of the server is about the real camera`() {
        val window = withLensWindows(listOf(camera("yard", "split")))[1]
        assertEquals("yard", window.serverId)
        assertEquals("yard", camera("yard").serverId)
    }
}
