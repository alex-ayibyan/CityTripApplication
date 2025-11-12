package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.Location
import edu.ap.citytripapplication.model.LocationCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class LocationState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,
    val currentLocation: android.location.Location? = null
)

class LocationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    fun saveLocation(
        name: String,
        description: String,
        latitude: Double,
        longitude: Double,
        category: LocationCategory,
        cityId: String,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            _locationState.value = _locationState.value.copy(
                error = "Naam mag niet leeg zijn"
            )
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _locationState.value = _locationState.value.copy(
                error = "Je moet ingelogd zijn om een locatie op te slaan"
            )
            return
        }

        viewModelScope.launch {
            _locationState.value = _locationState.value.copy(
                isSaving = true,
                error = null,
                savedSuccessfully = false
            )
            
            try {
                // Create location document
                val locationId = UUID.randomUUID().toString()
                val location = Location(
                    id = locationId,
                    name = name,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    category = category,
                    cityId = cityId,
                    userId = currentUser.uid
                )

                // Save to Firestore
                firestore.collection("locations")
                    .document(locationId)
                    .set(location)
                    .await()

                _locationState.value = _locationState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
                
                onSuccess()
            } catch (e: Exception) {
                _locationState.value = _locationState.value.copy(
                    isSaving = false,
                    error = "Fout bij opslaan: ${e.message}"
                )
            }
        }
    }

    fun updateCurrentLocation(location: android.location.Location) {
        _locationState.value = _locationState.value.copy(
            currentLocation = location
        )
    }

    fun clearError() {
        _locationState.value = _locationState.value.copy(error = null)
    }

    fun resetSavedState() {
        _locationState.value = _locationState.value.copy(savedSuccessfully = false)
    }
}