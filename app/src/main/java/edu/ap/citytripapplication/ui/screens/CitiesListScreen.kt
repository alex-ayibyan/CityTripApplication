package edu.ap.citytripapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ap.citytripapplication.model.City
import edu.ap.citytripapplication.viewmodel.CitiesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCityDetails: (String) -> Unit,
    onAddCity: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToChats: () -> Unit
) {
    val viewModel: CitiesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Load cache stats on first composition
    LaunchedEffect(Unit) {
        viewModel.loadCacheStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Steden")
                        if (uiState.lastSyncTime != null) {
                            Text(
                                text = "Laatst gesynchroniseerd: ${formatSyncTime(uiState.lastSyncTime!!)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Uitloggen",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refreshCities() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ververs data",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Chats button
                    IconButton(
                        onClick = onNavigateToChats,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Berichten",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Map button
                    IconButton(
                        onClick = onNavigateToMap,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Kaart weergeven",
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
            FloatingActionButton(
                onClick = onAddCity,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Stad toevoegen")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cache status banner
                // if (uiState.cacheStats != null && !uiState.cacheStats!!.isEmpty) {
                //     CacheStatusBanner(
                //         citiesCount = uiState.cacheStats!!.citiesCount,
                //         locationsCount = uiState.cacheStats!!.locationsCount,
                //         reviewsCount = uiState.cacheStats!!.reviewsCount,
                //         isRefreshing = uiState.isRefreshing
                //     )
                // }

                // Error message
                if (uiState.error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Sluit",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Content
                if (uiState.isLoading && uiState.cities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Data laden...")
                        }
                    }
                } else if (uiState.cities.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nog geen steden",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Klik op het + icoon om je eerste stad toe te voegen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToMap,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bekijk kaart")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.cities) { city ->
                            CityCard(
                                city = city,
                                onClick = { onNavigateToCityDetails(city.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// @Composable
// fun CacheStatusBanner(
//     citiesCount: Int,
//     locationsCount: Int,
//     reviewsCount: Int,
//     isRefreshing: Boolean
// ) {
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .padding(horizontal = 16.dp, vertical = 8.dp),
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
//         )
//     ) {
//         Row(
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .padding(12.dp),
//             horizontalArrangement = Arrangement.SpaceBetween,
//             verticalAlignment = Alignment.CenterVertically
//         ) {
//             Row(
//                 horizontalArrangement = Arrangement.spacedBy(4.dp),
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 Icon(
//                     imageVector = if (isRefreshing) Icons.Default.CloudSync else Icons.Default.Storage,
//                     contentDescription = null,
//                     modifier = Modifier.size(16.dp),
//                     tint = MaterialTheme.colorScheme.onSecondaryContainer
//                 )
//                 Text(
//                     text = if (isRefreshing) "Synchroniseren..." else "Offline beschikbaar:",
//                     style = MaterialTheme.typography.labelSmall,
//                     color = MaterialTheme.colorScheme.onSecondaryContainer
//                 )
//             }
            
//             if (!isRefreshing) {
//                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                     CacheStatChip(
//                         icon = Icons.Default.LocationCity,
//                         count = citiesCount,
//                         label = "steden"
//                     )
//                     CacheStatChip(
//                         icon = Icons.Default.LocationOn,
//                         count = locationsCount,
//                         label = "locaties"
//                     )
//                     CacheStatChip(
//                         icon = Icons.Default.Star,
//                         count = reviewsCount,
//                         label = "reviews"
//                     )
//                 }
//             }
//         }
//     }
// }

@Composable
fun CacheStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 60 -> "zojuist"
        minutes < 60 -> "$minutes min geleden"
        hours < 24 -> "$hours uur geleden"
        else -> SimpleDateFormat("dd MMM HH:mm", Locale("nl")).format(Date(timestamp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityCard(city: City, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = city.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = city.country,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (city.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = city.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}






