package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({

            val auth = FirebaseAuth.getInstance()

            if (auth.currentUser != null) {
                // ✅ Already logged in
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // ❌ Not logged in
                startActivity(Intent(this, StartActivity::class.java))
            }

            finish()

        }, 6000)
    }
}
