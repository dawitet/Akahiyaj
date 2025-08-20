package com.dawitf.akahidegn.ui.animation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Gesture helpers to trigger animations from taps or long-presses.
 */
object GestureAnimation {
    fun Modifier.onTap(trigger: () -> Unit): Modifier =
        this.pointerInput(Unit) {
            detectTapGestures(onTap = { trigger() })
        }

    fun Modifier.onLongPress(trigger: () -> Unit): Modifier =
        this.pointerInput(Unit) {
            detectTapGestures(onLongPress = { trigger() })
        }
}
