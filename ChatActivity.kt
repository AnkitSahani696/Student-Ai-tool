package com.example.firebase

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.FirebaseHelper
import com.example.firebase.databinding.ActivityChatBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var apiService: GeminiApiService
    private lateinit var database: ChatDatabase
    private val messages = mutableListOf<Message>()

    private var selectedFileUri: Uri? = null
    private var currentPhotoUri: Uri? = null

    // ✅ File Picker Launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            handleFileSelection(it)
        }
    }

    // ✅ Image Picker Launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            handleImageSelection(it)
        }
    }

    // ✅ Camera Launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            selectedFileUri = currentPhotoUri
            handleImageSelection(currentPhotoUri!!)
        }
    }

    // ✅ Permission Launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseHelper.saveToolUsage("Chat AI")

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupPromptChips()

        apiService = GeminiApiService()
        database = ChatDatabase.getDatabase(this)

        showWelcomeMessage()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, ChatHistoryActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)

        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {

        // ✅ Send Button
        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        // ✅ Attach Button (Plus Icon)
        binding.btnAttach.setOnClickListener {
            showAttachmentOptions()
        }

        // ✅ Remove Image Button
        binding.btnRemoveImage.setOnClickListener {
            hideImagePreview()
        }
    }

    // ✅ Show Attachment Options Dialog
    private fun showAttachmentOptions() {
        val options = arrayOf("📷 Camera", "🖼️ Gallery", "📁 File")

        AlertDialog.Builder(this)
            .setTitle("Choose Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                    2 -> openFilePicker()
                }
            }
            .show()
    }

    // ✅ Check Camera Permission
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // ✅ Open Camera
    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            currentPhotoUri = photoUri
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Create Image File
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // ✅ Open Gallery
    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    // ✅ Open File Picker
    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    // ✅ Handle Image Selection (WITH PREVIEW)
    private fun handleImageSelection(uri: Uri) {
        val fileName = getFileName(uri)

        // Show image preview
        binding.ivPreview.setImageURI(uri)
        binding.tvFileName.text = fileName
        binding.imagePreviewCard.visibility = View.VISIBLE

        Toast.makeText(this, "Image selected: $fileName", Toast.LENGTH_SHORT).show()
    }

    // ✅ Handle File Selection (WITH PREVIEW)
    private fun handleFileSelection(uri: Uri) {
        val fileName = getFileName(uri)

        // Show file preview (with file icon)
        binding.ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
        binding.tvFileName.text = "📁 $fileName"
        binding.imagePreviewCard.visibility = View.VISIBLE

        Toast.makeText(this, "File selected: $fileName", Toast.LENGTH_SHORT).show()
    }

    // ✅ Hide Image Preview
    private fun hideImagePreview() {
        binding.imagePreviewCard.visibility = View.GONE
        selectedFileUri = null
        currentPhotoUri = null
    }

    // ✅ Get File Name from URI
    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            fileName = uri.lastPathSegment ?: "Unknown"
        }
        return fileName
    }

    private fun setupPromptChips() {

        binding.chipExplain.setOnClickListener {
            binding.etMessage.setText("Explain this topic in simple words: ")
            binding.etMessage.setSelection(binding.etMessage.text!!.length)
        }

        binding.chipAssignment.setOnClickListener {
            binding.etMessage.setText("Write an assignment on: ")
            binding.etMessage.setSelection(binding.etMessage.text!!.length)
        }

        binding.chipCode.setOnClickListener {
            binding.etMessage.setText("Generate Kotlin code for: ")
            binding.etMessage.setSelection(binding.etMessage.text!!.length)
        }

        binding.chipTranslate.setOnClickListener {
            binding.etMessage.setText("Translate this into English: ")
            binding.etMessage.setSelection(binding.etMessage.text!!.length)
        }
    }

    private fun showWelcomeMessage() {
        val welcomeMsg = Message(
            text = Constants.WELCOME_MESSAGE,
            isUser = false
        )
        chatAdapter.addMessage(welcomeMsg)
        scrollToBottom()
    }

    private fun sendMessage() {

        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isEmpty() && selectedFileUri == null) {
            Toast.makeText(this, "कृपया कुछ लिखें या file select करें", Toast.LENGTH_SHORT).show()
            return
        }

        // If image is attached, show in chat
        if (selectedFileUri != null) {
            val fileName = getFileName(selectedFileUri!!)
            val attachmentMsg = Message(
                text = if (messageText.isEmpty()) "📷 Image: $fileName" else "$messageText\n\n📷 Image: $fileName",
                isUser = true
            )
            chatAdapter.addMessage(attachmentMsg)
            scrollToBottom()
        } else if (messageText.isNotEmpty()) {
            // Add User Message
            val userMessage = Message(text = messageText, isUser = true)
            chatAdapter.addMessage(userMessage)
            scrollToBottom()
        }

        // Save User Message to Database
        lifecycleScope.launch {
            database.chatDao().insertMessage(
                ChatEntity(
                    message = messageText,
                    isUser = true
                )
            )
        }

        // Clear Input & Hide Preview
        binding.etMessage.text?.clear()
        hideImagePreview()

        // Show Loading
        showLoading(true)

        // AI Response
        getAIResponse(messageText)
    }

    private fun getAIResponse(userMessage: String) {

        lifecycleScope.launch {

            val result = apiService.getChatResponse(userMessage)

            showLoading(false)

            result.onSuccess { aiResponse ->

                val aiMessage = Message(text = aiResponse, isUser = false)
                chatAdapter.addMessage(aiMessage)
                scrollToBottom()

                // Save AI Response to Database
                database.chatDao().insertMessage(
                    ChatEntity(
                        message = aiResponse,
                        isUser = false
                    )
                )

            }.onFailure { error ->

                val errorMessage = Message(
                    text = "${Constants.ERROR_MESSAGE}\n\nError: ${error.message}",
                    isUser = false
                )

                chatAdapter.addMessage(errorMessage)
                scrollToBottom()

                Toast.makeText(
                    this@ChatActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabSend.isEnabled = !show
        binding.etMessage.isEnabled = !show
        binding.btnAttach.isEnabled = !show
    }

    private fun scrollToBottom() {
        binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
    }
}