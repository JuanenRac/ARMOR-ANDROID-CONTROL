// ARMOR-ANDROID-CONTROL - the table of alarm codes that every client shares.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.DeviceText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The sentence for each alarm code comes from one table in eight languages, the same file the server and Studio read, so the three say the same thing. */
class AlarmCodesTest {
    private val languages = listOf("en", "es", "de", "fr", "it", "ja", "zh", "pt")
    private val asset = File("src/main/assets/alarm_codes.json")
    private val common = File("../../ARMOR-COMMON/src/armor_common/alarm_codes.json")

    @After fun reset() { AlarmCodes.use(emptyMap()); Tr.useTable("es", emptyMap()) }

    @Test fun `the table has every code in the eight languages and the one about a stale dns name`() {
        val table = AlarmCodes.parse(asset.readText(Charsets.UTF_8))
        assertTrue("the table has 58 codes or more, it has ${table.size}", table.size >= 58)
        assertTrue(table.containsKey("network_dns_stale"))
        for ((code, texts) in table) for (language in languages) assertTrue("$code in $language", !texts[language].isNullOrBlank())
    }

    @Test fun `the copy in the assets is the table of ARMOR-COMMON`() {
        if (!common.isFile) {
            assertTrue("ARMOR-COMMON must be next to this repository when ARMOR_REQUIRE_SIBLINGS is set", System.getenv("ARMOR_REQUIRE_SIBLINGS").isNullOrEmpty())
            return
        }
        assertEquals(common.readText(Charsets.UTF_8).replace("\r\n", "\n"), asset.readText(Charsets.UTF_8).replace("\r\n", "\n"))
    }

    @Test fun `a code is said in the language of the app and an unknown code keeps the phrases of the app`() {
        AlarmCodes.use(AlarmCodes.parse(asset.readText(Charsets.UTF_8)))
        for (language in languages) {
            Tr.useTable(language, emptyMap())
            assertEquals(language, AlarmCodes.text("network_dns_stale", language), DeviceText.alarmText("network_dns_stale"))
        }
        Tr.useTable("de", emptyMap())
        assertTrue(DeviceText.alarmText("network_dns_stale").isNotBlank() && DeviceText.alarmText("network_dns_stale") != "network_dns_stale")
        assertEquals("not_a_code", DeviceText.alarmText("not_a_code"))
    }

    @Test fun `every code the app used to know is in the table`() {
        val table = AlarmCodes.parse(asset.readText(Charsets.UTF_8))
        val source = File("src/main/java/es/electrohobby3d/armor/model/Devices.kt").readText(Charsets.UTF_8)
        val legacy = source.substringAfter("private fun legacyAlarmText").substringBefore("else -> code")
        val known = Regex("[\"]([a-z_]+)[\"] -> tr[(]").findAll(legacy).map { it.groupValues[1] }.toSet()
        assertTrue(known.size > 40)
        assertEquals(emptySet<String>(), known - table.keys)
    }
}
