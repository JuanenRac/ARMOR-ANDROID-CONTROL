// ARMOR-ANDROID-CONTROL - the picture in the notice of a camera: when it is wanted, what is taken for a picture, how it is shrunk and how the phone asks its own server for it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class NoticePhotoTest {
    private lateinit var server: FakeServer
    private lateinit var client: ArmorApiClient

    @Before fun start() { server = FakeServer(); client = ArmorApiClient() }
    @After fun stop() { client.forgetSession(server.origin); server.stop() }

    private fun notice(camera: String?) = AlarmNotice(77L, "t", "x", "high", NoticeKinds.SYSTEM, cameraId = camera)

    @Test fun `a notice gets a picture only when the person turned it on and the notice names a camera`() {
        assertTrue(NoticePhoto.wanted(true, notice("cam-1")))
        assertFalse(NoticePhoto.wanted(false, notice("cam-1")))
        assertFalse(NoticePhoto.wanted(true, notice(null)))
    }

    @Test fun `an id that is not the id of a camera of the server asks for no picture`() {
        assertFalse(NoticePhoto.wanted(true, notice("../admin")))
        assertFalse(NoticePhoto.wanted(true, notice("a b")))
        assertFalse(NoticePhoto.wanted(true, notice("")))
    }

    @Test fun `only the signature of a JPEG or a PNG is taken for a picture`() {
        assertTrue(NoticePhoto.looksLikePicture(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1)))
        assertTrue(NoticePhoto.looksLikePicture(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)))
        assertFalse(NoticePhoto.looksLikePicture("""{"error":"no"}""".toByteArray()))
        assertFalse(NoticePhoto.looksLikePicture(ByteArray(0)))
        assertFalse(NoticePhoto.looksLikePicture(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
    }

    @Test fun `a picture is shrunk by a power of two until its longest side is near the target and no smaller`() {
        assertEquals(1, NoticePhoto.sampleSize(640, 480))
        assertEquals(1, NoticePhoto.sampleSize(1024, 768))
        assertEquals(1, NoticePhoto.sampleSize(1920, 1080))
        assertEquals(2, NoticePhoto.sampleSize(2048, 1536))
        assertEquals(4, NoticePhoto.sampleSize(4096, 3072))
        assertEquals(1, NoticePhoto.sampleSize(0, 0))
    }

    @Test fun `the phone asks its own server for the picture with a GET and nothing in the body`() {
        server.reply(200, "JPEGBYTES")
        val bytes = client.cameraStill(server.origin, "cam-1")
        assertEquals("JPEGBYTES", String(bytes, Charsets.UTF_8))
        val sent = server.seen.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/v1/cameras/cam-1/snapshot", sent.path)
        assertEquals("", sent.body)
    }

    @Test fun `a picture longer than the limit is refused like a lost connection`() {
        server.reply(200, "x".repeat(200))
        try { client.cameraStill(server.origin, "cam-1", limit = 100); fail("expected the read to fail") } catch (_: IOException) { }
    }

    @Test fun `a camera without a snapshot address or asked too soon gives the error of the server and no picture`() {
        server.reply(422, """{"error":"this camera has no snapshot address"}""")
        val missing = try { client.cameraStill(server.origin, "cam-1"); null } catch (error: ArmorApiException) { error }
        assertEquals(422, missing?.code)
        server.reply(429, """{"error":"too soon"}""")
        val soon = try { client.cameraStill(server.origin, "cam-1"); null } catch (error: ArmorApiException) { error }
        assertEquals(429, soon?.code)
    }

    @Test fun `the picture is never asked for with the camera id of another path`() {
        server.reply(200, "ok")
        client.cameraStill(server.origin, "cam 1/../x")
        assertEquals("/api/v1/cameras/cam%201%2F..%2Fx/snapshot", server.seen.single().rawPath)
    }
}
