package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val key: String, // e.g., "quiz_last_sync", "quiz_version"
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)