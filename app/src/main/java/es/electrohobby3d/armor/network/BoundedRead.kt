// ARMOR-ANDROID-CONTROL - reading an answer up to a limit.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * What the app takes from a connection is read up to a limit: a server (or whatever stands in its place) that never stops sending cannot make the phone keep an answer without end in
 * memory. Past the limit the read fails with an [IOException], the same kind of failure as a lost connection, and the caller shows its usual "could not read" message.
 */
object BoundedRead {
    /** An answer of the API (JSON): the biggest the server makes is far under this. */
    const val JSON_LIMIT = 8 * 1024 * 1024
    /** A picture (the heat map as a PNG): the same limit the server puts on what it hands over. */
    const val PICTURE_LIMIT = 16_000_000
    /** The start of an error answer, enough to name the cause. */
    const val ERROR_LIMIT = 4 * 1024

    fun bytes(stream: InputStream, limit: Int): ByteArray {
        require(limit >= 0) { "limit must not be negative" }
        val out = ByteArrayOutputStream(minOf(limit, 16 * 1024).coerceAtLeast(16))
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw IOException("The answer is longer than $limit bytes")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    fun text(stream: InputStream, limit: Int = JSON_LIMIT): String = String(bytes(stream, limit), Charsets.UTF_8)

    /** The first bytes of a stream as text, the rest left unread (an error answer: its start says what happened). */
    fun head(stream: InputStream?, limit: Int = ERROR_LIMIT): String {
        if (stream == null) return ""
        val buffer = ByteArray(limit)
        var read = 0
        while (read < limit) {
            val count = stream.read(buffer, read, limit - read)
            if (count < 0) break
            read += count
        }
        return String(buffer, 0, read, Charsets.UTF_8)
    }
}
