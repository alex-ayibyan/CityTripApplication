package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.User
import edu.ap.citytripapplication.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(
        firestore = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance()
    )
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _currentUser.value = user
            user?.let {
                _firstName.value = it.firstName
                _lastName.value = it.lastName
            }
        }
    }

    fun setFirstName(value: String) {
        _firstName.value = value
    }

    fun setLastName(value: String) {
        _lastName.value = value
    }

    fun saveProfile(onSuccess: () -> Unit = {}) {
        val userId = _currentUser.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = userRepository.updateUserProfile(
                userId = userId,
                firstName = _firstName.value.takeIf { it.isNotBlank() },
                lastName = _lastName.value.takeIf { it.isNotBlank() }
            )

            result.onSuccess { updated ->
                _currentUser.value = updated
                onSuccess()
            }.onFailure { ex ->
                _error.value = ex.message ?: "Failed to update profile"
            }

            _isLoading.value = false
        }
    }
}
