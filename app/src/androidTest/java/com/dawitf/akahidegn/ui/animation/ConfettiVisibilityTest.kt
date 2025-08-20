package com.dawitf.akahidegn.ui.animation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfettiVisibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun changing_triggerKey_emits_confetti() {
        composeTestRule.setContent {
            AkahidegnTheme {
                var key by remember { mutableStateOf<Any?>(null) }
                // First blank render
                ConfettiEmitter(triggerKey = key, testTag = "confettiOverlay", seed = 1L, particleCount = 5, durationMs = 300)
                // Trigger
                key = 123L
            }
        }

        composeTestRule.onNodeWithTag("confettiOverlay").assertIsDisplayed()
    }
}
