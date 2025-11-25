package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.ap.citytripapplication.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class ReviewState(
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
)

class ReviewViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _reviewState = MutableStateFlow(ReviewState())
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()

    fun loadReviews(locationId: String) {
        viewModelScope.launch {
            println("DEBUG: Loading reviews for locationId: $locationId")
            _reviewState.value = _reviewState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // First try with orderBy
                val querySnapshot = try {
                    firestore.collection("reviews")
                        .whereEqualTo("locationId", locationId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                } catch (e: Exception) {
                    // If orderBy fails (missing index), try without it
                    println("DEBUG: OrderBy failed, trying without index: ${e.message}")
                    firestore.collection("reviews")
                        .whereEqualTo("locationId", locationId)
                        .get()
                        .await()
                }
                
                println("DEBUG: Found ${querySnapshot.documents.size} reviews")
                
                val reviewsList = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        println("DEBUG: Processing review doc: ${doc.id}")
                        println("DEBUG: Review data: ${doc.data}")
                        Review(
                            id = doc.id,
                            locationId = doc.getString("locationId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Anoniem",
                            userEmail = doc.getString("userEmail") ?: "",
                            rating = (doc.getLong("rating") ?: 0).toInt(),
                            comment = doc.getString("comment") ?: "",
                            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        println("DEBUG: Error parsing review ${doc.id}: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.createdAt.seconds } // Sort in memory if needed

                println("DEBUG: Mapped ${reviewsList.size} reviews to objects")
                _reviewState.value = _reviewState.value.copy(
                    reviews = reviewsList,
                    isLoading = false
                )
                println("DEBUG: ReviewState updated with ${reviewsList.size} reviews")
            } catch (e: Exception) {
                println("DEBUG: Error loading reviews: ${e.message}")
                e.printStackTrace()
                _reviewState.value = _reviewState.value.copy(
                    isLoading = false,
                    error = "Kon reviews niet laden: ${e.message}"
                )
            }
        }
    }

    fun addReview(
        locationId: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _reviewState.value = _reviewState.value.copy(
                error = "Je moet ingelogd zijn om een review toe te voegen"
            )
            return
        }

        if (rating < 1 || rating > 5) {
            _reviewState.value = _reviewState.value.copy(
                error = "Rating moet tussen 1 en 5 zijn"
            )
            return
        }

        viewModelScope.launch {
            _reviewState.value = _reviewState.value.copy(
                isSaving = true,
                error = null,
                savedSuccessfully = false
            )

            try {
                // Check if user already reviewed this location
                val existingReview = firestore.collection("reviews")
                    .whereEqualTo("locationId", locationId)
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                if (!existingReview.isEmpty) {
                    _reviewState.value = _reviewState.value.copy(
                        isSaving = false,
                        error = "Je hebt deze locatie al beoordeeld"
                    )
                    return@launch
                }

                // Create new review
                val reviewId = UUID.randomUUID().toString()
                val review = Review(
                    id = reviewId,
                    locationId = locationId,
                    userId = currentUser.uid,
                    userName = currentUser.email?.substringBefore("@") ?: "Anoniem",
                    userEmail = currentUser.email ?: "",
                    rating = rating,
                    comment = comment,
                    createdAt = Timestamp.now()
                )

                println("DEBUG: Creating review with ID: $reviewId")
                println("DEBUG: Review data: locationId=$locationId, rating=$rating, userId=${currentUser.uid}")

                // Save review
                firestore.collection("reviews")
                    .document(reviewId)
                    .set(review)
                    .await()

                println("DEBUG: Review saved successfully")

                // Update location's average rating
                println("DEBUG: Updating location rating...")
                updateLocationRating(locationId)

                // Reload reviews
                println("DEBUG: Reloading reviews...")
                loadReviews(locationId)

                _reviewState.value = _reviewState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )

                onSuccess()
            } catch (e: Exception) {
                _reviewState.value = _reviewState.value.copy(
                    isSaving = false,
                    error = "Kon review niet opslaan: ${e.message}"
                )
            }
        }
    }

    private suspend fun updateLocationRating(locationId: String) {
        try {
            // Get all reviews for this location
            val reviews = firestore.collection("reviews")
                .whereEqualTo("locationId", locationId)
                .get()
                .await()

            val totalRatings = reviews.size()
            val sumRatings = reviews.documents.sumOf { 
                (it.getLong("rating") ?: 0).toInt() 
            }

            val averageRating = if (totalRatings > 0) {
                sumRatings.toDouble() / totalRatings
            } else {
                0.0
            }

            // Update location document
            firestore.collection("locations")
                .document(locationId)
                .update(
                    mapOf(
                        "averageRating" to averageRating,
                        "totalRatings" to totalRatings
                    )
                )
                .await()

        } catch (e: Exception) {
            println("Error updating location rating: ${e.message}")
        }
    }

    fun deleteReview(reviewId: String, locationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("reviews")
                    .document(reviewId)
                    .delete()
                    .await()

                // Update location rating after deletion
                updateLocationRating(locationId)

                // Reload reviews
                loadReviews(locationId)

            } catch (e: Exception) {
                _reviewState.value = _reviewState.value.copy(
                    error = "Kon review niet verwijderen: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _reviewState.value = _reviewState.value.copy(error = null)
    }

    fun resetSavedState() {
        _reviewState.value = _reviewState.value.copy(savedSuccessfully = false)
    }
}