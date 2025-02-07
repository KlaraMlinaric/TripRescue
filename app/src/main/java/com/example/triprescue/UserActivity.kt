package com.example.triprescue

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.triprescue.Umap.GeocodingResponse
import com.example.triprescue.Umap.RetrofitInstance
import retrofit2.Call
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import coil.compose.rememberAsyncImagePainter
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.triprescue.ui.theme.TripRescueTheme
import com.example.triprescue.bnb.BotomNavBar
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UserMapScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMapScreen() {
    TripRescueTheme{
        var locationName by remember { mutableStateOf("") }
        val markers = remember { mutableStateListOf<Pair<LatLng, Uri?>>() }
        var selectedMarkerIndex by remember { mutableStateOf<Int?>(null) }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(48.520008, 11.404954), 5f)
        }

        val context = LocalContext.current
        val sharedPreferences = remember { context.getSharedPreferences("markers_prefs", Context.MODE_PRIVATE) }

        // img picker
        val pickImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null && locationName.isNotEmpty()) {
                saveImageIS(context, uri)?.let { savedUri ->
                    searchLocation(context,locationName) { latLng ->
                        if (latLng != null) {
                            markers.add(Pair(latLng, savedUri))
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 10f)
                            saveMarker(sharedPreferences, markers)
                        }
                    }
                }

            }
        }

        // load markers
        LaunchedEffect(Unit) {
            loadMarker(sharedPreferences)?.let {savedMarkers ->
                markers.clear()
                markers.addAll(savedMarkers)
            }
        }
        Scaffold(
            bottomBar = {BotomNavBar() },
            topBar = {
                TopAppBar(
                    title = { Text("Map of Memories")},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            content = { padding ->
                Column(modifier = Modifier.padding(padding)) {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Enter location name") },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp))
                    Button(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {Text("Select Picture and Place Marker")}

                    // googlemap composable
                    GoogleMap(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        cameraPositionState = cameraPositionState
                    ) {
                        markers.forEachIndexed { index, (position) ->
                            Marker(
                                state = MarkerState(position = position),
                                title = "Marker #${index + 1}",
                                snippet = locationName,
                                onClick = {
                                    selectedMarkerIndex = index
                                    true
                                }
                            )
                        }
                    }

                    // when marker selected
                    selectedMarkerIndex?.let { index ->
                        val imageUri = markers[index].second
                        if (imageUri != null) {
                            ImageDialog(imageUri, onDelete = {
                                markers.removeAt(index)
                                saveMarker(sharedPreferences, markers)
                                selectedMarkerIndex = null
                            }, onDismiss = {
                                selectedMarkerIndex = null
                            })
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ImageDialog(imageUri: Uri, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Marker Image") },
        text = {
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )},
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {Text("Close")}
        },
        dismissButton = {
            TextButton(onClick = { onDelete() }) {
                Text("Delete")
            }
        }
    )
}

fun saveImageIS(context: Context, uri: Uri): Uri? {//save image internal storage
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "images/${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs() // creat parent directory if needede
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: IOException) {
        Log.e("SaveImage", "Error saving image: ${e.message}")
        null}

}

fun saveMarker(sharedPreferences: SharedPreferences, markers: List<Pair<LatLng, Uri?>>) {
    val jsonArray = JSONArray()
    markers.forEach { (latLng, uri) ->
        val jsonObject = JSONObject().apply {
            put("lat", latLng.latitude)
            put("lng", latLng.longitude)
            put("uri", uri?.toString())
        }
        jsonArray.put(jsonObject)
    }
    sharedPreferences.edit().putString("markers", jsonArray.toString()).apply()}

fun loadMarker(sharedPreferences: SharedPreferences): List<Pair<LatLng, Uri?>>? {
    val jsonString = sharedPreferences.getString("markers", null) ?: return null
    return try {
        val jsonArray = JSONArray(jsonString)
        val markers = mutableListOf<Pair<LatLng, Uri?>>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val lat = jsonObject.getDouble("lat")
            val lng = jsonObject.getDouble("lng")
            val uriString = jsonObject.optString("uri")
            val uri = if (uriString.isNotEmpty()) Uri.parse(uriString) else null
            markers.add(Pair(LatLng(lat, lng), uri))
        }
        markers
    } catch (e: JSONException) {
        Log.e("LoadMarker", "Error parsing markers: ${e.message}")
        null
    }
}

fun searchLocation(context: Context, locationName: String, callback: (LatLng?) -> Unit) {
    val apiKey = context.getString(R.string.apiKey)
    val call = RetrofitInstance.geocodingService.geocodeLocation(locationName, apiKey)
    call.enqueue(object : retrofit2.Callback<GeocodingResponse>{
        override fun onResponse(call: Call<GeocodingResponse>, response: retrofit2.Response<GeocodingResponse>) {
            if (response.isSuccessful) {
                val results = response.body()?.results
                if (results != null && results.isNotEmpty()) {
                    val location = results[0].geometry.location
                    callback(LatLng(location.lat, location.lng))
                } else {
                    callback(null)
                }
            } else {
                Log.e("Geocoding", "Failed to get geocoding data")
                callback(null)
            }
        }

        override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
            Log.e("Geocoding", "Error: ${t.message}")
            callback(null)
        }
    })
}
