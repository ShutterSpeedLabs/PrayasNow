package com.example.prayasnow.data

import androidx.room.*

@Dao
interface BucketItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucketItem(bucketItem: BucketItem)

    @Query("SELECT * FROM bucket_items WHERE userId = :userId")
    suspend fun getAllBucketItemsForUser(userId: String): List<BucketItem>

    @Query("SELECT COUNT(*) FROM bucket_items WHERE userId = :userId")
    suspend fun getBucketItemCount(userId: String): Int
} 