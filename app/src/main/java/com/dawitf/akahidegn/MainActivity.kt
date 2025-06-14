package com.dawitf.akahidegn

import android.content.Context
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

class MainActivity : ComponentActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var groupsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val currentGroups = mutableListOf<Group>()
    private var currentDestination = ""

    companion object {
        // Theme mode key for preferences
        val THEME_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase components
        database = Firebase.database
        groupsRef = database.reference.child("groups")
        auth = Firebase.auth
        
        // Ensure the user is signed in anonymously
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "signInAnonymously:success")
                } else {
                    Log.w("AUTH", "signInAnonymously:failure", task.exception)
                }
            }
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
                        onNavigateToChat = { /* Navigate to chat */ },
                        onNavigateToNotifications = { /* Navigate to notifications */ },
                        onNavigateToHistory = { /* Navigate to history */ }
                    )
                }
            }
        }
    }
    
    private fun refreshGroups() {
        groupsRef.get().addOnSuccessListener { dataSnapshot ->
            val groups = mutableListOf<Group>()
            dataSnapshot.children.forEach { childSnapshot ->
                val group = childSnapshot.getValue(Group::class.java)
                group?.let {
                    it.groupId = childSnapshot.key
                    // Only add non-expired groups
                    val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
                    if (it.timestamp == null || it.timestamp!! > thirtyMinutesAgo) {
                        groups.add(it)
                    }
                }
            }
            currentGroups.clear()
            currentGroups.addAll(groups) // All groups are already filtered
        }.addOnFailureListener {
            Log.e("FIREBASE", "Error getting groups", it)
        }
    }
    
    private fun filterExpiredGroups(groups: List<Group>): List<Group> {
        val thirtyMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        return groups.filter { it.timestamp == null || it.timestamp!! > thirtyMinutesAgo }
    }
    
    private fun showCreateGroupDialog() {
        // For simplicity, let's create a group with hardcoded test data
        // In a real implementation, this would show a dialog to get input from the user
        val testDestinations = listOf(
            "Bole",
            "Megenagna",
            "Piassa",
            "Kazanchis",
            "Stadium",
            "Mexico",
            "Jemo",
            "CMC",
            "Summit",
            "Ayat"
        )
        
        val randomDestination = testDestinations.random()
        val randomLat = 8.9806 + (Math.random() * 0.1)
        val randomLng = 38.7578 + (Math.random() * 0.1)
        
        // Create the group with these random values
        createGroupInFirebase(
            destinationName = randomDestination,
            pickupLatitude = randomLat,
            pickupLongitude = randomLng
        )
        
        Toast.makeText(
            this,
            "Creating group for destination: $randomDestination",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun handleGroupSelection(group: Group) {
        // When a group is selected, try to join it
        joinGroupInFirebase(group) { success, message ->
            if (success) {
                Toast.makeText(
                    this,
                    "Successfully joined group to ${group.destinationName}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // In a real app, you would navigate to the group chat screen here
                // For now, we'll just refresh the groups list
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
        
        // Get a local avatar for the group
        val newGroup = Group(
            creatorId = currentUserId,
            destinationName = destinationName, // Explicitly set destination name
            pickupLat = pickupLatitude,
            pickupLng = pickupLongitude,
            timestamp = timestamp,
            maxMembers = 4,
            memberCount = 1,
            imageUrl = null // Force use of local images
        )
        
        // Add current user as a member
        newGroup.members[currentUserId] = true
        
        // Push to Firebase
        val newGroupRef = groupsRef.push()
        
        // Set the ID before saving so it's included in toMap()
        newGroup.groupId = newGroupRef.key
        
        newGroupRef.setValue(newGroup.toMap())
            .addOnSuccessListener {
                Log.d("FIREBASE", "Group created successfully")
                refreshGroups() // Refresh groups to immediately show the new group
                Toast.makeText(
                    this@MainActivity,
                    "Group created successfully: $destinationName",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error creating group", e)
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
        
        // First update the members list
        val updates = HashMap<String, Any>()
        updates["members/$currentUserId"] = true
        
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
}
