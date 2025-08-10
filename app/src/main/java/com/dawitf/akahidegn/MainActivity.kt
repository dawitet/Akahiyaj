package com.dawitf.akahidegn

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
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
import com.dawitf.akahidegn.ui.navigation.Screen
import androidx.compose.material3.Scaffold
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
import com.dawitf.akahidegn.ui.components.SignInPromptDialog
import com.dawitf.akahidegn.ui.components.CreateGroupDialog
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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
import com.dawitf.akahidegn.core.error.ErrorHandler
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


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val animationViewModel: AnimationViewModel by viewModels()
    @Inject lateinit var errorHandler: ErrorHandler
    @Inject lateinit var activityHistoryRepository: ActivityHistoryRepository
    @Inject lateinit var notificationManagerService: NotificationManagerService

    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private val currentGroups = mutableListOf<Group>() // Consider if this is still needed or use ViewModel's state
    private var selectedGroupForDialog by mutableStateOf<Group?>(null)
    private var showSignInDialog by mutableStateOf(false)
    private var showCreateGroupDialog by mutableStateOf(false)
    private var showDisbandConfirmDialog by mutableStateOf(false)
    private var showLeaveConfirmDialog by mutableStateOf(false)
    private var groupToDisband by mutableStateOf<Group?>(null)
    private var groupToLeave by mutableStateOf<Group?>(null)

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var nativeAd: NativeAd? = null

    private val _userLocationFlow = MutableStateFlow<Location?>(null)
    val userLocationFlow: StateFlow<Location?> = _userLocationFlow.asStateFlow()

    // Debug automation flags
    private var autoTestGroupCreated = false

    private lateinit var locationManager: LocationManager
    private var lastLocationUpdateTimestamp = 0L // Renamed for clarity

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _userLocationFlow.value = location // Emit to StateFlow, ViewModel will collect it
            val currentTime = System.currentTimeMillis()
            // Optional: Throttle direct logs if needed, but ViewModel handles its own update logic based on the flow
            if (currentTime - lastLocationUpdateTimestamp > 10000L) { // Log less frequently
                lastLocationUpdateTimestamp = currentTime
                Log.d("LOCATION", "Location updated via listener: ${location.latitude}, ${location.longitude}. Emitted to Flow.")
            }
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    // Removed isGroupCreator() as currentGroups might be deprecated in favor of ViewModel state

    private lateinit var sharedPreferences: SharedPreferences
    private var userName: String? = null

    companion object {
        val THEME_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
        private const val TAG = "MainActivityAuth"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Example of using errorHandler for a guarded call
        try {
            sharedPreferences = getSharedPreferences("akahhidegn_prefs", Context.MODE_PRIVATE)
        } catch (t: Throwable) {
            errorHandler.log(t, "prefs_init")
            Toast.makeText(this, errorHandler.toUserMessage(t), Toast.LENGTH_LONG).show()
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupLocationUpdates() // Request location updates
        requestNotificationPermission() // Request notification permission for push notifications

        MobileAds.initialize(this) { Log.d("ADS", "Mobile Ads SDK initialized") }

        database = Firebase.database
        groupsRef = database.reference.child("groups") // ViewModel uses this via initializeFirebase
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        loadRewardedAd()
        loadInterstitialAd()
        loadNativeAd()

        // Pass the location flow to the ViewModel once it's available
        mainViewModel.setUserLocationFlow(userLocationFlow)

        if (auth.currentUser == null) {
            Log.d(TAG, "No current user, starting Google Sign-In flow with Credential Manager")
            startGoogleSignInFlow()
            // DEBUG FALLBACK: If running a debug build on an emulator without any Google accounts,
            // automatically sign in anonymously after a short delay so we can proceed with smoke tests.
            if (BuildConfig.DEBUG) {
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(3500)
                    if (auth.currentUser == null) {
                        Log.w(TAG, "DEBUG FALLBACK: No user after Credential flow delay, attempting anonymous sign-in for smoke test")
                        auth.signInAnonymously()
                            .addOnSuccessListener {
                                Log.d(TAG, "DEBUG FALLBACK: Anonymous sign-in success -> checking profile")
                                // In debug we auto-complete a minimal profile so UI can load for smoke test
                                if (BuildConfig.DEBUG) {
                                    val existingName = sharedPreferences.getString("user_name", null)
                                    val existingPhone = sharedPreferences.getString("user_phone", null)
                                    if (existingName.isNullOrBlank() || existingPhone.isNullOrBlank()) {
                                        Log.d(TAG, "DEBUG FALLBACK: Injecting dummy profile for anonymous user")
                                        saveUserProfile(
                                            name = existingName ?: "Debug User",
                                            phone = existingPhone ?: "0000000000",
                                            avatarUrl = sharedPreferences.getString("user_avatar_url", null)
                                        )
                                    } else {
                                        checkUserProfile()
                                    }
                                } else {
                                    checkUserProfile()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "DEBUG FALLBACK: Anonymous sign-in failed", e)
                            }
                    }
                }
            }
        } else {
            Log.d(TAG, "User already signed in, checking profile")
            userName = auth.currentUser?.displayName ?: sharedPreferences.getString("user_name", null)
            val photoUrlFromFirebase = auth.currentUser?.photoUrl?.toString()
            if (photoUrlFromFirebase != null) {
                sharedPreferences.edit().putString("user_avatar_url", photoUrlFromFirebase).apply()
            }
            checkUserProfile() // This will call initializeMainScreen if profile is complete
        }
    }

    private fun startGoogleSignInFlow() {
        lifecycleScope.launch {
            val serverClientId = getString(R.string.default_web_client_id)
            if (serverClientId.isEmpty()) {
                Log.e(TAG, "R.string.default_web_client_id is empty. Cannot start Google Sign-In.")
                Toast.makeText(this@MainActivity, getString(R.string.toast_sign_in_config_error), Toast.LENGTH_LONG).show()
                return@launch
            }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                Log.d(TAG, "Attempting to get credential from CredentialManager...")
                val result = credentialManager.getCredential(this@MainActivity, request)
                Log.d(TAG, "GetCredential SUCCESS from CredentialManager")
                handleGoogleCredential(result)
            } catch (e: NoCredentialException) {
                Log.w(TAG, "NoGoogleCredentialException: No credentials available.", e)
                Toast.makeText(this@MainActivity, getString(R.string.toast_no_google_accounts), Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException from CredentialManager", e)
                Toast.makeText(this@MainActivity, getString(R.string.toast_sign_in_failed_or_cancelled), Toast.LENGTH_LONG).show()
            }
        }
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

    private fun firebaseAuthWithGoogleIdToken(idToken: String) {
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
                    checkUserProfile()
                } else {
                    Log.w(TAG, "Firebase Auth with Google ID Token: FAILURE", task.exception)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.toast_auth_failed_firebase_reason,
                            task.exception?.message ?: ""
                        ),
                        Toast.LENGTH_LONG
                    ).show()
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

    private fun loadNativeAd() {
        if (!BuildConfig.ADS_ENABLED || BuildConfig.ADMOB_NATIVE_ID.isEmpty()) {
            Log.d("ADS", "Ads disabled - NATIVE_ID empty or not enabled")
            return
        }
        val adRequest = AdRequest.Builder().build()
        val adLoader = com.google.android.gms.ads.AdLoader.Builder(this, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                Log.d("ADS", "Native ad loaded")
                nativeAd = ad
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("ADS", "Native ad failed: ${adError.message}")
                    nativeAd = null
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        
        adLoader.loadAd(adRequest)
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
        showCreateGroupDialog = true
    }

    private fun showRewardedAdForGroupCreation(destination: String, passengerCount: Int) {
        if (rewardedAd != null) {
            rewardedAd!!.show(this) { rewardItem ->
                Log.d("ADS", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                createGroupAfterAd(destination, passengerCount)
                loadRewardedAd()
            }
        } else {
            Log.d("ADS", "Rewarded ad not ready, creating group directly")
            createGroupAfterAd(destination, passengerCount)
        }
    }

    private fun showInterstitialAdForJoining(group: Group) {
        if (interstitialAd != null) {
            interstitialAd!!.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("ADS", "Interstitial ad dismissed, proceeding with join")
                    joinGroupAfterAd(group)
                    loadInterstitialAd() // Load a new ad for next time
                }
                
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d("ADS", "Interstitial ad failed to show: ${adError.message}")
                    joinGroupAfterAd(group)
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d("ADS", "Interstitial ad showed")
                    interstitialAd = null
                }
            }
            
            interstitialAd!!.show(this)
        } else {
            Log.d("ADS", "Interstitial ad not ready, joining group directly")
            joinGroupAfterAd(group)
        }
    }

    private fun joinGroupAfterAd(group: Group) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, getString(R.string.toast_user_must_sign_in_join), Toast.LENGTH_SHORT).show()
            startGoogleSignInFlow()
            return
        }

        if (group.groupId == null) {
            Log.e("JOIN_GROUP", "Group ID is null, cannot join")
            Toast.makeText(this, "Group ID error. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if group is full
        if (group.memberCount >= group.maxMembers) {
            Toast.makeText(this, "ቡድኑ ሙሉ ነው", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user is already a member
        if (group.members.containsKey(currentUserId) && group.members[currentUserId] == true) {
            Toast.makeText(this, "አስቀድመው ቡድኑ አባል ነዎት", Toast.LENGTH_SHORT).show()
            return
        }

        val groupRef = groupsRef.child(group.groupId!!)
        val userAvatar = if (auth.currentUser?.isAnonymous == true) {
            "avatar_6" // Use default avatar for anonymous users
        } else {
            sharedPreferences.getString("user_avatar", "avatar_1") ?: "avatar_1"
        }
        
        val memberInfo = hashMapOf(
            "name" to (auth.currentUser?.displayName ?: userName ?: "Unknown"),
            "phone" to (sharedPreferences.getString("user_phone", "") ?: ""),
            "avatar" to userAvatar,
            "joinedAt" to System.currentTimeMillis()
        )

        // Add user to members list
        groupRef.child("members").child(currentUserId).setValue(true)
            .addOnSuccessListener {
                // Add member details
                groupRef.child("memberDetails").child(currentUserId).setValue(memberInfo)
                    .addOnSuccessListener {
                        // Update member count
                        val newMemberCount = group.memberCount + 1
                        groupRef.child("memberCount").setValue(newMemberCount)
                            .addOnSuccessListener {
                                Log.d("JOIN_GROUP", "Successfully joined group ${group.groupId}")
                                Toast.makeText(this, "በተሳካ ሁኔታ ቡድኑን ተቀላቅለዋል!", Toast.LENGTH_SHORT).show()
                                
                                // Send notification to group creator about new member
                                val userName = auth.currentUser?.displayName ?: userName ?: "Someone"
                                notificationManagerService.showUserJoinedNotification(group, userName)
                                
                                // Check if group is now full and notify
                                if (newMemberCount >= group.maxMembers) {
                                    notificationManagerService.showGroupFullNotification(group)
                                }
                                
                                // Refresh groups to show updated data
                                mainViewModel.refreshGroups()
                                mainViewModel.loadUserGroups()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("JOIN_GROUP", "Failed to update member count: ${exception.message}")
                                Toast.makeText(this, "ቡድን መቀላቀል አልተቻለም", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("JOIN_GROUP", "Failed to add member details: ${exception.message}")
                        Toast.makeText(this, "ቡድን መቀላቀል አልተቻለም", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("JOIN_GROUP", "Failed to add to members: ${exception.message}")
                Toast.makeText(this, "ቡድን መቀላቀል አልተቻለም", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createGroupAfterAd(destination: String, passengerCount: Int) {
        val lat = _userLocationFlow.value?.latitude ?: 8.9806 // Default if null
        val lng = _userLocationFlow.value?.longitude ?: 38.7578 // Default if null
        createGroupInFirebase(toDestination = destination, pickupLatitude = lat, pickupLongitude = lng, passengerCount = passengerCount)
    }

    // Removed handleGroupSelection and joinGroupAfterAd (assuming they are part of a different flow or deprecated)
    // If needed, they should be refactored to use ViewModel state or specific group IDs.

    private fun createGroupInFirebase(
        toDestination: String, 
        pickupLatitude: Double,
        pickupLongitude: Double,
        passengerCount: Int
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
        val creatorDisplayName = auth.currentUser?.displayName ?: userName ?: "Anonymous User"
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatarUrlFromPrefs = sharedPreferences.getString("user_avatar_url", null)
        val finalUserAvatar = if (auth.currentUser?.isAnonymous == true) {
            "avatar_6" // Use default avatar for anonymous users
        } else {
            userAvatarUrlFromPrefs ?: sharedPreferences.getString("user_avatar", "avatar_1")!!
        }
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
            maxMembers = passengerCount,
            members = initialMembers,
            memberDetails = initialMemberDetails,
            memberCount = 1,
            imageUrl = null
        )

        newGroupRef.setValue(newGroup)
            .addOnSuccessListener {
                Log.d("FIREBASE", "Group created: ${newGroup.destinationName} with ID: ${newGroup.groupId}")
                
                // Refresh both main groups list and user groups list
                refreshGroupsFromActivity() // Refresh main groups through ViewModel
                mainViewModel.refreshUserGroups() // Refresh user groups as well
                
                Toast.makeText(this, getString(R.string.toast_group_created, newGroup.destinationName
                    ?: ""), Toast.LENGTH_SHORT).show()
                // Success feedback
                runCatching { notificationManagerService.playSuccessSound() }
                runCatching { notificationManagerService.playSuccessVibration() }
                // Also show a quick success UI animation (Amharic-first string)
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
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error creating group: ${newGroup.destinationName}, ID: ${newGroup.groupId}", e)
                Log.e("FIREBASE_DATA", "Data sent: $newGroup")
                Toast.makeText(this, getString(R.string.toast_failed_to_create_group, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
    }
    
    private fun tryJoinGroup(group: Group) {
        // Show interstitial ad first, then join the group
        if (interstitialAd != null) {
            interstitialAd?.show(this)
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("ADS", "Interstitial ad dismissed. Proceeding to join group.")
                    joinGroupAfterAd(group)
                    loadInterstitialAd() // Load next ad
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    Log.e("ADS", "Failed to show interstitial ad: ${e.message}")
                    joinGroupAfterAd(group) // Join anyway if ad fails
                    loadInterstitialAd() // Load next ad
                }
            }
        } else {
            Log.d("ADS", "Interstitial ad not ready. Joining group directly.")
            joinGroupAfterAd(group)
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
        val joinerDisplayName = auth.currentUser?.displayName ?: userName ?: "Anonymous User"
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatarUrlFromPrefs = sharedPreferences.getString("user_avatar_url", null)
        val finalUserAvatar = if (auth.currentUser?.isAnonymous == true) {
            "avatar_6" // Use default avatar for anonymous users
        } else {
            userAvatarUrlFromPrefs ?: sharedPreferences.getString("user_avatar", "avatar_1")!!
        }

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

        if (user == null) {
            Log.w(TAG, "checkUserProfile: auth.currentUser is null. Starting sign-in.")
            startGoogleSignInFlow()
            return
        }

        val currentDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: storedUserName
        this.userName = currentDisplayName

        if (currentDisplayName.isNullOrBlank() || userPhone.isNullOrBlank()) {
            Log.d(TAG, "User profile incomplete. Name: $currentDisplayName, Phone: $userPhone. Showing registration dialog.")
            val photoUrlForDialog = user.photoUrl?.toString() ?: sharedPreferences.getString("user_avatar_url", null)
            showUserRegistrationDialog(
                googleName = currentDisplayName,
                googlePhotoUrl = photoUrlForDialog
            )
        } else {
            Log.d(TAG, "User profile complete. Welcome: $currentDisplayName. Phone: $userPhone. Photo: ${user.photoUrl ?: sharedPreferences.getString("user_avatar_url", "N/A")}")
            initializeMainScreen()
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
        } else {
            Log.w(TAG, "User ID null, cannot save profile to Firebase.")
            initializeMainScreen()
        }
    }

    private fun initializeMainScreen() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null && !isFinishing) {
            Log.w(TAG, "initializeMainScreen: user not signed in. Starting sign-in.")
            startGoogleSignInFlow()
            return
        }
        // Ensure ViewModel is initialized with Firebase refs and user ID
        // Pass groupsRef from Activity to ViewModel; ViewModel will handle its own logic
        if (currentUserId != null) { // Double check currentUserId is not null
             mainViewModel.initializeFirebase(groupsRef, currentUserId)
             // Load user groups after Firebase is initialized
             mainViewModel.loadUserGroups()
        } else {
            Log.e(TAG, "Critical error: User ID is null at initializeMainScreen after checks. Cannot init ViewModel.")
            startGoogleSignInFlow() // Fallback to sign-in
            return
        }

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val colorScheme = when (currentRoute) {
                Screen.Main.route -> HomeColorScheme
                Screen.ActiveGroups.route -> ActiveGroupsColorScheme
                Screen.Settings.route -> SettingsColorScheme
                else -> null
            }
            AkahidegnTheme(selectedColorScheme = colorScheme) {
                val celebrationKey by animationViewModel.celebrationEvent.collectAsState()
                SharedElementsRoot {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(Screen.Main, Screen.ActiveGroups, Screen.Settings)
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
                                onGroupClick = { group -> 
                                    if (auth.currentUser?.isAnonymous == true) {
                                        // Show sign-in prompt for anonymous users
                                        showSignInPrompt()
                                    } else {
                                        // Check if group is full or user is already a member
                                        if (group.memberCount >= group.maxMembers) {
                                            showGroupMembersDialog(group)
                                        } else {
                                            val currentUserId = auth.currentUser?.uid
                                            if (currentUserId != null && 
                                                group.members.containsKey(currentUserId) && 
                                                group.members[currentUserId] == true) {
                                                // User is already a member, show members dialog
                                                showGroupMembersDialog(group)
                                            } else {
                                                // Show interstitial ad before joining
                                                showInterstitialAdForJoining(group)
                                            }
                                        }
                                    }
                                },
                                onRefreshGroups = { refreshGroupsFromActivity() },
                                onCreateGroup = { 
                                    if (auth.currentUser?.isAnonymous == true) {
                                        // Show sign-in prompt for anonymous users
                                        showSignInPrompt()
                                    } else {
                                        showCreateGroupDialog = true
                                    }
                                }
                            )
                        }
                        composable(Screen.ActiveGroups.route) { 
                            val userGroups by mainViewModel.userGroups.collectAsState()
                            val isLoadingUserGroups by mainViewModel.isLoadingUserGroups.collectAsState()
                            val userLocation by mainViewModel.currentLocation.collectAsState()
                            
                            ActiveGroupsScreen(
                                userGroups = userGroups,
                                isLoading = isLoadingUserGroups,
                                userLocation = userLocation,
                                onRefresh = { mainViewModel.refreshUserGroups() },
                                onGroupClick = { group ->
                                    // Navigate to group details or chat
                                    if (group.memberCount < group.maxMembers) {
                                        showGroupDialog(group)
                                    } else {
                                        showGroupMembersDialog(group)
                                    }
                                },
                                onDisbandGroup = { group ->
                                    groupToDisband = group
                                    showDisbandConfirmDialog = true
                                },
                                onLeaveGroup = { group ->
                                    groupToLeave = group
                                    showLeaveConfirmDialog = true
                                },
                                isGroupCreator = { group -> mainViewModel.isGroupCreator(group) }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(onSignOut = {
                                lifecycleScope.launch {
                                    try {
                                        auth.signOut()
                                        Log.d(TAG, "Firebase sign-out successful.")
                                        val clearRequest = ClearCredentialStateRequest()
                                        credentialManager.clearCredentialState(clearRequest)
                                        Log.d(TAG, "CredentialManager state cleared.")
                                        Toast.makeText(this@MainActivity, getString(R.string.toast_signed_out), Toast.LENGTH_SHORT).show()
                                        sharedPreferences.edit().remove("user_name").remove("user_phone").remove("user_avatar_url").apply()
                                        userName = null
                                        startGoogleSignInFlow()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error during sign out: ${e.message}", e)
                                        Toast.makeText(this@MainActivity, getString(R.string.toast_sign_out_error), Toast.LENGTH_SHORT).show()
                                        startGoogleSignInFlow()
                                    }
                                }
                            })
                        }
                        composable(Screen.Profile.route) { backStack ->
                            val userId = backStack.arguments?.getString("userId") ?: return@composable
                            com.dawitf.akahidegn.ui.screens.profile.ProfileScreen(
                                userId = userId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.ActivityHistory.route) {
                            com.dawitf.akahidegn.ui.activity.ActivityHistoryScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
                // Celebration confetti overlay (drawn on top of current screen)
                ConfettiEmitter(
                    triggerKey = celebrationKey,
                    testTag = "confettiOverlay",
                    seed = if (BuildConfig.DEBUG) 42L else null
                )
                }
            }
        }

        // DEBUG AUTO-SMOKE: create a temporary test group a few seconds after UI loads (debug builds only)
        if (BuildConfig.DEBUG && !autoTestGroupCreated) {
            Log.d("SMOKE_TEST", "Scheduling auto group creation for smoke test")
            lifecycleScope.launch {
                try {
                    // Small delay to allow first frame & initial refresh
                    kotlinx.coroutines.delay(2500)
                    val user = auth.currentUser
                    if (user != null && !autoTestGroupCreated) {
                        val lat = _userLocationFlow.value?.latitude ?: 8.9806
                        val lng = _userLocationFlow.value?.longitude ?: 38.7578
                        Log.d("SMOKE_TEST", "Creating AutoTest group now (user=${user.uid})")
                        autoTestGroupCreated = true
                        createGroupInFirebase(
                            toDestination = "AutoTest",
                            pickupLatitude = lat,
                            pickupLongitude = lng,
                            passengerCount = 3
                        )
                    } else if (user == null) {
                        Log.d("SMOKE_TEST", "User still null at auto-create time; will retry in 3s")
                        retryAutoCreateGroup()
                    }
                } catch (t: Throwable) {
                    Log.e("SMOKE_TEST", "Auto-create debug group scheduling failed", t)
                }
            }
        }
    }

    private fun retryAutoCreateGroup(attempt: Int = 1) {
        if (!BuildConfig.DEBUG || autoTestGroupCreated || attempt > 3) return
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            val user = auth.currentUser
            if (user != null && !autoTestGroupCreated) {
                val lat = _userLocationFlow.value?.latitude ?: 8.9806
                val lng = _userLocationFlow.value?.longitude ?: 38.7578
                Log.d("SMOKE_TEST", "Retry #$attempt succeeding; creating AutoTest group (user=${user.uid})")
                autoTestGroupCreated = true
                createGroupInFirebase(
                    toDestination = "AutoTest",
                    pickupLatitude = lat,
                    pickupLongitude = lng,
                    passengerCount = 3
                )
            } else if (user == null) {
                Log.d("SMOKE_TEST", "Retry #$attempt: user still null; scheduling another retry")
                retryAutoCreateGroup(attempt + 1)
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
            // Collect states from MainViewModel
            val groups by mainViewModel.groups.collectAsState()
            val vmIsLoading by mainViewModel.isLoadingGroups.collectAsState()
            val currentLocation by userLocationFlow.collectAsState()

            var searchQuery by remember { mutableStateOf("") }
            var selectedFilters by remember { mutableStateOf(SearchFilters()) }

            LaunchedEffect(searchQuery) {
                mainViewModel.updateSearchQuery(searchQuery)
            }

            // THIS CALL WILL NOW WORK PERFECTLY
            MainScreen(
                groups = groups,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery = query },
                selectedFilters = selectedFilters,
                onFiltersChange = { filters -> selectedFilters = filters },
                onGroupClick = onGroupClick,
                onJoinGroup = { group -> tryJoinGroup(group) }, // Add join functionality with interstitial ad
                isLoading = vmIsLoading,
                onRefreshGroups = onRefreshGroups,
                onCreateGroup = onCreateGroup,
                userLocation = currentLocation,
                onOpenProfile = {
                    val uid = auth.currentUser?.uid ?: return@MainScreen
                    navController.navigate(Screen.Profile.createRoute(uid))
                },
                onOpenHistory = { navController.navigate(Screen.ActivityHistory.route) }
            )

            selectedGroupForDialog?.let { group ->
                val currentUserId = auth.currentUser?.uid ?: ""
                val members = remember(group.memberDetails) { 
                    group.memberDetails.map { (userId, memberInfo) ->
                        GroupMember(
                            id = userId, name = memberInfo.name, phone = memberInfo.phone,
                            avatar = memberInfo.avatar ?: sharedPreferences.getString("user_avatar", "avatar_1")!!, 
                            isCreator = userId == group.creatorId
                        )
                    }
                }
                GroupMembersDialog(
                    group = group, members = members, currentUserId = currentUserId,
                    onDismiss = { selectedGroupForDialog = null },
                    onLeaveGroup = { groupId, userId ->
                        Log.d("GROUP_ACTION", "Leave group $groupId requested by $userId")
                        Toast.makeText(this@MainActivity, getString(R.string.toast_leave_group_not_implemented), Toast.LENGTH_SHORT).show()
                        selectedGroupForDialog = null 
                    }
                )
            }

            // Sign-in prompt dialog for anonymous users
            if (showSignInDialog) {
                SignInPromptDialog(
                    onDismiss = { showSignInDialog = false },
                    onSignIn = { 
                        showSignInDialog = false
                        startGoogleSignInFlow()
                    }
                )
            }

            // Create group dialog
            if (showCreateGroupDialog) {
                CreateGroupDialog(
                    onDismiss = { showCreateGroupDialog = false },
                    onCreateGroup = { destination, passengerCount ->
                        showCreateGroupDialog = false
                        showRewardedAdForGroupCreation(destination, passengerCount)
                    }
                )
            }

            // Disband group confirmation dialog
            if (showDisbandConfirmDialog && groupToDisband != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showDisbandConfirmDialog = false
                        groupToDisband = null
                    },
                    title = { Text("ቡድንን ማሰራጨት") },
                    text = { 
                        Column {
                            Text(
                                "እርግጠኛ ነዎት ይህንን ቡድን ማሰራጨት ይፈልጋሉ? ይህ ድርጊት መመለስ የማይችል ሲሆን ሁሉም አባላት ቡድኑን ይለቃሉ።",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Show native ad if available
                            nativeAd?.let { ad ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            val nativeAdView = com.google.android.gms.ads.nativead.NativeAdView(context)
                                            val inflater = android.view.LayoutInflater.from(context)
                                            val adView = inflater.inflate(android.R.layout.simple_list_item_2, nativeAdView, false)
                                            
                                            // Simple native ad implementation
                                            val titleView = adView.findViewById<android.widget.TextView>(android.R.id.text1)
                                            val bodyView = adView.findViewById<android.widget.TextView>(android.R.id.text2)
                                            
                                            titleView?.text = ad.headline ?: "Sponsored"
                                            bodyView?.text = ad.body ?: "Advertisement"
                                            
                                            nativeAdView.headlineView = titleView
                                            nativeAdView.bodyView = bodyView
                                            nativeAdView.setNativeAd(ad)
                                            nativeAdView.addView(adView)
                                            nativeAdView
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                groupToDisband?.let { group ->
                                    mainViewModel.disbandGroup(
                                        group = group,
                                        onSuccess = {
                                            Toast.makeText(this@MainActivity, "ቡድኑ በተሳካ ሁኔታ ተሰራጭቷል", Toast.LENGTH_SHORT).show()
                                            // Load a new native ad for next time
                                            loadNativeAd()
                                        },
                                        onError = { error ->
                                            Toast.makeText(this@MainActivity, "ቡድንን ማሰራጨት አልተቻለም: $error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                                showDisbandConfirmDialog = false
                                groupToDisband = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("ማሰራጨት")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showDisbandConfirmDialog = false
                                groupToDisband = null
                            }
                        ) {
                            Text("ተወው")
                        }
                    }
                )
            }

            // Leave group confirmation dialog
            if (showLeaveConfirmDialog && groupToLeave != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showLeaveConfirmDialog = false
                        groupToLeave = null
                    },
                    title = { Text("ቡድንን መልቀቅ") },
                    text = { 
                        Column {
                            Text(
                                "እርግጠኛ ነዎት ይህንን ቡድን መልቀቅ ይፈልጋሉ? በኋላ እንደገና መቀላቀል መቻል ይችላሉ።",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Show native ad if available
                            nativeAd?.let { ad ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            val nativeAdView = com.google.android.gms.ads.nativead.NativeAdView(context)
                                            val inflater = android.view.LayoutInflater.from(context)
                                            val adView = inflater.inflate(android.R.layout.simple_list_item_2, nativeAdView, false)
                                            
                                            // Simple native ad implementation
                                            val titleView = adView.findViewById<android.widget.TextView>(android.R.id.text1)
                                            val bodyView = adView.findViewById<android.widget.TextView>(android.R.id.text2)
                                            
                                            titleView?.text = ad.headline ?: "Sponsored"
                                            bodyView?.text = ad.body ?: "Advertisement"
                                            
                                            nativeAdView.headlineView = titleView
                                            nativeAdView.bodyView = bodyView
                                            nativeAdView.setNativeAd(ad)
                                            nativeAdView.addView(adView)
                                            nativeAdView
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                groupToLeave?.let { group ->
                                    mainViewModel.leaveGroup(
                                        group = group,
                                        onSuccess = {
                                            Toast.makeText(this@MainActivity, "ቡድኑን በተሳካ ሁኔታ ልቀዋል", Toast.LENGTH_SHORT).show()
                                            // Load a new native ad for next time
                                            loadNativeAd()
                                        },
                                        onError = { error ->
                                            Toast.makeText(this@MainActivity, "ቡድንን መልቀቅ አልተቻለም: $error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                                showLeaveConfirmDialog = false
                                groupToLeave = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("መውጣት")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showLeaveConfirmDialog = false
                                groupToLeave = null
                            }
                        ) {
                            Text("ተወው")
                        }
                    }
                )
            }
        }
    }

    private fun showGroupMembersDialog(group: Group) {
        selectedGroupForDialog = group 
    }

    private fun showGroupDialog(group: Group) {
        // This is for viewing group details when called from ActiveGroupsScreen
        // For now, we'll also use the members dialog, but this could be enhanced 
        // to show a different dialog for group creators/members
        showGroupMembersDialog(group)
    }

    private fun showSignInPrompt() {
        showSignInDialog = true
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

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("NOTIFICATIONS", "POST_NOTIFICATIONS permission not granted. Requesting...")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {
                Log.d("NOTIFICATIONS", "POST_NOTIFICATIONS permission already granted.")
            }
        } else {
            Log.d("NOTIFICATIONS", "POST_NOTIFICATIONS permission not required for this API level.")
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    @Suppress("DEPRECATION") 
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("LOCATION", "ACCESS_FINE_LOCATION permission granted.")
                    setupLocationUpdates()
                } else {
                    Log.w("LOCATION", "ACCESS_FINE_LOCATION permission denied.")
                    Toast.makeText(this, getString(R.string.toast_location_permission_required_nearby), Toast.LENGTH_LONG).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("NOTIFICATIONS", "POST_NOTIFICATIONS permission granted.")
                    Toast.makeText(this, "Notifications enabled. You'll receive updates about group activities.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("NOTIFICATIONS", "POST_NOTIFICATIONS permission denied.")
                    Toast.makeText(this, "Notifications disabled. You won't receive updates about group activities.", Toast.LENGTH_LONG).show()
                }
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
