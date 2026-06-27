// app/src/main/java/com/example/ui/components/AgentOrchestrator.kt
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

// ═══════════════════════════════════════
// Agent Orchestration Models
// ═══════════════════════════════════════

enum class AgentStatus {
    IDLE, PLANNING, EXECUTING, COMPLETED, FAILED, WAITING
}

enum class AgentType(
    val icon: ImageVector,
    val label: String,
    val color: Color
) {
    PLANNER(Icons.Rounded.Psychology, "Planner", BalanceGold),
    CODER(Icons.Rounded.Code, "Coder", Color(0xFF82B1FF)),
    RESEARCHER(Icons.Rounded.Search, "Researcher", Color(0xFFB9F6CA)),
    CREATOR(Icons.Rounded.Brush, "Creator", Color(0xFFFF80AB)),
    REVIEWER(Icons.Rounded.RateReview, "Reviewer", Color(0xFFFFB74D)),
    EXECUTOR(Icons.Rounded.Rocket, "Executor", Color(0xFFCE93D8))
}

data class AgentStep(
    val id: String,
    val agentType: AgentType,
    val description: String,
    val status: AgentStatus = AgentStatus.IDLE,
    val progress: Float = 0f,
    val result: String? = null,
    val children: List<AgentStep> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

data class OrchestrationPlan(
    val id: String,
    val goal: String,
    val steps: List<AgentStep>,
    val status: AgentStatus = AgentStatus.IDLE,
    val createdAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════
// Agent Orchestrator Composable
// ═══════════════════════════════════════

@Composable
fun AgentOrchestratorPanel(
    plan: OrchestrationPlan?,
    isExecuting: Boolean,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ShadowBlackCard
        ),
        border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BalanceGold.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Hub,
                            contentDescription = null,
                            tint = BalanceGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Agent Swarm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MilkyWhiteText
                        )
                        Text(
                            text = if (isExecuting) "Orchestrating..." else "Ready",
                            fontSize = 10.sp,
                            color = if (isExecuting) BalanceGold else MutedGrayDark
                        )
                    }
                }

                if (isExecuting && onCancel != null) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 11.sp,
                            color = ErrorRed
                        )
                    }
                }
            }

            if (plan != null) {
                Spacer(modifier = Modifier.height(12.dp))

                // Goal Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ShadowBlack
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Target,
                            contentDescription = null,
                            tint = BalanceGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = plan.goal,
                            fontSize = 12.sp,
                            color = MilkyWhiteText.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                val overallProgress = if (plan.steps.isNotEmpty()) {
                    plan.steps.sumOf { it.progress.toDouble() }.toFloat() / plan.steps.size
                } else 0f

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            fontSize = 10.sp,
                            color = MutedGrayDark
                        )
                        Text(
                            text = "${(overallProgress * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BalanceGold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BalanceGold,
                        trackColor = BorderGrayDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Agent Steps
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(plan.steps) { step ->
                        AgentStepCard(step = step)
                    }
                }

                // Retry button
                if (plan.status == AgentStatus.FAILED && onRetry != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f),
                            contentColor = ErrorRed
                        )
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Orchestration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isExecuting) {
                Spacer(modifier = Modifier.height(24.dp))
                AgentThinkingAnimation()
            }
        }
    }
}

@Composable
private fun AgentStepCard(
    step: AgentStep,
    modifier: Modifier = Modifier
) {
    val statusColor = when (step.status) {
        AgentStatus.COMPLETED -> SuccessGreen
        AgentStatus.EXECUTING -> BalanceGold
        AgentStatus.FAILED -> ErrorRed
        AgentStatus.PLANNING -> InfoBlue
        AgentStatus.WAITING -> MutedGrayDark
        AgentStatus.IDLE -> MutedGrayDark.copy(alpha = 0.5f)
    }

    val progressAnimation by animateFloatAsState(
        targetValue = step.progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "stepProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Agent Type Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (step.status == AgentStatus.EXECUTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = statusColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = when (step.status) {
                            AgentStatus.COMPLETED -> Icons.Rounded.Check
                            AgentStatus.FAILED -> Icons.Rounded.Close
                            AgentStatus.WAITING -> Icons.Rounded.HourglassEmpty
                            else -> step.agentType.icon
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = step.agentType.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = statusColor
                    )
                    Text(
                        text = "•",
                        fontSize = 8.sp,
                        color = MutedGrayDark
                    )
                    Text(
                        text = step.status.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = MutedGrayDark
                    )
                }

                Text(
                    text = step.description,
                    fontSize = 11.sp,
                    color = MilkyWhiteText.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (step.status == AgentStatus.EXECUTING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressAnimation },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = statusColor,
                        trackColor = BorderGrayDark
                    )
                }

                step.result?.let { result ->
                    if (step.status == AgentStatus.COMPLETED || step.status == AgentStatus.FAILED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result,
                            fontSize = 10.sp,
                            color = if (step.status == AgentStatus.FAILED) ErrorRed else SuccessGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Duration
            if (step.endTime != null) {
                val duration = step.endTime - step.startTime
                Text(
                    text = formatDuration(duration),
                    fontSize = 9.sp,
                    color = MutedGrayDark.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun AgentThinkingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "agentThinking")

    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, 200), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, 400), RepeatMode.Reverse),
        label = "dot3"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated hub
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .scale(pulse)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 3

            // Outer ring
            drawCircle(
                color = BalanceGold.copy(alpha = 0.2f),
                radius = radius * 1.5f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Middle ring
            drawCircle(
                color = BalanceGold.copy(alpha = 0.4f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center dot
            drawCircle(
                color = BalanceGold,
                radius = 4.dp.toPx(),
                center = center
            )

            // Orbiting dots
            for (i in 0..2) {
                val angle = Math.toRadians((i * 120.0 + (dot1 + dot2 + dot3) * 60).toDouble())
                val orbitRadius = radius * 1.2f
                val x = center.x + (orbitRadius * kotlin.math.cos(angle)).toFloat()
                val y = center.y + (orbitRadius * kotlin.math.sin(angle)).toFloat()
                drawCircle(
                    color = BalanceGold.copy(alpha = 0.7f),
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Agents are thinking...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MilkyWhiteText
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1, dot2, dot3).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .scale(alpha)
                            .clip(CircleShape)
                            .background(BalanceGold)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniAgentStatus(
    isActive: Boolean,
    activeAgentCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val pulseAlpha by rememberInfiniteTransition(label = "miniPulse").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) BalanceGold.copy(alpha = 0.1f) else Color.Transparent
            )
            .border(
                0.5.dp,
                if (isActive) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark,
                RoundedCornerShape(20.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(pulseAlpha)
                    .clip(CircleShape)
                    .background(SuccessGreen)
            )
        }
        Icon(
            Icons.Rounded.Hub,
            contentDescription = null,
            tint = if (isActive) BalanceGold else MutedGrayDark,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (isActive) "$activeAgentCount active" else "Idle",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) BalanceGold else MutedGrayDark
        )
    }
}

private fun formatDuration(millis: Long): String {
    return when {
        millis < 1000 -> "${millis}ms"
        millis < 60000 -> "${millis / 1000.0}s"
        else -> "${millis / 60000}m ${(millis % 60000) / 1000}s"
    }
}
