// LocationViewModel.kt
package edu.ap.citytripapplication.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    val isUploadingImage: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,
    val currentLocation: android.location.Location? = null,
    val imageUri: Uri? = null
)

class LocationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    fun saveLocation(
        name: String,
        description: String,
        latitude: Double,
        longitude: Double,
        category: LocationCategory,
        cityId: String,
        imageUri: Uri? = null,
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
                var imageUrl = ""

                // Upload image if provided
                if (imageUri != null) {
                    _locationState.value = _locationState.value.copy(isUploadingImage = true)
                    imageUrl = uploadImageToFirebase(imageUri)
                    _locationState.value = _locationState.value.copy(isUploadingImage = false)
                }

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
                    userId = currentUser.uid,
                    imageUrl = imageUrl
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
                    isUploadingImage = false,
                    error = "Fout bij opslaan: ${e.message}"
                )
            }
        }
    }

    private suspend fun uploadImageToFirebase(imageUri: Uri): String {
        return try {
            println("DEBUG: Starting image upload with URI: $imageUri")

            // Create a unique filename
            val fileName = "location_images/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            println("DEBUG: Storage reference: $storageRef")
            println("DEBUG: Current user: ${auth.currentUser?.uid}")

            // Upload the file with metadata
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            println("DEBUG: Starting upload...")
            val uploadTask = storageRef.putFile(imageUri, metadata)

            // Wait for upload to complete
            val taskSnapshot = uploadTask.await()
            println("DEBUG: Upload completed successfully")

            // Get the download URL
            println("DEBUG: Getting download URL...")
            val downloadUrl = storageRef.downloadUrl.await().toString()
            println("DEBUG: Download URL obtained: $downloadUrl")

            downloadUrl

        } catch (e: Exception) {
            println("DEBUG: Image upload failed: ${e.message}")
            e.printStackTrace()

            // Check for specific Firebase Storage errors
            when {
                e.message?.contains("does not have permission") == true -> {
                    throw Exception("Geen toestemming om afbeeldingen te uploaden. Controleer Firebase Storage regels.")
                }
                e.message?.contains("not authenticated") == true -> {
                    throw Exception("Niet ingelogd. Log opnieuw in.")
                }
                e.message?.contains("404") == true -> {
                    throw Exception("Firebase Storage niet gevonden. Controleer je Firebase configuratie.")
                }
                else -> {
                    throw Exception("Kon afbeelding niet uploaden: ${e.message}")
                }
            }
        }
    }


    fun setImageUri(uri: Uri?) {
        _locationState.value = _locationState.value.copy(imageUri = uri)
    }

    fun clearImage() {
        _locationState.value = _locationState.value.copy(imageUri = null)
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
        _locationState.value = _locationState.value.copy(
            savedSuccessfully = false,
            isSaving = false,
            isUploadingImage = false
        )
    }
}