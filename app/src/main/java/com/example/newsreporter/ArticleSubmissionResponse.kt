package com.example.newsreporter

data class ArticleSubmissionResponse(
    val id: Long,
    val title: String,
    val category: String,
    val content: String,
    val preview: String? = null,
    val status: String,
    val authorId: Long, // Change to match backend
    val createdAt: String,
    val updatedAt: String,
    var reviewComment: String? = null
)

