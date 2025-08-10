package com.example.prayasnow.utils

import android.content.Context
import android.util.Log
import com.example.prayasnow.repository.RepositoryFactory
import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class to test migration functionality
 * Call these functions from your MainActivity or any activity to test the migration
 */
object MigrationTestHelper {
    
    private const val TAG = "MigrationTest"
    
    /**
     * Test function to push local quizzes to Firestore
     * Call this from your MainActivity onCreate or from a button click
     */
    fun testPushQuizzesToFirestore(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🚀 Starting quiz migration test...")
        
        scope.launch {
            try {
                // First, get migration status
                val status = RepositoryFactory.getMigrationStatus(context)
                Log.d(TAG, "📊 Migration Status:\n$status")
                
                // Then push quizzes to Firestore
                val result = RepositoryFactory.pushLocalQuizzesToFirestore(context)
                Log.d(TAG, "📤 Migration Result:\n$result")
                
                // Get updated status
                val updatedStatus = RepositoryFactory.getMigrationStatus(context)
                Log.d(TAG, "📊 Updated Migration Status:\n$updatedStatus")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Migration test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Just get migration status without pushing
     */
    fun checkMigrationStatus(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "📊 Checking migration status...")
        
        scope.launch {
            try {
                val status = RepositoryFactory.getMigrationStatus(context)
                Log.d(TAG, "📊 Migration Status:\n$status")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Status check failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Initialize sample data and then push to Firestore
     */
    fun initializeAndMigrate(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🔄 Initializing sample data and migrating...")
        
        scope.launch {
            try {
                // Initialize sample data first
                val database = AppDatabase.getDatabase(context)
                val databaseInitializer = DatabaseInitializer(database)
                withContext(Dispatchers.IO) {
                    databaseInitializer.initializeWithSampleData()
                }
                Log.d(TAG, "✅ Sample data initialized")
                
                // Then migrate to Firestore
                val result = RepositoryFactory.pushLocalQuizzesToFirestore(context)
                Log.d(TAG, "📤 Migration Result:\n$result")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Initialize and migrate failed: ${e.message}", e)
            }
        }
    }
}

/**
 * Extension function to easily call migration from any Activity
 * Usage in MainActivity:
 * 
 * import com.example.prayasnow.utils.testMigration
 * 
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     
 *     // Test migration
 *     this.testMigration()
 * }
 */
fun Context.testMigration() {
    MigrationTestHelper.testPushQuizzesToFirestore(this)
}

fun Context.checkMigrationStatus() {
    MigrationTestHelper.checkMigrationStatus(this)
}

fun Context.initializeAndMigrate() {
    MigrationTestHelper.initializeAndMigrate(this)
}
