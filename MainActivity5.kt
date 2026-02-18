package com.example.firebase

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
 // ← Add this
import com.example.firebase.Constants2   // ← Add this
import com.example.firebase.FirebaseHelper

import com.example.firebase.com.example.firebase.InputType
import com.github.dhaval2404.imagepicker.ImagePicker

/**
 * Main Activity - Home Screen
 * Provides options: Scan Image, Upload PDF, Type Text
 */
class MainActivity5 : AppCompatActivity() {

    private var capturedImageUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                navigateToScanActivity2(uri)
            } else {
                Toast.makeText(this, "Failed to pick image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PDF picker launcher
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            navigateToPdfPreview(uri)
        } else {
            Toast.makeText(this, "Failed to pick PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main5)

        FirebaseHelper.saveToolUsage("Spelling Checker")

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Scan Image Button
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardScanImage)
            .setOnClickListener {
                showImageSourceDialog()
            }

        // Upload PDF Button
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardUploadPdf)
            .setOnClickListener {
                pickPdf()
            }

        // Type Text Button
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardTypeText)
            .setOnClickListener {
                navigateToPreviewActivity2()
            }
    }

    /**
     * Show dialog to choose camera or gallery
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    /**
     * Check camera permission
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            captureImage()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                Constants2.REQUEST_PERMISSIONS
            )
        }
    }

    /**
     * Capture image using camera
     */
    private fun captureImage() {
        ImagePicker.with(this)
            .cameraOnly()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }

    /**
     * Pick image from gallery
     */
    private fun pickImageFromGallery() {
        ImagePicker.with(this)
            .galleryOnly()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }

    /**
     * Pick PDF file
     */
    private fun pickPdf() {
        pdfPickerLauncher.launch("application/pdf")
    }

    /**
     * Navigate to Scan Activity with image
     */
    private fun navigateToScanActivity2(imageUri: Uri) {
        val intent = Intent(this, ScanActivity2::class.java).apply {
            putExtra(Constants2.KEY_IMAGE_URI, imageUri.toString())
        }
        startActivity(intent)
    }

    /**
     * Navigate to Preview Activity for PDF
     */
    private fun navigateToPdfPreview(pdfUri: Uri) {
        val intent = Intent(this, PreviewActivity2::class.java).apply {
            putExtra(Constants2.KEY_INPUT_TYPE, InputType.PDF.name)
            putExtra(Constants2.KEY_IMAGE_URI, pdfUri.toString())
        }
        startActivity(intent)
    }

    /**
     * Navigate to Preview Activity for direct text input
     */
    private fun navigateToPreviewActivity2() {
        val intent = Intent(this, PreviewActivity2::class.java).apply {
            putExtra(Constants2.KEY_INPUT_TYPE, InputType.TEXT.name)
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants2.REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                captureImage()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}