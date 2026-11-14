// ARMOR-ANDROID-CONTROL - the tile of Quick Settings: it shows whether the system is armed, read from the own server with the saved session, and a touch opens the app. It never arms or disarms:
// a tile can be touched from the lock screen, so the order stays inside the app, behind the lock of the person. The person adds the tile to the panel on purpose; nothing is shown before.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** What the tile and the widget say about the system: only whether it is armed, from the word the server gives for the mode. */
enum class SystemFace(val phrase: String) {
    ARMED("Armada"), DISARMED("Desarmada"), UNKNOWN("Sin datos");

    companion object {
        /** The mode of the server ("armed", "disarmed") as a face; anything else, or nothing, is unknown (the tile never guesses). */
        fun of(mode: String?): SystemFace = when (mode) { "armed" -> ARMED; "disarmed" -> DISARMED; else -> UNKNOWN }
    }
}

class StatusTileService : TileService() {
    private val main = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        val app = applicationContext
        Tr.init(app)
        paint(SystemFace.UNKNOWN)
        Thread {
            val mode = try { SavedServerSession.of(app)?.let { it.second.status(it.first).mode } } catch (_: Exception) { null }
            main.post { paint(SystemFace.of(mode)) }
        }.start()
    }

    override fun onClick() {
        super.onClick()
        val open = Intent(this, ArmorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else @Suppress("DEPRECATION") startActivityAndCollapse(open)
    }

    private fun paint(face: SystemFace) {
        val tile = qsTile ?: return
        tile.label = "A.R.M.O.R."
        tile.state = if (face == SystemFace.ARMED) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = tr(face.phrase)
        tile.updateTile()
    }
}
