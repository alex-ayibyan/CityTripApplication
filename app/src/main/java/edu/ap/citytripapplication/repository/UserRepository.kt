package edu.ap.citytripapplication.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.ap.citytripapplication.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /**
     * Get current user from Firebase
     */
    suspend fun getCurrentUser(): User? {
        return try {
            val firebaseUser = auth.currentUser ?: return null

            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (userDoc.exists()) {
                userDoc.toObject(User::class.java)?.copy(id = userDoc.id)
            } else {
                // Create a basic user profile if it doesn't exist
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    profileImage = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = com.google.firebase.Timestamp.now()
                )

                // Save to Firestore
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                newUser
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting current user", e)
            null
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (userDoc.exists()) {
                userDoc.toObject(User::class.java)?.copy(id = userDoc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user by ID", e)
            null
        }
    }

    /**
     * Get all users (for user search/messaging)
     */
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error getting users", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val user = document.toObject(User::class.java)
                        user?.copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error parsing user", e)
                        null
                    }
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Search users by name
     */
    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error searching users", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val user = document.toObject(User::class.java)
                        user?.copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error parsing user", e)
                        null
                    }
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(
        userId: String,
        name: String,
        profileImage: String? = null
    ): Result<User> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            profileImage?.let {
                updates["profileImage"] = it
            }

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            // Get updated user
            val updatedUser = getUserById(userId)
            if (updatedUser != null) {
                Result.success(updatedUser)
            } else {
                Result.failure(Exception("User not found after update"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Set user online status
     */
    suspend fun setUserOnlineStatus(isOnline: Boolean) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return

            firestore.collection("users")
                .document(currentUserId)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error setting online status", e)
        }
    }

    /**
     * Get users by IDs (for conversation participants)
     */
    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return try {
            if (userIds.isEmpty()) return emptyList()

            val users = mutableListOf<User>()
            // Firestore doesn't support OR queries with IN directly for multiple documents
            // So we fetch each user individually (not optimal for large lists)
            for (userId in userIds) {
                getUserById(userId)?.let { users.add(it) }
            }
            users
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting users by IDs", e)
            emptyList()
        }
    }
}