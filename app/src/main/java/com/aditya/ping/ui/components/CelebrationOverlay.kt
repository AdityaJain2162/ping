package com.aditya.ping.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Shows a brief celebration overlay — a green checkmark that scales in
 * with a burst of confetti particles. Auto-dismisses after 1.2 seconds.
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        LaunchedEffect(Unit) {
            delay(1200)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(300)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Confetti particles
            ConfettiBurst()

            // Checkmark with scale-in animation
            val scaleAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = { overshoot ->
                            // Bounce overshoot
                            val t = overshoot - 1
                            1f + 2.7f * t * t * t + 1.7f * t * t
                        },
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scaleAnim.value)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(80.dp),
                )
            }
        }
    }
}

@Composable
private fun ConfettiBurst() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confettiProgress",
    )

    val colors = listOf(
        Color(0xFFE85D5D), Color(0xFF00A8AE), Color(0xFFB45200),
        Color(0xFFE87B3E), Color(0xFF25D366), Color(0xFFFFB87D),
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxDistance = size.minDimension * 0.4f

        repeat(12) { i ->
            val angle = (i * 30).toFloat()
            val radians = Math.toRadians(angle.toDouble()).toFloat()
            val distance = maxDistance * progress
            val x = centerX + kotlin.math.cos(radians) * distance
            val y = centerY + kotlin.math.sin(radians) * distance
            val size = (8f * (1f - progress)).coerceAtLeast(0f)
            val color = colors[i % colors.size]

            drawCircle(
                color = color.copy(alpha = 1f - progress),
                radius = size,
                center = Offset(x, y),
            )
        }
    }
}
