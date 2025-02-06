package com.example.triprescue.Umap

data class GeocodingResponse(
    val results: List<GeocodingResult> // fromgeocoding API
)

data class GeocodingResult(
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)
//for google maps location