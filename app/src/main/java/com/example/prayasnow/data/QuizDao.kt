package com.example.prayasnow.data

import androidx.room.*

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes WHERE userId = :userId")
    suspend fun getAllQuizzesForUser(userId: String): List<Quiz>

    @Query("SELECT * FROM quizzes WHERE userId = :userId AND subject = :subject")
    suspend fun getQuizzesBySubject(userId: String, subject: String): List<Quiz>

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId")
    suspend fun getTotalQuizCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId AND subject = :subject")
    suspend fun getTotalQuizCountBySubject(userId: String, subject: String): Int

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId AND attempted = 1")
    suspend fun getAttemptedQuizCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId AND subject = :subject AND attempted = 1")
    suspend fun getAttemptedQuizCountBySubject(userId: String, subject: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: Quiz)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<Quiz>)

    @Update
    suspend fun updateQuiz(quiz: Quiz)

    @Delete
    suspend fun deleteQuiz(quiz: Quiz)

    @Query("DELETE FROM quizzes WHERE userId = :userId")
    suspend fun deleteAllQuizzesForUser(userId: String)

    @Query("SELECT DISTINCT subject FROM quizzes WHERE userId = :userId")
    suspend fun getAvailableSubjects(userId: String): List<String>
} 