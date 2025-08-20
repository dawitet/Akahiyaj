package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for AnimationComponents
 * Tests animation logic, configurations, and edge cases
 */
class AnimationComponentsTest {

    private lateinit var defaultAnimationConfig: AnimationConfig
    private lateinit var customAnimationConfig: AnimationConfig

    @Before
    fun setUp() {
        defaultAnimationConfig = AnimationConfig()
        customAnimationConfig = AnimationConfig(
            duration = 1000,
            delay = 300,
            easing = FastOutSlowInEasing,
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    @Test
    fun `AnimationConfig default values are correct`() {
        assertEquals(800, defaultAnimationConfig.duration)
        assertEquals(200, defaultAnimationConfig.delay)
        assertEquals(FastOutSlowInEasing, defaultAnimationConfig.easing)
        assertEquals(Spring.DampingRatioMediumBouncy, defaultAnimationConfig.dampingRatio)
        assertEquals(Spring.StiffnessMedium, defaultAnimationConfig.stiffness)
    }

    @Test
    fun `AnimationConfig custom values are set correctly`() {
        assertEquals(1000, customAnimationConfig.duration)
        assertEquals(300, customAnimationConfig.delay)
        assertEquals(FastOutSlowInEasing, customAnimationConfig.easing)
        assertEquals(Spring.DampingRatioMediumBouncy, customAnimationConfig.dampingRatio)
        assertEquals(Spring.StiffnessMedium, customAnimationConfig.stiffness)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AnimationConfig throws exception for negative duration`() {
        AnimationConfig(duration = -100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AnimationConfig throws exception for negative delay`() {
        AnimationConfig(delay = -50)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AnimationConfig throws exception for invalid damping ratio`() {
        AnimationConfig(dampingRatio = 2.0f)
    }

    @Test
    fun `AnimationSpeed values are correct`() {
        assertEquals(1.5f, AnimationSpeed.Slow.multiplier, 0.001f)
        assertEquals(1.0f, AnimationSpeed.Normal.multiplier, 0.001f)
        assertEquals(0.7f, AnimationSpeed.Fast.multiplier, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AnimationSpeed throws exception for zero multiplier`() {
        AnimationSpeed(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AnimationSpeed throws exception for negative multiplier`() {
        AnimationSpeed(-1f)
    }

    @Test
    fun `AnimationSize enum values are correct`() {
        assertEquals(48.dp, AnimationSize.Small.dp)
        assertEquals(64.dp, AnimationSize.Medium.dp)
        assertEquals(96.dp, AnimationSize.Large.dp)
    }

    @Test
    fun `AnimationType enum contains all expected values`() {
        val expectedTypes = listOf(
            AnimationType.SUCCESS,
            AnimationType.ERROR,
            AnimationType.WARNING,
            AnimationType.LOADING
        )

        val actualTypes = AnimationType.values().toList()
        assertEquals(expectedTypes.size, actualTypes.size)
        assertTrue(actualTypes.containsAll(expectedTypes))
    }

    @Test
    fun `SlideDirection enum contains all expected values`() {
        val expectedDirections = listOf(
            SlideDirection.TOP,
            SlideDirection.BOTTOM,
            SlideDirection.LEFT,
            SlideDirection.RIGHT
        )

        val actualDirections = SlideDirection.values().toList()
        assertEquals(expectedDirections.size, actualDirections.size)
        assertTrue(actualDirections.containsAll(expectedDirections))
    }

    @Test
    fun `NotificationItem has correct default values`() {
        val notification = NotificationItem(
            id = "test-id",
            type = AnimationType.SUCCESS,
            title = "Test Title"
        )

        assertEquals("test-id", notification.id)
        assertEquals(AnimationType.SUCCESS, notification.type)
        assertEquals("Test Title", notification.title)
        assertNull(notification.subtitle)
        assertTrue(notification.isVisible)
        assertNotNull(notification.onDismiss)
        assertNull(notification.onRetry)
        assertNull(notification.onAction)
    }

    @Test
    fun `AnimationController initial state is correct`() {
        val controller = AnimationController()
        assertFalse(controller.isPaused)
    }

    @Test
    fun `AnimationController pause functionality works`() {
        val controller = AnimationController()
        controller.pause()
        assertTrue(controller.isPaused)
    }

    @Test
    fun `AnimationController resume functionality works`() {
        val controller = AnimationController()
        controller.pause()
        controller.resume()
        assertFalse(controller.isPaused)
    }

    @Test
    fun `AnimationController toggle functionality works`() {
        val controller = AnimationController()

        // Initially not paused
        assertFalse(controller.isPaused)

        // Toggle to paused
        controller.toggle()
        assertTrue(controller.isPaused)

        // Toggle back to not paused
        controller.toggle()
        assertFalse(controller.isPaused)
    }

    @Test
    fun `SoundEffectsManager methods exist and don't throw exceptions`() {
        // Test that methods exist and can be called without exceptions
        try {
            SoundEffectsManager.playSuccessSound()
            SoundEffectsManager.playErrorSound()
            SoundEffectsManager.playWarningSound()
            // If we reach here, no exceptions were thrown
            assertTrue(true)
        } catch (e: Exception) {
            fail("SoundEffectsManager methods should not throw exceptions: ${e.message}")
        }
    }

    @Test
    fun `Animation duration calculation with different speeds`() {
        val baseConfig = AnimationConfig(duration = 1000)

        // Test with slow speed
        val slowSpeed = AnimationSpeed.Slow
        val slowDuration = (baseConfig.duration / slowSpeed.multiplier).toInt()
        assertEquals(666, slowDuration) // 1000 / 1.5 ≈ 666

        // Test with normal speed
        val normalSpeed = AnimationSpeed.Normal
        val normalDuration = (baseConfig.duration / normalSpeed.multiplier).toInt()
        assertEquals(1000, normalDuration) // 1000 / 1.0 = 1000

        // Test with fast speed
        val fastSpeed = AnimationSpeed.Fast
        val fastDuration = (baseConfig.duration / fastSpeed.multiplier).toInt()
        assertEquals(1428, fastDuration) // 1000 / 0.7 ≈ 1428
    }

    @Test
    fun `Animation configuration validation edge cases`() {
        // Test minimum valid values
        val minConfig = AnimationConfig(
            duration = 1,
            delay = 0,
            dampingRatio = 0.1f
        )
        assertEquals(1, minConfig.duration)
        assertEquals(0, minConfig.delay)
        assertEquals(0.1f, minConfig.dampingRatio, 0.001f)

        // Test maximum valid damping ratio
        val maxConfig = AnimationConfig(dampingRatio = 1.0f)
        assertEquals(1.0f, maxConfig.dampingRatio, 0.001f)
    }

    @Test
    fun `NotificationItem with all parameters`() {
        var dismissCalled = false
        var retryCalled = false
        var actionCalled = false

        val notification = NotificationItem(
            id = "full-test",
            type = AnimationType.ERROR,
            title = "Error Title",
            subtitle = "Error Subtitle",
            isVisible = false,
            onDismiss = { dismissCalled = true },
            onRetry = { retryCalled = true },
            onAction = { actionCalled = true }
        )

        assertEquals("full-test", notification.id)
        assertEquals(AnimationType.ERROR, notification.type)
        assertEquals("Error Title", notification.title)
        assertEquals("Error Subtitle", notification.subtitle)
        assertFalse(notification.isVisible)

        // Test callbacks
        notification.onDismiss()
        assertTrue(dismissCalled)

        notification.onRetry?.invoke()
        assertTrue(retryCalled)

        notification.onAction?.invoke()
        assertTrue(actionCalled)
    }

    @Test
    fun `AnimationSize dp values are positive`() {
        AnimationSize.values().forEach { size ->
            assertTrue("Size ${size.name} should be positive", size.dp.value > 0)
        }
    }

    @Test
    fun `AnimationSpeed multiplier validation`() {
        val validMultipliers = listOf(0.1f, 0.5f, 1.0f, 2.0f, 5.0f)

        validMultipliers.forEach { multiplier ->
            val speed = AnimationSpeed(multiplier)
            assertEquals(multiplier, speed.multiplier, 0.001f)
        }
    }

    @Test
    fun `Animation progress calculation bounds`() {
        // Test progress bounds in a simulated animation
        val progressValues = listOf(0.0f, 0.25f, 0.5f, 0.7f, 0.75f, 1.0f)

        progressValues.forEach { progress ->
            // Simulate checkmark progress calculation
            val checkProgress = if (progress > 0.7f) {
                (progress - 0.7f) / 0.3f
            } else {
                0f
            }

            assertTrue("Progress should be >= 0", checkProgress >= 0f)
            assertTrue("Progress should be <= 1", checkProgress <= 1f)
        }
    }
}
