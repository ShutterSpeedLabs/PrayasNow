package com.example.prayasnow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class, Quiz::class, Test::class, BucketItem::class, LoginCredentials::class, QuizProgress::class, SyncMetadata::class, Subject::class, UserQuizAttempt::class],
    version = 11,
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
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun subjectDao(): SubjectDao
    abstract fun userQuizAttemptDao(): UserQuizAttemptDao
    
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
        
        // Migration from version 7 to 8 - adds Firebase sync fields to quizzes table
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add Firebase sync columns to quizzes table
                    database.execSQL("ALTER TABLE quizzes ADD COLUMN firebaseId TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE quizzes ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE quizzes ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE quizzes ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                    
                    println("✅ Database migration 7->8 completed successfully")
                } catch (e: Exception) {
                    println("❌ Database migration 7->8 failed: ${e.message}")
                    // If migration fails, the fallback will handle it
                    throw e
                }
            }
        }
        
        // Migration from version 8 to 9 - adds sync_metadata table
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create sync_metadata table
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS sync_metadata (
                            key TEXT PRIMARY KEY NOT NULL,
                            value TEXT NOT NULL,
                            timestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                    
                    println("✅ Database migration 8->9 completed successfully")
                } catch (e: Exception) {
                    println("❌ Database migration 8->9 failed: ${e.message}")
                    // If migration fails, the fallback will handle it
                    throw e
                }
            }
        }
        
        // Migration from version 9 to 10 - restructure for shared quizzes
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create subjects table
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS subjects (
                            id TEXT PRIMARY KEY NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT NOT NULL DEFAULT '',
                            iconName TEXT NOT NULL DEFAULT '',
                            isActive INTEGER NOT NULL DEFAULT 1,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                    
                    // Create user_quiz_attempts table
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS user_quiz_attempts (
                            id TEXT PRIMARY KEY NOT NULL,
                            userId TEXT NOT NULL,
                            quizId INTEGER NOT NULL,
                            subjectId TEXT NOT NULL,
                            attemptNumber INTEGER NOT NULL DEFAULT 1,
                            userAnswer TEXT NOT NULL DEFAULT '',
                            isCorrect INTEGER NOT NULL DEFAULT 0,
                            timeSpent INTEGER NOT NULL DEFAULT 0,
                            completed INTEGER NOT NULL DEFAULT 0,
                            timestamp INTEGER NOT NULL,
                            syncedToFirebase INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                    
                    // Create new quizzes table with updated structure
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS quizzes_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            firebaseId TEXT NOT NULL DEFAULT '',
                            subjectId TEXT NOT NULL,
                            title TEXT NOT NULL,
                            question TEXT NOT NULL DEFAULT '',
                            options TEXT NOT NULL DEFAULT '[]',
                            answer TEXT NOT NULL DEFAULT '',
                            explanation TEXT NOT NULL DEFAULT '',
                            difficulty TEXT NOT NULL DEFAULT 'MEDIUM',
                            tags TEXT NOT NULL DEFAULT '[]',
                            createdBy TEXT NOT NULL DEFAULT 'system',
                            isActive INTEGER NOT NULL DEFAULT 1,
                            timestamp INTEGER NOT NULL,
                            version INTEGER NOT NULL DEFAULT 1,
                            lastUpdated INTEGER NOT NULL,
                            syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                        )
                    """.trimIndent())
                    
                    // Migrate existing quiz data (convert subject string to subjectId)
                    database.execSQL("""
                        INSERT INTO quizzes_new (id, firebaseId, subjectId, title, question, options, answer, explanation, timestamp, version, lastUpdated, syncStatus)
                        SELECT id, firebaseId, 
                               CASE 
                                   WHEN LOWER(subject) = 'science' THEN 'science'
                                   WHEN LOWER(subject) = 'history' THEN 'history'
                                   WHEN LOWER(subject) = 'geography' THEN 'geography'
                                   WHEN LOWER(subject) = 'maths' OR LOWER(subject) = 'mathematics' THEN 'maths'
                                   ELSE LOWER(subject)
                               END as subjectId,
                               title, question, options, answer, explanation, timestamp, version, lastUpdated, syncStatus
                        FROM quizzes
                    """)
                    
                    // Drop old quizzes table and rename new one
                    database.execSQL("DROP TABLE quizzes")
                    database.execSQL("ALTER TABLE quizzes_new RENAME TO quizzes")
                    
                    // Insert default subjects
                    val currentTime = System.currentTimeMillis()
                    database.execSQL("""
                        INSERT OR REPLACE INTO subjects (id, name, description, iconName, isActive, createdAt, updatedAt) VALUES
                        ('science', 'Science', 'Physics, Chemistry, Biology questions', 'science', 1, $currentTime, $currentTime),
                        ('history', 'History', 'Historical events and figures', 'history', 1, $currentTime, $currentTime),
                        ('geography', 'Geography', 'World geography and locations', 'geography', 1, $currentTime, $currentTime),
                        ('maths', 'Mathematics', 'Mathematical problems and concepts', 'math', 1, $currentTime, $currentTime)
                    """)
                    
                    println("✅ Database migration 9->10 completed successfully")
                } catch (e: Exception) {
                    println("❌ Database migration 9->10 failed: ${e.message}")
                    // If migration fails, the fallback will handle it
                    throw e
                }
            }
        }
        
        // Migration from version 10 to 11 - add role fields to User table
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add new columns to users table
                    database.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'")
                    database.execSQL("ALTER TABLE users ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE users ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                    
                    println("✅ Migration 10->11: Added role fields to users table")
                } catch (e: Exception) {
                    println("❌ Migration 10->11 failed: ${e.message}")
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
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration() // This will recreate the database if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}