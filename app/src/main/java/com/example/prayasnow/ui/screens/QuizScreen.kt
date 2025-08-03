package com.example.prayasnow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prayasnow.repository.QuizRepository
import com.example.prayasnow.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

data class SubjectInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val quizzes: List<String>
)

@Composable
fun QuizScreen(
    authViewModel: AuthViewModel,
    quizRepository: QuizRepository,
    onNavigateBack: () -> Unit,
    onNavigateToSubject: (String) -> Unit // Navigate to subject quiz list
) {
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Define quiz subjects with their available quizzes
    val subjects = listOf(
        SubjectInfo(
            name = "Science",
            icon = Icons.Default.Science,
            color = Color(0xFF4CAF50),
            description = "Physics, Chemistry, Biology",
            quizzes = listOf("Physics Basics", "Chemistry Fundamentals", "Biology Basics")
        ),
        SubjectInfo(
            name = "History",
            icon = Icons.Default.History,
            color = Color(0xFFFF9800),
            description = "Ancient, Medieval, Modern India",
            quizzes = listOf("Ancient India", "Medieval India", "Modern India")
        ),
        SubjectInfo(
            name = "Geography",
            icon = Icons.Default.Public,
            color = Color(0xFF2196F3),
            description = "Physical, Climate, Economic Geography",
            quizzes = listOf("Physical Geography", "Climate and Weather", "Economic Geography")
        ),
        SubjectInfo(
            name = "Maths",
            icon = Icons.Default.Calculate,
            color = Color(0xFF9C27B0),
            description = "Algebra, Geometry, Arithmetic",
            quizzes = listOf("Algebra", "Geometry", "Arithmetic")
        )
    )

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
            Text(
                text = "NCERT Quizzes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subjects list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(subjects) { subject ->
                SubjectCard(
                    subject = subject,
                    onNavigateToSubject = onNavigateToSubject
                )
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: SubjectInfo,
    onNavigateToSubject: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToSubject(subject.name) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = subject.icon,
                    contentDescription = subject.name,
                    tint = subject.color,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subject.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${subject.quizzes.size} quizzes available",
                        style = MaterialTheme.typography.bodySmall,
                        color = subject.color,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "View Quizzes",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}