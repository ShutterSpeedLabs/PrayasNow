package com.example.prayasnow.repository

import android.content.Context
import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.DatabaseInitializer
import com.example.prayasnow.viewmodel.SharedQuizViewModel
import com.example.prayasnow.viewmodel.AdminViewModel
import com.example.prayasnow.auth.AuthService
import com.example.prayasnow.auth.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object RepositoryFactory {
    
    fun createSharedQuizRepository(context: Context): SharedQuizRepository {
        val database = AppDatabase.getDatabase(context)
        return SharedQuizRepository(
            quizDao = database.quizDao(),
            subjectDao = database.subjectDao(),
            userQuizAttemptDao = database.userQuizAttemptDao()
        )
    }
    
    fun createFirebaseQuizRepository(context: Context): FirebaseQuizRepository {
        val database = AppDatabase.getDatabase(context)
        return FirebaseQuizRepository(
            firestore = FirebaseFirestore.getInstance(),
            localQuizDao = database.quizDao(),
            localSubjectDao = database.subjectDao(),
            localUserQuizAttemptDao = database.userQuizAttemptDao()
        )
    }
    
    fun createDatabaseInitializer(context: Context): DatabaseInitializer {
        val database = AppDatabase.getDatabase(context)
        return DatabaseInitializer(database)
    }
    
    fun createSharedQuizViewModel(context: Context): SharedQuizViewModel {
        val sharedQuizRepository = createSharedQuizRepository(context)
        val firebaseQuizRepository = createFirebaseQuizRepository(context)
        val databaseInitializer = createDatabaseInitializer(context)
        
        return SharedQuizViewModel(
            sharedQuizRepository = sharedQuizRepository,
            firebaseQuizRepository = firebaseQuizRepository,
            databaseInitializer = databaseInitializer
        )
    }
    
    fun createMigrationUtility(context: Context): FirebaseMigrationUtility {
        val database = AppDatabase.getDatabase(context)
        return FirebaseMigrationUtility(database)
    }
    
    fun createAdminRepository(context: Context): AdminRepository {
        val database = AppDatabase.getDatabase(context)
        return AdminRepository(
            database = database,
            firestore = FirebaseFirestore.getInstance()
        )
    }
    
    fun createRoleManager(context: Context): RoleManager {
        val database = AppDatabase.getDatabase(context)
        return RoleManager(
            userDao = database.userDao(),
            firestore = FirebaseFirestore.getInstance()
        )
    }
    
    fun createAuthService(context: Context): AuthService {
        val database = AppDatabase.getDatabase(context)
        val roleManager = createRoleManager(context)
        return AuthService(
            userDao = database.userDao(),
            roleManager = roleManager,
            firebaseAuth = FirebaseAuth.getInstance()
        )
    }
    
    fun createAdminViewModel(context: Context): AdminViewModel {
        val adminRepository = createAdminRepository(context)
        val authService = createAuthService(context)
        return AdminViewModel(
            adminRepository = adminRepository,
            authService = authService
        )
    }
    
    // Simple function to push local quizzes to Firestore
    suspend fun pushLocalQuizzesToFirestore(context: Context): String {
        return try {
            val migrationUtility = createMigrationUtility(context)
            
            println("🚀 Testing Firestore connection...")
            val connectionResult = migrationUtility.testFirestoreConnection()
            if (connectionResult.isFailure) {
                return "❌ Firestore connection failed: ${connectionResult.exceptionOrNull()?.message}"
            }
            
            println("✅ Firestore connection successful. Starting migration...")
            val result = migrationUtility.pushLocalQuizzesToFirestore()
            
            if (result.isSuccess) {
                val migrationResult = result.getOrNull()
                "✅ Migration completed!\n" +
                "📊 Uploaded: ${migrationResult?.uploadedCount}\n" +
                "❌ Failed: ${migrationResult?.failedCount}\n" +
                "📋 Details: ${migrationResult?.details}"
            } else {
                "❌ Migration failed: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            "❌ Migration error: ${e.message}"
        }
    }
    
    // Get migration status
    suspend fun getMigrationStatus(context: Context): String {
        return try {
            val migrationUtility = createMigrationUtility(context)
            val status = migrationUtility.getMigrationStatus()
            
            "📊 Migration Status:\n" +
            "📱 Local Quizzes: ${status.localQuizCount}\n" +
            "📱 Local Subjects: ${status.localSubjectCount}\n" +
            "☁️ Firestore Quizzes: ${status.firestoreQuizCount}\n" +
            "☁️ Firestore Subjects: ${status.firestoreSubjectCount}\n" +
            "✅ Synced Quizzes: ${status.syncedQuizCount}\n" +
            "🔄 Needs Migration: ${if (status.needsMigration) "Yes" else "No"}"
        } catch (e: Exception) {
            "❌ Error getting status: ${e.message}"
        }
    }
}
