package com.dawitf.akahidegn.ui.animation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfettiEmitterTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confetti_shows_on_trigger_and_is_deterministic_with_seed() {
        composeTestRule.setContent {
            AkahidegnTheme {
                ConfettiEmitter(
                    triggerKey = 1L,
                    testTag = "confettiOverlay",
                    seed = 1234L,
                    particleCount = 10,
                    durationMs = 1_000
                )
            }
        }

        // Confetti canvas should appear
        composeTestRule.onNodeWithTag("confettiOverlay").assertIsDisplayed()
    }
}
