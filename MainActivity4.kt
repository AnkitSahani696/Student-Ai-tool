package com.example.firebase

import android.content.Intent

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase.FirebaseHelper
import com.example.firebase.databinding.ActivityMain4Binding

class MainActivity4 : AppCompatActivity() {

    private lateinit var binding: ActivityMain4Binding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityMain4Binding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseHelper.saveToolUsage("Resume Builder")

        preferenceManager = PreferenceManager(this)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Check if user has existing resume
        val existingResume = preferenceManager.getResumeData()
        if (existingResume != null) {
            binding.btnContinueResume.visibility = android.view.View.VISIBLE
            binding.tvWelcomeMessage.text = "Welcome back, ${existingResume.personalInfo.fullName}!"
        } else {
            binding.btnContinueResume.visibility = android.view.View.GONE
            binding.tvWelcomeMessage.text = "Create Your Professional Resume"
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateNew.setOnClickListener {
            // Clear any existing data
            preferenceManager.clearResumeData()
            navigateToPersonalInfo()
        }

        binding.btnContinueResume.setOnClickListener {
            navigateToPersonalInfo()
        }

        binding.btnTemplates.setOnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
        }

        binding.btnMyResumes.setOnClickListener {
            Toast.makeText(this, "My Resumes feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToPersonalInfo() {
        val intent = Intent(this, PersonalInfoActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        setupUI() // Refresh UI when returning from other activities
    }
}