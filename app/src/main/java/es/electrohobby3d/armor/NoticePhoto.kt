// ARMOR-ANDROID-CONTROL - the picture in the notice of a camera: when the person turned it on, the phone asks its own server for the still picture of that camera (GET /api/v1/cameras/{id}/snapshot,
// with the saved session) and puts it in the notice as a big picture. It never goes through the push service of Google; off by default, and without a session or an answer the notice stays as it was.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** The server in use and a client with the saved session of the person (null when the address or the session does not allow it): the same way the reminder reads the server with the app closed. */
object SavedServerSession {
    fun of(context: Context): Pair<String, es.electrohobby3d.armor.network.ArmorApi>? {
        val preferences = context.getSharedPreferences("armor-control", Context.MODE_PRIVATE)
        val origin = preferences.getString("origin", "").orEmpty()
        if (ServerEndpoint.parse(origin) == null) return null
        TrustPins.install(PreferencesStore(preferences))
        val vault = KeyBoxes.sessionVault(context)
        if (!vault.has() || !KeyBoxes.sessionBox(context).usable()) return null
        val client = SharedFeed.api
        vault.load(origin)?.let { client.restoreSessionCookie(origin, it.name, it.value) }
        return origin to client
    }
}

object NoticePhoto {
    /** The biggest picture taken in: the server stops a bigger one, and the phone does not keep more than this in memory for a notice. */
    const val MAX_BYTES = 1_500_000
    /** The longest side the picture is shrunk to for the notice. */
    const val TARGET_SIDE = 1024
    private const val KEY = "notice_photo"

    fun enabled(context: Context): Boolean = context.applicationContext.getSharedPreferences("armor-control", Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setEnabled(context: Context, on: Boolean) { context.applicationContext.getSharedPreferences("armor-control", Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply() }

    /** Whether a notice gets a picture: the person turned it on and the notice names a camera (an id the server could have given). */
    fun wanted(enabled: Boolean, notice: AlarmNotice): Boolean = enabled && notice.cameraId?.let { OpenCamera.valid(it) } == true

    /** A JPEG (or a PNG) starts with its signature: anything else is not shown. */
    fun looksLikePicture(bytes: ByteArray): Boolean =
        (bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) ||
            (bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte())

    /** The power of two to shrink a picture of this size by so that its longest side is near [TARGET_SIDE] and no smaller. */
    fun sampleSize(width: Int, height: Int): Int {
        var longest = maxOf(width, height)
        var sample = 1
        while (longest / 2 >= TARGET_SIDE) { longest /= 2; sample *= 2 }
        return sample
    }

    /** The still picture of a camera as a bitmap, or null when there is no session, no answer, no picture or the bytes are not a picture. Blocking: call it off the main thread. */
    fun fetch(context: Context, camera: String): Bitmap? {
        if (!OpenCamera.valid(camera)) return null
        return try {
            val session = SavedServerSession.of(context) ?: return null
            val bytes = session.second.cameraStill(session.first, camera)
            if (!looksLikePicture(bytes)) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) })
        } catch (_: Exception) { null }
    }

    /** Puts the picture in the builder as a big picture, quietly: the second showing of a notice does not sound or vibrate again. */
    fun attach(builder: Notification.Builder, picture: Bitmap): Notification.Builder =
        builder.setStyle(Notification.BigPictureStyle().bigPicture(picture)).setOnlyAlertOnce(true)
}
