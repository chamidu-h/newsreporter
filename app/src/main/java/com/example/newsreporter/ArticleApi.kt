package com.example.newsreporter

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ArticleApi {
    @POST("api/articles")
    fun submitArticle(@Body request: ArticleSubmissionRequest): Call<ArticleSubmissionResponse>

    @GET("api/articles/submitted")
    fun getSubmittedArticles(): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/rejected")
    fun getRejectedArticles(): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/{id}")
    fun getArticleById(@Path("id") id: Long): Call<ArticleSubmissionResponse>

    // These endpoints are used only by the React dashboard for editorial actions.
    @PUT("api/articles/{id}/approve")
    fun approveArticle(@Path("id") id: Long): Call<Void>

    @PUT("api/articles/{id}/reject")
    fun rejectArticle(@Path("id") id: Long, @Body payload: Map<String, String>): Call<ArticleSubmissionResponse>
}

