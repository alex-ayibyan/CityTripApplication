package edu.ap.citytripapplication.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val profileImage: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {

    fun copyWithId(newId: String): User {
        return this.copy(id = newId)
    }
}