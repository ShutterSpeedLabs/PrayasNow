package com.example.prayasnow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserQuizAttemptDao {
    
    @Query("SELECT * FROM user_quiz_attempts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserAttempts(userId: String): Flow<List<UserQuizAttempt>>
    
    @Query("SELECT * FROM user_quiz_attempts WHERE userId = :userId AND subjectId = :subjectId ORDER BY timestamp DESC")
    fun getUserAttemptsBySubject(userId: String, subjectId: String): Flow<List<UserQuizAttempt>>
    
    @Query("SELECT * FROM user_quiz_attempts WHERE userId = :userId AND quizId = :quizId ORDER BY attemptNumber DESC")
    fun getUserAttemptsForQuiz(userId: String, quizId: Long): Flow<List<UserQuizAttempt>>
    
    @Query("SELECT * FROM user_quiz_attempts WHERE userId = :userId AND quizId = :quizId AND attemptNumber = :attemptNumber")
    suspend fun getUserQuizAttempt(userId: String, quizId: Long, attemptNumber: Int): UserQuizAttempt?
    
    @Query("SELECT COUNT(*) FROM user_quiz_attempts WHERE userId = :userId AND isCorrect = 1")
    suspend fun getUserCorrectAnswersCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_quiz_attempts WHERE userId = :userId")
    suspend fun getUserTotalAttemptsCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_quiz_attempts WHERE userId = :userId AND subjectId = :subjectId AND isCorrect = 1")
    suspend fun getUserCorrectAnswersBySubject(userId: String, subjectId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_quiz_attempts WHERE userId = :userId AND subjectId = :subjectId")
    suspend fun getUserTotalAttemptsBySubject(userId: String, subjectId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: UserQuizAttempt)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempts(attempts: List<UserQuizAttempt>)
    
    @Update
    suspend fun updateAttempt(attempt: UserQuizAttempt)
    
    @Delete
    suspend fun deleteAttempt(attempt: UserQuizAttempt)
    
    @Query("DELETE FROM user_quiz_attempts WHERE userId = :userId")
    suspend fun deleteAllUserAttempts(userId: String)
    
    @Query("UPDATE user_quiz_attempts SET syncedToFirebase = :synced WHERE id = :attemptId")
    suspend fun updateSyncStatus(attemptId: String, synced: Boolean)
}
