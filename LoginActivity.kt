package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogleSignIn: CardView
    private lateinit var btnFacebookSignIn: CardView
    private lateinit var tvSignup: TextView

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()

        callbackManager = CallbackManager.Factory.create()

        initViews()
        configureGoogleSignIn()
        setClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn)
        tvSignup = findViewById(R.id.tvSignup)
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setClickListeners() {
        btnLogin.setOnClickListener { loginWithEmailPassword() }
        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
        btnFacebookSignIn.setOnClickListener { signInWithFacebook() }
        tvSignup.setOnClickListener {
            startActivity(Intent(this, FacebookSignupActivity::class.java))
            finish()
        }
    }

    // ==================== FACEBOOK LOGIN ====================
    private fun signInWithFacebook() {
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK)
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            callbackManager,
            listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Facebook login successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    handleFacebookAccessToken(result.accessToken.token)
                }

                override fun onCancel() {
                    Toast.makeText(
                        this@LoginActivity,
                        "Facebook login cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        )
    }

    private fun handleFacebookAccessToken(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        database.child("Users").child(user.uid).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    // ✅ Existing user - Firestore update karo
                                    saveUserToFirestore(user, "Facebook")
                                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                } else {
                                    // ✅ New user - dono databases mein save karo
                                    val userMap = mapOf(
                                        "uid" to user.uid,
                                        "name" to (user.displayName ?: ""),
                                        "email" to (user.email ?: ""),
                                        "phone" to "",
                                        "accountType" to "Free",
                                        "createdAt" to System.currentTimeMillis(),
                                        "signInMethod" to "Facebook"
                                    )
                                    database.child("Users").child(user.uid).setValue(userMap)
                                        .addOnSuccessListener {
                                            saveUserToFirestore(user, "Facebook")
                                            Toast.makeText(
                                                this,
                                                "Account created successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navigateToHome()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                this,
                                                "Error: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Database error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ==================== EMAIL/PASSWORD LOGIN ====================
    private fun loginWithEmailPassword() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateInput(email, password)) return

        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    // ✅ Firestore mein update karo
                    auth.currentUser?.let { user ->
                        saveUserToFirestore(user, "Email")
                    }

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                } else {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }
        return true
    }

    // ==================== GOOGLE SIGN IN ====================
    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Toast.makeText(this, "Signing in with Google...", Toast.LENGTH_SHORT).show()
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {

                        // ✅ Pehle block check karo
                        FirebaseFirestore.getInstance()
                            .collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->

                                if (document.exists() &&
                                    document.getBoolean("isBlocked") == true) {

                                    // ✅ Blocked user - logout karo
                                    auth.signOut()
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleSignInClient.revokeAccess()
                                    }
                                    Toast.makeText(
                                        this,
                                        "❌ Aapka account block hai!\nSupport se contact karein.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {
                                    // ✅ Normal user - data save karo
                                    val userMap = mapOf(
                                        "uid" to user.uid,
                                        "name" to (user.displayName ?: ""),
                                        "email" to (user.email ?: ""),
                                        "phone" to "",
                                        "accountType" to "Free",
                                        "createdAt" to System.currentTimeMillis(),
                                        "signInMethod" to "Google"
                                    )
                                    database.child("Users").child(user.uid)
                                        .setValue(userMap)
                                        .addOnSuccessListener {
                                            saveUserToFirestore(user, "Google")
                                            Toast.makeText(
                                                this,
                                                "Google sign-in successful!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navigateToHome()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                // Error pe normal login
                                navigateToHome()
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ==================== FIRESTORE SAVE (NEW) ====================
    private fun saveUserToFirestore(user: FirebaseUser, loginMethod: String) {
        val userData = hashMapOf(
            "uid" to user.uid,
            "name" to (user.displayName ?: "Unknown"),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "isBlocked" to false,
            "loginProvider" to loginMethod,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastActive" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(user.uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("Firestore", "✅ User saved: ${user.email}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Firestore", "❌ Error: ${e.message}")
            }
    }

    // ==================== NAVIGATION ====================
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // ✅ Pehle block check karo phir navigate karo
            checkBlockedBeforeLogin(currentUser.uid)
        }
    }

    // ✅ Ye function add karo
    private fun checkBlockedBeforeLogin(uid: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isBlocked = document.getBoolean("isBlocked") ?: false

                    if (isBlocked) {
                        // ✅ Block hai - Google bhi logout karo
                        auth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInClient.revokeAccess()
                        }
                        Toast.makeText(
                            this,
                            "❌ Aapka account block hai!\nSupport se contact karein.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Normal login
                        navigateToHome()
                    }
                } else {
                    // User Firestore mein nahi hai - normal login
                    navigateToHome()
                }
            }
            .addOnFailureListener {
                // Error pe bhi home pe jao
                navigateToHome()
            }
    }
}