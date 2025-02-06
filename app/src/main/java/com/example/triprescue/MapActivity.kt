package com.example.triprescue

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.triprescue.bnb.BotomNavBar
import com.example.triprescue.ui.theme.TripRescueTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapActivity : ComponentActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private val isMapReady = mutableStateOf(false)
    private val startLocationState = mutableStateOf<LatLng?>(null)
    private val mapViewBundleKey = "MapViewBundleKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = "AIzaSyBiR60oUSZdiY7UFQHga0T1qm41zQLQtEA"
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
            Log.d("MapActivity", "Places SDK initialized.")
        }

        requestLocationPermissions()

        val mapViewBundle: Bundle? = savedInstanceState?.getBundle(mapViewBundleKey)

        mapView = MapView(this)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        setContent {
            TripRescueTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MapScreen(
                        mapView = mapView,
                        googleMap = googleMap,
                        isMapReady = isMapReady.value,
                        apiKey = apiKey,
                        startLocationState = startLocationState.value
                    )
                }
            }

        }
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            if (permissionsResult.values.all { it }) {
                Log.d("MapActivity", "Location permissions granted.")
            } else {
                Log.e("MapActivity", "Location permissions denied.")
            }
        }

        if (permissions.any {
                ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            permissionLauncher.launch(permissions)
        } else {
            Log.d("MapActivity", "Location permissions already granted.")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        isMapReady.value = true
        //Log.d("MapActivity", "Map is ready: ${isMapReady.value}")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,

                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {

                    startLocationState.value = LatLng(location.latitude, location.longitude)
                    Log.d("MapActivity", "Start location set: ${startLocationState.value}")
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocationState.value!!, 12f))
                }
            }
        } else {
            Log.e("MapActivity", "Location permissions are not granted for My Location layer.")
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val mapViewBundle = outState.getBundle(mapViewBundleKey) ?: Bundle()
        outState.putBundle(mapViewBundleKey, mapViewBundle)
        mapView.onSaveInstanceState(mapViewBundle)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()}

    override fun onLowMemory() { //reduce memory ussage
        mapView.onLowMemory()
        super.onLowMemory()
    }
}

@Composable
fun MapScreen(
    mapView: MapView,
    googleMap: GoogleMap?,
    isMapReady: Boolean,
    apiKey: String,
    startLocationState: LatLng?)  {
    val context = LocalContext.current
    val placesClient = Places.createClient(context)
    var endLocation by remember { mutableStateOf<LatLng?>(null) }
    var directions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDirections by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom =56.dp)
        ) {

            AutoCompleteTextField(
                label ="End Location",
                placesClient = placesClient,
                inputTextColor = MaterialTheme.colorScheme.onBackground,
                onLocationSelected = {location ->
                    endLocation = location
                    googleMap?.addMarker(MarkerOptions().position(location).title("End Location"))
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f)) }
            )
            Spacer(modifier = Modifier.height(16.dp))


            Button(
                onClick = {
                    if (isMapReady && googleMap != null && startLocationState != null && endLocation != null) {
                        coroutineScope.launch {
                            delay(500)

                            directions = getDirections(
                                startLocationState,
                                endLocation!!,
                                googleMap,
                                apiKey)
                            if (directions.isNotEmpty()) {
                                Toast.makeText(context, "Directions fetched", Toast.LENGTH_SHORT).show()
                                val boundsBuilder = LatLngBounds.builder()
                                    .include(startLocationState)
                                    .include(endLocation!!)
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                                showDirections = true
                            }else {
                                Toast.makeText(context, "No directions found", Toast.LENGTH_SHORT).show()
                                showDirections = false
                            }
                        }
                    } else{
                        Toast.makeText(context, "Map not ready or locations missing", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {Text("Show on Map") }

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),)

            Spacer(modifier = Modifier.height(16.dp))

            // expand menue directions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .padding(16.dp)) {
                Column {
                    Text(
                        text = "Step-by-Step Directions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    //show directions if expanded
                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (showDirections && directions.isNotEmpty()) {
                            //scrollable
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                items(directions){step ->
                                    Text(
                                        text = step,
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No Directions Available",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

            }
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

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    label: String,
    placesClient: PlacesClient,
    onLocationSelected: (LatLng) -> Unit,
    inputTextColor: Color = Color.Black //def text color
) {
    var text by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(text) {
        if (text.isNotEmpty()) {
            suggestions = getPredictions(text, placesClient)
        } else { suggestions = emptyList()} //clear suggestion if input empty
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { newText -> text = newText },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = inputTextColor)
        )

        if (suggestions.isNotEmpty() && text.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp))
            {
                items(suggestions) { prediction ->
                    val fullText = prediction.getPrimaryText(null).toString()
                    val locationWithCountry = prediction.getSecondaryText(null)?.toString()

                    Text(
                        text = if (locationWithCountry.isNullOrEmpty()) fullText else "$fullText, $locationWithCountry",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                suggestions = emptyList()
                                coroutineScope.launch {
                                    val location = locationFromPrediction(prediction, placesClient)
                                    location?.let {
                                        onLocationSelected(it)
                                        text = if (locationWithCountry.isNullOrEmpty()) fullText else "$fullText, $locationWithCountry"
                                    }
                                }
                            }
                            .padding(8.dp)

                    )
                }
            }
        }
    }
}

