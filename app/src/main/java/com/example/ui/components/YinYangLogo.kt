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
import kotlin.math.*

@Composable
fun YinYangLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    isSpinning: Boolean = false,
    outlineColor: Color = BalanceGold,
    glowEnabled: Boolean = true,
    hexagonal: Boolean = true
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
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        rememberUpdatedState(if (glowEnabled) 0.1f else 0f)
    }

    val breathScale by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
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
        val strokeWidth = maxOf(1.5.dp.toPx(), radius * 0.015f)

        withTransform({
            scale(breathScale, breathScale, center)
            rotate(rotationAngle, center)
        }) {
            if (hexagonal) {
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // HEXAGONAL YIN-YANG — Refined Sacred Geometry
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                // Hexagon path
                val hexPath = Path().apply {
                    val hexRadius = radius * 0.92f
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((i * 60.0 - 30.0))
                        val x = center.x + (hexRadius * cos(angle)).toFloat()
                        val y = center.y + (hexRadius * sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }

                // Outer glow ring
                if (isSpinning && glowEnabled) {
                    drawPath(
                        path = hexPath,
                        color = outlineColor.copy(alpha = glowAlpha * 0.4f),
                        style = Stroke(width = strokeWidth * 6)
                    )
                }

                // Draw background circle for clean edges
                drawCircle(
                    color = ShadowBlack,
                    radius = radius * 0.88f,
                    center = center
                )

                // S-curve dividing line - proper yin-yang shape
                val sCurvePath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.85f)
                    // Left curve (yang side)
                    cubicTo(
                        center.x - radius * 0.5f, center.y - radius * 0.4f,
                        center.x - radius * 0.5f, center.y + radius * 0.4f,
                        center.x, center.y + radius * 0.85f
                    )
                    // Right curve back (completing the yang half)
                    cubicTo(
                        center.x + radius * 0.85f, center.y + radius * 0.4f,
                        center.x + radius * 0.85f, center.y - radius * 0.4f,
                        center.x, center.y - radius * 0.85f
                    )
                    close()
                }
                drawPath(path = sCurvePath, color = MilkyWhite)

                // Small circles for eyes
                val eyeRadius = radius * 0.18f
                val dotRadius = radius * 0.08f
                
                // Upper eye (black dot in white)
                drawCircle(
                    color = ShadowBlack,
                    radius = eyeRadius,
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.35f)
                )
                drawCircle(
                    color = MilkyWhite,
                    radius = dotRadius,
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.35f)
                )

                // Lower eye (white dot in black - already on black background)
                drawCircle(
                    color = MilkyWhite,
                    radius = eyeRadius,
                    center = Offset(center.x + radius * 0.25f, center.y + radius * 0.35f)
                )
                drawCircle(
                    color = ShadowBlack,
                    radius = dotRadius,
                    center = Offset(center.x + radius * 0.25f, center.y + radius * 0.35f)
                )

                // Gold accent ring inside hexagon
                drawCircle(
                    color = outlineColor.copy(alpha = 0.2f),
                    radius = radius * 0.9f,
                    center = center,
                    style = Stroke(width = strokeWidth * 0.5f)
                )

                // Hexagon border
                drawPath(
                    path = hexPath,
                    color = outlineColor.copy(alpha = if (isSpinning) 0.7f else 0.5f),
                    style = Stroke(width = strokeWidth)
                )

                // Inner hexagon border
                val innerHexPath = Path().apply {
                    val innerRadius = radius * 0.75f
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((i * 60.0 - 30.0))
                        val x = center.x + (innerRadius * cos(angle)).toFloat()
                        val y = center.y + (innerRadius * sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = innerHexPath,
                    color = outlineColor.copy(alpha = 0.1f),
                    style = Stroke(width = strokeWidth * 0.4f)
                )

                // Orbiting particles
                if (isSpinning) {
                    val particleCount = 6
                    for (i in 0 until particleCount) {
                        val angle = Math.toRadians((i * 60.0 + rotationAngle).toDouble())
                        val particleRadius = radius * 0.95f
                        val px = center.x + (particleRadius * cos(angle)).toFloat()
                        val py = center.y + (particleRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.3f),
                            radius = maxOf(2.dp.toPx(), radius * 0.02f),
                            center = Offset(px, py)
                        )
                    }
                }

            } else {
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // CLASSIC CIRCULAR YIN-YANG
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                if (isSpinning && glowEnabled) {
                    drawCircle(
                        color = outlineColor.copy(alpha = glowAlpha * 0.5f),
                        radius = radius + strokeWidth * 3,
                        center = center,
                        style = Stroke(width = strokeWidth * 4)
                    )
                }

                // Yang (white) half
                drawArc(
                    color = MilkyWhite,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(0f, 0f)
                )

                // Large circles for each half
                drawCircle(color = ShadowBlack, radius = radius / 2f, center = Offset(center.x, center.y - radius / 2f))
                drawCircle(color = MilkyWhite, radius = radius / 2f, center = Offset(center.x, center.y + radius / 2f))

                // Small dots (eyes)
                drawCircle(color = MilkyWhite, radius = radius / 6f, center = Offset(center.x, center.y - radius / 2f))
                drawCircle(color = ShadowBlack, radius = radius / 6f, center = Offset(center.x, center.y + radius / 2f))

                // Outer ring
                drawCircle(
                    color = outlineColor.copy(alpha = if (isSpinning) 0.6f else 0.4f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Inner decorative ring
                drawCircle(
                    color = outlineColor.copy(alpha = if (isSpinning) 0.2f else 0.1f),
                    radius = radius - strokeWidth * 2,
                    center = center,
                    style = Stroke(width = strokeWidth * 0.4f)
                )

                if (isSpinning) {
                    val particleCount = 8
                    for (i in 0 until particleCount) {
                        val angle = Math.toRadians((i * (360.0 / particleCount) + rotationAngle * 2).toDouble())
                        val particleRadius = radius + strokeWidth * 6
                        val px = center.x + (particleRadius * cos(angle)).toFloat()
                        val py = center.y + (particleRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.2f),
                            radius = maxOf(1.5.dp.toPx(), radius * 0.015f),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}
