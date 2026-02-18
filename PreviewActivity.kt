package com.example.firebase
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.firebase.databinding.ActivityPreviewBinding
import com.example.firebase.ResumeData

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var pdfGenerator: PdfGenerator
    private var resumeData: ResumeData? = null
    private var generatedPdfFile: File? = null

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        pdfGenerator = PdfGenerator(this)

        setupToolbar()
        loadResumeData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Preview & Generate"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadResumeData() {
        resumeData = preferenceManager.getResumeData()

        resumeData?.let { data ->
            displayPreview(data)
        } ?: run {
            Toast.makeText(this, "No resume data found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayPreview(data: ResumeData) {
        binding.apply {
            // Personal Info
            tvName.text = data.personalInfo.fullName
            tvEmail.text = data.personalInfo.email
            tvPhone.text = data.personalInfo.phone

            if (data.personalInfo.objective.isNotEmpty()) {
                tvObjective.text = data.personalInfo.objective
                tvObjective.visibility = android.view.View.VISIBLE
            }

            // Education
            if (data.education.isNotEmpty()) {
                val educationText = buildString {
                    data.education.forEach { edu ->
                        append("${edu.degree} - ${edu.fieldOfStudy}\n")
                        append("${edu.institution}, ${edu.year}\n")
                        append("${edu.percentage}\n\n")
                    }
                }
                tvEducation.text = educationText
            }

            // Skills
            if (data.skills.isNotEmpty()) {
                tvSkills.text = data.skills.joinToString(", ")
            }

            // Projects
            if (data.projects.isNotEmpty()) {
                val projectsText = buildString {
                    data.projects.forEach { project ->
                        append("• ${project.title}\n")
                        append("  ${project.description}\n")
                        if (project.technologies.isNotEmpty()) {
                            append("  Tech: ${project.technologies}\n")
                        }
                        append("\n")
                    }
                }
                tvProjects.text = projectsText
            }

            // Experience
            if (data.experience.isNotEmpty()) {
                val experienceText = buildString {
                    data.experience.forEach { exp ->
                        append("${exp.jobTitle} at ${exp.company}\n")
                        append("${exp.duration}\n")
                        append("${exp.description}\n\n")
                    }
                }
                tvExperience.text = experienceText
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnGeneratePdf.setOnClickListener {
            if (checkStoragePermission()) {
                generateLocalPDF()
            } else {
                requestStoragePermission()
            }
        }

        binding.btnUploadToApi.setOnClickListener {
            uploadResumeToApi()
        }

        binding.btnSharePdf.setOnClickListener {
            generatedPdfFile?.let { file ->
                sharePDF(file)
            } ?: run {
                Toast.makeText(this, "Please generate PDF first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEdit.setOnClickListener {
            onBackPressed()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // No permission needed for scoped storage
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun generateLocalPDF() {
        resumeData?.let { data ->
            binding.progressBar.visibility = android.view.View.VISIBLE
            binding.btnGeneratePdf.isEnabled = false

            lifecycleScope.launch {
                try {
                    val pdfFile = withContext(Dispatchers.IO) {
                        pdfGenerator.generateResumePDF(data, "${data.personalInfo.fullName}_Resume.pdf")
                    }

                    generatedPdfFile = pdfFile

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PreviewActivity,
                            "PDF generated successfully!\nLocation: ${pdfFile.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()

                        binding.btnSharePdf.visibility = android.view.View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PreviewActivity,
                            "Failed to generate PDF: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnGeneratePdf.isEnabled = true
                    }
                }
            }
        }
    }

    private fun uploadResumeToApi() {
        resumeData?.let { data ->
            binding.progressBar.visibility = android.view.View.VISIBLE
            binding.btnUploadToApi.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.createResume(data)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val result = response.body()!!
                            Toast.makeText(
                                this@PreviewActivity,
                                "Resume uploaded successfully!\n${result.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            // If API returns PDF URL, you can download it
                            result.pdfUrl?.let { url ->
                                // Handle PDF URL
                            }
                        } else {
                            Toast.makeText(
                                this@PreviewActivity,
                                "Failed to upload resume",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PreviewActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnUploadToApi.isEnabled = true
                    }
                }
            }
        }
    }

    private fun sharePDF(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Resume PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateLocalPDF()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
