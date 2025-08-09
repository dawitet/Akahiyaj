package com.dawitf.akahidegn.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Minimal confetti particles for celebration effects.
 * Trigger by changing [triggerKey] (e.g., timestamp). Particles render for a short duration.
 */
@Composable
fun ConfettiEmitter(
    triggerKey: Any?,
    particleCount: Int = 60,
    colors: List<Color> = listOf(
        Color(0xFFFF5252), Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0)
    ),
    durationMs: Int = 1200,
    // Testability hooks
    testTag: String? = null,
    seed: Long? = null
) {
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val rng = remember(seed) { if (seed != null) Random(seed) else Random.Default }

    LaunchedEffect(triggerKey) {
        if (triggerKey == null) return@LaunchedEffect
        particles.clear()
        repeat(particleCount) {
        particles += ConfettiParticle.random(colors, rng)
        }
        // Animate all particles once
        particles.forEach { it.launch(durationMs) }
    }

    val base = if (testTag != null) Modifier.fillMaxSize().testTag(testTag) else Modifier.fillMaxSize()
    Canvas(base) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val x = p.startX * w + p.vx.value * w * 0.25f // small horizontal spread
            val y = p.startY * h + p.vy.value * h
            drawCircle(color = p.color, radius = p.size, center = Offset(x, y))
        }
    }
}

private class ConfettiParticle(
    val color: Color,
    val size: Float,
    val startX: Float,
    val startY: Float,
    val vx: Animatable<Float, *>,
    val vy: Animatable<Float, *>
) {
    suspend fun launch(durationMs: Int) {
        // Animate outward and downward with tween; X in [-1,1], Y in [0,1]
        vx.animateTo(targetValue = vx.targetValue, animationSpec = tween(durationMs))
        vy.animateTo(targetValue = vy.targetValue, animationSpec = tween(durationMs))
    }

    companion object {
        fun random(colors: List<Color>, rnd: Random = Random.Default): ConfettiParticle {
            val color = colors[rnd.nextInt(colors.size)]
            val size = rnd.nextFloat() * 6f + 3f
            val startX = rnd.nextFloat() // 0..1 (width fraction)
            val startY = 0f // start near top
            val vx = Animatable((rnd.nextFloat() - 0.5f) * 2f) // -1..1
            val vy = Animatable(0.2f + rnd.nextFloat() * 1.0f) // 0.2..1.2 downward
            return ConfettiParticle(color, size, startX, startY, vx, vy)
        }
    }
}
