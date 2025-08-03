package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "login_credentials")
data class LoginCredentials(
    @PrimaryKey val id: Int = 1, // Only one record
    val emailOrUsername: String,
    val password: String,
    val rememberMe: Boolean = true
) 