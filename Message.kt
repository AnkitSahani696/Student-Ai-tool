package com.example.firebase

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Gemini API Response models
data class GeminiResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: Content
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

// Gemini API Request model
data class GeminiRequest(
    val contents: List<RequestContent>
)

data class RequestContent(
    val parts: List<RequestPart>
)

data class RequestPart(
    val text: String
)