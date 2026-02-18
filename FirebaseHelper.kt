package com.example.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun saveToolUsage(toolName: String) {
        val userId = auth.currentUser?.uid ?: "anonymous"

        val data = hashMapOf(
            "userId" to userId,
            "toolName" to toolName,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("toolUsage")
            .add(data)
            .addOnSuccessListener {
                Log.d("FirebaseHelper", "✅ Saved: $toolName")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "❌ Error: ${e.message}")
            }
    }

    fun updateLastActive() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .update("lastActive", FieldValue.serverTimestamp())
            .addOnSuccessListener {
                Log.d("FirebaseHelper", "✅ Last active updated")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "❌ Error: ${e.message}")
            }
    }
}