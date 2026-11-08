package com.naro.atmos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF101B2B)
private val Panel = Color(0xFF1B2A3D)
private val Accent = Color(0xFF79BFFF)
private val Muted = Color(0xFFA8B7C9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Navy,
                    surface = Panel,
                    primary = Accent
                )
            ) {
                AtmosDashboard()
            }
        }
    }
}

@Composable
fun AtmosDashboard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Navy, Color(0xFF15283D), Navy)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "ATMOS",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = Color.White
                )
                Text(
                    "WEATHER INTELLIGENCE",
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = Muted
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search city",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(34.dp))

        Text(
            "CURRENT LOCATION",
            color = Muted,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = Accent
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Choose a city",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Panel
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.CloudQueue,
                    contentDescription = "Weather",
                    tint = Accent,
                    modifier = Modifier.size(76.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "--°",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )

                Text(
                    "Weather data unavailable",
                    color = Muted,
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Connect to live weather",
                    color = Accent,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "WEATHER OVERVIEW",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherMetric(
                "Humidity", "--",
                Icons.Outlined.WaterDrop,
                Modifier.weight(1f)
            )
            WeatherMetric(
                "Wind speed", "--",
                Icons.Outlined.Air,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherMetric(
                "UV index", "--",
                Icons.Outlined.WbSunny,
                Modifier.weight(1f)
            )
            WeatherMetric(
                "Feels like", "--",
                Icons.Outlined.Thermostat,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "FORECAST",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Panel
            )
        ) {
            Box(
                Modifier.padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Your hourly and 7-day forecasts will appear here.",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(
            "ATMOS  •  A PRODUCT OF NARO STUDIOS",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun WeatherMetric(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(23.dp)
            )
            Spacer(Modifier.height(15.dp))
            Text(
                value,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                title,
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}
