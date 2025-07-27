package com.dawitf.akahidegn.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for AnimationComponents
 * These tests capture screenshots of UI components for visual regression testing
 */
@RunWith(AndroidJUnit4::class)
class AnimationComponentsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun successAnimationCard_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                SuccessAnimationCard(
                    isVisible = true,
                    title = "Success Test",
                    subtitle = "This is a screenshot test",
                    onDismiss = {}
                )
            }
        }

        // Wait for animation to complete
        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Success Test").captureToImage()

        // Verify content is displayed
        composeTestRule.onNodeWithText("Success Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a screenshot test").assertIsDisplayed()
        composeTestRule.onNodeWithText("እሺ").assertIsDisplayed()
    }

    @Test
    fun errorAnimationCard_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                ErrorAnimationCard(
                    isVisible = true,
                    title = "Error Test",
                    subtitle = "This is an error screenshot test",
                    onDismiss = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Error Test").captureToImage()

        // Verify content is displayed
        composeTestRule.onNodeWithText("Error Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is an error screenshot test").assertIsDisplayed()
        composeTestRule.onNodeWithText("ሞክር").assertIsDisplayed()
        composeTestRule.onNodeWithText("እሺ").assertIsDisplayed()
    }

    @Test
    fun warningAnimationCard_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                WarningAnimationCard(
                    isVisible = true,
                    title = "Warning Test",
                    subtitle = "This is a warning screenshot test",
                    onDismiss = {},
                    onAction = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Warning Test").captureToImage()

        // Verify content is displayed
        composeTestRule.onNodeWithText("Warning Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a warning screenshot test").assertIsDisplayed()
    }

    @Test
    fun loadingAnimationCard_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                LoadingAnimationCard(
                    isVisible = true,
                    title = "Loading Test",
                    subtitle = "This is a loading screenshot test"
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Loading Test").captureToImage()

        // Verify content is displayed
        composeTestRule.onNodeWithText("Loading Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a loading screenshot test").assertIsDisplayed()
    }

    @Test
    fun animatedCheckmark_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                AnimatedCheckmark(
                    isVisible = true,
                    size = 64.dp
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot of the checkmark
        composeTestRule.onNodeWithContentDescription("Animated checkmark indicator")
            .captureToImage()

        // Verify checkmark is displayed
        composeTestRule.onNodeWithContentDescription("Animated checkmark indicator")
            .assertIsDisplayed()
    }

    @Test
    fun animationSizeVariants_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                Column {
                    AnimationSize.values().forEach { size ->
                        AnimatedCheckmark(
                            isVisible = true,
                            size = size.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot of all size variants
        composeTestRule.onAllNodesWithContentDescription("Animated checkmark indicator")
            .assertCountEquals(3)
    }

    @Test
    fun animationSpeedVariants_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                Column {
                    SuccessAnimationCard(
                        isVisible = true,
                        title = "Slow Animation",
                        animationSpeed = AnimationSpeed.Slow,
                        onDismiss = {}
                    )

                    SuccessAnimationCard(
                        isVisible = true,
                        title = "Normal Animation",
                        animationSpeed = AnimationSpeed.Normal,
                        onDismiss = {}
                    )

                    SuccessAnimationCard(
                        isVisible = true,
                        title = "Fast Animation",
                        animationSpeed = AnimationSpeed.Fast,
                        onDismiss = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Verify all animation speed variants are displayed
        composeTestRule.onNodeWithText("Slow Animation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Normal Animation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fast Animation").assertIsDisplayed()
    }

    @Test
    fun undoSnackbar_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                UndoSnackbar(
                    message = "Test notification dismissed",
                    onUndo = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Test notification dismissed").captureToImage()

        // Verify content is displayed
        composeTestRule.onNodeWithText("Test notification dismissed").assertIsDisplayed()
        composeTestRule.onNodeWithText("UNDO").assertIsDisplayed()
    }

    @Test
    fun animatedNotificationList_screenshot() {
        val notifications = listOf(
            NotificationItem(
                id = "1",
                type = AnimationType.SUCCESS,
                title = "Success Notification",
                subtitle = "This is a success message"
            ),
            NotificationItem(
                id = "2",
                type = AnimationType.ERROR,
                title = "Error Notification",
                subtitle = "This is an error message"
            ),
            NotificationItem(
                id = "3",
                type = AnimationType.WARNING,
                title = "Warning Notification",
                subtitle = "This is a warning message"
            )
        )

        composeTestRule.setContent {
            AkahidegnTheme {
                AnimatedNotificationList(
                    notifications = notifications
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Success Notification").captureToImage()

        // Verify all notifications are displayed
        composeTestRule.onNodeWithText("Success Notification").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error Notification").assertIsDisplayed()
        composeTestRule.onNodeWithText("Warning Notification").assertIsDisplayed()
    }

    @Test
    fun customIconAnimation_screenshot() {
        composeTestRule.setContent {
            AkahidegnTheme {
                SuccessAnimationCard(
                    isVisible = true,
                    title = "Custom Icon Test",
                    subtitle = "Using custom star icon",
                    customIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star icon",
                            tint = Color.Gold,
                            modifier = Modifier.size(64.dp)
                        )
                    },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Take screenshot
        composeTestRule.onNodeWithText("Custom Icon Test").captureToImage()

        // Verify custom icon is displayed
        composeTestRule.onNodeWithContentDescription("Star icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custom Icon Test").assertIsDisplayed()
    }

    @Test
    fun accessibilityProperties_test() {
        composeTestRule.setContent {
            AkahidegnTheme {
                SuccessAnimationCard(
                    isVisible = true,
                    title = "Accessibility Test",
                    subtitle = "Testing accessibility properties",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Test semantic properties
        composeTestRule.onNodeWithContentDescription("Success notification: Accessibility Test")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Success title: Accessibility Test")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Success subtitle: Testing accessibility properties")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Dismiss success notification")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun animationController_interaction_test() {
        val controller = AnimationController()

        composeTestRule.setContent {
            AkahidegnTheme {
                Column {
                    Text("Animation Status: ${if (controller.isPaused) "Paused" else "Running"}")
                    Button(
                        onClick = { controller.toggle() }
                    ) {
                        Text("Toggle")
                    }
                }
            }
        }

        // Initially not paused
        composeTestRule.onNodeWithText("Animation Status: Running").assertIsDisplayed()

        // Click toggle button
        composeTestRule.onNodeWithText("Toggle").performClick()

        // Should now be paused
        composeTestRule.onNodeWithText("Animation Status: Paused").assertIsDisplayed()
    }
}
