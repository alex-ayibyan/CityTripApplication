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
        println("DEBUG: ========== SAVING LOCATION ==========")
        println("DEBUG: Name: $name")
        println("DEBUG: CityId: $cityId")
        println("DEBUG: Category: ${category.name}")
        println("DEBUG: Latitude: $latitude, Longitude: $longitude")
        println("DEBUG: Has Image: ${imageUri != null}")
        
        if (name.isBlank()) {
            _locationState.value = _locationState.value.copy(
                error = "Naam mag niet leeg zijn"
            )
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("DEBUG: ❌ No user logged in!")
            _locationState.value = _locationState.value.copy(
                error = "Je moet ingelogd zijn om een locatie op te slaan"
            )
            return
        }
        
        println("DEBUG: User ID: ${currentUser.uid}")

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
                    println("DEBUG: Uploading image...")
                    _locationState.value = _locationState.value.copy(isUploadingImage = true)
                    imageUrl = uploadImageToFirebase(imageUri)
                    _locationState.value = _locationState.value.copy(isUploadingImage = false)
                    println("DEBUG: Image uploaded: $imageUrl")
                }

                // Create location document
                val locationId = UUID.randomUUID().toString()
                println("DEBUG: Generated location ID: $locationId")
                
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

                println("DEBUG: Location object created:")
                println("DEBUG:   - id: ${location.id}")
                println("DEBUG:   - name: ${location.name}")
                println("DEBUG:   - cityId: ${location.cityId}")
                println("DEBUG:   - category: ${location.category.name}")
                println("DEBUG:   - imageUrl: ${location.imageUrl}")

                // Save to Firestore
                println("DEBUG: Saving to Firestore collection 'locations'...")
                firestore.collection("locations")
                    .document(locationId)
                    .set(location)
                    .await()

                println("DEBUG: ✅ Location saved successfully to Firestore!")
                println("DEBUG: Document path: locations/$locationId")

                // Verify the save by reading it back
                try {
                    val savedDoc = firestore.collection("locations")
                        .document(locationId)
                        .get()
                        .await()
                    
                    if (savedDoc.exists()) {
                        println("DEBUG: ✅ Verification: Document exists in Firestore")
                        println("DEBUG: Saved data: ${savedDoc.data}")
                    } else {
                        println("DEBUG: ⚠️ Verification: Document NOT found!")
                    }
                } catch (e: Exception) {
                    println("DEBUG: ⚠️ Could not verify save: ${e.message}")
                }

                _locationState.value = _locationState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )

                println("DEBUG: Calling onSuccess callback...")
                onSuccess()
                println("DEBUG: ========== SAVE COMPLETE ==========")
                
            } catch (e: Exception) {
                println("DEBUG: ❌ Error saving location: ${e.message}")
                e.printStackTrace()
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
        println("DEBUG: Resetting saved state")
        _locationState.value = _locationState.value.copy(
            savedSuccessfully = false,
            isSaving = false,
            isUploadingImage = false
        )
    }
}






