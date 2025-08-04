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
// Removed: import androidx.activity.result.ActivityResultLauncher
// Removed: import androidx.activity.result.contract.ActivityResultContracts
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
import com.dawitf.akahidegn.ui.navigation.Screen
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
import kotlinx.coroutines.launch
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
// Removed: import com.google.android.gms.auth.api.signin.GoogleSignIn
// Removed: import com.google.android.gms.auth.api.signin.GoogleSignInAccount
// Removed: import com.google.android.gms.auth.api.signin.GoogleSignInClient
// Removed: import com.google.android.gms.auth.api.signin.GoogleSignInOptions
// Removed: import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint

// Credential Manager Imports
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException // Catch this base class
import androidx.credentials.exceptions.NoCredentialException // Specific exception for no credentials
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    // private lateinit var googleSignInClient: GoogleSignInClient // Removed
    // private lateinit var googleSignInLauncher: ActivityResultLauncher<android.content.Intent> // Removed

    private lateinit var credentialManager: CredentialManager // Added

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
    private var userName: String? = null

    companion object {
        val THEME_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        // private const val RC_SIGN_IN = 9001 // Removed
        private const val TAG = "MainActivityAuth"
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
        credentialManager = CredentialManager.create(this) // Initialize CredentialManager

        // Old Google Sign-In Client, Options, and Launcher are removed.

        loadRewardedAd()
        loadInterstitialAd()

        if (auth.currentUser == null) {
            Log.d(TAG, "No current user, starting Google Sign-In flow with Credential Manager")
            startGoogleSignInFlow()
        } else {
            Log.d(TAG, "User already signed in, checking profile")
            userName = auth.currentUser?.displayName ?: sharedPreferences.getString("user_name", null)
            // Update photo URL from Firebase if available, or SharedPreferences
            val photoUrlFromFirebase = auth.currentUser?.photoUrl?.toString()
            if (photoUrlFromFirebase != null) {
                sharedPreferences.edit().putString("user_avatar_url", photoUrlFromFirebase).apply()
            }
            checkUserProfile()
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

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Set to false to show all Google accounts, true to filter by previously used.
                .setServerClientId(serverClientId)
                // .setAutoSelectEnabled(true) // Optional: attempt auto sign-in if one account is available without user interaction.
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
                Log.w(TAG, "NoGoogleCredentialException from CredentialManager: No credentials available.", e)
                Toast.makeText(this@MainActivity, "No Google accounts found on this device.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialException) { // Catches GetCredentialCancellationException, GetCredentialInterruptedException etc.
                Log.e(TAG, "GetCredentialException from CredentialManager", e)
                Toast.makeText(this@MainActivity, "Google Sign-In failed or was cancelled.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleGoogleCredential(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                val photoUri = googleIdTokenCredential.profilePictureUri // Photo URI from Credential Manager
                Log.d(TAG, "Google ID Token successfully retrieved. Token: $googleIdToken ProfilePicUri: $photoUri")

                // Optionally save/use photoUri from here if Firebase takes time or fails to provide it
                if (photoUri != null) {
                    sharedPreferences.edit().putString("user_avatar_url", photoUri.toString()).apply()
                }
                firebaseAuthWithGoogleIdToken(googleIdToken)
            } catch (e: Exception) { // Broad catch for createFrom or other issues
                Log.e(TAG, "Failed to create GoogleIdTokenCredential from CustomCredential or other error", e)
                Toast.makeText(this, "Google Sign-In failed (token processing error).", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w(TAG, "CredentialManager returned a credential that is not Google ID Token. Type: ${credential.type}")
            Toast.makeText(this, "Google Sign-In failed (unexpected credential type).", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Auth with Google ID Token: SUCCESS")
                    val firebaseUser = auth.currentUser
                    userName = firebaseUser?.displayName // Get name from Google account
                    val storedName = sharedPreferences.getString("user_name", null)

                    if (userName != null && userName != storedName) {
                        sharedPreferences.edit().putString("user_name", userName).apply()
                    }

                    // Prioritize photo URL from Firebase User, but fallback to what Credential Manager gave if Firebase is null
                    val photoUrlFromFirebase = firebaseUser?.photoUrl?.toString()
                    val photoUrlToStore = photoUrlFromFirebase ?: sharedPreferences.getString("user_avatar_url", null)

                    if (photoUrlToStore != null) {
                        sharedPreferences.edit().putString("user_avatar_url", photoUrlToStore).apply()
                        Log.d(TAG, "User photo URL stored: $photoUrlToStore (Source Firebase: ${photoUrlFromFirebase != null})")
                    } else {
                        Log.d(TAG, "User photo URL from Firebase Auth and Credential Manager were both null.")
                        // Potentially clear any old "user_avatar_url" if it should not persist
                        // sharedPreferences.edit().remove("user_avatar_url").apply()
                    }
                    checkUserProfile()
                } else {
                    Log.w(TAG, "Firebase Auth with Google ID Token: FAILURE", task.exception)
                    Toast.makeText(this, "Firebase Authentication Failed. ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
                loadRewardedAd() // Preload next ad
            }
        } else {
            Log.d("ADS", "Rewarded ad not ready, creating group directly")
            createGroupAfterAd(destination)
        }
    }

    private fun createGroupAfterAd(destination: String) {
        val lat = userLocation?.latitude ?: 8.9806 // Default to Addis Ababa center if null
        val lng = userLocation?.longitude ?: 38.7578
        createGroupInFirebase(toDestination = destination, pickupLatitude = lat, pickupLongitude = lng)
    }

    private fun handleGroupSelection(group: Group) { // This function was marked as unused previously, ensure it's called if needed.
        if (interstitialAd != null) {
            interstitialAd!!.show(this)
            interstitialAd!!.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("ADS", "Interstitial ad dismissed.")
                    joinGroupAfterAd(group)
                    loadInterstitialAd() // Preload next ad
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e("ADS", "Interstitial ad failed to show: ${adError.message}")
                    joinGroupAfterAd(group) // Proceed even if ad fails to show
                }
                override fun onAdShowedFullScreenContent() {
                    Log.d("ADS", "Interstitial ad showed.")
                }
            }
        } else {
            Log.d("ADS", "Interstitial ad not ready, joining group directly.")
            joinGroupAfterAd(group)
        }
    }

    private fun joinGroupAfterAd(group: Group) {
        joinGroupInFirebase(group) { success, message ->
            if (success) {
                Toast.makeText(this, "Joined group to ${group.destinationName}", Toast.LENGTH_SHORT).show()
                showGroupMembersDialog(group) // Show members after successful join
                refreshGroups()
            } else {
                Toast.makeText(this, "Could not join group: ${message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createGroupInFirebase(
        toDestination: String, // This is the originalDestination for the Group object
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "You must be signed in to create a group.", Toast.LENGTH_SHORT).show()
            startGoogleSignInFlow() // Prompt sign-in
            return
        }

        val timestamp = System.currentTimeMillis()
        val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeString = timeFormatter.format(java.util.Date(timestamp))
        val creatorDisplayName = auth.currentUser?.displayName ?: userName ?: "Anonymous User"
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatarUrlFromPrefs = sharedPreferences.getString("user_avatar_url", null)
        val finalUserAvatar = userAvatarUrlFromPrefs ?: sharedPreferences.getString("user_avatar", "avatar_1")!!

        // For the 'from' field, use a descriptive string or coordinates
        // Example: Using coordinates for now, but could be a reverse geocoded address.
        val fromLocationString = "Lat: ${"%.4f".format(pickupLatitude)}, Lng: ${"%.4f".format(pickupLongitude)}"

        // The unique name for display purposes, includes time and creator for uniqueness
        val uniqueFirebaseDisplayDestinationName = "$toDestination ($timeString) - by $creatorDisplayName"

        // Prepare member details for the creator
        val creatorMemberInfo = MemberInfo(
            name = creatorDisplayName,
            phone = userPhone,
            avatar = finalUserAvatar,
            joinedAt = timestamp
        )
        val initialMembers = hashMapOf(currentUserId to true)
        val initialMemberDetails = hashMapOf(currentUserId to creatorMemberInfo)

        val newGroupRef = groupsRef.push()
        val newGroupId = newGroupRef.key // Get the unique key first

        if (newGroupId == null) {
            Log.e("FIREBASE", "Failed to get new group key from Firebase.")
            Toast.makeText(this, "Failed to create group: Could not get group ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val newGroup = Group(
            groupId = newGroupId, // Set the obtained groupId
            creatorId = currentUserId,
            creatorName = creatorDisplayName,
            destinationName = uniqueFirebaseDisplayDestinationName, // For display in lists etc.
            originalDestination = toDestination, // The actual destination string for filtering/rules
            from = fromLocationString, // Populating the 'from' field for rules
            to = toDestination, // Populating the 'to' field for rules (matches originalDestination)
            status = "active", // Initial status
            pickupLat = pickupLatitude, // Corrected to match Group.kt constructor
            pickupLng = pickupLongitude, // Corrected to match Group.kt constructor
            timestamp = timestamp,
            maxMembers = 4, // Default, can be configurable later
            members = initialMembers,
            memberDetails = initialMemberDetails,
            memberCount = 1,
            imageUrl = null // Placeholder
        )

        newGroupRef.setValue(newGroup) // Pass the Group object directly
            .addOnSuccessListener {
                Log.d("FIREBASE", "Group created successfully: ${newGroup.destinationName} with ID: ${newGroup.groupId}")
                refreshGroups()
                Toast.makeText(this, "Group created: ${newGroup.destinationName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error creating group: ${newGroup.destinationName}, ID: ${newGroup.groupId}", e)
                // Log the object being sent for detailed review
                Log.e("FIREBASE_DATA", "Data sent: $newGroup")
                Toast.makeText(this, "Failed to create group: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun joinGroupInFirebase(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            onComplete(false, "You must be signed in to join a group.")
            startGoogleSignInFlow() // Prompt sign-in
            return
        }
        val groupId = group.groupId
        if (groupId == null) {
            onComplete(false, "Group ID is missing.")
            return
        }

        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        val groupTimestamp = group.timestamp // Smart cast fix
        if (groupTimestamp != null && groupTimestamp < thirtyMinutesAgo) {
            onComplete(false, "This group has expired.")
            return
        }
        if (group.memberCount >= group.maxMembers) {
            onComplete(false, "This group is already full.")
            return
        }
        if (group.members.containsKey(currentUserId) && group.members[currentUserId] == true) {
            // User is already a member, perhaps just show members or give a different message
            onComplete(true, "You are already a member of this group.") // Consider this a success for UI flow
            showGroupMembersDialog(group)
            return
        }

        val groupRef = groupsRef.child(groupId)
        val joinerDisplayName = auth.currentUser?.displayName ?: userName ?: "Anonymous User"
        val userPhone = sharedPreferences.getString("user_phone", "") ?: ""
        val userAvatarUrlFromPrefs = sharedPreferences.getString("user_avatar_url", null)
        val finalUserAvatar = userAvatarUrlFromPrefs ?: sharedPreferences.getString("user_avatar", "avatar_1")!!


        val updates = HashMap<String, Any>()
        updates["members/$currentUserId"] = true
        updates["memberDetails/$currentUserId/name"] = joinerDisplayName
        updates["memberDetails/$currentUserId/phone"] = userPhone
        updates["memberDetails/$currentUserId/avatar"] = finalUserAvatar
        updates["memberDetails/$currentUserId/joinedAt"] = System.currentTimeMillis()
        // No need to update memberCount here, use transaction

        groupRef.updateChildren(updates)
            .addOnSuccessListener {
                // Atomically increment memberCount
                groupRef.child("memberCount").runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        var currentCount = mutableData.getValue(Int::class.java) ?: 0
                        // Ensure we don't exceed maxMembers, though initial check should prevent this
                        if (currentCount < group.maxMembers) {
                            mutableData.value = currentCount + 1
                            return com.google.firebase.database.Transaction.success(mutableData)
                        }
                        return com.google.firebase.database.Transaction.abort() // Abort if somehow full
                    }

                    override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, currentData: com.google.firebase.database.DataSnapshot?) {
                        if (error != null) {
                            Log.e("FIREBASE_TX", "Transaction failed for memberCount: ${error.message}")
                            onComplete(false, "Failed to update member count: ${error.message}")
                        } else if (!committed) {
                            Log.w("FIREBASE_TX", "Transaction for memberCount not committed (e.g., aborted due to max members).")
                            onComplete(false, "Group might be full or another issue occurred.")
                        } else {
                            Log.d("FIREBASE_TX", "Transaction for memberCount successful.")
                            onComplete(true, null)
                            refreshGroups() // Refresh after successful join and count update
                        }
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Failed to join group (permission or network issue).", e)
                onComplete(false, "Permission denied or network error: ${e.message}")
            }
    }

    private fun checkUserProfile() {
        val user = auth.currentUser
        val storedUserName = sharedPreferences.getString("user_name", null)
        val userPhone = sharedPreferences.getString("user_phone", null)

        if (user == null) { // Should not happen if this is called after successful sign-in
            Log.w(TAG, "checkUserProfile called but auth.currentUser is null. Starting sign-in flow.")
            startGoogleSignInFlow()
            return
        }

        // If Firebase has display name, ensure it's in prefs.
        // If SharedPreferences has name from CredentialManager/previous input, use it if Firebase's is null/empty.
        val currentDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: storedUserName
        this.userName = currentDisplayName // Update activity's userName cache

        if (currentDisplayName.isNullOrBlank() || userPhone.isNullOrBlank()) {
            Log.d(TAG, "User profile incomplete. Name: $currentDisplayName, Phone: $userPhone. Showing registration dialog.")
            // Try to get photo from Firebase user first, then from SharedPreferences (which might have it from Credential Manager)
            val photoUrlForDialog = user.photoUrl?.toString() ?: sharedPreferences.getString("user_avatar_url", null)
            showUserRegistrationDialog(
                googleName = currentDisplayName, // Pass the best available name
                googlePhotoUrl = photoUrlForDialog
            )
        } else {
            Log.d(TAG, "User profile complete. Welcome back, $currentDisplayName. Phone: $userPhone. Photo URL: ${user.photoUrl ?: sharedPreferences.getString("user_avatar_url", "N/A")}")
            initializeMainScreen()
        }
    }

    private fun showUserRegistrationDialog(googleName: String? = null, googlePhotoUrl: String? = null) {
        setContent { // This will replace the entire content. Be cautious if called after initializeMainScreen.
            AkahidegnTheme { // Ensure theme is applied
                UserRegistrationDialog(
                    initialName = googleName ?: "",
                    initialPhotoUrl = googlePhotoUrl ?: sharedPreferences.getString("user_avatar_url", null),
                    onComplete = { name, phone ->
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            saveUserProfile(name, phone, googlePhotoUrl ?: sharedPreferences.getString("user_avatar_url", null))
                        } else {
                            Toast.makeText(this, "Please fill all fields: Name and Phone are required.", Toast.LENGTH_LONG).show()
                            // Optionally, re-show dialog or guide user. For now, they can try again.
                            showUserRegistrationDialog(name.ifBlank { googleName }, googlePhotoUrl) // Re-show with current inputs
                        }
                    },
                    onDismiss = { // User dismissed the dialog
                        Toast.makeText(this, "Registration is required to use all app features.", Toast.LENGTH_LONG).show()
                        // Decide behavior: sign out, restrict functionality, or re-prompt.
                        // For now, re-prompting sign-in might be too aggressive if they are already Firebase-authed.
                        // Let's try to initialize the main screen with potentially limited functionality or another prompt there.
                        if (auth.currentUser != null) {
                            initializeMainScreen() // Try to proceed, main screen might handle further checks.
                        } else {
                            startGoogleSignInFlow() // If auth is null, definitely restart.
                        }
                    }
                )
            }
        }
    }

    private fun saveUserProfile(name: String, phone: String, avatarUrl: String?) {
        this.userName = name // Update activity's immediate cache
        val editor = sharedPreferences.edit()
        editor.putString("user_name", name)
        editor.putString("user_phone", phone)

        val finalAvatarUrlToSave = avatarUrl ?: sharedPreferences.getString("user_avatar_url", null)

        if (finalAvatarUrlToSave != null) {
            editor.putString("user_avatar_url", finalAvatarUrlToSave)
            editor.remove("user_avatar") // Remove old placeholder key if URL is now available
            Log.d(TAG, "Saving user profile with avatar URL: $finalAvatarUrlToSave")
        } else {
            // If no URL, ensure a placeholder is set (or re-set if "user_avatar_url" was cleared)
            editor.putString("user_avatar", sharedPreferences.getString("user_avatar", "avatar_1"))
            Log.d(TAG, "Saving user profile, no avatar URL, using placeholder.")
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
                    Toast.makeText(this, "Welcome, $name! Your profile is saved.", Toast.LENGTH_SHORT).show()
                    initializeMainScreen() // Proceed to main app
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save user profile to Firebase. It's saved locally.", e)
                    Toast.makeText(this, "Welcome, $name! (Profile saved locally)", Toast.LENGTH_SHORT).show()
                    initializeMainScreen() // Proceed to main app
                }
        } else {
            Log.w(TAG, "User ID is null, cannot save profile to Firebase. Profile saved locally.")
            initializeMainScreen() // Proceed to main app
        }
    }

    private fun initializeMainScreen() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null && !isFinishing) { // Check !isFinishing to avoid issues if activity is closing
            Log.w(TAG, "initializeMainScreen called but user is not signed in. Attempting sign-in flow.")
            // If UserRegistrationDialog was just dismissed, this might immediately follow,
            // ensure UserRegistrationDialog's onDismiss doesn't also call startGoogleSignInFlow if auth.currentUser is still null.
            startGoogleSignInFlow()
            return
        }

        currentUserId?.let { mainViewModel.initializeFirebase(groupsRef, it) }

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val colorScheme = when (currentRoute) {
                Screen.Main.route -> HomeColorScheme
                Screen.ActiveGroups.route -> ActiveGroupsColorScheme
                Screen.Settings.route -> SettingsColorScheme
                else -> null // Defaults to AkahidegnTheme's default
            }
            AkahidegnTheme(selectedColorScheme = colorScheme) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(Screen.Main, Screen.ActiveGroups, Screen.Settings)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
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
                                // onGroupClick = { group -> handleGroupSelection(group) }, // Ensure handleGroupSelection is used
                                onGroupClick = { group -> showGroupMembersDialog(group) }, // Using this as per existing code
                                onRefreshGroups = { refreshGroups() },
                                onCreateGroup = { showCreateGroupDialog() }
                            )
                        }
                        composable(Screen.ActiveGroups.route) { ActiveGroupsScreen(/* Pass necessary params */) }
                        composable(Screen.Settings.route) {
                            SettingsScreen(onSignOut = {
                                lifecycleScope.launch {
                                    try {
                                        auth.signOut() // Sign out from Firebase
                                        Log.d(TAG, "Firebase sign-out successful.")
                                        // Clear credentials from Credential Manager
                                        val clearRequest = ClearCredentialStateRequest()
                                        credentialManager.clearCredentialState(clearRequest)
                                        Log.d(TAG, "CredentialManager state cleared.")
                                        Toast.makeText(this@MainActivity, "You have been signed out.", Toast.LENGTH_SHORT).show()
                                        // Clear local user data (optional, depends on desired behavior)
                                        sharedPreferences.edit().remove("user_name").remove("user_phone").remove("user_avatar_url").apply()
                                        userName = null
                                        startGoogleSignInFlow() // Go back to sign-in
                                    } catch (e: ClearCredentialException) {
                                        Log.e(TAG, "Couldn't clear user credentials during sign out.", e)
                                        Toast.makeText(this@MainActivity, "Signed out (credential clear failed).", Toast.LENGTH_SHORT).show()
                                        startGoogleSignInFlow() // Still attempt to go to sign-in
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error during sign out process.", e)
                                        Toast.makeText(this@MainActivity, "Error signing out.", Toast.LENGTH_SHORT).show()
                                        startGoogleSignInFlow() // Attempt to go to sign-in
                                    }
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
            // LaunchedEffect(Unit) { onRefreshGroups() } // Removed to avoid multiple calls if refreshGroups is called elsewhere
            val emptyFilters = remember { SearchFilters() } // remember emptyFilters
            var searchQuery by remember { mutableStateOf("") }
            var selectedFilters by remember { mutableStateOf(emptyFilters) }

            MainScreen(
                groups = groups,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> searchQuery = query },
                selectedFilters = selectedFilters,
                onFiltersChange = { filters -> selectedFilters = filters },
                onGroupClick = onGroupClick, // Pass through from NavHost
                isLoading = vmIsLoading,
                onRefreshGroups = onRefreshGroups,
                onCreateGroup = onCreateGroup
            )

            // This dialog display logic should ideally be hoisted or handled by a NavController if it's a separate "screen"
            selectedGroupForDialog?.let { group ->
                val currentUserId = auth.currentUser?.uid ?: ""
                val members = remember(group.memberDetails) { // Recompute only if memberDetails change
                    group.memberDetails.map { (userId, memberInfo) ->
                        GroupMember(
                            id = userId, name = memberInfo.name, phone = memberInfo.phone,
                            avatar = memberInfo.avatar ?: sharedPreferences.getString("user_avatar", "avatar_1")!!, // Fallback for avatar
                            isCreator = userId == group.creatorId
                        )
                    }
                }
                GroupMembersDialog(
                    group = group, members = members, currentUserId = currentUserId,
                    onDismiss = { selectedGroupForDialog = null },
                    onLeaveGroup = { groupId, userId ->
                        Log.d("GROUP_ACTION", "Leave group $groupId requested by $userId")
                        // Implement leave group logic here
                        // mainViewModel.leaveGroup(groupId, userId)
                        Toast.makeText(this@MainActivity, "Leave group feature not yet implemented.", Toast.LENGTH_SHORT).show()
                        selectedGroupForDialog = null // Dismiss dialog after action
                    }
                )
            }
        }
    }

    private fun showGroupMembersDialog(group: Group) {
        selectedGroupForDialog = group // This will trigger the recomposition in MainScreenContent to show the dialog
    }

    private fun setupLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000L, 50f, locationListener)
                Log.d("LOCATION", "Location updates requested.")
            } catch (e: SecurityException) { // Should not happen if permission is granted
                Log.e("LOCATION", "SecurityException requesting location updates despite permission grant.", e)
                Toast.makeText(this, "Location permission error. Please enable it in settings.", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d("LOCATION", "ACCESS_FINE_LOCATION permission not granted. Requesting...")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    @Suppress("DEPRECATION") // Suppress for the super call as well
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LOCATION", "ACCESS_FINE_LOCATION permission granted by user.")
                setupLocationUpdates()
            } else {
                Log.w("LOCATION", "ACCESS_FINE_LOCATION permission denied by user.")
                Toast.makeText(this, "Location permission is required for core app functionality and finding nearby groups.", Toast.LENGTH_LONG).show()
                // Optionally, disable location-dependent features or guide user to settings
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationManager.isInitialized) { // Check if locationManager has been initialized
            try {
                locationManager.removeUpdates(locationListener)
                Log.d("LOCATION", "Location updates removed in onDestroy.")
            } catch (e: Exception) { // Catch any exception during removal
                Log.e("LOCATION", "Error removing location updates in onDestroy", e)
            }
        }
        rewardedAd = null // Release ad resources
        interstitialAd = null
    }
}
