package edu.ap.citytripapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.database.CityTripDatabase
import edu.ap.citytripapplication.model.City
import edu.ap.citytripapplication.repository.CacheStats
import edu.ap.citytripapplication.repository.CityTripRepository
import edu.ap.citytripapplication.repository.SyncStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class CitiesUiState(
    val cities: List<City> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long? = null,
    val cacheStats: CacheStats? = null
)

class CitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Room database and repository
    private val database = CityTripDatabase.getDatabase(application)
    private val repository = CityTripRepository(
        cityDao = database.cityDao(),
        locationDao = database.locationDao(),
        reviewDao = database.reviewDao()
    )

    private val _uiState = MutableStateFlow(CitiesUiState())
    val uiState: StateFlow<CitiesUiState> = _uiState.asStateFlow()

    init {
        // Load cached data immediately
        loadCachedCities()
        // Then sync with Firebase in background
        syncWithFirebase()
    }

    /**
     * Load cities from local cache (instant)
     */
    private fun loadCachedCities() {
        viewModelScope.launch {
            println("CITIES: Loading from cache...")
            repository.getAllCitiesFlow().collect { cities ->
                _uiState.value = _uiState.value.copy(
                    cities = cities,
                    isLoading = false
                )
                println("CITIES: Loaded ${cities.size} cities from cache")
            }
        }
    }

    /**
     * Sync with Firebase (background)
     */
    private fun syncWithFirebase() {
        viewModelScope.launch {
            println("CITIES: Starting background sync with Firebase...")
            
            try {
                val result = repository.syncCitiesFromFirebase()
                
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    _uiState.value = _uiState.value.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        error = null
                    )
                    println("CITIES: Background sync successful - $count cities")
                } else {
                    val error = result.exceptionOrNull()
                    println("CITIES: Background sync failed: ${error?.message}")
                    // Don't show error if we have cached data
                    if (_uiState.value.cities.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            error = "Kan data niet laden: ${error?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                println("CITIES: Sync error: ${e.message}")
                if (_uiState.value.cities.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Geen internetverbinding en geen gecachte data"
                    )
                }
            }
        }
    }

    /**
     * Manual refresh triggered by user
     */
    fun refreshCities() {
        viewModelScope.launch {
            println("CITIES: Manual refresh triggered")
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            try {
                val result = repository.syncCitiesFromFirebase()
                
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        lastSyncTime = System.currentTimeMillis(),
                        error = null
                    )
                    println("CITIES: Refresh successful - $count cities")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Refresh mislukt: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Refresh mislukt: ${e.message}"
                )
            }
        }
    }

    /**
     * Load cache statistics
     */
    fun loadCacheStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getCacheStats()
                _uiState.value = _uiState.value.copy(cacheStats = stats)
            } catch (e: Exception) {
                println("CITIES: Error loading cache stats: ${e.message}")
            }
        }
    }

    /**
     * Add new city (writes to Firebase and updates cache)
     */
    fun addCity(
        name: String,
        country: String,
        description: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        viewModelScope.launch {
            try {
                val cityId = UUID.randomUUID().toString()
                val city = City(
                    id = cityId,
                    name = name,
                    country = country,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    createdBy = currentUser.uid,
                    createdAt = Timestamp.now()
                )

                // Write to Firebase
                db.collection("cities")
                    .document(cityId)
                    .set(city)
                    .await()

                // Sync back to cache
                repository.syncCitiesFromFirebase()
                
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    error = "Kan stad niet toevoegen: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}