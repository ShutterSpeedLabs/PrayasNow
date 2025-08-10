package com.example.prayasnow.repository

import com.example.prayasnow.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for admin operations - adding and modifying shared content
 */
class AdminRepository(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    companion object {
        private const val SUBJECTS_COLLECTION = "subjects"
        private const val QUIZZES_COLLECTION = "shared_quizzes"
    }
    
    // ==================== SUBJECT OPERATIONS ====================
    
    /**
     * Get all subjects for admin management
     */
    fun getAllSubjects(): Flow<List<Subject>> {
        return database.subjectDao().getAllSubjects()
    }
    
    /**
     * Add a new subject to both local database and Firestore
     */
    suspend fun addSubject(subjectForm: SubjectForm): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            val subject = subjectForm.toSubject()
            
            // Add to local database first
            database.subjectDao().insertSubject(subject)
            
            // Add to Firestore
            val subjectData = mapOf(
                "name" to subject.name,
                "description" to subject.description,
                "iconName" to subject.iconName,
                "isActive" to subject.isActive,
                "createdAt" to subject.createdAt,
                "updatedAt" to subject.updatedAt
            )
            
            firestore.collection(SUBJECTS_COLLECTION)
                .document(subject.id)
                .set(subjectData)
                .await()
            
            AdminOperationResult(
                success = true,
                message = "Subject '${subject.name}' added successfully",
                data = subject
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Failed to add subject: ${e.message}"
            )
        }
    }
    
    /**
     * Update an existing subject in both local database and Firestore
     */
    suspend fun updateSubject(subjectForm: SubjectForm): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            val subject = subjectForm.toSubject().copy(
                updatedAt = System.currentTimeMillis()
            )
            
            // Update local database
            database.subjectDao().updateSubject(subject)
            
            // Update Firestore
            val subjectData = mapOf(
                "name" to subject.name,
                "description" to subject.description,
                "iconName" to subject.iconName,
                "isActive" to subject.isActive,
                "createdAt" to subject.createdAt,
                "updatedAt" to subject.updatedAt
            )
            
            firestore.collection(SUBJECTS_COLLECTION)
                .document(subject.id)
                .set(subjectData)
                .await()
            
            AdminOperationResult(
                success = true,
                message = "Subject '${subject.name}' updated successfully",
                data = subject
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Failed to update subject: ${e.message}"
            )
        }
    }
    
    /**
     * Delete a subject (mark as inactive)
     */
    suspend fun deleteSubject(subjectId: String): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            // Get the subject first
            val subjects = database.subjectDao().getAllSubjects().first()
            val subject = subjects.find { it.id == subjectId }
                ?: return@withContext AdminOperationResult(false, "Subject not found")
            
            // Mark as inactive locally
            val updatedSubject = subject.copy(isActive = false, updatedAt = System.currentTimeMillis())
            database.subjectDao().updateSubject(updatedSubject)
            
            // Update Firestore
            firestore.collection(SUBJECTS_COLLECTION)
                .document(subjectId)
                .update("isActive", false, "updatedAt", System.currentTimeMillis())
                .await()
            
            AdminOperationResult(
                success = true,
                message = "Subject '${subject.name}' deleted successfully"
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Failed to delete subject: ${e.message}"
            )
        }
    }
    
    // ==================== QUIZ OPERATIONS ====================
    
    /**
     * Get all quizzes for a specific subject
     */
    fun getQuizzesBySubject(subjectId: String): Flow<List<Quiz>> {
        return database.quizDao().getQuizzesBySubject(subjectId)
    }
    
    /**
     * Get all active quizzes
     */
    fun getAllQuizzes(): Flow<List<Quiz>> {
        return database.quizDao().getAllActiveQuizzes()
    }
    
    /**
     * Add a new quiz to both local database and Firestore
     */
    suspend fun addQuiz(quizForm: QuizForm): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            val quiz = quizForm.toQuiz()
            
            // Add to local database first
            val quizId = database.quizDao().insertQuiz(quiz)
            val savedQuiz = quiz.copy(id = quizId)
            
            // Add to Firestore
            val quizData = mapOf(
                "subjectId" to savedQuiz.subjectId,
                "title" to savedQuiz.title,
                "question" to savedQuiz.question,
                "options" to savedQuiz.options,
                "answer" to savedQuiz.answer,
                "explanation" to savedQuiz.explanation,
                "difficulty" to savedQuiz.difficulty,
                "tags" to savedQuiz.tags,
                "createdBy" to savedQuiz.createdBy,
                "isActive" to savedQuiz.isActive,
                "timestamp" to savedQuiz.timestamp,
                "version" to savedQuiz.version,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            val docRef = firestore.collection(QUIZZES_COLLECTION)
                .add(quizData)
                .await()
            
            // Update local quiz with Firebase ID
            val updatedQuiz = savedQuiz.copy(
                firebaseId = docRef.id,
                syncStatus = "SYNCED"
            )
            database.quizDao().updateQuiz(updatedQuiz)
            
            AdminOperationResult(
                success = true,
                message = "Quiz added successfully",
                data = updatedQuiz
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Failed to add quiz: ${e.message}"
            )
        }
    }
    
    /**
     * Update an existing quiz in both local database and Firestore
     */
    suspend fun updateQuiz(quizForm: QuizForm): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            val quiz = quizForm.toQuiz().copy(
                lastUpdated = System.currentTimeMillis(),
                version = quizForm.toQuiz().version + 1,
                syncStatus = "PENDING"
            )
            
            // Update local database
            database.quizDao().updateQuiz(quiz)
            
            // Update Firestore if it has a Firebase ID
            if (quiz.firebaseId.isNotEmpty()) {
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
                    "lastUpdated" to quiz.lastUpdated
                )
                
                firestore.collection(QUIZZES_COLLECTION)
                    .document(quiz.firebaseId)
                    .set(quizData)
                    .await()
                
                // Mark as synced
                database.quizDao().updateQuiz(quiz.copy(syncStatus = "SYNCED"))
            }
            
            AdminOperationResult(
                success = true,
                message = "Quiz updated successfully",
                data = quiz
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Failed to update quiz: ${e.message}"
            )
        }
    }
    
    /**
     * Delete a quiz (mark as inactive)
     */
    suspend fun deleteQuiz(quizId: Long): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            // Get the quiz first
            val quizzes = database.quizDao().getAllActiveQuizzes().first()
            val quiz = quizzes.find { it.id == quizId }
                ?: return@withContext AdminOperationResult(false, "Quiz not found")
            
            // Mark as inactive locally
            val updatedQuiz = quiz.copy(
                isActive = false,
                lastUpdated = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
            database.quizDao().updateQuiz(updatedQuiz)
            
            // Update Firestore if it has a Firebase ID
            if (quiz.firebaseId.isNotEmpty()) {
                firestore.collection(QUIZZES_COLLECTION)
                    .document(quiz.firebaseId)
                    .update(
                        "isActive", false,
                        "lastUpdated", System.currentTimeMillis()
                    )
                    .await()
                
                // Mark as synced
                database.quizDao().updateQuiz(updatedQuiz.copy(syncStatus = "SYNCED"))
            }
            
            AdminOperationResult(
                success = true,
                message = "Quiz deleted successfully"
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Failed to delete quiz: ${e.message}"
            )
        }
    }
    
    /**
     * Get quiz by ID for editing
     */
    suspend fun getQuizById(quizId: Long): Quiz? = withContext(Dispatchers.IO) {
        try {
            val quizzes = database.quizDao().getAllActiveQuizzes().first()
            quizzes.find { it.id == quizId }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get subject by ID for editing
     */
    suspend fun getSubjectById(subjectId: String): Subject? = withContext(Dispatchers.IO) {
        try {
            val subjects = database.subjectDao().getAllSubjects().first()
            subjects.find { it.id == subjectId }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Bulk sync all pending changes to Firestore
     */
    suspend fun syncPendingChanges(): AdminOperationResult = withContext(Dispatchers.IO) {
        try {
            val pendingQuizzes = database.quizDao().getUnsyncedQuizzes()
            var syncedCount = 0
            var failedCount = 0
            
            pendingQuizzes.forEach { quiz ->
                try {
                    if (quiz.firebaseId.isNotEmpty()) {
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
                            "lastUpdated" to quiz.lastUpdated
                        )
                        
                        firestore.collection(QUIZZES_COLLECTION)
                            .document(quiz.firebaseId)
                            .set(quizData)
                            .await()
                        
                        database.quizDao().updateQuiz(quiz.copy(syncStatus = "SYNCED"))
                        syncedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                }
            }
            
            AdminOperationResult(
                success = true,
                message = "Sync completed: $syncedCount synced, $failedCount failed"
            )
        } catch (e: Exception) {
            AdminOperationResult(
                success = false,
                message = "Sync failed: ${e.message}"
            )
        }
    }
}
