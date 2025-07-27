package com.example.prayasnow.repository

import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.User
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
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                syncUserDataToLocal(user)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
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
} 