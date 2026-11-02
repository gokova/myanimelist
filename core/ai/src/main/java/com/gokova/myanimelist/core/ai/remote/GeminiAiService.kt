package com.gokova.myanimelist.core.ai.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiAiService {
    @POST("v1beta/models/gemini-3.5-flash-lite:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest,
    ): Response<GeminiGenerateContentResponse>

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }
}
