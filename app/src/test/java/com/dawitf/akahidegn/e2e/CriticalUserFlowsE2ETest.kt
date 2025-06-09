package com.dawitf.akahidegn.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.dawitf.akahidegn.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for critical user flows in the Akahiyaj app
 * Tests complete user journeys from start to finish
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CriticalUserFlowsE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testCompleteUserOnboardingFlow() {
        // Test the complete user onboarding experience
        
        // 1. App starts with name input screen
        composeTestRule.onNodeWithText("Enter your name")
            .assertIsDisplayed()
        
        // 2. User enters name
        composeTestRule.onNodeWithTag("NameInput")
            .performTextInput("Test User")
        
        // 3. User submits name
        composeTestRule.onNodeWithText("Continue")
            .performClick()
        
        // 4. User is taken to main screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Main Content")
            .assertIsDisplayed()
        
        // 5. Location permission is handled (granted via rule)
        // App should show groups or location-based content
        composeTestRule.onNodeWithTag("GroupsList")
            .assertIsDisplayed()
    }

    @Test
    fun testRideCreationFlow() = runBlocking {
        // Complete ride creation flow
        completeOnboarding()
        
        // 1. Navigate to create ride
        composeTestRule.onNodeWithContentDescription("Create Ride")
            .performClick()
        
        // 2. Handle ad prompt
        composeTestRule.onNodeWithText("OK")
            .performClick()
        
        // 3. Enter destination
        composeTestRule.onNodeWithTag("DestinationInput")
            .performTextInput("Test Destination")
        
        // 4. Confirm ride creation
        composeTestRule.onNodeWithText("Create")
            .performClick()
        
        // 5. Verify ride was created
        delay(2000) // Wait for creation
        composeTestRule.onNodeWithText("Test Destination")
            .assertIsDisplayed()
    }

    @Test
    fun testSocialInteractionFlow() = runBlocking {
        // Test complete social interaction flow
        completeOnboarding()
        
        // 1. Navigate to social screen
        composeTestRule.onNodeWithContentDescription("Social")
            .performClick()
        
        // 2. Find a ride buddy
        composeTestRule.onNodeWithText("Find Ride Buddies")
            .performClick()
        
        // 3. Send a buddy request
        composeTestRule.onAllNodesWithText("Send Request")
            .onFirst()
            .performClick()
        
        // 4. Navigate to chat
        composeTestRule.onNodeWithText("Group Chat")
            .performClick()
        
        // 5. Send a message
        composeTestRule.onNodeWithTag("MessageInput")
            .performTextInput("Hello everyone!")
        
        composeTestRule.onNodeWithContentDescription("Send Message")
            .performClick()
        
        // 6. Verify message was sent
        composeTestRule.onNodeWithText("Hello everyone!")
            .assertIsDisplayed()
    }

    @Test
    fun testUserProfileManagementFlow() = runBlocking {
        // Test complete profile management flow
        completeOnboarding()
        
        // 1. Navigate to profile
        composeTestRule.onNodeWithContentDescription("Profile")
            .performClick()
        
        // 2. Edit profile information
        composeTestRule.onNodeWithText("Edit Profile")
            .performClick()
        
        // 3. Update bio
        composeTestRule.onNodeWithTag("BioInput")
            .performTextClearance()
        composeTestRule.onNodeWithTag("BioInput")
            .performTextInput("Updated bio for testing")
        
        // 4. Save changes
        composeTestRule.onNodeWithText("Save")
            .performClick()
        
        // 5. Verify changes were saved
        delay(1000)
        composeTestRule.onNodeWithText("Updated bio for testing")
            .assertIsDisplayed()
        
        // 6. Navigate to settings from profile
        composeTestRule.onNodeWithText("Settings")
            .performClick()
        
        // 7. Change theme
        composeTestRule.onNodeWithText("Dark Mode")
            .performClick()
        
        // 8. Verify theme changed (check for dark theme indicators)
        composeTestRule.onNodeWithTag("DarkThemeIndicator")
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityFlow() = runBlocking {
        // Test accessibility features flow
        completeOnboarding()
        
        // 1. Navigate to accessibility settings
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        
        composeTestRule.onNodeWithText("Accessibility")
            .performClick()
        
        // 2. Enable high contrast
        composeTestRule.onNodeWithText("High Contrast")
            .performClick()
        
        // 3. Enable large text
        composeTestRule.onNodeWithText("Large Text")
            .performClick()
        
        // 4. Enable voice guidance
        composeTestRule.onNodeWithText("Voice Guidance")
            .performClick()
        
        // 5. Navigate back and verify accessibility features are active
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        // 6. Verify high contrast is applied
        composeTestRule.onNodeWithTag("HighContrastIndicator")
            .assertIsDisplayed()
    }

    @Test
    fun testOfflineModeFlow() = runBlocking {
        // Test offline functionality
        completeOnboarding()
        
        // 1. Enable offline mode (simulate)
        // This would require mocking network connectivity
        
        // 2. Verify offline indicators
        composeTestRule.onNodeWithText("Offline Mode")
            .assertIsDisplayed()
        
        // 3. Try to access cached content
        composeTestRule.onNodeWithText("Cached Groups")
            .assertIsDisplayed()
        
        // 4. Try to create content offline
        composeTestRule.onNodeWithContentDescription("Create Ride")
            .performClick()
        
        // 5. Verify offline creation queues the request
        composeTestRule.onNodeWithText("Saved for later")
            .assertIsDisplayed()
    }

    @Test
    fun testMultiLanguageFlow() = runBlocking {
        // Test multi-language functionality
        completeOnboarding()
        
        // 1. Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        
        // 2. Change language to Amharic
        composeTestRule.onNodeWithText("Language")
            .performClick()
        
        composeTestRule.onNodeWithText("አማርኛ")
            .performClick()
        
        // 3. Verify UI is translated
        composeTestRule.onNodeWithText("መቼት") // Settings in Amharic
            .assertIsDisplayed()
        
        // 4. Navigate through different screens
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Profile")
            .performClick()
        
        // 5. Verify profile screen is also translated
        composeTestRule.onNodeWithText("መገለጫ") // Profile in Amharic
            .assertIsDisplayed()
    }

    @Test
    fun testPerformanceOptimizedFlow() = runBlocking {
        // Test that performance optimizations don't break functionality
        completeOnboarding()
        
        // 1. Trigger image loading
        composeTestRule.onNodeWithTag("UserAvatars")
            .assertIsDisplayed()
        
        // 2. Navigate rapidly between screens to test caching
        repeat(5) {
            composeTestRule.onNodeWithContentDescription("Profile")
                .performClick()
            
            composeTestRule.onNodeWithContentDescription("Navigate Back")
                .performClick()
            
            delay(500)
        }
        
        // 3. Verify app remains responsive
        composeTestRule.onNodeWithText("Main Content")
            .assertIsDisplayed()
        
        // 4. Create multiple groups to test memory management
        repeat(3) { index ->
            composeTestRule.onNodeWithContentDescription("Create Ride")
                .performClick()
            
            composeTestRule.onNodeWithText("OK")
                .performClick()
            
            composeTestRule.onNodeWithTag("DestinationInput")
                .performTextInput("Destination $index")
            
            composeTestRule.onNodeWithText("Create")
                .performClick()
            
            delay(1000)
        }
        
        // 5. Verify all groups are displayed
        composeTestRule.onNodeWithText("Destination 0")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Destination 2")
            .assertIsDisplayed()
    }

    @Test
    fun testErrorRecoveryFlow() = runBlocking {
        // Test error handling and recovery
        completeOnboarding()
        
        // 1. Simulate network error during group creation
        // This requires mocking network failures
        
        // 2. Try to create a ride
        composeTestRule.onNodeWithContentDescription("Create Ride")
            .performClick()
        
        composeTestRule.onNodeWithText("OK")
            .performClick()
        
        composeTestRule.onNodeWithTag("DestinationInput")
            .performTextInput("Test Destination")
        
        composeTestRule.onNodeWithText("Create")
            .performClick()
        
        // 3. Verify error is handled gracefully
        composeTestRule.onNodeWithText("Unable to create ride")
            .assertIsDisplayed()
        
        // 4. Verify retry mechanism
        composeTestRule.onNodeWithText("Retry")
            .performClick()
        
        // 5. Verify app continues to function
        composeTestRule.onNodeWithText("Main Content")
            .assertIsDisplayed()
    }

    /**
     * Helper function to complete onboarding process
     */
    private fun completeOnboarding() {
        // Skip onboarding if already completed
        if (composeTestRule.onAllNodesWithText("Enter your name").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("NameInput")
                .performTextInput("Test User")
            
            composeTestRule.onNodeWithText("Continue")
                .performClick()
            
            composeTestRule.waitForIdle()
        }
    }
}
