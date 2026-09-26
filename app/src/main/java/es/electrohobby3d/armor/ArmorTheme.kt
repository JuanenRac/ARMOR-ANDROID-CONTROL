// ARMOR-ANDROID-CONTROL - the A.R.M.O.R. look: near-black surfaces, a cyan accent, amber for attention, and a few pieces every screen shares.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon

object ArmorColors {
    val Background = Color(0xFF07090C)
    val Surface = Color(0xFF0D1218)
    val SurfaceRaised = Color(0xFF141C25)
    val Outline = Color(0xFF1B3A46)
    val Cyan = Color(0xFF00E5FF)
    val CyanDeep = Color(0xFF1B9BD1)
    val Amber = Color(0xFFFFB020)
    val Alert = Color(0xFFFF4D5E)
    val Ok = Color(0xFF3DDC97)
    val Text = Color(0xFFEDF7FF)
    val Muted = Color(0xFF91A8BD)
}

private val Scheme = darkColorScheme(
    primary = ArmorColors.Cyan,
    onPrimary = Color(0xFF031217),
    primaryContainer = Color(0xFF0B3440),
    onPrimaryContainer = ArmorColors.Text,
    secondary = Color(0xFF8BD6FF),
    secondaryContainer = Color(0xFF12303A),
    onSecondaryContainer = ArmorColors.Text,
    tertiary = ArmorColors.Amber,
    background = ArmorColors.Background,
    onBackground = ArmorColors.Text,
    surface = ArmorColors.Surface,
    onSurface = ArmorColors.Text,
    surfaceVariant = ArmorColors.SurfaceRaised,
    onSurfaceVariant = ArmorColors.Muted,
    outline = ArmorColors.Outline,
    outlineVariant = Color(0xFF12222B),
    error = ArmorColors.Alert,
    errorContainer = Color(0xFF3A1319),
    onErrorContainer = Color(0xFFFFD9DD),
)

private val ArmorTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun ArmorTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, typography = ArmorTypography, content = content)

/** The mark of the project: a rounded square with the letter A, the same as Studio's. */
@Composable
fun ArmorLogo(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(Color(0xFF7DF3E4), ArmorColors.CyanDeep)))
            .border(BorderStroke(size * 0.03f, Color(0x6600E5FF)), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) { Text("A", color = Color(0xFF031217), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = (size.value * 0.55f).sp) }
}

/** A raised card with a thin border: the container of nearly everything. `tint` colours the border and a faint wash (alerts, armed). */
@Composable
fun Panel(modifier: Modifier = Modifier, tint: Color? = null, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val base = modifier.clip(shape)
    Surface(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        shape = shape,
        color = ArmorColors.SurfaceRaised,
        border = BorderStroke(1.dp, (tint ?: ArmorColors.Outline).copy(alpha = if (tint != null) 0.7f else 1f)),
    ) {
        Column(Modifier.background(Brush.verticalGradient(listOf((tint ?: ArmorColors.Cyan).copy(alpha = if (tint != null) 0.10f else 0.03f), Color.Transparent))), content = content)
    }
}

/** An icon in a soft coloured disc: what the screens use in place of words. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = ArmorColors.Cyan, size: Dp = 44.dp, description: String? = null) {
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** A small round light: green when all is well, red when not, grey when unknown. */
@Composable
fun StatusDot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape))
}
