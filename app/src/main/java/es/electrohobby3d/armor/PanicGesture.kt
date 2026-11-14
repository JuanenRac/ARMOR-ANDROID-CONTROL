// ARMOR-ANDROID-CONTROL - the hidden panic gesture: a long press on the logo, or five quick touches on it. It asks the server for the silent duress alarm; nothing shows on the phone.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** The numbers of the gesture, in one place. */
object PanicGesture {
    /** Quick touches that make the gesture. */
    const val TAPS = 5
    /** All of them must fall inside this time. */
    const val WINDOW_MS = 3_000L
    /** Tries to reach the server when the connection fails (the first one included), and the wait between them. */
    const val TRIES = 2
    const val RETRY_MS = 2_000L
}

/** Counts quick touches: [touch] says true on the [taps]-th one inside [windowMs], and starts counting again after it. */
class TapCounter(private val taps: Int = PanicGesture.TAPS, private val windowMs: Long = PanicGesture.WINDOW_MS) {
    private val times = ArrayDeque<Long>()

    fun touch(atMs: Long): Boolean {
        times.addLast(atMs)
        while (times.isNotEmpty() && atMs - times.first() > windowMs) times.removeFirst()
        if (times.size < taps) return false
        times.clear()
        return true
    }
}

/** Listens for the gesture on this element; with [onPanic] null (the server does not offer it) it listens to nothing and touches go on as before. */
@Composable
fun Modifier.panicGesture(onPanic: (() -> Unit)?): Modifier {
    if (onPanic == null) return this
    val current by rememberUpdatedState(onPanic)
    val counter = remember { TapCounter() }
    return pointerInput(Unit) {
        detectTapGestures(onLongPress = { current() }, onTap = { if (counter.touch(System.currentTimeMillis())) current() })
    }
}
