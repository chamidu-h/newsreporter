package com.example.newsreporter

data class ArticleSubmissionResponse(
    val id: Long,
    val title: String,
    val category: String,
    val content: String,
    val status: String,
    val reporterId: Long,
    val createdAt: String,
    val updatedAt: String
)
