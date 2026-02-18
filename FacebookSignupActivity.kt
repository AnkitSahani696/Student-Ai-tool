package com.example.firebase

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageButton
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.security.MessageDigest

class FacebookSignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnSignup: MaterialButton
    private lateinit var btnGoogleSignUp: CardView
    private lateinit var btnFacebookSignUp: CardView
    private lateinit var tvLogin: TextView
    private lateinit var btnBack: ImageButton

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_facebooksignup)

        generateKeyHash()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()

        callbackManager = CallbackManager.Factory.create()

        initViews()
        configureGoogleSignIn()
        setClickListeners()
    }

    private fun generateKeyHash() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                android.util.Log.e("FB_KEY_HASH", "Key Hash: $keyHash")
            }
        } catch (e: Exception) {
            android.util.Log.e("FB_KEY_HASH", "Error: ${e.message}")
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnSignup = findViewById(R.id.btnSignup)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        btnFacebookSignUp = findViewById(R.id.btnFacebookSignUp)
        tvLogin = findViewById(R.id.tvLogin)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setClickListeners() {
        btnSignup.setOnClickListener { signUpWithEmailPassword() }
        btnGoogleSignUp.setOnClickListener { signUpWithGoogle() }
        btnFacebookSignUp.setOnClickListener { signUpWithFacebook() }
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        btnBack.setOnClickListener { finish() }
    }

    // ==================== FACEBOOK SIGNUP ====================
    private fun signUpWithFacebook() {
        LoginManager.getInstance().setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK)
        LoginManager.getInstance().logInWithReadPermissions(
            this, callbackManager, listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Toast.makeText(this@FacebookSignupActivity, "Facebook authentication successful!", Toast.LENGTH_SHORT).show()
                    handleFacebookAccessToken(result.accessToken.token)
                }
                override fun onCancel() {
                    Toast.makeText(this@FacebookSignupActivity, "Facebook login cancelled", Toast.LENGTH_SHORT).show()
                }
                override fun onError(error: FacebookException) {
                    Toast.makeText(this@FacebookSignupActivity, "Facebook login error: ${error.message}", Toast.LENGTH_LONG).show()
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
                                    // ✅ Existing user
                                    saveUserToFirestore(user, "Facebook")
                                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                } else {
                                    // ✅ New user
                                    createUserInDatabase(user)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Email already registered with different method.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Authentication failed: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun createUserInDatabase(user: FirebaseUser) {
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
                // ✅ Firestore mein bhi save karo
                saveUserToFirestore(user, "Facebook")
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ==================== EMAIL/PASSWORD SIGNUP ====================
    private fun signUpWithEmailPassword() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validateInput(name, email, password, confirmPassword)) return

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please accept Terms & Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        btnSignup.isEnabled = false
        btnSignup.text = "Creating Account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    auth.currentUser?.updateProfile(profileUpdates)

                    val userMap = mapOf(
                        "uid" to userId,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "accountType" to "Free",
                        "createdAt" to System.currentTimeMillis(),
                        "signInMethod" to "Email/Password"
                    )

                    database.child("Users").child(userId).setValue(userMap)
                        .addOnSuccessListener {
                            // ✅ Firestore mein bhi save karo
                            auth.currentUser?.let { user ->
                                saveUserToFirestore(user, "Email")
                            }
                            btnSignup.isEnabled = true
                            btnSignup.text = "Create Account"
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        }
                        .addOnFailureListener { e ->
                            btnSignup.isEnabled = true
                            btnSignup.text = "Create Account"
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    btnSignup.isEnabled = true
                    btnSignup.text = "Create Account"
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty()) { etName.error = "Name is required"; etName.requestFocus(); return false }
        if (name.length < 3) { etName.error = "Name must be at least 3 characters"; etName.requestFocus(); return false }
        if (email.isEmpty()) { etEmail.error = "Email is required"; etEmail.requestFocus(); return false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Please enter a valid email"; etEmail.requestFocus(); return false }
        if (password.isEmpty()) { etPassword.error = "Password is required"; etPassword.requestFocus(); return false }
        if (password.length < 6) { etPassword.error = "Password must be at least 6 characters"; etPassword.requestFocus(); return false }
        if (confirmPassword.isEmpty()) { etConfirmPassword.error = "Please confirm your password"; etConfirmPassword.requestFocus(); return false }
        if (password != confirmPassword) { etConfirmPassword.error = "Passwords do not match"; etConfirmPassword.requestFocus(); return false }
        return true
    }

    // ==================== GOOGLE SIGNUP ====================
    private fun signUpWithGoogle() {
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
                        database.child("Users").child(user.uid).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    // ✅ Existing user
                                    saveUserToFirestore(user, "Google")
                                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                } else {
                                    // ✅ New user
                                    val userMap = mapOf(
                                        "uid" to user.uid,
                                        "name" to (user.displayName ?: ""),
                                        "email" to (user.email ?: ""),
                                        "phone" to "",
                                        "accountType" to "Free",
                                        "createdAt" to System.currentTimeMillis(),
                                        "signInMethod" to "Google"
                                    )
                                    database.child("Users").child(user.uid).setValue(userMap)
                                        .addOnSuccessListener {
                                            // ✅ Firestore mein bhi save karo
                                            saveUserToFirestore(user, "Google")
                                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                            navigateToHome()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
            navigateToHome()
        }
    }
}