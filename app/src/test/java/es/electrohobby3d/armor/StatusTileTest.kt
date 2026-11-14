// ARMOR-ANDROID-CONTROL - the tile of Quick Settings: what it says, that it is declared the way Android wants, and that it never carries an order.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StatusTileTest {
    @Test fun `the face is armed or disarmed only for the two words of the server and unknown for anything else`() {
        assertEquals(SystemFace.ARMED, SystemFace.of("armed"))
        assertEquals(SystemFace.DISARMED, SystemFace.of("disarmed"))
        assertEquals(SystemFace.UNKNOWN, SystemFace.of(null))
        assertEquals(SystemFace.UNKNOWN, SystemFace.of(""))
        assertEquals(SystemFace.UNKNOWN, SystemFace.of("ARMED"))
        assertEquals(SystemFace.UNKNOWN, SystemFace.of("night"))
    }

    @Test fun `the three faces have their words in the seven tables`() {
        for (code in listOf("en", "de", "fr", "it", "ja", "zh", "pt")) {
            val table = JSONObject(File("src/main/assets/i18n/$code.json").readText(Charsets.UTF_8))
            for (face in SystemFace.values()) assertTrue("$code ${face.phrase}", table.has(face.phrase) && table.getString(face.phrase).isNotBlank())
        }
    }

    @Test fun `the tile is declared as a service that only the system can bind, with the action of tiles`() {
        val manifest = File("src/main/AndroidManifest.xml").readText(Charsets.UTF_8)
        val service = Regex("""<service[^>]*\.StatusTileService[^>]*>[\s\S]*?</service>""").find(manifest)?.value
        assertTrue("the tile service is declared", service != null)
        assertTrue(service!!.contains("""android:permission="android.permission.BIND_QUICK_SETTINGS_TILE""""))
        assertTrue(service.contains("android.service.quicksettings.action.QS_TILE"))
        assertTrue(File("src/main/res/drawable/ic_tile_shield.xml").isFile)
    }

    @Test fun `the tile never sends an order to the server`() {
        val source = File("src/main/java/es/electrohobby3d/armor/StatusTile.kt").readText(Charsets.UTF_8)
        for (word in listOf("setMode", "sendAlarmCommand", "acknowledge", "setArmingMode", "panic")) assertFalse("the tile must not call $word", source.contains(word))
    }

    @Test fun `the widget is declared as a receiver of the update of widgets with its provider and a description in the eight languages`() {
        val manifest = File("src/main/AndroidManifest.xml").readText(Charsets.UTF_8)
        val receiver = Regex("""<receiver[^>]*\.StatusWidgetReceiver[^>]*>[\s\S]*?</receiver>""").find(manifest)?.value
        assertTrue("the widget receiver is declared", receiver != null)
        assertTrue(receiver!!.contains("android.appwidget.action.APPWIDGET_UPDATE"))
        assertTrue(receiver.contains("@xml/status_widget_info"))
        val info = File("src/main/res/xml/status_widget_info.xml").readText(Charsets.UTF_8)
        assertTrue(info.contains("@string/status_widget_description"))
        for (folder in listOf("values", "values-es", "values-de", "values-fr", "values-it", "values-ja", "values-zh", "values-pt-rPT")) {
            val strings = File("src/main/res/$folder/strings.xml").readText(Charsets.UTF_8)
            assertTrue("$folder has the description", Regex("""<string name="status_widget_description">[^<]{10,}</string>""").containsMatchIn(strings))
        }
    }

    @Test fun `the widget only shows and opens the app, and the three faces do not look alike`() {
        val source = File("src/main/java/es/electrohobby3d/armor/StatusWidget.kt").readText(Charsets.UTF_8)
        for (word in listOf("setMode", "sendAlarmCommand", "acknowledge", "setArmingMode", "panic")) assertFalse("the widget must not call $word", source.contains(word))
        assertEquals(3, SystemFace.values().map { WidgetLook.background(it) }.toSet().size)
        assertTrue(WidgetLook.text(SystemFace.ARMED) != WidgetLook.text(SystemFace.DISARMED))
    }
}
