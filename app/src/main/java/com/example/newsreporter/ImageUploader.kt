package com.example.newsreporter

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

// ImageUploader.kt
object ImageUploader {
    // Helper function to copy the file from the given URI into a temporary file.
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("ImageUploader", "Input stream is null for URI: $uri")
                return null
            }
            // Create a temporary file in the cache directory.
            val tempFile = File.createTempFile("upload", ".tmp", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                val copiedBytes = inputStream.copyTo(outputStream)
                Log.d("ImageUploader", "Copied $copiedBytes bytes to temporary file: ${tempFile.absolutePath}")
            }
            inputStream.close()
            if (tempFile.length() == 0L) {
                Log.e("ImageUploader", "Temporary file is empty: ${tempFile.absolutePath}")
                return null
            }
            tempFile
        } catch (e: Exception) {
            Log.e("ImageUploader", "Error copying file from URI: ${e.message}")
            null
        }
    }

    fun uploadImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val mediaType = mimeType.toMediaTypeOrNull()

            // Create a temporary file from the URI.
            val file = getFileFromUri(context, uri)
            if (file == null) {
                Log.e("ImageUploader", "Temporary file is null")
                onResult(null)
                return
            }
            Log.d("ImageUploader", "Temporary file exists: ${file.exists()}, size: ${file.length()} bytes")

            // Build the request body using the temporary file.
            val requestBody = file.asRequestBody(mediaType)
            val extension = mimeType.substringAfter("/", "png")
            val filename = "upload_${System.currentTimeMillis()}.$extension"
            val body = MultipartBody.Part.createFormData("file", filename, requestBody)

            RetrofitClient.uploadApi.uploadImage(body).enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        val imageUrl = response.body()?.get("url")
                        Log.d("ImageUploader", "Upload success, URL: $imageUrl")
                        onResult(imageUrl)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "No error message"
                        Log.e("ImageUploader", "Upload failed: ${response.code()} $errorBody")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Log.e("ImageUploader", "Upload error: ${t.message}")
                    onResult(null)
                }
            })
        } catch (e: Exception) {
            Log.e("ImageUploader", "Exception during upload: ${e.message}")
            onResult(null)
        }
    }
}




