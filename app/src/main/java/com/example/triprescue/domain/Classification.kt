package com.example.triprescue.domain


data class Classification(
    val name: String, //name of building
    val score: Float, //how sure model is
)
