package edu.ap.citytripapplication.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val messageType: String = "text"
) {
    // Create a copy with updated ID
    fun withId(newId: String): ChatMessage {
        return this.copy(id = newId)
    }
}

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Create a copy with updated ID
    fun withId(newId: String): Conversation {
        return this.copy(id = newId)
    }

    // Create a copy with updated last message
    fun withLastMessage(message: ChatMessage): Conversation {
        return this.copy(lastMessage = message, updatedAt = Timestamp.now())
    }
}