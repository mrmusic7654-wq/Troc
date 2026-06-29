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
                        color = outlineColor.copy(alpha = glowAlpha * 0.3f),
                        style = Stroke(width = strokeWidth * 5)
                    )
                }

                // White half (Yang) - Left side
                val yangPath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.85f)
                    lineTo(center.x - radius * 0.74f, center.y - radius * 0.425f)
                    lineTo(center.x - radius * 0.74f, center.y + radius * 0.425f)
                    lineTo(center.x, center.y + radius * 0.85f)
                    // Curve back through S-line
                    cubicTo(
                        center.x - radius * 0.35f, center.y + radius * 0.3f,
                        center.x - radius * 0.35f, center.y - radius * 0.3f,
                        center.x, center.y - radius * 0.85f
                    )
                    close()
                }
                drawPath(path = yangPath, color = MilkyWhite)

                // Black half (Yin) - Right side (background already black)
                
                // Top small circle (Yin in Yang) - Black
                drawCircle(
                    color = ShadowBlack,
                    radius = radius * 0.28f,
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.3f)
                )

                // Bottom small circle (Yang in Yin) - White
                drawCircle(
                    color = MilkyWhite,
                    radius = radius * 0.28f,
                    center = Offset(center.x + radius * 0.25f, center.y + radius * 0.3f)
                )

                // Top seed dot (White in Black)
                drawCircle(
                    color = MilkyWhite,
                    radius = radius * 0.09f,
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.3f)
                )

                // Bottom seed dot (Black in White)
                drawCircle(
                    color = ShadowBlack,
                    radius = radius * 0.09f,
                    center = Offset(center.x + radius * 0.25f, center.y + radius * 0.3f)
                )

                // Gold S-curve line
                val sCurve = Path().apply {
                    moveTo(center.x, center.y - radius * 0.85f)
                    cubicTo(
                        center.x - radius * 0.5f, center.y - radius * 0.15f,
                        center.x - radius * 0.15f, center.y + radius * 0.05f,
                        center.x, center.y
                    )
                    cubicTo(
                        center.x + radius * 0.15f, center.y - radius * 0.05f,
                        center.x + radius * 0.5f, center.y + radius * 0.15f,
                        center.x, center.y + radius * 0.85f
                    )
                }
                drawPath(
                    path = sCurve,
                    color = outlineColor.copy(alpha = 0.15f),
                    style = Stroke(width = strokeWidth * 0.5f)
                )

                // Hexagon border
                drawPath(
                    path = hexPath,
                    color = outlineColor.copy(alpha = if (isSpinning) 0.5f else 0.35f),
                    style = Stroke(width = strokeWidth)
                )

                // Inner hexagon border
                val innerHexPath = Path().apply {
                    val innerRadius = radius * 0.78f
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
                    color = outlineColor.copy(alpha = 0.08f),
                    style = Stroke(width = strokeWidth * 0.3f)
                )

                // Orbiting particles
                if (isSpinning) {
                    val particleCount = 6
                    for (i in 0 until particleCount) {
                        val angle = Math.toRadians((i * 60.0 + rotationAngle * 2).toDouble())
                        val particleRadius = radius * 0.95f
                        val px = center.x + (particleRadius * cos(angle)).toFloat()
                        val py = center.y + (particleRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.2f),
                            radius = 1.5.dp.toPx(),
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

                drawArc(
                    color = MilkyWhite,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(0f, 0f)
                )

                drawCircle(color = ShadowBlack, radius = radius / 2f, center = Offset(radius, radius / 2f))
                drawCircle(color = MilkyWhite, radius = radius / 2f, center = Offset(radius, radius * 1.5f))
                drawCircle(color = MilkyWhite, radius = radius / 6f, center = Offset(radius, radius / 2f))
                drawCircle(color = ShadowBlack, radius = radius / 6f, center = Offset(radius, radius * 1.5f))

                drawCircle(
                    color = outlineColor.copy(alpha = if (isSpinning) 0.6f else 0.4f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                drawCircle(
                    color = outlineColor.copy(alpha = if (isSpinning) 0.2f else 0.1f),
                    radius = radius - strokeWidth,
                    center = center,
                    style = Stroke(width = strokeWidth * 0.3f)
                )

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
    }
}
