// app/src/main/java/com/example/ui/components/SettingsDrawerItem.kt
package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Workspace
import com.example.ui.theme.*

@Composable
fun SettingsDrawerItem(
    workspace: Workspace,
    isSelected: Boolean,
    isPremiumUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = workspace.isPremium && !isPremiumUnlocked

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.15f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "glow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                BalanceGold.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, BalanceGold.copy(alpha = 0.3f))
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(
                                    BalanceGold.copy(alpha = 0.2f),
                                    BalanceGold.copy(alpha = 0.05f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    ShadowBlackCard,
                                    ShadowBlackCard
                                )
                            )
                        }
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(1.dp, BalanceGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = workspace.icon,
                    contentDescription = workspace.contentDescription,
                    tint = when {
                        isSelected -> BalanceGold
                        isLocked -> MutedGrayDark.copy(alpha = 0.4f)
                        else -> MutedGrayDark
                    },
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(if (isLocked) 0.4f else 1f)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = workspace.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = when {
                        isSelected -> MilkyWhiteText
                        isLocked -> MutedGrayDark.copy(alpha = 0.4f)
                        else -> MilkyWhiteText.copy(alpha = 0.8f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isLocked) "Premium" else workspace.description,
                    fontSize = 10.sp,
                    color = if (isLocked) BalanceGold.copy(alpha = 0.5f) else MutedGrayDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BalanceGold)
                )
            }

            if (isLocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Premium feature",
                    tint = BalanceGold.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun DrawerHeader(
    isGenerating: Boolean,
    activeKeyCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            YinYangLogo(
                size = 40.dp,
                isSpinning = isGenerating
            )

            Column {
                Text(
                    text = "Troc Agent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Harmonized Intelligence",
                    style = MaterialTheme.typography.labelSmall,
                    color = BalanceGold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeKeyCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BalanceGold.copy(alpha = 0.08f))
                    .border(0.5.dp, BalanceGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = BalanceGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$activeKeyCount API key${if (activeKeyCount != 1) "s" else ""} active",
                    fontSize = 11.sp,
                    color = BalanceGold,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DrawerDivider(
    label: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MutedGrayDark,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        HorizontalDivider(
            color = BorderGrayDark,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
