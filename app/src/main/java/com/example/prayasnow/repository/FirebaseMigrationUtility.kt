package com.example.prayasnow.repository

import com.example.prayasnow.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Utility class to migrate local quiz data to Cloud Firestore
 */
class FirebaseMigrationUtility(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    companion object {
        private const val QUIZZES_COLLECTION = "shared_quizzes"
        private const val SUBJECTS_COLLECTION = "subjects"
        private const val USER_PROGRESS_COLLECTION = "user_quiz_progress"
    }
    
    /**
     * Push all local quizzes to Cloud Firestore
     */
    suspend fun pushLocalQuizzesToFirestore(): Result<MigrationResult> = withContext(Dispatchers.IO) {
        try {
            println("🚀 Starting migration of local quizzes to Firestore...")
            
            // First, initialize sample data if database is empty
            val existingQuizCount = database.quizDao().getTotalActiveQuizCount()
            if (existingQuizCount == 0) {
                println("📝 No local quizzes found. Initializing sample data first...")
                val initializer = DatabaseInitializer(database)
                initializer.initializeWithSampleData()
            }
            
            // Step 1: Migrate subjects
            val subjectsResult = migrateSubjects()
            println("📂 Subjects migration: ${subjectsResult.uploadedCount} uploaded, ${subjectsResult.failedCount} failed")
            
            // Step 2: Migrate quizzes
            val quizzesResult = migrateQuizzes()
            println("📚 Quizzes migration: ${quizzesResult.uploadedCount} uploaded, ${quizzesResult.failedCount} failed")
            
            val totalResult = MigrationResult(
                uploadedCount = subjectsResult.uploadedCount + quizzesResult.uploadedCount,
                failedCount = subjectsResult.failedCount + quizzesResult.failedCount,
                details = "Subjects: ${subjectsResult.uploadedCount}/${subjectsResult.uploadedCount + subjectsResult.failedCount}, " +
                         "Quizzes: ${quizzesResult.uploadedCount}/${quizzesResult.uploadedCount + quizzesResult.failedCount}"
            )
            
            println("✅ Migration completed! Total: ${totalResult.uploadedCount} uploaded, ${totalResult.failedCount} failed")
            Result.success(totalResult)
            
        } catch (e: Exception) {
            println("❌ Migration failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Migrate subjects to Firestore
     */
    private suspend fun migrateSubjects(): MigrationResult {
        return try {
            val subjects = database.subjectDao().getAllSubjects().first()
            var uploadedCount = 0
            var failedCount = 0
            
            subjects.forEach { subject ->
                try {
                    val subjectData = mapOf(
                        "name" to subject.name,
                        "description" to subject.description,
                        "iconName" to subject.iconName,
                        "isActive" to subject.isActive,
                        "createdAt" to subject.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    
                    firestore.collection(SUBJECTS_COLLECTION)
                        .document(subject.id)
                        .set(subjectData)
                        .await()
                    
                    uploadedCount++
                    println("✅ Uploaded subject: ${subject.name}")
                    
                } catch (e: Exception) {
                    failedCount++
                    println("❌ Failed to upload subject ${subject.name}: ${e.message}")
                }
            }
            
            MigrationResult(uploadedCount, failedCount, "Subjects migration completed")
            
        } catch (e: Exception) {
            println("❌ Error migrating subjects: ${e.message}")
            MigrationResult(0, 1, "Subjects migration failed: ${e.message}")
        }
    }
    
    /**
     * Migrate quizzes to Firestore
     */
    private suspend fun migrateQuizzes(): MigrationResult {
        return try {
            val quizzes = database.quizDao().getAllActiveQuizzes().first()
            var uploadedCount = 0
            var failedCount = 0
            
            quizzes.forEach { quiz ->
                try {
                    // Skip if already synced to Firebase
                    if (quiz.firebaseId.isNotEmpty() && quiz.syncStatus == "SYNCED") {
                        println("⏭️ Skipping already synced quiz: ${quiz.question.take(50)}...")
                        return@forEach
                    }
                    
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
                    
                    val docRef = firestore.collection(QUIZZES_COLLECTION)
                        .add(quizData)
                        .await()
                    
                    // Update local quiz with Firebase ID
                    val updatedQuiz = quiz.copy(
                        firebaseId = docRef.id,
                        syncStatus = "SYNCED",
                        lastUpdated = System.currentTimeMillis()
                    )
                    database.quizDao().updateQuiz(updatedQuiz)
                    
                    uploadedCount++
                    println("✅ Uploaded quiz: ${quiz.question.take(50)}... (ID: ${docRef.id})")
                    
                } catch (e: Exception) {
                    failedCount++
                    println("❌ Failed to upload quiz: ${quiz.question.take(50)}... Error: ${e.message}")
                }
            }
            
            MigrationResult(uploadedCount, failedCount, "Quizzes migration completed")
            
        } catch (e: Exception) {
            println("❌ Error migrating quizzes: ${e.message}")
            MigrationResult(0, 1, "Quizzes migration failed: ${e.message}")
        }
    }
    
    /**
     * Check Firestore connection and permissions
     */
    suspend fun testFirestoreConnection(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Try to read from a collection to test connection
            val testDoc = firestore.collection("test")
                .limit(1)
                .get()
                .await()
            
            Result.success("✅ Firestore connection successful. Can read collections.")
        } catch (e: Exception) {
            Result.failure(Exception("❌ Firestore connection failed: ${e.message}"))
        }
    }
    
    /**
     * Get migration status
     */
    suspend fun getMigrationStatus(): MigrationStatus = withContext(Dispatchers.IO) {
        try {
            val totalLocalQuizzes = database.quizDao().getTotalActiveQuizCount()
            val syncedQuizzes = database.quizDao().getUnsyncedQuizzes().let { 
                totalLocalQuizzes - it.size
            }
            val totalLocalSubjects = database.subjectDao().getAllSubjects().first().size
            
            // Check Firestore counts
            val firestoreQuizCount = try {
                firestore.collection(QUIZZES_COLLECTION).get().await().size()
            } catch (e: Exception) {
                0
            }
            
            val firestoreSubjectCount = try {
                firestore.collection(SUBJECTS_COLLECTION).get().await().size()
            } catch (e: Exception) {
                0
            }
            
            MigrationStatus(
                localQuizCount = totalLocalQuizzes,
                localSubjectCount = totalLocalSubjects,
                firestoreQuizCount = firestoreQuizCount,
                firestoreSubjectCount = firestoreSubjectCount,
                syncedQuizCount = syncedQuizzes,
                needsMigration = syncedQuizzes < totalLocalQuizzes || firestoreSubjectCount < totalLocalSubjects
            )
            
        } catch (e: Exception) {
            println("❌ Error getting migration status: ${e.message}")
            MigrationStatus(0, 0, 0, 0, 0, true)
        }
    }
}

/**
 * Result of migration operation
 */
data class MigrationResult(
    val uploadedCount: Int,
    val failedCount: Int,
    val details: String
)

/**
 * Status of migration
 */
data class MigrationStatus(
    val localQuizCount: Int,
    val localSubjectCount: Int,
    val firestoreQuizCount: Int,
    val firestoreSubjectCount: Int,
    val syncedQuizCount: Int,
    val needsMigration: Boolean
)
