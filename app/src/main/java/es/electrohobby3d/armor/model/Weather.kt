// ARMOR-ANDROID-CONTROL - the weather of the place the person chooses, from Open-Meteo (no key; CC BY 4.0): now, the rain of the next hour, warnings the forecast
// implies, 48 hours, ten days, the air and pollen, and the sun and the moon. Nothing is asked until a place is chosen, and only its coordinates leave the phone;
// no ARMOR-SERVER call is involved. Mirrors the core of ARMOR-STUDIO's Weather menu (its live rain-and-cloud radar map is not reproduced here).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ln
import kotlin.math.roundToInt

data class Place(val name: String, val region: String?, val country: String?, val latitude: Double, val longitude: Double)

data class WxCurrent(
    val time: String, val temperature: Double?, val apparent: Double?, val humidity: Double?, val isDay: Boolean, val precipitation: Double?, val code: Int?,
    val cloud: Double?, val pressure: Double?, val windSpeed: Double?, val windDir: Double?, val windGust: Double?,
)
data class WxHourly(
    val time: List<String>, val temperature: List<Double?>, val precipProb: List<Double?>, val precip: List<Double?>, val code: List<Int?>,
    val windSpeed: List<Double?>, val cloud: List<Double?>, val isDay: List<Int?>,
)
data class WxDaily(
    val time: List<String>, val code: List<Int?>, val tMax: List<Double?>, val tMin: List<Double?>, val sunrise: List<String>, val sunset: List<String>,
    val uvMax: List<Double?>, val precipSum: List<Double?>, val precipProbMax: List<Double?>, val windMax: List<Double?>,
)
data class WxMinutely(val time: List<String>, val precip: List<Double?>)
data class Forecast(val latitude: Double, val longitude: Double, val timezone: String, val utcOffsetSeconds: Int, val current: WxCurrent?, val minutely: WxMinutely, val hourly: WxHourly, val daily: WxDaily)
data class AirQuality(val europeanAqi: Int?, val pm10: Double?, val pm25: Double?, val o3: Double?, val no2: Double?, val pollen: Map<String, Double?>)
data class Warning(val level: String, val key: String, val value: Double, val at: String)

object WeatherParser {
    private fun num(array: JSONArray?, i: Int): Double? = if (array != null && i < array.length() && !array.isNull(i)) array.optDouble(i).takeIf { !it.isNaN() } else null
    private fun code(array: JSONArray?, i: Int): Int? = num(array, i)?.roundToInt()
    private fun doubles(array: JSONArray?): List<Double?> = buildList { val n = array?.length() ?: 0; for (i in 0 until n) add(num(array, i)) }
    private fun codes(array: JSONArray?): List<Int?> = buildList { val n = array?.length() ?: 0; for (i in 0 until n) add(code(array, i)) }
    private fun strings(array: JSONArray?): List<String> = buildList { val n = array?.length() ?: 0; for (i in 0 until n) add(array!!.optString(i)) }

    fun place(json: JSONObject): Place? {
        val lat = json.optDouble("latitude"); val lon = json.optDouble("longitude")
        if (lat.isNaN() || lon.isNaN()) return null
        return Place(json.optString("name"), json.optString("admin1").takeIf { it.isNotBlank() }, json.optString("country").takeIf { it.isNotBlank() }, lat, lon)
    }
    fun places(root: JSONObject): List<Place> {
        val array = root.optJSONArray("results") ?: return emptyList()
        return buildList { for (i in 0 until array.length()) array.optJSONObject(i)?.let(::place)?.let(::add) }
    }

