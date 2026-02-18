package com.example.firebase
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.com.example.firebase.SpellError
import com.example.firebase.com.example.firebase.TextAnalysisResult

import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Result Activity
 * Shows spell checking results with errors highlighted
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var tvOriginalText: TextView
    private lateinit var tvStats: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var recyclerViewErrors: RecyclerView
    private lateinit var btnApplyCorrections: MaterialButton
    private lateinit var btnBackToHome: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val spellChecker = SpellChecker()
    private var analysisResult: TextAnalysisResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_result)

        initViews()
        processText()
        setupClickListeners()
    }

    private fun initViews() {
        tvOriginalText = findViewById(R.id.tvOriginalText)
        tvStats = findViewById(R.id.tvStats)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        recyclerViewErrors = findViewById(R.id.recyclerViewErrors)
        btnApplyCorrections = findViewById(R.id.btnApplyCorrections)
        btnBackToHome = findViewById(R.id.btnBackToHome)
        progressBar = findViewById(R.id.progressBar)

        recyclerViewErrors.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Process text and check spelling
     */
    private fun processText() {
        val text = intent.getStringExtra(Constants2.KEY_TEXT_DATA) ?: ""

        if (text.isBlank()) {
            Toast.makeText(this, "No text to check", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    spellChecker.checkText(text)
                }

                analysisResult = result
                displayResults(result)
                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@ResultActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Display analysis results
     */
    private fun displayResults(result: TextAnalysisResult) {
        // Display text with errors highlighted
        highlightErrors(result)

        // Display statistics
        tvStats.text = buildString {
            append("Total Words: ${result.totalWords}\n")
            append("Errors Found: ${result.errorCount}")
        }

        // Display accuracy
        val accuracyPercent = result.getAccuracyPercentage()
        tvAccuracy.text = "Accuracy: $accuracyPercent"
        tvAccuracy.setTextColor(getAccuracyColor(result.accuracy))

        // Show errors in RecyclerView
        if (result.errors.isEmpty()) {
            findViewById<TextView>(R.id.tvNoErrors).visibility = View.VISIBLE
            recyclerViewErrors.visibility = View.GONE
            btnApplyCorrections.isEnabled = false
        } else {
            findViewById<TextView>(R.id.tvNoErrors).visibility = View.GONE
            recyclerViewErrors.visibility = View.VISIBLE
            setupErrorsList(result.errors.toMutableList())
        }
    }

    /**
     * Highlight spelling errors in text
     */
    private fun highlightErrors(result: TextAnalysisResult) {
        val text = result.originalText
        val spannable = SpannableString(text)

        for (error in result.errors) {
            try {
                val start = text.indexOf(error.word, error.position)
                if (start != -1) {
                    val end = start + error.word.length

                    // Red background
                    spannable.setSpan(
                        BackgroundColorSpan(Color.parseColor("#FFCDD2")),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Red text
                    spannable.setSpan(
                        ForegroundColorSpan(Color.RED),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } catch (e: Exception) {
                // Skip if error in highlighting
            }
        }

        tvOriginalText.text = spannable
    }

    /**
     * Setup errors list with suggestions
     */
    private fun setupErrorsList(errors: MutableList<SpellError>) {
        val adapter = ErrorsAdapter(errors) { error, position ->
            showSuggestionsDialog(error, position, errors)
        }
        recyclerViewErrors.adapter = adapter
    }

    /**
     * Show suggestions dialog for an error
     */
    private fun showSuggestionsDialog(
        error: SpellError,
        position: Int,
        errors: MutableList<SpellError>
    ) {
        val suggestions = error.suggestions.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Suggestions for '${error.word}'")
            .setItems(suggestions) { dialog, which ->
                // Mark error as fixed with selected suggestion
                error.isFixed = true
                val selectedSuggestion = suggestions[which]

                // Update suggestions list to have selected one first
                val newSuggestions = mutableListOf(selectedSuggestion)
                newSuggestions.addAll(error.suggestions.filter { it != selectedSuggestion })

                val updatedError = error.copy(suggestions = newSuggestions)
                errors[position] = updatedError

                recyclerViewErrors.adapter?.notifyItemChanged(position)

                Toast.makeText(
                    this,
                    "Selected: $selectedSuggestion",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        btnApplyCorrections.setOnClickListener {
            applyCorrections()
        }

        btnBackToHome.setOnClickListener {
            navigateToHome()
        }
    }

    /**
     * Apply corrections to text
     */
    private fun applyCorrections() {
        val result = analysisResult ?: return

        val correctedText = spellChecker.applyCorrections(
            result.originalText,
            result.errors
        )

        // Show corrected text dialog
        AlertDialog.Builder(this)
            .setTitle("Corrected Text")
            .setMessage(correctedText)
            .setPositiveButton("Copy") { _, _ ->
                copyToClipboard(correctedText)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Copy text to clipboard
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Corrected Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Navigate back to home
     */
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity5::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    /**
     * Get color based on accuracy
     */
    private fun getAccuracyColor(accuracy: Float): Int {
        return when {
            accuracy >= 0.9f -> Color.parseColor("#4CAF50") // Green
            accuracy >= 0.7f -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#F44336") // Red
        }
    }

    /**
     * Show/hide loading
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnApplyCorrections.isEnabled = !show
        btnBackToHome.isEnabled = !show
    }
}

/**
 * RecyclerView Adapter for Errors List
 */
class ErrorsAdapter(
    private val errors: List<SpellError>,
    private val onErrorClick: (SpellError, Int) -> Unit
) : RecyclerView.Adapter<ErrorsAdapter.ErrorViewHolder>() {

    inner class ErrorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvErrorWord: TextView = view.findViewById(R.id.tvErrorWord)
        val tvSuggestion: TextView = view.findViewById(R.id.tvSuggestion)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ErrorViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_error, parent, false)
        return ErrorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ErrorViewHolder, position: Int) {
        val error = errors[position]

        holder.tvErrorWord.text = error.word

        val suggestionText = if (error.suggestions.isNotEmpty()) {
            "→ ${error.suggestions.first()}"
        } else {
            "(No suggestions)"
        }
        holder.tvSuggestion.text = suggestionText

        // Change color if fixed
        if (error.isFixed) {
            holder.tvErrorWord.setTextColor(Color.parseColor("#4CAF50"))
            holder.tvSuggestion.setTextColor(Color.parseColor("#4CAF50"))
        }

        holder.itemView.setOnClickListener {
            onErrorClick(error, position)
        }
    }

    override fun getItemCount() = errors.size
}