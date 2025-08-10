package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String, // e.g., "science", "history", "geography", "maths"
    val name: String, // Display name e.g., "Science", "History"
    val description: String = "",
    val iconName: String = "", // Icon identifier for UI
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
