package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "quizzes")
@TypeConverters(QuizOptionsConverter::class)
data class Quiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firebaseId: String = "", // Unique ID from Firebase
    val subjectId: String, // Reference to Subject.id (e.g., "science", "history")
    val title: String,
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val explanation: String = "", // Brief explanation about why this answer is correct
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val tags: List<String> = emptyList(), // Topics/categories for filtering
    val createdBy: String = "system", // Who created this quiz (system, admin, etc.)
    val isActive: Boolean = true, // Whether quiz is available for users
    val timestamp: Long = System.currentTimeMillis(),
    val version: Long = 1, // Version number for tracking updates
    val lastUpdated: Long = System.currentTimeMillis(), // Last update timestamp from Firebase
    val syncStatus: String = "SYNCED" // SYNCED, PENDING, NEW
)