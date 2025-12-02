package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val currentUser = auth.currentUser
        _authState.value = AuthState(
            isAuthenticated = currentUser != null,
            user = currentUser
        )
    }

    fun signUp(email: String, password: String, firstName: String = "", lastName: String = "", onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = _authState.value.copy(
                error = "Email en wachtwoord mogen niet leeg zijn"
            )
            return
        }

        if (password.length < 6) {
            _authState.value = _authState.value.copy(
                error = "Wachtwoord moet minstens 6 karakters lang zijn"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                // Build display name from first/last if provided, else leave empty
                val displayName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")

                if (firebaseUser != null) {
                    // Update FirebaseUser profile displayName
                    if (displayName.isNotBlank()) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                    }

                    // Create user document in Firestore
                    val userDoc = User(
                        id = firebaseUser.uid,
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = firebaseUser.email ?: "",
                        profileImage = firebaseUser.photoUrl?.toString() ?: "",
                        createdAt = com.google.firebase.Timestamp.now(),
                        updatedAt = com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("users").document(firebaseUser.uid).set(userDoc).await()

                    _authState.value = AuthState(
                        isAuthenticated = true,
                        user = firebaseUser
                    )

                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(isLoading = false, error = "Registratie mislukt: geen gebruikersaccount aangemaakt")
                }
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = when {
                        e.message?.contains("already in use") == true -> 
                            "Dit email adres is al in gebruik"
                        e.message?.contains("invalid-email") == true -> 
                            "Ongeldig email adres"
                        else -> "Registratie mislukt: ${e.message}"
                    }
                )
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = _authState.value.copy(
                error = "Email en wachtwoord mogen niet leeg zijn"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState(
                    isAuthenticated = true,
                    user = result.user
                )
                onSuccess()
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = when {
                        e.message?.contains("no user record") == true || 
                        e.message?.contains("invalid-credential") == true -> 
                            "Ongeldig email of wachtwoord"
                        e.message?.contains("invalid-email") == true -> 
                            "Ongeldig email adres"
                        else -> "Login mislukt: ${e.message}"
                    }
                )
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState(isAuthenticated = false)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}