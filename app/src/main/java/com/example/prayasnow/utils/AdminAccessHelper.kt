package com.example.prayasnow.utils

import android.content.Context
import android.util.Log
import com.example.prayasnow.repository.RepositoryFactory
import com.example.prayasnow.auth.AuthService
import com.example.prayasnow.auth.RoleManager
import com.example.prayasnow.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class to test and manage admin access control
 * Call these functions to test role-based access and admin functionality
 */
object AdminAccessHelper {
    
    private const val TAG = "AdminAccess"
    
    /**
     * Test admin access control system
     */
    fun testAdminAccessControl(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🔐 Testing admin access control system...")
        
        scope.launch {
            try {
                val authService = RepositoryFactory.createAuthService(context)
                val roleManager = RepositoryFactory.createRoleManager(context)
                
                // Test current user access
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId != null) {
                    val hasAccess = authService.checkAdminAccess()
                    val userRole = authService.getUserRole()
                    
                    Log.d(TAG, "👤 Current User ID: $currentUserId")
                    Log.d(TAG, "🔑 User Role: $userRole")
                    Log.d(TAG, "🚪 Has Admin Access: $hasAccess")
                    
                    if (hasAccess) {
                        Log.d(TAG, "✅ Admin access granted - can access admin panel")
                    } else {
                        Log.d(TAG, "❌ Admin access denied - cannot access admin panel")
                    }
                } else {
                    Log.d(TAG, "❌ No user logged in")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Admin access test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Test making a user admin (for testing purposes)
     */
    fun testPromoteUserToAdmin(context: Context, userEmail: String, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "⬆️ Testing promote user to admin: $userEmail")
        
        scope.launch {
            try {
                val authService = RepositoryFactory.createAuthService(context)
                val roleManager = RepositoryFactory.createRoleManager(context)
                
                // Check if email should be admin
                val shouldBeAdmin = roleManager.isAdminEmail(userEmail)
                Log.d(TAG, "📧 Email $userEmail should be admin: $shouldBeAdmin")
                
                if (shouldBeAdmin) {
                    Log.d(TAG, "✅ Email is in admin list - user will be promoted on next login")
                } else {
                    Log.d(TAG, "ℹ️ Email not in admin list - add to ADMIN_EMAILS in RoleManager if needed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Promote user test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Create a test admin user (for development/testing)
     */
    fun createTestAdminUser(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "👨‍💼 Creating test admin user...")
        
        scope.launch {
            try {
                val database = com.example.prayasnow.data.AppDatabase.getDatabase(context)
                val userDao = database.userDao()
                
                // Create a test admin user
                val testAdmin = User(
                    uid = "test_admin_${System.currentTimeMillis()}",
                    email = "admin@prayasnow.com",
                    displayName = "Test Admin",
                    photoUrl = null,
                    role = "ADMIN",
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    lastSyncTime = System.currentTimeMillis()
                )
                
                userDao.insertUser(testAdmin)
                Log.d(TAG, "✅ Test admin user created: ${testAdmin.email}")
                Log.d(TAG, "🆔 User ID: ${testAdmin.uid}")
                Log.d(TAG, "🔑 Role: ${testAdmin.role}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Create test admin failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * List all users and their roles
     */
    fun listAllUsersAndRoles(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "📋 Listing all users and their roles...")
        
        scope.launch {
            try {
                val database = com.example.prayasnow.data.AppDatabase.getDatabase(context)
                val userDao = database.userDao()
                
                userDao.getAllUsers().collect { users ->
                    Log.d(TAG, "👥 Found ${users.size} users:")
                    users.forEach { user ->
                        Log.d(TAG, "  - ${user.displayName ?: "Unknown"} (${user.email})")
                        Log.d(TAG, "    Role: ${user.role}, Active: ${user.isActive}")
                        Log.d(TAG, "    Admin Access: ${user.hasAdminAccess()}")
                        Log.d(TAG, "    UID: ${user.uid}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ List users failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Test admin screen access with current user
     */
    fun testAdminScreenAccess(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🖥️ Testing admin screen access...")
        
        scope.launch {
            try {
                val adminViewModel = RepositoryFactory.createAdminViewModel(context)
                
                // Observe admin access
                adminViewModel.hasAdminAccess.collect { hasAccess ->
                    if (hasAccess) {
                        Log.d(TAG, "✅ Admin screen access granted")
                        Log.d(TAG, "🎛️ User can access admin panel")
                    } else {
                        Log.d(TAG, "❌ Admin screen access denied")
                        Log.d(TAG, "🚫 User will see access denied screen")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Admin screen access test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Initialize admin system with sample data and test user
     */
    fun initializeAdminSystem(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🚀 Initializing admin system...")
        
        scope.launch {
            try {
                // First initialize sample data
                MigrationTestHelper.initializeAndMigrate(context, scope)
                
                // Create test admin user
                createTestAdminUser(context, scope)
                
                // Test admin access control
                testAdminAccessControl(context, scope)
                
                // List all users
                listAllUsersAndRoles(context, scope)
                
                Log.d(TAG, "✅ Admin system initialization completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Admin system initialization failed: ${e.message}", e)
            }
        }
    }
}

/**
 * Extension functions to easily call admin access tests from any Activity
 * Usage in MainActivity:
 * 
 * import com.example.prayasnow.utils.testAdminAccess
 * 
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     
 *     // Test admin access
 *     this.testAdminAccess()
 * }
 */
fun Context.testAdminAccess() {
    AdminAccessHelper.testAdminAccessControl(this)
}

fun Context.createTestAdmin() {
    AdminAccessHelper.createTestAdminUser(this)
}

fun Context.listUsersAndRoles() {
    AdminAccessHelper.listAllUsersAndRoles(this)
}

fun Context.testAdminScreenAccess() {
    AdminAccessHelper.testAdminScreenAccess(this)
}

fun Context.initializeAdminSystem() {
    AdminAccessHelper.initializeAdminSystem(this)
}
