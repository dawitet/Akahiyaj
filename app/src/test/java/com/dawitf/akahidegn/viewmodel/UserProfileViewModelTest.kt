package com.dawitf.akahidegn.viewmodel

import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.domain.model.*
import com.dawitf.akahidegn.domain.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class UserProfileViewModelTest {

    
    private val mockRepository = mockk<UserProfileRepository>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initial state should be correct`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isUpdating)
        assertFalse(state.isUploadingImage)
        assertNull(state.userProfile)
        assertTrue(state.achievements.isEmpty())
        assertTrue(state.reviews.isEmpty())
        assertTrue(state.tripHistory.isEmpty())
        assertNull(state.analytics)
        assertNull(state.error)
    }

    @Test
    fun `load user profile should update state on success`() = runTest {
        // Given
        val userId = "user123"
        val mockProfile = createMockUserProfile(userId)
        val mockAchievements = listOf(createMockAchievement())
        val mockReviews = listOf(createMockReview())
        val mockTrips = listOf(createMockTripHistory())
        val mockAnalytics = createMockUserAnalytics(userId)

        coEvery { mockRepository.getUserProfile(userId) } returns Result.Success(mockProfile)
        coEvery { mockRepository.getUserAchievements(userId) } returns Result.Success(mockAchievements)
        coEvery { mockRepository.getUserReviews(userId) } returns Result.Success(mockReviews)
        coEvery { mockRepository.getUserTripHistory(userId) } returns Result.Success(mockTrips)
        coEvery { mockRepository.getUserAnalytics(userId) } returns Result.Success(mockAnalytics)

        // When
        viewModel.loadUserProfile(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockProfile, state.userProfile)
        assertEquals(mockAchievements, state.achievements)
        assertEquals(mockReviews, state.reviews)
        assertEquals(mockTrips, state.tripHistory)
        assertEquals(mockAnalytics, state.analytics)
        assertNull(state.error)
    }

    @Test
    fun `load user profile should handle errors`() = runTest {
        // Given
        val userId = "user123"
        val errorMessage = "Failed to load profile"
        
        coEvery { mockRepository.getUserProfile(userId) } returns Result.Error(Exception(errorMessage))
        coEvery { mockRepository.getUserAchievements(userId) } returns Result.Success(emptyList())
        coEvery { mockRepository.getUserReviews(userId) } returns Result.Success(emptyList())
        coEvery { mockRepository.getUserTripHistory(userId) } returns Result.Success(emptyList())
        coEvery { mockRepository.getUserAnalytics(userId) } returns Result.Success(createMockUserAnalytics(userId))

        // When
        viewModel.loadUserProfile(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `update user profile should update state on success`() = runTest {
        // Given
        val updatedProfile = createMockUserProfile("user123").copy(name = "Updated Name")
        
        coEvery { mockRepository.updateUserProfile(updatedProfile) } returns Result.Success(updatedProfile)

        // When
        viewModel.updateUserProfile(updatedProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isUpdating)
        assertEquals(updatedProfile, state.userProfile)
        assertNull(state.error)
    }

    @Test
    fun `upload profile image should update state on success`() = runTest {
        // Given
        val imageUri = "file://test.jpg"
        val imageUrl = "https://storage.firebase.com/test.jpg"
        val userProfile = createMockUserProfile("user123")
        
        // Set initial state
        viewModel.loadUserProfile("user123")
        coEvery { mockRepository.getUserProfile("user123") } returns Result.Success(userProfile)
        coEvery { mockRepository.getUserAchievements("user123") } returns Result.Success(emptyList())
        coEvery { mockRepository.getUserReviews("user123") } returns Result.Success(emptyList())
        coEvery { mockRepository.getUserTripHistory("user123") } returns Result.Success(emptyList())
        coEvery { mockRepository.getUserAnalytics("user123") } returns Result.Success(createMockUserAnalytics("user123"))
        
        coEvery { mockRepository.uploadProfileImage(imageUri) } returns Result.Success(imageUrl)
        coEvery { mockRepository.updateUserProfile(any()) } returns Result.Success(userProfile.copy(profileImageUrl = imageUrl))
        
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.uploadProfileImage(imageUri)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val profileImage = viewModel.profileImage.value
        assertEquals(imageUrl, profileImage)
        
        val state = viewModel.uiState.value
        assertFalse(state.isUploadingImage)
        assertNull(state.error)
    }

    @Test
    fun `unlock achievement should add to achievements list`() = runTest {
        // Given
        val achievementId = "first_trip"
        val mockAchievement = createMockAchievement(achievementId)
        
        coEvery { mockRepository.unlockAchievement(achievementId) } returns Result.Success(mockAchievement)

        // When
        viewModel.unlockAchievement(achievementId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.achievements.contains(mockAchievement))
    }

    @Test
    fun `submit review should add to reviews list`() = runTest {
        // Given
        val review = createMockReview()
        
        coEvery { mockRepository.submitReview(review) } returns Result.Success(review)

        // When
        viewModel.submitReview(review)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.reviews.contains(review))
    }

    @Test
    fun `clear error should reset error state`() {
        // Given - simulate an error state
        viewModel.loadUserProfile("invalid")
        coEvery { mockRepository.getUserProfile("invalid") } returns Result.Error(Exception("Error"))
        
        // When
        viewModel.clearError()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    private fun createMockUserProfile(userId: String): UserProfile {
        return UserProfile(
            userId = userId,
            name = "Test User",
            email = "test@example.com",
            phoneNumber = "+251911123456",
            profileImageUrl = null,
            bio = "Test bio",
            rating = 4.5,
            totalRatings = 10,
            isDriver = false,
            driverInfo = null,
            preferences = UserPreferences(
                notificationsEnabled = true,
                locationSharingEnabled = true,
                autoJoinEnabled = false,
                preferredLanguage = "en",
                theme = "system",
                soundEnabled = true,
                vibrationEnabled = true
            ),
            createdAt = System.currentTimeMillis() - 86400000,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createMockAchievement(id: String = "test_achievement"): UserAchievement {
        return UserAchievement(
            id = "achievement_$id",
            userId = "user123",
            achievementId = id,
            title = "Test Achievement",
            description = "Test achievement description",
            iconUrl = "test_icon",
            category = AchievementCategory.MILESTONE,
            rarity = AchievementRarity.COMMON,
            points = 10,
            isUnlocked = true,
            unlockedAt = System.currentTimeMillis()
        )
    }

    private fun createMockReview(): UserReview {
        return UserReview(
            id = "review123",
            reviewerId = "reviewer123",
            revieweeId = "user123",
            tripId = "trip123",
            rating = 5.0,
            comment = "Great trip!",
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createMockTripHistory(): TripHistoryItem {
        return TripHistoryItem(
            id = "trip123",
            destination = "Bole",
            date = System.currentTimeMillis(),
            duration = 30,
            distance = 15.5,
            cost = 50.0,
            rating = 4.5,
            role = TripRole.PASSENGER,
            driverName = "Test Driver",
            passengerCount = 3
        )
    }

    private fun createMockUserAnalytics(userId: String): UserAnalytics {
        return UserAnalytics(
            userId = userId,
            totalTripsAsPassenger = 25,
            totalTripsAsDriver = 5,
            totalDistanceTraveled = 500.0,
            averageRating = 4.5,
            totalRatings = 20,
            achievementsUnlocked = 8,
            carbonFootprintSaved = 125.0,
            favoriteDestinations = listOf("Bole", "Megenagna", "Piassa"),
            lastUpdated = System.currentTimeMillis()
        )
    }
}
