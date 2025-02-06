package com.example.triprescue.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getWeatherIcon(weatherCode: Int): ImageVector {
    return when (weatherCode) {
        0 -> Icons.Default.WbSunny // clear
        1 -> Icons.Default.WbSunny // mainly clear
        2 -> Icons.Default.CloudQueue // partly cloudy
        3 -> Icons.Default.Cloud // cloudy
        45, 48 -> Icons.Default.Cloud // fog ------???????????????????????
        51, 53, 55 -> Icons.Default.Opacity // light rain
        61, 63, 65 -> Icons.Default.Grain // rain
        71, 73, 75 -> Icons.Default.AcUnit // snow
        95, 96, 99 -> Icons.Default.Thunderstorm // storm
        else -> Icons.Default.WbSunny // defaul
    }
}
