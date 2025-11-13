// City.kt
package edu.ap.citytripapplication.model

import com.google.firebase.Timestamp

data class City(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
)