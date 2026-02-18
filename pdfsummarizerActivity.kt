package com.example.firebase

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.firebase.FirebaseHelper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class pdfsummarizerActivity : AppCompatActivity() {

    private val PICK_PDF_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100

    private lateinit var selectPdfBtn: Button
    private lateinit var processBtn: Button
    private lateinit var selectedFileText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedPdfUri: Uri? = null
    private var extractedText = ""

    // ✅ GEMINI API KEY - Yahan apni key paste karo
    private val GEMINI_API_KEY = "AIzaSyDbte05c_e1hqdLABH_tPSVFD54MzhEnGY "

    // Gemini API endpoint
    private val GEMINI_API_URL =
        "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_language)
        // PdfSummaryActivity.kt

// Jahan PDF ka summary ban raha hai, wahan:
        FirebaseHelper.saveToolUsage("PDF Summary")

        PDFBoxResourceLoader.init(applicationContext)

        selectPdfBtn = findViewById(R.id.selectPdfBtn)
        processBtn = findViewById(R.id.processBtn)
        selectedFileText = findViewById(R.id.selectedFileText)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        checkPermissions()

        selectPdfBtn.setOnClickListener {
            openFilePicker()
        }

        processBtn.setOnClickListener {
            selectedPdfUri?.let {
                extractAndSummarize()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission denied! App ko storage access chahiye.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, PICK_PDF_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedPdfUri = uri
                val fileName = uri.lastPathSegment ?: "Unknown"
                selectedFileText.text = "✅ Selected: $fileName"
                processBtn.isEnabled = true
                Toast.makeText(this, "PDF selected successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractAndSummarize() {
        showLoading(true, "PDF se text extract ho raha hai...")

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(selectedPdfUri!!)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                extractedText = stripper.getText(document)
                document.close()

                runOnUiThread {
                    statusText.text = "Text extracted! AI se summary generate ho rahi hai..."
                }

                // Limit text size (Gemini has 30,000 character limit)
                if (extractedText.length > 20000) {
                    extractedText = extractedText.substring(0, 20000)
                }

                // Call Gemini API
                callGeminiAPI(extractedText)

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false, "")
                    Toast.makeText(
                        this@pdfsummarizerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun callGeminiAPI(text: String) {
        try {
            // Create JSON request for Gemini API
            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val content = JSONObject().apply {
                    val parts = JSONArray()
                    val part = JSONObject().apply {
                        put("text", """
                            Ye ek student ke lecture notes hain. Mujhe iska:
                            1. Short summary (2-3 paragraphs me) - Hindi ya English me
                            2. Important key points (5-8 bullet points)
                            
                            Response format:
                            SUMMARY:
                            [summary yahan likho]
                            
                            KEY POINTS:
                            • [point 1]
                            • [point 2]
                            • [point 3]
                            ... (aur points)
                            
                            Text:
                            $text
                        """.trimIndent())
                    }
                    parts.put(part)
                    put("parts", parts)
                }
                contents.put(content)
                put("contents", contents)
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val mediaType = "application/json".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(GEMINI_API_URL)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        showLoading(false, "")
                        Toast.makeText(
                            this@pdfsummarizerActivity,
                            "Network Error: ${e.message}\nInternet connection check karo!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string() ?: ""

                        if (!response.isSuccessful) {
                            // Parse error message
                            val errorJson = JSONObject(responseBody)
                            val errorMessage = if (errorJson.has("error")) {
                                val error = errorJson.getJSONObject("error")
                                error.optString("message", "Unknown error")
                            } else {
                                "API call failed: $responseBody"
                            }

                            throw Exception(errorMessage)
                        }

                        // Parse Gemini response
                        val jsonResponse = JSONObject(responseBody)
                        val candidates = jsonResponse.getJSONArray("candidates")

                        if (candidates.length() == 0) {
                            throw Exception("No response from Gemini API")
                        }

                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        val summaryText = parts.getJSONObject(0).getString("text")

                        runOnUiThread {
                            showLoading(false, "")

                            val intent = Intent(this@pdfsummarizerActivity, SummaryActivity::class.java)
                            intent.putExtra("summary", summaryText)
                            startActivity(intent)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            showLoading(false, "")

                            // User-friendly error messages
                            val errorMsg = when {
                                e.message?.contains("API key") == true ->
                                    "❌ API Key invalid hai!\n\nSolution:\n1. Google AI Studio me jao\n2. Naya API key banao\n3. Code me paste karo"
                                e.message?.contains("quota") == true ->
                                    "❌ API limit exceed ho gaya!\n\nSolution:\n1. Wait karo (daily limit hai)\n2. Ya naya account banao"
                                e.message?.contains("billing") == true ->
                                    "❌ Billing enable nahi hai!\n\nSolution:\nGoogle Cloud Console me billing enable karo"
                                else ->
                                    "Parse Error: ${e.message}"
                            }

                            Toast.makeText(
                                this@pdfsummarizerActivity,
                                errorMsg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                showLoading(false, "")
                Toast.makeText(
                    this@pdfsummarizerActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean, message: String) {
        runOnUiThread {
            if (show) {
                progressBar.visibility = View.VISIBLE
                statusText.visibility = View.VISIBLE
                statusText.text = message
                selectPdfBtn.isEnabled = false
                processBtn.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                statusText.visibility = View.GONE
                selectPdfBtn.isEnabled = true
                processBtn.isEnabled = true
            }
        }
    }
}