package com.example.firebase
import android.content.Context
import android.content.SharedPreferences

// Data Models
data class Question(
    val id: Long = 0,
    val question: String,
    val answer: String,
    val type: String, // "short", "long", "mcq"
    val options: List<String>? = null
)

data class PdfItem(
    val id: Long,
    val name: String,
    val date: String,
    val questionCount: Int
)

// Preferences Manager
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("QuestionGeneratorPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "AIzaSyBDWwalrLDxtjvDt_VhqTwUprXl235kIHA"
        private const val KEY_SAVE_HISTORY = "save_history"
        private const val KEY_AUTO_SHARE = "auto_share"
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setSaveHistory(save: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, save).apply()
    }

    fun getSaveHistory(): Boolean {
        return prefs.getBoolean(KEY_SAVE_HISTORY, true)
    }

    fun setAutoShare(autoShare: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SHARE, autoShare).apply()
    }

    fun getAutoShare(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SHARE, false)
    }
}
