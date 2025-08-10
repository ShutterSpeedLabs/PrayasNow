package com.example.prayasnow.repository

import com.example.prayasnow.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SharedQuizRepository(
    private val quizDao: QuizDao,
    private val subjectDao: SubjectDao,
    private val userQuizAttemptDao: UserQuizAttemptDao
) {
    
    // Subject operations
    fun getAllActiveSubjects(): Flow<List<Subject>> = subjectDao.getAllActiveSubjects()
    
    suspend fun getSubjectById(subjectId: String): Subject? = subjectDao.getSubjectById(subjectId)
    
    suspend fun insertSubjects(subjects: List<Subject>) = subjectDao.insertSubjects(subjects)
    
    // Quiz operations (shared across all users)
    fun getAllActiveQuizzes(): Flow<List<Quiz>> = quizDao.getAllActiveQuizzes()
    
    fun getQuizzesBySubject(subjectId: String): Flow<List<Quiz>> = quizDao.getQuizzesBySubject(subjectId)
    
    suspend fun getQuizById(quizId: Long): Quiz? = quizDao.getQuizById(quizId)
    
    suspend fun insertQuiz(quiz: Quiz): Long = quizDao.insertQuiz(quiz)
    
    suspend fun insertQuizzes(quizzes: List<Quiz>) = quizDao.insertQuizzes(quizzes)
    
    suspend fun updateQuiz(quiz: Quiz) = quizDao.updateQuiz(quiz)
    
    // User progress operations (individual per user)
    fun getUserAttempts(userId: String): Flow<List<UserQuizAttempt>> = 
        userQuizAttemptDao.getUserAttempts(userId)
    
    fun getUserAttemptsBySubject(userId: String, subjectId: String): Flow<List<UserQuizAttempt>> = 
        userQuizAttemptDao.getUserAttemptsBySubject(userId, subjectId)
    
    fun getUserAttemptsForQuiz(userId: String, quizId: Long): Flow<List<UserQuizAttempt>> = 
        userQuizAttemptDao.getUserAttemptsForQuiz(userId, quizId)
    
    suspend fun getUserQuizAttempt(userId: String, quizId: Long, attemptNumber: Int): UserQuizAttempt? = 
        userQuizAttemptDao.getUserQuizAttempt(userId, quizId, attemptNumber)
    
    suspend fun insertUserAttempt(attempt: UserQuizAttempt) = 
        userQuizAttemptDao.insertAttempt(attempt)
    
    suspend fun updateUserAttempt(attempt: UserQuizAttempt) = 
        userQuizAttemptDao.updateAttempt(attempt)
    
    // Analytics operations
    suspend fun getUserStats(userId: String): UserQuizStats {
        val totalAttempts = userQuizAttemptDao.getUserTotalAttemptsCount(userId)
        val correctAnswers = userQuizAttemptDao.getUserCorrectAnswersCount(userId)
        val accuracy = if (totalAttempts > 0) (correctAnswers.toFloat() / totalAttempts * 100) else 0f
        
        return UserQuizStats(
            totalAttempts = totalAttempts,
            correctAnswers = correctAnswers,
            accuracy = accuracy
        )
    }
    
    suspend fun getUserStatsBySubject(userId: String, subjectId: String): UserQuizStats {
        val totalAttempts = userQuizAttemptDao.getUserTotalAttemptsBySubject(userId, subjectId)
        val correctAnswers = userQuizAttemptDao.getUserCorrectAnswersBySubject(userId, subjectId)
        val accuracy = if (totalAttempts > 0) (correctAnswers.toFloat() / totalAttempts * 100) else 0f
        
        return UserQuizStats(
            totalAttempts = totalAttempts,
            correctAnswers = correctAnswers,
            accuracy = accuracy
        )
    }
    
    // Combined operations
    fun getQuizzesWithUserProgress(userId: String, subjectId: String): Flow<List<QuizWithProgress>> {
        return combine(
            getQuizzesBySubject(subjectId),
            getUserAttemptsBySubject(userId, subjectId)
        ) { quizzes, attempts ->
            quizzes.map { quiz ->
                val userAttempts = attempts.filter { it.quizId == quiz.id }
                val bestAttempt = userAttempts.filter { it.isCorrect }.maxByOrNull { it.attemptNumber }
                val hasAttempted = userAttempts.isNotEmpty()
                val isCorrect = bestAttempt != null
                
                QuizWithProgress(
                    quiz = quiz,
                    hasAttempted = hasAttempted,
                    isCorrect = isCorrect,
                    attemptCount = userAttempts.size,
                    bestScore = if (isCorrect) 1 else 0,
                    lastAttemptTime = userAttempts.maxByOrNull { it.timestamp }?.timestamp
                )
            }
        }
    }
}

// Data classes for repository responses
data class UserQuizStats(
    val totalAttempts: Int,
    val correctAnswers: Int,
    val accuracy: Float
)

data class QuizWithProgress(
    val quiz: Quiz,
    val hasAttempted: Boolean,
    val isCorrect: Boolean,
    val attemptCount: Int,
    val bestScore: Int,
    val lastAttemptTime: Long?
)
