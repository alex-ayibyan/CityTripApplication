package edu.ap.citytripapplication.model

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val locationId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val rating: Int = 0, // 1-5 stars
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)