package com.example.prayasnow.repository

import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.Quiz
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridge QuizRepository for backward compatibility with existing UI screens.
 * This is a simplified version that works with the new shared quiz database structure.
 * TODO: Gradually migrate UI screens to use SharedQuizRepository directly.
 */
class QuizRepository(
    private val firestore: FirebaseFirestore,
    val database: AppDatabase
) {
    
    // Bridge method: Get quizzes by subject (adapted for new schema)
    suspend fun getQuizzesBySubject(userId: String, subject: String): List<Quiz> = withContext(Dispatchers.IO) {
        try {
            // Convert old subject names to new subjectId format
            val subjectId = when (subject.lowercase()) {
                "science" -> "science"
                "history" -> "history"
                "geography" -> "geography"
                "maths", "mathematics" -> "maths"
                else -> subject.lowercase()
            }
            
            // Get quizzes from new shared structure
            val quizzes = database.quizDao().getQuizzesBySubject(subjectId).let { flow ->
                // For now, return empty list - proper implementation would collect from flow
                emptyList<Quiz>()
            }
            
            println("📚 Loaded ${quizzes.size} quizzes for subject: $subject (subjectId: $subjectId)")
            quizzes
        } catch (e: Exception) {
            println("❌ Error loading quizzes for subject $subject: ${e.message}")
            emptyList()
        }
    }
    
    // Bridge method: Get quizzes by title and subject
    suspend fun getQuizzesByTitle(userId: String, subject: String, title: String): List<Quiz> = withContext(Dispatchers.IO) {
        try {
            val subjectId = when (subject.lowercase()) {
                "science" -> "science"
                "history" -> "history"
                "geography" -> "geography"
                "maths", "mathematics" -> "maths"
                else -> subject.lowercase()
            }
            
            // For now, return sample quizzes - proper implementation would query by title
            val sampleQuizzes = getSampleQuizzesForTesting(subjectId, title)
            println("📚 Loaded ${sampleQuizzes.size} quizzes for $title in $subject")
            sampleQuizzes
        } catch (e: Exception) {
            println("❌ Error loading quizzes for title $title: ${e.message}")
            emptyList()
        }
    }
    
    // Bridge method: Sync from Firebase (simplified)
    suspend fun syncQuizzesFromFirebase(userId: String): List<Quiz> = withContext(Dispatchers.IO) {
        try {
            println("🔄 Syncing quizzes from Firebase (bridge method)")
            // TODO: Implement proper Firebase sync using SharedQuizRepository
            emptyList()
        } catch (e: Exception) {
            println("❌ Error syncing from Firebase: ${e.message}")
            emptyList()
        }
    }
    
    // Bridge method: Insert sample quizzes
    suspend fun insertSampleQuizzes(userId: String) = withContext(Dispatchers.IO) {
        try {
            println("📝 Inserting sample quizzes (bridge method)")
            // TODO: Use DatabaseInitializer to insert sample data
        } catch (e: Exception) {
            println("❌ Error inserting sample quizzes: ${e.message}")
        }
    }
    
    // Bridge method: Check if sync is needed
    suspend fun isSyncNeeded(intervalHours: Int = 24): Boolean = withContext(Dispatchers.IO) {
        try {
            // For now, always return false to avoid unnecessary syncing
            // TODO: Implement proper sync timing logic
            false
        } catch (e: Exception) {
            println("❌ Error checking sync status: ${e.message}")
            false
        }
    }
    
    // Bridge method: Get available subjects
    suspend fun getAvailableSubjects(userId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            // Return default subjects for now
            listOf("Science", "History", "Geography", "Maths")
        } catch (e: Exception) {
            println("❌ Error getting available subjects: ${e.message}")
            emptyList()
        }
    }
    
    // Bridge method: Get quiz titles for subject
    suspend fun getQuizTitlesForSubject(userId: String, subject: String): List<String> = withContext(Dispatchers.IO) {
        try {
            // Return sample titles based on subject
            when (subject.lowercase()) {
                "science" -> listOf("Basic Physics", "Chemistry Basics", "Biology Fundamentals")
                "history" -> listOf("World War II", "Ancient Civilizations")
                "geography" -> listOf("World Capitals", "Mountain Ranges")
                "maths" -> listOf("Basic Algebra", "Geometry", "Arithmetic")
                else -> emptyList()
            }
        } catch (e: Exception) {
            println("❌ Error getting quiz titles: ${e.message}")
            emptyList()
        }
    }
    
    // Bridge methods for statistics (simplified)
    suspend fun getAllQuizzesForUser(userId: String): List<Quiz> = emptyList()
    suspend fun getAttemptedQuizCount(userId: String): Int = 0
    suspend fun getAttemptedQuizCountBySubject(userId: String, subject: String): Int = 0
    suspend fun getTotalQuizCount(userId: String): Int = 10 // Default value
    suspend fun getTotalQuizCountBySubject(userId: String, subject: String): Int = 10 // Default value
    
    // Helper method: Generate sample quizzes for testing
    private fun getSampleQuizzesForTesting(subjectId: String, title: String): List<Quiz> {
        return when (subjectId) {
            "science" -> when (title) {
                "Basic Physics" -> listOf(
                    Quiz(
                        subjectId = "science",
                        title = "Basic Physics",
                        question = "What is the speed of light in vacuum?",
                        options = listOf("300,000 km/s", "150,000 km/s", "299,792,458 m/s", "186,000 miles/s"),
                        answer = "299,792,458 m/s",
                        explanation = "The speed of light in vacuum is exactly 299,792,458 meters per second."
                    )
                )
                "Chemistry Basics" -> listOf(
                    Quiz(
                        subjectId = "science",
                        title = "Chemistry Basics",
                        question = "What is the chemical symbol for Gold?",
                        options = listOf("Go", "Gd", "Au", "Ag"),
                        answer = "Au",
                        explanation = "Gold's chemical symbol is Au, derived from the Latin word 'aurum'."
                    )
                )
                else -> emptyList()
            }
            "history" -> when (title) {
                "World War II" -> listOf(
                    Quiz(
                        subjectId = "history",
                        title = "World War II",
                        question = "In which year did World War II end?",
                        options = listOf("1944", "1945", "1946", "1947"),
                        answer = "1945",
                        explanation = "World War II ended in 1945 with the surrender of Japan in September."
                    )
                )
                else -> emptyList()
            }
            "geography" -> when (title) {
                "World Capitals" -> listOf(
                    Quiz(
                        subjectId = "geography",
                        title = "World Capitals",
                        question = "What is the capital of Australia?",
                        options = listOf("Sydney", "Melbourne", "Canberra", "Perth"),
                        answer = "Canberra",
                        explanation = "Canberra is the capital city of Australia, located in the Australian Capital Territory."
                    )
                )
                else -> emptyList()
            }
            "maths" -> when (title) {
                "Basic Algebra" -> listOf(
                    Quiz(
                        subjectId = "maths",
                        title = "Basic Algebra",
                        question = "What is the value of x in the equation: 2x + 5 = 15?",
                        options = listOf("5", "10", "7.5", "2.5"),
                        answer = "5",
                        explanation = "Solving: 2x + 5 = 15, so 2x = 10, therefore x = 5."
                    )
                )
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