    fun forecast(root: JSONObject): Forecast? {
        val lat = root.optDouble("latitude"); val lon = root.optDouble("longitude")
        if (lat.isNaN() || lon.isNaN()) return null
        val c = root.optJSONObject("current")
        val h = root.optJSONObject("hourly")
        val d = root.optJSONObject("daily")
        val m = root.optJSONObject("minutely_15")
        val current = c?.takeIf { it.has("time") }?.let {
            WxCurrent(
                it.optString("time"), it.optDouble("temperature_2m").takeIf { v -> !v.isNaN() }, it.optDouble("apparent_temperature").takeIf { v -> !v.isNaN() },
                it.optDouble("relative_humidity_2m").takeIf { v -> !v.isNaN() }, it.optInt("is_day") != 0, it.optDouble("precipitation").takeIf { v -> !v.isNaN() },
                if (it.has("weather_code")) it.optInt("weather_code") else null, it.optDouble("cloud_cover").takeIf { v -> !v.isNaN() }, it.optDouble("pressure_msl").takeIf { v -> !v.isNaN() },
                it.optDouble("wind_speed_10m").takeIf { v -> !v.isNaN() }, it.optDouble("wind_direction_10m").takeIf { v -> !v.isNaN() }, it.optDouble("wind_gusts_10m").takeIf { v -> !v.isNaN() },
            )
        }
        return Forecast(
            latitude = lat, longitude = lon, timezone = root.optString("timezone", "UTC"), utcOffsetSeconds = root.optInt("utc_offset_seconds"), current = current,
            minutely = WxMinutely(strings(m?.optJSONArray("time")), doubles(m?.optJSONArray("precipitation"))),
            hourly = WxHourly(
                strings(h?.optJSONArray("time")), doubles(h?.optJSONArray("temperature_2m")), doubles(h?.optJSONArray("precipitation_probability")),
                doubles(h?.optJSONArray("precipitation")), codes(h?.optJSONArray("weather_code")), doubles(h?.optJSONArray("wind_speed_10m")),
                doubles(h?.optJSONArray("cloud_cover")), codes(h?.optJSONArray("is_day")),
            ),
            daily = WxDaily(
                strings(d?.optJSONArray("time")), codes(d?.optJSONArray("weather_code")), doubles(d?.optJSONArray("temperature_2m_max")), doubles(d?.optJSONArray("temperature_2m_min")),
                strings(d?.optJSONArray("sunrise")), strings(d?.optJSONArray("sunset")), doubles(d?.optJSONArray("uv_index_max")), doubles(d?.optJSONArray("precipitation_sum")),
                doubles(d?.optJSONArray("precipitation_probability_max")), doubles(d?.optJSONArray("wind_speed_10m_max")),
            ),
        )
    }

    private val pollenKinds = listOf("alder", "birch", "grass", "mugwort", "olive", "ragweed")
    fun airQuality(root: JSONObject): AirQuality? {
        val c = root.optJSONObject("current") ?: return null
        if (!c.has("time")) return null
        return AirQuality(
            europeanAqi = if (c.has("european_aqi")) c.optInt("european_aqi") else null, pm10 = c.optDouble("pm10").takeIf { !it.isNaN() }, pm25 = c.optDouble("pm2_5").takeIf { !it.isNaN() },
            o3 = c.optDouble("ozone").takeIf { !it.isNaN() }, no2 = c.optDouble("nitrogen_dioxide").takeIf { !it.isNaN() },
            pollen = pollenKinds.associateWith { kind -> c.optDouble("${kind}_pollen").takeIf { !it.isNaN() } },
        )
    }
}

