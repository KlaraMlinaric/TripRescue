package com.example.triprescue.network

import com.example.triprescue.model.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface WeatherService {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}


object RetrofitInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")//api
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: WeatherService by lazy {
        retrofit.create(WeatherService::class.java)
    }
}