package edu.ap.citytripapplication.model

import com.google.firebase.Timestamp

data class Location(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: LocationCategory = LocationCategory.OTHER,
    val cityId: String = "",
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val averageRating: Double = 0.0,
    val totalRatings: Int = 0,
    val imageUrl: String = ""
)

enum class LocationCategory(val displayName: String) {
    RESTAURANT("Restaurant"),
    MUSEUM("Museum"),
    PARK("Park"),
    MONUMENT("Monument"),
    SHOPPING("Winkelen"),
    ENTERTAINMENT("Entertainment"),
    HOTEL("Hotel"),
    CAFE("Café"),
    ATTRACTION("Attractie"),
    NIGHTLIFE("Nachtleven"),
    OTHER("Overig");

    companion object {
        fun fromString(value: String): LocationCategory {
            return values().find { it.name == value } ?: OTHER
        }
    }
}