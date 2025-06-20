package com.dawitf.akahidegn.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.Group
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class GroupsMapViewModel : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val database = Firebase.database
    private val groupsRef = database.reference.child("groups")

    // Distance calculation function
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun refreshGroups() {
        loadAllGroups()
    }

    fun loadAllGroups() {
        _isLoading.value = true
        groupsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupsList = mutableListOf<Group>()
                val currentTime = System.currentTimeMillis()
                val thirtyMinutesAgo = currentTime - (30 * 60 * 1000) // 30 minutes in milliseconds
                
                for (childSnapshot in snapshot.children) {
                    childSnapshot.getValue(Group::class.java)?.let { group ->
                        // Filter out groups older than 30 minutes
                        val groupTimestamp = group.timestamp ?: 0
                        if (groupTimestamp > thirtyMinutesAgo) {
                            groupsList.add(group)
                        } else {
                            // Optionally delete expired groups from Firebase
                            Log.d("GroupsMapViewModel", "Group ${group.groupId} expired, removing from Firebase")
                            childSnapshot.ref.removeValue()
                        }
                    }
                }
                _groups.value = groupsList
                _isLoading.value = false
                Log.d("GroupsMapViewModel", "Loaded ${groupsList.size} groups (filtered by 30min expiry)")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupsMapViewModel", "Database error: ${error.message}")
                _isLoading.value = false
            }
        })
    }

    fun loadNearbyGroups(lat: Double, lng: Double, radiusMeters: Double = 500.0) {
        _isLoading.value = true
        groupsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupsList = mutableListOf<Group>()
                val currentTime = System.currentTimeMillis()
                val thirtyMinutesAgo = currentTime - (30 * 60 * 1000) // 30 minutes in milliseconds
                
                for (childSnapshot in snapshot.children) {
                    childSnapshot.getValue(Group::class.java)?.let { group ->
                        // Filter out groups older than 30 minutes
                        val groupTimestamp = group.timestamp ?: 0
                        if (groupTimestamp > thirtyMinutesAgo) {
                            // Check if group is within radius
                            group.pickupLat?.let { groupLat ->
                                group.pickupLng?.let { groupLng ->
                                    val distance = calculateDistance(lat, lng, groupLat, groupLng)
                                    if (distance <= radiusMeters) {
                                        groupsList.add(group)
                                    }
                                }
                            }
                        } else {
                            // Optionally delete expired groups from Firebase
                            Log.d("GroupsMapViewModel", "Group ${group.groupId} expired, removing from Firebase")
                            childSnapshot.ref.removeValue()
                        }
                    }
                }
                _groups.value = groupsList
                _isLoading.value = false
                Log.d("GroupsMapViewModel", "Loaded ${groupsList.size} nearby groups within ${radiusMeters}m (filtered by 30min expiry)")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupsMapViewModel", "Database error: ${error.message}")
                _isLoading.value = false
            }
        })
    }
}
