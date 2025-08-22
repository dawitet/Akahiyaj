package com.dawitf.akahidegn.service

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupCleanupService @Inject constructor(
    private val database: FirebaseDatabase
) {

    private val groupsRef = database.reference.child("groups")
    private var cleanupJob: Job? = null

    companion object {
        private const val TAG = "GroupCleanupService"
        private const val CLEANUP_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val GROUP_EXPIRATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    fun startPeriodicCleanup() {
        stopPeriodicCleanup()

        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    cleanupExpiredGroups()
                    delay(CLEANUP_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during periodic cleanup", e)
                    delay(CLEANUP_INTERVAL_MS) // Still wait before retrying
                }
            }
        }

        Log.d(TAG, "Started periodic group cleanup (every 5 minutes)")
    }

    fun stopPeriodicCleanup() {
        cleanupJob?.cancel()
        cleanupJob = null
        Log.d(TAG, "Stopped periodic group cleanup")
    }

    fun cleanupExpiredGroups(): CleanupResult {
        Log.d(TAG, "Starting cleanup of expired groups...")

        return try {
            val result = CleanupResult()

            groupsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "No groups found to clean up")
                        return
                    }

                    val now = System.currentTimeMillis()
                    val expiredGroupIds = mutableListOf<String>()

                    // Find expired groups
                    for (groupSnapshot in snapshot.children) {
                        val groupId = groupSnapshot.key ?: continue
                        val timestamp = groupSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val expiresAt = groupSnapshot.child("expiresAt").getValue(Long::class.java)
                            ?: (timestamp + GROUP_EXPIRATION_MS)

                        if (now > expiresAt) {
                            expiredGroupIds.add(groupId)
                            Log.d(TAG, "Found expired group: $groupId (expired at ${java.util.Date(expiresAt)})")
                        }
                    }

                    result.totalChecked = snapshot.childrenCount.toInt()
                    result.expiredCount = expiredGroupIds.size

                    // Remove expired groups
                    if (expiredGroupIds.isNotEmpty()) {
                        val updates = mutableMapOf<String, Any?>()
                        expiredGroupIds.forEach { groupId ->
                            updates[groupId] = null
                        }

                        groupsRef.updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "Successfully cleaned up ${expiredGroupIds.size} expired groups")
                                result.success = true
                            }
                            .addOnFailureListener { error ->
                                Log.e(TAG, "Failed to clean up expired groups", error)
                                result.success = false
                                result.error = error.message
                            }
                    } else {
                        Log.d(TAG, "No expired groups found")
                        result.success = true
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to read groups for cleanup", error.toException())
                    result.success = false
                    result.error = error.message
                }
            })

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception during cleanup", e)
            CleanupResult(success = false, error = e.message)
        }
    }

    data class CleanupResult(
        var success: Boolean = false,
        var totalChecked: Int = 0,
        var expiredCount: Int = 0,
        var error: String? = null
    )
}
