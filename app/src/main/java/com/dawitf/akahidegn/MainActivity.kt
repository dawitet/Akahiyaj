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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.dawitf.akahidegn.ui.navigation.Screen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.dawitf.akahidegn.ui.theme.HomeColorScheme
import com.dawitf.akahidegn.ui.theme.ActiveGroupsColorScheme
import com.dawitf.akahidegn.ui.theme.SettingsColorScheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dawitf.akahidegn.ui.screens.ActiveGroupsScreen
import com.dawitf.akahidegn.ui.screens.SettingsScreen
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
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<android.content.Intent>

    private val currentGroups = mutableListOf<Group>()
    private var selectedGroupForDialog by mutableStateOf<Group?>(null)

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    private var userLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private var lastLocationUpdate = 0L
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            userLocation = location
            val currentTime = System.currentTimeMillis()
            val updateInterval = if (isGroupCreator()) 30000L else 60000L
            if (currentTime - lastLocationUpdate > updateInterval) {
                lastLocationUpdate = currentTime
                mainViewModel.updateLocation(location)
                Log.d("LOCATION", "Location updated: ${location.latitude}, ${location.longitude}")
            }
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun isGroupCreator(): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        return currentGroups.any { group ->
            group.creatorId == currentUserId &&
            (group.timestamp ?: 0L) > (System.currentTimeMillis() - 30 * 60 * 1000L)
        }
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var userName: String? = null // This will be updated from Google Sign In or SharedPreferences

    companion object {
        val THEME_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupLocationUpdates()

        MobileAds.initialize(this) { Log.d("ADS", "Mobile Ads SDK initialized") }

        database = Firebase.database
        groupsRef = database.reference.child("groups")
        auth = Firebase.auth

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Make sure this string resource exists
            .requestEmail()
            .requestProfile() // Request profile for photo URL
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d("AUTH", "Google Sign-In successful, proceeding with Firebase Auth. Photo URL: ${account?.photoUrl}")
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    Log.w("AUTH", "Google Sign-In failed", e)
                    Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_LONG).show()
                }
            } else {
                 Log.w("AUTH", "Google Sign-In cancelled or failed. Result code: ${result.resultCode}")
                 Toast.makeText(this, "Google Sign-In was cancelled or failed.", Toast.LENGTH_LONG).show()
            }
        }

        loadRewardedAd()
        loadInterstitialAd()

        if (auth.currentUser == null) {
            Log.d("AUTH", "No current user, starting Google Sign-In flow")
            startGoogleSignInFlow()
        } else {
            Log.d("AUTH", "User already signed in, checking profile")
            userName = auth.currentUser?.displayName ?: sharedPreferences.getString("user_name", null)
            checkUserProfile()
        }
    }

    private fun startGoogleSignInFlow() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "Firebase Auth with Google:success")
                    val firebaseUser = auth.currentUser
                    userName = firebaseUser?.displayName // Get name from Google account
                    val storedName = sharedPreferences.getString("user_name", null)
                    if (userName != null && userName != storedName) {
                        sharedPreferences.edit().putString("user_name", userName).apply()
                    }
                    // Also store/update photo URL in SharedPreferences if needed, though primarily used for registration dialog
                    val photoUrl = firebaseUser?.photoUrl?.toString()
                    if (photoUrl != null) {
                        sharedPreferences.edit().putString("user_avatar_url", photoUrl).apply()
                    }
                    checkUserProfile()
                } else {
                    Log.w("AUTH", "Firebase Auth with Google:failure", task.exception)
                    Toast.makeText(this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadRewardedAd() {
        if (!BuildConfig.ADS_ENABLED || BuildConfig.ADMOB_REWARDED_ID.isEmpty()) {
            Log.d("ADS", "Ads disabled - REWARDED_ID: '${BuildConfig.ADMOB_REWARDED_ID}'")
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
            Log.d("ADS", "Ads disabled - INTERSTITIAL_ID: '${BuildConfig.ADMOB_INTERSTITIAL_ID}'")
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

    private fun refreshGroups() {
        mainViewModel.refreshGroups()
        logAllGroups()
    }

    private fun logAllGroups() {
        groupsRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val groupCount = dataSnapshot.childrenCount
                Log.d("FIREBASE_DEBUG", "Total groups: $groupCount")
                dataSnapshot.children.forEach { groupSnapshot ->
                    val group = groupSnapshot.getValue(Group::class.java)
                    group?.let {
                        Log.d("FIREBASE_DEBUG", "Group: ${it.destinationName}, Members: ${it.memberCount}/${it.maxMembers}, Creator: ${it.creatorName}")
                    }
                }
            } else {
                Log.d("FIREBASE_DEBUG", "No groups found")
            }
        }.addOnFailureListener { Log.e("FIREBASE_DEBUG", "Failed to fetch groups", it) }
    }

    private fun showCreateGroupDialog() {
        val editText = EditText(this).apply {
            hint = "ወደየት ነው የምትሄደው? (ምሳሌ: ቦሌ፣ መገናኛ፣ ፒያሳ)"
            setPadding(60, 40, 60, 40)
            textSize = 16f
        }
        AlertDialog.Builder(this)
            .setTitle("🚗 አዲስ ቡድን ፍጠር")
            .setMessage("የመሳፈሪያ ቡድን ይፍጠሩ እና ከሌሎች ሰዎች ጋር ተጋሩ!")
            .setView(editText)
            .setPositiveButton("✨ ቡድን ፍጠር") { _, _ ->
                val destination = editText.text.toString().trim()
                if (destination.isNotEmpty()) {
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
                createGroupAfterAd(destination)
                loadRewardedAd()
            }
        } else {
            Log.d("ADS", "Rewarded ad not ready")
            createGroupAfterAd(destination)
        }
    }

    private fun createGroupAfterAd(destination: String) {
        val lat = userLocation?.latitude ?: 8.9806
        val lng = userLocation?.longitude ?: 38.7578
        createGroupInFirebase(toDestination = destination, pickupLatitude = lat, pickupLongitude = lng)
    }

    private fun handleGroupSelection(group: Group) {
        if (interstitialAd != null) {
            interstitialAd!!.show(this)
            interstitialAd!!.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    joinGroupAfterAd(group)
                    loadInterstitialAd()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    joinGroupAfterAd(group)
                }
            }
        } else {
            joinGroupAfterAd(group)
        }
    }

    private fun joinGroupAfterAd(group: Group) {
        joinGroupInFirebase(group) { success, message ->
            if (success) {
                Toast.makeText(this, "Joined group to ${group.destinationName}", Toast.LENGTH_SHORT).show()
                showGroupMembersDialog(group)
                refreshGroups()
            } else {
                Toast.makeText(this, "Could not join: ${message ?: "Error"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createGroupInFirebase(
        toDestination: String,
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeString = timeFormatter.format(java.util.Date(timestamp))
        val creatorDisplayName = auth.currentUser?.displayName ?: userName ?: "User"
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        // Use Google photo URL if available, otherwise fallback to existing SharedPreferences or default
        val userAvatarUrl = auth.currentUser?.photoUrl?.toString() ?: sharedPreferences.getString("user_avatar_url", null) ?: sharedPreferences.getString("user_avatar", "avatar_1")


        val uniqueDestinationName = "$toDestination ($timeString) - by $creatorDisplayName"

        val newGroup = Group(
            creatorId = currentUserId,
            creatorName = creatorDisplayName,
            destinationName = uniqueDestinationName,
            originalDestination = toDestination,
            timestamp = timestamp,
            maxMembers = 4,
            memberCount = 1,
            imageUrl = null, // Or potentially userAvatarUrl if groups have images
        )
        newGroup.members[currentUserId] = true
        newGroup.memberDetails[currentUserId] = MemberInfo(
            name = creatorDisplayName,
            phone = userPhone,
            avatar = userAvatarUrl, // Use the Google Photo URL or fallback
            joinedAt = timestamp
        )
        val newGroupRef = groupsRef.push()
        newGroup.groupId = newGroupRef.key
        newGroupRef.setValue(newGroup.toMap())
            .addOnSuccessListener {
                Log.d("FIREBASE", "Group created: ${newGroup.destinationName}")
                refreshGroups()
                Toast.makeText(this, "Group created: ${newGroup.destinationName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error creating group: ${newGroup.destinationName}", e)
                Toast.makeText(this, "Failed to create group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinGroupInFirebase(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupId = group.groupId ?: return

        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        if (group.timestamp != null && group.timestamp!! < thirtyMinutesAgo) {
            onComplete(false, "Group has expired")
            return
        }
        if (group.memberCount >= group.maxMembers) {
            onComplete(false, "Group is full")
            return
        }
        if (group.members.containsKey(currentUserId) && group.members[currentUserId] == true) {
            onComplete(false, "Already a member")
            return
        }

        val groupRef = groupsRef.child(groupId)
        val joinerDisplayName = auth.currentUser?.displayName ?: userName ?: "User"
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
         // Use Google photo URL if available, otherwise fallback to existing SharedPreferences or default
        val userAvatarUrl = auth.currentUser?.photoUrl?.toString() ?: sharedPreferences.getString("user_avatar_url", null) ?: sharedPreferences.getString("user_avatar", "avatar_1")


        val updates = HashMap<String, Any>()
        updates["members/$currentUserId"] = true
        updates["memberDetails/$currentUserId/name"] = joinerDisplayName
        updates["memberDetails/$currentUserId/phone"] = userPhone
        updates["memberDetails/$currentUserId/avatar"] = userAvatarUrl // Use the Google Photo URL or fallback
        updates["memberDetails/$currentUserId/joinedAt"] = System.currentTimeMillis()

        groupRef.updateChildren(updates)
            .addOnSuccessListener {
                groupRef.child("memberCount").runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        val currentCount = mutableData.getValue(Int::class.java) ?: 0
                        mutableData.value = currentCount + 1
                        return com.google.firebase.database.Transaction.success(mutableData)
                    }
                    override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, currentData: com.google.firebase.database.DataSnapshot?) {
                        if (error != null) {
                            onComplete(false, error.message)
                        } else {
                            onComplete(true, null)
                            refreshGroups()
                        }
                    }
                })
            }
            .addOnFailureListener { e -> onComplete(false, "Permission denied: ${e.message}") }
    }

    private fun checkUserProfile() {
        val userPhone = sharedPreferences.getString("user_phone", null)
        // userName is already updated by Google Sign-In or from SharedPreferences by this point
        // photoUrl is also available via auth.currentUser?.photoUrl

        if (userName.isNullOrBlank() || userPhone.isNullOrBlank()) {
            showUserRegistrationDialog(
                googleName = auth.currentUser?.displayName,
                googlePhotoUrl = auth.currentUser?.photoUrl?.toString()
            )
        } else {
            Log.d("USER_PROFILE", "Welcome back, $userName with phone: $userPhone. Photo URL: ${auth.currentUser?.photoUrl}")
            initializeMainScreen()
        }
    }

    private fun showUserRegistrationDialog(googleName: String? = null, googlePhotoUrl: String? = null) {
        setContent { // This might cause issues if called multiple times. Consider a different approach for dialogs.
            AkahidegnTheme {
                UserRegistrationDialog(
                    initialName = googleName ?: "",
                    initialPhotoUrl = googlePhotoUrl, // Pass the photo URL
                    onComplete = { name, phone -> // Avatar is no longer passed from dialog
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            // Pass Google photo URL directly to saveUserProfile
                            saveUserProfile(name, phone, auth.currentUser?.photoUrl?.toString())
                        } else {
                            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = {
                        Toast.makeText(this, "Registration is required to continue.", Toast.LENGTH_LONG).show()
                        startGoogleSignInFlow()
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
        if (avatarUrl != null) { // Save Google Photo URL if available
            editor.putString("user_avatar_url", avatarUrl) // Store Google photo URL
            editor.remove("user_avatar") // Remove old local avatar key if it exists
        } else {
            // Fallback or keep existing logic if no Google photo URL (though Google usually provides a default)
            editor.putString("user_avatar", sharedPreferences.getString("user_avatar", "avatar_1"))
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
            if (avatarUrl != null) {
                userMap["avatarUrl"] = avatarUrl // Store Google photo URL in Firebase
            } else {
                userMap["avatar"] = sharedPreferences.getString("user_avatar", "avatar_1")!! // Fallback
            }

            userRef.setValue(userMap)
                .addOnSuccessListener {
                    Log.d("USER_PROFILE", "User profile saved: $name. Avatar URL: $avatarUrl")
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    initializeMainScreen()
                }
                .addOnFailureListener { e ->
                    Log.e("USER_PROFILE", "Failed to save user profile to Firebase", e)
                    Toast.makeText(this, "Welcome, $name! (Profile saved locally)", Toast.LENGTH_SHORT).show()
                    initializeMainScreen()
                }
        } else {
            initializeMainScreen() // Should ideally not happen if auth flow is correct
        }
    }

    private fun initializeMainScreen() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            mainViewModel.initializeFirebase(groupsRef, currentUserId)
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
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(Screen.Main, Screen.ActiveGroups, Screen.Settings)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
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
                                onRefreshGroups = { refreshGroups() },
                                onCreateGroup = { showCreateGroupDialog() }
                            )
                        }
                        composable(Screen.ActiveGroups.route) { ActiveGroupsScreen() }
                        composable(Screen.Settings.route) {
                            SettingsScreen(onSignOut = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    googleSignInClient.signOut().await() // Sign out from Google
                                    auth.signOut() // Sign out from Firebase
                                    Toast.makeText(this@MainActivity, "Signed out", Toast.LENGTH_SHORT).show()
                                    startGoogleSignInFlow() // Restart sign-in process
                                }
                            })
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
            val groups by mainViewModel.groups.collectAsState()
            val vmIsLoading by mainViewModel.isLoadingGroups.collectAsState()
            LaunchedEffect(Unit) { onRefreshGroups() }
            val emptyFilters = SearchFilters()
            var searchQuery by remember { mutableStateOf("") }
            var selectedFilters by remember { mutableStateOf(emptyFilters) }

            MainScreen(
                groups = groups,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery = query },
                selectedFilters = selectedFilters,
                onFiltersChange = { filters -> selectedFilters = filters },
                onGroupClick = onGroupClick,
                isLoading = vmIsLoading,
                onRefreshGroups = onRefreshGroups,
                onCreateGroup = onCreateGroup
            )

            selectedGroupForDialog?.let { group ->
                val currentUserId = auth.currentUser?.uid ?: ""
                val members = group.memberDetails.map { (userId, memberInfo) ->
                    GroupMember(
                        id = userId, name = memberInfo.name, phone = memberInfo.phone,
                        avatar = memberInfo.avatar, isCreator = userId == group.creatorId
                    )
                }
                GroupMembersDialog(
                    group = group, members = members, currentUserId = currentUserId,
                    onDismiss = { selectedGroupForDialog = null },
                    onLeaveGroup = { groupId, userId -> Log.d("MainActivity", "Leave group $groupId by $userId") }
                )
            }
        }
    }

    private fun showGroupMembersDialog(group: Group) {
        selectedGroupForDialog = group
    }

    private fun setupLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 50f, locationListener)
            Log.d("LOCATION", "Location updates started")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
    }
}
