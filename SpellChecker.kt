package com.example.firebase



import com.example.firebase.com.example.firebase.SpellError
import com.example.firebase.com.example.firebase.TextAnalysisResult
import kotlin.math.min

/**
 * Main Spell Checker Class
 * Handles English and Hindi spell checking
 */
class SpellChecker {

    /**
     * Check spelling for entire text
     */
    fun checkText(text: String): TextAnalysisResult {
        val words = text.split(Regex("\\s+"))
        val errors = mutableListOf<SpellError>()
        var position = 0

        for (word in words) {
            val cleanWord = word.replace(Regex("[^a-zA-Z\u0900-\u097F]"), "")

            if (cleanWord.isNotEmpty() && !isCorrect(cleanWord)) {
                val suggestions = getSuggestions(cleanWord)
                errors.add(
                    SpellError(
                        word = cleanWord,
                        position = position,
                        suggestions = suggestions
                    )
                )
            }
            position += word.length + 1
        }

        val totalWords = words.size
        val errorCount = errors.size
        val accuracy = if (totalWords > 0) {
            (totalWords - errorCount).toFloat() / totalWords
        } else {
            1.0f
        }

        return TextAnalysisResult(
            originalText = text,
            errors = errors,
            totalWords = totalWords,
            errorCount = errorCount,
            accuracy = accuracy
        )
    }

    /**
     * Check if a word is spelled correctly
     */
    private fun isCorrect(word: String): Boolean {
        val lowerWord = word.lowercase()

        // Check English dictionary
        if (Constants2.ENGLISH_DICTIONARY.contains(lowerWord)) {
            return true
        }

        // Check Hindi dictionary
        if (Constants2.HINDI_DICTIONARY.contains(word)) {
            return true
        }

        // Check if it's a number
        if (word.matches(Regex("\\d+"))) {
            return true
        }

        return false
    }

    /**
     * Get suggestions for misspelled word
     */
    fun getSuggestions(word: String): List<String> {
        val suggestions = mutableListOf<String>()

        // Check common mistakes first
        val correction = Constants2.COMMON_MISTAKES[word.lowercase()]
        if (correction != null) {
            suggestions.add(correction)
        }

        // Find similar words using Levenshtein distance
        val lowerWord = word.lowercase()
        val isHindi = word.matches(Regex(".*[\u0900-\u097F].*"))

        val dictionary = if (isHindi) {
            Constants2.HINDI_DICTIONARY
        } else {
            Constants2.ENGLISH_DICTIONARY
        }

        val similarWords = dictionary
            .filter { calculateDistance(it, lowerWord) <= 2 }
            .sortedBy { calculateDistance(it, lowerWord) }
            .take(5)

        suggestions.addAll(similarWords)

        // If no suggestions found, add generic ones
        if (suggestions.isEmpty()) {
            suggestions.add(word)
            suggestions.add("(No suggestions)")
        }

        return suggestions.distinct()
    }

    /**
     * Calculate Levenshtein Distance between two strings
     * Used for finding similar words
     */
    private fun calculateDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) {
            dp[i][0] = i
        }

        for (j in 0..len2) {
            dp[0][j] = j
        }

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Apply corrections to text
     */
    fun applyCorrections(
        originalText: String,
        errors: List<SpellError>
    ): String {
        var correctedText = originalText

        // Sort errors by position in reverse order
        // to maintain correct positions while replacing
        val sortedErrors = errors
            .filter { it.isFixed && it.suggestions.isNotEmpty() }
            .sortedByDescending { it.position }

        for (error in sortedErrors) {
            val replacement = error.suggestions.first()
            correctedText = correctedText.replaceFirst(
                error.word,
                replacement
            )
        }

        return correctedText
    }

    /**
     * Get statistics about the text
     */
    fun getTextStats(text: String): Map<String, Any> {
        val words = text.split(Regex("\\s+"))
        val sentences = text.split(Regex("[.!?]+"))
        val characters = text.length

        return mapOf(
            "words" to words.size,
            "sentences" to sentences.size,
            "characters" to characters,
            "paragraphs" to text.split("\n\n").size
        )
    }
}