package com.dawitf.akahidegn.data.remote.service.impl

import com.dawitf.akahidegn.Group
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.*

class FirestoreGeoGroupService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val groupsRef = firestore.collection("groups")

    // Simple distance calculation function
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

    fun getNearbyGroups(lat: Double, lng: Double, radiusMeters: Double): Flow<List<Group>> = callbackFlow {
        val reg = groupsRef.addSnapshotListener { snapshot, _ ->
            val allGroups = snapshot?.documents?.mapNotNull { it.toObject(Group::class.java) } ?: emptyList()
            val nearbyGroups = allGroups.filter { group ->
                group.pickupLat?.let { groupLat ->
                    group.pickupLng?.let { groupLng ->
                        calculateDistance(lat, lng, groupLat, groupLng) <= radiusMeters
                    } ?: false
                } ?: false
            }
            trySend(nearbyGroups)
        }
        awaitClose { reg.remove() }
    }

    fun getAllGroups(): Flow<List<Group>> = callbackFlow {
        val reg = groupsRef.addSnapshotListener { snapshot, _ ->
            val groups = snapshot?.documents?.mapNotNull { it.toObject(Group::class.java) } ?: emptyList()
            trySend(groups)
        }
        awaitClose { reg.remove() }
    }
}
