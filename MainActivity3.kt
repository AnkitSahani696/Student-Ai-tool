package com.example.firebase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.FirebaseHelper
import com.example.firebase.databinding.ActivityMain3Binding

import com.example.firebase.databinding.ActivityMainBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity3 : AppCompatActivity() {

    private lateinit var binding: ActivityMain3Binding
    private lateinit var pdfHistoryAdapter: PdfHistoryAdapter
    private val pdfList = mutableListOf<PdfItem>()

    // PDF picker launcher
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handlePdfSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityMain3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseHelper.saveToolUsage("Question Paper")

        setupUI()
        setupRecyclerView()
        loadHistory()
    }

    private fun setupUI() {
        // Upload PDF button click
        binding.btnUploadPdf.setOnClickListener {
            openPdfPicker()
        }

        // About button
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity2::class.java))
        }
    }

    private fun setupRecyclerView() {
        pdfHistoryAdapter = PdfHistoryAdapter(pdfList) { pdfItem ->
            // Open questions for this PDF
            val intent = Intent(this, QuestionsActivity::class.java)
            intent.putExtra("PDF_NAME", pdfItem.name)
            intent.putExtra("PDF_ID", pdfItem.id)
            startActivity(intent)
        }

        binding.rvPdfHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity3)
            adapter = pdfHistoryAdapter
        }
    }

    private fun openPdfPicker() {
        pdfPickerLauncher.launch("application/pdf")
    }

    private fun handlePdfSelected(uri: Uri) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnUploadPdf.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Extract text from PDF
                val pdfExtractor = PdfExtractor(this@MainActivity3)
                val extractedText = pdfExtractor.extractText(uri)

                if (extractedText.isBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity3,
                            "PDF se text extract nahi ho saka. Kripya clear PDF upload karein.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnUploadPdf.isEnabled = true
                    }
                    return@launch
                }

                // Get PDF name
                val pdfName = pdfExtractor.getPdfName(uri)

                // Navigate to processing activity
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@MainActivity3, ProcessingActivity::class.java)
                    intent.putExtra("PDF_TEXT", extractedText)
                    intent.putExtra("PDF_NAME", pdfName)
                    startActivity(intent)

                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnUploadPdf.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity3,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnUploadPdf.isEnabled = true
                }
            }
        }
    }

    private fun loadHistory() {
        // Load from database or SharedPreferences
        val dbHelper = DatabaseHelper(this)
        pdfList.clear()
        pdfList.addAll(dbHelper.getAllPdfHistory())
        pdfHistoryAdapter.notifyDataSetChanged()

        // Show/hide empty state
        if (pdfList.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvPdfHistory.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvPdfHistory.visibility = android.view.View.VISIBLE
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Question Generator App")
            .setMessage("""
                📚 Student Helper App
                
                Ye app students ki madad karta hai apne study material se automatically questions generate karne me.
                
                Features:
                ✅ PDF upload
                ✅ Automatic Short Questions
                ✅ Automatic Long Questions  
                ✅ Automatic MCQs with answers
                ✅ Questions save & share
                
                Version: 1.0
                Developed with ❤️ for students
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }
}