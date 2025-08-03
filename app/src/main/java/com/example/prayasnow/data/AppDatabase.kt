package com.example.prayasnow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class, Quiz::class, Test::class, BucketItem::class, LoginCredentials::class, QuizProgress::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(QuizOptionsConverter::class, QuizProgressConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun quizDao(): QuizDao
    abstract fun testDao(): TestDao
    abstract fun bucketItemDao(): BucketItemDao
    abstract fun loginCredentialsDao(): LoginCredentialsDao
    abstract fun quizProgressDao(): QuizProgressDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 5 to 6 - adds attemptNumber and bestScore fields
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add new columns to quiz_progress table
                    database.execSQL("ALTER TABLE quiz_progress ADD COLUMN attemptNumber INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE quiz_progress ADD COLUMN bestScore INTEGER NOT NULL DEFAULT 0")
                    
                    // Update bestScore to match current score for existing records
                    database.execSQL("UPDATE quiz_progress SET bestScore = score")
                    
                    println("✅ Database migration 5->6 completed successfully")
                } catch (e: Exception) {
                    println("❌ Database migration 5->6 failed: ${e.message}")
                    // If migration fails, the fallback will handle it
                    throw e
                }
            }
        }
        
        // Migration from version 6 to 7 - adds explanation field to quizzes table
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add explanation column to quizzes table
                    database.execSQL("ALTER TABLE quizzes ADD COLUMN explanation TEXT NOT NULL DEFAULT ''")
                    
                    println("✅ Database migration 6->7 completed successfully")
                } catch (e: Exception) {
                    println("❌ Database migration 6->7 failed: ${e.message}")
                    // If migration fails, the fallback will handle it
                    throw e
                }
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prayasnow_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration() // This will recreate the database if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}