package com.lavish.admob.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFF1E8E3E), // Botanical Green
    Color(0xFF1A73E8), // Solar Amber
    Color(0xFF12B5CB), // Ocean Azure
    Color(0xFFD93025), // Coral Red
    Color(0xFF9334E6), // Purple
    Color(0xFFE52592), // Pink
    Color(0xFF4285F4), // Gold
    Color(0xFF81C995), // Mint Green
    Color(0xFF78D9EC)  // Sky Blue
)

private enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    STRIP
}

private class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val vx: Float,
    val vy: Float,
    val particleSize: Float,
    val length: Float,
    val color: Color,
    val rotationSpeed: Float,
    val initialRotation: Float,
    val shape: ConfettiShape,
    val flutterFrequency: Float,
    val flutterPhase: Float
) {
    fun render(drawScope: DrawScope, progress: Float, width: Float, height: Float) {
        // Gravity & air resistance dynamics
        val t = progress * 3.5f // 3.5 seconds of simulated time
        val gravity = 450f
        val x = initialX * width + vx * t + sin(t * flutterFrequency + flutterPhase) * 35f
        val y = initialY * height + vy * t + 0.5f * gravity * t * t

        // Don't draw if far off-screen
        if (y > height + 100f || y < -150f) return

        // Alpha fades gracefully in the last 25% of the animation
        val alpha = if (progress > 0.75f) {
            ((1f - progress) / 0.25f).coerceIn(0f, 1f)
        } else {
            1f
        }

        val rotation = initialRotation + rotationSpeed * progress * 360f
        val drawColor = color.copy(alpha = alpha)

        drawScope.rotate(rotation, pivot = Offset(x, y)) {
            when (shape) {
                ConfettiShape.RECTANGLE -> {
                    drawRoundRect(
                        color = drawColor,
                        topLeft = Offset(x - particleSize / 2f, y - particleSize / 2f),
                        size = Size(particleSize, particleSize * 1.5f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
                ConfettiShape.CIRCLE -> {
                    drawCircle(
                        color = drawColor,
                        radius = particleSize / 2f,
                        center = Offset(x, y)
                    )
                }
                ConfettiShape.STRIP -> {
                    // Fluttering thin ribbon
                    val flutterScale = cos(t * flutterFrequency + flutterPhase)
                    drawRoundRect(
                        color = drawColor,
                        topLeft = Offset(x - (particleSize * flutterScale) / 2f, y - length / 2f),
                        size = Size(particleSize * flutterScale, length),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                }
            }
        }
    }
}

/**
 * High-performance celebration confetti particle burst.
 * Triggers an energetic burst whenever [triggerKey] changes.
 */
@Composable
fun CelebrationConfetti(
    triggerKey: Any?,
    modifier: Modifier = Modifier,
    particleCount: Int = 120
) {
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    val progressAnim = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey == null) return@LaunchedEffect

        val random = Random(triggerKey.hashCode().toLong() xor 0x5DEECE66DL)
        particles = List(particleCount) {
            val shape = when (random.nextInt(3)) {
                0 -> ConfettiShape.STRIP
                1 -> ConfettiShape.RECTANGLE
                else -> ConfettiShape.CIRCLE
            }

            ConfettiParticle(
                initialX = random.nextFloat() * 0.8f + 0.1f, // spread across 10%-90% of screen width
                initialY = random.nextFloat() * 0.25f + 0.05f, // start near top
                vx = (random.nextFloat() - 0.5f) * 260f, // horizontal outward burst
                vy = -random.nextFloat() * 220f + 30f,  // upward initial pop
                particleSize = random.nextFloat() * 10f + 8f,
                length = random.nextFloat() * 18f + 12f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                rotationSpeed = (random.nextFloat() - 0.5f) * 8f,
                initialRotation = random.nextFloat() * 360f,
                shape = shape,
                flutterFrequency = random.nextFloat() * 8f + 4f,
                flutterPhase = random.nextFloat() * 6.28f
            )
        }

        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
        )
        // Clean up particles when animation finishes
        particles = emptyList()
    }

    // Visibility depends only on the particle list; progress is read inside the draw phase,
    // so the animation redraws the Canvas without recomposing the parent each frame.
    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val progress = progressAnim.value
            val width = size.width
            val height = size.height
            for (particle in particles) {
                particle.render(this, progress, width, height)
            }
        }
    }
}
