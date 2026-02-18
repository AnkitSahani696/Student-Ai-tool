package com.example.firebase

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class CardViewActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var searchBar: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Store all cards with their tags for searching
    private val cardMap = mutableMapOf<CardView, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_card_view_grid)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize views
        searchBar = findViewById(R.id.searchBar)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnMenu = findViewById(R.id.btnMenu)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Setup Navigation Drawer
        navigationView.setNavigationItemSelectedListener(this)

        // Set user info in header
        setupNavigationHeader()

        // Back Button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Menu Button - Open Drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Setup card clicks and store cards with their tags
        setupCardClicks()

        // Setup search functionality
        setupSearch()

        // Setup logout button
        setupLogoutButton()
    }

    private fun setupNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
        val ivProfileImage = headerView.findViewById<ImageView>(R.id.ivProfileImage)

        // Get current user info from Firebase
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvUserName.text = currentUser.displayName ?: "Inspire-Tool"
            tvUserEmail.text = currentUser.email ?: "inspiretool123@gmail.com"

            // Load profile image if available
            // You can use Glide or Picasso to load image from URL
            // Glide.with(this).load(currentUser.photoUrl).into(ivProfileImage)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_text_translator -> {
                Toast.makeText(this, "Topic-Ai clicked", Toast.LENGTH_SHORT).show()
                // Add your Text Translator activity intent here
                // startActivity(Intent(this, TextTranslatorActivity::class.java))
            }
            R.id.menu_image_resize -> {
                Toast.makeText(this, "PDF-Summarize clicked", Toast.LENGTH_SHORT).show()
                // Add your Image Resize activity intent here
                // startActivity(Intent(this, ImageResizeActivity::class.java))
            }
            R.id.menu_maths_solver -> {
                Toast.makeText(this, "CHAT clicked", Toast.LENGTH_SHORT).show()
                // Add your Maths Solver activity intent here
                // startActivity(Intent(this, MathsSolverActivity::class.java))
            }
            R.id.menu_text_scanner -> {
                Toast.makeText(this, "Question.G clicked", Toast.LENGTH_SHORT).show()
                // Add your Text Scanner activity intent here
                // startActivity(Intent(this, TextScannerActivity::class.java))
            }
            R.id.menu_object_detect -> {
                Toast.makeText(this, "Resume", Toast.LENGTH_SHORT).show()
                // Add your Object Detect activity intent here
                // startActivity(Intent(this, ObjectDetectActivity::class.java))
            }
            R.id.menu_spelling_solver -> {
                Toast.makeText(this, "Spelling-Solver", Toast.LENGTH_SHORT).show()
                // Add your Object Detect activity intent here
                // startActivity(Intent(this, ObjectDetectActivity::class.java))
            }
        }

        drawerLayout.closeDrawer(GravityCompat.END)
        return true
    }

    private fun setupCardClicks() {
        // Card 1: Home
        val cardHome = findViewById<CardView>(R.id.cardHome)
        cardHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        cardMap[cardHome] = "topic.ai"

        // Card 2: Language
        val cardLanguage = findViewById<CardView>(R.id.cardLanguage)
        cardLanguage.setOnClickListener {
            val intent = Intent(this, pdfsummarizerActivity::class.java)
            startActivity(intent)
        }
        cardMap[cardLanguage] = "pdf-summariz pdf summarizer"

        // Card 3: Chat
        val cardChat = findViewById<CardView>(R.id.cardChat)
        cardChat.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
        cardMap[cardChat] = "chat"

        // Card 4: Calendar
        val cardCalendar = findViewById<CardView>(R.id.cardCalendar)
        cardCalendar.setOnClickListener {
            val intent = Intent(this, MainActivity3::class.java)
            startActivity(intent)
        }
        cardMap[cardCalendar] = "question.g question generator"

        // Card 5: Website
        val cardWebsite = findViewById<CardView>(R.id.cardWebsite)
        cardWebsite.setOnClickListener {
            val intent = Intent(this, MainActivity4::class.java)
            startActivity(intent)
        }
        cardMap[cardWebsite] = "resume cv"

        // Card 6: Info
        val cardInfo = findViewById<CardView>(R.id.cardInfo)
        cardInfo.setOnClickListener {
            val intent = Intent(this, MainActivity5::class.java)
            startActivity(intent)
        }
        cardMap[cardInfo] = "info information"
    }

    private fun setupSearch() {
        // Search text change listener
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().toLowerCase().trim()
                filterCards(query)

                // Show/hide clear button
                btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear button click listener
        btnClearSearch.setOnClickListener {
            searchBar.text.clear()
            searchBar.clearFocus()
        }
    }

    private fun filterCards(query: String) {
        if (query.isEmpty()) {
            // Show all cards
            cardMap.keys.forEach { card ->
                card.visibility = View.VISIBLE
            }
        } else {
            // Filter cards based on query
            var visibleCount = 0
            cardMap.forEach { (card, tags) ->
                if (tags.contains(query)) {
                    card.visibility = View.VISIBLE
                    visibleCount++
                } else {
                    card.visibility = View.GONE
                }
            }
        }
    }

    private fun setupLogoutButton() {
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener {
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, StartActivity::class.java))
                finish()
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}