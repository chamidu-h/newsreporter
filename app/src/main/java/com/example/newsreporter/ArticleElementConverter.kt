package com.example.newsreporter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ArticleElement(
    val type: ElementType,
    val content: String
)

enum class ElementType {
    HEADING,
    SUBHEADING,
    PARAGRAPH,
    QUOTE,
    IMAGE,
    VIDEO
}

class ArticleElementConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromArticleElementList(value: List<ArticleElement>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toArticleElementList(value: String?): List<ArticleElement>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<ArticleElement>>() {}.type
        return gson.fromJson(value, listType)
    }
}

