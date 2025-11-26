package edu.ap.citytripapplication.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
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
import java.io.ByteArrayOutputStream
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

class LocationViewModel(application: Application) : AndroidViewModel(application) {
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
                var imageBase64 = ""

                // Convert image to base64 if provided
                if (imageUri != null) {
                    println("DEBUG: Converting image to base64...")
                    _locationState.value = _locationState.value.copy(isUploadingImage = true)
                    imageBase64 = convertImageToBase64(imageUri)
                    _locationState.value = _locationState.value.copy(isUploadingImage = false)
                    println("DEBUG: Image converted successfully (${imageBase64.length} characters)")
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
                    imageUrl = imageBase64 // Store base64 string instead of URL
                )

                println("DEBUG: Location object created:")
                println("DEBUG:   - id: ${location.id}")
                println("DEBUG:   - name: ${location.name}")
                println("DEBUG:   - cityId: ${location.cityId}")
                println("DEBUG:   - category: ${location.category.name}")
                println("DEBUG:   - imageUrl length: ${location.imageUrl.length}")

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
                        val savedImageLength = savedDoc.getString("imageUrl")?.length ?: 0
                        println("DEBUG: Saved imageUrl length: $savedImageLength")
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

    /**
     * Convert image URI to base64 string with compression
     */
    private fun convertImageToBase64(imageUri: Uri): String {
        return try {
            println("DEBUG: Opening image from URI: $imageUri")
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(imageUri)
            
            if (inputStream == null) {
                throw Exception("Kon afbeelding niet openen")
            }

            // Decode the image
            println("DEBUG: Decoding bitmap...")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                throw Exception("Kon bitmap niet decoderen")
            }

            println("DEBUG: Original bitmap size: ${bitmap.width}x${bitmap.height}")

            // Compress and resize the image to reduce size
            val maxWidth = 1024
            val maxHeight = 1024
            
            val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val scale = Math.min(
                    maxWidth.toFloat() / bitmap.width,
                    maxHeight.toFloat() / bitmap.height
                )
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                
                println("DEBUG: Scaling bitmap to: ${newWidth}x${newHeight}")
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            // Convert to JPEG and then to base64
            println("DEBUG: Converting to JPEG and base64...")
            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            
            println("DEBUG: Compressed image size: ${byteArray.size} bytes")
            
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            
            // Clean up
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            
            println("DEBUG: Base64 conversion successful")
            base64String

        } catch (e: Exception) {
            println("DEBUG: Image conversion failed: ${e.message}")
            e.printStackTrace()
            throw Exception("Kon afbeelding niet converteren: ${e.message}")
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