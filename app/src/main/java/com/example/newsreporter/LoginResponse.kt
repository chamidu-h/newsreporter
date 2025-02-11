package com.example.newsreporter

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userId: Long?,
    val role: String?
)
