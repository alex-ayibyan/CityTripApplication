package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.ChatMessage
import edu.ap.citytripapplication.model.Conversation
import edu.ap.citytripapplication.model.User
import edu.ap.citytripapplication.repository.UserRepository
import edu.ap.citytripapplication.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(
        firestore = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance()
    ),
    private val userRepository: UserRepository = UserRepository(
        firestore = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance()
    )
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCurrentUser()
        loadConversations()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getConversations()
                .collect { conversations ->
                    _conversations.value = conversations
                }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .collect { users ->
                    // Filter out current user from the list
                    val currentUserId = _currentUser.value?.id
                    _users.value = if (currentUserId != null) {
                        users.filter { it.id != currentUserId }
                    } else {
                        users
                    }
                }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            userRepository.searchUsers(query)
                .collect { users ->
                    val currentUserId = _currentUser.value?.id
                    _users.value = if (currentUserId != null) {
                        users.filter { it.id != currentUserId }
                    } else {
                        users
                    }
                }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(conversationId)
                .collect { messages ->
                    _messages.value = messages
                    // Mark messages as read when loading
                    chatRepository.markMessagesAsRead(conversationId)
                }
        }
    }

    fun sendMessage(receiverId: String, message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = chatRepository.sendMessage(receiverId, message)

            result.onSuccess {
                // Message sent successfully
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to send message"
            }

            _isLoading.value = false
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = try {
                chatRepository.deleteConversation(conversationId)
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }

            result.onSuccess {
                // Optionally refresh conversations - repository listener should update automatically
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to delete conversation"
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getUserById(userId: String): User? {
        return _users.value.find { it.id == userId }
    }

    suspend fun getOrCreateConversationId(otherUserId: String): String {
        return chatRepository.getDirectConversationId(otherUserId)
    }
}