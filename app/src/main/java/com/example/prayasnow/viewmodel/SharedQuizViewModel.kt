package com.example.prayasnow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prayasnow.data.*
import com.example.prayasnow.repository.SharedQuizRepository
import com.example.prayasnow.repository.FirebaseQuizRepository
import com.example.prayasnow.repository.QuizWithProgress
import com.example.prayasnow.repository.UserQuizStats
import com.example.prayasnow.repository.FirebaseMigrationUtility
import com.example.prayasnow.repository.MigrationResult
import com.example.prayasnow.repository.MigrationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SharedQuizViewModel(
    private val sharedQuizRepository: SharedQuizRepository,
    private val firebaseQuizRepository: FirebaseQuizRepository,
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {

    // Migration utility for pushing local data to Firestore
    private lateinit var migrationUtility: FirebaseMigrationUtility

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Subjects
    val subjects: StateFlow<List<Subject>> = sharedQuizRepository.getAllActiveSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Quizzes for selected subject
    val quizzesForSelectedSubject: StateFlow<List<Quiz>> = _selectedSubject
        .filterNotNull()
        .flatMapLatest { subject ->
            sharedQuizRepository.getQuizzesBySubject(subject.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User progress for selected subject
    val userProgressForSelectedSubject: StateFlow<List<UserQuizAttempt>> = combine(
        _currentUserId,
        _selectedSubject
    ) { userId, subject ->
        if (userId.isNotEmpty() && subject != null) {
            sharedQuizRepository.getUserAttemptsBySubject(userId, subject.id)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined quiz data with user progress
    val quizzesWithProgress: StateFlow<List<QuizWithProgress>> = combine(
        _currentUserId,
        _selectedSubject
    ) { userId, subject ->
        if (userId.isNotEmpty() && subject != null) {
            sharedQuizRepository.getQuizzesWithUserProgress(userId, subject.id)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setCurrentUser(userId: String) {
        _currentUserId.value = userId
    }

    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
    }

    fun submitQuizAnswer(quizId: Long, userAnswer: String, timeSpent: Long = 0) {
        val userId = _currentUserId.value
        val subject = _selectedSubject.value
        
        if (userId.isEmpty() || subject == null) {
            _errorMessage.value = "User or subject not selected"
            return
        }

        viewModelScope.launch {
            try {
                val quiz = sharedQuizRepository.getQuizById(quizId)
                if (quiz == null) {
                    _errorMessage.value = "Quiz not found"
                    return@launch
                }

                val isCorrect = userAnswer.equals(quiz.answer, ignoreCase = true)
                val existingAttempts = sharedQuizRepository.getUserAttemptsForQuiz(userId, quizId)
                    .first()
                val attemptNumber = (existingAttempts.maxOfOrNull { it.attemptNumber } ?: 0) + 1

                val attempt = UserQuizAttempt(
                    id = "${userId}_${quizId}_${attemptNumber}",
                    userId = userId,
                    quizId = quizId,
                    subjectId = subject.id,
                    attemptNumber = attemptNumber,
                    userAnswer = userAnswer,
                    isCorrect = isCorrect,
                    timeSpent = timeSpent,
                    completed = true,
                    timestamp = System.currentTimeMillis(),
                    syncedToFirebase = false
                )

                sharedQuizRepository.insertUserAttempt(attempt)
                
                // Sync to Firebase in background
                syncUserProgressToFirebase()
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to submit answer: ${e.message}"
            }
        }
    }

    suspend fun getUserStats(subjectId: String? = null): UserQuizStats? {
        val userId = _currentUserId.value
        return if (userId.isEmpty()) {
            null
        } else {
            try {
                if (subjectId != null) {
                    sharedQuizRepository.getUserStatsBySubject(userId, subjectId)
                } else {
                    sharedQuizRepository.getUserStats(userId)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun syncQuizzesFromFirebase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val subjectsResult = firebaseQuizRepository.syncSubjectsFromFirebase()
                val quizzesResult = firebaseQuizRepository.syncQuizzesFromFirebase()
                
                if (subjectsResult.isSuccess && quizzesResult.isSuccess) {
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Sync failed: ${subjectsResult.exceptionOrNull()?.message ?: quizzesResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncUserProgressToFirebase() {
        val userId = _currentUserId.value
        if (userId.isEmpty()) return

        viewModelScope.launch {
            try {
                firebaseQuizRepository.syncUserProgressToFirebase(userId)
            } catch (e: Exception) {
                // Silent fail for background sync
                println("Background sync failed: ${e.message}")
            }
        }
    }

    fun initializeSampleData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                databaseInitializer.initializeWithSampleData()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to initialize sample data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refreshData() {
        // Trigger refresh by re-selecting current subject
        _selectedSubject.value?.let { subject ->
            _selectedSubject.value = subject
        }
    }

    // Initialize migration utility
    private fun initializeMigrationUtility(database: com.example.prayasnow.data.AppDatabase) {
        migrationUtility = FirebaseMigrationUtility(database)
    }

    // Push local quizzes to Firestore
    fun pushLocalQuizzesToFirestore(database: com.example.prayasnow.data.AppDatabase) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                initializeMigrationUtility(database)
                
                println("🚀 Starting migration of local quizzes to Firestore...")
                val result = migrationUtility.pushLocalQuizzesToFirestore()
                
                if (result.isSuccess) {
                    val migrationResult = result.getOrNull()
                    _errorMessage.value = null
                    println("✅ Migration completed! Uploaded: ${migrationResult?.uploadedCount}, Failed: ${migrationResult?.failedCount}")
                    println("📊 Details: ${migrationResult?.details}")
                } else {
                    val error = result.exceptionOrNull()
                    _errorMessage.value = "Migration failed: ${error?.message}"
                    println("❌ Migration failed: ${error?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Migration error: ${e.message}"
                println("❌ Migration error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Get migration status
    suspend fun getMigrationStatus(database: com.example.prayasnow.data.AppDatabase): MigrationStatus? {
        return try {
            initializeMigrationUtility(database)
            migrationUtility.getMigrationStatus()
        } catch (e: Exception) {
            println("❌ Error getting migration status: ${e.message}")
            null
        }
    }

    // Test Firestore connection
    fun testFirestoreConnection(database: com.example.prayasnow.data.AppDatabase) {
        viewModelScope.launch {
            try {
                initializeMigrationUtility(database)
                val result = migrationUtility.testFirestoreConnection()
                
                if (result.isSuccess) {
                    println("✅ ${result.getOrNull()}")
                    _errorMessage.value = null
                } else {
                    val error = result.exceptionOrNull()
                    _errorMessage.value = "Connection test failed: ${error?.message}"
                    println("❌ ${error?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection test error: ${e.message}"
                println("❌ Connection test error: ${e.message}")
            }
        }
    }
}
