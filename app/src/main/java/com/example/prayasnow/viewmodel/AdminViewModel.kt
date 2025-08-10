package com.example.prayasnow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prayasnow.data.*
import com.example.prayasnow.repository.AdminRepository
import com.example.prayasnow.auth.AuthService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for admin operations - managing subjects, quizzes, and questions
 */
class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val authService: AuthService
) : ViewModel() {
    
    // Admin access control
    val hasAdminAccess: StateFlow<Boolean> = authService.hasAdminAccess
    val currentUser: StateFlow<User?> = authService.currentUser
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Data State
    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()
    
    private val _allQuizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val allQuizzes: StateFlow<List<Quiz>> = _allQuizzes.asStateFlow()
    
    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject.asStateFlow()
    
    private val _subjectQuizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val subjectQuizzes: StateFlow<List<Quiz>> = _subjectQuizzes.asStateFlow()
    
    // Form State
    private val _currentSubjectForm = MutableStateFlow(SubjectForm())
    val currentSubjectForm: StateFlow<SubjectForm> = _currentSubjectForm.asStateFlow()
    
    private val _currentQuizForm = MutableStateFlow(QuizForm())
    val currentQuizForm: StateFlow<QuizForm> = _currentQuizForm.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    init {
        loadData()
    }
    
    // ==================== DATA LOADING ====================
    
    private fun loadData() {
        viewModelScope.launch {
            // Load subjects
            adminRepository.getAllSubjects().collect { subjectList ->
                _subjects.value = subjectList.filter { it.isActive }
            }
        }
        
        viewModelScope.launch {
            // Load all quizzes
            adminRepository.getAllQuizzes().collect { quizList ->
                _allQuizzes.value = quizList
                
                // Update subject quizzes if a subject is selected
                _selectedSubject.value?.let { subject ->
                    _subjectQuizzes.value = quizList.filter { it.subjectId == subject.id }
                }
            }
        }
    }
    
    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
        _subjectQuizzes.value = _allQuizzes.value.filter { it.subjectId == subject.id }
    }
    
    fun clearSelection() {
        _selectedSubject.value = null
        _subjectQuizzes.value = emptyList()
    }
    
    // ==================== SUBJECT OPERATIONS ====================
    
    fun startAddingSubject() {
        _currentSubjectForm.value = SubjectForm()
        _isEditMode.value = false
        clearMessages()
    }
    
    fun startEditingSubject(subject: Subject) {
        _currentSubjectForm.value = SubjectForm.fromSubject(subject)
        _isEditMode.value = true
        clearMessages()
    }
    
    fun updateSubjectForm(form: SubjectForm) {
        _currentSubjectForm.value = form
    }
    
    fun saveSubject() {
        viewModelScope.launch {
            if (!validateSubjectForm()) return@launch
            
            _isLoading.value = true
            clearMessages()
            
            try {
                val result = executeAdminOperation {
                    if (_isEditMode.value) {
                        adminRepository.updateSubject(_currentSubjectForm.value)
                    } else {
                        adminRepository.addSubject(_currentSubjectForm.value)
                    }
                }
                
                if (result.success) {
                    _successMessage.value = result.message
                    _currentSubjectForm.value = SubjectForm()
                    _isEditMode.value = false
                } else {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving subject: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            
            try {
                val result = adminRepository.deleteSubject(subject.id)
                if (result.success) {
                    _successMessage.value = result.message
                    if (_selectedSubject.value?.id == subject.id) {
                        clearSelection()
                    }
                } else {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting subject: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun validateSubjectForm(): Boolean {
        val form = _currentSubjectForm.value
        
        if (form.name.isBlank()) {
            _errorMessage.value = "Subject name is required"
            return false
        }
        
        if (form.description.isBlank()) {
            _errorMessage.value = "Subject description is required"
            return false
        }
        
        return true
    }
    
    // ==================== QUIZ OPERATIONS ====================
    
    fun startAddingQuiz(subjectId: String) {
        _currentQuizForm.value = QuizForm(subjectId = subjectId)
        _isEditMode.value = false
        clearMessages()
    }
    
    fun startEditingQuiz(quiz: Quiz) {
        _currentQuizForm.value = QuizForm.fromQuiz(quiz)
        _isEditMode.value = true
        clearMessages()
    }
    
    fun updateQuizForm(form: QuizForm) {
        _currentQuizForm.value = form
    }
    
    fun saveQuiz() {
        viewModelScope.launch {
            if (!validateQuizForm()) return@launch
            
            _isLoading.value = true
            clearMessages()
            
            try {
                val result = if (_isEditMode.value) {
                    adminRepository.updateQuiz(_currentQuizForm.value)
                } else {
                    adminRepository.addQuiz(_currentQuizForm.value)
                }
                
                if (result.success) {
                    _successMessage.value = result.message
                    _currentQuizForm.value = QuizForm()
                    _isEditMode.value = false
                } else {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving quiz: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteQuiz(quiz: Quiz) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            
            try {
                val result = adminRepository.deleteQuiz(quiz.id)
                if (result.success) {
                    _successMessage.value = result.message
                } else {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting quiz: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun validateQuizForm(): Boolean {
        val form = _currentQuizForm.value
        
        if (form.subjectId.isBlank()) {
            _errorMessage.value = "Please select a subject"
            return false
        }
        
        if (form.title.isBlank()) {
            _errorMessage.value = "Quiz title is required"
            return false
        }
        
        if (form.question.isBlank()) {
            _errorMessage.value = "Question is required"
            return false
        }
        
        val validOptions = form.options.filter { it.isNotBlank() }
        if (validOptions.size < 2) {
            _errorMessage.value = "At least 2 options are required"
            return false
        }
        
        if (form.answer.isBlank()) {
            _errorMessage.value = "Correct answer is required"
            return false
        }
        
        if (!validOptions.contains(form.answer)) {
            _errorMessage.value = "Correct answer must be one of the options"
            return false
        }
        
        return true
    }
    
    // ==================== ADMIN ACCESS CONTROL ====================
    
    /**
     * Check if current user has admin access before performing operations
     */
    private suspend fun validateAdminAccess(): Boolean {
        return authService.checkAdminAccess()
    }
    
    /**
     * Execute admin operation with access validation
     */
    private suspend fun executeAdminOperation(
        operation: suspend () -> AdminOperationResult
    ): AdminOperationResult {
        return if (validateAdminAccess()) {
            operation()
        } else {
            AdminOperationResult(
                success = false,
                message = "Access denied: Admin privileges required"
            )
        }
    }
    
    /**
     * Refresh admin access status
     */
    fun refreshAdminAccess() {
        viewModelScope.launch {
            authService.refreshUserData()
        }
    }
    
    // ==================== UTILITY FUNCTIONS ====================
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    fun syncPendingChanges() {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            
            try {
                val result = adminRepository.syncPendingChanges()
                if (result.success) {
                    _successMessage.value = result.message
                } else {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun cancelEdit() {
        _currentSubjectForm.value = SubjectForm()
        _currentQuizForm.value = QuizForm()
        _isEditMode.value = false
        clearMessages()
    }
    
    // Helper function to get difficulty levels
    fun getDifficultyLevels(): List<DifficultyLevel> {
        return DifficultyLevel.values().toList()
    }
    
    // Helper function to get available subjects for quiz creation
    fun getAvailableSubjects(): List<Subject> {
        return _subjects.value
    }
}
