// app/src/main/java/com/example/ui/components/YinYangLogo.kt
package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.*

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
                animation = tween(8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
    } else {
        rememberUpdatedState(0f)
    }

    val glowAlpha by if (isSpinning && glowEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        rememberUpdatedState(if (glowEnabled) 0.08f else 0f)
    }

    val breathScale by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
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
        val strokeWidth = (radius * 0.025f).coerceAtLeast(1.5f)

        withTransform({
            scale(breathScale, breathScale, center)
            rotate(rotationAngle, center)
        }) {
            // Outer glow
            if (isSpinning && glowEnabled) {
                drawCircle(
                    color = outlineColor.copy(alpha = glowAlpha * 0.4f),
                    radius = radius + strokeWidth * 3,
                    center = center,
                    style = Stroke(width = strokeWidth * 5)
                )
            }

            // Draw black background (Yin — dark/receptive)
            drawCircle(color = ShadowBlack, radius = radius, center = center)

            // Draw white half (Yang — light/active) as right semicircle
            // Using Path for the classic S-curve
            val yangPath = Path().apply {
                // Start at top center
                moveTo(center.x, center.y - radius)
                // Arc down the right side
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - radius, center.y - radius,
                        center.x + radius, center.y + radius
                    ),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                // S-curve back up: bottom small circle's outer curve
                // From bottom center, curve left and up through the center
                cubicTo(
                    center.x + radius * 0.4f, center.y + radius * 0.1f,
                    center.x + radius * 0.1f, center.y + radius * 0.4f,
                    center.x, center.y
                )
                // Continue S-curve to top
                cubicTo(
                    center.x - radius * 0.1f, center.y - radius * 0.4f,
                    center.x - radius * 0.4f, center.y - radius * 0.1f,
                    center.x, center.y - radius
                )
                close()
            }
            drawPath(path = yangPath, color = MilkyWhite)

            // Top small circle (Yin within Yang) — Black with white dot
            val eyeRadius = radius * 0.22f
            val dotRadius = radius * 0.07f
            
            drawCircle(
                color = ShadowBlack,
                radius = eyeRadius,
                center = Offset(center.x, center.y - radius * 0.5f)
            )
            drawCircle(
                color = MilkyWhite,
                radius = dotRadius,
                center = Offset(center.x, center.y - radius * 0.5f)
            )

            // Bottom small circle (Yang within Yin) — White with black dot
            drawCircle(
                color = MilkyWhite,
                radius = eyeRadius,
                center = Offset(center.x, center.y + radius * 0.5f)
            )
            drawCircle(
                color = ShadowBlack,
                radius = dotRadius,
                center = Offset(center.x, center.y + radius * 0.5f)
            )

            // Elegant gold border
            drawCircle(
                color = outlineColor.copy(alpha = if (isSpinning) 0.6f else 0.45f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Inner subtle ring
            drawCircle(
                color = outlineColor.copy(alpha = 0.12f),
                radius = radius - strokeWidth * 2,
                center = center,
                style = Stroke(width = strokeWidth * 0.4f)
            )

            // Orbiting particles when spinning
            if (isSpinning) {
                val particleCount = 6
                for (i in 0 until particleCount) {
                    val angle = Math.toRadians((i * 60.0 + rotationAngle * 1.5).toDouble())
                    val particleRadius = radius + strokeWidth * 5
                    val px = center.x + (particleRadius * cos(angle)).toFloat()
                    val py = center.y + (particleRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.2f),
                        radius = dotRadius * 0.6f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
