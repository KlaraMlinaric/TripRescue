package com.example.triprescue.model

data class WeatherResponse(
    val current_weather: CurrentWeather,
    val daily: DailyForecastData
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Int,
    val weathercode: Int
)

data class DailyForecastData(
    val time: List<String>, // List of dates(IOs format)
    val temperature_2m_max: List<Double>, // max temp
    val temperature_2m_min: List<Double>, // min temp
    val weathercode: List<Int>, // weather codes
    val precipitation_sum: List<Double>
) //list bc for each day
