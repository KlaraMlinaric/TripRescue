package com.example.triprescue

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CameraAlt
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.core.app.ActivityCompat
import com.example.triprescue.bnb.BotomNavBar
import com.example.triprescue.model.WeatherResponse
import com.example.triprescue.network.RetrofitInstance
import com.example.triprescue.ui.theme.TripRescueTheme
import com.example.triprescue.utils.getWeatherIcon
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import kotlinx.coroutines.launch






class HomeActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // request location
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) !=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }

        setContent {
            TripRescueTheme {
                HomeScreen(fusedLocationClient)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("Permissions", "Location permissions granted")
            } else {
                Log.d("Permissions", "Location permissions denied")
            }
        }
    }
}


@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(fusedLocationClient: FusedLocationProviderClient) {
    val pagerState = rememberPagerState( )
    val coroutineScope = rememberCoroutineScope()
    var cityName by remember{mutableStateOf("Fetching location...") }
    var weatherData by remember{mutableStateOf<WeatherResponse?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Permissions", "Location permissions are not granted")
            return@LaunchedEffect
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    cityName = addresses[0].locality ?: "Unknown location"
                    Log.d("Location", "Location retrieved: $cityName")

                    // Fetch weather data
                    coroutineScope.launch {
                        try {
                            val response = RetrofitInstance.api.getWeather(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            weatherData = response
                        } catch (e: Exception) {
                            Log.e("Weather", "Error fetching weather data", e)}
                    }
                } else{
                    Log.d("Geocoder", "No addresses found")
                }
            } else {
                Log.d("Location", "No location found")}
        }.addOnFailureListener {
            Log.e("Location", "Failed to get location", it)}
    }

    Scaffold(
        topBar = { TopAppBar()},
        bottomBar = { BotomNavBar()},
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                WelcomeText(cityName)
                Cards(pagerState, weatherData)
                Indicator(pagerState)
                ButtonBox()
                BottomText()
            }
        }
    )
}


@Composable
fun TopAppBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 20.dp,bottomEnd = 20.dp)
            ),
        contentAlignment= Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = colorScheme.tertiary)) {
                    append("Trip")}
                withStyle(style = SpanStyle(color = Color(0xFFF7F2F2))) {
                    append("Rescue")}
            },
            style =MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical =16.dp)
        )
    }
}

@Composable
fun WelcomeText(cityName: String) {
    Text(
        text = "Welcome to $cityName",
        color = colorScheme.onBackground,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, top = 45.dp, bottom = 45.dp),
        textAlign = TextAlign.Left
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Cards(pagerState: PagerState,weatherData: WeatherResponse?) {
    HorizontalPager(
        state = pagerState,
        count =3,
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .padding(vertical = 16.dp)
    ) { page ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal =16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.secondary)
                    .padding(16.dp)
            ) {
                when (page) {
                    0 -> {
                        if (weatherData != null) {
                            val currentWeather = weatherData.current_weather
                            val weatherIcon = getWeatherIcon(currentWeather.weathercode) // get weather icon

                            Row(
                                verticalAlignment =Alignment.CenterVertically,
                                horizontalArrangement =Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = weatherIcon,
                                    contentDescription = "Weather Icon",
                                    tint = Color(0xFFE0E0E0),
                                    modifier = Modifier.size(60.dp) //icon size
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Current Weather",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Temperature: ${currentWeather.temperature}°C",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Wind Speed: ${currentWeather.windspeed} m/s",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Wind Direction: ${currentWeather.winddirection}°",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        //card 2
                        if (weatherData != null) {
                            val daily = weatherData.daily
                            val forecastDays = (0 until minOf(3, daily.time.size))

                            Column {
                                Text(
                                    text = "3-Day Forecast",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =Arrangement.SpaceBetween
                                ) {
                                    forecastDays.forEachIndexed {index,dayIndex ->
                                        val date = daily.time[dayIndex]
                                        val formattedDate = formatDate(date)
                                        val tempMax = daily.temperature_2m_max[dayIndex]
                                        val tempMin = daily.temperature_2m_min[dayIndex]
                                        val weatherCode = daily.weathercode[dayIndex]
                                        val weatherIcon = getWeatherIcon(weatherCode)


                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .padding(horizontal =8.dp)
                                                .weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = weatherIcon,
                                                contentDescription = "Weather Icon",
                                                tint = Color(0xFFE0E0E0),
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = formattedDate,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = "${tempMax}°/${tempMin}°",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // card 3
                        Column {
                            Text(
                                text = "Weather Tips",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier =Modifier.height(8.dp))
                            Text(
                                text = "1. Wear sunscreen on sunny days to protect your skin.",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "2. Keep hydrated during hot weather.",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "3. Wear layers on cold days.",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun Indicator(pagerState: PagerState,pageCount: Int = 3) {//cards
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 0 until pageCount) {
            val color = if (pagerState.currentPage == i) colorScheme.primary else Color.Gray
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(3.dp)
                    .background(color, CircleShape)
            )
        }
    }
}


@Composable
fun ButtonBox() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(150.dp)
            .background(colorScheme.tertiary,RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.TopStart) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularButton(
                    icon = Icons.Default.CameraAlt,
                    text = "Scan",
                    onClick = {context.startActivity(Intent(context, MainActivity::class.java)) }
                )
                CircularButton(
                    icon = Icons.Default.Map,
                    text = "Map",
                    onClick = {context.startActivity(Intent(context, MapActivity::class.java))}
                )
                CircularButton(
                    icon = Icons.Default.PhotoLibrary,
                    text = "Memory map",
                    onClick = { context.startActivity(Intent(context, UserActivity::class.java)) }
                )
                CircularButton(
                    icon = Icons.Default.Translate,
                    text = "Translate",
                    onClick = {context.startActivity(Intent(context, TextRecognitionActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
fun CircularButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(colorScheme.scrim, CircleShape)
                .clickable(onClick =onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White
            )}
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onPrimary
        )
    }
}

@Composable
fun BottomText() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(350.dp)
            .background(colorScheme.primary, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.TopStart
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)//scrolling
        ) {
            Text(
                text = "About authors",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hi there! We are TripRescue team, students from Croatia, and we’ve created this app to make your travels smoother and more enjoyable. Got any questions or cool suggestions? Feel free to shoot us an email at klara.mlinaric2@skole.hr.\n" +
                        "\n" +
                        "Remember, as they say, “Follow your dreams—they know the way!” Safe travels and happy exploring!\n" +
                        "\n",
                color =Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

fun formatDate(date: String): String {
    val parts = date.split("-")
    val day = parts[2].toInt()
    val month = parts[1].toInt()
    return "${day}.${month}"
}