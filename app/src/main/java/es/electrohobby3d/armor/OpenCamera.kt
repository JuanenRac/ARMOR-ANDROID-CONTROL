// ARMOR-ANDROID-CONTROL - the action "Open camera" of a notice about a camera: the app opens on the Cameras place with that camera in full screen, once the person is recognised and the camera is known.
// The request travels as an extra of the intent and is held here until the screen takes it; an id that is not a camera id of the server is never held.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object OpenCamera {
    const val EXTRA = "open_camera"
    private val asked = MutableStateFlow<String?>(null)

    /** The camera the person asked to open and the screen has not taken yet. */
    val requested: StateFlow<String?> get() = asked

    /** An id the server gave: letters, digits, dots, dashes and underscores, short. Anything else is not acted on. */
    fun valid(id: String): Boolean = id.isNotEmpty() && id.length <= 80 && id.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }

    /** Takes the request from the intent that opened (or came back to) the app. */
    fun fromIntent(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA)
        if (id != null && valid(id)) asked.value = id
    }

    /** The screen took it (or could not use it): it is asked for no more. */
    fun taken() { asked.value = null }

    fun intent(context: Context, camera: String, noticeId: Int): PendingIntent {
        val open = Intent(context, ArmorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA, camera)
        return PendingIntent.getActivity(context, noticeId + 1, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
