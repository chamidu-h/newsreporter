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

    companion object {
        fun convert(content: String): List<ArticleElement> {
            return try {
                val gson = Gson()
                val listType = object : TypeToken<List<ArticleElement>>() {}.type
                // Try converting the content as a JSON list of ArticleElement
                val elements: List<ArticleElement>? = gson.fromJson(content, listType)
                if (elements.isNullOrEmpty()) {
                    // If conversion returns an empty list, treat the content as a single paragraph
                    listOf(ArticleElement(ElementType.PARAGRAPH, content))
                } else {
                    elements
                }
            } catch (e: Exception) {
                // In case of error, return the content as a single paragraph element
                listOf(ArticleElement(ElementType.PARAGRAPH, content))
            }
        }
    }
}

