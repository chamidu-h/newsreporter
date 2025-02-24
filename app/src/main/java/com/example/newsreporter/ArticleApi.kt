package com.example.newsreporter

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ArticleApi {
    @POST("api/articles")
    fun submitArticle(@Body request: ArticleSubmissionRequest): Call<ArticleSubmissionResponse>
    @GET("api/articles/submitted")
    fun getSubmittedArticles(): Call<List<ArticleSubmissionResponse>>

    @GET("api/articles/{id}")
    fun getArticleById(@Path("id") id: Long): Call<ArticleSubmissionResponse>
}
