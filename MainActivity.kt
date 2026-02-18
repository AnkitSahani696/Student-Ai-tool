package com.example.firebase


import android.content.Context.CONNECTIVITY_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.firebase.FirebaseHelper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var subjectInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var generateBtn: Button
    private lateinit var chatInput: TextInputEditText
    private lateinit var askBtn: Button
    private lateinit var resultContainer: LinearLayout
    private lateinit var chatContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tabLayout: LinearLayout
    private lateinit var notesTab: TextView
    private lateinit var mcqTab: TextView
    private lateinit var flashcardTab: TextView

    private var currentTab = "notes"
    private var generatedNotes = ""
    private var generatedMCQs = mutableListOf<MCQ>()
    private var generatedFlashcards = mutableListOf<Flashcard>()

    // Job for managing coroutines
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    // API Key - Replace with your key from https://aistudio.google.com/app/apikey
    private val GEMINI_API_KEY = "AIzaSyBs5KWsYnQBudIMYJbQhw61yz77NQZUmMs"

    init {
        // Validate API key on startup
        if (GEMINI_API_KEY.isEmpty() || GEMINI_API_KEY == "YOUR_API_KEY_HERE") {
            Log.e("StudyAssistant", "⚠️ Invalid API Key! Get from: https://aistudio.google.com/app/apikey")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        // TopicAiActivity.kt (jo file already hai)

// Jahan student ka topic generate ho raha hai, wahan ye line add karo:
        FirebaseHelper.saveToolUsage("Topic AI")

        initializeViews()
        setupListeners()

        // Check internet connectivity
        checkInternetConnection()
    }

    private fun checkInternetConnection() {
        try {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                Toast.makeText(this, "⚠️ No internet connection!", Toast.LENGTH_LONG).show()
            } else {
                Log.d("StudyAssistant", "✅ Internet connected")
            }
        } catch (e: Exception) {
            Log.e("StudyAssistant", "Connectivity check failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when activity is destroyed
        job.cancel()
    }

    private fun initializeViews() {
        try {
            subjectInput = findViewById(R.id.subjectInput)
            topicInput = findViewById(R.id.topicInput)
            generateBtn = findViewById(R.id.generateBtn)
            chatInput = findViewById(R.id.chatInput)
            askBtn = findViewById(R.id.askBtn)
            resultContainer = findViewById(R.id.resultContainer)
            chatContainer = findViewById(R.id.chatContainer)
            progressBar = findViewById(R.id.progressBar)
            tabLayout = findViewById(R.id.tabLayout)
            notesTab = findViewById(R.id.notesTab)
            mcqTab = findViewById(R.id.mcqTab)
            flashcardTab = findViewById(R.id.flashcardTab)

            Log.d("StudyAssistant", "✅ All views initialized successfully")
        } catch (e: Exception) {
            Log.e("StudyAssistant", "❌ View initialization failed: ${e.message}", e)
            Toast.makeText(this, "Layout error! Check XML files.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        generateBtn.setOnClickListener {
            val subject = subjectInput.text.toString().trim()
            val topic = topicInput.text.toString().trim()

            if (subject.isEmpty() || topic.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            generateContent(subject, topic)
        }

        askBtn.setOnClickListener {
            val question = chatInput.text.toString().trim()
            if (question.isEmpty()) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            answerQuestion(question)
        }

        notesTab.setOnClickListener { selectTab("notes") }
        mcqTab.setOnClickListener { selectTab("mcq") }
        flashcardTab.setOnClickListener { selectTab("flashcard") }
    }

    private fun selectTab(tab: String) {
        currentTab = tab

        notesTab.setBackgroundResource(R.drawable.tab_unselected)
        mcqTab.setBackgroundResource(R.drawable.tab_unselected)
        flashcardTab.setBackgroundResource(R.drawable.tab_unselected)

        when (tab) {
            "notes" -> {
                notesTab.setBackgroundResource(R.drawable.tab_selected)
                displayNotes()
            }
            "mcq" -> {
                mcqTab.setBackgroundResource(R.drawable.tab_selected)
                displayMCQs()
            }
            "flashcard" -> {
                flashcardTab.setBackgroundResource(R.drawable.tab_selected)
                displayFlashcards()
            }
        }
    }

    private fun generateContent(subject: String, topic: String) {
        progressBar.visibility = View.VISIBLE
        resultContainer.removeAllViews()
        generateBtn.isEnabled = false

        // Use scope instead of creating new CoroutineScope
        scope.launch {
            try {
                // Generate all content in IO dispatcher
                val notesDeferred = async(Dispatchers.IO) {
                    val notesPrompt = """
                        Create detailed study notes for the topic "$topic" in subject "$subject".
                        
                        IMPORTANT INSTRUCTIONS:
                        - Write ONLY in ENGLISH language
                        - Do NOT use Hindi or Hinglish
                        - Do NOT use hashtags (#) or markdown formatting
                        - Use simple bullet points with dashes (-) or numbers
                        - Keep paragraphs clear and readable
                        - Include examples where relevant
                        - Maximum 500 words
                        
                        Format the notes with:
                        1. Clear headings
                        2. Key points with simple bullets
                        3. Easy-to-understand explanations for students
                        4. Practical examples
                        
                        Write everything in plain English text only.
                    """.trimIndent()
                    cleanResponse(callGeminiAPI(notesPrompt))
                }

                val mcqDeferred = async(Dispatchers.IO) {
                    val mcqPrompt = """
                        Create exactly 5 multiple choice questions (MCQs) about "$topic" in "$subject".
                        
                        CRITICAL RULES:
                        - Write ONLY in ENGLISH language
                        - Do NOT use Hindi or Hinglish
                        - Do NOT use hashtags (#) or any markdown formatting
                        - Use EXACTLY this simple format:
                        
                        Q: What is photosynthesis?
                        A) Process of respiration
                        B) Process of making food in plants
                        C) Process of digestion
                        D) Process of reproduction
                        CORRECT: B
                        ---
                        
                        Make questions moderate difficulty for students.
                        Write in plain English only.
                        Separate each question with exactly "---"
                    """.trimIndent()
                    parseMCQs(cleanResponse(callGeminiAPI(mcqPrompt)))
                }

                val flashcardDeferred = async(Dispatchers.IO) {
                    val flashcardPrompt = """
                        Create exactly 6 flashcards about "$topic" in "$subject".
                        
                        STRICT REQUIREMENTS:
                        - Write ONLY in ENGLISH language
                        - Do NOT use Hindi or Hinglish
                        - Do NOT use hashtags (#) or markdown formatting
                        - Use EXACTLY this format:
                        
                        FRONT: What is the capital of India?
                        BACK: New Delhi
                        ---
                        
                        Make flashcards clear and educational.
                        Write in plain English only.
                        Separate each flashcard with exactly "---"
                    """.trimIndent()
                    parseFlashcards(cleanResponse(callGeminiAPI(flashcardPrompt)))
                }

                // Wait for all results
                generatedNotes = notesDeferred.await()
                generatedMCQs = mcqDeferred.await()
                generatedFlashcards = flashcardDeferred.await()

                // If parsing failed, create fallback content
                if (generatedMCQs.isEmpty()) {
                    generatedMCQs = createFallbackMCQs(topic)
                }
                if (generatedFlashcards.isEmpty()) {
                    generatedFlashcards = createFallbackFlashcards(topic)
                }

                // Update UI on Main thread
                progressBar.visibility = View.GONE
                generateBtn.isEnabled = true
                selectTab("notes")
                Toast.makeText(this@MainActivity, "Content generated successfully! ✅", Toast.LENGTH_SHORT).show()

            } catch (e: CancellationException) {
                // Coroutine was cancelled
                Log.d("StudyAssistant", "Content generation cancelled")
            } catch (e: SocketTimeoutException) {
                Log.e("StudyAssistant", "Timeout Error: ${e.message}", e)
                progressBar.visibility = View.GONE
                generateBtn.isEnabled = true
                Toast.makeText(this@MainActivity,
                    "⏱️ Request timeout! Internet is slow. Please try again.",
                    Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Log.e("StudyAssistant", "Network Error: ${e.message}", e)
                progressBar.visibility = View.GONE
                generateBtn.isEnabled = true
                Toast.makeText(this@MainActivity,
                    "🌐 Internet connection issue! Check WiFi/Data.",
                    Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("StudyAssistant", "Error: ${e.message}", e)
                progressBar.visibility = View.GONE
                generateBtn.isEnabled = true

                val errorMsg = when {
                    e.message?.contains("404") == true ->
                        "❌ API Model not found!\nUsing old Gemini version. Update code."
                    e.message?.contains("403") == true ->
                        "🔑 Invalid API key!\nGet new key from aistudio.google.com"
                    e.message?.contains("429") == true ->
                        "⏸️ Too many requests!\nWait 1 minute and try again"
                    e.message?.contains("400") == true ->
                        "⚠️ Bad request! Try different topic"
                    else ->
                        "❌ Error: ${e.message?.take(100)}\n\nCheck internet & API key"
                }

                Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun callGeminiAPI(prompt: String): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // IMPORTANT: Using gemini-2.5-flash
            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"
            val url = URL(apiUrl)
            connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000 // 30 seconds
            connection.readTimeout = 30000    // 30 seconds
            connection.doOutput = true
            connection.doInput = true

            val requestBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                    // Add these to prevent markdown formatting
                    put("topK", 40)
                    put("topP", 0.95)
                })
            }

            // Write request
            OutputStreamWriter(connection.outputStream).use {
                it.write(requestBody.toString())
                it.flush()
            }

            val responseCode = connection.responseCode
            Log.d("StudyAssistant", "Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                    it.readText()
                }
                Log.d("StudyAssistant", "✅ Response received: ${response.take(200)}...")

                val jsonResponse = JSONObject(response)
                return@withContext jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } else {
                val errorResponse = try {
                    BufferedReader(InputStreamReader(connection.errorStream)).use {
                        it.readText()
                    }
                } catch (e: Exception) {
                    "No error details available"
                }
                Log.e("StudyAssistant", "❌ Error Response: $errorResponse")
                throw Exception("API Error $responseCode: $errorResponse")
            }
        } catch (e: SocketTimeoutException) {
            throw SocketTimeoutException("Request timed out. Check internet speed.")
        } catch (e: IOException) {
            throw IOException("Network error: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    // New function to clean unwanted formatting from responses
    private fun cleanResponse(response: String): String {
        var cleaned = response

        // Remove markdown headers (###, ##, #)
        cleaned = cleaned.replace(Regex("#{1,6}\\s*"), "")

        // Remove bold markers (**text** or __text__)
        cleaned = cleaned.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        cleaned = cleaned.replace(Regex("__(.+?)__"), "$1")

        // Remove italic markers (*text* or _text_)
        cleaned = cleaned.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
        cleaned = cleaned.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)"), "$1")

        // Clean up excessive newlines
        cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")

        return cleaned.trim()
    }

    private fun parseMCQs(response: String): MutableList<MCQ> {
        val mcqs = mutableListOf<MCQ>()
        try {
            val questions = response.split("---")

            for (q in questions) {
                try {
                    val lines = q.trim().lines().filter { it.isNotEmpty() }
                    if (lines.size < 6) continue

                    val questionLine = lines.find {
                        val trimmed = it.trim()
                        trimmed.startsWith("Q:") || trimmed.startsWith("Q.") ||
                                trimmed.matches(Regex("^Q\\d+[:.)].*"))
                    }
                    if (questionLine == null) continue

                    val question = questionLine
                        .substringAfter(":")
                        .substringAfter(".")
                        .substringAfter(")")
                        .trim()

                    val options = mutableListOf<String>()
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.matches(Regex("^[A-D][).:].*"))) {
                            val option = trimmed
                                .substringAfter(")")
                                .substringAfter(".")
                                .substringAfter(":")
                                .trim()
                            if (option.isNotEmpty()) {
                                options.add(option)
                            }
                        }
                    }

                    if (options.size != 4) continue

                    val correctLine = lines.find {
                        it.contains("CORRECT", ignoreCase = true) ||
                                it.contains("Answer", ignoreCase = true)
                    }
                    if (correctLine == null) continue

                    val correctLetter = correctLine
                        .replace(":", "")
                        .replace(" ", "")
                        .filter { it.isLetter() }
                        .lastOrNull() ?: continue

                    val correctIndex = correctLetter.uppercaseChar() - 'A'

                    if (correctIndex in 0..3) {
                        mcqs.add(MCQ(question, options, correctIndex))
                    }
                } catch (e: Exception) {
                    Log.e("StudyAssistant", "Error parsing single MCQ: ${e.message}")
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e("StudyAssistant", "Error parsing MCQs: ${e.message}")
        }

        return mcqs
    }

    private fun parseFlashcards(response: String): MutableList<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()
        try {
            val cards = response.split("---")

            for (card in cards) {
                try {
                    val lines = card.trim().lines().filter { it.isNotEmpty() }
                    if (lines.size < 2) continue

                    val frontLine = lines.find { it.contains("FRONT", ignoreCase = true) }
                    val backLine = lines.find { it.contains("BACK", ignoreCase = true) }

                    if (frontLine == null || backLine == null) continue

                    val front = frontLine.substringAfter(":").trim()
                    val back = backLine.substringAfter(":").trim()

                    if (front.isNotEmpty() && back.isNotEmpty()) {
                        flashcards.add(Flashcard(front, back))
                    }
                } catch (e: Exception) {
                    Log.e("StudyAssistant", "Error parsing single flashcard: ${e.message}")
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e("StudyAssistant", "Error parsing flashcards: ${e.message}")
        }

        return flashcards
    }

    private fun createFallbackMCQs(topic: String): MutableList<MCQ> {
        return mutableListOf(
            MCQ(
                "What is the main concept of $topic?",
                listOf("Basic concept", "Advanced concept", "Complex theory", "Simple definition"),
                0
            ),
            MCQ(
                "$topic is related to which field?",
                listOf("Science", "Mathematics", "Technology", "All of the above"),
                3
            )
        )
    }

    private fun createFallbackFlashcards(topic: String): MutableList<Flashcard> {
        return mutableListOf(
            Flashcard("What is $topic?", "A concept that needs to be studied"),
            Flashcard("Why is $topic important?", "It helps in understanding the subject better")
        )
    }

    private fun displayNotes() {
        resultContainer.removeAllViews()

        if (generatedNotes.isEmpty()) {
            showEmptyState("📚 Generate content first to see notes!")
            return
        }

        val notesCard = layoutInflater.inflate(R.layout.item_notes, resultContainer, false)
        val titleText = notesCard.findViewById<TextView>(R.id.notesTitle)
        val contentText = notesCard.findViewById<TextView>(R.id.notesContent)

        titleText.text = "📝 Study Notes"
        contentText.text = generatedNotes

        resultContainer.addView(notesCard)
    }

    private fun displayMCQs() {
        resultContainer.removeAllViews()

        if (generatedMCQs.isEmpty()) {
            showEmptyState("❓ Generate content first to see MCQs!")
            return
        }

        generatedMCQs.forEachIndexed { index, mcq ->
            val mcqCard = layoutInflater.inflate(R.layout.item_mcq, resultContainer, false)
            val questionText = mcqCard.findViewById<TextView>(R.id.questionText)
            val optionsGroup = mcqCard.findViewById<RadioGroup>(R.id.optionsGroup)
            val submitBtn = mcqCard.findViewById<Button>(R.id.submitBtn)
            val resultText = mcqCard.findViewById<TextView>(R.id.resultText)

            questionText.text = "Q${index + 1}. ${mcq.question}"

            mcq.options.forEach { option ->
                val radioButton = RadioButton(this)
                radioButton.text = option
                radioButton.textSize = 16f
                radioButton.setPadding(16, 16, 16, 16)
                optionsGroup.addView(radioButton)
            }

            submitBtn.setOnClickListener {
                val selectedId = optionsGroup.checkedRadioButtonId
                if (selectedId == -1) {
                    Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedIndex = optionsGroup.indexOfChild(findViewById(selectedId))
                if (selectedIndex == mcq.correctAnswer) {
                    resultText.text = "✅ Correct Answer!"
                    resultText.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    resultText.text = "❌ Wrong! Correct answer: ${mcq.options[mcq.correctAnswer]}"
                    resultText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
                resultText.visibility = View.VISIBLE
                submitBtn.isEnabled = false
            }

            resultContainer.addView(mcqCard)
        }
    }

    private fun displayFlashcards() {
        resultContainer.removeAllViews()

        if (generatedFlashcards.isEmpty()) {
            showEmptyState("🎴 Generate content first to see flashcards!")
            return
        }

        generatedFlashcards.forEach { card ->
            val flashcardView = layoutInflater.inflate(R.layout.item_flashcard, resultContainer, false)
            val cardView = flashcardView.findViewById<CardView>(R.id.flashcardCard)
            val frontText = flashcardView.findViewById<TextView>(R.id.flashcardFront)
            val backText = flashcardView.findViewById<TextView>(R.id.flashcardBack)

            frontText.text = card.question
            backText.text = card.answer
            backText.visibility = View.GONE

            var isFlipped = false
            cardView.setOnClickListener {
                isFlipped = !isFlipped
                if (isFlipped) {
                    frontText.visibility = View.GONE
                    backText.visibility = View.VISIBLE
                } else {
                    frontText.visibility = View.VISIBLE
                    backText.visibility = View.GONE
                }
            }

            resultContainer.addView(flashcardView)
        }
    }

    private fun showEmptyState(message: String) {
        val textView = TextView(this)
        textView.text = message
        textView.textSize = 16f
        textView.setTextColor(getColor(android.R.color.darker_gray))
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textView.setPadding(32, 64, 32, 64)
        resultContainer.addView(textView)
    }

    private fun answerQuestion(question: String) {
        askBtn.isEnabled = false
        chatInput.isEnabled = false

        val loadingCard = layoutInflater.inflate(R.layout.item_chat, chatContainer, false)
        val questionView = loadingCard.findViewById<TextView>(R.id.questionView)
        val answerView = loadingCard.findViewById<TextView>(R.id.answerView)

        questionView.text = "Q: $question"
        answerView.text = "🤔 Thinking..."

        chatContainer.addView(loadingCard)
        chatInput.text?.clear()

        findViewById<ScrollView>(R.id.chatScrollView).post {
            findViewById<ScrollView>(R.id.chatScrollView).fullScroll(View.FOCUS_DOWN)
        }

        scope.launch {
            try {
                val answer = withContext(Dispatchers.IO) {
                    val prompt = """
                        Answer this student's question clearly and concisely.
                        
                        IMPORTANT RULES:
                        - Write ONLY in ENGLISH language
                        - Do NOT use Hindi or Hinglish
                        - Do NOT use hashtags (#) or markdown formatting
                        - Write in plain simple English
                        - Keep the answer educational and easy to understand
                        - Provide a helpful answer in 3-5 sentences
                        - Use simple words that students can understand
                        
                        Question: $question
                    """.trimIndent()

                    cleanResponse(callGeminiAPI(prompt))
                }

                answerView.text = "A: $answer"
                askBtn.isEnabled = true
                chatInput.isEnabled = true

                findViewById<ScrollView>(R.id.chatScrollView).post {
                    findViewById<ScrollView>(R.id.chatScrollView).fullScroll(View.FOCUS_DOWN)
                }

            } catch (e: CancellationException) {
                Log.d("StudyAssistant", "Chat cancelled")
            } catch (e: Exception) {
                Log.e("StudyAssistant", "Chat error: ${e.message}", e)
                answerView.text = "A: ❌ Error: ${e.message?.take(100)}\n\nCheck API key & internet."
                askBtn.isEnabled = true
                chatInput.isEnabled = true
            }
        }
    }

    data class MCQ(val question: String, val options: List<String>, val correctAnswer: Int)
    data class Flashcard(val question: String, val answer: String)
}