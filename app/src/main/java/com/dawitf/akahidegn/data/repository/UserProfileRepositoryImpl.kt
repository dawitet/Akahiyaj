package com.dawitf.akahidegn.data.repository

import android.net.Uri
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.data.local.dao.UserPreferencesDao
import com.dawitf.akahidegn.data.mapper.toEntity

import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.domain.model.UserReview
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import com.dawitf.akahidegn.domain.model.UserAnalytics
import com.dawitf.akahidegn.domain.model.UserPreferences
import com.dawitf.akahidegn.domain.model.NotificationSettings
import com.dawitf.akahidegn.domain.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.dawitf.akahidegn.data.repository.model.UserProfileUpdate


@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val userPreferencesDao: UserPreferencesDao
) : UserProfileRepository {

    private val usersCollection = firestore.collection("users")
    private val achievementsCollection = firestore.collection("achievements")
    private val reviewsCollection = firestore.collection("reviews")
    private val tripsCollection = firestore.collection("trips")

    override suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile != null) {
                    Result.Success(profile)
                } else {
                    Result.Error(AppError.ValidationError.NotFound("Profile data is invalid"))
                }
            } else {
                Result.Error(AppError.ValidationError.NotFound("User profile not found"))
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to get user profile"))
        }
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            usersCollection.document(userProfile.userId).set(userProfile).await()
            Result.Success(userProfile)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update profile"))
        }
    }

    override suspend fun uploadProfileImage(imageUri: String): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val imageRef = storage.reference
                .child("profile_images")
                .child("${currentUser.uid}_${System.currentTimeMillis()}.jpg")
            
            val uploadTask = imageRef.putFile(Uri.parse(imageUri)).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Failed to upload profile image"))
        }
    }

    // Achievement methods removed for simplification
    /*
    override suspend fun getUserAchievements(userId: String): Result<List<UserAchievement>> {
        return try {
            val snapshot = achievementsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isUnlocked", true)
                .get()
                .await()
            
            val achievements = snapshot.documents.mapNotNull { document ->
                document.toObject(UserAchievement::class.java)
            }
            
            Result.Success(achievements)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Failed to get user achievements"))
        }
    }

    override suspend fun unlockAchievement(achievementId: String): Result<UserAchievement> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val achievement = UserAchievement(
                id = achievementId,
                title = getAchievementTitle(achievementId),
                description = getAchievementDescription(achievementId),
                iconUrl = getAchievementIcon(achievementId),
                unlockedDate = System.currentTimeMillis(),
                progress = 1f,
                maxProgress = 1f,
                category = getAchievementCategory(achievementId),
                points = 10,
                isUnlocked = true
            )
            
            achievementsCollection.document(UUID.randomUUID().toString()).set(achievement).await()
            
            Result.Success(achievement)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Failed to unlock achievement"))
        }
    }
    */

    override suspend fun getUserReviews(userId: String): Result<List<UserReview>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("revieweeId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { document ->
                document.toObject(UserReview::class.java)
            }
            
            Result.Success(reviews)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to load reviews"))
        }
    }

    override suspend fun submitReview(review: UserReview): Result<UserReview> {
        return try {
            val reviewWithId = review.copy(
                reviewId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis()
            )
            
            reviewsCollection.document(reviewWithId.reviewId).set(reviewWithId).await()
            
            
            
            Result.success(reviewWithId)
        } catch (e: Exception) {
            Result.error(AppError.UnknownError(e.message ?: "Failed to submit review"))
        }
    }

    override suspend fun getUserTripHistory(userId: String): Result<List<TripHistoryItem>> {
        return try {
            val snapshot = tripsCollection
                .whereArrayContains("participantIds", userId)
                .whereEqualTo("status", "completed")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            
            val trips = snapshot.documents.mapNotNull { document ->
                document.toObject(TripHistoryItem::class.java)
            }
            
            Result.success(trips)
        } catch (e: Exception) {
            Result.error(AppError.UnknownError(e.message ?: "Failed to get trip history"))
        }
    }

    override suspend fun getUserAnalytics(userId: String): Result<UserAnalytics> {
        return try {
            // Get trip statistics
            val tripsAsPassenger = tripsCollection
                .whereArrayContains("passengerIds", userId)
                .whereEqualTo("status", "completed")
                .get()
                .await()
                .size()
            
            val tripsAsDriver = tripsCollection
                .whereEqualTo("driverId", userId)
                .whereEqualTo("status", "completed")
                .get()
                .await()
                .size()
            
            // Get rating information
            val reviews = reviewsCollection
                .whereEqualTo("revieweeId", userId)
                .get()
                .await()
            
            val totalRatings = reviews.size()
            val averageRating = if (totalRatings > 0) {
                reviews.documents.mapNotNull { it.getDouble("rating") }.average()
            } else 0.0
            
            // Get achievement count
            val achievementCount = achievementsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isUnlocked", true)
                .get()
                .await()
                .size()
            
            val analytics = UserAnalytics(
                userId = userId,
                totalAppUsage = 0L, // Would need to track from app analytics
                averageSessionLength = 0L, // Would need to track from app analytics  
                searchCount = 0, // Would need to track from search analytics
                groupCreationCount = tripsAsDriver,
                groupJoinCount = tripsAsPassenger,
                messagesSent = 0, // Would need to track from chat analytics
                favoriteDestinations = emptyList(),
                peakUsageHours = emptyList(), // Would need complex analytics
                mostActiveDay = "", // Would need analytics calculation
                conversionRate = 0f, // Would need to calculate
                referralCount = 0, // Would need referral tracking
                reportsMade = 0, // Would need report tracking
                reportsReceived = 0 // Would need report tracking
            )
            
            Result.success(analytics)
        } catch (e: Exception) {
            Result.error(AppError.UnknownError(e.message ?: "Failed to get user analytics"))
        }
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<UserPreferences> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val updates = mapOf(
                "preferences" to preferences,
                "lastActiveDate" to System.currentTimeMillis()
            )
            
            usersCollection.document(currentUser.uid).update(updates).await()
            
            Result.success(preferences)
        } catch (e: Exception) {
            Result.error(AppError.UnknownError(e.message ?: "Failed to update user preferences"))
        }
    }

    override suspend fun getUserPreferences(userId: String): Result<UserPreferences> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                @Suppress("UNCHECKED_CAST")
                val preferences = document.get("preferences") as? Map<String, Any>
                preferences?.let {
                    // Convert map to UserPreferences object
                    val userPreferences = UserPreferences(
                        preferredLanguage = it["preferredLanguage"] as? String ?: "system",
                        notificationSettings = NotificationSettings(), // Would need to map from nested object
                        searchRadius = it["searchRadius"] as? Double ?: 25.0,
                        autoAcceptRadius = it["autoAcceptRadius"] as? Double ?: 5.0,
                        maxPassengers = it["maxPassengers"] as? Int ?: 4,
                        vehicleInfo = null // Would need to map from nested object
                    )
                    Result.success(userPreferences)
                } ?: Result.error(AppError.ValidationError.NotFound("Preferences not found"))
            } else {
                Result.error(AppError.ValidationError.NotFound("User not found"))
            }
        } catch (e: Exception) {
            Result.error(AppError.UnknownError(e.message ?: "Failed to get user preferences"))
        }
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        
        awaitClose { listener.remove() }
    }

    // Achievement observation method removed for simplification
    /*
    override fun observeUserAchievements(userId: String): Flow<List<UserAchievement>> = callbackFlow {
        val listener = achievementsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isUnlocked", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val achievements = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(UserAchievement::class.java)
                } ?: emptyList()
                
                trySend(achievements)
            }
        
        awaitClose { listener.remove() }
    }
    */

    override fun observeUserReviews(userId: String): Flow<List<UserReview>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("revieweeId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val reviews = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(UserReview::class.java)
                } ?: emptyList()
                
                trySend(reviews)
            }
        
        awaitClose { listener.remove() }
    }

    

    // Achievement system removed for simplicity
    // override fun getAchievements(): Flow<List<Achievement>> = ...

    // Carbon footprint tracking removed for simplicity
    // override fun getCarbonFootprint(): Flow<CarbonFootprintData?> = ...

    // Helper methods
    

    // Achievement helper methods
    private fun getAchievementTitle(achievementId: String): String {
        return when (achievementId) {
            "first_trip" -> "First Journey"
            "early_bird" -> "Early Bird"
            "night_owl" -> "Night Owl"
            "eco_warrior" -> "Eco Warrior"
            "social_butterfly" -> "Social Butterfly"
            "reliable_rider" -> "Reliable Rider"
            "helpful_driver" -> "Helpful Driver"
            "speed_demon" -> "Speed Demon"
            "safe_driver" -> "Safe Driver"
            "trip_master" -> "Trip Master"
            else -> "Achievement"
        }
    }

    private fun getAchievementDescription(achievementId: String): String {
        return when (achievementId) {
            "first_trip" -> "Completed your first ride"
            "early_bird" -> "Completed 10 trips before 8 AM"
            "night_owl" -> "Completed 10 trips after 10 PM"
            "eco_warrior" -> "Saved 100kg of CO2 through ride sharing"
            "social_butterfly" -> "Joined 50 different groups"
            "reliable_rider" -> "Maintained 4.8+ rating for 6 months"
            "helpful_driver" -> "Helped 100 passengers reach their destination"
            "speed_demon" -> "Completed 20 trips in a single day"
            "safe_driver" -> "Zero incidents for 1 year"
            "trip_master" -> "Completed 1000 trips"
            else -> "Special achievement unlocked"
        }
    }

    private fun getAchievementIcon(achievementId: String): String {
        return when (achievementId) {
            "first_trip" -> "🚗"
            "early_bird" -> "🌅"
            "night_owl" -> "🌙"
            "eco_warrior" -> "🌱"
            "social_butterfly" -> "🦋"
            "reliable_rider" -> "⭐"
            "helpful_driver" -> "🤝"
            "speed_demon" -> "⚡"
            "safe_driver" -> "🛡️"
            "trip_master" -> "👑"
            else -> "🏆"
        }
    }

    // Achievement methods removed for simplification
    /*
    private fun getAchievementCategory(achievementId: String): com.dawitf.akahidegn.features.profile.AchievementCategory {
        return when (achievementId) {
            "first_trip", "trip_master" -> com.dawitf.akahidegn.features.profile.AchievementCategory.RIDES
            "early_bird", "night_owl" -> com.dawitf.akahidegn.features.profile.AchievementCategory.STREAKS
            "eco_warrior" -> com.dawitf.akahidegn.features.profile.AchievementCategory.ECO_FRIENDLY
            "social_butterfly", "reliable_rider" -> com.dawitf.akahidegn.features.profile.AchievementCategory.SOCIAL
            "helpful_driver", "safe_driver", "speed_demon" -> com.dawitf.akahidegn.features.profile.AchievementCategory.SAVINGS
            else -> com.dawitf.akahidegn.features.profile.AchievementCategory.SPECIAL
        }
    }

    private fun getAchievementRarity(achievementId: String): AchievementRarity {
        return when (achievementId) {
            "first_trip" -> AchievementRarity.COMMON
            "early_bird", "night_owl", "social_butterfly" -> AchievementRarity.UNCOMMON
            "eco_warrior", "reliable_rider", "helpful_driver" -> AchievementRarity.RARE
            "speed_demon", "safe_driver", "trip_master" -> AchievementRarity.LEGENDARY
            else -> AchievementRarity.COMMON
        }
    }
    */

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun UserProfileUpdate.toMap(): Map<String, Any?> {
        return mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "emergencyContact" to emergencyContact,
            "homeAddress" to homeAddress,
            "workAddress" to workAddress,
            "preferredPaymentMethod" to preferredPaymentMethod,
            "languages" to languages,
            "bio" to bio,
            "interests" to interests
        ).filterValues { it != null }
    }
}
