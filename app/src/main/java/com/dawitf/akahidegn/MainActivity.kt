package com.dawitf.akahidegn

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {
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
        sharedPreferences = getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
        
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
        
        setContent {
            AkahidegnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Use a simple state for the UI without the ViewModel dependencies
                    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
                    var isRefreshing by remember { mutableStateOf(false) }
                    var searchQuery by remember { mutableStateOf("") }
                    
                    // Initial load of groups
                    LaunchedEffect(Unit) {
                        refreshGroups()
                    }
                    
                    // Create empty filters of the correct type
                    val emptyFilters = com.dawitf.akahidegn.ui.components.SearchFilters()
                    
                    MainScreen(
                        groups = filterExpiredGroups(currentGroups),
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedFilters = emptyFilters,
                        onFiltersChange = { /* Handler for filter changes */ },
                        onGroupClick = { group -> handleGroupSelection(group) },
                        isLoading = isRefreshing,
                        onRefreshGroups = { 
                            isRefreshing = true
                            refreshGroups()
                            isRefreshing = false
                        },
                        onCreateGroup = { showCreateGroupDialog() },
                        onNavigateToSettings = { /* Navigate to settings */ },
                        onNavigateToProfile = { /* Navigate to profile */ },
                        onNavigateToBookmarks = { /* Navigate to bookmarks */ },
                        onNavigateToNotifications = { /* Navigate to notifications */ },
                        onNavigateToHistory = { /* Navigate to history */ }
                    )
                }
            }
        }
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
        groupsRef.get().addOnSuccessListener { dataSnapshot ->
            val groups = mutableListOf<Group>()
            dataSnapshot.children.forEach { childSnapshot ->
                val group = childSnapshot.getValue(Group::class.java)
                group?.let {
                    it.groupId = childSnapshot.key
                    // Only add non-expired groups within 500 meters
                    val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
                    if (it.timestamp == null || it.timestamp!! > thirtyMinutesAgo) {
                        // Check if group is within 500 meters if user location is available
                        if (userLocation != null && it.pickupLat != null && it.pickupLng != null) {
                            val groupLocation = Location("group").apply {
                                latitude = it.pickupLat!!
                                longitude = it.pickupLng!!
                            }
                            val distance = userLocation!!.distanceTo(groupLocation)
                            if (distance <= 500) { // 500 meters
                                groups.add(it)
                            }
                        } else {
                            // If no location data, show all non-expired groups
                            groups.add(it)
                        }
                    }
                }
            }
            currentGroups.clear()
            currentGroups.addAll(groups)
        }.addOnFailureListener {
            Log.e("FIREBASE", "Error getting groups", it)
        }
    }
    
    private fun filterExpiredGroups(groups: List<Group>): List<Group> {
        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        return groups.filter { it.timestamp == null || it.timestamp!! > thirtyMinutesAgo }
    }
    
    private fun showCreateGroupDialog() {
        // Create destination input dialog
        val editText = EditText(this).apply {
            hint = "Enter destination (e.g., Bole, Megenagna, Piassa)"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Create New Group")
            .setMessage("Where are you going?")
            .setView(editText)
            .setPositiveButton("Create Group") { _, _ ->
                val destination = editText.text.toString().trim()
                if (destination.isNotEmpty()) {
                    // Show rewarded ad before creating group
                    showRewardedAdForGroupCreation(destination)
                } else {
                    Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
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
        // Use current location or default coordinates
        val lat = userLocation?.latitude ?: (8.9806 + (Math.random() * 0.1))
        val lng = userLocation?.longitude ?: (38.7578 + (Math.random() * 0.1))
        
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
            // Use a simple state for the UI without the ViewModel dependencies
            var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
            var isRefreshing by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }
            
            // Initial load of groups
            LaunchedEffect(Unit) {
                refreshGroups()
            }
            
            // Create empty filters of the correct type
            val emptyFilters = com.dawitf.akahidegn.ui.components.SearchFilters()
            
            MainScreen(
                groups = filterExpiredGroups(currentGroups),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedFilters = emptyFilters,
                onFiltersChange = { /* Handler for filter changes */ },
                onGroupClick = { group -> handleGroupSelection(group) },
                isLoading = isRefreshing,
                onRefreshGroups = { 
                    isRefreshing = true
                    refreshGroups()
                    isRefreshing = false
                },
                onCreateGroup = { showCreateGroupDialog() },
                onNavigateToSettings = { /* Navigate to settings */ },
                onNavigateToProfile = { /* Navigate to profile */ },
                onNavigateToBookmarks = { /* Navigate to bookmarks */ },
                onNavigateToNotifications = { /* Navigate to notifications */ },
                onNavigateToHistory = { /* Navigate to history */ }
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
                    onDismiss = { 
                        // Return to main screen
                        initializeMainScreen()
                    }
                )
            }
        }
    }
}
