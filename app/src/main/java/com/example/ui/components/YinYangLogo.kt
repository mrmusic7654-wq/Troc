// app/src/main/java/com/example/ui/components/YinYangLogo.kt
package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BalanceGold
import com.example.ui.theme.MilkyWhite
import com.example.ui.theme.ShadowBlack
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun YinYangLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    isSpinning: Boolean = false,
    outlineColor: Color = BalanceGold,
    glowEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "yinyang_rotation")
    val rotationAngle by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
    } else {
        rememberUpdatedState(0f)
    }

    val glowAlpha by if (isSpinning && glowEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        rememberUpdatedState(if (glowEnabled) 0.1f else 0f)
    }

    val breathScale by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
    } else {
        rememberUpdatedState(1f)
    }

    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size
        val radius = canvasSize.width / 2f
        val center = Offset(radius, radius)
        val strokeWidth = 1.5.dp.toPx()

        withTransform({
            scale(breathScale, breathScale, center)
            rotate(rotationAngle, center)
        }) {
            // Outer glow ring
            if (isSpinning && glowEnabled) {
                drawCircle(
                    color = outlineColor.copy(alpha = glowAlpha * 0.5f),
                    radius = radius + strokeWidth * 3,
                    center = center,
                    style = Stroke(width = strokeWidth * 4)
                )
            }

            // White half (Yang - light, active) - Left side semicircle
            drawArc(
                color = MilkyWhite,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(0f, 0f)
            )

            // Black half (Yin - dark, receptive) - Right side remains black
            // Top small circle (Yin inside Yang) - Black
            drawCircle(
                color = ShadowBlack,
                radius = radius / 2f,
                center = Offset(radius, radius / 2f)
            )

            // Bottom small circle (Yang inside Yin) - White
            drawCircle(
                color = MilkyWhite,
                radius = radius / 2f,
                center = Offset(radius, radius * 1.5f)
            )

            // Seed dot in top (White in black) - Spark of light in darkness
            drawCircle(
                color = MilkyWhite,
                radius = radius / 6f,
                center = Offset(radius, radius / 2f)
            )

            // Seed dot in bottom (Black in white) - Depth in light
            drawCircle(
                color = ShadowBlack,
                radius = radius / 6f,
                center = Offset(radius, radius * 1.5f)
            )

            // Inner delicate curves - S-line details
            val path = Path().apply {
                // Top curve from center-left to center
                moveTo(0f, radius)
                cubicTo(
                    radius * 0.5f, radius * 0.35f,
                    radius * 0.5f, radius * 0.65f,
                    radius, radius * 0.5f
                )
                // Bottom curve from center to center-right
                cubicTo(
                    radius * 0.5f, radius * 0.35f,
                    radius * 1.5f, radius * 1.35f,
                    radius * 2f, radius
                )
            }

            drawPath(
                path = path,
                color = outlineColor.copy(alpha = 0.08f),
                style = Stroke(width = strokeWidth * 0.5f)
            )
        }

        // Border circle - remains static for grounding
        drawCircle(
            color = outlineColor.copy(alpha = if (isSpinning) 0.6f else 0.4f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Second subtle border
        drawCircle(
            color = outlineColor.copy(alpha = if (isSpinning) 0.2f else 0.1f),
            radius = radius - strokeWidth,
            center = center,
            style = Stroke(width = strokeWidth * 0.3f)
        )

        // Orbiting particles when spinning
        if (isSpinning) {
            val particleCount = 8
            for (i in 0 until particleCount) {
                val angle = Math.toRadians((i * (360.0 / particleCount) + rotationAngle * 2).toDouble())
                val particleRadius = radius + strokeWidth * 6
                val px = center.x + (particleRadius * cos(angle)).toFloat()
                val py = center.y + (particleRadius * sin(angle)).toFloat()

                drawCircle(
                    color = outlineColor.copy(alpha = 0.15f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
