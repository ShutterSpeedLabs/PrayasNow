package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tests")
data class Test(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val attempted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) 