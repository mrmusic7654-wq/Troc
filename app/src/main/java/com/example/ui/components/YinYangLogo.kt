package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BalanceGold
import com.example.ui.theme.MilkyWhite
import com.example.ui.theme.ShadowBlack

@Composable
fun YinYangLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    isSpinning: Boolean = false,
    outlineColor: Color = BalanceGold
) {
    // Rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "yinyang_rotation")
    val rotationAngle by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
    } else {
        rememberUpdatedState(0f)
    }

    Canvas(modifier = modifier.size(size)) {
        val radius = size.toPx() / 2f
        val center = Offset(radius, radius)

        rotate(rotationAngle, center) {
            // 1. Draw solid background circles
            // Dark Side (Right half) - Draw full circle in black first, then overlay light side
            drawCircle(
                color = ShadowBlack,
                radius = radius,
                center = center
            )

            // Light Side (Left half) - Sweep 180 degrees from 90 to 270 (top to bottom)
            drawArc(
                color = MilkyWhite,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(0f, 0f)
            )

            // 2. Draw the interlocking S-curves (Semicircles)
            // Top small circle (Yin) - Black color, radius/2, centered at (radius, radius/2)
            drawCircle(
                color = ShadowBlack,
                radius = radius / 2,
                center = Offset(radius, radius / 2)
            )

            // Bottom small circle (Yang) - White color, radius/2, centered at (radius, radius * 1.5)
            drawCircle(
                color = MilkyWhite,
                radius = radius / 2,
                center = Offset(radius, radius * 1.5f)
            )

            // 3. Draw the seed dots
            // Top seed dot (White dot inside black semicircle) - radius/6
            drawCircle(
                color = MilkyWhite,
                radius = radius / 6,
                center = Offset(radius, radius / 2)
            )

            // Bottom seed dot (Black dot inside white semicircle) - radius/6
            drawCircle(
                color = ShadowBlack,
                radius = radius / 6,
                center = Offset(radius, radius * 1.5f)
            )
        }

        // 4. Draw elegant thin golden border around the entire logo to ground it
        drawCircle(
            color = outlineColor,
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
