package com.dawitf.akahidegn.ui.viewmodels

import com.dawitf.akahidegn.ui.components.AnimationType

/**
 * Data class for animation settings configuration
 */
data class AnimationSettings(
    val enableAnimations: Boolean = true,
    val animationDuration: Long = 300L,
    val defaultAnimationType: AnimationType = AnimationType.FADE,
    val reduceMotion: Boolean = false,
    val enableParallax: Boolean = true,
    val enableSpringAnimations: Boolean = true,
    val animationScale: Float = 1.0f
)

/**
 * Animation preference settings
 */
data class AnimationPreferences(
    val enableSharedElementTransitions: Boolean = true,
    val enableCustomTransitions: Boolean = true,
    val enableAutoplay: Boolean = false,
    val enableHapticFeedback: Boolean = true
)
