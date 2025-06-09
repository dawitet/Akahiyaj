package com.dawitf.akahidegn.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawitf.akahidegn.MainActivity
import com.dawitf.akahidegn.analytics.AnalyticsManager
import com.dawitf.akahidegn.localization.LocalizationManager
import com.dawitf.akahidegn.offline.OfflineManager
import com.dawitf.akahidegn.accessibility.AccessibilityManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for enhanced component navigation and functionality
 * Tests the complete navigation flow between different screens and components
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var localizationManager: LocalizationManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var accessibilityManager: AccessibilityManager

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testMainScreenToUserProfileNavigation() {
        // Wait for app to load
        composeTestRule.waitForIdle()
        
        // Navigate to user profile from main screen
        composeTestRule.onNodeWithContentDescription("Navigate to Profile")
            .performClick()
        
        // Verify user profile screen is displayed
        composeTestRule.onNodeWithText("User Profile")
            .assertIsDisplayed()
        
        // Test back navigation
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        // Verify we're back on main screen
        composeTestRule.onNodeWithText("Main Content")
            .assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenIntegration() {
        composeTestRule.waitForIdle()
        
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Navigate to Settings")
            .performClick()
        
        // Verify settings screen components
        composeTestRule.onNodeWithText("Settings")
            .assertIsDisplayed()
        
        // Test theme toggle
        composeTestRule.onNodeWithText("Theme")
            .assertIsDisplayed()
        
        // Test language selection
        composeTestRule.onNodeWithText("Language")
            .assertIsDisplayed()
    }

    @Test
    fun testSocialScreenIntegration() {
        composeTestRule.waitForIdle()
        
        // Navigate to social screen
        composeTestRule.onNodeWithContentDescription("Navigate to Social")
            .performClick()
        
        // Verify social screen components
        composeTestRule.onNodeWithText("Social Features")
            .assertIsDisplayed()
        
        // Test ride buddy functionality
        composeTestRule.onNodeWithText("Find Ride Buddies")
            .assertIsDisplayed()
        
        // Test chat integration
        composeTestRule.onNodeWithText("Group Chat")
            .assertIsDisplayed()
    }

    @Test
    fun testEnhancedSearchIntegration() {
        composeTestRule.waitForIdle()
        
        // Open enhanced search
        composeTestRule.onNodeWithContentDescription("Enhanced Search")
            .performClick()
        
        // Verify search components
        composeTestRule.onNodeWithText("Search")
            .assertIsDisplayed()
        
        // Test search functionality
        composeTestRule.onNodeWithTag("SearchTextField")
            .performTextInput("Test Search")
        
        // Verify search results
        composeTestRule.onNodeWithTag("SearchResults")
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilitySettingsIntegration() {
        composeTestRule.waitForIdle()
        
        // Navigate to accessibility settings
        composeTestRule.onNodeWithContentDescription("Accessibility Settings")
            .performClick()
        
        // Verify accessibility options
        composeTestRule.onNodeWithText("High Contrast")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Large Text")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Voice Guidance")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Color Blind Support")
            .assertIsDisplayed()
    }

    @Test
    fun testOfflineModeIntegration() {
        composeTestRule.waitForIdle()
        
        // Simulate offline mode
        // This would require mocking network connectivity
        
        // Verify offline indicators
        composeTestRule.onNodeWithText("Offline Mode")
            .assertIsDisplayed()
        
        // Test cached content accessibility
        composeTestRule.onNodeWithText("Cached Groups")
            .assertIsDisplayed()
    }

    @Test
    fun testMultiLanguageIntegration() {
        composeTestRule.waitForIdle()
        
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Navigate to Settings")
            .performClick()
        
        // Change language to Amharic
        composeTestRule.onNodeWithText("Language")
            .performClick()
        
        composeTestRule.onNodeWithText("አማርኛ")
            .performClick()
        
        // Verify UI elements are translated
        composeTestRule.onNodeWithText("መቼት")
            .assertIsDisplayed()
    }

    @Test
    fun testAnalyticsIntegration() {
        composeTestRule.waitForIdle()
        
        // Perform various actions that should trigger analytics
        composeTestRule.onNodeWithContentDescription("Navigate to Profile")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        // Analytics verification would require mocking/testing framework integration
        // In a real implementation, you'd verify analytics events were fired
    }

    @Test
    fun testCompleteUserFlow() {
        composeTestRule.waitForIdle()
        
        // Complete user flow test
        // 1. Start on main screen
        composeTestRule.onNodeWithText("Main Content")
            .assertIsDisplayed()
        
        // 2. Navigate to user profile
        composeTestRule.onNodeWithContentDescription("Navigate to Profile")
            .performClick()
        
        // 3. Go to settings from profile
        composeTestRule.onNodeWithText("Settings")
            .performClick()
        
        // 4. Access accessibility settings
        composeTestRule.onNodeWithText("Accessibility")
            .performClick()
        
        // 5. Enable high contrast
        composeTestRule.onNodeWithText("High Contrast")
            .performClick()
        
        // 6. Navigate back to main
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Navigate Back")
            .performClick()
        
        // 7. Verify we're back on main screen with accessibility enabled
        composeTestRule.onNodeWithText("Main Content")
            .assertIsDisplayed()
    }
}
