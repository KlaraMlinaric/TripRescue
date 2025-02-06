package com.example.triprescue.Presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.triprescue.domain.Classification
import com.example.triprescue.domain.LandmarkClassifier

class LandmarkImageAnalyzer(
    private val classifier: LandmarkClassifier,
    private val onResults: (List<Classification>) -> Unit

): ImageAnalysis.Analyzer{

    private var frameSkipCounter = 0//don't analyse every frame

    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 60 ==0) {//every 60 frames, every sec one image

            val rotationDegrees = image.imageInfo.rotationDegrees
            val bitmap = image
                .toBitmap()
                .centerCrop(321, 321)

            val results = classifier.classifly(bitmap, rotationDegrees)
            onResults(results)
        }
        frameSkipCounter++

        image.close()
    }
}