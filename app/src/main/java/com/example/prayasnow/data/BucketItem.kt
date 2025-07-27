package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bucket_items")
data class BucketItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val itemType: String, // e.g., "quiz", "test", "other"
    val itemId: Long,
    val addedTimestamp: Long = System.currentTimeMillis()
) 