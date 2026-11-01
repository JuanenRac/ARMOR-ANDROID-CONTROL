// ARMOR-ANDROID-CONTROL - the reports of the server: what is read from them, what a refusal says, and that every phrase of the screen is in the eight languages.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.ReportRefusal
import es.electrohobby3d.armor.model.ReportsParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReportsTest {
    private val summaryJson = """{"date":"2026-03-05","language":"es","summary":{"schema":"armor-daily-summary/1","totals":{"events":7,"alarms":5,"serious_alarms":2,"false_alarms":1}},
        "findings":[{"kind":"recurrent_false_alarm","subject":{"zone":"garden"},"count":4},{"kind":"hourly_repeat","subject":{"code":"intrusion","zone":"hall"},"count":3},{"kind":"intermittent_link","subject":{},"count":2}],
        "text":"Un día tranquilo.","ai_note":"Mira el sensor del jardín."}"""

    @Test fun `the summary is read with its counts, its patterns and its note`() {
        val summary = ReportsParser.summary(JSONObject(summaryJson))
        assertEquals(listOf(7, 5, 2, 1), listOf(summary.events, summary.alarms, summary.serious, summary.falseAlarms))
        assertEquals("Un día tranquilo.", summary.text)
        assertEquals("Mira el sensor del jardín.", summary.aiNote)
        assertEquals(listOf("garden", "intrusion / hall", "-"), summary.findings.map { it.subject })
        assertEquals(listOf(4, 3, 2), summary.findings.map { it.count })
    }

    @Test fun `a summary without a note or findings reads as empty and never fails`() {
        val summary = ReportsParser.summary(JSONObject("""{"date":"2026-03-05","language":"en","text":"x"}"""))
        assertNull(summary.aiNote)
        assertTrue(summary.findings.isEmpty())
        assertEquals(0, summary.events)
    }

    @Test fun `the heat map keeps the grid and the positions counted`() {
        val map = ReportsParser.heatmap(JSONObject("""{"camera_id":"cam-front","cols":4,"rows":3,"accepted_positions":12,"contains_images":false}"""), byteArrayOf(1, 2, 3))
        assertEquals(listOf("cam-front", 4, 3, 12), listOf(map.cameraId, map.cols, map.rows, map.positions))
        assertNotNull(map.png)
        assertEquals(map, ReportsParser.heatmap(JSONObject("""{"camera_id":"cam-front","cols":4,"rows":3,"accepted_positions":12}"""), byteArrayOf(1, 2, 3)))
    }

    @Test fun `a day is a real date from the year 2000`() {
        for (good in listOf("2026-03-05", "2024-02-29", "2000-01-01", "2026-12-31")) assertTrue(good, ReportsParser.validDate(good))
        for (bad in listOf("2026-02-30", "2026-13-01", "1999-12-31", "2026-1-1", "yesterday", "", "2026-03-05T10", "2026-00-10", "2026-04-31", "2100-02-29")) assertFalse(bad, ReportsParser.validDate(bad))
    }

    @Test fun `every refusal of the server has words that say what to do, and the same status never says something else`() {
        val cases = mapOf(
            (404 to "daily_summary_disabled") to "apagado", (404 to "heatmap_disabled") to "apagado", (422 to "invalid_date") to "Eso no es un día", (503 to "heatmap_not_configured") to "no está configurado",
            (503 to "daily_summary_not_configured") to "no está configurado", (503 to "report_busy") to "otro informe", (502 to "heatmap_failed") to "No se pudo", (502 to "") to "No se pudo",
            (429 to "") to "Demasiadas", (403 to "") to "administrador o un operador", (401 to "") to "administrador o un operador", (404 to "") to "apagado", (503 to "") to "no está configurado",
            (404 to "unknown_camera") to "no conoce esa cámara", (404 to "heatmap_pipeline_off") to "flujo de actividad",
        )
        for ((case, words) in cases) assertTrue("$case -> ${ReportRefusal.phrase(case.first, case.second)}", ReportRefusal.phrase(case.first, case.second).contains(words))
        assertTrue(ReportRefusal.phrase(null, "").contains("no ha respondido"))
        assertTrue(ReportRefusal.phrase(418, "").isNotBlank())
        assertEquals(ReportRefusal.ALL.size, ReportRefusal.ALL.toSet().size)
    }

    private val languages = listOf("de", "en", "fr", "it", "ja", "pt", "zh")
    private fun table(language: String) = JSONObject(File("src/main/assets/i18n/$language.json").readText(Charsets.UTF_8))

    @Test fun `every phrase of the reports is in the seven tables of the app`() {
        val phrases = ReportRefusal.ALL + listOf(
            "Informes", "El resumen de un día y el mapa de calor de actividad", "Resumen de un día", "Día (AAAA-MM-DD)", "Mostrar", "Cargando…", "Eventos", "Alarmas", "Alarmas graves", "Falsas alarmas",
            "Patrones encontrados", "%s veces", "Nota de la IA:", "Mapa de calor de actividad", "La única / la primera", "Mapa de calor de la cámara %s", "%s: %s posiciones contadas en una rejilla de %s x %s.",
            "La misma alarma a la misma hora cada hora", "Un sensor que falla a ratos", "Un nodo que pierde el enlace una y otra vez", "Una zona con falsas alarmas repetidas", "Consumo muy por encima de lo habitual",
            "Hecho con las alarmas que guardó el servidor, los enlaces perdidos, las falsas alarmas y, si el registro diario está activo, las lecturas y el consumo de la casa. Ningún nombre, texto ni dirección de la casa llega a la IA.",
            "Dónde se vio actividad en la escena de una cámara: solo posiciones contadas, nunca una imagen de nadie. Viene del servicio de visión en vivo.",
            "Para administradores y operadores. Cada informe está apagado hasta que un administrador lo enciende en Seguridad, en Studio; es para que lo lea una persona y no cambia nada.",
        )
        for (language in languages) {
            val words = table(language)
            for (phrase in phrases) {
                assertTrue("$language lacks: $phrase", words.has(phrase))
                assertTrue("$language empty: $phrase", words.getString(phrase).isNotBlank())
            }
            // a phrase with holes keeps the same number of them in every language
            for (phrase in phrases.filter { it.contains("%s") }) assertEquals("$language holes of $phrase", phrase.split("%s").size, words.getString(phrase).split("%s").size)
        }
        assertEquals("Relatórios", table("pt").getString("Informes"))
        assertEquals("Mapa de calor da câmara %s", table("pt").getString("Mapa de calor de la cámara %s"))
    }
}
