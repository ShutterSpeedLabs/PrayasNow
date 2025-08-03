package com.example.prayasnow.data

import androidx.room.*

@Dao
interface LoginCredentialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: LoginCredentials)
    
    @Query("SELECT * FROM login_credentials WHERE id = 1")
    suspend fun getCredentials(): LoginCredentials?
    
    @Delete
    suspend fun deleteCredentials(credentials: LoginCredentials)
    
    @Query("DELETE FROM login_credentials")
    suspend fun clearCredentials()
} 