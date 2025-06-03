package com.dawitf.akahidegn.features.social.impl

import android.location.Location
import android.util.Log
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.features.social.RideBuddyService
import com.dawitf.akahidegn.features.social.RideBuddyService.*
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class RideBuddyServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val analyticsService: AnalyticsService
) : RideBuddyService {
    
    private val buddiesRef = database.getReference("ride_buddies")
    private val invitationsRef = database.getReference("buddy_invitations")
    private val regularGroupsRef = database.getReference("regular_groups")
    private val userStatsRef = database.getReference("buddy_stats")
    private val rideHistoryRef = database.getReference("ride_history")
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    override suspend fun sendBuddyInvitation(
        toUserId: String,
        toUserName: String,
        message: String,
        groupId: String?
    ): Result<String> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            val userName = auth.currentUser?.displayName ?: "Anonymous"
            
            val invitationId = invitationsRef.push().key 
                ?: return Result.failure(Exception("Failed to generate invitation ID"))
            
            val invitation = RideBuddyInvitation(
                invitationId = invitationId,
                fromUserId = userId,
                fromUserName = userName,
                toUserId = toUserId,
                toUserName = toUserName,
                message = message,
                groupId = groupId
            )
            
            // Store invitation
            invitationsRef.child(invitationId).setValue(invitation.toMap()).await()
            
            // Add to recipient's pending invitations
            invitationsRef.child("by_user").child(toUserId).child("received")
                .child(invitationId).setValue(true).await()
            
            // Add to sender's sent invitations
            invitationsRef.child("by_user").child(userId).child("sent")
                .child(invitationId).setValue(true).await()
            
            // Track analytics
            analyticsService.trackEvent("buddy_invitation_sent", mapOf(
                "to_user_id" to toUserId,
                "has_message" to message.isNotBlank(),
                "from_group" to (groupId != null)
            ))
            
            Log.d("RideBuddyService", "Invitation sent successfully: $invitationId")
            Result.success(invitationId)
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to send invitation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun respondToBuddyInvitation(
        invitationId: String,
        accept: Boolean,
        message: String
    ): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Get invitation details
            val invitationSnapshot = invitationsRef.child(invitationId).get().await()
            val invitation = invitationSnapshot.toInvitation() 
                ?: return Result.failure(Exception("Invitation not found"))
            
            if (invitation.toUserId != userId) {
                return Result.failure(Exception("Not authorized to respond to this invitation"))
            }
            
            // Update invitation status
            val newStatus = if (accept) InvitationStatus.ACCEPTED else InvitationStatus.DECLINED
            invitationsRef.child(invitationId).child("status").setValue(newStatus.name).await()
            
            if (accept) {
                // Add each other as buddies
                addBuddyRelationship(invitation.fromUserId, invitation.fromUserName, userId, invitation.toUserName)
            }
            
            // Track analytics
            analyticsService.trackEvent("buddy_invitation_responded", mapOf(
                "invitation_id" to invitationId,
                "accepted" to accept,
                "from_user_id" to invitation.fromUserId
            ))
            
            Log.d("RideBuddyService", "Invitation response recorded: $invitationId, accepted: $accept")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to respond to invitation", e)
            Result.failure(e)
        }
    }
    
    private suspend fun addBuddyRelationship(user1Id: String, user1Name: String, user2Id: String, user2Name: String) {
        try {
            // Add user2 to user1's buddies
            val buddy1 = RideBuddy(
                userId = user2Id,
                displayName = user2Name,
                totalRidesTogether = 0,
                addedDate = System.currentTimeMillis()
            )
            buddiesRef.child(user1Id).child(user2Id).setValue(buddy1.toMap()).await()
            
            // Add user1 to user2's buddies
            val buddy2 = RideBuddy(
                userId = user1Id,
                displayName = user1Name,
                totalRidesTogether = 0,
                addedDate = System.currentTimeMillis()
            )
            buddiesRef.child(user2Id).child(user1Id).setValue(buddy2.toMap()).await()
            
            Log.d("RideBuddyService", "Buddy relationship established between $user1Id and $user2Id")
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to add buddy relationship", e)
            throw e
        }
    }
    
    override suspend fun removeBuddy(buddyUserId: String): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Remove from both users' buddy lists
            buddiesRef.child(userId).child(buddyUserId).removeValue().await()
            buddiesRef.child(buddyUserId).child(userId).removeValue().await()
            
            analyticsService.trackEvent("buddy_removed", mapOf(
                "buddy_user_id" to buddyUserId
            ))
            
            Log.d("RideBuddyService", "Buddy removed: $buddyUserId")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to remove buddy", e)
            Result.failure(e)
        }
    }
    
    override fun getRideBuddies(): Flow<List<RideBuddy>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val buddies = mutableListOf<RideBuddy>()
                for (child in snapshot.children) {
                    child.toRideBuddy()?.let { buddies.add(it) }
                }
                trySend(buddies.sortedByDescending { it.totalRidesTogether })
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("RideBuddyService", "Failed to load buddies", error.toException())
                close(error.toException())
            }
        }
        
        buddiesRef.child(userId).addValueEventListener(listener)
        
        awaitClose {
            buddiesRef.child(userId).removeEventListener(listener)
        }
    }
    
    override fun getPendingInvitations(): Flow<List<RideBuddyInvitation>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invitations = mutableListOf<RideBuddyInvitation>()
                for (child in snapshot.children) {
                    val invitationId = child.key ?: continue
                    // Get full invitation details
                    invitationsRef.child(invitationId).get().addOnSuccessListener { invSnapshot ->
                        invSnapshot.toInvitation()?.let { invitation ->
                            if (invitation.status == InvitationStatus.PENDING) {
                                invitations.add(invitation)
                            }
                        }
                        trySend(invitations.sortedByDescending { it.timestamp })
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("RideBuddyService", "Failed to load pending invitations", error.toException())
                close(error.toException())
            }
        }
        
        invitationsRef.child("by_user").child(userId).child("received").addValueEventListener(listener)
        
        awaitClose {
            invitationsRef.child("by_user").child(userId).child("received").removeEventListener(listener)
        }
    }
    
    override fun getSentInvitations(): Flow<List<RideBuddyInvitation>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invitations = mutableListOf<RideBuddyInvitation>()
                for (child in snapshot.children) {
                    val invitationId = child.key ?: continue
                    invitationsRef.child(invitationId).get().addOnSuccessListener { invSnapshot ->
                        invSnapshot.toInvitation()?.let { invitations.add(it) }
                        trySend(invitations.sortedByDescending { it.timestamp })
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("RideBuddyService", "Failed to load sent invitations", error.toException())
                close(error.toException())
            }
        }
        
        invitationsRef.child("by_user").child(userId).child("sent").addValueEventListener(listener)
        
        awaitClose {
            invitationsRef.child("by_user").child(userId).child("sent").removeEventListener(listener)
        }
    }
    
    override suspend fun createRegularGroup(
        groupName: String,
        memberIds: List<String>,
        commonRoute: String,
        preferredTimes: List<String>,
        description: String
    ): Result<String> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            val groupId = regularGroupsRef.push().key 
                ?: return Result.failure(Exception("Failed to generate group ID"))
            
            val regularGroup = RegularGroup(
                groupId = groupId,
                groupName = groupName,
                members = memberIds + userId,
                creatorId = userId,
                commonRoute = commonRoute,
                preferredTimes = preferredTimes,
                description = description
            )
            
            regularGroupsRef.child(groupId).setValue(regularGroup.toMap()).await()
            
            // Add group to each member's regular groups list
            for (memberId in regularGroup.members) {
                regularGroupsRef.child("by_user").child(memberId).child(groupId).setValue(true).await()
            }
            
            analyticsService.trackEvent("regular_group_created", mapOf(
                "group_id" to groupId,
                "member_count" to regularGroup.members.size,
                "has_description" to description.isNotBlank()
            ))
            
            Log.d("RideBuddyService", "Regular group created: $groupId")
            Result.success(groupId)
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to create regular group", e)
            Result.failure(e)
        }
    }
    
    override suspend fun recordRideWithUser(
        otherUserId: String,
        otherUserName: String,
        groupId: String,
        rating: Float?
    ): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Update ride count for both users
            val updates = mutableMapOf<String, Any>()
            
            // Update buddy relationship if it exists
            val buddyRef = buddiesRef.child(userId).child(otherUserId)
            val buddySnapshot = buddyRef.get().await()
            
            if (buddySnapshot.exists()) {
                val currentCount = buddySnapshot.child("totalRidesTogether").getValue(Int::class.java) ?: 0
                updates["buddies/$userId/$otherUserId/totalRidesTogether"] = currentCount + 1
                updates["buddies/$userId/$otherUserId/lastRideDate"] = System.currentTimeMillis()
                
                val otherBuddyRef = buddiesRef.child(otherUserId).child(userId)
                val otherBuddySnapshot = otherBuddyRef.get().await()
                if (otherBuddySnapshot.exists()) {
                    val otherCurrentCount = otherBuddySnapshot.child("totalRidesTogether").getValue(Int::class.java) ?: 0
                    updates["buddies/$otherUserId/$userId/totalRidesTogether"] = otherCurrentCount + 1
                    updates["buddies/$otherUserId/$userId/lastRideDate"] = System.currentTimeMillis()
                }
                
                // Update rating if provided
                rating?.let {
                    val currentRating = buddySnapshot.child("averageRating").getValue(Float::class.java) ?: 0f
                    val rideCount = currentCount + 1
                    val newRating = ((currentRating * currentCount) + rating) / rideCount
                    updates["buddies/$userId/$otherUserId/averageRating"] = newRating
                }
            }
            
            // Record ride in history for analytics
            val rideRecord = mapOf(
                "userId" to userId,
                "otherUserId" to otherUserId,
                "groupId" to groupId,
                "timestamp" to System.currentTimeMillis(),
                "rating" to rating
            )
            rideHistoryRef.push().setValue(rideRecord).await()
            
            // Apply all updates
            if (updates.isNotEmpty()) {
                database.reference.updateChildren(updates).await()
            }
            
            analyticsService.trackEvent("ride_with_buddy_recorded", mapOf(
                "other_user_id" to otherUserId,
                "group_id" to groupId,
                "has_rating" to (rating != null)
            ))
            
            Log.d("RideBuddyService", "Ride recorded with user: $otherUserId")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to record ride", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getBuddySuggestions(): List<RideBuddySuggestion> {
        return try {
            val userId = currentUserId ?: return emptyList()
            
            // Get ride history to find frequent co-riders who aren't buddies yet
            val rideHistorySnapshot = rideHistoryRef.orderByChild("userId").equalTo(userId).get().await()
            val userRideCounts = mutableMapOf<String, Int>()
            val userNames = mutableMapOf<String, String>()
            
            for (child in rideHistorySnapshot.children) {
                val otherUserId = child.child("otherUserId").getValue(String::class.java) ?: continue
                userRideCounts[otherUserId] = (userRideCounts[otherUserId] ?: 0) + 1
                // Store user name for suggestion
                // In a real app, you'd fetch this from user profiles
            }
            
            // Get current buddies to exclude them
            val currentBuddies = buddiesRef.child(userId).get().await()
            val buddyIds = currentBuddies.children.mapNotNull { it.key }.toSet()
            
            // Create suggestions for users with multiple rides who aren't buddies
            val suggestions = mutableListOf<RideBuddySuggestion>()
            for ((otherUserId, rideCount) in userRideCounts) {
                if (otherUserId !in buddyIds && rideCount >= 2) {
                    val confidence = minOf(rideCount / 10f, 1f) // Max confidence at 10+ rides
                    suggestions.add(
                        RideBuddySuggestion(
                            userId = otherUserId,
                            displayName = userNames[otherUserId] ?: "User $otherUserId",
                            ridesTogether = rideCount,
                            suggestionReason = "You've ridden together $rideCount times",
                            confidence = confidence
                        )
                    )
                }
            }
            
            suggestions.sortedByDescending { it.confidence }
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to get buddy suggestions", e)
            emptyList()
        }
    }
    
    // Additional implementation methods...
    override suspend fun joinRegularGroup(groupId: String): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            val groupRef = regularGroupsRef.child(groupId)
            val groupSnapshot = groupRef.get().await()
            
            if (!groupSnapshot.exists()) {
                return Result.failure(Exception("Group not found"))
            }
            
            // Add user to group members
            groupRef.child("members").child(userId).setValue(true).await()
            regularGroupsRef.child("by_user").child(userId).child(groupId).setValue(true).await()
            
            analyticsService.trackEvent("regular_group_joined", mapOf("group_id" to groupId))
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to join regular group", e)
            Result.failure(e)
        }
    }
    
    override suspend fun leaveRegularGroup(groupId: String): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            regularGroupsRef.child(groupId).child("members").child(userId).removeValue().await()
            regularGroupsRef.child("by_user").child(userId).child(groupId).removeValue().await()
            
            analyticsService.trackEvent("regular_group_left", mapOf("group_id" to groupId))
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to leave regular group", e)
            Result.failure(e)
        }
    }
    
    override fun getRegularGroups(): Flow<List<RegularGroup>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = mutableListOf<RegularGroup>()
                for (child in snapshot.children) {
                    val groupId = child.key ?: continue
                    regularGroupsRef.child(groupId).get().addOnSuccessListener { groupSnapshot ->
                        groupSnapshot.toRegularGroup()?.let { groups.add(it) }
                        trySend(groups.sortedBy { it.groupName })
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("RideBuddyService", "Failed to load regular groups", error.toException())
                close(error.toException())
            }
        }
        
        regularGroupsRef.child("by_user").child(userId).addValueEventListener(listener)
        
        awaitClose {
            regularGroupsRef.child("by_user").child(userId).removeEventListener(listener)
        }
    }
    
    override suspend fun updateBuddyRating(buddyUserId: String, rating: Float): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            val buddyRef = buddiesRef.child(userId).child(buddyUserId)
            val snapshot = buddyRef.get().await()
            
            if (snapshot.exists()) {
                val currentRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 0f
                val rideCount = snapshot.child("totalRidesTogether").getValue(Int::class.java) ?: 1
                val newRating = ((currentRating * (rideCount - 1)) + rating) / rideCount
                
                buddyRef.child("averageRating").setValue(newRating).await()
                Result.success(true)
            } else {
                Result.failure(Exception("Buddy not found"))
            }
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to update buddy rating", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchPotentialBuddies(query: String, currentLocation: Location?): List<RideBuddy> {
        // This would typically search a user directory/profile database
        // For now, return empty list as it requires additional user profile infrastructure
        return emptyList()
    }
    
    override suspend fun getBuddiesInArea(location: Location, radiusKm: Double): List<RideBuddy> {
        // This would require location data for buddies
        // For now, return empty list as it requires location sharing infrastructure
        return emptyList()
    }
    
    override suspend fun updateNotificationPreferences(
        enableInvitations: Boolean,
        enableGroupUpdates: Boolean,
        enableRideReminders: Boolean
    ): Result<Boolean> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            val preferences = mapOf(
                "enableInvitations" to enableInvitations,
                "enableGroupUpdates" to enableGroupUpdates,
                "enableRideReminders" to enableRideReminders
            )
            
            database.getReference("user_preferences").child(userId).child("buddy_notifications")
                .setValue(preferences).await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to update notification preferences", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getFrequentRidePartners(limit: Int): List<RideBuddy> {
        return try {
            val userId = currentUserId ?: return emptyList()
            
            val snapshot = buddiesRef.child(userId).orderByChild("totalRidesTogether").get().await()
            val buddies = mutableListOf<RideBuddy>()
            
            for (child in snapshot.children) {
                child.toRideBuddy()?.let { buddies.add(it) }
            }
            
            buddies.sortedByDescending { it.totalRidesTogether }.take(limit)
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to get frequent ride partners", e)
            emptyList()
        }
    }
    
    override suspend fun getBuddyStats(): BuddyStats {
        return try {
            val userId = currentUserId ?: return BuddyStats(0, 0, 0f, null, 0, 0)
            
            val buddiesSnapshot = buddiesRef.child(userId).get().await()
            val regularGroupsSnapshot = regularGroupsRef.child("by_user").child(userId).get().await()
            
            val buddies = mutableListOf<RideBuddy>()
            for (child in buddiesSnapshot.children) {
                child.toRideBuddy()?.let { buddies.add(it) }
            }
            
            val totalRides = buddies.sumOf { it.totalRidesTogether }
            val averageRating = if (buddies.isNotEmpty()) {
                buddies.map { it.averageRating }.filter { it > 0 }.average().toFloat()
            } else 0f
            
            val mostFrequentBuddy = buddies.maxByOrNull { it.totalRidesTogether }
            val regularGroupsCount = regularGroupsSnapshot.childrenCount.toInt()
            
            // Calculate monthly rides (last 30 days)
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
            val monthlyRides = buddies.count { it.lastRideDate > thirtyDaysAgo }
            
            BuddyStats(
                totalBuddies = buddies.size,
                totalRidesWithBuddies = totalRides,
                averageBuddyRating = averageRating,
                mostFrequentBuddy = mostFrequentBuddy,
                regularGroupsCount = regularGroupsCount,
                monthlyRidesWithBuddies = monthlyRides
            )
            
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to get buddy stats", e)
            BuddyStats(0, 0, 0f, null, 0, 0)
        }
    }
    
    // Extension functions for data conversion
    private fun DataSnapshot.toRideBuddy(): RideBuddy? {
        return try {
            RideBuddy(
                userId = child("userId").getValue(String::class.java) ?: return null,
                displayName = child("displayName").getValue(String::class.java) ?: "Unknown",
                profileImageUrl = child("profileImageUrl").getValue(String::class.java),
                totalRidesTogether = child("totalRidesTogether").getValue(Int::class.java) ?: 0,
                lastRideDate = child("lastRideDate").getValue(Long::class.java) ?: 0,
                averageRating = child("averageRating").getValue(Float::class.java) ?: 0f,
                preferredPickupLocations = (child("preferredPickupLocations").value as? List<String>) ?: emptyList(),
                commonDestinations = (child("commonDestinations").value as? List<String>) ?: emptyList(),
                isActive = child("isActive").getValue(Boolean::class.java) ?: true,
                addedDate = child("addedDate").getValue(Long::class.java) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to parse RideBuddy", e)
            null
        }
    }
    
    private fun DataSnapshot.toInvitation(): RideBuddyInvitation? {
        return try {
            RideBuddyInvitation(
                invitationId = child("invitationId").getValue(String::class.java) ?: return null,
                fromUserId = child("fromUserId").getValue(String::class.java) ?: return null,
                fromUserName = child("fromUserName").getValue(String::class.java) ?: "Unknown",
                toUserId = child("toUserId").getValue(String::class.java) ?: return null,
                toUserName = child("toUserName").getValue(String::class.java) ?: "Unknown",
                message = child("message").getValue(String::class.java) ?: "",
                timestamp = child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                status = InvitationStatus.valueOf(child("status").getValue(String::class.java) ?: "PENDING"),
                groupId = child("groupId").getValue(String::class.java)
            )
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to parse RideBuddyInvitation", e)
            null
        }
    }
    
    private fun DataSnapshot.toRegularGroup(): RegularGroup? {
        return try {
            RegularGroup(
                groupId = child("groupId").getValue(String::class.java) ?: return null,
                groupName = child("groupName").getValue(String::class.java) ?: "Unnamed Group",
                members = (child("members").value as? List<String>) ?: emptyList(),
                creatorId = child("creatorId").getValue(String::class.java) ?: "",
                commonRoute = child("commonRoute").getValue(String::class.java) ?: "",
                preferredTimes = (child("preferredTimes").value as? List<String>) ?: emptyList(),
                isActive = child("isActive").getValue(Boolean::class.java) ?: true,
                totalTrips = child("totalTrips").getValue(Int::class.java) ?: 0,
                createdDate = child("createdDate").getValue(Long::class.java) ?: System.currentTimeMillis(),
                description = child("description").getValue(String::class.java) ?: ""
            )
        } catch (e: Exception) {
            Log.e("RideBuddyService", "Failed to parse RegularGroup", e)
            null
        }
    }
    
    // Data class extension functions for Firebase conversion
    private fun RideBuddy.toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "profileImageUrl" to profileImageUrl,
        "totalRidesTogether" to totalRidesTogether,
        "lastRideDate" to lastRideDate,
        "averageRating" to averageRating,
        "preferredPickupLocations" to preferredPickupLocations,
        "commonDestinations" to commonDestinations,
        "isActive" to isActive,
        "addedDate" to addedDate
    )
    
    private fun RideBuddyInvitation.toMap(): Map<String, Any?> = mapOf(
        "invitationId" to invitationId,
        "fromUserId" to fromUserId,
        "fromUserName" to fromUserName,
        "toUserId" to toUserId,
        "toUserName" to toUserName,
        "message" to message,
        "timestamp" to timestamp,
        "status" to status.name,
        "groupId" to groupId
    )
    
    private fun RegularGroup.toMap(): Map<String, Any?> = mapOf(
        "groupId" to groupId,
        "groupName" to groupName,
        "members" to members,
        "creatorId" to creatorId,
        "commonRoute" to commonRoute,
        "preferredTimes" to preferredTimes,
        "isActive" to isActive,
        "totalTrips" to totalTrips,
        "createdDate" to createdDate,
        "description" to description
    )
}
