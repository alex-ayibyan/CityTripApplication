package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.User
import edu.ap.citytripapplication.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class UserListViewModel(
    private val userRepository: UserRepository = UserRepository(
        firestore = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance()
    )
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCurrentUser()
        loadUsers()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .collect { users ->
                    val currentUserId = _currentUser.value?.id
                    _users.value = if (currentUserId != null) {
                        users.filter { it.id != currentUserId }
                    } else {
                        users
                    }
                    _filteredUsers.value = _users.value
                }
        }
    }

    fun searchUsers(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            _filteredUsers.value = _users.value
        } else {
            viewModelScope.launch {
                userRepository.searchUsers(query)
                    .collect { users ->
                        val currentUserId = _currentUser.value?.id
                        _filteredUsers.value = if (currentUserId != null) {
                            users.filter { it.id != currentUserId }
                        } else {
                            users
                        }
                    }
            }
        }
    }
}