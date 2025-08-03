package com.example.prayasnow.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.QuizProgress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProgressRepository(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    
    // Check if internet is available
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
    
    // Save progress locally with enhanced offline support and multiple attempts
    suspend fun saveProgressLocally(
        userId: String,
        subject: String,
        score: Int,
        totalQuestions: Int,
        questionsAttempted: Int,
        currentQuestionIndex: Int,
        userAnswers: Map<Int, String>,
        completed: Boolean,
        attemptNumber: Int = 1
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Get best score from previous attempts
                val bestScore = database.quizProgressDao().getBestScore(userId, subject) ?: 0
                val currentBestScore = maxOf(bestScore, score)
                
                val progress = QuizProgress(
                    id = "${userId}_${subject}_${attemptNumber}",
                    userId = userId,
                    subject = subject,
                    attemptNumber = attemptNumber,
                    score = score,
                    totalQuestions = totalQuestions,
                    questionsAttempted = questionsAttempted,
                    currentQuestionIndex = currentQuestionIndex,
                    userAnswers = userAnswers,
                    completed = completed,
                    timestamp = System.currentTimeMillis(),
                    syncedToFirebase = false,
                    bestScore = currentBestScore
                )
                
                database.quizProgressDao().insertProgress(progress)
                println("✅ Progress saved locally: $subject (Attempt $attemptNumber) - Score: $score/$totalQuestions, Best: $currentBestScore")
                
                // Try to sync to Firebase if internet is available
                if (isInternetAvailable()) {
                    syncToFirebase(progress)
                } else {
                    println("📡 No internet connection - progress saved locally only. Will sync when online.")
                }
                
            } catch (e: Exception) {
                println("❌ Error saving progress locally: ${e.message}")
            }
        }
    }
    
    // Load latest progress from local storage
    suspend fun loadProgressLocally(userId: String, subject: String): QuizProgress? {
        return withContext(Dispatchers.IO) {
            try {
                val progress = database.quizProgressDao().getLatestProgress(userId, subject)
                if (progress != null) {
                    println("📥 Loaded latest progress from local storage: $subject (Attempt ${progress.attemptNumber}) - Score: ${progress.score}/${progress.totalQuestions}")
                } else {
                    println("📥 No local progress found for $subject")
                }
                progress
            } catch (e: Exception) {
                println("❌ Error loading progress locally: ${e.message}")
                null
            }
        }
    }
    
    // Get next attempt number for a quiz
    suspend fun getNextAttemptNumber(userId: String, subject: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val maxAttempt = database.quizProgressDao().getMaxAttemptNumber(userId, subject) ?: 0
                maxAttempt + 1
            } catch (e: Exception) {
                println("❌ Error getting next attempt number: ${e.message}")
                1
            }
        }
    }
    
    // Check if user can reattempt (if they have completed at least one attempt)
    suspend fun canReattempt(userId: String, subject: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val allProgress = database.quizProgressDao().getAllProgressForSubject(userId, subject)
                allProgress.any { it.completed }
            } catch (e: Exception) {
                println("❌ Error checking reattempt eligibility: ${e.message}")
                false
            }
        }
    }
    
    // Enhanced Firebase sync with better error handling
    private suspend fun syncToFirebase(progress: QuizProgress) {
        try {
            val progressData = hashMapOf(
                "userId" to progress.userId,
                "subject" to progress.subject,
                "attemptNumber" to progress.attemptNumber,
                "score" to progress.score,
                "totalQuestions" to progress.totalQuestions,
                "questionsAttempted" to progress.questionsAttempted,
                "currentQuestionIndex" to progress.currentQuestionIndex,
                "userAnswers" to progress.userAnswers,
                "completed" to progress.completed,
                "timestamp" to progress.timestamp,
                "bestScore" to progress.bestScore,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            // Use await for better error handling
            firestore.collection("quiz_progress")
                .document(progress.id)
                .set(progressData)
                .await()
            
            // Mark as synced in local database
            database.quizProgressDao().markAsSynced(progress.id)
            println("✅ Progress synced to Firebase: ${progress.subject} - Score: ${progress.score}/${progress.totalQuestions}")
            
        } catch (e: Exception) {
            println("❌ Failed to sync to Firebase: ${e.message}")
            // Keep syncedToFirebase as false so it will retry later
        }
    }
    
    // Enhanced sync all unsynced progress to Firebase
    suspend fun syncAllUnsyncedProgress() {
        withContext(Dispatchers.IO) {
            if (!isInternetAvailable()) {
                println("📡 No internet connection - skipping sync")
                return@withContext
            }
            
            try {
                val unsyncedProgress = database.quizProgressDao().getUnsyncedProgress()
                if (unsyncedProgress.isNotEmpty()) {
                    println("🔄 Syncing ${unsyncedProgress.size} unsynced progress items...")
                    var syncedCount = 0
                    unsyncedProgress.forEach { progress ->
                        try {
                            syncToFirebase(progress)
                            syncedCount++
                        } catch (e: Exception) {
                            println("❌ Failed to sync progress ${progress.id}: ${e.message}")
                        }
                    }
                    println("✅ Successfully synced $syncedCount/${unsyncedProgress.size} progress items")
                } else {
                    println("✅ All progress already synced")
                }
            } catch (e: Exception) {
                println("❌ Error syncing unsynced progress: ${e.message}")
            }
        }
    }
    
    // Load progress from Firebase and merge with local data
    suspend fun loadProgressFromFirebase(userId: String, subject: String): QuizProgress? {
        return withContext(Dispatchers.IO) {
            if (!isInternetAvailable()) {
                println("📡 No internet connection - loading from local storage only")
                return@withContext loadProgressLocally(userId, subject)
            }
            
            try {
                // Load latest attempt from Firebase
                val documents = firestore.collection("quiz_progress")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("subject", subject)
                    .orderBy("attemptNumber", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val data = document.data
                    val firebaseProgress = QuizProgress(
                        id = document.id,
                        userId = data?.get("userId") as? String ?: userId,
                        subject = data?.get("subject") as? String ?: subject,
                        attemptNumber = (data?.get("attemptNumber") as? Long)?.toInt() ?: 1,
                        score = (data?.get("score") as? Long)?.toInt() ?: 0,
                        totalQuestions = (data?.get("totalQuestions") as? Long)?.toInt() ?: 0,
                        questionsAttempted = (data?.get("questionsAttempted") as? Long)?.toInt() ?: 0,
                        currentQuestionIndex = (data?.get("currentQuestionIndex") as? Long)?.toInt() ?: 0,
                        userAnswers = (data?.get("userAnswers") as? Map<String, String>)?.mapKeys { it.key.toIntOrNull() ?: 0 }?.filterKeys { it != 0 } ?: emptyMap(),
                        completed = data?.get("completed") as? Boolean ?: false,
                        timestamp = data?.get("timestamp") as? Long ?: System.currentTimeMillis(),
                        syncedToFirebase = true,
                        bestScore = (data?.get("bestScore") as? Long)?.toInt() ?: 0
                    )
                    
                    // Save to local database
                    database.quizProgressDao().insertProgress(firebaseProgress)
                    println("✅ Loaded progress from Firebase: $subject (Attempt ${firebaseProgress.attemptNumber}) - Score: ${firebaseProgress.score}/${firebaseProgress.totalQuestions}")
                    return@withContext firebaseProgress
                } else {
                    println("📥 No Firebase progress found for $subject, checking local storage")
                    return@withContext loadProgressLocally(userId, subject)
                }
            } catch (e: Exception) {
                println("❌ Error loading progress from Firebase: ${e.message}")
                return@withContext loadProgressLocally(userId, subject)
            }
        }
    }
    
    // Get all progress for a user
    suspend fun getAllProgressForUser(userId: String): List<QuizProgress> {
        return withContext(Dispatchers.IO) {
            try {
                database.quizProgressDao().getAllProgressForUser(userId)
            } catch (e: Exception) {
                println("❌ Error getting all progress: ${e.message}")
                emptyList()
            }
        }
    }
    
    // Get best score for a specific quiz
    suspend fun getBestScore(userId: String, subject: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                database.quizProgressDao().getBestScore(userId, subject) ?: 0
            } catch (e: Exception) {
                println("❌ Error getting best score: ${e.message}")
                0
            }
        }
    }
} 