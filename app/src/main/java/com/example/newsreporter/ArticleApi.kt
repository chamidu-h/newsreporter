package com.example.newsreporter

import retrofit2.Call
import retrofit2.http.*

interface ArticleApi {
    @POST("api/articles")
    fun submitArticle(@Body request: ArticleSubmissionRequest): Call<ArticleSubmissionResponse>

    @GET("api/articles/submitted")
    fun getSubmittedArticles(): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/rejected")
    fun getRejectedArticles(): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/{id}")
    fun getArticleById(@Path("id") id: Long): Call<ArticleSubmissionResponse>

    @PUT("api/articles/{id}/approve")
    fun approveArticle(@Path("id") id: Long): Call<Void>

    @PUT("api/articles/{id}/reject")
    fun rejectArticle(@Path("id") id: Long, @Body payload: Map<String, String>): Call<ArticleSubmissionResponse>

    // ✅ ENHANCED: Multiple endpoint options for different scenarios
    @GET("api/articles/all")
    fun getAllSubmittedArticles(): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/approved")
    fun getApprovedArticles(): Call<List<ArticleSubmissionResponse>>

    // ✅ ADD USER-SPECIFIC ENDPOINTS
    @GET("api/articles/user/{userId}")
    fun getUserArticles(@Path("userId") userId: Long): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/user/{userId}/status/{status}")
    fun getUserArticlesByStatus(
        @Path("userId") userId: Long,
        @Path("status") status: String
    ): Call<List<ArticleSubmissionResponse>>
}
