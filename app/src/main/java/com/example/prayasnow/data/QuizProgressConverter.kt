package com.example.prayasnow.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuizProgressConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromUserAnswersMap(userAnswers: Map<Int, String>): String {
        return gson.toJson(userAnswers)
    }
    
    @TypeConverter
    fun toUserAnswersMap(userAnswersString: String): Map<Int, String> {
        val type = object : TypeToken<Map<Int, String>>() {}.type
        return gson.fromJson(userAnswersString, type) ?: emptyMap()
    }
} 