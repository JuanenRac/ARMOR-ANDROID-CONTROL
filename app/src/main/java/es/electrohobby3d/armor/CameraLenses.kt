// ARMOR-ANDROID-CONTROL - the windows of a camera that has two lenses in one stream.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.CameraView

/**
 * A camera whose stream carries two lenses, one above the other, appears as two windows of the list, one after the other: each shows its half of the picture and the name says which lens it
 * is, while what the buttons do (snapshot, record, move) is done to the real camera behind them ([CameraView.sourceId]).
 */
fun withLensWindows(cameras: List<CameraView>): List<CameraView> = cameras.flatMap { camera ->
    if (camera.layout != "split") listOf(camera)
    else listOf(LensHalf.UPPER to tr("Objetivo superior"), LensHalf.LOWER to tr("Objetivo inferior")).map { (half, label) ->
        camera.copy(id = "${camera.id}.${half.name.lowercase()}", name = "${camera.name} · $label", layout = null, lens = half, sourceId = camera.id)
    }
}

/** The identifier that the server knows for a camera of the list. */
val CameraView.serverId: String get() = sourceId ?: id
