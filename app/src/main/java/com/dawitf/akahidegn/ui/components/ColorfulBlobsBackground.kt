package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A composable that draws animated, colorful, soft blobs for a modern background accent.
 * Can be layered behind content for a playful, glassmorphic look.
 */
@Composable
fun ColorfulBlobsBackground(
    modifier: Modifier = Modifier,
    blobCount: Int = 3,
    blobColors: List<List<Color>> = listOf(
        listOf(Color(0xFFB388FF), Color(0xFF8C9EFF)),
        listOf(Color(0xFFFF8A80), Color(0xFFFFD180)),
        listOf(Color(0xFF80D8FF), Color(0xFFA7FFEB))
    ),
    minBlobSize: Dp = 180.dp,
    maxBlobSize: Dp = 320.dp,
    animationDuration: Int = 8000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")
    val angles = List(blobCount) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(animationDuration + i * 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "angle$i"
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            for (i in 0 until blobCount) {
                val angle = angles[i].value + i * 120f
                val radius = (minBlobSize.toPx() + (maxBlobSize.toPx() - minBlobSize.toPx()) * (0.5f + 0.5f * sin(angle * PI / 180))).toFloat()
                val centerX = width / 2 + cos(angle * PI / 180) * width * 0.25f * (1f - i * 0.2f)
                val centerY = height / 2 + sin(angle * PI / 180) * height * 0.25f * (1f - i * 0.2f)
                drawIntoCanvas { canvas ->
                    withTransform({
                        translate(centerX.toFloat() - radius / 2, centerY.toFloat() - radius / 2)
                    }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = blobColors.getOrNull(i) ?: listOf(Color.Magenta, Color.Cyan),
                                center = Offset(radius / 2, radius / 2),
                                radius = radius / 1.2f
                            ),
                            radius = radius / 2,
                            center = Offset(radius / 2, radius / 2),
                            alpha = 0.32f
                        )
                    }
                }
            }
        }
    }
}
