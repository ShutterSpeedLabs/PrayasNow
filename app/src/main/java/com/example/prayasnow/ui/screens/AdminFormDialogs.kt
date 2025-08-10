package com.example.prayasnow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.prayasnow.data.DifficultyLevel
import com.example.prayasnow.data.SubjectForm
import com.example.prayasnow.data.QuizForm
import com.example.prayasnow.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectFormDialog(
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectForm by adminViewModel.currentSubjectForm.collectAsStateWithLifecycle()
    val isEditMode by adminViewModel.isEditMode.collectAsStateWithLifecycle()
    val isLoading by adminViewModel.isLoading.collectAsStateWithLifecycle()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Edit Subject" else "Add New Subject",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Form Fields
                OutlinedTextField(
                    value = subjectForm.name,
                    onValueChange = { 
                        adminViewModel.updateSubjectForm(subjectForm.copy(name = it))
                    },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = subjectForm.description,
                    onValueChange = { 
                        adminViewModel.updateSubjectForm(subjectForm.copy(description = it))
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 2,
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = subjectForm.iconName,
                    onValueChange = { 
                        adminViewModel.updateSubjectForm(subjectForm.copy(iconName = it))
                    },
                    label = { Text("Icon Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("quiz, book, science, etc.") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = subjectForm.isActive,
                        onCheckedChange = { 
                            adminViewModel.updateSubjectForm(subjectForm.copy(isActive = it))
                        },
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { adminViewModel.saveSubject() },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isEditMode) "Update" else "Add")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizFormDialog(
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quizForm by adminViewModel.currentQuizForm.collectAsStateWithLifecycle()
    val isEditMode by adminViewModel.isEditMode.collectAsStateWithLifecycle()
    val isLoading by adminViewModel.isLoading.collectAsStateWithLifecycle()
    val availableSubjects = adminViewModel.getAvailableSubjects()
    val difficultyLevels = adminViewModel.getDifficultyLevels()
    
    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showDifficultyDropdown by remember { mutableStateOf(false) }
    var tagsInput by remember { mutableStateOf(quizForm.tags.joinToString(", ")) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Edit Quiz" else "Add New Quiz",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Subject Selection
                    ExposedDropdownMenuBox(
                        expanded = showSubjectDropdown,
                        onExpandedChange = { showSubjectDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = availableSubjects.find { it.id == quizForm.subjectId }?.name ?: "Select Subject",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isLoading
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false }
                        ) {
                            availableSubjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name) },
                                    onClick = {
                                        adminViewModel.updateQuizForm(quizForm.copy(subjectId = subject.id))
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Quiz Title
                    OutlinedTextField(
                        value = quizForm.title,
                        onValueChange = { 
                            adminViewModel.updateQuizForm(quizForm.copy(title = it))
                        },
                        label = { Text("Quiz Title") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Question
                    OutlinedTextField(
                        value = quizForm.question,
                        onValueChange = { 
                            adminViewModel.updateQuizForm(quizForm.copy(question = it))
                        },
                        label = { Text("Question") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        minLines = 2,
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Options
                    Text(
                        text = "Answer Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    quizForm.options.forEachIndexed { index, option ->
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newValue ->
                                val newOptions = quizForm.options.toMutableList()
                                newOptions[index] = newValue
                                adminViewModel.updateQuizForm(quizForm.copy(options = newOptions))
                            },
                            label = { Text("Option ${index + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true
                        )
                        
                        if (index < quizForm.options.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Correct Answer
                    OutlinedTextField(
                        value = quizForm.answer,
                        onValueChange = { 
                            adminViewModel.updateQuizForm(quizForm.copy(answer = it))
                        },
                        label = { Text("Correct Answer") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        placeholder = { Text("Must match one of the options exactly") }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Explanation
                    OutlinedTextField(
                        value = quizForm.explanation,
                        onValueChange = { 
                            adminViewModel.updateQuizForm(quizForm.copy(explanation = it))
                        },
                        label = { Text("Explanation (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        minLines = 2,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Difficulty
                    ExposedDropdownMenuBox(
                        expanded = showDifficultyDropdown,
                        onExpandedChange = { showDifficultyDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = difficultyLevels.find { it.name == quizForm.difficulty }?.displayName ?: quizForm.difficulty,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Difficulty") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDifficultyDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isLoading
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showDifficultyDropdown,
                            onDismissRequest = { showDifficultyDropdown = false }
                        ) {
                            difficultyLevels.forEach { difficulty ->
                                DropdownMenuItem(
                                    text = { Text(difficulty.displayName) },
                                    onClick = {
                                        adminViewModel.updateQuizForm(quizForm.copy(difficulty = difficulty.name))
                                        showDifficultyDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tags
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { 
                            tagsInput = it
                            val tags = it.split(",").map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() }
                            adminViewModel.updateQuizForm(quizForm.copy(tags = tags))
                        },
                        label = { Text("Tags (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        placeholder = { Text("Separate tags with commas") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = quizForm.isActive,
                            onCheckedChange = { 
                                adminViewModel.updateQuizForm(quizForm.copy(isActive = it))
                            },
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { adminViewModel.saveQuiz() },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isEditMode) "Update" else "Add")
                        }
                    }
                }
            }
        }
    }
}
