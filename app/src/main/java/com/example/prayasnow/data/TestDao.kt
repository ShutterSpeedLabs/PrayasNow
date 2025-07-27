package com.example.prayasnow.data

import androidx.room.*

@Dao
interface TestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: Test)

    @Query("SELECT * FROM tests WHERE userId = :userId")
    suspend fun getAllTestsForUser(userId: String): List<Test>

    @Query("SELECT COUNT(*) FROM tests WHERE userId = :userId AND attempted = 1")
    suspend fun getAttemptedTestCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM tests WHERE userId = :userId")
    suspend fun getTotalTestCount(userId: String): Int
} 