/** The wording, icons and simple classifications of the Weather screen, in plain Spanish (matches ARMOR-STUDIO's). */
object WeatherText {
    private val codes: Map<Int, Pair<String, String>> = mapOf(
        0 to ("Despejado" to "☀️"), 1 to ("Poco nuboso" to "🌤️"), 2 to ("Parcialmente nublado" to "⛅"), 3 to ("Cubierto" to "☁️"), 45 to ("Niebla" to "🌫️"), 48 to ("Niebla helada" to "🌫️"),
        51 to ("Llovizna ligera" to "🌦️"), 53 to ("Llovizna" to "🌦️"), 55 to ("Llovizna intensa" to "🌧️"), 56 to ("Llovizna helada" to "🌧️"), 57 to ("Llovizna helada intensa" to "🌧️"),
        61 to ("Lluvia ligera" to "🌦️"), 63 to ("Lluvia" to "🌧️"), 65 to ("Lluvia intensa" to "🌧️"), 66 to ("Lluvia helada" to "🌧️"), 67 to ("Lluvia helada intensa" to "🌧️"),
        71 to ("Nevada ligera" to "🌨️"), 73 to ("Nevada" to "🌨️"), 75 to ("Nevada intensa" to "❄️"), 77 to ("Granos de nieve" to "🌨️"),
        80 to ("Chubascos ligeros" to "🌦️"), 81 to ("Chubascos" to "🌧️"), 82 to ("Chubascos violentos" to "⛈️"),
        85 to ("Chubascos de nieve ligeros" to "🌨️"), 86 to ("Chubascos de nieve intensos" to "❄️"), 95 to ("Tormenta" to "⛈️"), 96 to ("Tormenta con granizo" to "⛈️"), 99 to ("Tormenta con granizo fuerte" to "⛈️"),
    )
    fun description(code: Int?): String = code?.let { codes[it]?.first } ?: "Desconocido"
    fun icon(code: Int?, isDay: Boolean = true): String = code?.let { codes[it]?.second } ?: "❔"

    private val compassWords = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    fun compass(degrees: Double?): String? = degrees?.let { compassWords[(((it % 360) + 360) % 360 / 45.0).roundToInt() % 8] }

    fun wind(kmh: Double?): String = when {
        kmh == null -> "—"; kmh < 2 -> "Calma"; kmh < 20 -> "Flojo"; kmh < 39 -> "Moderado"; kmh < 50 -> "Fresco"; kmh < 62 -> "Fuerte"; kmh < 88 -> "Muy fuerte"; kmh < 118 -> "Temporal"; else -> "Huracanado"
    }
    fun uv(index: Double?): String = when {
        index == null -> "—"; index < 3 -> "Bajo"; index < 6 -> "Moderado"; index < 8 -> "Alto"; index < 11 -> "Muy alto"; else -> "Extremo"
    }
    fun aqi(index: Int?): String = when {
        index == null -> "—"; index <= 20 -> "Buena"; index <= 40 -> "Razonable"; index <= 60 -> "Moderada"; index <= 80 -> "Mala"; index <= 100 -> "Muy mala"; else -> "Extrema"
    }
    fun pollenName(key: String): String = when (key) {
        "alder" -> "Aliso"; "birch" -> "Abedul"; "grass" -> "Gramíneas"; "mugwort" -> "Artemisa"; "olive" -> "Olivo"; "ragweed" -> "Ambrosía"; else -> key
    }
    fun pollen(grains: Double?): String = when {
        grains == null -> "—"; grains < 1 -> "Nulo"; grains < 10 -> "Bajo"; grains < 50 -> "Moderado"; grains < 200 -> "Alto"; else -> "Muy alto"
    }
    fun pressureTrend(now: Double?, before: Double?): String? {
        if (now == null || before == null) return null
        val change = now - before
        return if (change >= 1) "Subiendo" else if (change <= -1) "Bajando" else "Estable"
    }
    fun dewPoint(celsius: Double?, humidity: Double?): Double? {
        if (celsius == null || humidity == null || humidity <= 0) return null
        val a = 17.62; val b = 243.12; val gamma = ln(humidity / 100) + (a * celsius) / (b + celsius)
        return (((b * gamma) / (a - gamma)) * 10).roundToInt() / 10.0
    }
    fun moonPhase(epochMs: Long): Pair<String, String> {
        val days = epochMs / 86400000.0 + 2440587.5 - 2451550.1
        val phase = ((days / 29.530588853) % 1 + 1) % 1
        val index = (phase * 8).roundToInt() % 8
        val icons = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")
        val names = listOf("Luna nueva", "Creciente", "Cuarto creciente", "Gibosa creciente", "Luna llena", "Gibosa menguante", "Cuarto menguante", "Menguante")
        return icons[index] to names[index]
    }

