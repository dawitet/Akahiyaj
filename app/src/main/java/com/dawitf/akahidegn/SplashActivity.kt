package com.dawitf.akahidegn

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
// Coil imports for animated WebP support
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.ui.theme.FontSize
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "SplashActivityNew"
        private const val MINIMUM_SPLASH_DURATION_MS = 3000L // 3 seconds minimum
        private const val MAX_AUTH_TIMEOUT_MS = 15000L // 15 seconds max for auth
        const val EXTRA_GOOGLE_ID_TOKEN = "google_id_token"
        const val EXTRA_IS_FIRST_TIME_USER = "is_first_time_user"
    }
    
    private lateinit var credentialManager: CredentialManager
    private var googleIdToken: String? = null
    private var isAuthenticationComplete = false
    private var splashStartTime = 0L
    private var hasNavigated = false
    private var isFirstTimeUser = false
    private var userDisplayName = ""
    private var authenticationFailed = false
    private var authFailureMessage = ""
    private var showLoadingOverlay = false
    
    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Location permission granted")
        } else {
            Log.w(TAG, "Location permission denied")
            Toast.makeText(this, "Location permission is needed for the app to work properly", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        splashStartTime = System.currentTimeMillis()
        Log.d(TAG, "Starting consolidated SplashActivity")
        
        // Initialize CredentialManager
        try {
            credentialManager = CredentialManager.create(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create CredentialManager", e)
            authenticationFailed = true
            authFailureMessage = "Authentication service unavailable"
        }
        
        // Check if user is returning (has previous auth data)
        isFirstTimeUser = checkIfFirstTimeUser()
        
        setContent {
            AkahidegnTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                SplashScreenWithOverlay()
            }
        }
        
        // Start the authentication and permission flow
        startAuthenticationFlow()
        
        // Ensure minimum 3-second display
        Handler(Looper.getMainLooper()).postDelayed({
            checkIfReadyToNavigate()
        }, MINIMUM_SPLASH_DURATION_MS)
        
        // Maximum timeout protection
        Handler(Looper.getMainLooper()).postDelayed({
            if (!hasNavigated) {
                Log.w(TAG, "Maximum timeout reached, forcing navigation")
                if (!isAuthenticationComplete) {
                    authenticationFailed = true
                    authFailureMessage = "Authentication timed out"
                }
                navigateToMainActivity()
            }
        }, MAX_AUTH_TIMEOUT_MS)
    }
    
    override fun onResume() {
        super.onResume()
        // If splash was paused/destroyed and restarted, reset timer
        if (splashStartTime == 0L) {
            splashStartTime = System.currentTimeMillis()
            Log.d(TAG, "SplashActivity resumed, resetting timer")
        }
    }
    
    private fun checkIfFirstTimeUser(): Boolean {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val hasUserData = prefs.getBoolean("has_user_data", false)
        
        // Also check FirebaseAuth current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        return !hasUserData && currentUser == null
    }
    
    private fun startAuthenticationFlow() {
        // Request location permission for first-time users
        if (isFirstTimeUser && !hasLocationPermission()) {
            Log.d(TAG, "First-time user: requesting location permission")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // Start Google authentication in background
        lifecycleScope.launch {
            try {
                authenticateWithGoogle()
            } catch (e: Exception) {
                Log.e(TAG, "Error in authentication flow", e)
                authenticationFailed = true
                authFailureMessage = "Authentication failed: ${e.message}"
                isAuthenticationComplete = true
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private suspend fun authenticateWithGoogle() {
        try {
            Log.d(TAG, "Starting Google authentication...")
            
            // Check Google Play Services
            if (!checkGooglePlayServicesAvailability()) {
                authenticationFailed = true
                authFailureMessage = "Google Play Services unavailable"
                isAuthenticationComplete = true
                return
            }
            
            val serverClientId = getString(R.string.default_web_client_id)
            if (serverClientId.isEmpty()) {
                Log.e(TAG, "Web client ID is empty")
                authenticationFailed = true
                authFailureMessage = "Configuration error"
                isAuthenticationComplete = true
                return
            }
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(!isFirstTimeUser) // For returning users, filter by authorized accounts
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(!isFirstTimeUser) // Auto-select for returning users
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Attempting credential retrieval...")
            
            val result = withTimeout(10000L) { // 10 second timeout
                credentialManager.getCredential(this@SplashActivity, request)
            }
            
            handleGoogleCredential(result)
            
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "Authentication timed out", e)
            authenticationFailed = true
            authFailureMessage = "Sign-in timed out"
            isAuthenticationComplete = true
            
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials available", e)
            authenticationFailed = true
            authFailureMessage = "Please sign in with Google on your device"
            isAuthenticationComplete = true
            
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential error", e)
            authenticationFailed = true
            authFailureMessage = "Sign-in failed"
            isAuthenticationComplete = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected authentication error", e)
            authenticationFailed = true
            authFailureMessage = "Authentication error"
            isAuthenticationComplete = true
        }
    }
    
    private fun checkGooglePlayServicesAvailability(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services available")
                true
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
            ConnectionResult.SERVICE_UPDATING -> {
                Log.w(TAG, "Google Play Services needs update")
                false
            }
            else -> {
                Log.w(TAG, "Google Play Services not available: $resultCode")
                false
            }
        }
    }
    
    private fun handleGoogleCredential(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdToken = googleIdTokenCredential.idToken
                userDisplayName = googleIdTokenCredential.displayName ?: ""
                
                Log.d(TAG, "Google authentication successful for user: $userDisplayName")
                
                // Save user data if first time
                if (isFirstTimeUser) {
                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_user_data", true).apply()
                }
                
                isAuthenticationComplete = true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Google credential", e)
                authenticationFailed = true
                authFailureMessage = "Failed to process sign-in"
                isAuthenticationComplete = true
            }
        } else {
            Log.e(TAG, "Unexpected credential type")
            authenticationFailed = true
            authFailureMessage = "Invalid credential type"
            isAuthenticationComplete = true
        }
    }
    
    private fun checkIfReadyToNavigate() {
        val elapsedTime = System.currentTimeMillis() - splashStartTime
        
        if (elapsedTime >= MINIMUM_SPLASH_DURATION_MS && isAuthenticationComplete) {
            // Both conditions met, navigate immediately
            navigateToMainActivity()
        } else if (elapsedTime >= MINIMUM_SPLASH_DURATION_MS && !isAuthenticationComplete) {
            // 3 seconds passed but auth still in progress, show overlay
            showLoadingOverlay = true
            Log.d(TAG, "Showing loading overlay while waiting for authentication")
        }
        // If auth completed but less than 3 seconds, wait for minimum time
    }
    
    private fun navigateToMainActivity() {
        if (hasNavigated) return
        hasNavigated = true
        
        Log.d(TAG, "Navigating to MainActivity")
        
        // Show appropriate toast
        showCompletionToast()
        
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            if (googleIdToken != null) {
                intent.putExtra(EXTRA_GOOGLE_ID_TOKEN, googleIdToken)
                intent.putExtra(EXTRA_IS_FIRST_TIME_USER, isFirstTimeUser)
            }
            
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to MainActivity", e)
            finish()
        }
    }
    
    private fun showCompletionToast() {
        try {
            if (authenticationFailed) {
                Toast.makeText(this, authFailureMessage, Toast.LENGTH_LONG).show()
            } else if (googleIdToken != null) {
                if (isFirstTimeUser) {
                    Toast.makeText(this, "Sign complete welcome!", Toast.LENGTH_LONG).show()
                } else {
                    val message = if (userDisplayName.isNotEmpty()) {
                        "Welcome back $userDisplayName"
                    } else {
                        "Welcome back!"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Failed please start the app again and make sure you're signed in with google on the device",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast", e)
        }
    }
}

@Composable
fun SplashScreenWithOverlay() {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Animation values
    val alphaAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ), label = "alpha"
    )
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ), label = "scale"
    )
    
    // Start animation
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Main splash content
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/splash_screen.webp")
                .crossfade(true)
                .placeholder(R.drawable.splash_fallback)
                .error(R.drawable.splash_fallback)
                .build(),
            contentDescription = stringResource(id = R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnimation)
                .scale(scaleAnimation)
                .padding(32.dp)
        )
        
        // Loading overlay (shown after 3 seconds if auth not complete)
        // This will be controlled by state from the activity
        // For now, just the splash animation
    }
}
