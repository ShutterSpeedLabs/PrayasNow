package com.example.prayasnow.data

/**
 * Data models for admin operations - adding and editing shared content
 */

data class SubjectForm(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconName: String = "quiz",
    val isActive: Boolean = true
) {
    fun toSubject(): Subject {
        return Subject(
            id = if (id.isEmpty()) generateSubjectId() else id,
            name = name,
            description = description,
            iconName = iconName,
            isActive = isActive,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    companion object {
        fun fromSubject(subject: Subject): SubjectForm {
            return SubjectForm(
                id = subject.id,
                name = subject.name,
                description = subject.description,
                iconName = subject.iconName,
                isActive = subject.isActive
            )
        }
        
        private fun generateSubjectId(): String {
            return "subject_${System.currentTimeMillis()}"
        }
    }
}

data class QuizForm(
    val id: Long = 0,
    val firebaseId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val answer: String = "",
    val explanation: String = "",
    val difficulty: String = "MEDIUM",
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true
) {
    fun toQuiz(): Quiz {
        return Quiz(
            id = id,
            firebaseId = firebaseId,
            subjectId = subjectId,
            title = title,
            question = question,
            options = options.filter { it.isNotBlank() },
            answer = answer,
            explanation = explanation,
            difficulty = difficulty,
            tags = tags,
            createdBy = "admin",
            isActive = isActive,
            timestamp = System.currentTimeMillis(),
            version = 1,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
    }
    
    companion object {
        fun fromQuiz(quiz: Quiz): QuizForm {
            // Ensure we have at least 4 options for the form
            val formOptions = quiz.options.toMutableList()
            while (formOptions.size < 4) {
                formOptions.add("")
            }
            
            return QuizForm(
                id = quiz.id,
                firebaseId = quiz.firebaseId,
                subjectId = quiz.subjectId,
                title = quiz.title,
                question = quiz.question,
                options = formOptions,
                answer = quiz.answer,
                explanation = quiz.explanation,
                difficulty = quiz.difficulty,
                tags = quiz.tags,
                isActive = quiz.isActive
            )
        }
    }
}

enum class DifficultyLevel(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    EXPERT("Expert")
}

data class AdminOperationResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