    /** Short date/time formatting from Open-Meteo's local, offset-less ISO strings ("2026-10-02T14:00"). */
    fun hourOf(iso: String): String = iso.takeIf { it.length >= 16 }?.substring(11, 16) ?: iso
    fun dayOf(iso: String): String {
        val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        return runCatching { val y = iso.substring(0, 4).toInt(); val mo = iso.substring(5, 7).toInt(); val d = iso.substring(8, 10).toInt(); "$d ${months[mo - 1]}" }.getOrDefault(iso)
    }
    fun weekday(iso: String): String {
        return runCatching {
            val cal = java.util.Calendar.getInstance()
            cal.set(iso.substring(0, 4).toInt(), iso.substring(5, 7).toInt() - 1, iso.substring(8, 10).toInt())
            listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }.getOrDefault("")
    }

    /** The place's current local time, as Open-Meteo's own ISO form, to find "now" in its hourly/daily series. */
    fun localIso(nowMs: Long, utcOffsetSeconds: Int): String {
        val local = nowMs + utcOffsetSeconds * 1000L
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(local))
    }
    fun hourIndex(times: List<String>, iso: String): Int {
        val hour = iso.take(13)
        val at = times.indexOfFirst { it.take(13) >= hour }
        return if (at < 0) (times.size - 1).coerceAtLeast(0) else at
    }

    /** The rain of the next hour from the 15-minute series: the millimetres expected and whether it is raining right now. */
    fun nextHourRain(minutely: WxMinutely, iso: String): Pair<Double, Boolean> {
        val quarter = iso.take(14) + (iso.substring(14, 16).toInt() / 15 * 15).toString().padStart(2, '0')
        val from = minutely.time.indexOfFirst { it >= quarter }
        if (from < 0) return 0.0 to false
        val steps = minutely.precip.drop(from).take(4)
        val mm = (steps.sumOf { it ?: 0.0 } * 10).roundToInt() / 10.0
        return mm to ((steps.firstOrNull() ?: 0.0) >= 0.1)
    }

    /** A few plain-threshold warnings from the next 48 hours (not an official warning service): heavy rain, strong wind, high UV, frost. */
    fun warnings(forecast: Forecast, fromIndex: Int, hours: Int = 48): List<Warning> {
        val out = mutableListOf<Warning>()
        val end = (fromIndex + hours).coerceAtMost(forecast.hourly.time.size)
        if (fromIndex >= end) return out
        val rain = forecast.hourly.precip.subList(fromIndex, end)
        val wind = forecast.hourly.windSpeed.subList(fromIndex, end)
        val temp = forecast.hourly.temperature.subList(fromIndex, end)
        rain.withIndex().maxByOrNull { it.value ?: 0.0 }?.let { (i, v) -> if ((v ?: 0.0) >= 4) out.add(Warning("warning", "Lluvia intensa", v!!, forecast.hourly.time[fromIndex + i])) }
        wind.withIndex().maxByOrNull { it.value ?: 0.0 }?.let { (i, v) -> if ((v ?: 0.0) >= 50) out.add(Warning(if ((v ?: 0.0) >= 70) "warning" else "watch", "Viento fuerte", v!!, forecast.hourly.time[fromIndex + i])) }
        temp.withIndex().minByOrNull { it.value ?: 100.0 }?.let { (i, v) -> if (v != null && v <= 0) out.add(Warning("watch", "Riesgo de helada", v, forecast.hourly.time[fromIndex + i])) }
        forecast.daily.uvMax.firstOrNull()?.let { if (it >= 8) out.add(Warning(if (it >= 11) "warning" else "watch", "Índice UV muy alto", it, forecast.daily.time.firstOrNull().orEmpty())) }
        return out
    }
}
