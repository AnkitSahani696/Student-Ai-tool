package com.example.firebase


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {
    private var summaryText: TextView? = null
    private var keyPointsText: TextView? = null
    private var copyBtn: Button? = null
    private var shareBtn: Button? = null
    private var backBtn: Button? = null

    private var fullSummary: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_summary)


        // Initialize views
        summaryText = findViewById<TextView?>(R.id.summaryText)
        keyPointsText = findViewById<TextView?>(R.id.keyPointsText)
        copyBtn = findViewById<Button?>(R.id.copyBtn)
        shareBtn = findViewById<Button?>(R.id.shareBtn)
        backBtn = findViewById<Button?>(R.id.backBtn)


        // Get summary from intent
        val intent = getIntent()
        if (intent != null && intent.hasExtra("summary")) {
            fullSummary = intent.getStringExtra("summary")
            displaySummary(fullSummary!!)
        }


        // Copy button
        copyBtn!!.setOnClickListener(View.OnClickListener { v: View? -> copyToClipboard() })


        // Share button
        shareBtn!!.setOnClickListener(View.OnClickListener { v: View? -> shareContent() })


        // Back button
        backBtn!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })
    }

    private fun displaySummary(content: String) {
        try {
            // Split summary and key points
            val parts: Array<String?> =
                content.split("KEY POINTS:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (parts.size >= 1) {
                // Extract summary part
                val summaryPart = parts[0]!!.replace("SUMMARY:", "").trim { it <= ' ' }
                summaryText!!.setText(summaryPart)
            }

            if (parts.size >= 2) {
                // Extract key points part
                val keyPointsPart = parts[1]!!.trim { it <= ' ' }
                keyPointsText!!.setText(keyPointsPart)
            } else {
                keyPointsText!!.setText("Key points extract nahi ho sake.")
            }
        } catch (e: Exception) {
            // If parsing fails, show full content
            summaryText!!.setText(content)
            keyPointsText!!.setText("Key points alag se extract nahi ho sake.")
        }
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("PDF Summary", fullSummary)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "✅ Clipboard me copy ho gaya!", Toast.LENGTH_SHORT).show()
    }

    private fun shareContent() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "PDF Summary")
        shareIntent.putExtra(Intent.EXTRA_TEXT, fullSummary)
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}