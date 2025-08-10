package com.example.prayasnow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prayasnow.repository.QuizRepository
import com.example.prayasnow.repository.ProgressRepository
import com.example.prayasnow.viewmodel.AuthViewModel
import com.example.prayasnow.data.SubjectInfo
import kotlinx.coroutines.launch

@Composable
fun SubjectQuizListScreen(
    subject: String,
    authViewModel: AuthViewModel,
    quizRepository: QuizRepository,
    progressRepository: ProgressRepository,
    onNavigateBack: () -> Unit,
    onNavigateToQuizTaking: (String, String) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Get subject info with dynamic quiz loading
    var subjectInfo by remember { mutableStateOf(getSubjectInfoStatic(subject)) }
    
    // Load dynamic quiz titles
    LaunchedEffect(subject, authState.user?.uid) {
        authState.user?.uid?.let { userId ->
            try {
                subjectInfo = getSubjectInfo(subject, quizRepository, userId)
            } catch (e: Exception) {
                println("❌ Error loading subject info: ${e.message}")
            }
        }
    }
    
    // Track quiz progress for each quiz
    var quizProgress by remember { mutableStateOf<Map<String, QuizProgressInfo>>(emptyMap()) }
    
    // Load progress for all quizzes in this subject
    LaunchedEffect(authState.user?.uid, subject) {
        authState.user?.uid?.let { userId ->
            coroutineScope.launch {
                try {
                    val progressMap = mutableMapOf<String, QuizProgressInfo>()
                    subjectInfo.quizzes.forEach { quizTitle ->
                        val progress = progressRepository.loadProgressLocally(userId, "$subject-$quizTitle")
                        val canReattempt = progressRepository.canReattempt(userId, "$subject-$quizTitle")
                        val bestScore = progressRepository.getBestScore(userId, "$subject-$quizTitle")
                        
                        progressMap[quizTitle] = QuizProgressInfo(
                            completed = progress?.completed ?: false,
                            bestScore = bestScore,
                            totalQuestions = progress?.totalQuestions ?: 10,
                            canReattempt = canReattempt,
                            currentProgress = progress?.currentQuestionIndex ?: 0
                        )
                    }
                    quizProgress = progressMap
                } catch (e: Exception) {
                    println("Error loading quiz progress: ${e.message}")
                }
            }
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
                    text = subject,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subjectInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subject icon and info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = subjectInfo.icon,
                    contentDescription = subject,
                    tint = subjectInfo.color,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${subjectInfo.quizzes.size} Quizzes Available",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Test your knowledge in ${subject.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quiz list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subjectInfo.quizzes) { quizTitle ->
                val progress = quizProgress[quizTitle]
                QuizCard(
                    quizTitle = quizTitle,
                    subject = subject,
                    subjectColor = subjectInfo.color,
                    progress = progress,
                    onNavigateToQuizTaking = onNavigateToQuizTaking
                )
            }
        }
    }
}

data class QuizProgressInfo(
    val completed: Boolean,
    val bestScore: Int,
    val totalQuestions: Int,
    val canReattempt: Boolean,
    val currentProgress: Int
)

@Composable
fun QuizCard(
    quizTitle: String,
    subject: String,
    subjectColor: Color,
    progress: QuizProgressInfo?,
    onNavigateToQuizTaking: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToQuizTaking(subject, quizTitle) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Quiz,
                    contentDescription = "Quiz",
                    modifier = Modifier.size(24.dp),
                    tint = subjectColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quizTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Progress information
                    if (progress != null) {
                        when {
                            progress.completed -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Best Score: ${progress.bestScore}/${progress.totalQuestions}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4CAF50)
                                    )
                                    if (progress.canReattempt) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• Can Reattempt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            progress.currentProgress > 0 -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "In Progress",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFF9800)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Progress: ${progress.currentProgress + 1}/${progress.totalQuestions}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Not started • ${progress.totalQuestions} questions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "10 questions • Not started",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Start Quiz",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper function to get subject information with dynamic quiz loading
suspend fun getSubjectInfo(subject: String, quizRepository: QuizRepository, userId: String): SubjectInfo {
    val quizTitles = try {
        quizRepository.getQuizTitlesForSubject(userId, subject)
    } catch (e: Exception) {
        println("❌ Error loading quiz titles: ${e.message}")
        getDefaultQuizTitles(subject)
    }
    
    return when (subject) {
        "Science" -> SubjectInfo(
            name = "Science",
            icon = Icons.Default.Science,
            color = Color(0xFF4CAF50),
            description = "Physics, Chemistry, Biology",
            quizzes = quizTitles.ifEmpty { getDefaultQuizTitles(subject) }
        )
        "History" -> SubjectInfo(
            name = "History",
            icon = Icons.Default.History,
            color = Color(0xFFFF9800),
            description = "Ancient, Medieval, Modern India",
            quizzes = quizTitles.ifEmpty { getDefaultQuizTitles(subject) }
        )
        "Geography" -> SubjectInfo(
            name = "Geography",
            icon = Icons.Default.Public,
            color = Color(0xFF2196F3),
            description = "Physical, Climate, Economic Geography",
            quizzes = quizTitles.ifEmpty { getDefaultQuizTitles(subject) }
        )
        "Maths" -> SubjectInfo(
            name = "Maths",
            icon = Icons.Default.Calculate,
            color = Color(0xFF9C27B0),
            description = "Algebra, Geometry, Arithmetic",
            quizzes = quizTitles.ifEmpty { getDefaultQuizTitles(subject) }
        )
        else -> SubjectInfo(
            name = subject,
            icon = Icons.Default.Quiz,
            color = Color(0xFF607D8B),
            description = "Quiz topics",
            quizzes = quizTitles.ifEmpty { emptyList() }
        )
    }
}

// Fallback quiz titles if Firebase data is not available
private fun getDefaultQuizTitles(subject: String): List<String> {
    return when (subject) {
        "Science" -> listOf("Physics Basics", "Chemistry Fundamentals", "Biology Basics")
        "History" -> listOf("Ancient India", "Medieval India", "Modern India")
        "Geography" -> listOf("Physical Geography", "Climate and Weather", "Economic Geography")
        "Maths" -> listOf("Algebra", "Geometry", "Arithmetic")
        else -> emptyList()
    }
}

// Static subject info for initial display
fun getSubjectInfoStatic(subject: String): SubjectInfo {
    return when (subject) {
        "Science" -> SubjectInfo(
            name = "Science",
            icon = Icons.Default.Science,
            color = Color(0xFF4CAF50),
            description = "Physics, Chemistry, Biology",
            quizzes = getDefaultQuizTitles(subject)
        )
        "History" -> SubjectInfo(
            name = "History",
            icon = Icons.Default.History,
            color = Color(0xFFFF9800),
            description = "Ancient, Medieval, Modern India",
            quizzes = getDefaultQuizTitles(subject)
        )
        "Geography" -> SubjectInfo(
            name = "Geography",
            icon = Icons.Default.Public,
            color = Color(0xFF2196F3),
            description = "Physical, Climate, Economic Geography",
            quizzes = getDefaultQuizTitles(subject)
        )
        "Maths" -> SubjectInfo(
            name = "Maths",
            icon = Icons.Default.Calculate,
            color = Color(0xFF9C27B0),
            description = "Algebra, Geometry, Arithmetic",
            quizzes = getDefaultQuizTitles(subject)
        )
        else -> SubjectInfo(
            name = subject,
            icon = Icons.Default.Quiz,
            color = Color(0xFF607D8B),
            description = "Quiz topics",
            quizzes = emptyList()
        )
    }
}
