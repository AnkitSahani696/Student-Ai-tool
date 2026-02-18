package com.example.firebase

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // ⚠️ Recommendation: move to BuildConfig later
    private val apiKey = "AIzaSyAzoxO7-NuLZ7Yi8h2OD-HgQOMLCunkeQU"

    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent"

    private var lastApiCallTime = 0L

    suspend fun getChatResponse(userMessage: String): Result<String> {
        return withContext(Dispatchers.IO) {

            // 🛑 Anti-spam (minimum 3 sec gap)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastApiCallTime < 3000) {
                return@withContext Result.failure(
                    Exception("Please wait a moment before sending another message.")
                )
            }
            lastApiCallTime = currentTime

            try {
                val requestBody = GeminiRequest(
                    contents = listOf(
                        RequestContent(
                            parts = listOf(RequestPart(text = userMessage))
                        )
                    )
                )

                val jsonBody = gson.toJson(requestBody)

                val request = Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                // 🚨 Handle 429 properly
                if (response.code == 429) {
                    delay(5000) // wait 5 sec
                    return@withContext Result.failure(
                        Exception("API limit reached. Please wait 5 seconds and try again.")
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("API Error: ${response.code}")
                    )
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val geminiResponse =
                    gson.fromJson(responseBody, GeminiResponse::class.java)

                val aiText = geminiResponse.candidates
                    .firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                    ?: "No response generated."

                Result.success(aiText)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
