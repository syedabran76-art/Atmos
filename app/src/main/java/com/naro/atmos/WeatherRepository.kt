package com.naro.atmos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CityResult(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

data class HourlyWeather(
    val time: String,
    val temperature: Double,
    val icon: String
)

data class DailyWeather(
    val day: String,
    val description: String,
    val icon: String,
    val high: Double,
    val low: Double
)

data class WeatherData(
    val placeName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val uvIndex: Double,
    val high: Double,
    val low: Double,
    val description: String,
    val icon: String,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
)

object WeatherRepository {

    private suspend fun getJson(url: String): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 12000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")

                val code = connection.responseCode
                if (code !in 200..299) {
                    throw Exception("Server returned HTTP $code")
                }

                val body = connection.inputStream.bufferedReader().use {
                    it.readText()
                }
                JSONObject(body)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun searchCities(query: String): List<CityResult> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val json = getJson(
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$encoded&count=8&language=en&format=json"
        )

        val results = json.optJSONArray("results") ?: return emptyList()

        return (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            CityResult(
                name = item.optString("name", "Unknown"),
                country = item.optString("country", ""),
                latitude = item.optDouble("latitude"),
                longitude = item.optDouble("longitude")
            )
        }
    }

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        placeName: String
    ): WeatherData {
        val url =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                "&hourly=temperature_2m,weather_code" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,uv_index_max" +
                "&forecast_days=7&timezone=auto"

        val json = getJson(url)
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val hourly = json.getJSONObject("hourly")

        val currentCode = current.optInt("weather_code")
        val currentTime = current.optString("time").take(13)

        val hourlyTimes = hourly.optJSONArray("time")
        val hourlyTemps = hourly.optJSONArray("temperature_2m")
        val hourlyCodes = hourly.optJSONArray("weather_code")

        var startIndex = 0
        if (hourlyTimes != null) {
            for (i in 0 until hourlyTimes.length()) {
                if (hourlyTimes.optString(i).startsWith(currentTime)) {
                    startIndex = i
                    break
                }
            }
        }

        val hourlyList = mutableListOf<HourlyWeather>()
        if (hourlyTimes != null && hourlyTemps != null && hourlyCodes != null) {
            for (i in startIndex until minOf(startIndex + 8, hourlyTimes.length())) {
                hourlyList.add(
                    HourlyWeather(
                        time = hourlyTimes.optString(i).substringAfter("T"),
                        temperature = hourlyTemps.optDouble(i),
                        icon = weatherIcon(hourlyCodes.optInt(i))
                    )
                )
            }
        }

        val dates = daily.optJSONArray("time")
        val codes = daily.optJSONArray("weather_code")
        val highs = daily.optJSONArray("temperature_2m_max")
        val lows = daily.optJSONArray("temperature_2m_min")
        val uvValues = daily.optJSONArray("uv_index_max")

        val dailyList = mutableListOf<DailyWeather>()
        if (dates != null && codes != null && highs != null && lows != null) {
            for (i in 0 until minOf(7, dates.length())) {
                val date = LocalDate.parse(dates.optString(i))
                dailyList.add(
                    DailyWeather(
                        day = if (i == 0) "Today" else
                            date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                        description = weatherDescription(codes.optInt(i)),
                        icon = weatherIcon(codes.optInt(i)),
                        high = highs.optDouble(i),
                        low = lows.optDouble(i)
                    )
                )
            }
        }

        return WeatherData(
            placeName = placeName,
            temperature = current.optDouble("temperature_2m"),
            feelsLike = current.optDouble("apparent_temperature"),
            humidity = current.optInt("relative_humidity_2m"),
            windSpeed = current.optDouble("wind_speed_10m"),
            uvIndex = uvValues?.optDouble(0) ?: 0.0,
            high = highs?.optDouble(0) ?: 0.0,
            low = lows?.optDouble(0) ?: 0.0,
            description = weatherDescription(currentCode),
            icon = weatherIcon(currentCode),
            hourly = hourlyList,
            daily = dailyList
        )
    }

    private fun weatherDescription(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Weather conditions"
    }

    private fun weatherIcon(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌦️"
        in 85..86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}
