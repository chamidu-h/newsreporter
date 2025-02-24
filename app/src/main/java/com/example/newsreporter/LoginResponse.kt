package com.example.newsreporter

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userId: Long? = null,
    val role: String? = null,
    val token: String? = null  // Added token property for JWT
)
