package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.CameraView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraTraitsTest {
    private fun camera(ptz: Boolean? = null, zoom: Boolean? = null) = CameraView("c1", "Gate", "h", "", "", 80, 554, false, false, ptz, zoom)

    @Test fun `a camera that never said what it can do keeps every button`() {
        val unknown = camera()
        assertTrue(unknown.canPanTilt)
        assertTrue(unknown.canZoom)
        assertTrue(unknown.hasMovement)
    }

    @Test fun `a fixed camera with no zoom shows no movement at all`() {
        val fixed = camera(ptz = false, zoom = false)
        assertFalse(fixed.canPanTilt)
        assertFalse(fixed.canZoom)
        assertFalse(fixed.hasMovement)
    }

    @Test fun `a fixed camera that zooms shows only the zoom`() {
        val zooming = camera(ptz = false, zoom = true)
        assertFalse(zooming.canPanTilt)
        assertEquals(true, zooming.canZoom)
        assertTrue(zooming.hasMovement)
    }
}
