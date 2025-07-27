package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "quizzes")
@TypeConverters(QuizOptionsConverter::class)
data class Quiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val attempted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) 