package com.example.prayasnow.repository

import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.User
import com.example.prayasnow.data.LoginCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase
) {
    
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                syncUserDataToLocal(user)
                // Store credentials locally
                storeCredentials(email, password)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmailAndPassword(email: String, password: String, name: String, username: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Store user data in Firestore with name and username
                val userData = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "name" to name,
                    "username" to username,
                    "displayName" to name,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userData).await()
                
                syncUserDataToLocal(user)
                storeCredentials(email, password)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithUsernameOrEmail(usernameOrEmail: String, password: String): Result<FirebaseUser> {
        return try {
            val email = if (usernameOrEmail.contains("@")) {
                usernameOrEmail
            } else {
                // Look up email from username in Firestore
                val snapshot = firestore.collection("users")
                    .whereEqualTo("username", usernameOrEmail)
                    .get().await()
                if (snapshot.isEmpty) {
                    throw Exception("Username not found")
                }
                snapshot.documents.first().getString("email") ?: throw Exception("Email not found for username")
            }
            
            signInWithEmailAndPassword(email, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStoredCredentials(): LoginCredentials? {
        return withContext(Dispatchers.IO) {
            database.loginCredentialsDao().getCredentials()
        }
    }
    
    private suspend fun storeCredentials(emailOrUsername: String, password: String) {
        withContext(Dispatchers.IO) {
            val credentials = LoginCredentials(
                emailOrUsername = emailOrUsername,
                password = password,
                rememberMe = true
            )
            database.loginCredentialsDao().insertCredentials(credentials)
        }
    }
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { user ->
                syncUserDataToLocal(user)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut() {
        auth.signOut()
        database.userDao().deleteAllUsers()
        // Clear stored credentials on logout
        withContext(Dispatchers.IO) {
            database.loginCredentialsDao().clearCredentials()
        }
    }
    
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    suspend fun isUserLoggedInLocally(): Boolean {
        return database.userDao().getUserById(auth.currentUser?.uid ?: "") != null
    }
    
    private suspend fun syncUserDataToLocal(firebaseUser: FirebaseUser) {
        withContext(Dispatchers.IO) {
            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            database.userDao().insertUser(user)
            
            // Sync additional data from Firestore if needed
            try {
                val userDoc = firestore.collection("users").document(firebaseUser.uid)
                userDoc.set(user.toMap()).await()
            } catch (e: Exception) {
                // Handle Firestore sync error
            }
        }
    }
    
    private fun User.toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "lastSyncTime" to lastSyncTime
        )
    }

    suspend fun createTestUser(): Result<FirebaseUser> {
        return try {
            val testEmail = "test@prayasnow.com"
            val testPassword = "test123456"
            val testName = "Test User"
            val testUsername = "testuser"
            
            val result = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
            result.user?.let { user ->
                // Store user data in Firestore
                val userData = mapOf(
                    "uid" to user.uid,
                    "email" to testEmail,
                    "name" to testName,
                    "username" to testUsername,
                    "displayName" to testName,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userData).await()
                
                syncUserDataToLocal(user)
                storeCredentials(testEmail, testPassword)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            // If user already exists, try to sign in
            try {
                val signInResult = auth.signInWithEmailAndPassword("test@prayasnow.com", "test123456").await()
                signInResult.user?.let { user ->
                    syncUserDataToLocal(user)
                    storeCredentials("test@prayasnow.com", "test123456")
                }
                Result.success(signInResult.user!!)
            } catch (signInException: Exception) {
                Result.failure(signInException)
            }
        }
    }
} 