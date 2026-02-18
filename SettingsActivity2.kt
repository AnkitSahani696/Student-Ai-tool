package com.example.firebase
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase.databinding.ActivitySettings2Binding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.text.isEmpty
import kotlin.text.isNotEmpty
import kotlin.text.trim
import kotlin.text.trimIndent
import kotlin.toString

class SettingsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySettings2Binding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettings2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadSettings() {
        // Load saved API key
        val savedApiKey = prefsManager.getApiKey()
        if (savedApiKey.isNotEmpty()) {
            binding.etApiKey.setText(savedApiKey)
        }

        // Load other preferences
        binding.switchSaveHistory.isChecked = prefsManager.getSaveHistory()
        binding.switchAutoShare.isChecked = prefsManager.getAutoShare()
    }

    private fun setupListeners() {
        // Save API Key button
        binding.btnSaveApiKey.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Kripya API key enter karein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefsManager.saveApiKey(apiKey)
            Toast.makeText(this, "API key save ho gayi!", Toast.LENGTH_SHORT).show()
        }

        // Get API Key Help button
        binding.btnGetApiKey.setOnClickListener {
            showApiKeyHelp()
        }

        // Save History switch
        binding.switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setSaveHistory(isChecked)
        }

        // Auto Share switch
        binding.switchAutoShare.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setAutoShare(isChecked)
        }

        // Clear History button
        binding.btnClearHistory.setOnClickListener {
            clearHistory()
        }
    }

    private fun showApiKeyHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🔑 API Key Kaise Milega?")
            .setMessage("""
                Google Gemini API Key lene ke liye:
                
                1️⃣ https://makersuite.google.com/app/apikey par jaayein
                
                2️⃣ Google account se login karein
                
                3️⃣ "Create API Key" button pe click karein
                
                4️⃣ API key copy karein
                
                5️⃣ Yahan paste karke save karein
                
                ⭐ Free tier me 60 requests per minute milta hai, jo students ke liye kaafi hai!
                
                Note: API key ko safe rakhein aur kisi ke saath share na karein.
            """.trimIndent())
            .setPositiveButton("Samajh Gaya") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Website Open Karein") { _, _ ->
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://makersuite.google.com/app/apikey")
                )
                startActivity(intent)
            }
            .show()
    }

    private fun clearHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear History?")
            .setMessage("Kya aap saari PDF history delete karna chahte hain? Ye action undo nahi ho sakta.")
            .setPositiveButton("Clear") { _, _ ->
                val dbHelper = DatabaseHelper(this)
                dbHelper.clearAllHistory()

                Toast.makeText(
                    this,
                    "History cleared successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
