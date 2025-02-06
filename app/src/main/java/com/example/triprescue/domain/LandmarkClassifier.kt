package com.example.triprescue.domain

import android.graphics.Bitmap

interface LandmarkClassifier {
    fun classifly(bitmap: Bitmap, rotation:Int): List<Classification> //takes bitmap and rotation output is list of name and score

}