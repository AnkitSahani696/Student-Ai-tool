package com.example.firebase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firebase.com.example.firebase.InputType

import com.google.android.material.button.MaterialButton
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preview Activity
 * Shows extracted/typed text before spell checking
 */
class PreviewActivity2 : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var btnCheckSpelling: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var inputType: InputType = InputType.TEXT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_preview2)

        // Initialize PDF Box
        PDFBoxResourceLoader.init(applicationContext)

        initViews()
        loadData()
        setupClickListeners()
    }

    private fun initViews() {
        editText = findViewById(R.id.etTextInput)
        btnCheckSpelling = findViewById(R.id.btnCheckSpelling)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loadData() {
        val inputTypeString = intent.getStringExtra(Constants2.KEY_INPUT_TYPE)
        inputType = try {
            InputType.valueOf(inputTypeString ?: "TEXT")
        } catch (e: Exception) {
            InputType.TEXT
        }

        when (inputType) {
            InputType.IMAGE -> {
                // Text already extracted from image
                val text = intent.getStringExtra(Constants2.KEY_TEXT_DATA) ?: ""
                editText.setText(text)
            }
            InputType.PDF -> {
                // Extract text from PDF
                val uriString = intent.getStringExtra(Constants2.KEY_IMAGE_URI)
                if (uriString != null) {
                    extractPdfText(Uri.parse(uriString))
                }
            }
            InputType.TEXT -> {
                // User will type directly
                editText.hint = "Type or paste your text here..."
            }
        }
    }

    private fun setupClickListeners() {
        btnCheckSpelling.setOnClickListener {
            val text = editText.text.toString()

            if (text.isBlank()) {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkSpelling(text)
        }
    }

    /**
     * Extract text from PDF file
     */
    private fun extractPdfText(pdfUri: Uri) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val inputStream = contentResolver.openInputStream(pdfUri)
                    val document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    val extractedText = stripper.getText(document)
                    document.close()
                    inputStream?.close()
                    extractedText
                }

                showLoading(false)
                editText.setText(text)

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@PreviewActivity2,
                    "Failed to extract PDF text: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Navigate to Result Activity for spell checking
     */
    private fun checkSpelling(text: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(Constants2.KEY_TEXT_DATA, text)
        }
        startActivity(intent)
    }

    /**
     * Show/hide loading indicator
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnCheckSpelling.isEnabled = !show
        editText.isEnabled = !show
    }
}