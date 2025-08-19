package com.dawitf.akahidegn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.dawitf.akahidegn.ui.navigation.Screen
import androidx.compose.material3.Scaffold
import androidx.navigation.NavType
import androidx.navigation.compose.navArgument
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.dawitf.akahidegn.ui.theme.HomeColorScheme
import com.dawitf.akahidegn.ui.theme.ActiveGroupsColorScheme
import com.dawitf.akahidegn.ui.theme.SettingsColorScheme
import com.dawitf.akahidegn.ui.screens.ActiveGroupsScreen
import com.dawitf.akahidegn.ui.screens.SettingsScreen
import com.dawitf.akahidegn.notifications.service.NotificationManagerService
import com.dawitf.akahidegn.ui.screens.MainScreen
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.dawitf.akahidegn.ui.components.UserRegistrationDialog
import com.dawitf.akahidegn.ui.components.GroupMembersDialog
import com.dawitf.akahidegn.ui.components.GroupMember
import com.dawitf.akahidegn.ui.components.UiEventHandler
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.app.AlertDialog
import android.widget.EditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.dawitf.akahidegn.core.error.ErrorHandler
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import com.dawitf.akahidegn.activityhistory.ActivityHistoryRepository
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel
import com.dawitf.akahidegn.ui.viewmodels.showQuickSuccess
import com.dawitf.akahidegn.ui.animation.ConfettiEmitter
import com.dawitf.akahidegn.ui.animation.shared.SharedElementsRoot
import androidx.compose.animation.AnimatedVisibility
import com.dawitf.akahidegn.ui.animation.shared.renderInSharedTransitionScopeOverlay
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.dawitf.akahidegn.features.profile.ProfileSyncService
import com.dawitf.akahidegn.features.auth.EnhancedAuthService
import com.dawitf.akahidegn.features.location.DeviceConsistencyService


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val animationViewModel: AnimationViewModel by viewModels()
    @Inject lateinit var errorHandler: ErrorHandler
    @Inject lateinit var activityHistoryRepository: ActivityHistoryRepository
    @Inject lateinit var notificationManagerService: NotificationManagerService
    @Inject lateinit var profileSyncService: ProfileSyncService
    @Inject lateinit var enhancedAuthService: EnhancedAuthService
    @Inject lateinit var deviceConsistencyService: DeviceConsistencyService

    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private val currentGroups = mutableListOf<Group>() // Consider if this is still needed or use ViewModel's state
    private var selectedGroupForDialog by mutableStateOf<Group?>(null)

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    private val _userLocationFlow = MutableStateFlow<Location?>(null)
    val userLocationFlow: StateFlow<Location?> = _userLocationFlow.asStateFlow()

    // Remove debug automation flags - these should not be in production
    // private var autoTestGroupCreated = false

    private lateinit var locationManager: LocationManager
    private var lastLocationUpdateTimestamp = 0L // Renamed for clarity

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Use DeviceConsistencyService for reliable location processing
            lifecycleScope.launch {
                try {
                    val reliableLocationResult = deviceConsistencyService.getReliableLocation()
                    when (reliableLocationResult) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            val validatedLocation = reliableLocationResult.data
                            _userLocationFlow.value = validatedLocation
                            
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastLocationUpdateTimestamp > 10000L) {
                                lastLocationUpdateTimestamp = currentTime
                                Log.d("LOCATION", "Reliable location updated: ${validatedLocation.latitude}, ${validatedLocation.longitude}. Accuracy: ${validatedLocation.accuracy}m")
                            }
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            Log.w("LOCATION", "DeviceConsistencyService failed, using raw location: ${reliableLocationResult.error}")
                            // Fall back to raw location if consistency service fails
                            _userLocationFlow.value = location
                            
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastLocationUpdateTimestamp > 10000L) {
                                lastLocationUpdateTimestamp = currentTime
                                Log.d("LOCATION", "Raw location updated (fallback): ${location.latitude}, ${location.longitude}")
                            }
                        }
                        is com.dawitf.akahidegn.core.result.Result.Loading -> {
                            Log.d("LOCATION", "DeviceConsistencyService still processing location")
                            // Use raw location while processing
                            _userLocationFlow.value = location
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOCATION", "Error processing location through DeviceConsistencyService", e)
                    // Emergency fallback - use raw location
                    _userLocationFlow.value = location
                }
            }
        }
        override fun onProviderEnabled(provider: String) {
            Log.d("LOCATION", "Location provider enabled: $provider")
        }
        override fun onProviderDisabled(provider: String) {
            Log.w("LOCATION", "Location provider disabled: $provider")
        }
    }

    // Removed isGroupCreator() as currentGroups might be deprecated in favor of ViewModel state

    private lateinit var sharedPreferences: SharedPreferences
    private var userName: String? = null

    companion object {
        val THEME_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivityAuth"
        
        // Google Play Services availability check constants
        const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
        const val CREDENTIAL_TIMEOUT_MS = 10000L // 10 seconds - standard timeout
        const val FIRST_LAUNCH_TIMEOUT_MS = 30000L // 30 seconds - for first app launch
        const val MAX_RETRY_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    // Note: Ensure no imports are placed inside the class body outside the imports section.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity onCreate - launched from SplashActivity")
        
        // Enable edge-to-edge for immersive UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Example of using errorHandler for a guarded call
        try {
            sharedPreferences = getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
        } catch (t: Throwable) {
            errorHandler.log(t, "prefs_init")
            Toast.makeText(this, errorHandler.toUserMessage(t), Toast.LENGTH_LONG).show()
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupLocationUpdates() // Request location updates

        MobileAds.initialize(this) { Log.d("ADS", "Mobile Ads SDK initialized") }

        database = Firebase.database
        groupsRef = database.reference.child("groups") // ViewModel uses this via initializeFirebase
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        loadRewardedAd()
        loadInterstitialAd()

        // Pass the location flow to the ViewModel once it's available
        mainViewModel.setUserLocationFlow(userLocationFlow)

        // Check if Google ID token was passed from EnhancedSplashActivity
        val googleIdTokenFromSplash = intent.getStringExtra(EnhancedSplashActivity.EXTRA_GOOGLE_ID_TOKEN)
        val isFirstTimeUser = intent.getBooleanExtra(EnhancedSplashActivity.EXTRA_IS_FIRST_TIME_USER, false)
        
        // Set up initial UI to prevent white screen
        setInitialLoadingContent()
        
        if (googleIdTokenFromSplash != null) {
            Log.d(TAG, "Google ID token received from SplashActivity - processing background authentication")
            Log.d(TAG, "First time user: $isFirstTimeUser")
            processGoogleIdTokenFromSplash(googleIdTokenFromSplash, isFirstTimeUser)
        } else if (auth.currentUser != null) {
            Log.d(TAG, "User already authenticated with Firebase, no token needed - proceeding to profile check")
            // User is already authenticated but no fresh Google token was available
            // This is normal after app process restart when Firebase auth persists
            verifyCurrentUserToken()
        } else {
            Log.d(TAG, "No current user and no token from splash, starting Google Sign-In flow")
            startGoogleSignInFlowWithRetry()
        }
    }

    /**
     * Sets up initial loading UI to prevent white screen during authentication
     */
    private fun setInitialLoadingContent() {
        setContent {
            AkahidegnTheme {
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Verifies that the current user's token is still valid
     * If invalid, starts fresh authentication flow
     */
    private fun verifyCurrentUserToken() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "verifyCurrentUserToken: currentUser is null")
            startGoogleSignInFlowWithRetry()
            return
        }
        
        Log.d(TAG, "Verifying token for user: ${currentUser.email}")
        
        // Check if we've verified the token recently (within the last hour)
        val lastTokenVerification = sharedPreferences.getLong("last_token_verification", 0)
        val currentTime = System.currentTimeMillis()
        val oneHourInMillis = 60 * 60 * 1000L // 1 hour
        
        if (currentTime - lastTokenVerification < oneHourInMillis) {
            Log.d(TAG, "Token was verified recently, skipping verification and proceeding")
            userName = currentUser.displayName ?: sharedPreferences.getString("user_name", null)
            val photoUrlFromFirebase = currentUser.photoUrl?.toString()
            if (photoUrlFromFirebase != null) {
                sharedPreferences.edit().putString("user_avatar_url", photoUrlFromFirebase).apply()
            }
            checkUserProfile() // This will call initializeMainScreen if profile is complete
            return
        }
        
        // Only force refresh if more than 1 hour has passed
        Log.d(TAG, "Token verification needed, checking token validity")
        currentUser.getIdToken(false) // Don't force refresh unless necessary
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Token verification successful, proceeding with profile check")
                    
                    // Save the current timestamp as last successful verification
                    sharedPreferences.edit().putLong("last_token_verification", currentTime).apply()
                    
                    userName = currentUser.displayName ?: sharedPreferences.getString("user_name", null)
                    val photoUrlFromFirebase = currentUser.photoUrl?.toString()
                    if (photoUrlFromFirebase != null) {
                        sharedPreferences.edit().putString("user_avatar_url", photoUrlFromFirebase).apply()
                    }
                    checkUserProfile() // This will call initializeMainScreen if profile is complete
                } else {
                    Log.w(TAG, "Token verification failed, user needs to re-authenticate", task.exception)
                    
                    // Clear the invalid auth state and timestamp
                    lifecycleScope.launch {
                        try {
                            auth.signOut()
                            sharedPreferences.edit().remove("last_token_verification").apply()
                            Log.d(TAG, "Signed out invalid user, starting fresh authentication")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error signing out invalid user", e)
                        }
                        
                        // Clear any cached token data
                        intent.removeExtra(EnhancedSplashActivity.EXTRA_GOOGLE_ID_TOKEN)
                        
                        // Start fresh authentication
                        Toast.makeText(this@MainActivity, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show()
                        startGoogleSignInFlowWithRetry()
                    }
                }
            }
    }

    /**
     * Checks if Google Play Services is available on this device.
     * Shows appropriate error messages for devices without GMS (like Huawei).
     * @return true if Google Play Services is available, false otherwise
     */
    private fun checkGooglePlayServicesAvailability(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is available")
                true
            }
            ConnectionResult.SERVICE_MISSING -> {
                Log.w(TAG, "Google Play Services is not installed")
                showGooglePlayServicesUnavailableDialog(
                    title = "Google Play Services Required",
                    message = "This app requires Google Play Services to function properly. Unfortunately, your device doesn't have Google Play Services installed.\n\nSupport for devices like Huawei is coming soon!",
                    canResolve = false
                )
                false
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.w(TAG, "Google Play Services needs to be updated")
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
                } else {
                    showGooglePlayServicesUnavailableDialog(
                        title = "Update Required",
                        message = "Google Play Services needs to be updated to use this app. Please update from the Google Play Store.",
                        canResolve = true
                    )
                }
                false
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Log.w(TAG, "Google Play Services is disabled")
                showGooglePlayServicesUnavailableDialog(
                    title = "Google Play Services Disabled",
                    message = "Google Play Services is disabled on your device. Please enable it in Settings > Apps to use this app.",
                    canResolve = true
                )
                false
            }
            else -> {
                Log.w(TAG, "Google Play Services availability check failed with code: $resultCode")
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
                } else {
                    showGooglePlayServicesUnavailableDialog(
                        title = "Google Play Services Issue",
                        message = "There's an issue with Google Play Services on your device. Support for alternative authentication methods is coming soon!",
                        canResolve = false
                    )
                }
                false
            }
        }
    }

    /**
     * Shows a dialog when Google Play Services is not available
     */
    private fun showGooglePlayServicesUnavailableDialog(title: String, message: String, canResolve: Boolean) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (!canResolve) {
                    // For devices like Huawei without GMS, gracefully exit
                    Toast.makeText(this, "Alternative authentication coming soon!", Toast.LENGTH_LONG).show()
                    finishAffinity() // Gracefully close the app
                }
            }
            .apply {
                if (canResolve) {
                    setNegativeButton("Exit") { _, _ ->
                        finishAffinity()
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Enhanced Google Sign-In flow with retry logic and increased timeout
     */
    private fun startGoogleSignInFlowWithRetry(retryAttempt: Int = 0) {
        if (!checkGooglePlayServicesAvailability()) {
            Log.e(TAG, "Cannot start Google Sign-In: Google Play Services not available")
            return
        }

        lifecycleScope.launch {
            try {
                startGoogleSignInFlow()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In attempt $retryAttempt failed", e)
                
                if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (retryAttempt + 1) + Random.nextLong(500) // Add jitter
                    Log.d(TAG, "Retrying Google Sign-In in ${delayMs}ms (attempt ${retryAttempt + 1})")
                    
                    // Show optimistic UI feedback
                    Toast.makeText(this@MainActivity, "Retrying authentication...", Toast.LENGTH_SHORT).show()
                    
                    delay(delayMs)
                    startGoogleSignInFlowWithRetry(retryAttempt + 1)
                } else {
                    Log.e(TAG, "All Google Sign-In retry attempts failed")
                    Toast.makeText(this@MainActivity, "Authentication failed after multiple attempts. Please try again later.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startGoogleSignInFlow() {
        lifecycleScope.launch {
            val serverClientId = getString(R.string.default_web_client_id)
            if (serverClientId.isEmpty()) {
                Log.e(TAG, "R.string.default_web_client_id is empty. Cannot start Google Sign-In.")
                Toast.makeText(this@MainActivity, "Sign-in configuration error.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val isFirstLaunch = hasBeenLaunchedBefore()
            if (isFirstLaunch) {
                Log.d(TAG, "First launch detected - applying pre-authentication delay")
                Toast.makeText(this@MainActivity, "Setting up Google services for first-time use...", Toast.LENGTH_LONG).show()
                // Give Google services time to initialize on first launch
                kotlinx.coroutines.delay(2000)
            }

            // Show loading state for optimistic UI
            Toast.makeText(this@MainActivity, "Connecting to Google...", Toast.LENGTH_SHORT).show()

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false) // Allow user to select account explicitly
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                Log.d(TAG, "Attempting to get credential from CredentialManager with extended timeout...")
                
                // Use withTimeout to implement custom timeout handling
                val result = kotlinx.coroutines.withTimeout(CREDENTIAL_TIMEOUT_MS) {
                    credentialManager.getCredential(this@MainActivity, request)
                }
                
                Log.d(TAG, "GetCredential SUCCESS from CredentialManager")
                handleGoogleCredential(result)
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "Google Sign-In timed out after ${CREDENTIAL_TIMEOUT_MS}ms", e)
                Toast.makeText(this@MainActivity, "First-time authentication setup in progress. This may take up to 30 seconds. Please wait...", Toast.LENGTH_LONG).show()
                
                // Fallback - retry with a longer timeout for first launch (30 seconds)
                try {
                    val fallbackResult = kotlinx.coroutines.withTimeout(FIRST_LAUNCH_TIMEOUT_MS) {
                        credentialManager.getCredential(this@MainActivity, request)
                    }
                    Log.d(TAG, "GetCredential SUCCESS on fallback attempt")
                    handleGoogleCredential(fallbackResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Fallback Google Sign-In also failed after ${FIRST_LAUNCH_TIMEOUT_MS}ms", e)
                    handleAuthenticationFailure(e)
                }
                
            } catch (e: NoCredentialException) {
                Log.w(TAG, "NoGoogleCredentialException: No credentials available.", e)
                
                // Special handling for first launch - Google services might need more time
                if (isFirstLaunch) {
                    Log.d(TAG, "First launch NoCredentialException - retrying after delay")
                    Toast.makeText(this@MainActivity, "Initializing Google accounts for first use. Please wait...", Toast.LENGTH_LONG).show()
                    
                    // Wait for Google services to fully initialize
                    kotlinx.coroutines.delay(5000)
                    
                    try {
                        val retryResult = credentialManager.getCredential(this@MainActivity, request)
                        Log.d(TAG, "GetCredential SUCCESS on NoCredentialException retry")
                        handleGoogleCredential(retryResult)
                    } catch (retryException: Exception) {
                        Log.e(TAG, "Retry after NoCredentialException failed", retryException)
                        Toast.makeText(this@MainActivity, "No Google accounts found on this device. Please add a Google account in Settings.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "No Google accounts found on this device.", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException from CredentialManager", e)
                handleAuthenticationFailure(e)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Google Sign-In", e)
                handleAuthenticationFailure(e)
            }
        }
    }

    /**
     * Handles authentication failures with appropriate user feedback
     */
    private fun handleAuthenticationFailure(exception: Exception) {
        val message = when {
            exception.message?.contains("NETWORK_ERROR") == true -> 
                "Network error. Please check your internet connection and try again."
            exception.message?.contains("DEVELOPER_ERROR") == true -> 
                "Configuration error. Please contact support."
            exception.message?.contains("CANCELED") == true -> 
                "Sign-in was cancelled."
            else -> 
                "Google Sign-In failed. Please try again."
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Checks if this is the first time the app has been launched
     * Uses SharedPreferences to track launch state
     */
    private fun hasBeenLaunchedBefore(): Boolean {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasLaunched = prefs.getBoolean("has_launched_before", false)
        
        if (!hasLaunched) {
            // Mark as launched for future checks
            prefs.edit().putBoolean("has_launched_before", true).apply()
            return true // This is the first launch
        }
        
        return false // Not the first launch
    }

    private fun handleGoogleCredential(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                val photoUri = googleIdTokenCredential.profilePictureUri
                Log.d(TAG, "Google ID Token: $googleIdToken ProfilePicUri: $photoUri")

                if (photoUri != null) {
                    sharedPreferences.edit().putString("user_avatar_url", photoUri.toString()).apply()
                }
                firebaseAuthWithGoogleIdToken(googleIdToken)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create GoogleIdTokenCredential or other error", e)
                Toast.makeText(this, getString(R.string.toast_sign_in_token_error), Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential.type}")
            Toast.makeText(this, getString(R.string.toast_sign_in_unexpected_credential), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Processes Google ID token received from SplashActivity background authentication
     */
    private fun processGoogleIdTokenFromSplash(googleIdToken: String, isFirstTimeUser: Boolean = false) {
        Log.d(TAG, "Processing Google ID token from SplashActivity background authentication")
        Log.d(TAG, "Is first time user: $isFirstTimeUser")
        
        lifecycleScope.launch {
            try {
                // Directly authenticate with Firebase using the token from splash
                firebaseAuthWithGoogleIdToken(googleIdToken, isFirstTimeUser)
                
                // No toast message - seamless authentication
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Google ID token from splash", e)
                Toast.makeText(this@MainActivity, "Background authentication failed. Please try manual sign-in.", Toast.LENGTH_LONG).show()
                
                // Fallback to manual sign-in
                startGoogleSignInFlowWithRetry()
            }
        }
    }

    private fun firebaseAuthWithGoogleIdToken(idToken: String, isFirstTimeUser: Boolean = false) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Auth with Google ID Token: SUCCESS")
                    val firebaseUser = auth.currentUser
                    userName = firebaseUser?.displayName
                    if (userName != null && userName != sharedPreferences.getString("user_name", null)) {
                        sharedPreferences.edit().putString("user_name", userName).apply()
                    }
                    val photoUrlFromFirebase = firebaseUser?.photoUrl?.toString()
                    val photoUrlToStore = photoUrlFromFirebase ?: sharedPreferences.getString("user_avatar_url", null)

                    if (photoUrlToStore != null) {
                        sharedPreferences.edit().putString("user_avatar_url", photoUrlToStore).apply()
                        Log.d(TAG, "User photo URL stored: $photoUrlToStore")
                    } else {
                        Log.d(TAG, "User photo URL from Firebase Auth and Credential Manager were both null.")
                    }
                    
                    // Handle first-time user flow
                    if (isFirstTimeUser) {
                        Log.d(TAG, "First-time user detected - showing registration flow")
                        showRegistrationFlow()
                    } else {
                        Log.d(TAG, "Returning user - checking profile")
                        checkUserProfile()
                    }
                } else {
                    val exception = task.exception
                    Log.w(TAG, "Firebase Auth with Google ID Token: FAILURE", exception)
                    
                    // Check if this is an expired token error
                    if (exception?.message?.contains("expired") == true || 
                        exception?.message?.contains("incorrect") == true ||
                        exception?.message?.contains("malformed") == true ||
                        exception?.message?.contains("not issued by Google") == true) {
                        
                        Log.w(TAG, "Token appears to be expired or invalid, clearing and starting fresh authentication")
                        
                        // Clear any cached token data
                        intent.removeExtra(EnhancedSplashActivity.EXTRA_GOOGLE_ID_TOKEN)
                        
                        // Start fresh authentication flow
                        Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show()
                        startGoogleSignInFlowWithRetry()
                        
                    } else {
                        // Handle other authentication errors
                        Toast.makeText(
                            this,
                            getString(
                                R.string.toast_auth_failed_firebase_reason,
                                exception?.message ?: ""
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    private fun showRegistrationFlow() {
        Log.d(TAG, "Showing registration flow for first-time user")
        
        // Set content to show registration UI
        setContent {
            AkahidegnTheme {
                RegistrationScreen(
                    onRegistrationComplete = { profileComplete ->
                        Log.d(TAG, "Registration completed, profile complete: $profileComplete")
                        if (profileComplete) {
                            // Initialize main screen after successful registration
                            initializeMainScreen()
                        } else {
                            // Show user registration dialog if profile is incomplete
                            showUserRegistrationDialog()
                            initializeMainScreen()
                        }
                    }
                )
            }
        }
    }

    private fun loadRewardedAd() {
        if (!BuildConfig.ADS_ENABLED || BuildConfig.ADMOB_REWARDED_ID.isEmpty()) {
            Log.d("ADS", "Ads disabled - REWARDED_ID empty or not enabled")
            return
        }
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, BuildConfig.ADMOB_REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Rewarded ad failed: ${adError.message}")
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("ADS", "Rewarded ad loaded")
                rewardedAd = ad
            }
        })
    }

    private fun loadInterstitialAd() {
        if (!BuildConfig.ADS_ENABLED || BuildConfig.ADMOB_INTERSTITIAL_ID.isEmpty()) {
            Log.d("ADS", "Ads disabled - INTERSTITIAL_ID empty or not enabled")
            return
        }
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, BuildConfig.ADMOB_INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Interstitial ad failed: ${adError.message}")
                interstitialAd = null
            }
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("ADS", "Interstitial ad loaded")
                interstitialAd = ad
            }
        })
    }

    private fun refreshGroupsFromActivity() {
        Log.d("MainActivity", "refreshGroupsFromActivity called, delegating to ViewModel.")
        mainViewModel.refreshGroups()
        // logAllGroups() // Debug log, can be removed if ViewModel handles logging
    }

    // logAllGroups might be redundant if ViewModel provides sufficient logging or data.
    private fun logAllGroups() { 
        // groupsRef is now initialized in MainViewModel
        // Consider removing this or accessing groups via mainViewModel.groups.value for logging
    }

    private fun showCreateGroupDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.destination_input_label_new)
            setPadding(60, 40, 60, 40)
            textSize = 16f
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_create_group_title_new))
            .setMessage(getString(R.string.dialog_create_group_message))
            .setView(editText)
            .setPositiveButton(getString(R.string.dialog_button_create_group)) { _, _ ->
                val destination = editText.text.toString().trim()
                if (destination.isNotEmpty()) {
                    showRewardedAdForGroupCreation(destination)
                } else {
                    Toast.makeText(this, getString(R.string.toast_please_enter_destination), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    private fun showRewardedAdForGroupCreation(destination: String) {
        if (rewardedAd != null) {
            rewardedAd!!.show(this) { rewardItem ->
                Log.d("ADS", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                createGroupAfterAd(destination)
                loadRewardedAd()
            }
        } else {
            Log.d("ADS", "Rewarded ad not ready, creating group directly")
            createGroupAfterAd(destination)
        }
    }

    private fun createGroupAfterAd(destination: String) {
        val lat = _userLocationFlow.value?.latitude ?: 8.9806 // Default if null
        val lng = _userLocationFlow.value?.longitude ?: 38.7578 // Default if null
        createGroupInFirebase(toDestination = destination, pickupLatitude = lat, pickupLongitude = lng)
    }

    // Removed handleGroupSelection and joinGroupAfterAd (assuming they are part of a different flow or deprecated)
    // If needed, they should be refactored to use ViewModel state or specific group IDs.

    private fun createGroupInFirebase(
        toDestination: String, 
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
    Log.d("SMOKE_TEST", "createGroupInFirebase called dest=$toDestination lat=$pickupLatitude lng=$pickupLongitude user=${auth.currentUser?.uid}")
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, getString(R.string.toast_user_must_sign_in_create), Toast.LENGTH_SHORT).show()
            startGoogleSignInFlow()
            return
        }

        val timestamp = System.currentTimeMillis()
        val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeString = timeFormatter.format(java.util.Date(timestamp))
        val creatorDisplayName = auth.currentUser?.displayName ?: userName ?: run {
            // If no display name is available, user must complete profile setup
            Toast.makeText(this, getString(R.string.toast_please_complete_profile), Toast.LENGTH_SHORT).show()
            checkUserProfile()
            return
        }
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatarUrlFromPrefs = sharedPreferences.getString("user_avatar_url", null)
        val finalUserAvatar = userAvatarUrlFromPrefs ?: sharedPreferences.getString("user_avatar", "avatar_1")!!
        val fromLocationString = "Lat: ${"%.4f".format(pickupLatitude)}, Lng: ${"%.4f".format(pickupLongitude)}"
        val uniqueFirebaseDisplayDestinationName = "$toDestination ($timeString) - by $creatorDisplayName"

        val creatorMemberInfo = MemberInfo(
            name = creatorDisplayName,
            phone = userPhone,
            avatar = finalUserAvatar,
            joinedAt = timestamp
        )
        val initialMembers = hashMapOf(currentUserId to true)
        val initialMemberDetails = hashMapOf(currentUserId to creatorMemberInfo)

        val newGroupRef = groupsRef.push()
        val newGroupId = newGroupRef.key

        if (newGroupId == null) {
            Log.e("FIREBASE", "Failed to get new group key from Firebase.")
            Toast.makeText(this, getString(R.string.toast_failed_get_group_id), Toast.LENGTH_SHORT).show()
            return
        }

        val newGroup = Group(
            groupId = newGroupId,
            creatorId = currentUserId,
            creatorName = creatorDisplayName,
            destinationName = uniqueFirebaseDisplayDestinationName,
            originalDestination = toDestination,
            from = fromLocationString,
            to = toDestination,
            status = "active",
            pickupLat = pickupLatitude,
            pickupLng = pickupLongitude,
            timestamp = timestamp,
            maxMembers = 4,
            members = initialMembers,
            memberDetails = initialMemberDetails,
            memberCount = 1,
            imageUrl = null
        )

        // Use optimistic UI for instant group creation feedback
        mainViewModel.createGroup(newGroup, currentUserId)
        
        // Success feedback (immediate)
        runCatching { notificationManagerService.playSuccessSound() }
        runCatching { notificationManagerService.playSuccessVibration() }
        runCatching {
            val msg = getString(R.string.toast_group_created, newGroup.destinationName ?: "")
            animationViewModel.showQuickSuccess(msg)
        }

        lifecycleScope.launch {
            runCatching {
                activityHistoryRepository.add(
                    TripHistoryItem(
                        tripId = newGroupId,
                        groupId = newGroupId,
                        destinationName = newGroup.destinationName ?: toDestination,
                        memberCount = 1,
                        role = "CREATOR",
                        status = "ACTIVE"
                    )
                )
            }.onFailure { e -> Log.e("HISTORY", "Failed to record created group", e) }
        }
    }
    
    private fun joinGroupInFirebase(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            onComplete(false, getString(R.string.toast_user_not_authenticated))
            startGoogleSignInFlow()
            return
        }
        val groupId = group.groupId
        if (groupId == null) {
            onComplete(false, getString(R.string.toast_error_group_id_missing))
            return
        }

        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        if ((group.timestamp ?: 0L) < thirtyMinutesAgo) {
            onComplete(false, getString(R.string.toast_group_expired))
            return
        }
        if (group.memberCount >= group.maxMembers) {
            onComplete(false, getString(R.string.toast_group_is_full))
            return
        }
        if (group.members.containsKey(currentUserId) == true && group.members[currentUserId] == true) {
            onComplete(true, "You are already a member of this group.")
            showGroupMembersDialog(group)
            return
        }

        val groupRef = groupsRef.child(groupId)
        val joinerDisplayName = auth.currentUser?.displayName ?: userName ?: run {
            // If no display name is available, user must complete profile setup
            onComplete(false, getString(R.string.toast_please_complete_profile))
            checkUserProfile()
            return
        }
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatarUrlFromPrefs = sharedPreferences.getString("user_avatar_url", null)
        val finalUserAvatar = userAvatarUrlFromPrefs ?: sharedPreferences.getString("user_avatar", "avatar_1")!!

        val updates = HashMap<String, Any>()
        updates["members/$currentUserId"] = true
        updates["memberDetails/$currentUserId/name"] = joinerDisplayName
        updates["memberDetails/$currentUserId/phone"] = userPhone
        updates["memberDetails/$currentUserId/avatar"] = finalUserAvatar
        updates["memberDetails/$currentUserId/joinedAt"] = System.currentTimeMillis()

        groupRef.updateChildren(updates)
            .addOnSuccessListener {
                groupRef.child("memberCount").runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        var currentCount = mutableData.getValue(Int::class.java) ?: 0
                        if (currentCount < group.maxMembers) {
                            mutableData.value = currentCount + 1
                            return com.google.firebase.database.Transaction.success(mutableData)
                        }
                        return com.google.firebase.database.Transaction.abort()
                    }

                    override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, currentData: com.google.firebase.database.DataSnapshot?) {
                        if (error != null) {
                            Log.e("FIREBASE_TX", "Transaction failed for memberCount: ${error.message}")
                            onComplete(false, getString(R.string.toast_failed_update_member_count, error.message ?: ""))
                        } else if (!committed) {
                            Log.w("FIREBASE_TX", "Transaction for memberCount not committed.")
                            onComplete(false, getString(R.string.toast_group_maybe_full_or_issue))
                        } else {
                            Log.d("FIREBASE_TX", "Transaction for memberCount successful.")
                            onComplete(true, null)
                            refreshGroupsFromActivity() // Refresh through ViewModel
                            // Play a small join success sequence
                            runCatching {
                                val name = group.destinationName ?: group.to.orEmpty()
                                animationViewModel.runGroupJoinSequence(name)
                            }
                            lifecycleScope.launch {
                                runCatching {
                                    activityHistoryRepository.add(
                                        TripHistoryItem(
                                            tripId = groupId,
                                            groupId = groupId,
                                            destinationName = group.destinationName ?: group.to.orEmpty(),
                                            memberCount = (group.memberCount + 1),
                                            role = "MEMBER",
                                            status = "ACTIVE"
                                        )
                                    )
                                }.onFailure { e -> Log.e("HISTORY", "Failed to record joined group", e) }
                            }
                        }
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Failed to join group.", e)
                onComplete(false, getString(R.string.toast_permission_denied_or_network_error, e.message ?: ""))
            }
    }

    private fun checkUserProfile() {
        val user = auth.currentUser
        val storedUserName = sharedPreferences.getString("user_name", null)
        val userPhone = sharedPreferences.getString("user_phone", null)
        
        // If we came from splash screen with a token, give Firebase auth time to sync
        val intentHadToken = intent.getStringExtra(EnhancedSplashActivity.EXTRA_GOOGLE_ID_TOKEN) != null

        if (user == null && intentHadToken) {
            Log.d(TAG, "checkUserProfile: Token from splash detected, waiting for Firebase auth to sync")
            lifecycleScope.launch {
                // Wait for Firebase auth to sync
                var attempts = 0
                while (auth.currentUser == null && attempts < 10) {
                    delay(100)
                    attempts++
                }
                
                if (auth.currentUser != null) {
                    Log.d(TAG, "checkUserProfile: Firebase auth synced successfully")
                    // Continue with profile check
                    checkUserProfile()
                } else {
                    Log.w(TAG, "checkUserProfile: Firebase auth sync timeout, starting sign-in flow")
                    startGoogleSignInFlowWithRetry()
                }
            }
            return
        }

        if (user == null && !intentHadToken) {
            Log.w(TAG, "checkUserProfile: auth.currentUser is null and no token from splash. Starting sign-in.")
            startGoogleSignInFlowWithRetry()
            return
        }

        // Get fresh user reference after auth sync checks
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "checkUserProfile: auth.currentUser is still null after checks. Starting sign-in.")
            startGoogleSignInFlowWithRetry()
            return
        }

        // Use enhanced sync service to validate profile consistency
    lifecycleScope.launch {
            try {
                // First, attempt to sync profile data from Firebase
                profileSyncService?.syncProfileData()
                
                // Validate consistency between local and remote data
                val consistencyResult = profileSyncService?.validateProfileConsistency()
                when (consistencyResult) {
                    is com.dawitf.akahidegn.core.result.Result.Success -> {
                        if (!consistencyResult.data) {
                            Log.w(TAG, "Profile data inconsistency detected, forcing sync")
                            profileSyncService?.forceSyncProfile()
                        }
                    }
                    is com.dawitf.akahidegn.core.result.Result.Error -> {
                        Log.w(TAG, "Profile validation failed: ${consistencyResult.error}")
                    }
                    is com.dawitf.akahidegn.core.result.Result.Loading -> {
                        Log.d(TAG, "Profile validation still loading")
                    }
                    null -> Log.w(TAG, "ProfileSyncService not available")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Profile sync failed: ${e.message}")
            }
            
            val currentDisplayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: storedUserName
            this@MainActivity.userName = currentDisplayName

            if (!userPhone.isNullOrBlank() && !currentDisplayName.isNullOrBlank()) {
                Log.d(TAG, "User profile complete. Welcome: $currentDisplayName. Phone: $userPhone. Photo: ${currentUser?.photoUrl ?: sharedPreferences.getString("user_avatar_url", "N/A")}")
                initializeMainScreen()
                return@launch
            }

            // If local profile incomplete, check remote users/{uid} doc to decide whether to skip overlay
            try {
                val uid = currentUser?.uid
                if (uid != null) {
                    val snapshot = database.reference
                        .child("users").child(uid).get().await()
                    if (snapshot.exists()) {
                        val remoteName = snapshot.child("name").getValue(String::class.java)
                        val remotePhone = snapshot.child("phone").getValue(String::class.java)
                        if (!remoteName.isNullOrBlank() && !remotePhone.isNullOrBlank()) {
                            sharedPreferences.edit()
                                .putString("user_name", remoteName)
                                .putString("user_phone", remotePhone)
                                .apply()
                            Log.d(TAG, "Remote user profile found; skipping registration overlay")
                            initializeMainScreen()
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote user check failed: ${e.message}")
            }

            Log.d(TAG, "User profile incomplete. Name: $currentDisplayName, Phone: $userPhone. Showing registration dialog.")
            val photoUrlForDialog = currentUser?.photoUrl?.toString() ?: sharedPreferences.getString("user_avatar_url", null)
            showUserRegistrationDialog(
                googleName = currentDisplayName,
                googlePhotoUrl = photoUrlForDialog
            )
        }
    }

    private fun showUserRegistrationDialog(googleName: String? = null, googlePhotoUrl: String? = null) {
        setContent {
            AkahidegnTheme {
                UserRegistrationDialog(
                    initialName = googleName ?: "",
                    initialPhotoUrl = googlePhotoUrl ?: sharedPreferences.getString("user_avatar_url", null),
                    onComplete = { name, phone ->
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            saveUserProfile(name, phone, googlePhotoUrl ?: sharedPreferences.getString("user_avatar_url", null))
                        } else {
                            Toast.makeText(this, getString(R.string.toast_name_and_phone_required), Toast.LENGTH_LONG).show()
                            showUserRegistrationDialog(name.ifBlank { googleName }, googlePhotoUrl)
                        }
                    },
                    onDismiss = { 
                        Toast.makeText(this, getString(R.string.toast_registration_required), Toast.LENGTH_LONG).show()
                        if (auth.currentUser != null) {
                            initializeMainScreen()
                        } else {
                            startGoogleSignInFlow()
                        }
                    }
                )
            }
        }
    }

    private fun saveUserProfile(name: String, phone: String, avatarUrl: String?) {
        this.userName = name
        val editor = sharedPreferences.edit()
        editor.putString("user_name", name)
        editor.putString("user_phone", phone)

        val finalAvatarUrlToSave = avatarUrl ?: sharedPreferences.getString("user_avatar_url", null)
        if (finalAvatarUrlToSave != null) {
            editor.putString("user_avatar_url", finalAvatarUrlToSave)
            editor.remove("user_avatar")
            Log.d(TAG, "Saving profile with avatar URL: $finalAvatarUrlToSave")
        } else {
            editor.putString("user_avatar", sharedPreferences.getString("user_avatar", "avatar_1"))
            Log.d(TAG, "Saving profile, no avatar URL, using placeholder.")
        }
        editor.putLong("user_registration_time", System.currentTimeMillis())
        editor.apply()

        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            // Use ProfileSyncService for comprehensive data management
            lifecycleScope.launch {
                try {
                    // Update through ProfileSyncService to maintain consistency
                    val syncResult = profileSyncService?.updateProfileData(
                        name = name,
                        phone = phone,
                        avatarUrl = finalAvatarUrlToSave
                    )
                    
                    when (syncResult) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            Log.d(TAG, "Profile sync successful through ProfileSyncService")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, getString(R.string.toast_welcome_profile_saved, name), Toast.LENGTH_SHORT).show()
                                initializeMainScreen()
                            }
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            Log.w(TAG, "Profile sync failed, falling back to direct Firebase save: ${syncResult.error}")
                            fallbackToDirectFirebaseSave(name, phone, finalAvatarUrlToSave, currentUserId)
                        }
                        is com.dawitf.akahidegn.core.result.Result.Loading -> {
                            Log.d(TAG, "Profile sync still loading, waiting...")
                            // Could implement a loading state here
                            fallbackToDirectFirebaseSave(name, phone, finalAvatarUrlToSave, currentUserId)
                        }
                        null -> {
                            Log.w(TAG, "ProfileSyncService not available, using direct Firebase save")
                            fallbackToDirectFirebaseSave(name, phone, finalAvatarUrlToSave, currentUserId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during profile sync", e)
                    fallbackToDirectFirebaseSave(name, phone, finalAvatarUrlToSave, currentUserId)
                }
            }
        } else {
            Log.w(TAG, "User ID null, cannot save profile to Firebase.")
            initializeMainScreen()
        }
    }
    
    private fun fallbackToDirectFirebaseSave(name: String, phone: String, finalAvatarUrlToSave: String?, currentUserId: String) {
        val userRef = database.reference.child("users").child(currentUserId)
        val userMap = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "email" to (auth.currentUser?.email ?: ""),
            "registrationTime" to System.currentTimeMillis(),
            "lastActive" to System.currentTimeMillis()
        )
        
        if (finalAvatarUrlToSave != null) {
            userMap["avatarUrl"] = finalAvatarUrlToSave
        } else {
            userMap["avatar"] = sharedPreferences.getString("user_avatar", "avatar_1")!!
        }

        userRef.setValue(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User profile saved to Firebase: $name. Avatar URL: $finalAvatarUrlToSave")
                Toast.makeText(this, getString(R.string.toast_welcome_profile_saved, name), Toast.LENGTH_SHORT).show()
                initializeMainScreen()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save profile to Firebase.", e)
                Toast.makeText(this, getString(R.string.toast_welcome_profile_saved_locally, name), Toast.LENGTH_SHORT).show()
                initializeMainScreen()
            }
    }

    private fun initializeMainScreen() {
        val currentUserId = auth.currentUser?.uid
        
        // If we came from splash screen with a token, trust that authentication was successful
        val intentHadToken = intent.getStringExtra("googleIdToken") != null
        
        if (currentUserId == null && !isFinishing && !intentHadToken) {
            Log.w(TAG, "initializeMainScreen: user not signed in and no token from splash. Starting sign-in.")
            startGoogleSignInFlow()
            return
        }
        
        // If we have a token from splash screen, wait a moment for Firebase auth to catch up
        if (intentHadToken && currentUserId == null && !isFinishing) {
            Log.d(TAG, "Token from splash detected, waiting for Firebase auth to sync")
            lifecycleScope.launch {
                // Wait for Firebase auth to sync
                var attempts = 0
                while (auth.currentUser?.uid == null && attempts < 10) {
                    delay(100)
                    attempts++
                }
                
                if (auth.currentUser?.uid != null) {
                    Log.d(TAG, "Firebase auth synced successfully")
                    // Continue with initialization
                    initializeMainScreen()
                } else {
                    Log.w(TAG, "Firebase auth sync timeout, starting sign-in flow")
                    startGoogleSignInFlow()
                }
            }
            return
        }
        
        // ViewModel now uses StateFlow and repository pattern - no manual Firebase initialization needed
        if (currentUserId == null) {
            Log.e(TAG, "Critical error: User ID is null at initializeMainScreen after checks.")
            startGoogleSignInFlow() // Fallback to sign-in
            return
        }

        // Set up the main UI content
        setupMainScreenContent()
    }
    
    /**
     * Sets up the main screen content with proper navigation
     */
    private fun setupMainScreenContent() {
        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val colorScheme = when (currentRoute) {
                Screen.Main.route -> HomeColorScheme
                Screen.Groups.route -> ActiveGroupsColorScheme
                Screen.Settings.route -> SettingsColorScheme
                else -> null
            }
            AkahidegnTheme(selectedColorScheme = colorScheme) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(Screen.Main, Screen.Groups, Screen.Settings)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { screen.icon?.let { Icon(imageVector = it, contentDescription = screen.title) } },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route || currentRoute?.startsWith(screen.route) == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(navController, startDestination = Screen.Main.route, Modifier.padding(innerPadding)) {
                        composable(Screen.Main.route) {
                            MainScreenContent(
                                navController = navController,
                                onGroupClick = { group -> showGroupMembersDialog(group) },
                                onRefreshGroups = { refreshGroupsFromActivity() },
                                onCreateGroup = { showCreateGroupDialog() }
                            )
                        }
                        composable(Screen.Groups.route) {
                            ActiveGroupsScreen(
                                onOpenHistory = {
                                    navController.navigate(Screen.ActivityHistory.route)
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onSignOut = {
                                    signOut()
                                    finish()
                                },
                                onOpenProfile = {
                                    val userId = auth.currentUser?.uid
                                    if (userId != null) {
                                        navController.navigate(Screen.Profile.createRoute(userId))
                                    } else {
                                        Toast.makeText(this@MainActivity, getString(R.string.toast_user_must_sign_in_create), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        // Register ActivityHistoryScreen
                        composable(Screen.ActivityHistory.route) {
                            com.dawitf.akahidegn.ui.activity.ActivityHistoryScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        // Register ProfileScreen
                        composable(
                            route = Screen.Profile.route,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                            com.dawitf.akahidegn.ui.screens.profile.ProfileScreen(
                                userId = userId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainScreenContent(
        navController: NavController,
        onGroupClick: (Group) -> Unit,
        onRefreshGroups: () -> Unit,
        onCreateGroup: () -> Unit
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val mainGroups by mainViewModel.mainGroups.collectAsState()
            val vmIsLoading by mainViewModel.isLoadingGroups.collectAsState()
            val currentLocation by userLocationFlow.collectAsState()

            var searchQuery by remember { mutableStateOf("") }
            var selectedFilters by remember { mutableStateOf(SearchFilters()) }

            LaunchedEffect(searchQuery) {
                mainViewModel.updateSearchQuery(searchQuery)
            }

            MainScreen(
                mainGroups = mainGroups,
                activeGroups = emptyList(),
                historyGroups = emptyList(),
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery = query },
                selectedFilters = selectedFilters,
                onFiltersChange = { filters -> selectedFilters = filters },
                onGroupClick = onGroupClick,
                onJoinGroup = { group -> 
                    val currentUserId = auth.currentUser?.uid
                    val userName = auth.currentUser?.displayName ?: run {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_please_complete_profile), Toast.LENGTH_SHORT).show()
                        checkUserProfile()
                        return@MainScreen
                    }
                    if (currentUserId != null) {
                        mainViewModel.joinGroup(group, currentUserId, userName)
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_user_must_sign_in_create), Toast.LENGTH_SHORT).show()
                        startGoogleSignInFlow()
                    }
                },
                isLoading = vmIsLoading,
                onRefreshGroups = onRefreshGroups,
                onCreateGroup = onCreateGroup,
                userLocation = currentLocation
            )
        }
    }

    private fun showGroupMembersDialog(group: Group) {
        selectedGroupForDialog = group 
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                auth.signOut()
                Log.d(TAG, "Firebase sign-out successful.")
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                Log.d(TAG, "CredentialManager state cleared.")
                Toast.makeText(this@MainActivity, "Signed out.", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().remove("user_name").remove("user_phone").remove("user_avatar_url").apply()
                userName = null
                startGoogleSignInFlow()
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error signing out.", Toast.LENGTH_SHORT).show()
                startGoogleSignInFlow()
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun setupLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener)
                Log.d("LOCATION", "Location updates requested.")
            } catch (e: SecurityException) { 
                Log.e("LOCATION", "SecurityException requesting location updates.", e)
                Toast.makeText(this, getString(R.string.toast_location_permission_error), Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d("LOCATION", "ACCESS_FINE_LOCATION permission not granted. Requesting...")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    @Suppress("DEPRECATION") 
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LOCATION", "ACCESS_FINE_LOCATION permission granted.")
                setupLocationUpdates()
            } else {
                Log.w("LOCATION", "ACCESS_FINE_LOCATION permission denied.")
                Toast.makeText(this, getString(R.string.toast_location_permission_required_nearby), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationManager.isInitialized) { 
            try {
                locationManager.removeUpdates(locationListener)
                Log.d("LOCATION", "Location updates removed.")
            } catch (e: Exception) { 
                Log.e("LOCATION", "Error removing location updates.", e)
            }
        }
        rewardedAd = null 
        interstitialAd = null
    }

    private fun handleAuthError(t: Throwable) {
        errorHandler.log(t, "auth_flow")
        Toast.makeText(this, errorHandler.toUserMessage(t), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RegistrationScreen(
    onRegistrationComplete: (Boolean) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    // Full-screen registration with golden theme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFC30B)) // Golden background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Welcome header
            Text(
                text = "Welcome to Akahidegn!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your rideshare community starts here",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            when (currentStep) {
                0 -> {
                    // Step 1: Introduction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚗 Find & Join Groups",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connect with people traveling to similar destinations",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Get Started", color = Color(0xFFFFC30B), fontWeight = FontWeight.Bold)
                    }
                }
                
                1 -> {
                    // Step 2: Profile setup
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✨ Setup Complete!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Google account is ready. You can customize your profile later in settings.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { 
                            // Complete registration
                            onRegistrationComplete(true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Start Exploring", color = Color(0xFFFFC30B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
