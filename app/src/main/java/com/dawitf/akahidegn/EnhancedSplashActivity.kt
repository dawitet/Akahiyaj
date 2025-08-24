package com.dawitf.akahidegn

import android.content.Intent
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import android.graphics.ImageDecoder
import com.dawitf.akahidegn.R

class EnhancedSplashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_GOOGLE_ID_TOKEN = "google_id_token"
        const val EXTRA_IS_FIRST_TIME_USER = "is_first_time_user"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private var uiState by mutableStateOf<UiState>(UiState.Loading)
    private var registrationName by mutableStateOf("")
    private var registrationPhone by mutableStateOf("")
    private var errorMessage by mutableStateOf<String?>(null)

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(Exception::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: Exception) {
            errorMessage = "Google sign-in failed. Please try again."
            uiState = UiState.SignIn
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object SignIn : UiState()
        object Registration : UiState()
        object Main : UiState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AkahidegnTheme {
                when (uiState) {
                    UiState.Loading -> EnhancedSplashScreen()
                    UiState.SignIn -> SignInCard(errorMessage) { launchGoogleSignIn() }
                    UiState.Registration -> RegistrationCard(
                        name = registrationName,
                        phone = registrationPhone,
                        error = errorMessage,
                        onNameChange = { registrationName = it },
                        onPhoneChange = { registrationPhone = it },
                        onRegister = { handleRegistration() }
                    )
                    UiState.Main -> { /* Should never be visible, handled in goToMain() */ }
                }
            }
        }

        lifecycleScope.launch {
            // Only check auth status, not Firestore, on launch
            if (auth.currentUser != null) {
                goToMain()
            } else {
                uiState = UiState.Loading
                // Show splash for a short time, then show sign-in card
                kotlinx.coroutines.delay(1200)
                uiState = UiState.SignIn
            }
        }
    }

    private fun launchGoogleSignIn() {
        errorMessage = null
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) {
            errorMessage = "Google sign-in failed. Please try again."
            uiState = UiState.SignIn
            return
        }
        uiState = UiState.Loading
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        // After sign-in, check Firestore for user profile
                        val user = auth.currentUser
                        if (user != null) {
                            try {
                                val userDoc = firestore.collection("users").document(user.uid).get().await()
                                if (userDoc.exists()) {
                                    goToMain()
                                } else {
                                    uiState = UiState.Registration
                                }
                            } catch (e: Exception) {
                                errorMessage = "Network error. Please try again."
                                uiState = UiState.SignIn
                            }
                        } else {
                            errorMessage = "Sign-in failed. Please try again."
                            uiState = UiState.SignIn
                        }
                    }
                } else {
                    errorMessage = "Firebase authentication failed."
                    uiState = UiState.SignIn
                }
            }
    }

    private fun handleRegistration() {
        val name = registrationName.trim()
        val phone = registrationPhone.trim()
        if (name.isEmpty() || phone.isEmpty()) {
            errorMessage = "Please enter your name and phone number."
            return
        }
        val user = auth.currentUser ?: return
        uiState = UiState.Loading
        val userMap = mapOf(
            "name" to name,
            "phone" to phone,
            "email" to (user.email ?: ""),
            "registrationTime" to System.currentTimeMillis(),
            "lastActive" to System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener { goToMain() }
            .addOnFailureListener {
                errorMessage = "Failed to save profile. Please try again."
                uiState = UiState.Registration
            }
    }

    private fun goToMain() {
        uiState = UiState.Main
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun EnhancedSplashScreen() {
    val context = LocalContext.current
    // Use remember to only create the ImageView once
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(ctx.resources, R.drawable.splash_screen)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = 1 // Play once
                        drawable.registerAnimationCallback(object : android.graphics.drawable.Animatable2.AnimationCallback() {
                            override fun onAnimationEnd(drawable: android.graphics.drawable.Drawable?) {
                                // Do nothing, just hold last frame
                            }
                        })
                        setImageDrawable(drawable)
                        drawable.start()
                    } else {
                        setImageDrawable(drawable)
                    }
                } else {
                    // Fallback for older Android: just show the first frame
                    setImageResource(R.drawable.splash_screen)
                }
            }
        }
    )
}

@Composable
fun SignInCard(error: String?, onSignIn: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Sign in to continue", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSignIn) {
                    Text("Sign in with Google")
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Color.Red, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RegistrationCard(
    name: String,
    phone: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRegister: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Complete Registration", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number") },
                    singleLine = true
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Color.Red, fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRegister) {
                    Text("Register")
                }
            }
        }
    }
}
