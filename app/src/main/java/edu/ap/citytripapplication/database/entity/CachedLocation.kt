package edu.ap.citytripapplication.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.ap.citytripapplication.model.Location
import edu.ap.citytripapplication.model.LocationCategory

@Entity(tableName = "cached_locations")
data class CachedLocation(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val cityId: String,
    val userId: String,
    val createdAt: Long, // Timestamp in milliseconds
    val averageRating: Double,
    val totalRatings: Int,
    val imageUrl: String,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun toLocation(): Location {
        return Location(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            category = LocationCategory.fromString(category),
            cityId = cityId,
            userId = userId,
            createdAt = com.google.firebase.Timestamp(createdAt / 1000, 0),
            averageRating = averageRating,
            totalRatings = totalRatings,
            imageUrl = imageUrl
        )
    }

    companion object {
        fun fromLocation(location: Location): CachedLocation {
            return CachedLocation(
                id = location.id,
                name = location.name,
                description = location.description,
                latitude = location.latitude,
                longitude = location.longitude,
                category = location.category.name,
                cityId = location.cityId,
                userId = location.userId,
                createdAt = location.createdAt.seconds * 1000,
                averageRating = location.averageRating,
                totalRatings = location.totalRatings,
                imageUrl = location.imageUrl
            )
        }
    }
}