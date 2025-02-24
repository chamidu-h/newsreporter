package com.example.newsreporter

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface UploadApi {
    @POST("/api/uploads")
    @Multipart
    fun uploadImage(@Part file: MultipartBody.Part): Call<Map<String, String>>
}