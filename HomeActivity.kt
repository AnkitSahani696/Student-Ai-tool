package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailText = findViewById<TextView>(R.id.tvUserEmail)
        val btnNext = findViewById<MaterialButton>(R.id.btnNext)

        // Display user email
        emailText.text = auth.currentUser?.email

        // Google Sign-In Client setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Next Button
        btnNext.setOnClickListener {
            startActivity(Intent(this, CardViewActivity::class.java))
        }

        // ✅ Block check karo
        checkIfUserBlocked()
    }

    // ✅ Block check function
    private fun checkIfUserBlocked() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener

                if (document != null && document.exists()) {
                    val isBlocked = document.getBoolean("isBlocked") ?: false

                    if (isBlocked) {
                        // ✅ Firebase + Google dono logout karo
                        auth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInClient.revokeAccess().addOnCompleteListener {
                                Toast.makeText(
                                    this,
                                    "❌ Aapka account block hai!\nSupport se contact karein.",
                                    Toast.LENGTH_LONG
                                ).show()

                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
    }
}