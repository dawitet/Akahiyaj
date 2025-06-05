package com.dawitf.akahidegn

// Removed LocalContext as most direct UI context needs are in Composables now
// Removed LocalFocusManager as it's handled in Composable files
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.ui.components.CreateRideDialog
import com.dawitf.akahidegn.ui.screens.ChatScreen
import com.dawitf.akahidegn.ui.screens.NameInputScreen
import com.dawitf.akahidegn.ui.screens.NewMainScreen
import com.dawitf.akahidegn.ui.social.RideBuddyScreen
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.debug.GroupCleanupDebugHelper
import javax.inject.Inject

// Data classes (ChatMessage, Group) - consider moving to a 'data' package
@IgnoreExtraProperties
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var groupCleanupDebugHelper: GroupCleanupDebugHelper

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var activeGroupsRef: DatabaseReference
    private lateinit var groupChatsRef: DatabaseReference

    private var currentFirebaseUserId by mutableStateOf<String?>(null)
    private var currentUserDisplayName by mutableStateOf<String?>(null)
    private val currentCloudflareUserId: String = "dummy_cloudflare_user_123" // Placeholder

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var activityCurrentLocation by mutableStateOf<Location?>(null) // Activity-level location for updates
    private lateinit var locationCallback: LocationCallback
    private var locationPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    // ViewModel reference for accessing from ad callbacks
    private lateinit var mainViewModel: MainViewModel

    private var interstitialAd: InterstitialAd? = null
    private val adUnitIdInterstitial = "ca-app-pub-3940256099942544/1033173712" // Test ID
    private var rewardedAd: RewardedAd? = null
    private val adUnitIdRewarded = "ca-app-pub-3940256099942544/5224354917" // Test ID
    private var isLoadingRewardedAd by mutableStateOf(false)

    private val fcmChannelId = "akahidegn_group_notifications"
    private val fcmNotificationId = 1001

    private val snackbarHostState = SnackbarHostState()

    private enum class AppScreen { ASK_NAME, MAIN_CONTENT, CHAT, RIDE_BUDDIES, DEBUG_MENU }
    private var currentAppScreen by mutableStateOf(AppScreen.ASK_NAME)

    // Debug mode flag - set to true during testing
    private var isDebugMode by mutableStateOf(true)

    // State for Create Ride Dialog flow
    private var showCreateRideDialog by mutableStateOf(false)
    private var showAdPromptForCreateRideDialog by mutableStateOf(false)
    private var tempDestinationForCreateRide by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        try {
            firebaseAuth = Firebase.auth
            
            // Note: Firebase persistence is already enabled in AkahidegnApplication.onCreate()
            activeGroupsRef = Firebase.database.getReference("active_groups")
            groupChatsRef = Firebase.database.getReference("group_chats")
            com.google.firebase.installations.FirebaseInstallations.getInstance().id
                .addOnSuccessListener { installationId ->
                    Log.d("FIREBASE_INSTALL", "Firebase Installation ID: $installationId")
                }
                .addOnFailureListener { exception ->
                    Log.e("FIREBASE_INSTALL", "Firebase Installation error: ${exception.message}", exception)
                }
        } catch (e: Exception) {
            Log.e("FIREBASE_INIT", "Failed to initialize Firebase: ${e.message}", e)
            Toast.makeText(this, "Firebase initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            // Consider finishing activity or showing a persistent error UI
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()

        // Check for existing display name
        val savedName = PreferenceManager.getUserDisplayName(this)
        if (savedName != null) {
            currentUserDisplayName = savedName
            currentAppScreen = AppScreen.MAIN_CONTENT
            Log.d("USER_INIT", "User name loaded: $savedName")
        } else {
            currentAppScreen = AppScreen.ASK_NAME
            Log.d("USER_INIT", "No user name found. Requesting input.")
        }

        // Load recent searches
        // This will now be handled by the ViewModel

        // Authenticate user
        if (::firebaseAuth.isInitialized) {
            signInToFirebaseAnonymously()
        }

        // Load Ads, setup notifications
        loadInterstitialAd()
        loadRewardedAd()
        createNotificationChannel()
        retrieveFCMToken()


        setContent {
            // Get ViewModel instance
            val viewModel: MainViewModel = viewModel()
            
            // Store ViewModel reference for use in ad callbacks
            mainViewModel = viewModel
            
            // Collect state from ViewModel
            val groups by viewModel.groups.collectAsState()
            val isLoadingGroups by viewModel.isLoadingGroups.collectAsState()
            val recentSearches by viewModel.recentSearches.collectAsState()
            val currentSearchQuery by viewModel.searchQuery.collectAsState()
            val currentLocation by viewModel.currentLocation.collectAsState()
            val selectedGroupForChat by viewModel.selectedGroup.collectAsState()
            val chatMessages by viewModel.chatMessages.collectAsState()
            
            // Initialize ViewModel with Firebase references when they're ready
            LaunchedEffect(currentFirebaseUserId) {
                val userId = currentFirebaseUserId
                if (userId != null && ::activeGroupsRef.isInitialized) {
                    viewModel.initializeFirebase(activeGroupsRef, groupChatsRef, userId)
                    viewModel.loadRecentSearches(this@MainActivity)
                }
            }
            
            // Update location in ViewModel when it changes
            LaunchedEffect(activityCurrentLocation) {
                activityCurrentLocation?.let { viewModel.updateLocation(it) }
            }

            locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted || coarseLocationGranted) {
                    Log.d("LOCATION_TAG", "Location Permission granted after request. Starting updates.")
                    startLocationUpdates()
                } else {
                    Log.w("LOCATION_TAG", "Location permission denied after request.")
                    lifecycleScope.launch {
                        snackbarHostState.showSnackbar(getString(R.string.toast_location_permission_required))
                    }
                }
            }

            // Simplified LaunchedEffect for initial data loading
            LaunchedEffect(currentFirebaseUserId, hasLocationPermission()) {
                if (currentAppScreen == AppScreen.MAIN_CONTENT && currentFirebaseUserId != null) {
                    if (hasLocationPermission()) {
                        // ViewModel will handle the search automatically through debouncing
                        Log.d("MainActivity", "Permissions granted, ViewModel will handle search")
                    } else {
                        requestInitialPermissions()
                    }
                }
            }

            AkahidegnTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (currentAppScreen) {
                        AppScreen.ASK_NAME -> NameInputScreen(
                            onNameSubmitted = { name ->
                                PreferenceManager.saveUserDisplayName(this@MainActivity, name)
                                currentUserDisplayName = name
                                currentAppScreen = AppScreen.MAIN_CONTENT
                                // Initialize ViewModel with recent searches when name is set
                                viewModel.loadRecentSearches(this@MainActivity)
                                // After name is submitted, if auth is done & permissions are okay, data fetching will trigger via LaunchedEffect
                                if (currentFirebaseUserId != null && hasLocationPermission()) {
                                    // ViewModel will handle search automatically
                                    Log.d("USER_INIT", "User authenticated and has permissions, ViewModel will handle search")
                                } else if (currentFirebaseUserId != null && !hasLocationPermission()) {
                                    requestInitialPermissions()
                                }
                                Log.d("USER_INIT", "User name saved: $name")
                            }
                        )
                        AppScreen.MAIN_CONTENT -> {
                            // This logic handles automatic navigation to ChatScreen if a group is selected
                            if (selectedGroupForChat != null && currentFirebaseUserId != null && currentUserDisplayName != null) {
                                currentAppScreen = AppScreen.CHAT // Trigger recomposition
                            } else {
                                NewMainScreen(
                                    groups = groups,
                                    isLoadingGroups = isLoadingGroups,
                                    recentSearches = recentSearches,
                                    currentSearchText = currentSearchQuery,
                                    onSearchQueryChanged = { query ->
                                        viewModel.updateSearchQuery(query)
                                    },
                                    onPerformSearch = { query ->
                                        val trimmedQuery = query.trim()
                                        viewModel.updateSearchQuery(trimmedQuery)
                                        if (trimmedQuery.isNotBlank()) {
                                            viewModel.addRecentSearch(this@MainActivity, trimmedQuery)
                                        }
                                        Log.d("Search", "Search submitted for: $trimmedQuery")
                                    },
                                    onRideClicked = { group ->
                                        // Show interstitial ad before joining the group
                                        showInterstitialAdForJoinGroup(group)
                                    },
                                    onCreateRideClicked = {
                                        if (currentFirebaseUserId == null) {
                                            Toast.makeText(this, getString(R.string.toast_user_not_authenticated), Toast.LENGTH_SHORT).show()
                                            return@NewMainScreen
                                        }
                                        if (!hasLocationPermission() || activityCurrentLocation == null) {
                                            Toast.makeText(this, getString(R.string.toast_location_not_available_for_create), Toast.LENGTH_SHORT).show()
                                            requestPermissionsLogic() // Attempt to get permissions
                                            return@NewMainScreen
                                        }
                                        // Start the create ride flow by showing the ad prompt first
                                        showAdPromptForCreateRideDialog = true
                                    },                    onBackClicked = {
                        // Define appropriate back behavior, e.g., finish activity
                        finish()
                    },
                    onNavigateToRideBuddies = {
                        currentAppScreen = AppScreen.RIDE_BUDDIES
                    },
                    onNavigateToDebug = if (isDebugMode) {
                        { currentAppScreen = AppScreen.DEBUG_MENU }
                    } else null,
                    snackbarHostState = snackbarHostState
                )

                                // Ad Prompt Dialog for creating a ride
                                if (showAdPromptForCreateRideDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showAdPromptForCreateRideDialog = false },
                                        title = { Text(stringResource(R.string.create_group_ad_prompt_title)) },
                                        text = { Text(stringResource(R.string.create_group_ad_prompt_message)) },
                                        confirmButton = {
                                            Button(onClick = {
                                                showAdPromptForCreateRideDialog = false
                                                tempDestinationForCreateRide = "" // Reset
                                                showCreateRideDialog = true       // Show next dialog
                                            }) { Text(stringResource(R.string.dialog_button_ok_amharic)) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showAdPromptForCreateRideDialog = false }) {
                                                Text(stringResource(R.string.dialog_button_no_amharic))
                                            }
                                        }
                                    )
                                }

                                // Destination Input Dialog for creating a ride
                                if (showCreateRideDialog) {
                                    CreateRideDialog( // This is your composable from ui.components
                                        destinationInput = tempDestinationForCreateRide,
                                        onDestinationChange = { tempDestinationForCreateRide = it },
                                        onDismissRequest = { showCreateRideDialog = false },
                                        onConfirm = { destination ->
                                            showCreateRideDialog = false
                                            if (destination.isNotBlank()) {
                                                onShowRewardedAdForCreateGroup(destination) // Call AdMob flow
                                            } else {
                                                Toast.makeText(this@MainActivity, getString(R.string.toast_please_enter_destination), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        AppScreen.CHAT -> {
                            if (selectedGroupForChat == null || currentFirebaseUserId == null || currentUserDisplayName == null) {
                                // Fallback: if essential data is missing, navigate back to main content
                                currentAppScreen = AppScreen.MAIN_CONTENT
                                viewModel.selectGroup(null) // Clear selection in ViewModel
                            } else {
                                ChatScreen(
                                    group = selectedGroupForChat!!,
                                    messages = chatMessages,
                                    currentUserId = currentFirebaseUserId!!,
                                    currentUserDisplayName = currentUserDisplayName!!,
                                    onSendMessage = { messageText ->
                                        viewModel.sendMessage(selectedGroupForChat!!.groupId!!, messageText, currentFirebaseUserId!!, currentUserDisplayName!!)
                                    },
                                    onCloseChat = {
                                        selectedGroupForChat?.groupId?.let { viewModel.detachChatListener(it) }
                                        viewModel.selectGroup(null)
                                        currentAppScreen = AppScreen.MAIN_CONTENT // Navigate back
                                    },
                                    snackbarHostState = snackbarHostState
                                )
                                // Attach listener when ChatScreen becomes visible
                                LaunchedEffect(selectedGroupForChat?.groupId) {
                                    selectedGroupForChat?.groupId?.let { 
                                        viewModel.attachChatListener(it, currentUserDisplayName!!)
                                    }
                                }
                            }
                        }
                        AppScreen.RIDE_BUDDIES -> {
                            RideBuddyScreen(
                                onNavigateBack = {
                                    currentAppScreen = AppScreen.MAIN_CONTENT
                                }
                            )
                        }
                        AppScreen.DEBUG_MENU -> {
                            DebugMenuScreen(
                                groupCleanupDebugHelper = groupCleanupDebugHelper,
                                onNavigateBack = {
                                    currentAppScreen = AppScreen.MAIN_CONTENT
                                }
                            )
                        }
                    }
                }
            }
        }
    } // End of onCreate

    // --- CONTINUATION FROM MainActivity.kt - Part 2 of 3 ---

    // --- AdMob Methods ---
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, adUnitIdInterstitial, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADMOB_TAG", "Interstitial ad failed to load: ${adError.message}")
                interstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("ADMOB_TAG", "Interstitial ad loaded successfully.")
                this@MainActivity.interstitialAd = interstitialAd
            }
        })
    }

    private fun showInterstitialAd(onAdDismissedOrFailed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("ADMOB_TAG", "Interstitial Ad was dismissed.")
                    interstitialAd = null // Ad is used up
                    loadInterstitialAd()   // Preload next
                    onAdDismissedOrFailed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("ADMOB_TAG", "Interstitial Ad failed to show: ${adError.message}")
                    interstitialAd = null // Ad might be invalid
                    loadInterstitialAd()   // Try to load another one
                    onAdDismissedOrFailed()
                }
                override fun onAdShowedFullScreenContent() {
                    Log.d("ADMOB_TAG", "Interstitial Ad showed fullscreen content.")
                    // The interstitialAd is invalidated by the SDK after it's shown.
                }
            }
            interstitialAd?.show(this) // 'this' is the Activity context
        } else {
            Log.d("ADMOB_TAG", "Interstitial ad not ready. Skipping ad show.")
            loadInterstitialAd() // Try to load an ad if it wasn't ready
            onAdDismissedOrFailed() // Proceed with the action if ad is not shown
        }
    }

    private fun loadRewardedAd() {
        if (rewardedAd != null || isLoadingRewardedAd) {
            return // Already loaded or loading
        }
        isLoadingRewardedAd = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, adUnitIdRewarded, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADMOB_TAG", "Rewarded ad failed to load: ${adError.message}")
                rewardedAd = null
                isLoadingRewardedAd = false
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d("ADMOB_TAG", "Rewarded ad loaded successfully.")
                this@MainActivity.rewardedAd = rewardedAd
                isLoadingRewardedAd = false
                // Set full screen content callback to handle ad lifecycle events
                this@MainActivity.rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("ADMOB_TAG", "Rewarded ad was dismissed.")
                        this@MainActivity.rewardedAd = null // Ad is used up or dismissed.
                        loadRewardedAd() // Preload the next rewarded ad.
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e("ADMOB_TAG", "Rewarded ad failed to show: ${adError.message}")
                        this@MainActivity.rewardedAd = null
                        loadRewardedAd() // Try to load another one.
                    }
                    override fun onAdShowedFullScreenContent() {
                        Log.d("ADMOB_TAG", "Rewarded ad showed fullscreen content.")
                    }
                }
            }
        })
    }

    // This function is called after the user confirms the destination for a new ride
    private fun onShowRewardedAdForCreateGroup(destination: String) {
        if (rewardedAd != null) {
            rewardedAd?.show(this@MainActivity) { rewardItem ->
                Log.d("ADMOB_TAG", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                // User earned the reward, proceed with group creation
                createGroupInFirebase(destination) { success, messageOrGroupId ->
                    if (success) {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_group_created_successfully, messageOrGroupId?.take(6) ?: "N/A"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_failed_to_create_group, messageOrGroupId ?: "Unknown error"), Toast.LENGTH_SHORT).show()
                    }
                    // ViewModel will automatically refresh the group list
                }
            }
        } else {
            Log.d("ADMOB_TAG", "Rewarded ad for create group wasn't ready when requested.")
            Toast.makeText(this@MainActivity, getString(R.string.toast_rewarded_ad_not_ready), Toast.LENGTH_SHORT).show()
            loadRewardedAd() // Attempt to load an ad for next time
            // Decide if you want to allow group creation without ad as a fallback, or just inform the user.
            // For now, it just shows a toast and doesn't create the group.
        }
    }


    // --- Firebase Group Interaction Methods ---
    fun createGroupInFirebase(destination: String, callback: (Boolean, String?) -> Unit) {
        val cleanDestination = destination.trim().lowercase(Locale.getDefault())
        val loc = activityCurrentLocation // Use activity-level location variable
        val uid = currentFirebaseUserId
        // val creatorName = currentUserDisplayName ?: getString(R.string.default_display_name_anonymous) // If storing creator name in group

        if (uid == null) {
            callback(false, getString(R.string.toast_user_not_authenticated_generic))
            return
        }
        if (loc == null) {
            callback(false, getString(R.string.toast_location_not_available_generic))
            return
        }
        if (cleanDestination.isBlank()) {
            callback(false, getString(R.string.toast_destination_blank_generic))
            return
        }

        val groupId = activeGroupsRef.push().key
        if (groupId == null) {
            Log.e("CREATE_GROUP_TAG", "Failed to generate groupId from Firebase.")
            callback(false, "Failed to create group ID.")
            return
        }

        // Example of adding a random image URL for the new UI
        val randomImageSeed = (0..1000).random()
        val placeholderImageUrl = "https://picsum.photos/seed/${groupId}-${randomImageSeed}/400/300"


        val newGroup = Group(
            creatorId = uid,
            creatorCloudflareId = currentCloudflareUserId, // Placeholder
            destinationName = cleanDestination,
            pickupLat = loc.latitude,
            pickupLng = loc.longitude,
            timestamp = System.currentTimeMillis(),
            members = hashMapOf(uid to true), // Creator is the first member
            memberCount = 1,
            maxMembers = 4, // Default max members
            imageUrl = placeholderImageUrl // Add image URL for new UI
        )

        Log.d("CREATE_GROUP_TAG", "Attempting to create group: $groupId with data: ${newGroup.toMap()}")
        activeGroupsRef.child(groupId).setValue(newGroup.toMap())
            .addOnSuccessListener {
                Log.i("CREATE_GROUP_TAG", "Group '$cleanDestination' created successfully with ID: $groupId")
                callback(true, groupId)
            }
            .addOnFailureListener { e ->
                Log.e("CREATE_GROUP_TAG", "Failed to create group '$cleanDestination': ${e.message}", e)
                callback(false, e.message ?: getString(R.string.toast_failed_generic))
            }
    }

    fun joinGroupInFirebase(groupToJoin: Group, callback: (Boolean, String?) -> Unit) {
        val uid = currentFirebaseUserId
        val gId = groupToJoin.groupId

        if (uid == null) { callback(false, getString(R.string.toast_user_not_authenticated_generic)); return }
        if (gId == null) { callback(false, "Group ID is missing."); return }

        val groupRef = activeGroupsRef.child(gId)
        groupRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val group = currentData.getValue(Group::class.java) ?: return Transaction.abort() // Group deleted
                if (group.members.containsKey(uid)) return Transaction.abort() // Already a member
                if (group.memberCount >= group.maxMembers) return Transaction.abort() // Group is full

                group.members[uid] = true
                group.memberCount++
                currentData.value = group.toMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("JOIN_GROUP_TAG", "Transaction failed: ${error.message}", error.toException())
                    callback(false, error.message)
                } else if (committed) {
                    Log.i("JOIN_GROUP_TAG", "Successfully joined group $gId")
                    callback(true, null)
                } else { // Transaction aborted
                    Log.w("JOIN_GROUP_TAG", "Join transaction aborted for group $gId.")
                    val groupNow = currentData?.getValue(Group::class.java)
                    val reason = when {
                        groupNow == null -> getString(R.string.toast_group_no_longer_exists)
                        groupNow.members.containsKey(uid) -> getString(R.string.toast_already_a_member)
                        groupNow.memberCount >= groupNow.maxMembers -> getString(R.string.toast_group_is_full)
                        else -> getString(R.string.toast_join_group_failed_generic)
                    }
                    callback(false, reason)
                }
            }
        })
    }

    private fun disbandGroupInFirebase(groupToDisband: Group, callback: (Boolean, String?) -> Unit) {
        val uid = currentFirebaseUserId
        val gId = groupToDisband.groupId

        if (uid == null) { callback(false, getString(R.string.toast_user_not_authenticated_generic)); return }
        if (gId == null) { callback(false, "Group ID missing, cannot disband."); return }
        if (groupToDisband.creatorId != uid) {
            callback(false, getString(R.string.toast_not_creator_disband))
            return
        }

        Log.d("DISBAND_GROUP_TAG", "Attempting to disband group: $gId by user: $uid")
        groupChatsRef.child(gId).removeValue() // Remove chat history
        activeGroupsRef.child(gId).removeValue() // Remove group
            .addOnSuccessListener {
                Log.i("DISBAND_GROUP_TAG", "Group $gId and its chat disbanded successfully.")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("DISBAND_GROUP_TAG", "Failed to disband group $gId: ${e.message}", e)
                callback(false, e.message ?: getString(R.string.toast_failed_generic))
            }
    }

    private fun changeMaxMembersInFirebase(group: Group, newMaxMembers: Int, callback: (Boolean, String?) -> Unit) {
        val uid = currentFirebaseUserId
        val gId = group.groupId

        if (uid == null) { callback(false, getString(R.string.toast_user_not_authenticated_generic)); return }
        if (gId == null) { callback(false, "Group ID missing."); return }
        if (group.creatorId != uid) { callback(false, getString(R.string.toast_not_creator_change_members)); return }
        if (newMaxMembers < 1 || newMaxMembers > 10) { // Example: allow 1 to 10 members
            callback(false, getString(R.string.toast_invalid_max_members_range, "1", "10"))
            return
        }

        activeGroupsRef.child(gId).runTransaction(object: Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentGroupState = currentData.getValue(Group::class.java) ?: return Transaction.abort()
                if (newMaxMembers < currentGroupState.memberCount) { // Cannot set max lower than current
                    return Transaction.abort()
                }
                currentGroupState.maxMembers = newMaxMembers
                currentData.value = currentGroupState.toMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    callback(false, error.message)
                } else if (committed) {
                    callback(true, null)
                } else {
                    val groupNow = currentData?.getValue(Group::class.java)
                    val reason = if (groupNow != null && newMaxMembers < groupNow.memberCount) {
                        getString(R.string.toast_cannot_reduce_max_members, groupNow.memberCount.toString())
                    } else {
                        getString(R.string.toast_failed_generic) // Generic abort reason
                    }
                    callback(false, reason)
                }
            }
        })
    }


    // --- Authentication and Location Methods ---
    private fun signInToFirebaseAnonymously() {
        if (firebaseAuth.currentUser == null) {
            Log.d("AUTH_TAG", "Attempting Firebase Anonymous Sign-In...")
            firebaseAuth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        currentFirebaseUserId = firebaseAuth.currentUser?.uid
                        Log.d("AUTH_TAG", "Firebase Anonymous Sign-In: SUCCESS, UID: $currentFirebaseUserId")
                        // ViewModel will handle the search automatically once initialized
                        if (currentAppScreen == AppScreen.MAIN_CONTENT && !hasLocationPermission()) {
                            // If name is already known (so on MAIN_CONTENT) but no location permission, request it.
                            requestInitialPermissions()
                        }
                    } else {
                        Log.w("AUTH_TAG", "Firebase Anonymous Sign-In: FAILURE", task.exception)
                        Toast.makeText(this@MainActivity, getString(R.string.toast_auth_failed_firebase_reason, task.exception?.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // User is already authenticated
            currentFirebaseUserId = firebaseAuth.currentUser?.uid
            Log.d("AUTH_TAG", "User already authenticated: UID: $currentFirebaseUserId")
            // ViewModel will handle the search automatically once initialized
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.lastLocation
                if (lastLocation != null) {
                    activityCurrentLocation = lastLocation
                    Log.d("LOCATION_TAG", "Location Updated: ${activityCurrentLocation?.latitude}, ${activityCurrentLocation?.longitude}")
                    // Location update will trigger ViewModel update via LaunchedEffect
                }
            }
        }
    }

    private fun requestInitialPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        // Location Permissions
        if (!hasLocationPermission()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d("PERM_TAG", "Requesting permissions: ${permissionsToRequest.joinToString()}")
            locationPermissionLauncher?.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("PERM_TAG", "All necessary permissions (location/notifications) already granted.")
            // If all permissions are already granted, and specifically location is granted, start updates.
            if (hasLocationPermission()) {
                startLocationUpdates()
            }
        }
    }

    // Called by UI elements (e.g., a button) if permission needs to be re-requested
    private fun requestPermissionsLogic() {
        Log.d("LOCATION_TAG", "requestPermissionsLogic called by UI.")
        requestInitialPermissions() // Reuse the same consolidated logic
    }

    @SuppressLint("MissingPermission") // We check permissions with hasLocationPermission() before calling this
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w("LOCATION_TAG", "startLocationUpdates called without location permission.")
            // Optionally, trigger requestPermissionsLogic() again or show a persistent message
            return
        }
        
        Log.d("LOCATION_TAG", "Starting location updates.")
        
        // Check location settings before starting updates
        checkLocationSettings(
            onSuccess = {
                // Location settings are satisfied, proceed with location updates
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L) // Update every 10 seconds
                    .setMinUpdateIntervalMillis(5000L) // Minimum interval 5 seconds
                    .build()
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    Log.d("LOCATION_TAG", "Location updates started successfully")
                } catch (e: SecurityException) {
                    Log.e("LOCATION_TAG", "SecurityException in requestLocationUpdates: ${e.message}")
                }
            },
            onFailure = {
                // Location settings are not satisfied, but we can still try to get location
                // This provides a fallback in case the user chooses not to enable high accuracy
                Log.w("LOCATION_TAG", "Location settings not optimal, starting with current settings")
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000L)
                    .setMinUpdateIntervalMillis(10000L)
                    .build()
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    Log.e("LOCATION_TAG", "SecurityException in fallback requestLocationUpdates: ${e.message}")
                }
            }
        )
    }

    private fun stopLocationUpdates() {
        Log.d("LOCATION_TAG", "Stopping location updates.")
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (e: Exception) {
                Log.e("LOCATION_TAG", "Error stopping location updates: ${e.message}")
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // New function to check if location services (GPS) are enabled
    private fun checkLocationSettings(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Shows the dialog even if location is currently disabled
            .build()

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { _: LocationSettingsResponse ->
                Log.d("LOCATION_TAG", "Location settings are satisfied")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.w("LOCATION_TAG", "Location settings are not satisfied", exception)
                // Here you could show a dialog to the user to enable location services
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(
                        "Please enable location services for better accuracy. Check your device settings."
                    )
                }
                onFailure()
            }
    }


    // --- Lifecycle Overrides ---
    override fun onResume() {
        super.onResume()
        // Start location updates only if user is on main content or chat screen and has permission
        if (currentAppScreen == AppScreen.MAIN_CONTENT || currentAppScreen == AppScreen.CHAT) {
            if (hasLocationPermission()) {
                Log.d("LIFECYCLE_TAG", "onResume: Permissions granted, starting location updates.")
                startLocationUpdates()
            } else {
                Log.d("LIFECYCLE_TAG", "onResume: Location permissions NOT granted. User might need to grant via UI or initial prompt.")
                // Optionally, if on main screen and permissions were previously denied, you might prompt again
                // Be careful not to create annoying loops.
                // if(currentAppScreen == AppScreen.MAIN_CONTENT) requestInitialPermissions()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("LIFECYCLE_TAG", "onPause: Stopping location updates.")
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Chat cleanup is handled by ViewModel
    }


    // --- FCM & Notification Methods ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.fcm_channel_name)
            val descriptionText = getString(R.string.fcm_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(fcmChannelId, name, importance).apply {
                description = descriptionText
                // enableLights(true)
                // lightColor = android.graphics.Color.RED
                // enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("FCM_TAG", "Notification channel $fcmChannelId created.")
        }
    }

    private fun retrieveFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TAG", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TAG", "FCM Registration Token: $token")
            // TODO LATER: Send this token to your server/Firebase if you implement user-specific notifications for specific groups/users.
        })
    }

    fun showLocalNotification(title: String, message: String) {
        Log.d("NOTIFICATION_TAG", "Attempting to show local notification: $title - $message")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NOTIFICATION_TAG", "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                // Consider requesting the permission here or guiding the user to settings.
                // For now, we just inform via Toast if permission is missing.
                Toast.makeText(this, getString(R.string.notification_permission_needed), Toast.LENGTH_LONG).show()
                return
            }
        }

        val builder = NotificationCompat.Builder(this, fcmChannelId)
            .setSmallIcon(R.drawable.ic_notification_icon) // Ensure this drawable exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss notification when tapped
        // TODO: Add an Intent to open the app/specific screen when notification is tapped
        // val intent = Intent(this, MainActivity::class.java)
        // val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // builder.setContentIntent(pendingIntent)

        try {
            notificationManager.notify(fcmNotificationId, builder.build())
            Log.d("NOTIFICATION_TAG", "Notification sent.")
        } catch (e: SecurityException) {
            Log.e("NOTIFICATION_TAG", "SecurityException showing notification: ${e.message}")
        } catch (e: Exception) {
            Log.e("NOTIFICATION_TAG", "Exception showing notification: ${e.message}")
        }
    }

    // This function shows an interstitial ad before joining a group
    private fun showInterstitialAdForJoinGroup(group: Group) {
        Log.d("ADMOB_TAG", "Attempting to show interstitial ad for joining group: ${group.groupId}")
        
        showInterstitialAd {
            // Ad was dismissed or failed, proceed with joining the group
            Log.d("ADMOB_TAG", "Interstitial ad dismissed, proceeding with group join")
            proceedWithJoiningGroup(group)
        }
    }
    
    // This function handles the actual group joining logic
    private fun proceedWithJoiningGroup(group: Group) {
        joinGroupInFirebase(group) { success, message ->
            if (success) {
                Log.d("JOIN_GROUP_TAG", "Successfully joined group, navigating to chat")
                Toast.makeText(this@MainActivity, getString(R.string.toast_join_group_success), Toast.LENGTH_SHORT).show()
                
                // Navigate to chat screen by selecting the group
                mainViewModel.selectGroup(group)
                // The recomposition will handle navigation to chat screen automatically
            } else {
                Log.e("JOIN_GROUP_TAG", "Failed to join group: $message")
                Toast.makeText(this@MainActivity, getString(R.string.toast_join_group_failed, message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
            }
        }
    }
} // End of MainActivity Class

// Helper extension function (should be top-level, outside the class)
fun Double.format(digits: Int): String = "%.${digits}f".format(this, Locale.US)

@Composable
fun DebugMenuScreen(
    groupCleanupDebugHelper: GroupCleanupDebugHelper,
    onNavigateBack: () -> Unit
) {
    var statusMessage by remember { mutableStateOf("Ready for testing") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Debug Menu - Group Cleanup Testing",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        item {
            Text(
                text = "Status: $statusMessage",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Group Testing Actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            statusMessage = "Creating test group..."
                            groupCleanupDebugHelper.createTestGroup("Debug Test Group ${System.currentTimeMillis() % 1000}")
                            statusMessage = "Test group created! Check logs for details."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Test Group")
                    }

                    Button(
                        onClick = {
                            statusMessage = "Listing all groups with timestamps..."
                            groupCleanupDebugHelper.logAllGroupsWithTimestamps()
                            statusMessage = "Groups listed in logs. Check Android logs."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log All Groups with Timestamps")
                    }

                    Button(
                        onClick = {
                            statusMessage = "Logging current timestamp info..."
                            groupCleanupDebugHelper.logCurrentTimestamp()
                            statusMessage = "Timestamp info logged. Check Android logs."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Current Timestamp Info")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cleanup Testing Actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            statusMessage = "Triggering immediate cleanup..."
                            groupCleanupDebugHelper.triggerImmediateCleanup()
                            statusMessage = "Immediate cleanup triggered! Check logs for results."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Trigger Immediate Cleanup")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Testing Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = """
                        1. Create test groups using the button above
                        2. Check logs to see group timestamps and age
                        3. Groups should persist for 30 minutes before cleanup
                        4. Use 'Trigger Immediate Cleanup' to test cleanup process
                        5. Monitor Android logs (tag: GroupCleanupDebug, FirebaseGroupService)
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


