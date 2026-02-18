package com.example.firebase

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.isNotEmpty() && phone.length >= 10
    }

    fun isValidName(name: String): Boolean {
        return name.isNotEmpty() && name.length >= 2
    }

    fun isValidUrl(url: String): Boolean {
        return url.isEmpty() || Patterns.WEB_URL.matcher(url).matches()
    }

    fun getErrorMessage(field: String, value: String): String? {
        return when (field) {
            "email" -> if (!isValidEmail(value)) "Invalid email address" else null
            "phone" -> if (!isValidPhone(value)) "Invalid phone number" else null
            "name" -> if (!isValidName(value)) "Name must be at least 2 characters" else null
            "url" -> if (!isValidUrl(value)) "Invalid URL" else null
            else -> null
        }
    }
}
