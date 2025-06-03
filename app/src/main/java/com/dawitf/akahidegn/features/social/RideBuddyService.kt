package com.dawitf.akahidegn.features.social

import kotlinx.coroutines.flow.Flow

/**
 * Service for managing ride buddies - users who frequently ride together
 * and form regular groups for consistent commuting
 */
interface RideBuddyService {
    
    /**
     * Data classes for ride buddy system
     */
    data class RideBuddy(
        val userId: String,
        val displayName: String,
        val profileImageUrl: String? = null,
        val totalRidesTogether: Int = 0,
        val lastRideDate: Long = 0,
        val averageRating: Float = 0f,
        val preferredPickupLocations: List<String> = emptyList(),
        val commonDestinations: List<String> = emptyList(),
        val isActive: Boolean = true,
        val addedDate: Long = System.currentTimeMillis()
    )
    
    data class RideBuddyInvitation(
        val invitationId: String,
        val fromUserId: String,
        val fromUserName: String,
        val toUserId: String,
        val toUserName: String,
        val message: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val status: InvitationStatus = InvitationStatus.PENDING,
        val groupId: String? = null // Reference to the group where they met
    )
    
    data class RegularGroup(
        val groupId: String,
        val groupName: String,
        val members: List<String>, // User IDs
        val creatorId: String,
        val commonRoute: String,
        val preferredTimes: List<String>, // e.g., "08:00", "17:30"
        val isActive: Boolean = true,
        val totalTrips: Int = 0,
        val createdDate: Long = System.currentTimeMillis(),
        val description: String = ""
    )
    
    data class RideBuddySuggestion(
        val userId: String,
        val displayName: String,
        val ridesTogether: Int,
        val suggestionReason: String,
        val confidence: Float // 0.0 to 1.0
    )
    
    enum class InvitationStatus {
        PENDING, ACCEPTED, DECLINED, EXPIRED
    }
    
    /**
     * Core ride buddy management
     */
    suspend fun sendBuddyInvitation(
        toUserId: String,
        toUserName: String,
        message: String = "",
        groupId: String? = null
    ): Result<String>
    
    suspend fun respondToBuddyInvitation(
        invitationId: String,
        accept: Boolean,
        message: String = ""
    ): Result<Boolean>
    
    suspend fun removeBuddy(buddyUserId: String): Result<Boolean>
    
    /**
     * Data access
     */
    fun getRideBuddies(): Flow<List<RideBuddy>>
    
    fun getPendingInvitations(): Flow<List<RideBuddyInvitation>>
    
    fun getSentInvitations(): Flow<List<RideBuddyInvitation>>
    
    /**
     * Regular group management
     */
    suspend fun createRegularGroup(
        groupName: String,
        memberIds: List<String>,
        commonRoute: String,
        preferredTimes: List<String>,
        description: String = ""
    ): Result<String>
    
    suspend fun joinRegularGroup(groupId: String): Result<Boolean>
    
    suspend fun leaveRegularGroup(groupId: String): Result<Boolean>
    
    fun getRegularGroups(): Flow<List<RegularGroup>>
    
    /**
     * Analytics and suggestions
     */
    suspend fun recordRideWithUser(
        otherUserId: String,
        otherUserName: String,
        groupId: String,
        rating: Float? = null
    ): Result<Boolean>
    
    suspend fun getBuddySuggestions(): List<RideBuddySuggestion>
    
    suspend fun updateBuddyRating(buddyUserId: String, rating: Float): Result<Boolean>
    
    /**
     * Search and discovery
     */
    suspend fun searchPotentialBuddies(
        query: String,
        currentLocation: android.location.Location? = null
    ): List<RideBuddy>
    
    suspend fun getBuddiesInArea(
        location: android.location.Location,
        radiusKm: Double = 5.0
    ): List<RideBuddy>
    
    /**
     * Notifications and preferences
     */
    suspend fun updateNotificationPreferences(
        enableInvitations: Boolean,
        enableGroupUpdates: Boolean,
        enableRideReminders: Boolean
    ): Result<Boolean>
    
    suspend fun getFrequentRidePartners(limit: Int = 10): List<RideBuddy>
    
    /**
     * Statistics
     */
    suspend fun getBuddyStats(): BuddyStats
    
    data class BuddyStats(
        val totalBuddies: Int,
        val totalRidesWithBuddies: Int,
        val averageBuddyRating: Float,
        val mostFrequentBuddy: RideBuddy?,
        val regularGroupsCount: Int,
        val monthlyRidesWithBuddies: Int
    )
}
