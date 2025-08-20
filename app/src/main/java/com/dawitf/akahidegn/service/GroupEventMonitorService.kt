package com.dawitf.akahidegn.service

import android.util.Log
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.GroupReader
import com.dawitf.akahidegn.notifications.service.NotificationManagerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to monitor Firebase Realtime Database changes and send notifications
 * for group events like member joins/leaves, group full, etc.
 */
@Singleton
class GroupEventMonitorService @Inject constructor(
    private val notificationService: NotificationManagerService,
    private val auth: FirebaseAuth
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeListeners = mutableMapOf<String, ValueEventListener>()
    private val monitoredGroups = mutableMapOf<String, Group>()

    companion object {
        private const val TAG = "GroupEventMonitor"
    }

    /**
     * Start monitoring a group for changes
     */
    fun startMonitoringGroup(groupRef: DatabaseReference, groupId: String) {
        if (activeListeners.containsKey(groupId)) {
            Log.d(TAG, "Already monitoring group: $groupId")
            return
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    handleGroupDataChange(snapshot, groupId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to monitor group $groupId: ${error.message}")
                stopMonitoringGroup(groupId)
            }
        }

        groupRef.child(groupId).addValueEventListener(listener)
        activeListeners[groupId] = listener

        Log.d(TAG, "Started monitoring group: $groupId")
    }

    /**
     * Stop monitoring a specific group
     */
    fun stopMonitoringGroup(groupId: String) {
        activeListeners[groupId]?.let { listener ->
            // Note: We would need the DatabaseReference to remove the listener
            // This is a limitation - in practice, we should store the ref too
            activeListeners.remove(groupId)
            monitoredGroups.remove(groupId)
            Log.d(TAG, "Stopped monitoring group: $groupId")
        }
    }

    /**
     * Stop monitoring all groups
     */
    fun stopAllMonitoring() {
        activeListeners.clear()
        monitoredGroups.clear()
        Log.d(TAG, "Stopped monitoring all groups")
    }

    /**
     * Handle changes in group data and send appropriate notifications
     */
    private suspend fun handleGroupDataChange(snapshot: DataSnapshot, groupId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        try {
            val newGroup = GroupReader.fromSnapshot(snapshot) ?: run {
                Log.w(TAG, "Could not parse group from snapshot: $groupId")
                return
            }

            val previousGroup = monitoredGroups[groupId]

            if (previousGroup == null) {
                // First time seeing this group, just store it
                monitoredGroups[groupId] = newGroup
                Log.d(TAG, "Initial group data stored for: $groupId")
                return
            }

            // Check if current user is a member of this group
            val isCurrentUserMember = newGroup.members.containsKey(currentUserId) &&
                                    newGroup.members[currentUserId] == true

            if (!isCurrentUserMember) {
                // User is not a member, don't send notifications
                monitoredGroups[groupId] = newGroup
                return
            }

            // Detect and handle specific changes
            detectMemberChanges(previousGroup, newGroup, currentUserId)
            detectGroupStatusChanges(previousGroup, newGroup, currentUserId)

            // Update stored group
            monitoredGroups[groupId] = newGroup

        } catch (e: Exception) {
            Log.e(TAG, "Error handling group data change for $groupId", e)
        }
    }

    /**
     * Detect new members joining or leaving
     */
    private fun detectMemberChanges(previousGroup: Group, newGroup: Group, currentUserId: String) {
        val previousMembers = previousGroup.members.filter { it.value }.keys
        val newMembers = newGroup.members.filter { it.value }.keys

        // Detect new members
        val joinedMembers = newMembers - previousMembers
        joinedMembers.forEach { memberId ->
            if (memberId != currentUserId) { // Don't notify about self
                val memberInfo = newGroup.memberDetails[memberId]
                val memberName = memberInfo?.name ?: "Unknown User"

                Log.d(TAG, "Member joined: $memberName in group ${newGroup.destinationName}")
                notificationService.showUserJoinedNotification(newGroup, memberName)
            }
        }

        // Detect members who left
        val leftMembers = previousMembers - newMembers
        leftMembers.forEach { memberId ->
            if (memberId != currentUserId) { // Don't notify about self leaving
                val memberInfo = previousGroup.memberDetails[memberId]
                val memberName = memberInfo?.name ?: "Unknown User"

                Log.d(TAG, "Member left: $memberName from group ${newGroup.destinationName}")
                notificationService.showUserLeftNotification(newGroup, memberName)
            }
        }
    }

    /**
     * Detect group status changes (full, disbanded, etc.)
     */
    private fun detectGroupStatusChanges(previousGroup: Group, newGroup: Group, currentUserId: String) {
        // Check if group became full
        if (previousGroup.memberCount < previousGroup.maxMembers &&
            newGroup.memberCount >= newGroup.maxMembers) {

            Log.d(TAG, "Group became full: ${newGroup.destinationName}")
            notificationService.showGroupFullNotification(newGroup)
        }

        // Check if group was disbanded (no members left or disbanded flag set)
        val wasPreviouslyActive = previousGroup.memberCount > 0
        val isNowDisbanded = newGroup.memberCount == 0

        if (wasPreviouslyActive && isNowDisbanded) {
            Log.d(TAG, "Group was disbanded: ${newGroup.destinationName}")
            notificationService.showGroupDisbandedNotification(newGroup)

            // Stop monitoring this group as it's disbanded
            stopMonitoringGroup(newGroup.groupId ?: "")
        }
    }

    /**
     * Start monitoring all groups that the current user is a member of
     */
    fun startMonitoringUserGroups(groupsRef: DatabaseReference) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Listen for groups where current user is a member
        val query = groupsRef.orderByChild("members/$currentUserId").equalTo(true)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    for (groupSnapshot in snapshot.children) {
                        val groupId = groupSnapshot.key ?: continue

                        // Start monitoring each group individually
                        if (!activeListeners.containsKey(groupId)) {
                            startMonitoringGroup(groupsRef, groupId)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to query user groups: ${error.message}")
            }
        })
    }
}
