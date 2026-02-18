package com.example.firebase.com.example.firebase


/**
 * Data class to represent a spelling error
 */
data class SpellError(
    val word: String,              // The incorrect word
    val position: Int,              // Position in text
    val suggestions: List<String>,  // Suggested corrections
    var isFixed: Boolean = false    // Whether user fixed it
)

/**
 * Data class for text analysis result
 */
data class TextAnalysisResult(
    val originalText: String,
    val errors: List<SpellError>,
    val totalWords: Int,
    val errorCount: Int,
    val accuracy: Float
) {
    fun getAccuracyPercentage(): String {
        return String.format("%.1f%%", accuracy * 100)
    }
}

/**
 * Enum for input type
 */
enum class InputType {
    IMAGE,
    PDF,
    TEXT
}