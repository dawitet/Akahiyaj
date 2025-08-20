package com.dawitf.akahidegn.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * Tiny sequential animation runner for chaining simple steps.
 * Supply a list of suspend lambdas; each runs sequentially with an optional delay between.
 */
class SequentialAnimationChain(
    private val steps: List<suspend () -> Unit>,
    private val delayMsBetween: Long = 120L
) {
    suspend fun run() {
        steps.forEachIndexed { index, step ->
            step()
            if (index != steps.lastIndex && delayMsBetween > 0) delay(delayMsBetween)
        }
    }
}

@Composable
fun RunSequentialAnimation(
    key: Any,
    steps: List<suspend () -> Unit>,
    delayMsBetween: Long = 120L
) {
    LaunchedEffect(key) {
        SequentialAnimationChain(steps, delayMsBetween).run()
    }
}
