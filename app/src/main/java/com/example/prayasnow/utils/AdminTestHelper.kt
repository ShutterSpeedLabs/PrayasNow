package com.example.prayasnow.utils

import android.content.Context
import android.util.Log
import com.example.prayasnow.repository.RepositoryFactory
import com.example.prayasnow.viewmodel.AdminViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class to test admin functionality
 * Call these functions from your MainActivity or any activity to test the admin features
 */
object AdminTestHelper {
    
    private const val TAG = "AdminTest"
    
    /**
     * Test function to create and initialize admin components
     * Call this from your MainActivity onCreate or from a button click
     */
    fun testAdminSetup(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🔧 Testing admin setup...")
        
        scope.launch {
            try {
                val repositoryFactory = RepositoryFactory
                
                // Create admin repository and viewmodel
                val adminRepository = repositoryFactory.createAdminRepository(context)
                val adminViewModel = repositoryFactory.createAdminViewModel(context)
                
                Log.d(TAG, "✅ Admin components created successfully")
                Log.d(TAG, "📊 Admin repository: ${adminRepository.javaClass.simpleName}")
                Log.d(TAG, "📊 Admin viewmodel: ${adminViewModel.javaClass.simpleName}")
                
                // Test basic functionality
                adminRepository.getAllSubjects().collect { subjects ->
                    Log.d(TAG, "📚 Found ${subjects.size} subjects in database")
                    subjects.forEach { subject ->
                        Log.d(TAG, "  - ${subject.name}: ${subject.description}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Admin setup test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Test adding a sample subject
     */
    fun testAddSampleSubject(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "➕ Testing add sample subject...")
        
        scope.launch {
            try {
                val adminRepository = RepositoryFactory.createAdminRepository(context)
                
                val sampleSubject = com.example.prayasnow.data.SubjectForm(
                    name = "Test Subject ${System.currentTimeMillis()}",
                    description = "This is a test subject created by AdminTestHelper",
                    iconName = "test",
                    isActive = true
                )
                
                val result = adminRepository.addSubject(sampleSubject)
                
                if (result.success) {
                    Log.d(TAG, "✅ ${result.message}")
                } else {
                    Log.e(TAG, "❌ ${result.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Add subject test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Test adding a sample quiz
     */
    fun testAddSampleQuiz(context: Context, subjectId: String, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "➕ Testing add sample quiz...")
        
        scope.launch {
            try {
                val adminRepository = RepositoryFactory.createAdminRepository(context)
                
                val sampleQuiz = com.example.prayasnow.data.QuizForm(
                    subjectId = subjectId,
                    title = "Test Quiz ${System.currentTimeMillis()}",
                    question = "What is the capital of France?",
                    options = listOf("London", "Berlin", "Paris", "Madrid"),
                    answer = "Paris",
                    explanation = "Paris is the capital and largest city of France.",
                    difficulty = "EASY",
                    tags = listOf("geography", "capitals", "france"),
                    isActive = true
                )
                
                val result = adminRepository.addQuiz(sampleQuiz)
                
                if (result.success) {
                    Log.d(TAG, "✅ ${result.message}")
                } else {
                    Log.e(TAG, "❌ ${result.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Add quiz test failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Initialize admin with sample data
     */
    fun initializeAdminWithSampleData(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        Log.d(TAG, "🔄 Initializing admin with sample data...")
        
        scope.launch {
            try {
                // First ensure we have sample data in the database
                MigrationTestHelper.initializeAndMigrate(context, scope)
                
                // Then test admin functionality
                testAdminSetup(context, scope)
                
                Log.d(TAG, "✅ Admin initialization completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Admin initialization failed: ${e.message}", e)
            }
        }
    }
}

/**
 * Extension functions to easily call admin tests from any Activity
 * Usage in MainActivity:
 * 
 * import com.example.prayasnow.utils.testAdminSetup
 * 
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     
 *     // Test admin functionality
 *     this.testAdminSetup()
 * }
 */
fun Context.testAdminSetup() {
    AdminTestHelper.testAdminSetup(this)
}

fun Context.testAddSampleSubject() {
    AdminTestHelper.testAddSampleSubject(this)
}

fun Context.testAddSampleQuiz(subjectId: String) {
    AdminTestHelper.testAddSampleQuiz(this, subjectId)
}

fun Context.initializeAdminWithSampleData() {
    AdminTestHelper.initializeAdminWithSampleData(this)
}
