// ARMOR-ANDROID-CONTROL. Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

/** Kept independent from Android networking so endpoint policy is testable. */
fun validEndpoint(endpoint: String): Boolean = ServerEndpoint.parse(endpoint) != null
