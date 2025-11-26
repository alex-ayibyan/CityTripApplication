package edu.ap.citytripapplication.ui.screens

import LocationProvider
import LocationService
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import edu.ap.citytripapplication.model.Location
import edu.ap.citytripapplication.model.LocationCategory
import edu.ap.citytripapplication.ui.components.AddReviewDialog
import edu.ap.citytripapplication.ui.components.RatingDisplay
import edu.ap.citytripapplication.ui.components.ReviewsList
import edu.ap.citytripapplication.viewmodel.CityDetailsViewModel
import edu.ap.citytripapplication.viewmodel.LocationViewModel
import edu.ap.citytripapplication.viewmodel.ReviewViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDetailsScreen(
    cityId: String,
    locationService: LocationService = LocationProvider.getService(),
    onNavigateBack: () -> Unit,
    onAddLocation: (String) -> Unit
) {
    val cityDetailsViewModel: CityDetailsViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    val reviewViewModel: ReviewViewModel = viewModel()
    
    val city by cityDetailsViewModel.city.collectAsState()
    val filteredLocations by cityDetailsViewModel.filteredLocations.collectAsState()
    val selectedCategories by cityDetailsViewModel.selectedCategories.collectAsState()
    val isLoading by cityDetailsViewModel.isLoading.collectAsState()
    
    val reviewState by reviewViewModel.reviewState.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showLocationModeDialog by remember { mutableStateOf(false) }
    var selectedLocationForReview by remember { mutableStateOf<Location?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showReviewsForLocation by remember { mutableStateOf<Location?>(null) }

    // Get current location from service
    val currentLocation by remember(locationService) {
        derivedStateOf { locationService.getCurrentLocation() }
    }

    // Track if we're using real location
    val isUsingRealLocation by remember(locationService) {
        derivedStateOf { locationService.isUsingRealLocation() }
    }

    LaunchedEffect(cityId) {
        cityDetailsViewModel.loadCityData(cityId)
    }

    // Refresh locations when a new one is added
    val locationState by locationViewModel.locationState.collectAsState()
    LaunchedEffect(locationState.savedSuccessfully) {
        if (locationState.savedSuccessfully) {
            cityDetailsViewModel.refreshData(cityId)
        }
    }

    // Refresh after review is added
    LaunchedEffect(reviewState.savedSuccessfully) {
        if (reviewState.savedSuccessfully) {
            cityDetailsViewModel.refreshData(cityId)
            reviewViewModel.resetSavedState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(city?.name ?: "Laden...")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Terug"
                        )
                    }
                },
                actions = {
                    // Location mode indicator and switcher
                    IconButton(onClick = { showLocationModeDialog = true }) {
                        Icon(
                            imageVector = if (isUsingRealLocation) {
                                Icons.Default.LocationOn
                            } else {
                                Icons.Default.LocationOff
                            },
                            contentDescription = if (isUsingRealLocation) {
                                "Echte locatie ingeschakeld"
                            } else {
                                "Testlocatie ingeschakeld"
                            },
                            tint = if (isUsingRealLocation) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }

                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedCategories.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onAddLocation(cityId)
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Locatie toevoegen")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Location mode banner
                    item {
                        LocationModeBanner(
                            isUsingRealLocation = isUsingRealLocation,
                            currentLocation = currentLocation,
                            onSwitchMode = { showLocationModeDialog = true }
                        )
                    }

                    // Active filters
                    if (selectedCategories.isNotEmpty()) {
                        item {
                            ActiveFiltersRow(
                                selectedCategories = selectedCategories,
                                onClearFilters = cityDetailsViewModel::clearFilters
                            )
                        }
                    }

                    // Empty state
                    if (filteredLocations.isEmpty()) {
                        item {
                            Text(
                                text = if (selectedCategories.isEmpty()) {
                                    "Nog geen locaties toegevoegd\n\nKlik op het + icoon om je eerste locatie toe te voegen!"
                                } else {
                                    "Geen locaties gevonden voor geselecteerde filters"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Location cards
                        items(filteredLocations) { location ->
                            LocationCard(
                                location = location,
                                currentLocation = currentLocation,
                                onAddReview = {
                                    selectedLocationForReview = location
                                    showReviewDialog = true
                                },
                                onShowReviews = {
                                    showReviewsForLocation = location
                                    reviewViewModel.loadReviews(location.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            selectedCategories = selectedCategories,
            onCategoryToggle = cityDetailsViewModel::toggleCategoryFilter,
            onClearFilters = cityDetailsViewModel::clearFilters,
            onDismiss = { showFilterDialog = false }
        )
    }

    // Location Mode Dialog
    if (showLocationModeDialog) {
        LocationModeDialog(
            isUsingRealLocation = isUsingRealLocation,
            onUseRealLocation = {
                locationService.setUseRealLocation(true)
                showLocationModeDialog = false
            },
            onUseDummyLocation = {
                locationService.setUseRealLocation(false)
                showLocationModeDialog = false
            },
            onDismiss = { showLocationModeDialog = false }
        )
    }

    // Add Review Dialog
    if (showReviewDialog && selectedLocationForReview != null) {
        AddReviewDialog(
            locationName = selectedLocationForReview!!.name,
            onDismiss = {
                if (!reviewState.isSaving) {
                    showReviewDialog = false
                    selectedLocationForReview = null
                }
            },
            onSubmit = { rating, comment ->
                reviewViewModel.addReview(
                    locationId = selectedLocationForReview!!.id,
                    rating = rating,
                    comment = comment,
                    onSuccess = {
                        showReviewDialog = false
                        selectedLocationForReview = null
                    }
                )
            },
            isSaving = reviewState.isSaving
        )
    }

    // Reviews List Bottom Sheet
    if (showReviewsForLocation != null) {
        ReviewsBottomSheet(
            location = showReviewsForLocation!!,
            reviews = reviewState.reviews,
            isLoading = reviewState.isLoading,
            onDismiss = { showReviewsForLocation = null },
            onDeleteReview = { reviewId ->
                reviewViewModel.deleteReview(reviewId, showReviewsForLocation!!.id)
            }
        )
    }

    // Show error if any
    reviewState.error?.let { error ->
        LaunchedEffect(error) {
            // You could show a Snackbar here
            println("Review error: $error")
        }
    }
}

@Composable
fun LocationCard(
    location: Location,
    currentLocation: android.location.Location? = null,
    onAddReview: () -> Unit,
    onShowReviews: () -> Unit
) {
    // Calculate distance if we have current location
    val distance = remember(location, currentLocation) {
        calculateDistance(location, currentLocation)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Show image if available
            if (location.imageUrl.isNotBlank()) {
            Base64Image(
                base64String = location.imageUrl,
                contentDescription = "Foto van ${location.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            }  else {
                // Placeholder when no image is available
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Geen foto beschikbaar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CategoryChip(category = location.category)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (location.description.isNotBlank()) {
                    Text(
                        text = location.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Rating display
                if (location.totalRatings > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RatingDisplay(
                                rating = location.averageRating.toInt(),
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${"%.1f".format(location.averageRating)} (${location.totalRatings})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = onShowReviews) {
                            Text("Bekijk reviews")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Distance row - show if we have current location
                if (currentLocation != null && distance != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Afstand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDistance(distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coordinates
                    Text(
                        text = "📍 ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add review button
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddReview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review toevoegen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsBottomSheet(
    location: Location,
    reviews: List<edu.ap.citytripapplication.model.Review>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onDeleteReview: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(true) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                onDismiss()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (location.totalRatings > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingDisplay(
                                    rating = location.averageRating.toInt(),
                                    size = 20.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${"%.1f".format(location.averageRating)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Reviews list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    ReviewsList(
                        reviews = reviews,
                        onDeleteReview = onDeleteReview
                    )
                }
            }
        }
    }
}

@Composable
fun LocationModeBanner(
    isUsingRealLocation: Boolean,
    currentLocation: android.location.Location?,
    onSwitchMode: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUsingRealLocation) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = onSwitchMode
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUsingRealLocation) {
                    Icons.Default.LocationOn
                } else {
                    Icons.Default.LocationOff
                },
                contentDescription = null,
                tint = if (isUsingRealLocation) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isUsingRealLocation) {
                        "Echte locatie ingeschakeld"
                    } else {
                        "Testlocatie ingeschakeld"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isUsingRealLocation) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                if (currentLocation != null) {
                    Text(
                        text = "📍 ${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUsingRealLocation) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Wijzig",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUsingRealLocation) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LocationModeDialog(
    isUsingRealLocation: Boolean,
    onUseRealLocation: () -> Unit,
    onUseDummyLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Locatiemodus") },
        text = {
            Column {
                Text("Kies welke locatiegegevens je wilt gebruiken:")
                Spacer(modifier = Modifier.height(16.dp))

                // Real location option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUsingRealLocation) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    onClick = onUseRealLocation
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isUsingRealLocation) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Echte locatie",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Gebruik je werkelijke GPS-locatie",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Dummy location option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isUsingRealLocation) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    onClick = onUseDummyLocation
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = if (!isUsingRealLocation) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Testlocatie",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Gebruik vaste locatie voor testen",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Sluiten")
            }
        }
    )
}

private fun calculateDistance(
    location: Location,
    currentLocation: android.location.Location?
): Float? {
    if (currentLocation == null) return null

    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        currentLocation.latitude,
        currentLocation.longitude,
        location.latitude,
        location.longitude,
        results
    )

    return results[0]
}

private fun formatDistance(distanceMeters: Float): String {
    return when {
        distanceMeters < 1000 -> "${distanceMeters.toInt()} m"
        distanceMeters < 10000 -> "${"%.1f".format(distanceMeters / 1000)} km"
        else -> "${(distanceMeters / 1000).toInt()} km"
    }
}

@Composable
fun ActiveFiltersRow(
    selectedCategories: Set<LocationCategory>,
    onClearFilters: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filters: ${selectedCategories.joinToString { it.displayName }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onClearFilters,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Wis")
        }
    }
}

@Composable
fun CategoryChip(category: LocationCategory) {
    val (text, color) = when (category) {
        LocationCategory.RESTAURANT -> "🍴 Restaurant" to MaterialTheme.colorScheme.primary
        LocationCategory.HOTEL -> "🏨 Hotel" to MaterialTheme.colorScheme.secondary
        LocationCategory.ATTRACTION -> "🏛️ Attractie" to MaterialTheme.colorScheme.tertiary
        LocationCategory.SHOPPING -> "🛍️ Winkelen" to MaterialTheme.colorScheme.error
        LocationCategory.NIGHTLIFE -> "🌙 Nachtleven" to MaterialTheme.colorScheme.primary
        LocationCategory.OTHER -> "📌 Anders" to MaterialTheme.colorScheme.onSurfaceVariant
        LocationCategory.MUSEUM -> "Museum" to MaterialTheme.colorScheme.primary
        LocationCategory.PARK -> "Park" to MaterialTheme.colorScheme.primary
        LocationCategory.MONUMENT -> "Monument" to MaterialTheme.colorScheme.primary
        LocationCategory.ENTERTAINMENT -> "Entertainment" to MaterialTheme.colorScheme.primary
        LocationCategory.CAFE -> "Café" to MaterialTheme.colorScheme.primary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    selectedCategories: Set<LocationCategory>,
    onCategoryToggle: (LocationCategory) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter op categorie") },
        text = {
            Column {
                LocationCategory.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategories.contains(category),
                        onClick = { onCategoryToggle(category) },
                        label = {
                            Text(category.displayName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onClearFilters) {
                    Text("Filters wissen")
                }
                Button(onClick = onDismiss) {
                    Text("Toepassen")
                }
            }
        }
    )
}

@Composable
fun Base64Image(
    base64String: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (base64String.isBlank()) {
        // Show placeholder if no image
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Geen foto",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
        return
    }

    val bitmap = remember(base64String) {
        try {
            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            println("ERROR: Failed to decode base64 image: ${e.message}")
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        // Show error placeholder
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Afbeelding laden mislukt",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}





