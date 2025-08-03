package com.example.prayasnow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prayasnow.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        try {
            // Set up Firebase Auth state listener
            FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                _authState.value = _authState.value.copy(
                    user = currentUser,
                    isLoggedIn = currentUser != null
                )
                println("Firebase Auth State Changed: user=${currentUser?.email}, isLoggedIn=${currentUser != null}")
            }
            
            // Try auto-login with stored credentials
            tryAutoLogin()
        } catch (e: Exception) {
            println("Error in AuthViewModel init: ${e.message}")
            // Continue without auto-login if there's an error
        }
    }
    
    private fun tryAutoLogin() {
        viewModelScope.launch {
            // Only try auto-login if not already logged in
            if (!authState.value.isLoggedIn) {
                val storedCredentials = authRepository.getStoredCredentials()
                if (storedCredentials != null) {
                    signInWithUsernameOrEmail(storedCredentials.emailOrUsername, storedCredentials.password)
                }
            }
        }
    }
    
    fun signInWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            authRepository.signInWithEmailAndPassword(email, password)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
    
    fun signInWithUsernameOrEmail(usernameOrEmail: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            authRepository.signInWithUsernameOrEmail(usernameOrEmail, password)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
    
    fun signUpWithEmailAndPassword(email: String, password: String, name: String, username: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            authRepository.signUpWithEmailAndPassword(email, password, name, username)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            authRepository.resetPassword(email)
                .onSuccess {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Password reset email sent successfully"
                    )
                }
                .onFailure { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState()
        }
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    suspend fun getStoredCredentials() = authRepository.getStoredCredentials()
    
    fun createTestUser() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            authRepository.createTestUser()
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
} 