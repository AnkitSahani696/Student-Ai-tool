package com.example.firebase

object Constants2 {

    // Intent Keys
    const val KEY_INPUT_TYPE = "input_type"
    const val KEY_TEXT_DATA = "text_data"
    const val KEY_IMAGE_URI = "image_uri"
    const val KEY_ANALYSIS_RESULT = "analysis_result"

    // Request Codes
    const val REQUEST_IMAGE_CAPTURE = 101
    const val REQUEST_IMAGE_PICK = 102
    const val REQUEST_PDF_PICK = 103
    const val REQUEST_PERMISSIONS = 104

    // Preferences
    const val PREF_NAME = "firebase_prefs"
    const val PREF_HISTORY = "scan_history"

    // File
    const val EXPORT_FOLDER = "DocProof"

    // English Common Words (Sample - expand this list)
    val ENGLISH_DICTIONARY = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their",
        "what", "so", "up", "out", "if", "about", "who", "get", "which", "go",
        "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
        "take", "people", "into", "year", "your", "good", "some", "could", "them",
        "see", "other", "than", "then", "now", "look", "only", "come", "its",
        "over", "think", "also", "back", "after", "use", "two", "how", "our",
        "work", "first", "well", "way", "even", "new", "want", "because", "any",
        "these", "give", "day", "most", "us", "is", "was", "are", "been", "has",
        "had", "were", "said", "did", "having", "may", "should", "could", "would",
        "text", "sample", "document", "file", "word", "spelling", "error", "correct",
        "check", "scan", "image", "photo", "picture", "upload", "download", "save",
        "hello", "world", "example", "test", "demo", "project", "app", "application"
    )

    // Hindi Common Words (Sample - expand this list)
    val HINDI_DICTIONARY = setOf(
        "और", "का", "एक", "में", "की", "है", "यह", "से", "हैं", "को",
        "पर", "इस", "होता", "कर", "ने", "था", "द्वारा", "होती", "हो", "गया",
        "किया", "हुआ", "हुई", "थी", "थे", "होने", "वाले", "बनी", "करने", "किए",
        "दिया", "हुए", "ऐसे", "ही", "साथ", "करते", "बाद", "लिए", "आप", "कहा",
        "वह", "के", "बड़ा", "यहाँ", "वहाँ", "कौन", "क्या", "कहाँ", "कब", "कैसे",
        "परीक्षा", "विद्यार्थी", "शिक्षक", "विद्यालय", "पुस्तक", "लेख", "दस्तावेज"
    )

    // Common Spelling Mistakes (for suggestions)
    val COMMON_MISTAKES = mapOf(
        "recieve" to "receive",
        "occured" to "occurred",
        "seperate" to "separate",
        "definately" to "definitely",
        "tommorow" to "tomorrow",
        "untill" to "until",
        "begining" to "beginning",
        "writting" to "writing",
        "developement" to "development",
        "enviroment" to "environment"
    )
}