package com.example.prayasnow.data

import androidx.room.*

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): SyncMetadata?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: SyncMetadata)
    
    @Query("UPDATE sync_metadata SET value = :value, timestamp = :timestamp WHERE key = :key")
    suspend fun updateMetadata(key: String, value: String, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteMetadata(metadata: SyncMetadata)
    
    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun deleteMetadataByKey(key: String)
}