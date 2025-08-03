package com.example.prayasnow.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayasnow.data.Quiz
import com.example.prayasnow.repository.QuizRepository
import com.example.prayasnow.repository.ProgressRepository
import com.example.prayasnow.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun QuizTakingScreen(
    subject: String,
    quizTitle: String, // Added quiz title parameter
    authViewModel: AuthViewModel,
    quizRepository: QuizRepository,
    progressRepository: ProgressRepository,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var quizzes by remember { mutableStateOf<List<Quiz>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var totalQuestions by remember { mutableStateOf(0) }
    var userAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var currentAttemptNumber by remember { mutableStateOf(1) }
    var canReattempt by remember { mutableStateOf(false) }
    var showReattemptDialog by remember { mutableStateOf(false) }

    // Load existing progress and check reattempt eligibility
    fun loadProgressFromStorage() {
        authState.user?.uid?.let { userId ->
            coroutineScope.launch {
                try {
                    // Check if user can reattempt this quiz
                    canReattempt = progressRepository.canReattempt(userId, "$subject-$quizTitle")
                    
                    // Get next attempt number
                    currentAttemptNumber = progressRepository.getNextAttemptNumber(userId, "$subject-$quizTitle")
                    
                    // Try to load from Firebase first, fallback to local storage
                    val progress = progressRepository.loadProgressFromFirebase(userId, "$subject-$quizTitle")
                    if (progress != null && !progress.completed) {
                        // Only restore progress if quiz is not completed
                        score = progress.score
                        currentQuestionIndex = progress.currentQuestionIndex
                        currentAttemptNumber = progress.attemptNumber
                        userAnswers = progress.userAnswers.mapValues { (_, value) ->
                            value?.toString() ?: ""
                        }
                        println("📥 Restored progress: Score $score, Question ${currentQuestionIndex + 1}/${progress.totalQuestions} (Attempt ${progress.attemptNumber})")
                    } else {
                        println("📥 Starting fresh attempt #$currentAttemptNumber for $subject-$quizTitle")
                    }
                } catch (e: Exception) {
                    println("❌ Error loading progress: ${e.message}")
                }
            }
        }
    }
    
    // Start a new attempt with shuffled questions
    fun startNewAttempt() {
        coroutineScope.launch {
            try {
                authState.user?.uid?.let { userId ->
                    currentAttemptNumber = progressRepository.getNextAttemptNumber(userId, "$subject-$quizTitle")
                    
                    // Reset quiz state
                    currentQuestionIndex = 0
                    selectedAnswer = null
                    showAnswer = false
                    score = 0
                    userAnswers = emptyMap()
                    showCompletionDialog = false
                    showReattemptDialog = false
                    
                    // Shuffle questions for new attempt
                    quizzes = quizzes.shuffled()
                    
                    println("🔄 Started new attempt #$currentAttemptNumber with shuffled questions")
                }
            } catch (e: Exception) {
                println("❌ Error starting new attempt: ${e.message}")
            }
        }
    }

    // Mark individual quiz as attempted
    fun markQuizAsAttempted(questionIndex: Int) {
        if (questionIndex < quizzes.size) {
            coroutineScope.launch {
                try {
                    val quiz = quizzes[questionIndex].copy(attempted = true)
                    quizRepository.database.quizDao().updateQuiz(quiz)
                    println("✅ Marked quiz question ${questionIndex + 1} as attempted")
                } catch (e: Exception) {
                    println("❌ Error marking quiz as attempted: ${e.message}")
                }
            }
        }
    }

    // Mark all quizzes in current session as attempted
    fun markAllQuizzesAsAttempted() {
        coroutineScope.launch {
            try {
                val updatedQuizzes = quizzes.map { it.copy(attempted = true) }
                quizRepository.database.quizDao().insertQuizzes(updatedQuizzes)
                println("✅ Marked all ${updatedQuizzes.size} quizzes as attempted")
            } catch (e: Exception) {
                println("❌ Error marking all quizzes as attempted: ${e.message}")
            }
        }
    }

    // Save progress locally with Firebase sync and attempt tracking
    fun saveProgressLocally() {
        authState.user?.uid?.let { userId ->
            coroutineScope.launch {
                try {
                    val questionsAttempted = currentQuestionIndex + 1
                    val completed = currentQuestionIndex >= totalQuestions - 1

                    progressRepository.saveProgressLocally(
                        userId = userId,
                        subject = "$subject-$quizTitle",
                        score = score,
                        totalQuestions = totalQuestions,
                        questionsAttempted = questionsAttempted,
                        currentQuestionIndex = currentQuestionIndex,
                        userAnswers = userAnswers,
                        completed = completed,
                        attemptNumber = currentAttemptNumber
                    )
                } catch (e: Exception) {
                    println("❌ Error saving progress: ${e.message}")
                }
            }
        }
    }

    // Load quizzes for the subject and specific quiz title
    LaunchedEffect(subject, quizTitle, authState.user?.uid) {
        try {
            authState.user?.uid?.let { userId ->
                // Get quizzes based on subject
                val allQuizzes = when (subject) {
                    "Science" -> quizRepository.getScienceQuizzes(userId)
                    "History" -> quizRepository.getHistoryQuizzes(userId)
                    "Geography" -> quizRepository.getGeographyQuizzes(userId)
                    "Maths" -> quizRepository.getMathsQuizzes(userId)
                    else -> emptyList()
                }
                
                // Filter quizzes by title and shuffle them
                quizzes = allQuizzes.filter { it.title == quizTitle }.shuffled()
                totalQuestions = quizzes.size

                // Insert quizzes if they don't exist
                if (quizzes.isNotEmpty()) {
                    try {
                        quizRepository.database.quizDao().insertQuizzes(quizzes)
                        println("📚 Loaded ${quizzes.size} questions for $quizTitle")
                    } catch (e: Exception) {
                        println("❌ Error inserting quizzes: ${e.message}")
                    }
                }

                // Load existing progress after quizzes are loaded
                loadProgressFromStorage()
            }
        } catch (e: Exception) {
            println("Error loading quizzes: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = quizTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$subject (Attempt #$currentAttemptNumber)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canReattempt && currentQuestionIndex == 0 && userAnswers.isEmpty()) {
                IconButton(onClick = { showReattemptDialog = true }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reattempt", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = if (totalQuestions > 0) (currentQuestionIndex + 1).toFloat() / totalQuestions else 0f,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Score display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Question and options
        if (quizzes.isNotEmpty() && currentQuestionIndex < quizzes.size) {
            val currentQuiz = quizzes[currentQuestionIndex]

            // Question
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = currentQuiz.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentQuiz.options) { option ->
                    val isSelected = selectedAnswer == option
                    val isCorrect = option == currentQuiz.answer
                    val showResult = showAnswer

                    Card(
                        onClick = {
                            if (!showAnswer) {
                                selectedAnswer = option
                                showAnswer = true
                                userAnswers = userAnswers + (currentQuestionIndex to option)

                                if (option == currentQuiz.answer) {
                                    score++
                                }
                                
                                // Save progress immediately after answering
                                saveProgressLocally()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                showResult && isSelected && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                showResult && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
                                showResult && !isSelected && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(
                                width = 2.dp,
                                color = if (showResult) {
                                    if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                                } else MaterialTheme.colorScheme.primary
                            )
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (showResult) {
                                Icon(
                                    imageVector = if (isSelected == isCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = if (isSelected == isCorrect) "Correct" else "Incorrect",
                                    tint = if (isSelected == isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation section (shown after answer is revealed)
            if (showAnswer && currentQuiz.explanation.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Explanation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentQuiz.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Next/Finish button
            if (showAnswer) {
                Button(
                    onClick = {
                        if (currentQuestionIndex < quizzes.size - 1) {
                            // Mark current quiz as attempted and save progress
                            markQuizAsAttempted(currentQuestionIndex)
                            saveProgressLocally()
                            currentQuestionIndex++
                            selectedAnswer = null
                            showAnswer = false
                        } else {
                            // Quiz finished - mark final quiz as attempted, save progress and show completion dialog
                            markQuizAsAttempted(currentQuestionIndex)
                            markAllQuizzesAsAttempted()
                            saveProgressLocally()
                            println("Quiz finished! Final score: $score/$totalQuestions (Attempt $currentAttemptNumber)")
                            showCompletionDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (currentQuestionIndex < quizzes.size - 1) "Next Question" else "Finish Quiz",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (quizzes.isEmpty()) {
            // Loading indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Completion Dialog with reattempt option
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Quiz Completed!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Congratulations!", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attempt #$currentAttemptNumber Score:")
                    Text("$score out of $totalQuestions", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Questions attempted: $totalQuestions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Progress saved and synced", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showCompletionDialog = false
                            startNewAttempt()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Try Again")
                    }
                    Button(
                        onClick = {
                            showCompletionDialog = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Continue")
                    }
                }
            }
        )
    }
    
    // Reattempt Dialog (shown when user can reattempt)
    if (showReattemptDialog) {
        AlertDialog(
            onDismissRequest = { showReattemptDialog = false },
            title = { Text("Reattempt Quiz?") },
            text = {
                Text("You have already completed this quiz. Would you like to try again with shuffled questions?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        startNewAttempt()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start New Attempt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReattemptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 