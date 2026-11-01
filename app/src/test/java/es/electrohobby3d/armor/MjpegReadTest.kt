package es.electrohobby3d.armor

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MjpegReadTest {
    private fun jpeg(payload: Int = 10): ByteArray {
        val scan = ByteArray(payload) { 0x11 }
        return byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xe0.toByte(), 0x00, 0x04, 0x01, 0x02, 0xff.toByte(), 0xda.toByte(), 0x00, 0x02) + scan + byteArrayOf(0xff.toByte(), 0xd9.toByte())
    }

    private class Endless(private val fill: Int) : InputStream() {
        var taken = 0L
        override fun read(): Int { taken += 1; return fill }
    }

    @Test fun aWholePictureIsCutAtItsEndAndNoFurther() {
        val picture = jpeg()
        val stream = ByteArrayInputStream("--armorframe\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray() + picture + "\r\n--armorframe".toByteArray())
        assertArrayEquals(picture, readJpeg(stream))
    }

    @Test fun aStreamThatNeverShowsAPictureIsNotReadToItsEnd() {
        for (fill in intArrayOf(0x41, 0xff)) {
            val stream = Endless(fill)
            assertNull(readJpeg(stream))
            assertTrue("read ${stream.taken} bytes of nothing", stream.taken < 9 * 1024 * 1024 + 16)
        }
    }

    @Test fun garbageNeverCrashesAndNeverReturnsAnEmptyPicture() {
        val random = Random(42)
        repeat(300) {
            val bytes = ByteArray(random.nextInt(400)) { (if (random.nextInt(4) == 0) 0xff else random.nextInt(256)).toByte() }
            val picture = readJpeg(ByteArrayInputStream(bytes))
            if (picture != null) { assertNotNull(picture); assertTrue(picture.size >= 4) }
        }
    }

    @Test fun twoPicturesInARowComeOutOneByOne() {
        val first = jpeg(3); val second = jpeg(7)
        val stream = ByteArrayInputStream(first + "\r\n--armorframe\r\n\r\n".toByteArray() + second)
        assertArrayEquals(first, readJpeg(stream)); assertArrayEquals(second, readJpeg(stream))
    }
}
