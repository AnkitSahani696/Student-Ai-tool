package com.example.firebase

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson


class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "ResumeBuilderPrefs"
        private const val KEY_RESUME_DATA = "resume_data"
        private const val KEY_IS_FIRST_TIME = "is_first_time"
        private const val KEY_SELECTED_TEMPLATE = "selected_template"
        private const val KEY_USER_NAME = "user_name"
    }

    fun saveResumeData(resumeData: ResumeData) {
        val json = gson.toJson(resumeData)
        sharedPreferences.edit().putString(KEY_RESUME_DATA, json).apply()
    }

    fun getResumeData(): ResumeData? {
        val json = sharedPreferences.getString(KEY_RESUME_DATA, null)
        return if (json != null) {
            gson.fromJson(json, ResumeData::class.java)
        } else {
            null
        }
    }

    fun clearResumeData() {
        sharedPreferences.edit().remove(KEY_RESUME_DATA).apply()
    }

    fun setFirstTime(isFirstTime: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_FIRST_TIME, isFirstTime).apply()
    }

    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_FIRST_TIME, true)
    }

    fun saveSelectedTemplate(templateId: Int) {
        sharedPreferences.edit().putInt(KEY_SELECTED_TEMPLATE, templateId).apply()
    }

    fun getSelectedTemplate(): Int {
        return sharedPreferences.getInt(KEY_SELECTED_TEMPLATE, 1)
    }

    fun saveUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
    }
}