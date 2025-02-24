package com.example.newsreporter

data class ArticleSubmissionRequest(
    val title: String,
    val category: String,
    val content: String,  // JSON string representing the list of ArticleElement objects
    val reporterId: Long
)

