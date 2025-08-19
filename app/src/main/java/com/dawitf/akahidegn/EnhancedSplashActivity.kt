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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Icon
import com.dawitf.akahidegn.ui.components.SafeUserRegistrationDialog
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class EnhancedSplashActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "EnhancedSplashActivity"
        const val WEBP_ANIMATION_DURATION_MS = 3000L // Duration of the WebP animation (3 seconds) - faster for better UX
        private const val MAX_AUTH_TIMEOUT_MS = 10000L // 10 seconds max for auth
        const val EXTRA_GOOGLE_ID_TOKEN = "google_id_token"
        const val EXTRA_IS_FIRST_TIME_USER = "is_first_time_user"
    }
    
    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    // State variables
    private var googleIdToken: String? = null
    private var hasNavigated = false
    private var authenticationComplete = false
    private var webpAnimationComplete = false
    private var isFirstTimeUser = false
    private var userDisplayName = ""
    private var authenticationFailed = false
    private var authFailureMessage = ""
    private var showSignInPrompt = false
    private var showRegistrationDialog = false
    
    // Authentication states
    enum class AuthState {
        CHECKING,           // Checking authentication in background
        AUTHENTICATED,      // User is authenticated, can proceed to main
        NEEDS_SIGNIN,      // User exists in Firebase but not signed in locally
        NEEDS_REGISTRATION, // New user, needs Google Sign-In + registration
        ERROR              // Authentication error
    }
    
    private var authState by mutableStateOf(AuthState.CHECKING)
    
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
        
        Log.d(TAG, "Starting Enhanced SplashActivity with smart authentication flow")
        
        // Initialize Firebase and CredentialManager
        try {
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()
            credentialManager = CredentialManager.create(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase/CredentialManager", e)
            authState = AuthState.ERROR
            authFailureMessage = "Initialization failed"
        }
        
        setContent {
            AkahidegnTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                EnhancedSplashScreen(
                    authState = authState,
                    authFailureMessage = authFailureMessage,
                    showSignInPrompt = showSignInPrompt,
                    showRegistrationDialog = showRegistrationDialog,
                    onSignInClick = { startGoogleSignInFlow() },
                    onRegistrationComplete = { name, phone ->
                        completeUserRegistration(name, phone)
                    },
                    onRetryAuth = { restartAuthenticationFlow() }
                )
            }
        }
        
        // Start the authentication check in background while WebP plays
        startBackgroundAuthenticationCheck()
        
        // Start WebP animation timer
        startWebpAnimationTimer()
        
        // Request location permission for new users
        requestLocationPermissionIfNeeded()
        
        // Maximum timeout protection
        Handler(Looper.getMainLooper()).postDelayed({
            if (!hasNavigated) {
                Log.w(TAG, "Maximum timeout reached, forcing navigation based on current state")
                handleNavigationBasedOnState()
            }
        }, MAX_AUTH_TIMEOUT_MS)
    }
    
    private fun startBackgroundAuthenticationCheck() {
        lifecycleScope.launch {
            try {
                checkUserAuthenticationStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Error in background authentication check", e)
                authState = AuthState.ERROR
                authFailureMessage = "Authentication check failed: ${e.message}"
            }
        }
    }
    
    private suspend fun checkUserAuthenticationStatus() {
        Log.d(TAG, "Checking user authentication status...")
        
        // Check if user is already signed in with Firebase
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            Log.d(TAG, "User is signed in with Firebase: ${currentUser.email}")
            
            // Check if user exists in database
            try {
                val userExists = checkUserExistsInDatabase(currentUser.uid)
                if (userExists) {
                    Log.d(TAG, "User exists in database, attempting to get fresh Google ID token")
                    
                    // Try to get a fresh Google credential first
                    try {
                        val hasStoredCredentials = tryGetStoredGoogleCredentials()
                        if (hasStoredCredentials) {
                            Log.d(TAG, "Successfully retrieved fresh Google credentials")
                            // googleIdToken should now be set by handleGoogleCredential
                        } else {
                            Log.w(TAG, "Could not retrieve fresh Google credentials, proceeding without token")
                            googleIdToken = null // Don't pass expired token
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get fresh Google credentials, proceeding without token", e)
                        googleIdToken = null // Clear potentially expired token
                    }
                    
                    userDisplayName = currentUser.displayName ?: ""
                    authState = AuthState.AUTHENTICATED
                    authenticationComplete = true
                    isFirstTimeUser = false
                    checkIfReadyToNavigate()
                } else {
                    Log.d(TAG, "User signed in but not in database, needs registration")
                    authState = AuthState.NEEDS_REGISTRATION
                    isFirstTimeUser = true
                    showRegistrationDialog = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking user in database", e)
                authState = AuthState.NEEDS_SIGNIN
                showSignInPrompt = true
            }
        } else {
            Log.d(TAG, "No current Firebase user, checking for stored credentials...")
            
            // Try to get saved Google credentials
            try {
                val hasStoredCredentials = tryGetStoredGoogleCredentials()
                if (hasStoredCredentials) {
                    Log.d(TAG, "Found stored credentials, attempting silent sign-in")
                    // This will be handled by the credential flow
                } else {
                    Log.d(TAG, "No stored credentials, user needs to sign in")
                    authState = AuthState.NEEDS_SIGNIN
                    showSignInPrompt = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking stored credentials", e)
                authState = AuthState.NEEDS_SIGNIN
                showSignInPrompt = true
            }
        }
    }
    
    private suspend fun tryGetStoredGoogleCredentials(): Boolean {
        return try {
            val serverClientId = getString(R.string.default_web_client_id)
            if (serverClientId.isEmpty()) {
                Log.e(TAG, "Web client ID is empty")
                return false
            }
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true) // Only get if already authorized
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true) // Auto-select for silent sign-in
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Attempting silent credential retrieval...")
            
            val result = withTimeout(5000L) { // 5 second timeout for silent sign-in
                credentialManager.getCredential(this@EnhancedSplashActivity, request)
            }
            
            handleGoogleCredential(result)
            true
            
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No stored credentials available")
            false
        } catch (e: GetCredentialException) {
            Log.d(TAG, "Could not retrieve stored credentials: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving stored credentials", e)
            false
        }
    }
    
    private suspend fun checkUserExistsInDatabase(uid: String): Boolean {
        return try {
            val snapshot = database.reference.child("users").child(uid).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user in database", e)
            false
        }
    }
    
    private fun startWebpAnimationTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            webpAnimationComplete = true
            Log.d(TAG, "WebP animation complete")
            checkIfReadyToNavigate()
        }, WEBP_ANIMATION_DURATION_MS)
    }
    
    private fun checkIfReadyToNavigate() {
        Log.d(TAG, "Checking if ready to navigate - Animation: $webpAnimationComplete, Auth: $authenticationComplete, State: $authState")
        
        if (webpAnimationComplete && authenticationComplete && authState == AuthState.AUTHENTICATED) {
            navigateToMainActivity()
        }
        // If animation is complete but auth is not, show appropriate UI (sign-in prompt or registration)
    }
    
    private fun startGoogleSignInFlow() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting Google Sign-In flow...")
                authState = AuthState.CHECKING
                showSignInPrompt = false
                
                if (!checkGooglePlayServicesAvailability()) {
                    authState = AuthState.ERROR
                    authFailureMessage = "Google Play Services unavailable"
                    return@launch
                }
                
                val serverClientId = getString(R.string.default_web_client_id)
                if (serverClientId.isEmpty()) {
                    Log.e(TAG, "Web client ID is empty")
                    authState = AuthState.ERROR
                    authFailureMessage = "Configuration error"
                    return@launch
                }
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // Allow account selection
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false) // Show account picker
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d(TAG, "Attempting credential retrieval...")
                
                val result = withTimeout(10000L) { // 10 second timeout
                    credentialManager.getCredential(this@EnhancedSplashActivity, request)
                }
                
                handleGoogleCredential(result)
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "Sign-in timed out", e)
                authState = AuthState.ERROR
                authFailureMessage = "Sign-in timed out"
                
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No credentials available", e)
                authState = AuthState.ERROR
                authFailureMessage = "Please sign in with Google"
                
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential error", e)
                authState = AuthState.ERROR
                authFailureMessage = "Sign-in failed"
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign-in error", e)
                authState = AuthState.ERROR
                authFailureMessage = "Authentication error"
            }
        }
    }
    
    private suspend fun handleGoogleCredential(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdToken = googleIdTokenCredential.idToken
                userDisplayName = googleIdTokenCredential.displayName ?: ""
                
                Log.d(TAG, "Google authentication successful for user: $userDisplayName")
                
                // Sign in to Firebase with Google credential
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val result = auth.signInWithCredential(authCredential).await()
                
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    // Get a fresh Firebase ID token to ensure validity
                    try {
                        val tokenResult = firebaseUser.getIdToken(true).await() // Force refresh
                        googleIdToken = tokenResult.token
                        Log.d(TAG, "Fresh Firebase token obtained after sign-in")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not refresh Firebase token, using Google token", e)
                        // Keep the Google ID token from the credential
                    }
                    
                    // Check if user exists in database
                    val userExists = checkUserExistsInDatabase(firebaseUser.uid)
                    
                    if (userExists) {
                        Log.d(TAG, "Returning user, proceeding to main activity")
                        authState = AuthState.AUTHENTICATED
                        authenticationComplete = true
                        isFirstTimeUser = false
                        checkIfReadyToNavigate()
                    } else {
                        Log.d(TAG, "New user, showing registration dialog")
                        authState = AuthState.NEEDS_REGISTRATION
                        isFirstTimeUser = true
                        showRegistrationDialog = true
                    }
                } else {
                    authState = AuthState.ERROR
                    authFailureMessage = "Firebase authentication failed"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Google credential", e)
                authState = AuthState.ERROR
                authFailureMessage = "Failed to process sign-in"
            }
        } else {
            Log.e(TAG, "Unexpected credential type")
            authState = AuthState.ERROR
            authFailureMessage = "Invalid credential type"
        }
    }
    
    private fun completeUserRegistration(name: String, phone: String) {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user != null && googleIdToken != null) {
                    // Save user data to Firebase Database
                    val userData = mapOf(
                        "name" to name,
                        "phone" to phone,
                        "email" to user.email,
                        "uid" to user.uid,
                        "photoUrl" to user.photoUrl?.toString(),
                        "createdAt" to System.currentTimeMillis()
                    )
                    
                    database.reference.child("users").child(user.uid).setValue(userData).await()
                    
                    // Save to SharedPreferences
                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("has_user_data", true)
                        .putString("user_name", name)
                        .putString("user_phone", phone)
                        .apply()
                    
                    Log.d(TAG, "User registration completed successfully")
                    showRegistrationDialog = false
                    authState = AuthState.AUTHENTICATED
                    authenticationComplete = true
                    checkIfReadyToNavigate()
                    
                } else {
                    Log.e(TAG, "No authenticated user or token for registration")
                    authState = AuthState.ERROR
                    authFailureMessage = "Registration failed - not authenticated"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error completing user registration", e)
                authState = AuthState.ERROR
                authFailureMessage = "Registration failed: ${e.message}"
            }
        }
    }
    
    private fun restartAuthenticationFlow() {
        authState = AuthState.CHECKING
        authFailureMessage = ""
        showSignInPrompt = false
        showRegistrationDialog = false
        authenticationComplete = false
        
        startBackgroundAuthenticationCheck()
    }
    
    private fun handleNavigationBasedOnState() {
        when (authState) {
            AuthState.AUTHENTICATED -> navigateToMainActivity()
            AuthState.NEEDS_SIGNIN -> showSignInPrompt = true
            AuthState.NEEDS_REGISTRATION -> showRegistrationDialog = true
            AuthState.ERROR -> {
                // Show error and allow retry
                showSignInPrompt = true
            }
            AuthState.CHECKING -> {
                // Still checking, show loading state or timeout to sign-in
                showSignInPrompt = true
            }
        }
    }
    
    private fun requestLocationPermissionIfNeeded() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Requesting location permission")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
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
    
    private fun navigateToMainActivity() {
        if (hasNavigated) return
        hasNavigated = true
        
        Log.d(TAG, "Navigating to MainActivity")
        
        // Show completion toast
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
            when (authState) {
                AuthState.AUTHENTICATED -> {
                    if (isFirstTimeUser) {
                        Toast.makeText(this, "Welcome to Akahidegn! Registration complete.", Toast.LENGTH_LONG).show()
                    } else {
                        val message = if (userDisplayName.isNotEmpty()) {
                            "Welcome back, $userDisplayName!"
                        } else {
                            "Welcome back!"
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
                AuthState.ERROR -> {
                    Toast.makeText(this, authFailureMessage, Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(this, "Please sign in to continue", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast", e)
        }
    }
}

@Composable
fun EnhancedSplashScreen(
    authState: EnhancedSplashActivity.AuthState,
    authFailureMessage: String,
    showSignInPrompt: Boolean,
    showRegistrationDialog: Boolean,
    onSignInClick: () -> Unit,
    onRegistrationComplete: (String, String) -> Unit,
    onRetryAuth: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var animationCompleted by remember { mutableStateOf(false) }
    
    // Animation values for smooth transitions
    val alphaAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ), label = "alpha"
    )
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.95f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ), label = "scale"
    )
    
    // Start animation and mark as completed after duration
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        delay(EnhancedSplashActivity.WEBP_ANIMATION_DURATION_MS)
        animationCompleted = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Clean white background
    ) {
        
        // Background WebP Animation - Always visible, stops animating after duration
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.splash_screen)
                .crossfade(true)
                .listener(
                    onStart = { Log.d("AsyncImage", "Loading splash screen WebP...") },
                    onSuccess = { _, _ -> Log.d("AsyncImage", "Splash screen WebP loaded successfully") },
                    onError = { _, result -> Log.e("AsyncImage", "Failed to load splash screen WebP: ${result.throwable}") }
                )
                .build(),
            contentDescription = stringResource(id = R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnimation)
                .scale(scaleAnimation)
        )
        
        // Authentication UI - Shows OVER the WebP animation after it completes
        if (animationCompleted) {
            // Semi-transparent overlay for better contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
            when {
                showRegistrationDialog -> {
                    // Show registration dialog for new users
                    SafeUserRegistrationDialog(
                        onComplete = { name, phone, avatar -> 
                            onRegistrationComplete(name, phone)
                        },
                        onDismiss = { } // Cannot dismiss - required for new users
                    )
                }
                
                showSignInPrompt || authState == EnhancedSplashActivity.AuthState.NEEDS_SIGNIN -> {
                    // Show sign-in prompt
                    SignInPromptCard(
                        onSignInClick = onSignInClick,
                        onRetryClick = onRetryAuth,
                        authFailureMessage = authFailureMessage,
                        isError = authState == EnhancedSplashActivity.AuthState.ERROR
                    )
                }
                
                authState == EnhancedSplashActivity.AuthState.CHECKING -> {
                    // Show loading indicator
                    AuthenticationLoadingCard()
                }
                
                authState == EnhancedSplashActivity.AuthState.ERROR -> {
                    // Show error state
                    ErrorStateCard(
                        message = authFailureMessage,
                        onRetryClick = onRetryAuth,
                        onSignInClick = onSignInClick
                    )
                }
            }
        }
        
        // Subtle loading indicator during WebP animation if checking auth
        if (!animationCompleted && authState == EnhancedSplashActivity.AuthState.CHECKING) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun SignInPromptCard(
    onSignInClick: () -> Unit,
    onRetryClick: () -> Unit,
    authFailureMessage: String,
    isError: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App icon or logo
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Welcome to Akahidegn",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (isError && authFailureMessage.isNotEmpty()) {
                Text(
                    text = authFailureMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Please sign in with your Google account to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (isError) {
                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun AuthenticationLoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Checking authentication...",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Please wait while we verify your account",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorStateCard(
    message: String,
    onRetryClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Authentication Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry")
                }
                
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign In")
                }
            }
        }
    }
}
