// ARMOR-ANDROID-CONTROL - the Hydra look: near-black surfaces, a cyan accent and amber for attention.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ArmorColors {
    val Background = Color(0xFF07090C)
    val Surface = Color(0xFF0D1218)
    val SurfaceRaised = Color(0xFF141C25)
    val Outline = Color(0xFF12303A)
    val Cyan = Color(0xFF00E5FF)
    val Amber = Color(0xFFFFB020)
    val Alert = Color(0xFFFF4D5E)
    val Text = Color(0xFFEDF7FF)
    val Muted = Color(0xFF91A8BD)
}

private val Scheme = darkColorScheme(
    primary = ArmorColors.Cyan,
    onPrimary = Color(0xFF031217),
    secondary = Color(0xFF8BD6FF),
    tertiary = ArmorColors.Amber,
    background = ArmorColors.Background,
    onBackground = ArmorColors.Text,
    surface = ArmorColors.Surface,
    onSurface = ArmorColors.Text,
    surfaceVariant = ArmorColors.SurfaceRaised,
    onSurfaceVariant = ArmorColors.Muted,
    outline = ArmorColors.Outline,
    error = ArmorColors.Alert,
)

@Composable
fun ArmorTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
