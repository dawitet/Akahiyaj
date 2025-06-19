package com.dawitf.akahidegn.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.GroupsWebMapView
import com.dawitf.akahidegn.ui.viewmodel.GroupsMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMainScreen(
    groups: List<Group>,
    userLocation: Location?,
    modifier: Modifier = Modifier
) {
    // Initialize ViewModel for Firebase Realtime Database groups
    val groupsMapViewModel: GroupsMapViewModel = viewModel()
    val realtimeGroups by groupsMapViewModel.groups.collectAsState()
    
    // Use realtime groups if available, otherwise fallback to passed groups
    val displayGroups = if (realtimeGroups.isNotEmpty()) realtimeGroups else groups
    
    // Use the display groups directly
    val finalGroups = displayGroups
    
    // Load groups on launch and when user location changes
    LaunchedEffect(userLocation) {
        userLocation?.let {
            // Load nearby groups if location is available
            groupsMapViewModel.loadNearbyGroups(it.latitude, it.longitude, 500.0)
        } ?: run {
            // Load all groups if no location
            groupsMapViewModel.loadAllGroups()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("አካሂያጅ") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Found ${finalGroups.size} groups")
            Text("Location: ${if (userLocation != null) "Available (${userLocation.latitude.format(4)}, ${userLocation.longitude.format(4)})" else "Not available"}")
            
            // Debug: Show group details
            if (finalGroups.isNotEmpty()) {
                Text("Active groups:")
                finalGroups.take(3).forEach { group ->
                    val dest = group.originalDestination ?: group.destinationName ?: "Unknown"
                    val coords = if (group.pickupLat != null && group.pickupLng != null) 
                        " at ${group.pickupLat!!.format(4)}, ${group.pickupLng!!.format(4)}" else ""
                    Text("- $dest (${group.memberCount}/${group.maxMembers})$coords")
                }
                if (finalGroups.size > 3) {
                    Text("... and ${finalGroups.size - 3} more")
                }
            } else {
                Text("No active groups found - create one to see it on the map!")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // OSM + Leaflet WebView Map
            GroupsWebMapView(
                context = LocalContext.current,
                groups = finalGroups,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Extension function to format doubles
private fun Double.format(digits: Int) = "%.${digits}f".format(this)
