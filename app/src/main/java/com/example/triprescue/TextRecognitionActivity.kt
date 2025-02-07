package com.example.triprescue

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import com.example.triprescue.bnb.BotomNavBar
import com.example.triprescue.ui.theme.TripRescueTheme

import java.io.File
import java.util.concurrent.Executors

class TextRecognitionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextRecognitionApp()
        }
    }
}

@Composable
fun TextRecognitionApp() {
    TripRescueTheme {
        var imageFile by remember { mutableStateOf<File?>(null)}
        var recognizedText by remember { mutableStateOf("")}
        var cameraPermissionGranted by remember { mutableStateOf(false)}
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val imageCapture = remember { ImageCapture.Builder().build()}
        val cameraExecutor = remember { Executors.newSingleThreadExecutor()}

        val cameraProviderFuture = remember {ProcessCameraProvider.getInstance(context)}

        var cameraProvider by remember {mutableStateOf<ProcessCameraProvider?>(null) }
        var cameraError by remember { mutableStateOf<String?>(null)}

        LaunchedEffect(cameraProviderFuture) {
            cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraX", "Failed to get CameraProvider: ${e.message}")
                cameraError = "Failed to initialize CameraX: ${e.message}"
                null}

        }

        if (cameraError != null) {
            Text(
                text = cameraError ?: "Unknown error occurred.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            return@TripRescueTheme
        }

        RequestCameraPermission { isGranted ->
            if (isGranted) {
                cameraPermissionGranted = true
            } else {
                Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_LONG).show()
            }
        }

        Scaffold(
            bottomBar = { BotomNavBar() }
        ) { innerPadding ->
            if (cameraPermissionGranted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Spacer(modifier = Modifier.height(50.dp)) //top padding

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 150.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(factory = { ctx ->
                            val previewView = androidx.camera.view.PreviewView(ctx)

                            cameraProvider?.let { provider ->
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider) }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture)
                                } catch (e: Exception) {
                                    Log.e("CameraX", "Binding failed: ${e.message}")
                                    cameraError = "Binding failed: ${e.message}"
                                }
                            }


                            previewView
                        })
                    }

                    Button(
                        onClick = {
                            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        imageFile = file
                                        Toast.makeText(context, "Image captured successfully", Toast.LENGTH_SHORT).show()

                                        val uri = Uri.fromFile(file)
                                        processTextFromImage(context, uri) { text ->
                                            recognizedText = text
                                            translateTextToEnglish(context,text) { translatedText ->
                                                recognizedText = translatedText //update with translated text
                                                Toast.makeText(context, "Translated: $translatedText", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CameraX", "Image capture failed: ${exception.message}", exception)
                                        Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 200.dp)//button top padin from top screen
                    ) {
                        Text(
                            text = "Capture Image",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    //scrolable translation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 50.dp)
                    ) {
                        Column {
                            imageFile?.let { file ->
                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription ="Captured Image",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            if (recognizedText.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .padding(16.dp)) {
                                    Text(
                                        text = recognizedText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}


fun processTextFromImage(
    context: android.content.Context,
    uri: Uri,
    onTextRecognized: (String) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->onTextRecognized(result.text) }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Error recognizing text: ${e.message}")
                onTextRecognized("Error: ${e.message}")}
    } catch (e: Exception) {
        Log.e("TextRecognition", "Error loading image: ${e.message}")
        onTextRecognized("Error: ${e.message}")}

}

@Composable
fun RequestCameraPermission(onPermissionResult: (Boolean) -> Unit) {
    LocalContext.current
    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            onPermissionResult(isGranted)
        }
    )
    LaunchedEffect(Unit) {
        cameraPermission.launch(android.Manifest.permission.CAMERA)
    }
}


fun translateTextToEnglish(context: Context, text: String, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val apiKey = context.getString(R.string.apiKey)
            val url = "https://translation.googleapis.com/language/translate/v2"
            //create JSON payload
            val jsonPayload = """
                {
                  "q": "$text",
                  "target": "en"
                }
            """.trimIndent()

            //build HTTP request
            val client = OkHttpClient()
            val requestBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                jsonPayload
            )
            val request = Request.Builder()
                .url("$url?key=$apiKey")
                .post(requestBody)
                .build()

            //execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val translatedText = JSONObject(responseBody)
                    .getJSONObject("data")
                    .getJSONArray("translations")
                    .getJSONObject(0)
                    .getString("translatedText")

                withContext(Dispatchers.Main) {
                    onResult(translatedText) //return transl. text
                }
            } else {
                Log.e("TranslationError", "API Error: ${response.code}")
                withContext(Dispatchers.Main) {
                    onResult("Translation failed. Error: ${response.code}")
                }

            }
        } catch (e: Exception) {
            Log.e("TranslationError", "Translation failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onResult("Translation failed: ${e.message}")
            }
        }
    }
}
