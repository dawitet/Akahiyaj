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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.ui.screens.MainScreen
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import com.dawitf.akahidegn.ui.components.UserRegistrationDialog
import com.dawitf.akahidegn.ui.components.GroupMembersDialog
import com.dawitf.akahidegn.ui.components.GroupMember
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ViewModel instance for the activity
    private val mainViewModel: MainViewModel by viewModels()
    
    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val currentGroups = mutableListOf<Group>()
    private var currentDestination = ""
    
    // Ad variables
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    
    // User location
    private var userLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private var lastLocationUpdate = 0L
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            userLocation = location
            
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
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
        
        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // Request location permissions and start location updates
        setupLocationUpdates()
        
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {
            Log.d("ADS", "Mobile Ads SDK initialized")
        }
        
        // Initialize Firebase components
        database = Firebase.database
        groupsRef = database.reference.child("groups")
        auth = Firebase.auth
        
        // Load ads
        loadRewardedAd()
        loadInterstitialAd()
        
        // Ensure the user is signed in anonymously
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "signInAnonymously:success")
                    checkUserProfile()
                } else {
                    Log.w("AUTH", "signInAnonymously:failure", task.exception)
                }
            }
        } else {
            checkUserProfile()
        }
        
        // Removed early setContent call - let proper initialization flow handle UI
    }
    
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Rewarded ad failed to load: ${adError.message}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("ADS", "Rewarded ad loaded")
                rewardedAd = ad
            }
        })
    }
    
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Interstitial ad failed to load: ${adError.message}")
                interstitialAd = null
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("ADS", "Interstitial ad loaded")
                interstitialAd = ad
            }
        })
    }
    
    private fun refreshGroups() {
        // Use the ViewModel to refresh groups (shows all active groups)
        mainViewModel.refreshGroups()
    }
    
    
    private fun filterExpiredGroups(groups: List<Group>): List<Group> {
        // Temporarily disable expiration filter for testing - include all groups
        return groups
        // TODO: Re-enable expiration filter with proper timestamp logic later
        // val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        // return groups.filter { it.timestamp == null || it.timestamp!! > thirtyMinutesAgo }
    }
    
    private fun showCreateGroupDialog() {
        // Create destination input dialog with enhanced Amharic UI
        val editText = EditText(this).apply {
            hint = "ወደየት ነው የምትሄደው? (ምሳሌ: ቦሌ፣ መገናኛ፣ ፒያሳ)"
            setPadding(60, 40, 60, 40)
            textSize = 16f
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
            Toast.makeText(this, "Ad not ready, creating group anyway", Toast.LENGTH_SHORT).show()
            createGroupAfterAd(destination)
        }
    }
    
    private fun createGroupAfterAd(destinationName: String) {
        // Use current location exactly, or default Addis Ababa coordinates without random offset
        val lat = userLocation?.latitude ?: 8.9806
        val lng = userLocation?.longitude ?: 38.7578
        
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
            Toast.makeText(this, "Ad not ready, joining group anyway", Toast.LENGTH_SHORT).show()
            joinGroupAfterAd(group)
        }
    }
    
    private fun joinGroupAfterAd(group: Group) {
        // When a group is selected, try to join it
        joinGroupInFirebase(group) { success, message ->
            if (success) {
                Toast.makeText(
                    this,
                    "Successfully joined group to ${group.destinationName}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Show group members view after successful join
                showGroupMembersDialog(group)
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
    
    private fun createGroupInFirebase(
        destinationName: String,
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        
        // Create a unique group identifier combining destination, time, and user name
        val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeString = timeFormatter.format(java.util.Date(timestamp))
        
        // Create a more descriptive group name with time and creator differentiator
        val creatorName = userName ?: "User"
        val uniqueDestinationName = "$destinationName ($timeString) - by $creatorName"
        
        // Get user profile data
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatar = sharedPreferences.getString("user_avatar", "avatar_1") ?: "avatar_1"
        
        // Get a local avatar for the group
        val newGroup = Group(
            creatorId = currentUserId,
            creatorName = userName ?: "User",
            destinationName = uniqueDestinationName, // Use time-differentiated name
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
        Log.d("FIREBASE", "Creating group: ${newGroup.destinationName} with ID: ${newGroup.groupId}")
        
        newGroupRef.setValue(newGroup.toMap())
            .addOnSuccessListener {
                Log.d("FIREBASE", "Group created successfully: ${newGroup.destinationName}")
                refreshGroups() // Refresh groups to immediately show the new group
                Toast.makeText(
                    this@MainActivity,
                    "Group created: ${newGroup.destinationName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error creating group: ${newGroup.destinationName}", e)
                Toast.makeText(
                    this@MainActivity,
                    "Failed to create group: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    
    private fun joinGroupInFirebase(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupId = group.groupId ?: return
        
        // Check if group is expired (older than 30 minutes)
        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        if (group.timestamp != null && group.timestamp!! < thirtyMinutesAgo) {
            onComplete(false, "Group has expired")
            return
        }
        
        // Check if group is full
        if (group.memberCount >= group.maxMembers) {
            onComplete(false, "Group is full")
            return
        }
        
        // Check if user is already a member
        if (group.members.containsKey(currentUserId) && group.members[currentUserId] == true) {
            onComplete(false, "You are already a member of this group")
            return
        }
        
        // Using transaction for atomic operations to prevent race conditions
        val groupRef = groupsRef.child(groupId)
        
        // Get user profile data
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatar = sharedPreferences.getString("user_avatar", "avatar_1") ?: "avatar_1"
        
        // First update the members list and member details
        val updates = HashMap<String, Any>()
        updates["members/$currentUserId"] = true
        updates["memberDetails/$currentUserId/name"] = userName ?: "User"
        updates["memberDetails/$currentUserId/phone"] = userPhone
        updates["memberDetails/$currentUserId/avatar"] = userAvatar
        updates["memberDetails/$currentUserId/joinedAt"] = System.currentTimeMillis()
        
        groupRef.updateChildren(updates)
            .addOnSuccessListener {
                // Then update member count atomically
                groupRef.child("memberCount").runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        val currentCount = mutableData.getValue(Int::class.java) ?: 0
                        mutableData.value = currentCount + 1
                        return com.google.firebase.database.Transaction.success(mutableData)
                    }
                    
                    override fun onComplete(
                        error: com.google.firebase.database.DatabaseError?,
                        committed: Boolean,
                        currentData: com.google.firebase.database.DataSnapshot?
                    ) {
                        if (error != null) {
                            Log.e("FIREBASE", "Error updating member count", error.toException())
                            onComplete(false, error.message)
                        } else {
                            onComplete(true, null)
                            refreshGroups()
                            Toast.makeText(
                                this@MainActivity,
                                "Successfully joined group: ${group.destinationName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error joining group", e)
                onComplete(false, "Permission denied: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Failed to join group: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                UserRegistrationDialog(
                    onComplete = { name, phone, avatar ->
                        if (name.isNotEmpty() && phone.isNotEmpty()) {
                            saveUserProfile(name, phone, avatar)
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { /* Cannot dismiss - required */ }
                )
            }
        }
    }
    
    private fun saveUserProfile(name: String, phone: String, avatar: String) {
        userName = name
        sharedPreferences.edit()
            .putString("user_name", name)
            .putString("user_phone", phone)
            .putString("user_avatar", avatar)
            .putLong("user_registration_time", System.currentTimeMillis())
            .apply()
        
        // Also save to Firebase for potential future use
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            val userRef = database.reference.child("users").child(currentUserId)
            val userMap = mapOf(
                "name" to name,
                "phone" to phone,
                "avatar" to avatar,
                "registrationTime" to System.currentTimeMillis(),
                "lastActive" to System.currentTimeMillis()
            )
            
            userRef.setValue(userMap)
                .addOnSuccessListener {
                    Log.d("USER_PROFILE", "User profile saved: $name")
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    initializeMainScreen()
                }
                .addOnFailureListener { e ->
                    Log.e("USER_PROFILE", "Failed to save user profile", e)
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
            Log.d("MAIN_SCREEN", "ViewModel initialized with Firebase")
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
            
            // Initial load of groups
            LaunchedEffect(Unit) {
                refreshGroups()
            }
            
            // Create empty filters of the correct type
            val emptyFilters = com.dawitf.akahidegn.ui.components.SearchFilters()
            
            // State for search and filters
            var searchQuery by remember { mutableStateOf("") }
            var selectedFilters by remember { mutableStateOf(emptyFilters) }
            
            MainScreen(
                groups = filterExpiredGroups(groups),
                userLocation = userLocation,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery = query },
                selectedFilters = selectedFilters,
                onFiltersChange = { filters -> selectedFilters = filters },
                onGroupClick = { group ->
                    // Group click callback - show members dialog
                    showGroupMembersDialog(group)
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
                onNavigateToProfile = {
                    // Navigate to profile callback - placeholder for now
                    Log.d("MainActivity", "Navigate to profile clicked")
                },
                onNavigateToNotifications = {
                    // Navigate to notifications callback - placeholder for now
                    Log.d("MainActivity", "Navigate to notifications clicked")
                },
                onNavigateToBookmarks = {
                    // Navigate to bookmarks callback - placeholder for now
                    Log.d("MainActivity", "Navigate to bookmarks clicked")
                },
                onNavigateToHistory = {
                    // Navigate to history callback - placeholder for now
                    Log.d("MainActivity", "Navigate to history clicked")
                },
                onNavigateToSettings = {
                    // Navigate to settings callback - placeholder for now
                    Log.d("MainActivity", "Navigate to settings clicked")
                }
            )
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
        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, request location updates (less frequent to prevent ANR)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                30000, // 30 seconds (increased from 5 seconds)
                50f, // 50 meters (increased from 10 meters)
                locationListener
            )
            Log.d("LOCATION", "Location updates started")
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, restart location updates
                setupLocationUpdates()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Location permission is required for this feature", Toast.LENGTH_SHORT).show()
            }
        }
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
}
