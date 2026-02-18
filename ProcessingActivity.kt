package com.example.firebase
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase.databinding.ActivityProcessingBinding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcessingBinding
    private lateinit var apiService: GeminiApiService2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pdfText = intent.getStringExtra("PDF_TEXT") ?: ""
        val pdfName = intent.getStringExtra("PDF_NAME") ?: "Document"

        apiService = GeminiApiService2()
        apiService.setContext(this)

        binding.tvPdfName.text = pdfName

        startQuestionGeneration(pdfText, pdfName)
    }

    private fun startQuestionGeneration(pdfText: String, pdfName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update UI - Generating Short Questions
                updateProgress("Short Questions generate ho rahe hain...", 33)
                val shortQuestions = apiService.generateShortQuestions(pdfText)

                // Update UI - Generating Long Questions
                updateProgress("Long Questions generate ho rahe hain...", 66)
                val longQuestions = apiService.generateLongQuestions(pdfText)

                // Update UI - Generating MCQs
                updateProgress("MCQs generate ho rahe hain...", 90)
                val mcqs = apiService.generateMCQs(pdfText)

                // Save to database
                val dbHelper = DatabaseHelper(this@ProcessingActivity)
                val pdfId = dbHelper.savePdfWithQuestions(
                    pdfName,
                    shortQuestions,
                    longQuestions,
                    mcqs
                )

                // Navigate to Questions Activity
                updateProgress("Complete! Questions taiyar hain.", 100)

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@ProcessingActivity, QuestionsActivity::class.java)
                    intent.putExtra("PDF_NAME", pdfName)
                    intent.putExtra("PDF_ID", pdfId)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProcessingActivity,
                        "Error: ${e.message}\n\nKripya internet connection check karein ya API key verify karein.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private suspend fun updateProgress(message: String, progress: Int) {
        withContext(Dispatchers.Main) {
            binding.tvStatus.text = message
            binding.progressBar.progress = progress
            binding.tvProgress.text = "$progress%"
        }
    }
}