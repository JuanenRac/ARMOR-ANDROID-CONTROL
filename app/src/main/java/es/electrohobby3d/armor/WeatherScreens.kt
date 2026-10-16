// ARMOR-ANDROID-CONTROL - the weather of the chosen place: now, the rain of the next hour, warnings, 24 hours, ten days, the air and pollen, the sun and the moon,
// and the live rain-and-cloud radar map (see WeatherRadarView.kt).
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.Place
import es.electrohobby3d.armor.model.WeatherText

@Composable
fun WeatherScreen(weather: WeatherUiState, onSearch: (String) -> Unit, onPick: (Place) -> Unit, onRefresh: () -> Unit, onChangePlace: () -> Unit) {
    if (weather.place == null) { PlacePicker(weather, onSearch, onPick); return }
    val forecast = weather.forecast
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(weather.place.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium); Text(listOfNotNull(weather.place.region, weather.place.country).joinToString(", "), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Cambiar lugar"), style = MaterialTheme.typography.labelMedium, color = ArmorColors.Cyan, modifier = Modifier.clickable(onClick = onChangePlace).padding(4.dp))
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = tr("Actualizar"), tint = ArmorColors.Cyan) }
                }
            }
        }
        if (weather.loading && forecast == null) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = ArmorColors.Cyan) } }
        if (weather.error != null) item { Text(weather.error, color = ArmorColors.Alert, style = MaterialTheme.typography.bodySmall) }
        if (forecast != null) {
            val nowIso = WeatherText.localIso(System.currentTimeMillis(), forecast.utcOffsetSeconds)
            val fromIndex = WeatherText.hourIndex(forecast.hourly.time, nowIso)
            item { HeroPanel(forecast) }
            val (rainMm, raining) = WeatherText.nextHourRain(forecast.minutely, nowIso)
            if (rainMm > 0 || raining) item {
                Panel(Modifier.fillMaxWidth()) { Text(if (raining) trf("Está lloviendo ahora · %s mm en la próxima hora", rainMm) else trf("Puede llover: %s mm en la próxima hora", rainMm), Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium) }
            }
            val warnings = WeatherText.warnings(forecast, fromIndex)
            if (warnings.isNotEmpty()) item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        warnings.forEach { Text("${if (it.level == "warning") "⚠️" else "⚠"} ${it.key} · ${WeatherText.dayOf(it.at)} ${WeatherText.hourOf(it.at)}", style = MaterialTheme.typography.bodySmall, color = if (it.level == "warning") ArmorColors.Alert else ArmorColors.Amber) }
                    }
                }
            }
            item { Text(tr("Radar en directo"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item { RadarMapView(weather.place.latitude, weather.place.longitude, weather.place.name) }
            item { Text(tr("Próximas horas"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item { HourlyRow(forecast, fromIndex) }
            item { Text(tr("Diez días"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(forecast.daily.time.indices.toList()) { DayRow(forecast, it) }
            if (weather.air != null) item { AirPanel(weather) }
            item { SunMoonPanel(forecast) }
        }
    }
}

@Composable
private fun PlacePicker(weather: WeatherUiState, onSearch: (String) -> Unit, onPick: (Place) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Elige el lugar del que quieres ver el tiempo"), style = MaterialTheme.typography.bodyMedium)
        Text(tr("No se pide nada por Internet hasta que eliges un lugar; solo sale su coordenada."), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(query, { query = it; onSearch(it) }, label = { Text(tr("Buscar un lugar")) }, singleLine = true, modifier = Modifier.weight(1f))
            IconButton(onClick = { onSearch(query) }) { Icon(Icons.Filled.Search, contentDescription = tr("Buscar"), tint = ArmorColors.Cyan) }
        }
        weather.results.forEach { place ->
            Panel(Modifier.fillMaxWidth(), onClick = { onPick(place) }) {
                Column(Modifier.padding(12.dp)) { Text(place.name, fontWeight = FontWeight.SemiBold); Text(listOfNotNull(place.region, place.country).joinToString(", "), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted) }
            }
        }
    }
}

@Composable
private fun HeroPanel(forecast: es.electrohobby3d.armor.model.Forecast) {
    val current = forecast.current
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(WeatherText.icon(current?.code, current?.isDay ?: true), style = MaterialTheme.typography.displaySmall)
                Column {
                    Text("${current?.temperature?.let { Math.round(it) } ?: "—"}°", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(WeatherText.description(current?.code), style = MaterialTheme.typography.bodyMedium, color = ArmorColors.Muted)
                }
            }
            Text(trf("Sensación %s° · Humedad %s%% · Nubes %s%%", current?.apparent?.let { Math.round(it) } ?: "—", current?.humidity?.toInt() ?: "—", current?.cloud?.toInt() ?: "—"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Text(trf("Viento %s %s · %s km/h (ráfagas %s)", WeatherText.wind(current?.windSpeed), WeatherText.compass(current?.windDir) ?: "", current?.windSpeed?.toInt() ?: "—", current?.windGust?.toInt() ?: "—"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            Text(trf("Presión %s hPa", current?.pressure?.toInt() ?: "—"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun HourlyRow(forecast: es.electrohobby3d.armor.model.Forecast, fromIndex: Int) {
    val end = (fromIndex + 24).coerceAtMost(forecast.hourly.time.size)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items((fromIndex until end).toList()) { i ->
            Panel(Modifier.width(72.dp)) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(WeatherText.hourOf(forecast.hourly.time[i]), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                    Text(WeatherText.icon(forecast.hourly.code.getOrNull(i), (forecast.hourly.isDay.getOrNull(i) ?: 1) != 0))
                    Text("${forecast.hourly.temperature.getOrNull(i)?.let { Math.round(it) } ?: "—"}°", fontWeight = FontWeight.SemiBold)
                    val prob = forecast.hourly.precipProb.getOrNull(i)
                    if ((prob ?: 0.0) > 0) Text("${prob?.toInt()}%", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Cyan)
                }
            }
        }
    }
}

@Composable
private fun DayRow(forecast: es.electrohobby3d.armor.model.Forecast, i: Int) {
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(WeatherText.weekday(forecast.daily.time[i]) + " " + WeatherText.dayOf(forecast.daily.time[i]), modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
            Text(WeatherText.icon(forecast.daily.code.getOrNull(i)), modifier = Modifier.width(32.dp))
            val prob = forecast.daily.precipProbMax.getOrNull(i)
            Text(if ((prob ?: 0.0) > 0) "${prob?.toInt()}%" else "", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Cyan)
            Text("${forecast.daily.tMin.getOrNull(i)?.let { Math.round(it) } ?: "—"}° / ${forecast.daily.tMax.getOrNull(i)?.let { Math.round(it) } ?: "—"}°", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AirPanel(weather: WeatherUiState) {
    val air = weather.air ?: return
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Aire y polen"), fontWeight = FontWeight.SemiBold)
            Text(trf("Calidad del aire: %s (índice %s) · PM2.5 %s · PM10 %s · O₃ %s · NO₂ %s", WeatherText.aqi(air.europeanAqi), air.europeanAqi ?: "—", air.pm25?.toInt() ?: "—", air.pm10?.toInt() ?: "—", air.o3?.toInt() ?: "—", air.no2?.toInt() ?: "—"), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            val pollen = air.pollen.entries.filter { it.value != null && it.value!! > 0 }
            if (pollen.isNotEmpty()) Text(pollen.joinToString(" · ") { "${WeatherText.pollenName(it.key)}: ${WeatherText.pollen(it.value)}" }, style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
            else Text(tr("Sin polen relevante ahora mismo."), style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted)
        }
    }
}

@Composable
private fun SunMoonPanel(forecast: es.electrohobby3d.armor.model.Forecast) {
    val (icon, name) = WeatherText.moonPhase(System.currentTimeMillis())
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(trf("☀️ Amanece %s", forecast.daily.sunrise.firstOrNull()?.let(WeatherText::hourOf) ?: "—"), style = MaterialTheme.typography.bodySmall); Text(trf("Anochece %s", forecast.daily.sunset.firstOrNull()?.let(WeatherText::hourOf) ?: "—"), style = MaterialTheme.typography.bodySmall) }
            Column(horizontalAlignment = Alignment.End) { Text("${icon} ${name}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
