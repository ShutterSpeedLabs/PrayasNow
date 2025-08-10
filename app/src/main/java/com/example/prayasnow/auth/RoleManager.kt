package com.example.prayasnow.auth

import com.example.prayasnow.data.User
import com.example.prayasnow.data.UserDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages user roles and admin access control
 */
class RoleManager(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ADMIN_ROLES_COLLECTION = "admin_roles"
        
        // Predefined admin emails (fallback system)
        private val ADMIN_EMAILS = setOf(
            "admin@prayasnow.com",
            "developer@prayasnow.com"
            // Add more admin emails as needed
        )
    }
    
    /**
     * Check if current user has admin access
     */
    suspend fun hasAdminAccess(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
            return@withContext user?.hasAdminAccess() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get current user's role
     */
    suspend fun getUserRole(userId: String): String = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
            return@withContext user?.role ?: "USER"
        } catch (e: Exception) {
            "USER"
        }
    }
    
    /**
     * Check if user is admin by email (fallback method)
     */
    fun isAdminEmail(email: String): Boolean {
        return ADMIN_EMAILS.contains(email.lowercase())
    }
    
    /**
     * Update user role locally and sync to Firestore
     */
    suspend fun updateUserRole(userId: String, newRole: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: return@withContext false
            
            val updatedUser = user.copy(
                role = newRole,
                lastSyncTime = System.currentTimeMillis()
            )
            
            // Update local database
            userDao.updateUser(updatedUser)
            
            // Update Firestore
            val userData = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "role" to newRole,
                "isActive" to user.isActive,
                "createdAt" to user.createdAt,
                "lastSyncTime" to System.currentTimeMillis()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userData)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Initialize user with proper role based on email
     */
    suspend fun initializeUserRole(user: User): User = withContext(Dispatchers.IO) {
        try {
            // Check if user should be admin based on email
            val shouldBeAdmin = isAdminEmail(user.email)
            val roleFromFirestore = getUserRoleFromFirestore(user.uid)
            
            val finalRole = when {
                roleFromFirestore != null -> roleFromFirestore
                shouldBeAdmin -> "ADMIN"
                else -> "USER"
            }
            
            val updatedUser = user.copy(role = finalRole)
            
            // Update local database
            userDao.updateUser(updatedUser)
            
            // Sync to Firestore if role changed
            if (finalRole != "USER") {
                syncUserToFirestore(updatedUser)
            }
            
            updatedUser
        } catch (e: Exception) {
            user
        }
    }
    
    /**
     * Get user role from Firestore
     */
    private suspend fun getUserRoleFromFirestore(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            document.getString("role")
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Sync user data to Firestore
     */
    private suspend fun syncUserToFirestore(user: User) = withContext(Dispatchers.IO) {
        try {
            val userData = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "role" to user.role,
                "isActive" to user.isActive,
                "createdAt" to user.createdAt,
                "lastSyncTime" to user.lastSyncTime
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userData)
                .await()
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    /**
     * Get all admin users
     */
    suspend fun getAllAdminUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            userDao.getAllUsers().first().filter { it.isAdmin() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Flow to observe if current user has admin access
     */
    fun observeAdminAccess(userId: String): Flow<Boolean> {
        return userDao.getUserFlow(userId).map { user ->
            user?.hasAdminAccess() ?: false
        }
    }
    
    /**
     * Promote user to admin (only callable by existing admin)
     */
    suspend fun promoteToAdmin(adminUserId: String, targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if the requesting user is admin
            if (!hasAdminAccess(adminUserId)) {
                return@withContext false
            }
            
            // Update target user role
            updateUserRole(targetUserId, "ADMIN")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Demote admin to regular user (only callable by existing admin)
     */
    suspend fun demoteFromAdmin(adminUserId: String, targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if the requesting user is admin
            if (!hasAdminAccess(adminUserId)) {
                return@withContext false
            }
            
            // Prevent self-demotion
            if (adminUserId == targetUserId) {
                return@withContext false
            }
            
            // Update target user role
            updateUserRole(targetUserId, "USER")
        } catch (e: Exception) {
            false
        }
    }
}
