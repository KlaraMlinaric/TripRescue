package com.example.triprescue.Umap
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitInstance {
    private const val BASE_URL = "https://maps.googleapis.com/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL) //for api request maps
        .addConverterFactory(GsonConverterFactory.create())//convert json resolt
        .build()

    val geocodingService: GeocodingService = retrofit.create(GeocodingService::class.java)
}
