package com.example.prayasnow.data

import androidx.room.*

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: Quiz)

    @Query("SELECT * FROM quizzes WHERE userId = :userId")
    suspend fun getAllQuizzesForUser(userId: String): List<Quiz>

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId AND attempted = 1")
    suspend fun getAttemptedQuizCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId")
    suspend fun getTotalQuizCount(userId: String): Int
} 