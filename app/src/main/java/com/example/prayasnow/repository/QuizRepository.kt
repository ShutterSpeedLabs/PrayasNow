package com.example.prayasnow.repository

import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.data.Quiz
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class QuizRepository(
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase
) {
    suspend fun fetchQuizzesFromFirebase(userId: String): List<Quiz> = withContext(Dispatchers.IO) {
        val quizList = mutableListOf<Quiz>()
        try {
            val snapshot = firestore.collection("quizzes")
                .whereEqualTo("subject", "science")
                .whereEqualTo("source", "NCERT")
                .get().await()
            for (doc in snapshot.documents) {
                val quiz = Quiz(
                    id = 0, // Room will auto-generate
                    userId = userId,
                    title = doc.getString("title") ?: "",
                    question = doc.getString("question") ?: "",
                    options = (doc.get("options") as? List<*>)?.map { it.toString() } ?: emptyList(),
                    answer = doc.getString("answer") ?: "",
                    attempted = false,
                    timestamp = System.currentTimeMillis()
                )
                quizList.add(quiz)
                database.quizDao().insertQuiz(quiz)
            }
        } catch (e: Exception) {
            // Handle error (log or propagate)
        }
        quizList
    }

    suspend fun insertSampleQuizzes(userId: String) = withContext(Dispatchers.IO) {
        val sampleQuizzes = listOf(
            Quiz(
                userId = userId,
                title = "NCERT Science: Water Cycle",
                question = "What is the process by which water changes from a liquid to a gas?",
                options = listOf("Condensation", "Evaporation", "Precipitation", "Transpiration"),
                answer = "Evaporation"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Human Body",
                question = "Which organ pumps blood throughout the body?",
                options = listOf("Liver", "Heart", "Lungs", "Kidney"),
                answer = "Heart"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Plants",
                question = "What is the green pigment in plants called?",
                options = listOf("Chlorophyll", "Hemoglobin", "Melanin", "Keratin"),
                answer = "Chlorophyll"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Food Chain",
                question = "What is the primary source of energy in a food chain?",
                options = listOf("Animals", "Plants", "Sun", "Water"),
                answer = "Sun"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: States of Matter",
                question = "Which state of matter has a definite shape and volume?",
                options = listOf("Solid", "Liquid", "Gas", "Plasma"),
                answer = "Solid"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Electricity",
                question = "What is the unit of electric current?",
                options = listOf("Volt", "Ampere", "Watt", "Ohm"),
                answer = "Ampere"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Light",
                question = "What type of lens is used to correct nearsightedness?",
                options = listOf("Convex", "Concave", "Bifocal", "Progressive"),
                answer = "Concave"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Sound",
                question = "What is the unit of frequency?",
                options = listOf("Decibel", "Hertz", "Watt", "Meter"),
                answer = "Hertz"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Force",
                question = "What is the SI unit of force?",
                options = listOf("Joule", "Newton", "Pascal", "Watt"),
                answer = "Newton"
            ),
            Quiz(
                userId = userId,
                title = "NCERT Science: Energy",
                question = "Which form of energy is stored in food?",
                options = listOf("Kinetic", "Potential", "Chemical", "Nuclear"),
                answer = "Chemical"
            )
        )
        sampleQuizzes.forEach { database.quizDao().insertQuiz(it) }
    }

    suspend fun getAllQuizzesForUser(userId: String): List<Quiz> =
        database.quizDao().getAllQuizzesForUser(userId)

    suspend fun getAttemptedQuizCount(userId: String): Int =
        database.quizDao().getAttemptedQuizCount(userId)

    suspend fun getTotalQuizCount(userId: String): Int =
        database.quizDao().getTotalQuizCount(userId)
} 