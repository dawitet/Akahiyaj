package com.dawitf.akahidegn

import android.Manifest
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
import androidx.core.content.edit
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.dawitf.akahidegn.ui.navigation.Screen
import androidx.compose.material3.Scaffold
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.dawitf.akahidegn.ui.theme.HomeColorScheme
import com.dawitf.akahidegn.ui.theme.ActiveGroupsColorScheme
import com.dawitf.akahidegn.ui.theme.SettingsColorScheme
import com.dawitf.akahidegn.ui.screens.MainScreen
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.dawitf.akahidegn.ui.components.UserRegistrationDialog
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.core.error.ErrorHandler
import com.dawitf.akahidegn.activityhistory.ActivityHistoryRepository
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel
import com.dawitf.akahidegn.features.profile.ProfileSyncService
import com.dawitf.akahidegn.features.auth.EnhancedAuthService
import com.dawitf.akahidegn.features.location.DeviceConsistencyService
import com.dawitf.akahidegn.service.GroupCleanupService
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dawitf.akahidegn.core.notification.NotificationManagerService
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import android.app.AlertDialog
import android.widget.EditText
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.domain.model.MemberInfo
import com.dawitf.akahidegn.ui.screens.ActiveGroupsScreen
import com.dawitf.akahidegn.ui.screens.SettingsScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

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
    @Inject lateinit var groupCleanupService: GroupCleanupService

    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var firestore: FirebaseFirestore

    private var selectedGroupForDialog by mutableStateOf<Group?>(null)

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    private val _userLocationFlow = MutableStateFlow<Location?>(null)
    val userLocationFlow: StateFlow<Location?> = _userLocationFlow.asStateFlow()

    private lateinit var locationManager: LocationManager
    private var lastLocationUpdateTimestamp = 0L

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
                            _userLocationFlow.value = location
                            
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastLocationUpdateTimestamp > 10000L) {
                                lastLocationUpdateTimestamp = currentTime
                                Log.d("LOCATION", "Raw location updated (fallback): ${location.latitude}, ${location.longitude}")
                            }
                        }
                        is com.dawitf.akahidegn.core.result.Result.Loading -> {
                            Log.d("LOCATION", "DeviceConsistencyService still processing location")
                            _userLocationFlow.value = location
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOCATION", "Error processing location through DeviceConsistencyService", e)
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

    private lateinit var sharedPreferences: SharedPreferences
    private var userName: String? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivityAuth"
        
        const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
        const val CREDENTIAL_TIMEOUT_MS = 10000L
        const val FIRST_LAUNCH_TIMEOUT_MS = 30000L
        const val MAX_RETRY_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity onCreate - launched from SplashActivity")
        
        // Enable edge-to-edge for immersive UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Example of using errorHandler for a guarded call
        try {
            sharedPreferences = getSharedPreferences("akahidegn_prefs", MODE_PRIVATE)
        } catch (t: Throwable) {
            errorHandler.log(t, "prefs_init")
            Toast.makeText(this, errorHandler.toUserMessage(t), Toast.LENGTH_LONG).show()
        }
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        setupLocationUpdates() // Request location updates

        MobileAds.initialize(this) { Log.d("ADS", "Mobile Ads SDK initialized") }

        database = Firebase.database
        groupsRef = database.reference.child("groups") // ViewModel uses this via initializeFirebase
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)
        firestore = FirebaseFirestore.getInstance()

        loadRewardedAd()
        loadInterstitialAd()

        // Pass the location flow to the ViewModel once it's available
        mainViewModel.setUserLocationFlow(userLocationFlow)

        // Start the periodic group cleanup service (every 5 minutes)
        groupCleanupService.startPeriodicCleanup()
        Log.d(TAG, "Started automatic group cleanup service")

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
                sharedPreferences.edit { putString("user_avatar_url", photoUrlFromFirebase) }
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
                    sharedPreferences.edit { putLong("last_token_verification", currentTime) }
                    
                    userName = currentUser.displayName ?: sharedPreferences.getString("user_name", null)
                    val photoUrlFromFirebase = currentUser.photoUrl?.toString()
                    if (photoUrlFromFirebase != null) {
                        sharedPreferences.edit {
	                        putString(
		                        "user_avatar_url",
		                        photoUrlFromFirebase
	                        )
                        }
                    }
                    checkUserProfile() // This will call initializeMainScreen if profile is complete
                } else {
                    Log.w(TAG, "Token verification failed, user needs to re-authenticate", task.exception)
                    
                    // Clear the invalid auth state and timestamp
                    lifecycleScope.launch {
                        try {
                            auth.signOut()
                            sharedPreferences.edit { remove("last_token_verification") }
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
                delay(2000)
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
                    delay(5000)

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
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasLaunched = prefs.getBoolean("has_launched_before", false)
        
        if (!hasLaunched) {
            // Mark as launched for future checks
            prefs.edit { putBoolean("has_launched_before", true) }
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
                    sharedPreferences.edit { putString("user_avatar_url", photoUri.toString()) }
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
                        sharedPreferences.edit { putString("user_name", userName) }
                    }
                    val photoUrlFromFirebase = firebaseUser?.photoUrl?.toString()
                    val photoUrlToStore = photoUrlFromFirebase ?: sharedPreferences.getString("user_avatar_url", null)

                    if (photoUrlToStore != null) {
                        sharedPreferences.edit { putString("user_avatar_url", photoUrlToStore) }
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
            expiresAt = timestamp + (30 * 60 * 1000),
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
                sharedPreferences.edit {
                    remove("user_name")
                    remove("user_phone")
                    remove("user_avatar_url")
                }
                userName = null
                startGoogleSignInFlow()
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error signing out.", Toast.LENGTH_SHORT).show()
                startGoogleSignInFlow()
            }
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

        // Stop the periodic group cleanup service
        groupCleanupService.stopPeriodicCleanup()
        Log.d(TAG, "Stopped automatic group cleanup service")

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
                    checkUserProfile()
                } else {
                    Log.w(TAG, "checkUserProfile: Firebase auth sync timeout, starting sign-in flow")
                    startGoogleSignInFlowWithRetry()
                }
            }
            return
        }

        if (user == null) {
            Log.w(TAG, "checkUserProfile: auth.currentUser is null. Starting sign-in.")
            startGoogleSignInFlowWithRetry()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "checkUserProfile: auth.currentUser is still null after checks. Starting sign-in.")
            startGoogleSignInFlowWithRetry()
            return
        }

        lifecycleScope.launch {
            try {
                profileSyncService.syncProfileData()
                val consistencyResult = profileSyncService.validateProfileConsistency()
                when (consistencyResult) {
                    is com.dawitf.akahidegn.core.result.Result.Success -> {
                        if (!consistencyResult.data) {
                            profileSyncService.forceSyncProfile()
                        }
                    }
                    is com.dawitf.akahidegn.core.result.Result.Error -> {
                        Log.w(TAG, "Profile validation failed: ${consistencyResult.error}")
                    }
                    is com.dawitf.akahidegn.core.result.Result.Loading -> {
                        Log.d(TAG, "Profile validation still loading")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Profile sync failed: ${e.message}")
            }

            val currentDisplayName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: storedUserName
            this@MainActivity.userName = currentDisplayName

            if (!userPhone.isNullOrBlank() && !currentDisplayName.isNullOrBlank()) {
                initializeMainScreen()
                return@launch
            }

            try {
                val uid = currentUser.uid
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    val remoteName = snapshot.getString("name")
                    val remotePhone = snapshot.getString("phone")
                    if (!remoteName.isNullOrBlank() && !remotePhone.isNullOrBlank()) {
                        sharedPreferences.edit {
                            putString("user_name", remoteName)
                            putString("user_phone", remotePhone)
                        }
                        initializeMainScreen()
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote user check failed: ${e.message}")
            }

            val photoUrlForDialog = currentUser.photoUrl?.toString() ?: sharedPreferences.getString("user_avatar_url", null)
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
        sharedPreferences.edit {
            putString("user_name", name)
            putString("user_phone", phone)
            if (avatarUrl != null) {
                putString("user_avatar_url", avatarUrl)
            }
            putLong("user_registration_time", System.currentTimeMillis())
        }

        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            lifecycleScope.launch {
                try {
                    val syncResult = profileSyncService.updateProfileData(name, phone, avatarUrl)
                    when (syncResult) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, getString(R.string.toast_welcome_profile_saved, name), Toast.LENGTH_SHORT).show()
                                initializeMainScreen()
                            }
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            fallbackToDirectFirebaseSave(name, phone, avatarUrl, currentUserId)
                        }
                        is com.dawitf.akahidegn.core.result.Result.Loading -> {
                            Log.d(TAG, "Profile sync still loading, waiting...")
                            fallbackToDirectFirebaseSave(name, phone, avatarUrl, currentUserId)
                        }
                    }
                } catch (_: Exception) {
                    fallbackToDirectFirebaseSave(name, phone, avatarUrl, currentUserId)
                }
            }
        } else {
            initializeMainScreen()
        }
    }

    private fun fallbackToDirectFirebaseSave(name: String, phone: String, avatarUrl: String?, currentUserId: String) {
        val userMap = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "email" to (auth.currentUser?.email ?: ""),
            "registrationTime" to System.currentTimeMillis(),
            "lastActive" to System.currentTimeMillis()
        )
        if (avatarUrl != null) {
            userMap["avatarUrl"] = avatarUrl
        }

        firestore.collection("users").document(currentUserId)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.toast_welcome_profile_saved, name), Toast.LENGTH_SHORT).show()
                initializeMainScreen()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.toast_welcome_profile_saved_locally, name), Toast.LENGTH_SHORT).show()
                initializeMainScreen()
            }
    }

    private fun initializeMainScreen() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null && !isFinishing) {
            startGoogleSignInFlow()
            return
        }

        setupMainScreenContent()
    }

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
                        composable(Screen.ActivityHistory.route) {
                            com.dawitf.akahidegn.ui.activity.ActivityHistoryScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
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
}

@Composable
fun RegistrationScreen(onRegistrationComplete: (Boolean) -> Unit) {
    // Minimal placeholder registration screen
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome to Akahidegn!", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onRegistrationComplete(true) }) { Text("Continue") }
            }
        }
    }
}
