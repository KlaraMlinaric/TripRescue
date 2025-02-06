package com.example.triprescue.Umap

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// retrofit service geocodingg
interface GeocodingService {
    @GET("maps/api/geocode/json")
    fun geocodeLocation(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Call<GeocodingResponse>
}
