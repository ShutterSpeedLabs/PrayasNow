package com.example.prayasnow.repository

import com.example.prayasnow.data.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseQuizRepository(
    private val firestore: FirebaseFirestore,
    private val localQuizDao: QuizDao,
    private val localSubjectDao: SubjectDao,
    private val localUserQuizAttemptDao: UserQuizAttemptDao
) {
    
    companion object {
        private const val QUIZZES_COLLECTION = "shared_quizzes"
        private const val SUBJECTS_COLLECTION = "subjects"
        private const val USER_PROGRESS_COLLECTION = "user_quiz_progress"
    }
    
    // Sync shared quizzes from Firebase to local database
    suspend fun syncQuizzesFromFirebase(): Result<Int> {
        return try {
            val snapshot = firestore.collection(QUIZZES_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val quizzes = snapshot.documents.mapNotNull { doc ->
                try {
                    Quiz(
                        id = 0, // Will be auto-generated locally
                        firebaseId = doc.id,
                        subjectId = doc.getString("subjectId") ?: "",
                        title = doc.getString("title") ?: "",
                        question = doc.getString("question") ?: "",
                        options = (doc.get("options") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        answer = doc.getString("answer") ?: "",
                        explanation = doc.getString("explanation") ?: "",
                        difficulty = doc.getString("difficulty") ?: "MEDIUM",
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        createdBy = doc.getString("createdBy") ?: "system",
                        isActive = doc.getBoolean("isActive") ?: true,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        version = doc.getLong("version") ?: 1,
                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis(),
                        syncStatus = "SYNCED"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            localQuizDao.insertQuizzes(quizzes)
            Result.success(quizzes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Sync subjects from Firebase to local database
    suspend fun syncSubjectsFromFirebase(): Result<Int> {
        return try {
            val snapshot = firestore.collection(SUBJECTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val subjects = snapshot.documents.mapNotNull { doc ->
                try {
                    Subject(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        iconName = doc.getString("iconName") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            localSubjectDao.insertSubjects(subjects)
            Result.success(subjects.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Sync user progress to Firebase
    suspend fun syncUserProgressToFirebase(userId: String): Result<Int> {
        return try {
            val unsyncedAttempts = localUserQuizAttemptDao.getUserAttempts(userId)
                .let { flow ->
                    // Convert Flow to List for this operation
                    // In a real implementation, you might want to collect this differently
                    emptyList<UserQuizAttempt>() // Placeholder - implement proper collection
                }
            
            var syncedCount = 0
            unsyncedAttempts.forEach { attempt ->
                if (!attempt.syncedToFirebase) {
                    val progressData = mapOf(
                        "userId" to attempt.userId,
                        "quizId" to attempt.quizId,
                        "subjectId" to attempt.subjectId,
                        "attemptNumber" to attempt.attemptNumber,
                        "userAnswer" to attempt.userAnswer,
                        "isCorrect" to attempt.isCorrect,
                        "timeSpent" to attempt.timeSpent,
                        "completed" to attempt.completed,
                        "timestamp" to attempt.timestamp
                    )
                    
                    firestore.collection(USER_PROGRESS_COLLECTION)
                        .document(attempt.id)
                        .set(progressData)
                        .await()
                    
                    localUserQuizAttemptDao.updateSyncStatus(attempt.id, true)
                    syncedCount++
                }
            }
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get quizzes by subject from Firebase (for real-time updates)
    suspend fun getQuizzesBySubjectFromFirebase(subjectId: String): Result<List<Quiz>> {
        return try {
            val snapshot = firestore.collection(QUIZZES_COLLECTION)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("isActive", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val quizzes = snapshot.documents.mapNotNull { doc ->
                try {
                    Quiz(
                        id = 0,
                        firebaseId = doc.id,
                        subjectId = doc.getString("subjectId") ?: "",
                        title = doc.getString("title") ?: "",
                        question = doc.getString("question") ?: "",
                        options = (doc.get("options") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        answer = doc.getString("answer") ?: "",
                        explanation = doc.getString("explanation") ?: "",
                        difficulty = doc.getString("difficulty") ?: "MEDIUM",
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        createdBy = doc.getString("createdBy") ?: "system",
                        isActive = doc.getBoolean("isActive") ?: true,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        version = doc.getLong("version") ?: 1,
                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis(),
                        syncStatus = "SYNCED"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(quizzes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload new quiz to Firebase (admin function)
    suspend fun uploadQuizToFirebase(quiz: Quiz): Result<String> {
        return try {
            val quizData = mapOf(
                "subjectId" to quiz.subjectId,
                "title" to quiz.title,
                "question" to quiz.question,
                "options" to quiz.options,
                "answer" to quiz.answer,
                "explanation" to quiz.explanation,
                "difficulty" to quiz.difficulty,
                "tags" to quiz.tags,
                "createdBy" to quiz.createdBy,
                "isActive" to quiz.isActive,
                "timestamp" to quiz.timestamp,
                "version" to quiz.version,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            val docRef = firestore.collection(QUIZZES_COLLECTION).add(quizData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check for quiz updates from Firebase
    suspend fun checkForQuizUpdates(): Result<Boolean> {
        return try {
            val lastSyncTime = localQuizDao.getLastSyncTimestamp() ?: 0
            
            val snapshot = firestore.collection(QUIZZES_COLLECTION)
                .whereGreaterThan("lastUpdated", lastSyncTime)
                .get()
                .await()
            
            Result.success(snapshot.documents.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload all local quizzes to Firebase
    suspend fun uploadAllLocalQuizzesToFirebase(): Result<Int> {
        return try {
            // Get all local quizzes
            val localQuizzes = localQuizDao.getAllActiveQuizzes().let { flow ->
                // For this migration, we'll get a snapshot of current quizzes
                // In a real implementation, you'd collect from the flow properly
                emptyList<Quiz>() // Placeholder - will be replaced with actual data
            }
            
            var uploadedCount = 0
            
            // Upload each quiz to Firebase
            localQuizzes.forEach { quiz ->
                val result = uploadQuizToFirebase(quiz)
                if (result.isSuccess) {
                    // Update local quiz with Firebase ID
                    val firebaseId = result.getOrNull() ?: ""
                    if (firebaseId.isNotEmpty()) {
                        val updatedQuiz = quiz.copy(
                            firebaseId = firebaseId,
                            syncStatus = "SYNCED",
                            lastUpdated = System.currentTimeMillis()
                        )
                        localQuizDao.updateQuiz(updatedQuiz)
                        uploadedCount++
                        println("✅ Uploaded quiz: ${quiz.question.take(50)}...")
                    }
                } else {
                    println("❌ Failed to upload quiz: ${quiz.question.take(50)}...")
                }
            }
            
            Result.success(uploadedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload all local subjects to Firebase
    suspend fun uploadAllLocalSubjectsToFirebase(): Result<Int> {
        return try {
            // Get all local subjects
            val localSubjects = localSubjectDao.getAllSubjects().let { flow ->
                // For this migration, we'll get a snapshot of current subjects
                // In a real implementation, you'd collect from the flow properly
                emptyList<Subject>() // Placeholder - will be replaced with actual data
            }
            
            var uploadedCount = 0
            
            // Upload each subject to Firebase
            localSubjects.forEach { subject ->
                val subjectData = mapOf(
                    "name" to subject.name,
                    "description" to subject.description,
                    "iconName" to subject.iconName,
                    "isActive" to subject.isActive,
                    "createdAt" to subject.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                try {
                    firestore.collection(SUBJECTS_COLLECTION)
                        .document(subject.id)
                        .set(subjectData)
                        .await()
                    
                    uploadedCount++
                    println("✅ Uploaded subject: ${subject.name}")
                } catch (e: Exception) {
                    println("❌ Failed to upload subject ${subject.name}: ${e.message}")
                }
            }
            
            Result.success(uploadedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
