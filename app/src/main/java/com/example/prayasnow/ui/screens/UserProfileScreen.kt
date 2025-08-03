package com.example.prayasnow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import com.example.prayasnow.viewmodel.AuthViewModel
import com.example.prayasnow.repository.QuizRepository
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Logout

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel,
    quizRepository: QuizRepository,
    progressRepository: com.example.prayasnow.repository.ProgressRepository,
    onNavigateToLogin: () -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var quizAttempts by remember { mutableStateOf(0) }
    var testAttempts by remember { mutableStateOf(0) }
    var bucketItems by remember { mutableStateOf(0) }
    var totalQuizzes by remember { mutableStateOf(0) }

    // Load user stats
    LaunchedEffect(authState.user?.uid) {
        try {
            authState.user?.uid?.let { userId ->
                // Get quiz attempts from progress data
                val allProgress = progressRepository.getAllProgressForUser(userId)
                
                // Count unique quizzes attempted (not total attempts)
                val uniqueQuizzesAttempted = allProgress
                    .filter { it.questionsAttempted > 0 }
                    .map { it.subject }
                    .distinct()
                    .size
                
                quizAttempts = uniqueQuizzesAttempted
                
                // Get total quizzes available
                totalQuizzes = quizRepository.getTotalQuizCount(userId)
                
                // Count completed quizzes
                val completedQuizzes = allProgress
                    .filter { it.completed }
                    .map { it.subject }
                    .distinct()
                    .size
                
                testAttempts = completedQuizzes
                bucketItems = 0 // Keep as 0 for now
                
                println("📊 User stats loaded: Unique Attempts=$quizAttempts, Completed=$testAttempts, Total=$totalQuizzes")
            }
        } catch (e: Exception) {
            println("Error loading user stats: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .navigationBarsPadding()
            .safeDrawingPadding()
    ) {
        // User info section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authState.user?.displayName?.take(2)?.uppercase() 
                            ?: authState.user?.email?.take(2)?.uppercase() 
                            ?: "U",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User name
                Text(
                    text = authState.user?.displayName ?: "User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // User email
                Text(
                    text = authState.user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats section
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard(
                    title = "Quizzes Attempted",
                    value = quizAttempts.toString(),
                    icon = Icons.Default.Quiz,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                StatCard(
                    title = "Quizzes Completed",
                    value = testAttempts.toString(),
                    icon = Icons.Default.Assignment,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                StatCard(
                    title = "Items in Bucket",
                    value = bucketItems.toString(),
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            item {
                StatCard(
                    title = "Total Quizzes",
                    value = totalQuizzes.toString(),
                    icon = Icons.Default.LibraryBooks,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Take Quiz button
        Button(
            onClick = { onNavigateToQuiz() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Quiz,
                contentDescription = "Quizzes",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Take NCERT Quizzes")
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout button
        Button(
            onClick = {
                authViewModel.signOut()
                onNavigateToLogin()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = "Logout",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Logout")
        }
    }
} 