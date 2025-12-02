package edu.ap.citytripapplication.repository

import android.util.Log
import edu.ap.citytripapplication.model.ChatMessage
import edu.ap.citytripapplication.model.Conversation
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow

        // Remove server-side ordering to avoid requiring a composite index.
        // We'll sort client-side by `updatedAt` instead so the listener works without an index.
        val listener = firestore.collection("conversations")
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error getting conversations", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val conversation = document.toObject(Conversation::class.java)
                        conversation?.withId(document.id)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error parsing conversation", e)
                        null
                    }
                } ?: emptyList()

                // Sort conversations client-side by updatedAt descending
                val sorted = conversations.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }

                trySend(sorted)
            }

        awaitClose { listener.remove() }
    }

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error getting messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val message = document.toObject(ChatMessage::class.java)
                        message?.withId(document.id)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error parsing message", e)
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        receiverId: String,
        message: String,
        messageType: String = "text"
    ): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            // Find or create conversation
            val conversationId = getOrCreateConversation(currentUserId, receiverId)

            // Create message with Firestore-generated ID
            val messageId = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document().id

            val chatMessage = ChatMessage(
                id = messageId,
                senderId = currentUserId,
                receiverId = receiverId,
                message = message,
                messageType = messageType,
                timestamp = Timestamp.now(),
                isRead = false
            )

            // Add message to conversation
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(chatMessage).await()

            // Update conversation last message and timestamp
            firestore.collection("conversations")
                .document(conversationId)
                .update(
                    "lastMessage", chatMessage,
                    "updatedAt", Timestamp.now()
                ).await()

            Result.success("Message sent successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateConversation(userId1: String, userId2: String): String {
        val participants = listOf(userId1, userId2).sorted()

        val query = firestore.collection("conversations")
            .whereEqualTo("participantIds", participants)
            .limit(1)

        val snapshot = query.get().await()

        return if (!snapshot.isEmpty) {
            snapshot.documents.first().id
        } else {
            // Create new conversation
            val conversationId = firestore.collection("conversations").document().id

            val conversation = Conversation(
                id = conversationId,
                participantIds = participants,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            firestore.collection("conversations")
                .document(conversationId)
                .set(conversation).await()

            conversationId
        }
    }

    suspend fun markMessagesAsRead(conversationId: String) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return

            val messagesQuery = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)

            val snapshot = messagesQuery.get().await()

            val batch = firestore.batch()
            snapshot.documents.forEach { document ->
                batch.update(document.reference, "isRead", true)
            }

            if (!snapshot.isEmpty) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking messages as read", e)
        }
    }

    // Helper method to get or create conversation ID for direct messaging
    suspend fun getDirectConversationId(otherUserId: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        return getOrCreateConversation(currentUserId, otherUserId)
    }

    // Delete a conversation and its messages
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            val conversationRef = firestore.collection("conversations").document(conversationId)

            // Delete all messages in the subcollection in batches
            val messagesSnapshot = conversationRef.collection("messages").get().await()
            val batch = firestore.batch()
            messagesSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            // Delete the conversation document itself
            batch.delete(conversationRef)

            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting conversation", e)
            Result.failure(e)
        }
    }
}