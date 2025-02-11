package com.example.newsreporter

import com.example.newsreporter.LoginRequest
import com.example.newsreporter.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>
}