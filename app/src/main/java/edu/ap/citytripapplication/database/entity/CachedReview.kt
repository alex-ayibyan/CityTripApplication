package edu.ap.citytripapplication.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.ap.citytripapplication.model.Review

@Entity(tableName = "cached_reviews")
data class CachedReview(
    @PrimaryKey
    val id: String,
    val locationId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long, // Timestamp in milliseconds
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun toReview(): Review {
        return Review(
            id = id,
            locationId = locationId,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            rating = rating,
            comment = comment,
            createdAt = com.google.firebase.Timestamp(createdAt / 1000, 0)
        )
    }

    companion object {
        fun fromReview(review: Review): CachedReview {
            return CachedReview(
                id = review.id,
                locationId = review.locationId,
                userId = review.userId,
                userName = review.userName,
                userEmail = review.userEmail,
                rating = review.rating,
                comment = review.comment,
                createdAt = review.createdAt.seconds * 1000
            )
        }
    }
}






