// ARMOR-ANDROID-CONTROL - the widget of the home screen: whether the system is armed, read from the own server with the saved session every half hour, and a touch opens the app. Like the tile of Quick
// Settings it only shows: it never arms or disarms, and the person adds it to the home screen on purpose; nothing is shown before.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The colours of a face, apart from the drawing so a test can read them: armed in the colour of the app, disarmed and unknown quieter. */
object WidgetLook {
    fun background(face: SystemFace): Long = when (face) { SystemFace.ARMED -> 0xFF0B3A44; SystemFace.DISARMED -> 0xFF1A1F26; SystemFace.UNKNOWN -> 0xFF14181D }
    fun text(face: SystemFace): Long = if (face == SystemFace.ARMED) 0xFF00E5FF else 0xFFE6EAEE
}

class StatusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext
        Tr.init(app)
        val face = withContext(Dispatchers.IO) {
            try { SavedServerSession.of(app)?.let { SystemFace.of(it.second.status(it.first).mode) } } catch (_: Exception) { null }
        } ?: SystemFace.UNKNOWN
        val open = Intent(app, ArmorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = Intent(app, ArmorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val title = "A.R.M.O.R."
        val state = tr(face.phrase)
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(WidgetLook.background(face)))).padding(12.dp).clickable(actionStartActivity(open)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = TextStyle(color = ColorProvider(Color(0xFF9AA4AF)), fontSize = 12.sp))
                Text(state, style = TextStyle(color = ColorProvider(Color(WidgetLook.text(face))), fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatusWidget()
}
