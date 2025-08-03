package com.example.prayasnow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizProgressDao {
    @Query("SELECT * FROM quiz_progress WHERE userId = :userId AND subject = :subject ORDER BY attemptNumber DESC LIMIT 1")
    suspend fun getLatestProgress(userId: String, subject: String): QuizProgress?
    
    @Query("SELECT * FROM quiz_progress WHERE userId = :userId AND subject = :subject AND attemptNumber = :attemptNumber")
    suspend fun getProgress(userId: String, subject: String, attemptNumber: Int): QuizProgress?
    
    @Query("SELECT * FROM quiz_progress WHERE userId = :userId AND subject = :subject ORDER BY attemptNumber DESC")
    suspend fun getAllProgressForSubject(userId: String, subject: String): List<QuizProgress>
    
    @Query("SELECT * FROM quiz_progress WHERE userId = :userId")
    suspend fun getAllProgressForUser(userId: String): List<QuizProgress>
    
    @Query("SELECT MAX(attemptNumber) FROM quiz_progress WHERE userId = :userId AND subject = :subject")
    suspend fun getMaxAttemptNumber(userId: String, subject: String): Int?
    
    @Query("SELECT MAX(bestScore) FROM quiz_progress WHERE userId = :userId AND subject = :subject")
    suspend fun getBestScore(userId: String, subject: String): Int?
    
    @Query("SELECT COUNT(DISTINCT subject) FROM quiz_progress WHERE userId = :userId AND completed = 1")
    suspend fun getCompletedQuizCount(userId: String): Int
    
    @Query("SELECT * FROM quiz_progress WHERE syncedToFirebase = 0")
    suspend fun getUnsyncedProgress(): List<QuizProgress>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: QuizProgress)
    
    @Update
    suspend fun updateProgress(progress: QuizProgress)
    
    @Query("UPDATE quiz_progress SET syncedToFirebase = 1 WHERE id = :progressId")
    suspend fun markAsSynced(progressId: String)
    
    @Delete
    suspend fun deleteProgress(progress: QuizProgress)
    
    @Query("DELETE FROM quiz_progress WHERE userId = :userId")
    suspend fun deleteAllProgressForUser(userId: String)
}