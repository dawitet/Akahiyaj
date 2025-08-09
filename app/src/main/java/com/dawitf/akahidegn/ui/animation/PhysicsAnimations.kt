package com.dawitf.akahidegn.ui.animation

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Physics-inspired specs for reusable spring animations.
 */
object PhysicsAnimations {
    fun softSpring(): SpringSpec<Float> = spring(stiffness = 200f, dampingRatio = 0.6f)
    fun snappySpring(): SpringSpec<Float> = spring(stiffness = 800f, dampingRatio = 0.75f)
    fun bouncySpring(): SpringSpec<Float> = spring(stiffness = 400f, dampingRatio = 0.45f)
}
