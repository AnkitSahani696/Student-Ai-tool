package com.example.firebase

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.apply
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.toList
import kotlin.io.bufferedReader
import kotlin.io.readText
import kotlin.io.use
import kotlin.let
import kotlin.text.isEmpty
import kotlin.text.isNotEmpty
import kotlin.text.lines
import kotlin.text.matches
import kotlin.text.startsWith
import kotlin.text.substringAfter
import kotlin.text.take
import kotlin.text.toByteArray
import kotlin.text.trim
import kotlin.text.trimIndent

class GeminiApiService2 {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent"

    fun generateShortQuestions(pdfText: String): List<Question> {
        val apiKey = getApiKey()

        val prompt = """
               Generate 10 short questions (with 1-2 line answers) from the text below.
            
            Format:
            Q1: [Question]
            A1: [Answer]
            
            Q2: [Question]
            A2: [Answer]
            
            Text:
            ${pdfText.take(15000)} 
            
            Requirements:
            - Questions should be clear and specific
            - Answers should be concise and to-the-point
            - Cover important points from the text
            - Use the same language as the input text
        """.trimIndent()

        val response = makeApiCall(apiKey, prompt)
        return parseShortQuestions(response)
    }

    fun generateLongQuestions(pdfText: String): List<Question> {
        val apiKey = getApiKey()

        val prompt = """
          Generate 5 long questions (with detailed answers) from the text below.
            
            
            Format:
            Q1: [Question]
            A1: [Detailed Answer]
            
            Q2: [Question]
            A2: [Detailed Answer]
            
            Text:
            ${pdfText.take(15000)}
            
          Requirements:
            - Questions should be comprehensive
            - Answers should be detailed and well-explained (4-5 lines minimum)
            - Cover important concepts from the text
            - Use the same language as the input text
        """.trimIndent()

        val response = makeApiCall(apiKey, prompt)
        return parseLongQuestions(response)
    }

    fun generateMCQs(pdfText: String): List<Question> {
        val apiKey = getApiKey()

        val prompt = """
           Generate 10 Multiple Choice Questions (MCQs) with 4 options each from the text below.
            
            
            Format:
            Q1: [Question]
            A) [Option 1]
            B) [Option 2]
            C) [Option 3]
            D) [Option 4]
            Correct Answer: [A/B/C/D]
            
            Text:
            ${pdfText.take(15000)}
            
            Requirements:
            - Questions should be clear
            - All options should be plausible
            - Only one correct answer
            - Medium difficulty level
            - Use the same language as the input text
        """.trimIndent()

        val response = makeApiCall(apiKey, prompt)
        return parseMCQs(response)
    }

    private fun makeApiCall(apiKey: String, prompt: String): String {
        val url = URL("$baseUrl?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Create request body
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            // Send request
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            // Get response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("API Error: $responseCode - $errorStream")
            }

        } finally {
            connection.disconnect()
        }
    }

    private fun parseShortQuestions(jsonResponse: String): List<Question> {
        val questions = mutableListOf<Question>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val candidates = jsonObject.getJSONArray("candidates")
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            // Parse questions from text
            val lines = text.lines()
            var currentQuestion = ""
            var currentAnswer = ""

            for (line in lines) {
                when {
                    line.trim().startsWith("Q", ignoreCase = true) -> {
                        if (currentQuestion.isNotEmpty() && currentAnswer.isNotEmpty()) {
                            questions.add(Question(
                                question = currentQuestion,
                                answer = currentAnswer,
                                type = "short"
                            ))
                        }
                        currentQuestion = line.substringAfter(":").trim()
                        currentAnswer = ""
                    }
                    line.trim().startsWith("A", ignoreCase = true) -> {
                        currentAnswer = line.substringAfter(":").trim()
                    }
                    line.trim().isNotEmpty() && currentQuestion.isNotEmpty() && currentAnswer.isEmpty() -> {
                        currentQuestion += " " + line.trim()
                    }
                    line.trim().isNotEmpty() && currentAnswer.isNotEmpty() -> {
                        currentAnswer += " " + line.trim()
                    }
                }
            }

            // Add last question
            if (currentQuestion.isNotEmpty() && currentAnswer.isNotEmpty()) {
                questions.add(Question(
                    question = currentQuestion,
                    answer = currentAnswer,
                    type = "short"
                ))
            }

        } catch (e: Exception) {
            throw Exception("Questions parse karne me error: ${e.message}")
        }

        return questions
    }

    private fun parseLongQuestions(jsonResponse: String): List<Question> {
        // Similar parsing logic as short questions
        return parseShortQuestions(jsonResponse).map {
            it.copy(type = "long")
        }
    }

    private fun parseMCQs(jsonResponse: String): List<Question> {
        val questions = mutableListOf<Question>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val candidates = jsonObject.getJSONArray("candidates")
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            val lines = text.lines()
            var currentQuestion = ""
            val currentOptions = mutableListOf<String>()
            var correctAnswer = ""

            for (line in lines) {
                when {
                    line.trim().startsWith("Q", ignoreCase = true) -> {
                        if (currentQuestion.isNotEmpty() && currentOptions.isNotEmpty()) {
                            questions.add(Question(
                                question = currentQuestion,
                                answer = correctAnswer,
                                type = "mcq",
                                options = currentOptions.toList()
                            ))
                        }
                        currentQuestion = line.substringAfter(":").trim()
                        currentOptions.clear()
                        correctAnswer = ""
                    }
                    line.trim().matches(Regex("^[A-D]\\).*")) -> {
                        currentOptions.add(line.substringAfter(")").trim())
                    }
                    line.trim().startsWith("Correct Answer:", ignoreCase = true) -> {
                        correctAnswer = line.substringAfter(":").trim()
                    }
                }
            }

            // Add last question
            if (currentQuestion.isNotEmpty() && currentOptions.isNotEmpty()) {
                questions.add(Question(
                    question = currentQuestion,
                    answer = correctAnswer,
                    type = "mcq",
                    options = currentOptions.toList()
                ))
            }

        } catch (e: Exception) {
            throw Exception("MCQs parse karne me error: ${e.message}")
        }

        return questions
    }

    private var context: android.content.Context? = null

    fun setContext(context: android.content.Context) {
        this.context = context
    }

    private fun getApiKey(): String {
        context?.let {
            val prefsManager = PreferencesManager(it)
            val apiKey = prefsManager.getApiKey()
            if (apiKey.isNotEmpty()) {
                return apiKey
            }
        }
        throw Exception("API key nahi mili. Kripya Settings me jaake API key set karein.")
    }
}
