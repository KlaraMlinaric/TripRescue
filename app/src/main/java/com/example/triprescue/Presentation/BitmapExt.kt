package com.example.triprescue.Presentation
//just for croping image for model to recognise 320px
import android.graphics.Bitmap

fun Bitmap.centerCrop(desiredWidth: Int, desiredHeight: Int): Bitmap{
    val xStart = (width-desiredWidth)/2
    val yStart = (height-desiredHeight)/2

    if(xStart<0 || yStart<0 || desiredWidth>width || desiredHeight>height){
        throw IllegalArgumentException("Invalid Argument for center cropping")
    }
    return Bitmap.createBitmap(this, xStart, yStart, desiredWidth, desiredHeight)
}