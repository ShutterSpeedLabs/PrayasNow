package com.example.prayasnow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.prayasnow.data.Subject
import com.example.prayasnow.data.Quiz
import com.example.prayasnow.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subjects by adminViewModel.subjects.collectAsStateWithLifecycle()
    val selectedSubject by adminViewModel.selectedSubject.collectAsStateWithLifecycle()
    val subjectQuizzes by adminViewModel.subjectQuizzes.collectAsStateWithLifecycle()
    val isLoading by adminViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by adminViewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by adminViewModel.successMessage.collectAsStateWithLifecycle()
    
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showQuizDialog by remember { mutableStateOf(false) }
    
    // Show messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Handle error message display
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            // Handle success message display
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Admin Panel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row {
                IconButton(
                    onClick = { adminViewModel.syncPendingChanges() }
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync Changes")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    adminViewModel.startAddingSubject()
                    showSubjectDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Subject")
            }
            
            Button(
                onClick = {
                    if (selectedSubject != null) {
                        adminViewModel.startAddingQuiz(selectedSubject!!.id)
                        showQuizDialog = true
                    }
                },
                enabled = selectedSubject != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Quiz, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Quiz")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error message
        errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Success message
        successMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subjects List
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Subjects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn {
                        items(subjects) { subject ->
                            SubjectItem(
                                subject = subject,
                                isSelected = selectedSubject?.id == subject.id,
                                onSelect = { adminViewModel.selectSubject(subject) },
                                onEdit = {
                                    adminViewModel.startEditingSubject(subject)
                                    showSubjectDialog = true
                                },
                                onDelete = { adminViewModel.deleteSubject(subject) }
                            )
                        }
                    }
                }
            }
            
            // Quizzes List
            Card(
                modifier = Modifier.weight(2f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = selectedSubject?.let { subject ->
                            "Quizzes in ${subject.name}"
                        } ?: "Select a subject to view quizzes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (selectedSubject != null) {
                        LazyColumn {
                            items(subjectQuizzes) { quiz ->
                                QuizItem(
                                    quiz = quiz,
                                    onEdit = {
                                        adminViewModel.startEditingQuiz(quiz)
                                        showQuizDialog = true
                                    },
                                    onDelete = { adminViewModel.deleteQuiz(quiz) }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a subject from the left to manage its quizzes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Subject Dialog
    if (showSubjectDialog) {
        SubjectFormDialog(
            adminViewModel = adminViewModel,
            onDismiss = {
                showSubjectDialog = false
                adminViewModel.cancelEdit()
            }
        )
    }
    
    // Quiz Dialog
    if (showQuizDialog) {
        QuizFormDialog(
            adminViewModel = adminViewModel,
            onDismiss = {
                showQuizDialog = false
                adminViewModel.cancelEdit()
            }
        )
    }
}

@Composable
fun SubjectItem(
    subject: Subject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subject.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Subject",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Subject",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun QuizItem(
    quiz: Quiz,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title.ifEmpty { "Untitled Quiz" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = quiz.question.take(100) + if (quiz.question.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(quiz.difficulty) }
                        )
                        AssistChip(
                            onClick = { },
                            label = { Text("${quiz.options.size} options") }
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Quiz",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Quiz",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
