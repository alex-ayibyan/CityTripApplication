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

    // Helper to convert a Firestore document snapshot into a User,
    // handling legacy `name` field by splitting into first/last when needed.
    private fun mapDocumentToUser(document: com.google.firebase.firestore.DocumentSnapshot): User? {
        return try {
            val id = document.id
            val firstName = document.getString("firstName") ?: ""
            val lastName = document.getString("lastName") ?: ""
            val email = document.getString("email") ?: ""
            val profileImage = document.getString("profileImage") ?: ""

            // Keep first/last as stored. If both are blank, leave them blank.
            val resolvedFirst = firstName
            val resolvedLast = lastName

            val lastSeen = document.getTimestamp("lastSeen") ?: com.google.firebase.Timestamp.now()
            val createdAt = document.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now()
            val updatedAt = document.getTimestamp("updatedAt") ?: com.google.firebase.Timestamp.now()

            User(
                id = id,
                firstName = resolvedFirst,
                lastName = resolvedLast,
                email = email,
                profileImage = profileImage,
                isOnline = document.getBoolean("isOnline") ?: false,
                lastSeen = lastSeen,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e("UserRepository", "Error mapping user document", e)
            null
        }
    }

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
                mapDocumentToUser(userDoc)
            } else {
                // Create a basic user profile if it doesn't exist
                // Try to split displayName into first/last if present
                val displayName = firebaseUser.displayName ?: ""
                val nameParts = displayName.trim().split(" ").filter { it.isNotBlank() }
                val firstName = nameParts.firstOrNull() ?: ""
                val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

                val newUser = User(
                    id = firebaseUser.uid,
                    firstName = firstName,
                    lastName = lastName,
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
                mapDocumentToUser(userDoc)
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
            .orderBy("firstName")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error getting users", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { document ->
                    mapDocumentToUser(document)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Search users by name
     */
    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        val q = query.trim()

        // We'll run two prefix listeners: one on firstName and one on email, then merge results.
        val combined = linkedMapOf<String, User>()

        val firstNameListener = firestore.collection("users")
            .orderBy("firstName")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error searching users by firstName", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Update combined map with firstName results
                snapshot?.documents?.forEach { doc ->
                    mapDocumentToUser(doc)?.let { combined[it.id] = it }
                }

                trySend(combined.values.toList())
            }

        val emailListener = firestore.collection("users")
            .orderBy("email")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error searching users by email", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Update combined map with email results
                snapshot?.documents?.forEach { doc ->
                    mapDocumentToUser(doc)?.let { combined[it.id] = it }
                }

                trySend(combined.values.toList())
            }

        awaitClose {
            firstNameListener.remove()
            emailListener.remove()
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(
        userId: String,
        firstName: String? = null,
        lastName: String? = null,
        profileImage: String? = null
    ): Result<User> {
        return try {
            val updates = hashMapOf<String, Any>(
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firstName?.let { if (it.isNotBlank()) updates["firstName"] = it }
            lastName?.let { if (it.isNotBlank()) updates["lastName"] = it }

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