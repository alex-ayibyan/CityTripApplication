package edu.ap.citytripapplication.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.ap.citytripapplication.model.City

@Entity(tableName = "cached_cities")
data class CachedCity(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String,
    val createdBy: String,
    val createdAt: Long, // Timestamp in milliseconds
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun toCity(): City {
        return City(
            id = id,
            name = name,
            country = country,
            description = description,
            latitude = latitude,
            longitude = longitude,
            imageUrl = imageUrl,
            createdBy = createdBy,
            createdAt = com.google.firebase.Timestamp(createdAt / 1000, 0)
        )
    }

    companion object {
        fun fromCity(city: City): CachedCity {
            return CachedCity(
                id = city.id,
                name = city.name,
                country = city.country,
                description = city.description,
                latitude = city.latitude,
                longitude = city.longitude,
                imageUrl = city.imageUrl,
                createdBy = city.createdBy,
                createdAt = city.createdAt.seconds * 1000
            )
        }
    }
}