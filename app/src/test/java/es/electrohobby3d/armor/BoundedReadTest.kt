package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.BoundedRead
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BoundedReadTest {
    /** A stream that never ends: it counts how much was taken from it. */
    private class Endless : InputStream() {
        var taken = 0L
        override fun read(): Int { taken += 1; return 'a'.code }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int { taken += length; buffer.fill('a'.code.toByte(), offset, offset + length); return length }
    }

    @Test fun anAnswerWithinTheLimitIsReadWhole() {
        assertEquals("hello", BoundedRead.text(ByteArrayInputStream("hello".toByteArray()), 5))
        assertEquals("", BoundedRead.text(ByteArrayInputStream(ByteArray(0)), 5))
        assertEquals(3, BoundedRead.bytes(ByteArrayInputStream(ByteArray(3)), 3).size)
    }

    @Test fun anAnswerOverTheLimitFailsAsAnIoErrorAndIsNotReadToItsEnd() {
        val stream = Endless()
        try { BoundedRead.bytes(stream, 100_000); fail("an endless answer was accepted") } catch (_: IOException) { }
        assertTrue("it kept reading after the limit: ${stream.taken}", stream.taken < 100_000 + 16 * 1024)
        try { BoundedRead.text(ByteArrayInputStream(ByteArray(11)), 10); fail("one byte over was accepted") } catch (_: IOException) { }
    }

    @Test fun theStartOfAnErrorAnswerIsTakenAndTheRestLeft() {
        val stream = Endless()
        val head = BoundedRead.head(stream, 64)
        assertEquals(64, head.length)
        assertTrue(stream.taken <= 64)
        assertEquals("", BoundedRead.head(null))
        assertEquals("short", BoundedRead.head(ByteArrayInputStream("short".toByteArray())))
    }
}
