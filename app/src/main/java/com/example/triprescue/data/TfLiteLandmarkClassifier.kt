package com.example.triprescue.data

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import com.example.triprescue.domain.Classification
import com.example.triprescue.domain.LandmarkClassifier
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.lang.IllegalStateException

class TfLiteLandmarkClassifier(
    private val context: Context,
    private val treshold: Float = 0.5f, //from which score we consider something classified
    private val maxResults: Int=1 //display only one recognised place

    ): LandmarkClassifier {

    private var classifier: ImageClassifier? = null
    private fun setupClassifier(){
        val baseOptions = BaseOptions.builder()
            .setNumThreads(2)
            .build()
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .setScoreThreshold(treshold)
            .build()

        try {
            classifier= ImageClassifier.createFromFileAndOptions(
                context,
                "1.tflite",
                options,

                )

        }catch (e: IllegalStateException){
            e.printStackTrace()
        }
    }


    override fun classifly(bitmap: Bitmap, rotation: Int): List<Classification> {
        if(classifier == null){
            setupClassifier()
        }

        val imageProcessor = org.tensorflow.lite.support.image.ImageProcessor.Builder().build()
        val TensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val ImageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = classifier?.classify(TensorImage, ImageProcessingOptions) //important(magic happens)

        return results?.flatMap {classications ->
            classications.categories.map { category ->
                Classification(
                    name = category.displayName,
                    score = category.score
                )
            }

        }?.distinctBy { it.name } ?: emptyList() //remove duplicates from list
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when(rotation){
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }
}