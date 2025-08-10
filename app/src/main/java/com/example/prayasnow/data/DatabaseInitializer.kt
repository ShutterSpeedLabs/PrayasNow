package com.example.prayasnow.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseInitializer(
    private val database: AppDatabase
) {
    
    suspend fun initializeWithSampleData() = withContext(Dispatchers.IO) {
        try {
            // Initialize subjects first
            initializeSubjects()
            
            // Initialize sample quizzes
            initializeSampleQuizzes()
            
            println("✅ Database initialized with sample data successfully")
        } catch (e: Exception) {
            println("❌ Failed to initialize database with sample data: ${e.message}")
        }
    }
    
    private suspend fun initializeSubjects() {
        val subjects = listOf(
            Subject(
                id = "science",
                name = "Science",
                description = "Physics, Chemistry, Biology questions",
                iconName = "science",
                isActive = true
            ),
            Subject(
                id = "history",
                name = "History",
                description = "Historical events and figures",
                iconName = "history",
                isActive = true
            ),
            Subject(
                id = "geography",
                name = "Geography",
                description = "World geography and locations",
                iconName = "geography",
                isActive = true
            ),
            Subject(
                id = "maths",
                name = "Mathematics",
                description = "Mathematical problems and concepts",
                iconName = "math",
                isActive = true
            )
        )
        
        database.subjectDao().insertSubjects(subjects)
    }
    
    private suspend fun initializeSampleQuizzes() {
        val sampleQuizzes = listOf(
            // Science Quizzes
            Quiz(
                subjectId = "science",
                title = "Basic Physics",
                question = "What is the speed of light in vacuum?",
                options = listOf("300,000 km/s", "150,000 km/s", "299,792,458 m/s", "186,000 miles/s"),
                answer = "299,792,458 m/s",
                explanation = "The speed of light in vacuum is exactly 299,792,458 meters per second.",
                difficulty = "MEDIUM",
                tags = listOf("physics", "light", "constants")
            ),
            Quiz(
                subjectId = "science",
                title = "Chemistry Basics",
                question = "What is the chemical symbol for Gold?",
                options = listOf("Go", "Gd", "Au", "Ag"),
                answer = "Au",
                explanation = "Gold's chemical symbol is Au, derived from the Latin word 'aurum'.",
                difficulty = "EASY",
                tags = listOf("chemistry", "elements", "periodic table")
            ),
            Quiz(
                subjectId = "science",
                title = "Biology Fundamentals",
                question = "Which organelle is known as the powerhouse of the cell?",
                options = listOf("Nucleus", "Mitochondria", "Ribosome", "Endoplasmic Reticulum"),
                answer = "Mitochondria",
                explanation = "Mitochondria produce ATP, the energy currency of cells, earning them the title 'powerhouse of the cell'.",
                difficulty = "EASY",
                tags = listOf("biology", "cell", "organelles")
            ),
            
            // History Quizzes
            Quiz(
                subjectId = "history",
                title = "World War II",
                question = "In which year did World War II end?",
                options = listOf("1944", "1945", "1946", "1947"),
                answer = "1945",
                explanation = "World War II ended in 1945 with the surrender of Japan in September.",
                difficulty = "EASY",
                tags = listOf("world war", "20th century", "global history")
            ),
            Quiz(
                subjectId = "history",
                title = "Ancient Civilizations",
                question = "Which ancient wonder of the world was located in Alexandria?",
                options = listOf("Hanging Gardens", "Lighthouse of Alexandria", "Colossus of Rhodes", "Temple of Artemis"),
                answer = "Lighthouse of Alexandria",
                explanation = "The Lighthouse of Alexandria (Pharos of Alexandria) was one of the Seven Wonders of the Ancient World.",
                difficulty = "MEDIUM",
                tags = listOf("ancient history", "wonders", "alexandria")
            ),
            
            // Geography Quizzes
            Quiz(
                subjectId = "geography",
                title = "World Capitals",
                question = "What is the capital of Australia?",
                options = listOf("Sydney", "Melbourne", "Canberra", "Perth"),
                answer = "Canberra",
                explanation = "Canberra is the capital city of Australia, located in the Australian Capital Territory.",
                difficulty = "MEDIUM",
                tags = listOf("capitals", "australia", "cities")
            ),
            Quiz(
                subjectId = "geography",
                title = "Mountain Ranges",
                question = "Which is the longest mountain range in the world?",
                options = listOf("Himalayas", "Rocky Mountains", "Andes", "Alps"),
                answer = "Andes",
                explanation = "The Andes mountain range in South America is the longest continental mountain range in the world.",
                difficulty = "MEDIUM",
                tags = listOf("mountains", "south america", "physical geography")
            ),
            
            // Mathematics Quizzes
            Quiz(
                subjectId = "maths",
                title = "Basic Algebra",
                question = "What is the value of x in the equation: 2x + 5 = 15?",
                options = listOf("5", "10", "7.5", "2.5"),
                answer = "5",
                explanation = "Solving: 2x + 5 = 15, so 2x = 10, therefore x = 5.",
                difficulty = "EASY",
                tags = listOf("algebra", "equations", "solving")
            ),
            Quiz(
                subjectId = "maths",
                title = "Geometry",
                question = "What is the area of a circle with radius 5 units? (Use π ≈ 3.14)",
                options = listOf("78.5 sq units", "31.4 sq units", "15.7 sq units", "62.8 sq units"),
                answer = "78.5 sq units",
                explanation = "Area of circle = πr² = 3.14 × 5² = 3.14 × 25 = 78.5 square units.",
                difficulty = "MEDIUM",
                tags = listOf("geometry", "circle", "area")
            ),
            Quiz(
                subjectId = "maths",
                title = "Number Theory",
                question = "Which of the following is a prime number?",
                options = listOf("15", "21", "17", "25"),
                answer = "17",
                explanation = "17 is a prime number as it has only two factors: 1 and 17.",
                difficulty = "EASY",
                tags = listOf("prime numbers", "number theory", "factors")
            )
        )
        
        database.quizDao().insertQuizzes(sampleQuizzes)
    }
    
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            println("✅ All database data cleared successfully")
        } catch (e: Exception) {
            println("❌ Failed to clear database data: ${e.message}")
        }
    }
}