suspend fun getPredictions(
    query: String,
    placesClient: PlacesClient
): List<AutocompletePrediction> = withContext(Dispatchers.IO) {
    if (query.isEmpty()) return@withContext emptyList()
    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(query)
        .build()

    try {
        val response = placesClient.findAutocompletePredictions(request).await()
        response.autocompletePredictions
    } catch (e: Exception) {
        Log.e("AutoCompleteTextField", "Error fetching autocomplete predictions", e)
        emptyList()
    }
}

private suspend fun locationFromPrediction(
    prediction: AutocompletePrediction,
    placesClient: PlacesClient
): LatLng? = withContext(Dispatchers.IO) {
    val placeId = prediction.placeId
    val placeFields = listOf(Place.Field.LAT_LNG)
    val request = FetchPlaceRequest.builder(placeId, placeFields).build()


    try {
        val response = placesClient.fetchPlace(request).await()
        response.place.latLng
    } catch (e: Exception) {
        //Log.e("AutoCompleteTextField", "Error fetching place details", e)
        null}
}

suspend fun getDirections(
    startLocation: LatLng,
    endLocation: LatLng,
    googleMap: GoogleMap,
    apiKey: String
): List<String> = withContext(Dispatchers.IO) {
    Log.d("MapScreen", "Fetching directions from $startLocation to $endLocation")
    val url = "https://maps.googleapis.com/maps/api/directions/json?" +
            "origin=${startLocation.latitude},${startLocation.longitude}" +
            "&destination=${endLocation.latitude},${endLocation.longitude}" +
            "&key=$apiKey"

    try {
        Log.d("MapScreen", "Request URL: $url")
        val result = URL(url).readText()
        Log.d("MapScreen", "Directions API response: $result")

        val jsonResponse = JSONObject(result)
        val status = jsonResponse.getString("status")
        if (status != "OK") {
            Log.e("MapScreen", "Directions API error: $status")
            return@withContext emptyList<String>()}

        val routes = jsonResponse.getJSONArray("routes")
        if (routes.length() == 0) {
            Log.e("MapScreen", "No routes found in the API response")
            return@withContext emptyList()}

        val steps = mutableListOf<String>()
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        val leg = legs.getJSONObject(0)
        val stepsArray = leg.getJSONArray("steps")

        val polylineOptions = PolylineOptions().color(0xFF3A6EA5.toInt()).width(10f)

        for (i in 0 until stepsArray.length()) {
            val step = stepsArray.getJSONObject(i)
            val instruction = step.getString("html_instructions").replace(Regex("<[^>]*>"), "")
            steps.add(instruction)

            val points = step.getJSONObject("polyline").getString("points")
            val decodedPoints = PolyUtil.decode(points)
            polylineOptions.addAll(decodedPoints)
        }

        withContext(Dispatchers.Main) {
            googleMap.addPolyline(polylineOptions)
            Log.d("MapScreen", "Polyline added to the map.")
        }

        Log.d("MapScreen", "Steps: $steps")
        steps
    } catch (e: Exception) {
        Log.e("MapScreen", "Error fetching directions", e)
        emptyList()
    }
}


suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
}