package com.example.prayasnow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    // Shared quiz operations (no userId dependency)
    @Query("SELECT * FROM quizzes WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getAllActiveQuizzes(): Flow<List<Quiz>>

    @Query("SELECT * FROM quizzes WHERE subjectId = :subjectId AND isActive = 1 ORDER BY timestamp DESC")
    fun getQuizzesBySubject(subjectId: String): Flow<List<Quiz>>
    
    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    suspend fun getQuizById(quizId: Long): Quiz?
    
    @Query("SELECT * FROM quizzes WHERE subjectId = :subjectId AND difficulty = :difficulty AND isActive = 1")
    fun getQuizzesBySubjectAndDifficulty(subjectId: String, difficulty: String): Flow<List<Quiz>>
    
    @Query("SELECT * FROM quizzes WHERE tags LIKE '%' || :tag || '%' AND isActive = 1")
    fun getQuizzesByTag(tag: String): Flow<List<Quiz>>

    @Query("SELECT COUNT(*) FROM quizzes WHERE isActive = 1")
    suspend fun getTotalActiveQuizCount(): Int

    @Query("SELECT COUNT(*) FROM quizzes WHERE subjectId = :subjectId AND isActive = 1")
    suspend fun getTotalQuizCountBySubject(subjectId: String): Int
    
    @Query("SELECT DISTINCT subjectId FROM quizzes WHERE isActive = 1")
    suspend fun getAvailableSubjects(): List<String>
    
    @Query("SELECT DISTINCT difficulty FROM quizzes WHERE subjectId = :subjectId AND isActive = 1")
    suspend fun getAvailableDifficulties(subjectId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: Quiz): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<Quiz>)

    @Update
    suspend fun updateQuiz(quiz: Quiz)

    @Delete
    suspend fun deleteQuiz(quiz: Quiz)
    
    @Query("UPDATE quizzes SET isActive = :isActive WHERE id = :quizId")
    suspend fun updateQuizActiveStatus(quizId: Long, isActive: Boolean)
    
    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuizById(quizId: Long)
    
    // Firebase sync related queries
    @Query("SELECT * FROM quizzes WHERE firebaseId = :firebaseId")
    suspend fun getQuizByFirebaseId(firebaseId: String): Quiz?
    
    @Query("SELECT MAX(lastUpdated) FROM quizzes")
    suspend fun getLastSyncTimestamp(): Long?
    
    @Query("SELECT * FROM quizzes WHERE syncStatus = 'PENDING' OR syncStatus = 'NEW'")
    suspend fun getUnsyncedQuizzes(): List<Quiz>
    
    @Query("UPDATE quizzes SET syncStatus = 'SYNCED', lastUpdated = :timestamp WHERE firebaseId = :firebaseId")
    suspend fun markQuizAsSynced(firebaseId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE quizzes SET syncStatus = 'PENDING' WHERE id = :quizId")
    suspend fun markQuizForSync(quizId: Long)
    
    @Query("DELETE FROM quizzes WHERE firebaseId = :firebaseId")
    suspend fun deleteQuizByFirebaseId(firebaseId: String)
    
    // Search and filter operations
    @Query("SELECT * FROM quizzes WHERE (title LIKE '%' || :searchQuery || '%' OR question LIKE '%' || :searchQuery || '%') AND isActive = 1")
    fun searchQuizzes(searchQuery: String): Flow<List<Quiz>>
    
    @Query("SELECT * FROM quizzes WHERE subjectId = :subjectId AND (title LIKE '%' || :searchQuery || '%' OR question LIKE '%' || :searchQuery || '%') AND isActive = 1")
    fun searchQuizzesBySubject(subjectId: String, searchQuery: String): Flow<List<Quiz>>
}