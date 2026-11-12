// ARMOR-ANDROID-CONTROL - the two reports of the server for administrators and operators: the summary of a day and the activity heat map of a camera.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import es.electrohobby3d.armor.contracts.parseTolerant
import org.json.JSONArray
import org.json.JSONObject

/** A pattern the summary found: what kind, about what (a code and a zone, a node, a zone) and how many times. */
data class ReportFinding(val kind: String, val subject: String, val count: Int)

/** The summary of one day, as the server made it: the counts, the readable text in the language asked and, only when the settings turn it on, the note of the AI. */
data class DailySummaryView(
    val date: String, val language: String, val events: Int, val alarms: Int, val serious: Int, val falseAlarms: Int, val text: String, val findings: List<ReportFinding>, val aiNote: String?,
)

/** The heat map of a camera: the grid it was counted on and the picture (PNG bytes), when it came. Only counted positions: never a picture of anybody. */
data class HeatmapView(val cameraId: String, val cols: Int, val rows: Int, val positions: Int, val png: ByteArray?) {
    override fun equals(other: Any?) = other is HeatmapView && cameraId == other.cameraId && cols == other.cols && rows == other.rows && positions == other.positions && (png?.contentEquals(other.png ?: ByteArray(0)) ?: (other.png == null))
    override fun hashCode() = cameraId.hashCode() * 31 + positions
}

private fun Long.clamped(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

object ReportsParser {
    val LANGUAGES = listOf("en", "es", "de", "fr", "it", "ja", "zh", "pt")

    /**
     * The summary of a day. A document that is the message `daily_summary` of the shared contract (an unknown member is ignored) is read through it; anything else is read by the
     * tolerant reader below, so a summary the contract does not describe still shows what it has.
     */
    fun summary(json: JSONObject): DailySummaryView {
        val message = parseTolerant("daily_summary", json) as? es.electrohobby3d.armor.contracts.DailySummary
        if (message != null) {
            val totals = message.summary.totals
            val findings = message.findings.map { item ->
                val about = listOf(item.subject.code, item.subject.zone, item.subject.node).mapNotNull { it?.ifBlank { null } }.joinToString(" / ")
                ReportFinding(item.kind, about.ifBlank { "-" }, item.count.clamped())
            }
            return DailySummaryView(
                date = message.date, language = message.language, events = totals.events.clamped(), alarms = totals.alarms.clamped(), serious = totals.serious_alarms.clamped(),
                falseAlarms = totals.false_alarms.clamped(), text = message.text, findings = findings, aiNote = message.ai_note?.ifBlank { null },
            )
        }
        return legacySummary(json)
    }

    private fun legacySummary(json: JSONObject): DailySummaryView {
        val body = json.optJSONObject("summary")
        val totals = body?.optJSONObject("totals")
        return DailySummaryView(
            date = json.optString("date"), language = json.optString("language"),
            events = totals?.optInt("events") ?: 0, alarms = totals?.optInt("alarms") ?: 0, serious = totals?.optInt("serious_alarms") ?: 0, falseAlarms = totals?.optInt("false_alarms") ?: 0,
            text = json.optString("text"), findings = findings(json.optJSONArray("findings")), aiNote = json.optString("ai_note", "").ifBlank { null },
        )
    }

    private fun findings(array: JSONArray?): List<ReportFinding> = buildList {
        if (array == null) return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val subject = item.optJSONObject("subject")
            val about = listOf("code", "zone", "node").mapNotNull { subject?.optString(it, "")?.ifBlank { null } }.joinToString(" / ")
            add(ReportFinding(item.optString("kind"), about.ifBlank { "-" }, item.optInt("count")))
        }
    }

    fun heatmap(json: JSONObject, png: ByteArray?): HeatmapView {
        val message = parseTolerant("heatmap", json) as? es.electrohobby3d.armor.contracts.Heatmap
        if (message != null) return HeatmapView(message.camera_id, message.cols.clamped(), message.rows.clamped(), message.accepted_positions.clamped(), png)
        return HeatmapView(json.optString("camera_id"), json.optInt("cols"), json.optInt("rows"), json.optInt("accepted_positions"), png)
    }

    /** A day as YYYY-MM-DD that is a real day from the year 2000 (what the server accepts), or false. */
    fun validDate(text: String): Boolean {
        val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(text) ?: return false
        val (year, month, day) = match.destructured
        val y = year.toInt(); val m = month.toInt(); val d = day.toInt()
        if (y < 2000 || m !in 1..12 || d < 1) return false
        val longest = when (m) { 2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
        return d <= longest
    }
}

/** Why a report was refused, as the Spanish phrase of the app (the key of its table) that says what to do; null is never returned: an unknown refusal has a phrase too. */
object ReportRefusal {
    fun phrase(status: Int?, error: String): String = when {
        error == "daily_summary_disabled" -> "El resumen diario está apagado. Un administrador lo enciende en Configuración > Seguridad (\"Resumen de un día\")."
        error == "heatmap_disabled" -> "El mapa de calor está apagado. Un administrador lo enciende en Configuración > Seguridad (\"Mapa de calor de actividad\")."
        error == "invalid_date" -> "Eso no es un día (usa una fecha real desde el año 2000)."
        error == "invalid_language" || error == "invalid_query" -> "El servidor no ha entendido la petición."
        error == "unknown_camera" -> "El servicio de visión no conoce esa cámara."
        error == "heatmap_pipeline_off" -> "El flujo de actividad está apagado en el servicio de visión, así que no hay mapa."
        error == "heatmap_not_configured" -> "El mapa de calor no está configurado: falta la dirección del servicio de visión, es incorrecta o está en otra máquina sin permiso (Configuración > Seguridad)."
        error == "daily_summary_not_configured" -> "El resumen diario no está configurado en el servidor (el servicio de resumen y su archivo de ajustes)."
        error == "daily_summary_off_in_settings" -> "El propio servicio de resumen tiene el resumen apagado en su archivo de ajustes."
        error == "report_busy" -> "El servidor está haciendo otro informe. Inténtalo de nuevo en unos segundos."
        error == "daily_summary_failed" || error == "heatmap_failed" || status == 502 -> "No se pudo hacer el informe (el servicio falló o respondió algo que no es un informe). No se ha inventado nada."
        status == 429 -> "Demasiadas peticiones este minuto. Espera un poco e inténtalo de nuevo."
        status == 401 || status == 403 -> "Solo un administrador o un operador puede pedir los informes."
        status == 404 -> "Ese informe está apagado en el servidor."
        status == 503 -> "El informe no está configurado en el servidor."
        status == null -> "El servidor no ha respondido. Inténtalo de nuevo en un momento."
        else -> "El servidor ha rechazado la petición."
    }

    /** Every phrase this object can give, for the tests that keep the translation tables whole. */
    val ALL: List<String> = listOf(
        phrase(404, "daily_summary_disabled"), phrase(404, "heatmap_disabled"), phrase(422, "invalid_date"), phrase(422, "invalid_query"), phrase(404, "unknown_camera"), phrase(404, "heatmap_pipeline_off"),
        phrase(503, "heatmap_not_configured"), phrase(503, "daily_summary_not_configured"), phrase(503, "daily_summary_off_in_settings"), phrase(503, "report_busy"), phrase(502, "heatmap_failed"),
        phrase(429, ""), phrase(403, ""), phrase(404, ""), phrase(503, ""), phrase(null, ""), phrase(418, ""),
    ).distinct()
}
