package com.example.prayasnow.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class SubjectInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val quizzes: List<String>
)