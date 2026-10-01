// ARMOR-ANDROID-CONTROL - talks to Open-Meteo directly (no key, CC BY 4.0), never to ARMOR-SERVER: the forecast, the air quality and a place search by name. Nothing is asked
// until the person chooses a place in the Weather screen, and only that place's coordinates (or the text they typed, for a search) leave the phone.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.network

import es.electrohobby3d.armor.model.AirQuality
import es.electrohobby3d.armor.model.Forecast
import es.electrohobby3d.armor.model.Place
import es.electrohobby3d.armor.model.WeatherParser
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val FORECAST_API = "https://api.open-meteo.com"
private const val AIR_QUALITY_API = "https://air-quality-api.open-meteo.com"
private const val GEOCODING_API = "https://geocoding-api.open-meteo.com"
private const val HOURLY = "temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,cloud_cover,is_day"
private const val DAILY = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max"
private const val CURRENT = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
private const val AIR = "european_aqi,pm10,pm2_5,ozone,nitrogen_dioxide,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen"

class WeatherApiClient {
    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.connectTimeout = 10_000; connection.readTimeout = 12_000
        connection.setRequestProperty("Accept", "application/json")
        try {
            if (connection.responseCode != 200) throw IllegalStateException("HTTP ${connection.responseCode}")
            return JSONObject(connection.inputStream.bufferedReader().readText())
        } finally { connection.disconnect() }
    }

    fun forecast(place: Place): Forecast {
        val url = "$FORECAST_API/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}&timezone=auto&forecast_days=10&forecast_minutely_15=8" +
            "&wind_speed_unit=kmh&current=$CURRENT&minutely_15=precipitation&hourly=$HOURLY&daily=$DAILY"
        return WeatherParser.forecast(getJson(url)) ?: throw IllegalStateException("not a forecast")
    }

    fun airQuality(place: Place): AirQuality? = runCatching {
        WeatherParser.airQuality(getJson("$AIR_QUALITY_API/v1/air-quality?latitude=${place.latitude}&longitude=${place.longitude}&timezone=auto&current=$AIR"))
    }.getOrNull()

    fun search(query: String): List<Place> {
        if (query.trim().length < 2) return emptyList()
        val encoded = URLEncoder.encode(query.trim().take(80), "UTF-8")
        val url = "$GEOCODING_API/v1/search?name=$encoded&count=8&language=es&format=json"
        return WeatherParser.places(getJson(url))
    }
}
