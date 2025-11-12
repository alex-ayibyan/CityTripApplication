@file:OptIn(ExperimentalPermissionsApi::class)

package edu.ap.citytripapplication

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import edu.ap.citytripapplication.navigation.AppNavigation
import edu.ap.citytripapplication.navigation.Screen
import edu.ap.citytripapplication.ui.theme.CityTripApplicationTheme
import edu.ap.citytripapplication.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSM Configuration
        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        enableEdgeToEdge()
        setContent {
            CityTripApplicationTheme {
                // Use AppNavigation to handle login/logout flow
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController? = null,
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
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
            // TopBar with logout button
            if (authViewModel != null && navController != null) {
                TopAppBar(
                    title = { Text("City Trip") },
                    actions = {
                        IconButton(
                            onClick = {
                                authViewModel.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Uitloggen"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(16.dp)
            ) {
                // Add location button - only show if we have navController and current location
                if (navController != null && currentLocation != null) {
                    SmallFloatingActionButton(
                        onClick = {
                            currentLocation?.let { location ->
                                navController.navigate(
                                    Screen.AddLocation.createRoute(
                                        cityId = "default_city",
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
                            goToMyLocation(fusedLocationClient, mapView) { location ->
                                currentLocation = location
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Mijn locatie"
                    )
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
                onLocationReceived(it)
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                mapView?.controller?.animateTo(geoPoint)
                mapView?.controller?.setZoom(15.0)
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}