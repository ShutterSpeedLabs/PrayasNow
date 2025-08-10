package com.example.prayasnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val role: String = "USER", // USER, ADMIN, MODERATOR
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTime: Long = System.currentTimeMillis()
) {
    fun isAdmin(): Boolean = role == "ADMIN"
    fun isModerator(): Boolean = role == "MODERATOR" || role == "ADMIN"
    fun hasAdminAccess(): Boolean = isAdmin() && isActive
} 