package com.naro.atmos

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.isActive

private val Background = Color(0xFF080F1D)
private val Panel = Color(0xFF121F32)
private val Blue = Color(0xFF78BFFF)
private val Muted = Color(0xFF91A5BE)
private val White = Color(0xFFF4F8FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Background,
                    surface = Panel,
                    primary = Blue
                )
            ) {
                AtmosApp()
            }
        }
    }
}

@Composable
private fun AtmosApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fusedLocation = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fahrenheit by remember { mutableStateOf(false) }

    var showSearch by remember { mutableStateOf(false) }
    var cityQuery by remember { mutableStateOf("") }
    var cityResults by remember {
        mutableStateOf<List<CityResult>>(emptyList())
    }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }

    val loadCurrentLocation: () -> Unit = {
        scope.launch {
            loading = true
            errorMessage = null

            try {
                val location = fusedLocation.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).awaitLocation()
                    ?: fusedLocation.lastLocation.awaitLocation()

                if (location == null) {
                    throw Exception(
                        "Couldn't get your location. Try city search."
                    )
                }

                weather = WeatherRepository.fetchWeather(
                    location.latitude,
                    location.longitude,
                    "Current location"
                )
            } catch (e: Exception) {
                errorMessage = e.message
                    ?: "Location or weather request failed."
            } finally {
                loading = false
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                loadCurrentLocation()
            } else {
                errorMessage =
                    "Location permission denied. Search for a city instead."
            }
        }

    fun useMyLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            loadCurrentLocation()
        } else {
            permissionLauncher.launch(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun searchForCity() {
        if (cityQuery.isBlank()) {
            searchError = "Enter a city name."
            return
        }

        scope.launch {
            searching = true
            searchError = null
            cityResults = emptyList()

            try {
                cityResults =
                    WeatherRepository.searchCities(cityQuery)

                if (cityResults.isEmpty()) {
                    searchError =
                        "No matching city found. Try another name."
                }
            } catch (e: Exception) {
                searchError = e.message
                    ?: "City search failed. Check your connection."
            } finally {
                searching = false
            }
        }
    }

    fun chooseCity(city: CityResult) {
        showSearch = false

        scope.launch {
            loading = true
            errorMessage = null

            try {
                val label = if (city.country.isBlank()) {
                    city.name
                } else {
                    "${city.name}, ${city.country}"
                }

                weather = WeatherRepository.fetchWeather(
                    city.latitude,
                    city.longitude,
                    label
                )
            } catch (e: Exception) {
                errorMessage = e.message
                    ?: "Couldn't load weather for this city."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF101D31),
                        Background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.atmos_logo),
                contentDescription = "ATMOS logo",
                modifier = Modifier.size(76.dp)
            )

            TextButton(onClick = { fahrenheit = !fahrenheit }) {
                Text(
                    if (fahrenheit) "°F" else "°C",
                    color = Blue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { useMyLocation() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text("My location")
            }

            OutlinedButton(
                onClick = {
                    showSearch = true
                    cityQuery = ""
                    cityResults = emptyList()
                    searchError = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text("Search city")
            }
        }

        Spacer(Modifier.height(28.dp))

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Blue
            )
            Spacer(Modifier.height(12.dp))
            Text("Getting weather data…", color = Muted)
        }

        errorMessage?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF382532)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    color = White,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        val current = weather

        if (current == null) {
            Text(
                "Your weather, at a glance.",
                color = White,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Use your location or search for a city to get started.",
                color = Muted,
                fontSize = 14.sp
            )
        } else {
            Text(
                current.placeName,
                color = Muted,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "${displayTemp(current.temperature, fahrenheit)}°",
                color = White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Light
            )

            Text(
                "${current.icon}  ${current.description}",
                color = Blue,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Feels like ${displayTemp(current.feelsLike, fahrenheit)}°  •  " +
                    "H: ${displayTemp(current.high, fahrenheit)}°  " +
                    "L: ${displayTemp(current.low, fahrenheit)}°",
                color = Muted,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(26.dp))

            Text(
                "CURRENT CONDITIONS",
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    "Humidity",
                    "${current.humidity}%",
                    Modifier.weight(1f)
                )
                MetricCard(
                    "Wind",
                    "${current.windSpeed.toInt()} km/h",
                    Modifier.weight(1f)
                )
                MetricCard(
                    "UV index",
                    String.format(Locale.getDefault(), "%.1f", current.uvIndex),
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "HOURLY FORECAST",
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                current.hourly.forEach { hour ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Panel
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 14.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(hour.time, color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(hour.icon, fontSize = 22.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "${displayTemp(hour.temperature, fahrenheit)}°",
                                color = White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "7-DAY FORECAST",
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Panel
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    current.daily.forEach { day ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                day.day,
                                color = White,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                day.icon,
                                fontSize = 20.sp
                            )

                            Text(
                                "${displayTemp(day.low, fahrenheit)}°",
                                color = Muted,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )

                            Text(
                                "${displayTemp(day.high, fahrenheit)}°",
                                color = White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "ATMOS  •  WEATHER DATA BY OPEN-METEO",
            color = Muted,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            containerColor = Panel,
            title = {
                Text("Search a city", color = White)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        label = { Text("City name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { searchForCity() },
                        enabled = !searching,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (searching) "Searching…" else "Search"
                        )
                    }

                    searchError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFFFA6A6))
                    }

                    cityResults.forEach { city ->
                        TextButton(
                            onClick = { chooseCity(city) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    city.name,
                                    color = White
                                )
                                Text(
                                    city.country,
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearch = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Panel
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(label, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun displayTemp(
    celsius: Double,
    fahrenheit: Boolean
): Int {
    return if (fahrenheit) {
        (celsius * 9.0 / 5.0 + 32.0).toInt()
    } else {
        celsius.toInt()
    }
}

private suspend fun <T> Task<T>.awaitLocation(): T =
    suspendCancellableCoroutine { continuation ->

        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }

        addOnCanceledListener {
            continuation.cancel()
        }
    }
