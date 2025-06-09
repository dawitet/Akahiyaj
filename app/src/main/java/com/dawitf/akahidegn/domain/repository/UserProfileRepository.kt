package com.dawitf.akahidegn.domain.repository

import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.domain.model.UserAchievement
import com.dawitf.akahidegn.domain.model.UserReview
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import com.dawitf.akahidegn.domain.model.UserAnalytics
import com.dawitf.akahidegn.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    suspend fun getUserProfile(userId: String): Result<UserProfile>
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>
    suspend fun uploadProfileImage(imageUri: String): Result<String>
    suspend fun getUserAchievements(userId: String): Result<List<UserAchievement>>
    suspend fun unlockAchievement(achievementId: String): Result<UserAchievement>
    suspend fun getUserReviews(userId: String): Result<List<UserReview>>
    suspend fun submitReview(review: UserReview): Result<UserReview>
    suspend fun getUserTripHistory(userId: String): Result<List<TripHistoryItem>>
    suspend fun getUserAnalytics(userId: String): Result<UserAnalytics>
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<UserPreferences>
    suspend fun getUserPreferences(userId: String): Result<UserPreferences>
    
    // Flow-based methods for real-time updates
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    fun observeUserAchievements(userId: String): Flow<List<UserAchievement>>
    fun observeUserReviews(userId: String): Flow<List<UserReview>>
}
