package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.City
import edu.ap.citytripapplication.model.Location
import edu.ap.citytripapplication.model.LocationCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CityDetailsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _city = MutableStateFlow<City?>(null)
    val city: StateFlow<City?> = _city.asStateFlow()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    private val _filteredLocations = MutableStateFlow<List<Location>>(emptyList())
    val filteredLocations: StateFlow<List<Location>> = _filteredLocations.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<LocationCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<LocationCategory>> = _selectedCategories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCityData(cityId: String) {
        viewModelScope.launch {
            println("DEBUG: ========== LOADING CITY DATA ==========")
            println("DEBUG: CityId: $cityId")
            _isLoading.value = true
            try {
                // Load city
                val cityDoc = db.collection("cities").document(cityId).get().await()
                _city.value = City(
                    id = cityDoc.id,
                    name = cityDoc.getString("name") ?: "",
                    country = cityDoc.getString("country") ?: "",
                    description = cityDoc.getString("description") ?: "",
                    latitude = cityDoc.getDouble("latitude") ?: 0.0,
                    longitude = cityDoc.getDouble("longitude") ?: 0.0,
                    imageUrl = cityDoc.getString("imageUrl") ?: "",
                    createdBy = cityDoc.getString("createdBy") ?: "",
                    createdAt = cityDoc.getTimestamp("createdAt") ?: Timestamp.now()
                )
                println("DEBUG: City loaded: ${_city.value?.name}")

                // Load locations for this city
                println("DEBUG: Querying locations collection for cityId: $cityId")
                val locationsQuery = db.collection("locations")
                    .whereEqualTo("cityId", cityId)
                    .get()
                    .await()
                
                println("DEBUG: Query completed. Found ${locationsQuery.documents.size} documents")
                
                if (locationsQuery.documents.isEmpty()) {
                    println("DEBUG: ⚠️ No locations found for this city!")
                    println("DEBUG: Check Firebase Console if locations exist with cityId: $cityId")
                }
                
                val locationsList = locationsQuery.documents.mapIndexedNotNull { index, doc ->
                    try {
                        println("DEBUG: [$index] Processing location document: ${doc.id}")
                        val locationData = mapOf(
                            "name" to doc.getString("name"),
                            "cityId" to doc.getString("cityId"),
                            "category" to doc.getString("category"),
                            "imageUrl" to doc.getString("imageUrl")
                        )
                        println("DEBUG: [$index] Location data: $locationData")
                        
                        val location = Location(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            category = LocationCategory.fromString(doc.getString("category") ?: "OTHER"),
                            cityId = doc.getString("cityId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            totalRatings = (doc.getLong("totalRatings") ?: 0).toInt(),
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                        println("DEBUG: [$index] ✅ Location parsed successfully: ${location.name}")
                        location
                    } catch (e: Exception) {
                        println("DEBUG: [$index] ❌ Error parsing location ${doc.id}: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
                
                println("DEBUG: Successfully parsed ${locationsList.size} out of ${locationsQuery.documents.size} locations")
                println("DEBUG: Updating _locations state...")
                _locations.value = locationsList
                _filteredLocations.value = locationsList
                println("DEBUG: State updated. _locations.value.size = ${_locations.value.size}")
                println("DEBUG: State updated. _filteredLocations.value.size = ${_filteredLocations.value.size}")
                println("DEBUG: ========== LOADING COMPLETE ==========")
            } catch (e: Exception) {
                println("DEBUG: ❌ Error loading city data: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCategoryFilter(category: LocationCategory) {
        val newSelectedCategories = _selectedCategories.value.toMutableSet()
        if (newSelectedCategories.contains(category)) {
            newSelectedCategories.remove(category)
        } else {
            newSelectedCategories.add(category)
        }
        _selectedCategories.value = newSelectedCategories
        applyFilters()
    }

    fun clearFilters() {
        _selectedCategories.value = emptySet()
        _filteredLocations.value = _locations.value
    }

    private fun applyFilters() {
        val selected = _selectedCategories.value
        val allLocations = _locations.value

        if (selected.isEmpty()) {
            _filteredLocations.value = allLocations
        } else {
            _filteredLocations.value = allLocations.filter { location ->
                selected.contains(location.category)
            }
        }
    }

    fun refreshData(cityId: String) {
        println("DEBUG: Manual refresh triggered for cityId: $cityId")
        loadCityData(cityId)
    }
}