package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "user_quiz_attempts")
@TypeConverters(QuizProgressConverter::class)
data class UserQuizAttempt(
    @PrimaryKey val id: String, // userId_quizId_attemptNumber
    val userId: String,
    val quizId: Long, // Reference to Quiz.id
    val subjectId: String, // Reference to Subject.id for easier querying
    val attemptNumber: Int = 1,
    val userAnswer: String = "", // User's selected answer
    val isCorrect: Boolean = false,
    val timeSpent: Long = 0, // Time spent on this quiz in milliseconds
    val completed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val syncedToFirebase: Boolean = false
)
