package com.example.prayasnow.data

import androidx.room.TypeConverter

class QuizOptionsConverter {
    @TypeConverter
    fun fromOptionsList(options: List<String>): String = options.joinToString("|;|")

    @TypeConverter
    fun toOptionsList(data: String): List<String> =
        if (data.isEmpty()) emptyList() else data.split("|;|")
} 