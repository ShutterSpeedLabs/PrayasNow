package com.example.prayasnow.auth

import com.example.prayasnow.data.User
import com.example.prayasnow.data.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Authentication service that manages user login and role-based access
 */
class AuthService(
    private val userDao: UserDao,
    private val roleManager: RoleManager,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _hasAdminAccess = MutableStateFlow(false)
    val hasAdminAccess: StateFlow<Boolean> = _hasAdminAccess.asStateFlow()
    
    init {
        // Initialize with current Firebase user
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            initializeUser(firebaseUser)
        }
        
        // Listen to auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                initializeUser(user)
            } else {
                clearUserSession()
            }
        }
    }
    
    /**
     * Initialize user from Firebase user
     */
    private fun initializeUser(firebaseUser: FirebaseUser) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    role = "USER", // Will be updated by role manager
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Initialize user role and save to database
                val userWithRole = roleManager.initializeUserRole(user)
                userDao.insertUser(userWithRole)
                
                // Update state
                _currentUser.value = userWithRole
                _isLoggedIn.value = true
                _hasAdminAccess.value = userWithRole.hasAdminAccess()
                
            } catch (e: Exception) {
                clearUserSession()
            }
        }
    }
    
    /**
     * Clear user session
     */
    private fun clearUserSession() {
        _currentUser.value = null
        _isLoggedIn.value = false
        _hasAdminAccess.value = false
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return _currentUser.value?.uid
    }
    
    /**
     * Check if current user has admin access
     */
    suspend fun checkAdminAccess(): Boolean = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext false
        return@withContext roleManager.hasAdminAccess(userId)
    }
    
    /**
     * Refresh user data and admin access
     */
    suspend fun refreshUserData() = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext
        
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                _hasAdminAccess.value = user.hasAdminAccess()
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Observe admin access for current user
     */
    fun observeAdminAccess(): Flow<Boolean> {
        return currentUser.map { user ->
            user?.hasAdminAccess() ?: false
        }
    }
    
    /**
     * Sign out user
     */
    fun signOut() {
        firebaseAuth.signOut()
        clearUserSession()
    }
    
    /**
     * Get user role
     */
    suspend fun getUserRole(): String = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext "USER"
        return@withContext roleManager.getUserRole(userId)
    }
    
    /**
     * Update current user role (admin only)
     */
    suspend fun updateUserRole(targetUserId: String, newRole: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: return@withContext false
        
        if (!checkAdminAccess()) {
            return@withContext false
        }
        
        return@withContext roleManager.updateUserRole(targetUserId, newRole)
    }
    
    /**
     * Promote user to admin (admin only)
     */
    suspend fun promoteToAdmin(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: return@withContext false
        return@withContext roleManager.promoteToAdmin(currentUserId, targetUserId)
    }
    
    /**
     * Demote user from admin (admin only)
     */
    suspend fun demoteFromAdmin(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: return@withContext false
        return@withContext roleManager.demoteFromAdmin(currentUserId, targetUserId)
    }
    
    /**
     * Check if user is admin by email (for initial setup)
     */
    fun isAdminEmail(email: String): Boolean {
        return roleManager.isAdminEmail(email)
    }
}
