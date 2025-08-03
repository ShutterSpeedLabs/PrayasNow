package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "quizzes")
@TypeConverters(QuizOptionsConverter::class)
data class Quiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firebaseId: String = "", // Unique ID from Firebase
    val userId: String,
    val title: String,
    val subject: String, // Science, History, Geography, Maths
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val explanation: String = "", // Brief explanation about why this answer is correct
    val attempted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Long = 1, // Version number for tracking updates
    val lastUpdated: Long = System.currentTimeMillis(), // Last update timestamp from Firebase
    val syncStatus: String = "SYNCED" // SYNCED, PENDING, NEW
)