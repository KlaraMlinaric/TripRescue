package com.example.triprescue

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.triprescue.Presentation.CameraPreview
import com.example.triprescue.Presentation.LandmarkImageAnalyzer
import com.example.triprescue.data.TfLiteLandmarkClassifier
import com.example.triprescue.domain.Classification
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.lifecycle.lifecycleScope
import com.example.triprescue.ui.theme.TripRescueTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.triprescue.bnb.BotomNavBar


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.CAMERA), 0
            )
        }
        setContent {
            TripRescueTheme() {
                var classifications by remember { mutableStateOf(emptyList<Classification>()) }
                var selectedPlace by remember { mutableStateOf<Classification?>(null) }
                var placeInfo by remember { mutableStateOf("") }
                var isInfoDialogVisible by remember { mutableStateOf(false) }

                val analyzer = remember {
                    LandmarkImageAnalyzer(
                        classifier = TfLiteLandmarkClassifier(
                            context = applicationContext,
                        ),
                        onResults = {
                            classifications = it
                        }
                    )
                }
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(applicationContext),
                            analyzer
                        )
                    }
                }

                // fetch infos from Wikipedia
                fun fetchPlaceInfo(placeName: String) {
                    lifecycleScope.launch {
                        val info = getInfoWikipedia(placeName)
                        placeInfo = info ?: "No information available."
                        isInfoDialogVisible = true
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(controller, Modifier.fillMaxSize())

                    // display clasification
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        classifications.forEach { classification ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {

                                Text(
                                    text = classification.name,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Button(onClick = {selectedPlace = classification
                                    fetchPlaceInfo(classification.name)}){
                                    Text("Info")
                                }
                            }
                        }
                    }

                    if (isInfoDialogVisible && selectedPlace != null) {
                        AlertDialog(
                            onDismissRequest = { isInfoDialogVisible = false },
                            title = {
                                Text(
                                    text = "Information about ${selectedPlace?.name}",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            },
                            text = { Text(placeInfo) },
                            confirmButton = {
                                Button(onClick = { isInfoDialogVisible = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        BotomNavBar()
                    }
                }
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED


    private suspend fun getInfoWikipedia(placeName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://en.wikipedia.org/api/rest_v1/page/summary/${placeName.replace(" ", "_")}"
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = org.json.JSONObject(responseBody)
                        val extract = json.optString("extract")
                        extract //return text
                    } else {
                        null}
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}
