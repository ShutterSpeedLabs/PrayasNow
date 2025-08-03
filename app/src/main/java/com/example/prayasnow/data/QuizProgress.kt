package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_progress")
data class QuizProgress(
    @PrimaryKey val id: String, // userId_subject_attemptNumber
    val userId: String,
    val subject: String,
    val attemptNumber: Int = 1,
    val score: Int,
    val totalQuestions: Int,
    val questionsAttempted: Int,
    val currentQuestionIndex: Int,
    val userAnswers: Map<Int, String>,
    val completed: Boolean,
    val timestamp: Long,
    val syncedToFirebase: Boolean = false,
    val bestScore: Int = score // Track best score across attempts
)