@file:OptIn(ExperimentalPermissionsApi::class)

package edu.ap.citytripapplication

import edu.ap.citytripapplication.navigation.AppNavigation
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import edu.ap.citytripapplication.ui.theme.CityTripApplicationTheme
import edu.ap.citytripapplication.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import edu.ap.citytripapplication.navigation.Screen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import edu.ap.citytripapplication.database.CityTripDatabase
import edu.ap.citytripapplication.repository.CityTripRepository
import edu.ap.citytripapplication.viewmodel.CitiesViewModel
import edu.ap.citytripapplication.model.Location as AppLocation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocationProvider.initialize(applicationContext)

        // OSM Configuration
        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        enableEdgeToEdge()
        setContent {
            CityTripApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: androidx.navigation.NavController? = null,
    onNavigateBack: () -> Unit = {},
    onLocationUpdate: (android.location.Location) -> Unit = { _ -> } // Change to accept Location object
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // ViewModels / data
    val citiesViewModel: CitiesViewModel = viewModel()
    val citiesState by citiesViewModel.uiState.collectAsState()

    // Repository to access cached locations
    val application = LocalContext.current.applicationContext as android.app.Application
    val database = CityTripDatabase.getDatabase(application)
    val repository = CityTripRepository(
        cityDao = database.cityDao(),
        locationDao = database.locationDao(),
        reviewDao = database.reviewDao()
    )

    val locationsState = remember { mutableStateListOf<AppLocation>() }
    val scope = rememberCoroutineScope()

    // Collect locations from local cache
    LaunchedEffect(Unit) {
        repository.getAllLocationsFlow().collect { list ->
            locationsState.clear()
            locationsState.addAll(list)
        }
    }

    // MapView state
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("City Trip - Kaart") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Terug naar steden",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(16.dp)
            ) {
                // Add location button - only show if we have navController and current location
                if (navController != null && currentLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            currentLocation?.let { location ->
                                navController.navigate(
                                    Screen.AddLocation.createRoute(
                                        cityId = "default_city", // You might want to get this from a selected city
                                        latitude = location.latitude,
                                        longitude = location.longitude
                                    )
                                )
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Locatie toevoegen"
                        )
                    }
                }

                // My location button
                FloatingActionButton(
                    onClick = {
                        if (locationPermissions.allPermissionsGranted) {
                            goToMyLocation(
                                fusedLocationClient = fusedLocationClient,
                                mapView = mapView,
                                onLocationReceived = { location ->
                                    currentLocation = location
                                    onLocationUpdate(location) // Pass the location object
                                }
                            )
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Mijn locatie")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // OSM MapView
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        // Standard center on Antwerp
                        controller.setCenter(GeoPoint(51.2194, 4.4025))

                        mapView = this

                        if (locationPermissions.allPermissionsGranted) {
                            val overlay = MyLocationNewOverlay(
                                GpsMyLocationProvider(ctx),
                                this
                            )
                            overlay.enableMyLocation()
                            overlays.add(overlay)
                            locationOverlay = overlay
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    if (locationPermissions.allPermissionsGranted && locationOverlay == null) {
                        val overlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(context),
                            view
                        )
                        overlay.enableMyLocation()
                        view.overlays.add(overlay)
                        locationOverlay = overlay
                    }

                    // Remove existing city/location markers to avoid duplicates
                    val toRemove = view.overlays.filterIsInstance<Marker>()
                    toRemove.forEach { view.overlays.remove(it) }

                    // Add city markers
                    citiesState.cities.forEach { city ->
                        try {
                            val marker = Marker(view)
                            marker.position = GeoPoint(city.latitude, city.longitude)
                            marker.title = city.name
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.infoWindow = null
                            marker.setOnMarkerClickListener { m, mv ->
                                // navigate to city details
                                navController?.navigate(
                                    edu.ap.citytripapplication.navigation.Screen.CityDetails.createRoute(city.id)
                                )
                                true
                            }
                            view.overlays.add(marker)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Add location markers (different small marker)
                    locationsState.forEach { loc ->
                        try {
                            val marker = Marker(view)
                            marker.position = GeoPoint(loc.latitude, loc.longitude)
                            marker.title = loc.name
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.infoWindow = null
                            marker.setOnMarkerClickListener { m, mv ->
                                // navigate to city details or location details: if location has cityId, open city, else fallback
                                if (loc.cityId.isNotBlank()) {
                                    navController?.navigate(
                                        edu.ap.citytripapplication.navigation.Screen.CityDetails.createRoute(loc.cityId)
                                    )
                                } else {
                                    // no dedicated location details route exists; navigate to city list as fallback
                                    navController?.navigate(edu.ap.citytripapplication.navigation.Screen.CitiesList.route)
                                }
                                true
                            }
                            view.overlays.add(marker)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}

private fun goToMyLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    mapView: MapView?,
    onLocationReceived: (android.location.Location) -> Unit = {}
) {
    try {
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            location?.let {
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                mapView?.controller?.animateTo(geoPoint)
                mapView?.controller?.setZoom(15.0)
                onLocationReceived(it)
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}