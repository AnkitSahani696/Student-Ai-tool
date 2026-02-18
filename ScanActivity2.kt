package com.example.firebase


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firebase.com.example.firebase.InputType

import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Scan Activity
 * Processes image and extracts text using OCR
 */
class ScanActivity2 : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var btnExtractText: MaterialButton
    private lateinit var btnRetake: MaterialButton

    private var imageUri: Uri? = null
    private var imageBitmap: Bitmap? = null
    private val ocrProcessor = OCRProcessor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_scan2)

        initViews()
        loadImage()
        setupClickListeners()
    }

    private fun initViews() {
        imageView = findViewById(R.id.imageView)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.tvStatus)
        btnExtractText = findViewById(R.id.btnExtractText)
        btnRetake = findViewById(R.id.btnRetake)
    }

    private fun loadImage() {
        val uriString = intent.getStringExtra(Constants2.KEY_IMAGE_URI)

        if (uriString != null) {
            imageUri = Uri.parse(uriString)

            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri!!)
                imageBitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(imageBitmap)
                inputStream?.close()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        btnExtractText.setOnClickListener {
            extractText()
        }

        btnRetake.setOnClickListener {
            finish()
        }
    }

    /**
     * Extract text from image using OCR
     */
    private fun extractText() {
        if (imageBitmap == null) {
            Toast.makeText(this, "No image to process", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            statusText.text = "Extracting text from image..."

            try {
                val result = withContext(Dispatchers.IO) {
                    ocrProcessor.extractTextFromBitmap(imageBitmap!!)
                }

                showLoading(false)

                result.onSuccess { text ->
                    statusText.text = "Text extracted successfully!"
                    navigateToPreviewActivity2(text)
                }.onFailure { exception ->
                    statusText.text = "Failed to extract text"
                    Toast.makeText(
                        this@ScanActivity2,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                showLoading(false)
                statusText.text = "Error occurred"
                Toast.makeText(
                    this@ScanActivity2,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Navigate to Preview Activity with extracted text
     */
    private fun navigateToPreviewActivity2(text: String) {
        val intent = Intent(this, PreviewActivity2::class.java).apply {
            putExtra(Constants2.KEY_INPUT_TYPE, InputType.IMAGE.name)
            putExtra(Constants2.KEY_TEXT_DATA, text)
        }
        startActivity(intent)
        finish()
    }

    /**
     * Show/hide loading indicator
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnExtractText.isEnabled = !show
        btnRetake.isEnabled = !show
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrProcessor.close()
        imageBitmap?.recycle()
    }
}
