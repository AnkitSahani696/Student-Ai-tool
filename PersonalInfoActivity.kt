package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase.databinding.ActivityPersonalInfoBinding

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalInfoBinding
    private lateinit var preferenceManager: PreferenceManager
    private var resumeData: ResumeData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityPersonalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupToolbar()
        loadExistingData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Personal Information"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadExistingData() {
        resumeData = preferenceManager.getResumeData()
        resumeData?.personalInfo?.let { info ->
            binding.apply {
                etFullName.setText(info.fullName)
                etEmail.setText(info.email)
                etPhone.setText(info.phone)
                etAddress.setText(info.address)
                etLinkedin.setText(info.linkedin)
                etGithub.setText(info.github)
                etObjective.setText(info.objective)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            if (validateInputs()) {
                saveData()
                navigateToEducation()
            }
        }

        binding.btnSaveDraft.setOnClickListener {
            saveData()
            Toast.makeText(this, "Draft saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        // Validate full name
        if (!ValidationUtils.isValidName(fullName)) {
            binding.etFullName.error = "Please enter your full name"
            binding.etFullName.requestFocus()
            return false
        }

        // Validate email
        if (!ValidationUtils.isValidEmail(email)) {
            binding.etEmail.error = "Please enter a valid email"
            binding.etEmail.requestFocus()
            return false
        }

        // Validate phone
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.etPhone.error = "Please enter a valid phone number"
            binding.etPhone.requestFocus()
            return false
        }

        // Validate LinkedIn URL if provided
        val linkedin = binding.etLinkedin.text.toString().trim()
        if (linkedin.isNotEmpty() && !ValidationUtils.isValidUrl(linkedin)) {
            binding.etLinkedin.error = "Please enter a valid URL"
            binding.etLinkedin.requestFocus()
            return false
        }

        // Validate GitHub URL if provided
        val github = binding.etGithub.text.toString().trim()
        if (github.isNotEmpty() && !ValidationUtils.isValidUrl(github)) {
            binding.etGithub.error = "Please enter a valid URL"
            binding.etGithub.requestFocus()
            return false
        }

        return true
    }

    private fun saveData() {
        val personalInfo = PersonalInfo(
            fullName = binding.etFullName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            address = binding.etAddress.text.toString().trim(),
            linkedin = binding.etLinkedin.text.toString().trim(),
            github = binding.etGithub.text.toString().trim(),
            objective = binding.etObjective.text.toString().trim()
        )

        if (resumeData == null) {
            resumeData = ResumeData()
        }
        resumeData?.personalInfo = personalInfo

        preferenceManager.saveResumeData(resumeData!!)
    }

    private fun navigateToEducation() {
        val intent = Intent(this, EducationActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}