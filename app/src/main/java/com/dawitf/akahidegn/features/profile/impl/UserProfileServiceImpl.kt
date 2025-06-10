package com.dawitf.akahidegn.features.profile.impl

import android.net.Uri
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.features.profile.*
import com.dawitf.akahidegn.security.SecurityService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
// Removed Achievement import - achievement system simplified

@Singleton
class UserProfileServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val analyticsService: AnalyticsService,
    private val securityService: SecurityService
) : UserProfileService {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val RIDES_COLLECTION = "rides"
        private const val ACHIEVEMENTS_COLLECTION = "achievements"
        private const val BADGES_COLLECTION = "badges"
        private const val FRIENDS_COLLECTION = "friends"
        private const val REFERRALS_COLLECTION = "referrals"
        private const val USER_PREFERENCES_COLLECTION = "userPreferences"
        private const val CARBON_FOOTPRINT_COLLECTION = "carbonFootprint"
        
        private const val PROFILE_PHOTOS_PATH = "profile_photos"
    }
    
    override suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return Result.Error(AppError.ValidationError.ResourceNotFound("User profile not found"))
            }
            
            val profile = UserProfile(
                id = userId,
                firstName = userDoc.getString("firstName") ?: "",
                lastName = userDoc.getString("lastName") ?: "",
                email = userDoc.getString("email") ?: "",
                phoneNumber = userDoc.getString("phoneNumber") ?: "",
                profilePhotoUrl = userDoc.getString("profilePhotoUrl"),
                dateOfBirth = userDoc.getLong("dateOfBirth"),
                gender = userDoc.getString("gender")?.let { Gender.valueOf(it) },
                joinDate = userDoc.getLong("joinDate") ?: System.currentTimeMillis(),
                isVerified = userDoc.getBoolean("isVerified") ?: false,
                verificationLevel = userDoc.getString("verificationLevel")?.let { 
                    VerificationLevel.valueOf(it) 
                } ?: VerificationLevel.BASIC,
                rating = userDoc.getDouble("rating")?.toFloat() ?: 0f,
                totalRating = userDoc.getLong("totalRating")?.toInt() ?: 0,
                emergencyContact = userDoc.get("emergencyContact") as? EmergencyContact,
                homeAddress = userDoc.get("homeAddress") as? Address,
                workAddress = userDoc.get("workAddress") as? Address,
                preferredPaymentMethod = userDoc.getString("preferredPaymentMethod")?.let { 
                    PaymentMethod.valueOf(it) 
                },
                languages = userDoc.get("languages") as? List<String> ?: emptyList(),
                bio = userDoc.getString("bio"),
                interests = userDoc.get("interests") as? List<String> ?: emptyList()
            )
            
            Result.Success(profile)
        } catch (e: Exception) {
            analyticsService.logError(e, "get_user_profile_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to get user profile"))
        }
    }
    
    override suspend fun updateUserProfile(profile: UserProfileUpdate): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            // Validate input
            profile.phoneNumber?.let { phone ->
                if (!securityService.validateInput(phone, "phone")) {
                    return Result.Error(AppError.ValidationError.InvalidInput("Invalid phone number format"))
                }
            }
            
            val updateData = mutableMapOf<String, Any?>()
            profile.firstName?.let { updateData["firstName"] = it }
            profile.lastName?.let { updateData["lastName"] = it }
            profile.phoneNumber?.let { updateData["phoneNumber"] = it }
            profile.dateOfBirth?.let { updateData["dateOfBirth"] = it }
            profile.gender?.let { updateData["gender"] = it.name }
            profile.emergencyContact?.let { updateData["emergencyContact"] = it }
            profile.homeAddress?.let { updateData["homeAddress"] = it }
            profile.workAddress?.let { updateData["workAddress"] = it }
            profile.preferredPaymentMethod?.let { updateData["preferredPaymentMethod"] = it.name }
            profile.languages?.let { updateData["languages"] = it }
            profile.bio?.let { updateData["bio"] = it }
            profile.interests?.let { updateData["interests"] = it }
            
            updateData["lastUpdated"] = System.currentTimeMillis()
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updateData)
                .await()
            
            analyticsService.trackEvent("user_profile_updated", mapOf(
                "fields_updated" to updateData.keys.joinToString(",")
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            analyticsService.logError(e, "update_user_profile_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update profile"))
        }
    }
    
    override suspend fun uploadProfilePhoto(photoUri: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val photoRef = storage.reference
                .child(PROFILE_PHOTOS_PATH)
                .child("$userId.jpg")
            
            val uri = Uri.parse(photoUri)
            val uploadTask = photoRef.putFile(uri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            
            // Update user profile with photo URL
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("profilePhotoUrl", downloadUrl)
                .await()
            
            analyticsService.trackEvent("profile_photo_uploaded", emptyMap())
            
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            analyticsService.logError(e, "upload_profile_photo_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to upload photo"))
        }
    }
    
    override fun getRideStats(): Flow<RideStatistics> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val ridesSnapshot = firestore.collection(RIDES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val rides = ridesSnapshot.documents
            val totalRides = rides.size
            val completedRides = rides.count { it.getString("status") == "COMPLETED" }
            val cancelledRides = rides.count { it.getString("status") == "CANCELLED" }
            
            val totalDistance = rides.sumOf { it.getDouble("distance") ?: 0.0 }
            val totalSpent = rides.sumOf { it.getDouble("fare") ?: 0.0 }
            val totalTime = rides.sumOf { it.getLong("duration") ?: 0L }.toInt()
            
            // Calculate ratings
            val ratings = rides.mapNotNull { it.getDouble("rating")?.toFloat() }
            val averageRating = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
            
            // Calculate carbon saved (estimated)
            val carbonSaved = totalDistance * 0.2 // 0.2 kg CO2 per km saved
            
            // Most frequent destination
            val destinations = rides.mapNotNull { it.getString("destination") }
            val favoriteDestination = destinations.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            
            // Monthly ride distribution
            val monthlyRides = rides.groupBy { ride ->
                val timestamp = ride.getLong("timestamp") ?: 0L
                java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(timestamp))
            }.mapValues { it.value.size }
            
            val stats = RideStatistics(
                totalRides = totalRides,
                totalDistance = totalDistance,
                totalSpent = totalSpent,
                averageRating = averageRating,
                totalTimeSaved = (totalDistance * 2).toLong(), // Estimated time saved vs walking
                carbonSaved = carbonSaved
            )
            
            emit(stats)
        } catch (e: Exception) {
            analyticsService.logError(e, "get_ride_stats_failed")
        }
    }
    
    override fun getRideHistory(): Flow<List<RideHistoryItem>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(RIDES_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val rides = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        RideHistoryItem(
                            id = doc.id,
                            date = doc.getLong("timestamp") ?: 0L,
                            from = doc.getString("pickup") ?: "",
                            to = doc.getString("destination") ?: "",
                            distance = doc.getDouble("distance") ?: 0.0,
                            duration = doc.getLong("duration")?.toInt() ?: 0,
                            fare = doc.getDouble("fare") ?: 0.0,
                            status = RideStatus.valueOf(doc.getString("status") ?: "COMPLETED"),
                            driverName = doc.getString("driverName"),
                            driverRating = doc.getDouble("driverRating")?.toFloat(),
                            vehicleInfo = doc.getString("vehicleInfo"),
                            paymentMethod = PaymentMethod.valueOf(
                                doc.getString("paymentMethod") ?: "CASH"
                            ),
                            rideType = RideType.valueOf(
                                doc.getString("rideType") ?: "REGULAR"
                            ),
                            groupSize = doc.getLong("groupSize")?.toInt() ?: 1,
                            carbonSaved = (doc.getDouble("distance") ?: 0.0) * 0.2
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(rides)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun getRideDetails(rideId: String): Result<RideDetails> {
        return try {
            val rideDoc = firestore.collection(RIDES_COLLECTION)
                .document(rideId)
                .get()
                .await()
            
            if (!rideDoc.exists()) {
                return Result.Error(AppError.ValidationError.ResourceNotFound("Ride not found"))
            }
            
            val details = RideDetails(
                id = rideId,
                date = rideDoc.getLong("timestamp") ?: 0L,
                pickupLocation = rideDoc.get("pickupLocation") as? LocationInfo ?: LocationInfo("", 0.0, 0.0),
                dropoffLocation = rideDoc.get("dropoffLocation") as? LocationInfo ?: LocationInfo("", 0.0, 0.0),
                distance = rideDoc.getDouble("distance") ?: 0.0,
                duration = rideDoc.getLong("duration")?.toInt() ?: 0,
                fare = rideDoc.get("fareBreakdown") as? FareBreakdown ?: FareBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                status = RideStatus.valueOf(rideDoc.getString("status") ?: "COMPLETED"),
                driver = rideDoc.get("driver") as? DriverInfo,
                vehicle = rideDoc.get("vehicle") as? VehicleInfo,
                rating = rideDoc.get("rating") as? RideRating,
                receipt = rideDoc.get("receipt") as? RideReceipt
            )
            
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to get ride details"))
        }
    }
    
    // Achievement system removed for simplicity
    // override fun getUserAchievements(): Flow<List<Achievement>> = ...
    
    // Badge system removed for simplicity  
    // override fun getBadgeProgress(): Flow<List<BadgeProgress>> = ...
    
    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(USER_PREFERENCES_COLLECTION)
                .document(userId)
                .set(preferences)
                .await()
            
            analyticsService.trackEvent("user_preferences_updated", mapOf(
                "theme" to preferences.theme,
                "language" to preferences.language
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update preferences"))
        }
    }
    
    override suspend fun getUserPreferences(): Result<UserPreferences> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val prefsDoc = firestore.collection(USER_PREFERENCES_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (prefsDoc.exists()) {
                val preferences = prefsDoc.toObject(UserPreferences::class.java)
                    ?: getDefaultPreferences()
                Result.Success(preferences)
            } else {
                Result.Success(getDefaultPreferences())
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to get preferences"))
        }
    }
    
    override fun getFriends(): Flow<List<Friend>> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val friendsSnapshot = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            
            val friends = friendsSnapshot.documents.mapNotNull { doc ->
                try {
                    Friend(
                        id = doc.getString("friendId") ?: "",
                        name = doc.getString("friendName") ?: "",
                        photoUrl = doc.getString("profilePhotoUrl")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(friends)
        } catch (e: Exception) {
            analyticsService.logError(e, "get_friends_failed")
        }
    }
    
    override suspend fun sendFriendRequest(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            if (currentUserId == userId) {
                return Result.Error(AppError.ValidationError.InvalidInput("Cannot send friend request to yourself"))
            }
            
            firestore.collection("friend_requests")
                .add(mapOf(
                    "fromUserId" to currentUserId,
                    "toUserId" to userId,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            analyticsService.trackEvent("friend_request_sent", emptyMap())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to send friend request"))
        }
    }
    
    // Social methods removed for simplification
    /*
    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            // Update request status
            firestore.collection("friend_requests")
                .document(requestId)
                .update("status", "ACCEPTED")
                .await()
            
            // Create friendship records
            val requestDoc = firestore.collection("friend_requests")
                .document(requestId)
                .get()
                .await()
            
            val fromUserId = requestDoc.getString("fromUserId") ?: ""
            
            // Add to both users' friend collections
            firestore.collection(FRIENDS_COLLECTION)
                .add(mapOf(
                    "userId" to currentUserId,
                    "friendId" to fromUserId,
                    "status" to "ACCEPTED",
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            firestore.collection(FRIENDS_COLLECTION)
                .add(mapOf(
                    "userId" to fromUserId,
                    "friendId" to currentUserId,
                    "status" to "ACCEPTED",
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            analyticsService.trackEvent("friend_request_accepted", emptyMap())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to accept friend request"))
        }
    }

    override suspend fun generateReferralCode(): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val referralCode = generateUniqueReferralCode()
            
            firestore.collection(REFERRALS_COLLECTION)
                .document(userId)
                .set(mapOf(
                    "userId" to userId,
                    "referralCode" to referralCode,
                    "createdAt" to System.currentTimeMillis(),
                    "totalReferrals" to 0,
                    "successfulReferrals" to 0,
                    "totalEarnings" to 0.0
                ))
                .await()
            
            Result.Success(referralCode)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to generate referral code"))
        }
    }
    */
    
    // Referral system removed for simplicity
    // override fun getReferralStats(): Flow<ReferralStats> = ...
    
    // Carbon footprint tracking removed for simplicity
    
    // Carbon footprint tracking removed for simplicity
    // override fun getCarbonFootprintData(): Flow<CarbonFootprintData> = ...
    
    // Private helper methods
    
    private fun calculateRidingStreak(rides: List<com.google.firebase.firestore.DocumentSnapshot>): Int {
        // Calculate consecutive days with rides
        val dates = rides.mapNotNull { doc ->
            doc.getLong("timestamp")?.let { timestamp ->
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }.distinct().sorted()
        
        if (dates.isEmpty()) return 0
        
        var streak = 1
        for (i in dates.size - 1 downTo 1) {
            val current = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[i])
            val previous = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[i - 1])
            
            val diffInDays = ((current?.time ?: 0) - (previous?.time ?: 0)) / (24 * 60 * 60 * 1000)
            
            if (diffInDays == 1L) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun getDefaultPreferences(): UserPreferences {
        return UserPreferences(
            notifications = NotificationPreferences(),
            privacy = PrivacyPreferences()
        )
    }
    
    private fun generateUniqueReferralCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
}
