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
import androidx.compose.runtime.Composable
import com.dawitf.akahidegn.ui.screens.MainScreen
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.dawitf.akahidegn.ui.components.GroupMembersWithDialerDialog
import com.dawitf.akahidegn.service.GroupEventMonitorService
import com.dawitf.akahidegn.notifications.service.NotificationManagerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import android.content.SharedPreferences
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
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Correct imports for missing classes
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.MemberInfo
import com.dawitf.akahidegn.ui.components.SuccessWithLeaveGroupDialog
import com.dawitf.akahidegn.ui.components.GroupMembersDialog
import com.dawitf.akahidegn.ui.components.GroupMember

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ViewModel instance for the activity
    private val mainViewModel: MainViewModel by viewModels()
    
    // Inject notification service
    @Inject
    lateinit var notificationService: NotificationManagerService

    // Inject group event monitor service
    @Inject
    lateinit var groupEventMonitorService: GroupEventMonitorService

    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val currentGroups = mutableListOf<Group>()

    // Ad variables
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    
    // User location
    private var userLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private var lastLocationUpdate = 0L

    // Modern permission handling
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start location updates
            setupLocationUpdates()
        } else {
            // Permission denied, show a message to the user
            Toast.makeText(this, "Location permission is required for this feature", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            userLocation = location
            Log.d("LOCATION_UPDATE", "Location received in MainActivity: ${location.latitude}, ${location.longitude}")
            
            // Real-time location updates: 30 seconds for group creators, 1 minute for members
            val currentTime = System.currentTimeMillis()
            val updateInterval = if (isGroupCreator()) 30000L else 60000L // 30s vs 60s
            
            if (currentTime - lastLocationUpdate > updateInterval) {
                lastLocationUpdate = currentTime
                // Update ViewModel with new location on background thread
                mainViewModel.updateLocation(location)
                Log.d("LOCATION", "Location updated: ${location.latitude}, ${location.longitude}")
            }
        }
        
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    
    /**
     * Check if current user is a creator of any active group
     */
    private fun isGroupCreator(): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        return currentGroups.any { group -> 
            group.creatorId == currentUserId && 
            (group.timestamp ?: 0L) > (System.currentTimeMillis() - 30 * 60 * 1000L) // Group is active (less than 30 minutes old)
        }
    }
    
    // User preferences
    private lateinit var sharedPreferences: SharedPreferences
    private var userName: String? = null

    companion object {
        // Theme mode key for preferences
        val THEME_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("akahiyaj_prefs", Context.MODE_PRIVATE)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // Request location permissions and start location updates
        setupLocationUpdates()
        
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {
            Log.d("ADS", "Mobile Ads SDK initialized")
        }
        
        // Initialize Firebase components
        database = Firebase.database("https://akahiyaj-79376-default-rtdb.europe-west1.firebasedatabase.app")
        groupsRef = database.reference.child("groups")
        auth = Firebase.auth
        
        // Load ads
        loadRewardedAd()
        loadInterstitialAd()
        
        // Ensure the user is signed in anonymously
        // TEMPORARY BYPASS: Directly initialize main screen for debugging
        auth.signInAnonymously().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AUTH", "signInAnonymously:success - Bypassing user profile check")
                // Hardcode a user name for testing purposes
                userName = "DebugUser"
                initializeMainScreen()
            } else {
                Log.w("AUTH", "signInAnonymously:failure", task.exception)
                // Handle authentication failure, maybe show an error message
            }
        }
    }
    
    private fun loadRewardedAd() {
        // Only load ads if enabled in build configuration
        if (!BuildConfig.ADS_ENABLED || BuildConfig.ADMOB_REWARDED_ID.isEmpty()) {
            Log.d("ADS", "Ads disabled in this build configuration - ADS_ENABLED: ${BuildConfig.ADS_ENABLED}, REWARDED_ID: '${BuildConfig.ADMOB_REWARDED_ID}'")
            return
        }
        
        Log.d("ADS", "Loading rewarded ad with ID: ${BuildConfig.ADMOB_REWARDED_ID}")
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, BuildConfig.ADMOB_REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Rewarded ad failed to load: ${adError.message}")
                Log.d("ADS", "Error details - Code: ${adError.code}, Domain: ${adError.domain}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("ADS", "Rewarded ad loaded successfully")
                rewardedAd = ad
            }
        })
    }
    
    private fun loadInterstitialAd() {
        // Only load ads if enabled in build configuration
        if (!BuildConfig.ADS_ENABLED || BuildConfig.ADMOB_INTERSTITIAL_ID.isEmpty()) {
            Log.d("ADS", "Ads disabled in this build configuration - ADS_ENABLED: ${BuildConfig.ADS_ENABLED}, INTERSTITIAL_ID: '${BuildConfig.ADMOB_INTERSTITIAL_ID}'")
            return
        }
        
        Log.d("ADS", "Loading interstitial ad with ID: ${BuildConfig.ADMOB_INTERSTITIAL_ID}")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, BuildConfig.ADMOB_INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Interstitial ad failed to load: ${adError.message}")
                Log.d("ADS", "Error details - Code: ${adError.code}, Domain: ${adError.domain}")
                interstitialAd = null
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("ADS", "Interstitial ad loaded successfully")
                interstitialAd = ad
            }
        })
    }
    
    private fun refreshGroups() {
        Log.d("GROUP_REFRESH", "refreshGroups() called")
        // Use the ViewModel to refresh groups (shows all active groups)
        mainViewModel.refreshGroups()

        // Debug: Log all groups in the database
        logAllGroups()
    }
    
    // Debug function to log all groups in Firebase
    private fun logAllGroups() {
        groupsRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val groupCount = dataSnapshot.childrenCount
                Log.d("FIREBASE_DEBUG", "Total groups in database: $groupCount")

                dataSnapshot.children.forEach { groupSnapshot ->
                    val group = groupSnapshot.getValue(Group::class.java)
                    group?.let {
                        Log.d("FIREBASE_DEBUG", "Group: ${it.destinationName}, " +
                            "Members: ${it.memberCount}/${it.maxMembers}, " +
                            "Created: ${java.util.Date(it.timestamp ?: 0L)}, " +
                            "Creator: ${it.creatorName}")

                        // Log member details
                        it.memberDetails.forEach { (uid, member) ->
                            Log.d("FIREBASE_DEBUG", "   Member: ${member.name}, " +
                                "Phone: ${member.phone}, Avatar: ${member.avatar}")
                        }
                    }
                }
            } else {
                Log.d("FIREBASE_DEBUG", "No groups found in database")
            }
        }.addOnFailureListener { exception ->
            Log.e("FIREBASE_DEBUG", "Failed to fetch groups", exception)
        }
    }

    private fun filterExpiredGroups(groups: List<Group>): List<Group> {
        try {
            val currentTime = System.currentTimeMillis()
            val thirtyMinutesAgo = currentTime - TimeUnit.MINUTES.toMillis(30)

            // Enhanced expiration filter with proper timestamp logic AND 500m radius filtering
            val filteredGroups = groups.filter { group ->
                // First check expiration
                val isNotExpired = when {
                    // Keep groups with null timestamp (legacy data)
                    group.timestamp == null -> {
                        Log.d("EXPIRATION_FILTER", "Keeping group ${group.destinationName} - null timestamp (legacy)")
                        true
                    }
                    // Keep groups within 30 minutes
                    group.timestamp!! > thirtyMinutesAgo -> {
                        val ageMinutes = (currentTime - group.timestamp!!) / (1000 * 60)
                        Log.d("EXPIRATION_FILTER", "Keeping group ${group.destinationName} - age: ${ageMinutes}min")
                        true
                    }
                    // Filter out expired groups
                    else -> {
                        val ageMinutes = (currentTime - group.timestamp!!) / (1000 * 60)
                        Log.d("EXPIRATION_FILTER", "Filtering out expired group ${group.destinationName} - age: ${ageMinutes}min")
                        false
                    }
                }

                // Then check 500m radius if user location is available
                val isWithinRange = if (userLocation != null && group.pickupLat != null && group.pickupLng != null) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        userLocation!!.latitude, userLocation!!.longitude,
                        group.pickupLat!!, group.pickupLng!!,
                        results
                    )
                    val distance = results[0]
                    val isNear = distance <= 500f // 500 meters

                    Log.d("LOCATION_FILTER", "🎯 DISTANCE CHECK: Group '${group.destinationName}' is ${distance.toInt()}m away from user")
                    Log.d("LOCATION_FILTER", "📍 User: (${userLocation!!.latitude}, ${userLocation!!.longitude})")
                    Log.d("LOCATION_FILTER", "📍 Group: (${group.pickupLat}, ${group.pickupLng})")
                    Log.d("LOCATION_FILTER", "✅ Decision: ${if (isNear) "INCLUDE (≤500m)" else "EXCLUDE (>500m)"}")
                    isNear
                } else {
                    // If no location data, include the group (fallback)
                    Log.d("LOCATION_FILTER", "⚠️ No location data available - including group ${group.destinationName} (fallback)")
                    true
                }

                isNotExpired && isWithinRange
            }

            // Show user feedback if groups were filtered out
            val expiredCount = groups.size - filteredGroups.size
            if (expiredCount > 0) {
                showExpirationNotification(expiredCount)
            }

            Log.d("GROUP_FILTER", "Filtered ${groups.size} groups -> ${filteredGroups.size} active groups within 500m")
            return filteredGroups

        } catch (e: Exception) {
            Log.e("GROUP_FILTER", "Error filtering groups, returning all groups", e)
            // Fallback: return all groups if filtering fails
            return groups
        }
    }

    private fun showExpirationNotification(expiredCount: Int) {
        // Enhanced with our new animation system
        try {
            runOnUiThread {
                setContent {
                    AkahidegnTheme {
                        val animationViewModel: com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val notifications by animationViewModel.notifications.collectAsState()

                        // Show expiration warning animation
                        LaunchedEffect(Unit) {
                            animationViewModel.showSuccess(
                                title = "ቡድኖች ተዘምኑ!",
                                subtitle = "ቀድሞ የነበሩ $expiredCount ቡድኖች ተወግደዋል (ከ30 ደቂቃ በላይ የቆዩ)",
                                preset = com.dawitf.akahidegn.ui.components.NotificationPresets.quickSuccess("ቡድኖች ተዘምኑ")
                            )

                            // Auto-dismiss after 2 seconds and return to main screen
                            kotlinx.coroutines.delay(2000)
                            initializeMainScreen()
                        }

                        // Display animations
                        com.dawitf.akahidegn.ui.components.AnimatedNotificationList(
                            notifications = notifications,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Log.i("EXPIRATION_FILTER", "Showed expiration notification for $expiredCount groups")

        } catch (e: Exception) {
            Log.e("EXPIRATION_FILTER", "Error showing expiration notification", e)
        }
    }
    
    private fun showCreateGroupDialog() {
        // Create destination input dialog with enhanced Amharic UI
        val editText = EditText(this).apply {
            hint = "ወደየት ነው የምትሄደው? (ምሳሌ: ቦሌ፣ መገናኛ፣ ፒያሳ)"
            setPadding(60, 40, 60, 40)
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setBackgroundResource(android.R.drawable.edit_text)
        }
        
        AlertDialog.Builder(this)
            .setTitle("🚗 አዲስ ቡድን ፍጠር")
            .setMessage("የመሳፈሪያ ቡድን ይፍጠሩ እና ከሌሎች ሰዎች ጋር ተጋሩ!")
            .setView(editText)
            .setPositiveButton("✨ ቡድን ፍጠር") { _, _ ->
                val destination = editText.text.toString().trim()
                if (destination.isNotEmpty()) {
                    // Show rewarded ad before creating group
                    showRewardedAdForGroupCreation(destination)
                } else {
                    Toast.makeText(this, "እባክዎ መድረሻ ያስገቡ", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("❌ ሰርዝ", null)
            .show()
    }
    
    private fun showRewardedAdForGroupCreation(destination: String) {
        if (rewardedAd != null) {
            rewardedAd!!.show(this) { rewardItem ->
                Log.d("ADS", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                // Create group after ad completion
                createGroupAfterAd(destination)
                // Load a new rewarded ad for next time
                loadRewardedAd()
            }
        } else {
            Log.d("ADS", "Rewarded ad not ready, creating group without ad")
            // Create group without showing any message to user
            createGroupAfterAd(destination)
        }
    }
    
    private fun createGroupAfterAd(destinationName: String) {
        // Use current location exactly, or default Addis Ababa coordinates without random offset
        val lat = 9.005401
        val lng = 38.763611
        
        Log.d("GROUP_CREATION", "Creating group '$destinationName' at exact location: $lat, $lng (userLocation: $userLocation)")
        createGroupInFirebase(destinationName, lat, lng)
    }
    
    private fun handleGroupSelection(group: Group) {
        // Show interstitial ad before joining group
        if (interstitialAd != null) {
            interstitialAd!!.show(this)
            interstitialAd!!.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("ADS", "Interstitial ad dismissed")
                    // Join group after ad is dismissed
                    joinGroupAfterAd(group)
                    // Load a new interstitial ad for next time
                    loadInterstitialAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d("ADS", "Interstitial ad failed to show")
                    // Join group even if ad fails
                    joinGroupAfterAd(group)
                }
            }
        } else {
            Log.d("ADS", "Interstitial ad not ready, joining group without ad")
            // Join group without showing any message to user
            joinGroupAfterAd(group)
        }
    }
    
    private fun joinGroupAfterAd(group: Group) {
        // When a group is selected, try to join it
        joinGroupInFirebase(group) { success, message ->
            if (success) {
                // Play success vibration and show success dialog
                notificationService.playSuccessVibration()

                // Send notification to other group members about new member joining
                val currentUserName = userName ?: "User"
                notificationService.showUserJoinedNotification(group, currentUserName)

                // Show success animation and then group members with phone numbers
                showSuccessAndGroupMembers(group)

                refreshGroups()
            } else {
                Toast.makeText(
                    this,
                    "Could not join group: ${message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Show success animation followed by group members dialog with phone numbers
     */
    private fun showSuccessAndGroupMembers(group: Group) {
        setContent {
            AkahidegnTheme {
                var showSuccess by remember { mutableStateOf(true) }
                var showGroupMembers by remember { mutableStateOf(false) }

                // Success Dialog with Leave Group option
                if (showSuccess) {
                    SuccessWithLeaveGroupDialog(
                        group = group,
                        isVisible = true,
                        onDismiss = {
                            showSuccess = false
                            showGroupMembers = true
                        },
                        onLeaveGroup = {
                            leaveGroup(group)
                            showSuccess = false
                            // Return to main screen after leaving
                            initializeMainScreen()
                        }
                    )
                }

                // Group Members Dialog with Phone Dialer
                if (showGroupMembers) {
                    GroupMembersWithDialerDialog(
                        group = group,
                        isVisible = true,
                        onDismiss = {
                            showGroupMembers = false
                            // Return to main screen
                            initializeMainScreen()
                        },
                        onLeaveGroup = {
                            leaveGroup(group)
                            showGroupMembers = false
                            // Return to main screen after leaving
                            initializeMainScreen()
                        }
                    )
                }
            }
        }
    }

    private fun createGroupInFirebase(
        destinationName: String,
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        
        // Get user profile data
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatar = sharedPreferences.getString("user_avatar", "avatar_1") ?: "avatar_1"
        
        // Use the destination name directly as the group name
        val newGroup = Group(
            creatorId = currentUserId,
            creatorName = userName ?: "User",
            destinationName = destinationName, // Use the destination name directly
            originalDestination = destinationName, // Store original destination
            pickupLat = pickupLatitude,
            pickupLng = pickupLongitude,
            timestamp = timestamp,
            maxMembers = 4,
            memberCount = 1,
            imageUrl = null // Force use of local images
        )
        
        Log.d("GROUP_CREATION", "Creating group '${destinationName}' at location: ${pickupLatitude}, ${pickupLongitude}")
        
        // Add current user as a member
        newGroup.members[currentUserId] = true
        
        // Add creator member details
        newGroup.memberDetails[currentUserId] = MemberInfo(
            name = userName ?: "User",
            phone = userPhone,
            avatar = userAvatar,
            joinedAt = timestamp
        )
        
        // Push to Firebase with automatic unique key generation
        val newGroupRef = groupsRef.push()
        
        // Set the ID before saving so it's included in toMap()
        newGroup.groupId = newGroupRef.key
        
        // Log the group creation for debugging
        Log.d("FIREBASE", "Attempting to write group to Firebase: ${newGroup.destinationName} with ID: ${newGroup.groupId}")
        Log.d("FIREBASE", "Executing setValue for group: ${newGroup.groupId}")
        
        newGroupRef.setValue(newGroup.toMap())
            .addOnSuccessListener {
                Log.d("FIREBASE", "Group created successfully: ${newGroup.destinationName} - Firebase write SUCCESS")
                refreshGroups() // Refresh groups to immediately show the new group

                // Enhanced with animation system instead of basic Toast
                runOnUiThread {
                    setContent {
                        AkahidegnTheme {
                            val animationViewModel: com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            val notifications by animationViewModel.notifications.collectAsState()

                            // Show group creation success animation
                            LaunchedEffect(Unit) {
                                animationViewModel.showSuccess(
                                    title = "ቡድን ተፈጠረ!",
                                    subtitle = "'${newGroup.destinationName}' ቡድን በተሳካ ሁኔታ ተፈጠረ።",
                                    preset = com.dawitf.akahidegn.ui.components.ContextPresets.FormSubmission.success
                                )

                                // After animation, return to main screen
                                kotlinx.coroutines.delay(3000)
                                initializeMainScreen()
                            }

                            // Display animations
                            com.dawitf.akahidegn.ui.components.AnimatedNotificationList(
                                notifications = notifications,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error creating group: ${newGroup.destinationName} - Firebase write FAILED", e)

                // Enhanced error handling with animation system
                runOnUiThread {
                    setContent {
                        AkahidegnTheme {
                            val animationViewModel: com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            val notifications by animationViewModel.notifications.collectAsState()

                            // Show error animation
                            LaunchedEffect(Unit) {
                                animationViewModel.showError(
                                    title = "ቡድን መፍጠር አልተሳካም!",
                                    subtitle = "ስህተት: ${e.message}",
                                    preset = com.dawitf.akahidegn.ui.components.ContextPresets.FormSubmission.error,
                                    onRetry = {
                                        // Retry group creation
                                        createGroupInFirebase(destinationName, pickupLatitude, pickupLongitude)
                                    }
                                )

                                // After animation, return to main screen
                                kotlinx.coroutines.delay(5000)
                                initializeMainScreen()
                            }

                            // Display animations
                            com.dawitf.akahidegn.ui.components.AnimatedNotificationList(
                                notifications = notifications,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
    }
    
    private fun joinGroupInFirebase(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupId = group.groupId ?: return
        
        Log.d("JOIN_GROUP", "Starting to join group: $groupId, user: $currentUserId")
        
        // Check if group is expired (older than 30 minutes)
        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        if (group.timestamp != null && group.timestamp!! < thirtyMinutesAgo) {
            Log.d("JOIN_GROUP", "Group has expired: ${group.timestamp} < $thirtyMinutesAgo")
            onComplete(false, "Group has expired")
            return
        }
        
        // Check if group is full
        if (group.memberCount >= group.maxMembers) {
            Log.d("JOIN_GROUP", "Group is full: ${group.memberCount}/${group.maxMembers}")
            onComplete(false, "Group is full")
            return
        }
        
        // Check if user is already a member
        if (group.members.containsKey(currentUserId) && group.members[currentUserId] == true) {
            Log.d("JOIN_GROUP", "User is already a member")
            onComplete(false, "You are already a member of this group")
            return
        }
        
        // Using transaction for atomic operations to ensure consistency
        val groupRef = groupsRef.child(groupId)
        
        // Get user profile data
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatar = sharedPreferences.getString("user_avatar", "avatar_1") ?: "avatar_1"
        val userDisplayName = userName ?: "User"
        
        // Use a single transaction to update all data atomically
        groupRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                try {
                    // Get current data as a map to avoid deserialization issues
                    @Suppress("UNCHECKED_CAST")
                    val currentData = mutableData.getValue() as? Map<String, Any?> ?: return com.google.firebase.database.Transaction.abort()
                    
                    // Get the current member count
                    val currentMemberCount = (currentData["memberCount"] as? Long)?.toInt() ?: 0
                    val maxMembers = (currentData["maxMembers"] as? Long)?.toInt() ?: 4
                    
                    // Verify that the group isn't full
                    if (currentMemberCount >= maxMembers) {
                        Log.d("JOIN_GROUP", "Group is full: $currentMemberCount/$maxMembers")
                        return com.google.firebase.database.Transaction.abort()
                    }
                    
                    // Check timestamp for expiry
                    val groupTimestamp = currentData["timestamp"] as? Long ?: currentData["createdAt"] as? Long
                    if (groupTimestamp != null && groupTimestamp < thirtyMinutesAgo) {
                        Log.d("JOIN_GROUP", "Group has expired: $groupTimestamp < $thirtyMinutesAgo")
                        return com.google.firebase.database.Transaction.abort()
                    }
                    
                    // Get members list safely
                    @Suppress("UNCHECKED_CAST")
                    val members = currentData["members"] as? Map<String, Any?> ?: HashMap<String, Any?>()
                    if (members.containsKey(currentUserId)) {
                        Log.d("JOIN_GROUP", "User is already a member")
                        return com.google.firebase.database.Transaction.abort()
                    }
                    
                    // Create updated data map
                    val updatedData = HashMap<String, Any?>(currentData)
                    
                    // Update member count
                    updatedData["memberCount"] = currentMemberCount + 1
                    
                    // Add user to members
                    val updatedMembers = HashMap<String, Any?>(members)
                    updatedMembers[currentUserId] = true
                    updatedData["members"] = updatedMembers
                    
                    // Add member details
                    @Suppress("UNCHECKED_CAST")
                    val memberDetails = currentData["memberDetails"] as? Map<String, Any?> ?: HashMap<String, Any?>()
                    val updatedMemberDetails = HashMap<String, Any?>(memberDetails)
                    
                    updatedMemberDetails[currentUserId] = mapOf(
                        "name" to userDisplayName,
                        "phone" to userPhone,
                        "avatar" to userAvatar,
                        "joinedAt" to System.currentTimeMillis()
                    )
                    updatedData["memberDetails"] = updatedMemberDetails
                    
                    // Update the data in Firebase
                    mutableData.value = updatedData
                    Log.d("JOIN_GROUP", "Transaction data prepared: memberCount=${updatedData["memberCount"]}, added user=$currentUserId")
                    return com.google.firebase.database.Transaction.success(mutableData)
                } catch (e: Exception) {
                    Log.e("JOIN_GROUP", "Error in transaction", e)
                    return com.google.firebase.database.Transaction.abort()
                }
            }
            
            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                currentData: com.google.firebase.database.DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("JOIN_GROUP", "Transaction failed", error.toException())
                    onComplete(false, "Failed to join group: ${error.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to join group: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (!committed) {
                    // Transaction was aborted, check why and provide specific message
                    val reason = when {
                        currentData?.child("memberCount")?.getValue(Int::class.java) ?: 0 >= group.maxMembers -> "Group is full"
                        currentData?.child("members")?.child(currentUserId)?.exists() == true -> "You are already a member"
                        else -> "Unable to join group"
                    }
                    
                    Log.d("JOIN_GROUP", "Transaction aborted: $reason")
                    onComplete(false, reason)
                    Toast.makeText(
                        this@MainActivity,
                        reason,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Success - Enhanced with animation system
                    Log.d("JOIN_GROUP", "Successfully joined group $groupId")
                    onComplete(true, null)
                    refreshGroups()

                    // Use animation system instead of basic Toast
                    runOnUiThread {
                        setContent {
                            AkahidegnTheme {
                                val animationViewModel: com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                                val notifications by animationViewModel.notifications.collectAsState()

                                // Show success animation
                                LaunchedEffect(Unit) {
                                    animationViewModel.showSuccess(
                                        title = "ወደ ቡድን ገብተዋል!",
                                        subtitle = "በተሳካ ሁኔታ ወደ '${group.destinationName}' ቡድን ገብተዋል።",
                                        preset = com.dawitf.akahidegn.ui.components.ContextPresets.FormSubmission.success
                                    )

                                    // After animation, return to main screen
                                    kotlinx.coroutines.delay(3000)
                                    initializeMainScreen()
                                }

                                // Display animations
                                com.dawitf.akahidegn.ui.components.AnimatedNotificationList(
                                    notifications = notifications,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        })
    }
    
    private fun checkUserProfile() {
        // Check if user has a name stored
        userName = sharedPreferences.getString("user_name", null)
        
        if (userName.isNullOrBlank()) {
            // First time user - show name registration dialog
            showUserRegistrationDialog()
        } else {
            // User already registered, continue with normal flow
            Log.d("USER_PROFILE", "Welcome back, $userName")
            initializeMainScreen()
        }
    }
    
    private fun showUserRegistrationDialog() {
        // Create a dialog with custom layout for name, phone, and avatar
        val context = this
        var selectedAvatar = "avatar_1" // Default avatar
        var nameInput = ""
        var phoneInput = ""
        
        setContent {
            AkahidegnTheme {
                // Use the safe registration dialog with emoji avatars
                com.dawitf.akahidegn.ui.components.SafeUserRegistrationDialog(
                    onComplete = { name, phone, _ -> // Ignore avatar
                        if (name.isNotEmpty() && phone.isNotEmpty()) {
                            saveUserProfile(name, phone)
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { /* Cannot dismiss - required */ }
                )
            }
        }
    }
    
    private fun saveUserProfile(name: String, phone: String) {
        Log.d("USER_PROFILE", "Attempting to save user profile for: $name")
        userName = name
        sharedPreferences.edit()
            .putString("user_name", name)
            .putString("user_phone", phone)
            .putLong("user_registration_time", System.currentTimeMillis())
            .apply()
        
        // Also save to Firebase for potential future use
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            val userRef = database.reference.child("users").child(currentUserId)
            val userMap = mapOf(
                "name" to name,
                "phone" to phone,
                "registrationTime" to System.currentTimeMillis(),
                "lastActive" to System.currentTimeMillis()
            )
            
            userRef.setValue(userMap)
                .addOnSuccessListener {
                    Log.d("USER_PROFILE", "User profile saved successfully to Firebase for: $name")
                    Log.d("USER_PROFILE", "User profile saved: $name")
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    initializeMainScreen()
                }
                .addOnFailureListener { e ->
                    Log.e("USER_PROFILE", "Failed to save user profile to Firebase", e)
                    // Still proceed with local storage
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    initializeMainScreen()
                }
        } else {
            initializeMainScreen()
        }
    }
    
    private fun initializeMainScreen() {
        // Initialize ViewModel with Firebase references
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            mainViewModel.initializeFirebase(groupsRef, currentUserId)

            // Ensure MainViewModel has an initial location for filtering
            val initialLocation = Location("manual").apply {
                latitude = 9.005401
                longitude = 38.763611
            }
            mainViewModel.updateLocation(initialLocation)

            // Start monitoring groups that the user is a member of for real-time notifications
            groupEventMonitorService.startMonitoringUserGroups(groupsRef)

            Log.d("MAIN_SCREEN", "ViewModel and group monitoring initialized with Firebase")
        }
        
        setContent {
            AkahidegnTheme {
                MainScreenContent()
            }
        }
    }
    
    @Composable
    private fun MainScreenContent() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Observe ViewModel state properly
            val groups by mainViewModel.groups.collectAsState()
            val vmIsLoading by mainViewModel.isLoadingGroups.collectAsState()
            Log.d("MAIN_SCREEN_CONTENT", "Groups received: ${groups.size}, Is Loading: $vmIsLoading")
            
            // Initial load of groups is now handled by updateLocation in initializeMainScreen
            // LaunchedEffect(Unit) {
            //     refreshGroups()
            // }
            
            // State for search
            var searchQuery by remember { mutableStateOf("") }
            
            
            MainScreen(
                groups = filterExpiredGroups(groups),
                userLocation = userLocation,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery = query },
                
                
                onGroupClick = { group ->
                    // Group click callback - try to join group
                    handleGroupSelection(group)
                },
                isLoading = vmIsLoading,
                onRefreshGroups = {
                    // Refresh callback
                    refreshGroups()
                },
                onCreateGroup = {
                    // Create group callback - show destination dialog
                    showCreateGroupDialog()
                },
                onNavigateToSettings = {
                    // Navigate to settings - show settings screen
                    showSettingsScreen()
                },
                
                
                onNavigateToNotifications = {
                    // Navigate to notifications - show notification screen
                    showNotificationScreen()
                },
                
            )

            // Removed success dialog and group members dialog from here
        }
    }
    
    private fun showGroupMembersDialog(group: Group) {
        // Convert group member details to GroupMember objects
        val currentUserId = auth.currentUser?.uid ?: ""
        val members = group.memberDetails.map { (userId, memberInfo) ->
            GroupMember(
                id = userId,
                name = memberInfo.name,
                phone = memberInfo.phone,
                avatar = memberInfo.avatar,
                isCreator = userId == group.creatorId
            )
        }
        
        setContent {
            AkahidegnTheme {
                GroupMembersDialog(
                    group = group,
                    members = members,
                    currentUserId = auth.currentUser?.uid ?: "",
                    onDismiss = { 
                        // Return to main screen
                        initializeMainScreen()
                    }
                )
            }
        }
    }
    
    private fun setupLocationUpdates() {
        // Hardcode user location for debugging purposes
        userLocation = Location("manual").apply {
            latitude = 9.005401
            longitude = 38.763611
        }
        Log.d("LOCATION_UPDATE", "User location hardcoded to: ${userLocation?.latitude}, ${userLocation?.longitude}")
        // No need to request location updates from system when hardcoding
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
    }
    
    // Function to handle group creation from MainScreen
    private fun createGroup(group: Group) {
        Log.d("MainActivity", "Creating group: ${group.destinationName}")
        
        // Validate user authentication
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e("GROUP_CREATION", "User not authenticated")
            return
        }
        
        // Use existing logic from createGroupAfterAd but with the provided Group object
        val destinationName = group.destinationName ?: "Unknown Destination"
        
        // Get user location if available, otherwise use default location
        val pickupLatitude = userLocation?.latitude ?: 9.005401 // Default to Addis Ababa
        val pickupLongitude = userLocation?.longitude ?: 38.763611
        
        // Create the group in Firebase using existing function
        createGroupInFirebase(
            destinationName = destinationName,
            pickupLatitude = pickupLatitude,
            pickupLongitude = pickupLongitude
        )
    }

    // Helper functions for user information
    private fun getUserDisplayName(): String {
        return auth.currentUser?.displayName ?: "User"
    }
    
    private fun getUserPhoneNumber(): String {
        return auth.currentUser?.phoneNumber ?: ""
    }

    /**
     * Leave group functionality with notifications
     */
    private fun leaveGroup(group: Group) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupId = group.groupId ?: return

        Log.d("LEAVE_GROUP", "User $currentUserId leaving group $groupId")

        val groupRef = groupsRef.child(groupId)

        // Use transaction to safely remove user from group
        groupRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val currentData = mutableData.getValue() as? Map<String, Any?> ?: return com.google.firebase.database.Transaction.abort()

                    val currentMemberCount = (currentData["memberCount"] as? Long)?.toInt() ?: 0

                    // Remove user from members
                    @Suppress("UNCHECKED_CAST")
                    val members = currentData["members"] as? MutableMap<String, Any?> ?: return com.google.firebase.database.Transaction.abort()

                    if (!members.containsKey(currentUserId)) {
                        Log.d("LEAVE_GROUP", "User is not a member of this group")
                        return com.google.firebase.database.Transaction.abort()
                    }

                    members.remove(currentUserId)

                    // Remove from member details
                    @Suppress("UNCHECKED_CAST")
                    val memberDetails = currentData["memberDetails"] as? MutableMap<String, Any?> ?: mutableMapOf()
                    memberDetails.remove(currentUserId)

                    // Update data
                    val updatedData = HashMap<String, Any?>(currentData)
                    updatedData["memberCount"] = maxOf(0, currentMemberCount - 1)
                    updatedData["members"] = members
                    updatedData["memberDetails"] = memberDetails

                    // If this was the creator and group becomes empty, mark for deletion
                    val creatorId = currentData["creatorId"] as? String
                    if (creatorId == currentUserId || updatedData["memberCount"] == 0) {
                        // Mark group as disbanded
                        updatedData["disbanded"] = true
                        updatedData["disbandedAt"] = System.currentTimeMillis()
                    }

                    mutableData.value = updatedData
                    return com.google.firebase.database.Transaction.success(mutableData)
                } catch (e: Exception) {
                    Log.e("LEAVE_GROUP", "Error in leave group transaction", e)
                    return com.google.firebase.database.Transaction.abort()
                }
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                currentData: com.google.firebase.database.DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("LEAVE_GROUP", "Failed to leave group", error.toException())
                    Toast.makeText(this@MainActivity, "Failed to leave group: ${error.message}", Toast.LENGTH_SHORT).show()
                } else if (!committed) {
                    Log.d("LEAVE_GROUP", "Leave group transaction was aborted")
                    Toast.makeText(this@MainActivity, "Unable to leave group", Toast.LENGTH_SHORT).show()
                } else {
                    // Success
                    Log.d("LEAVE_GROUP", "Successfully left group")

                    // Send notifications to remaining members
                    val currentUserName = userName ?: "User"
                    notificationService.showUserLeftNotification(group, currentUserName)

                    // Check if group was disbanded
                    val memberCount = currentData?.child("memberCount")?.getValue(Int::class.java) ?: 0
                    if (memberCount == 0) {
                        notificationService.showGroupDisbandedNotification(group)
                    } else if (memberCount >= group.maxMembers) {
                        notificationService.showGroupFullNotification(group)
                    }

                    Toast.makeText(this@MainActivity, "Left group: ${group.destinationName}", Toast.LENGTH_SHORT).show()
                    refreshGroups()
                }
            }
        })
    }

    /**
     * Show settings screen
     */
    private fun showSettingsScreen() {
        setContent {
            AkahidegnTheme {
                com.dawitf.akahidegn.ui.screens.SettingsScreen(
                    onNavigateBack = {
                        // Return to main screen
                        initializeMainScreen()
                    }
                )
            }
        }
    }

    /**
     * Show notification screen
     */
    private fun showNotificationScreen() {
        setContent {
            AkahidegnTheme {
                com.dawitf.akahidegn.ui.screens.NotificationScreen(
                    onNavigateBack = {
                        // Return to main screen
                        initializeMainScreen()
                    }
                )
            }
        }
    }
}
