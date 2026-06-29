// app/src/main/java/com/example/ui/components/ModelSelectorUI.kt
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiModel
import com.example.data.api.ModelCategory
import com.example.data.api.ModelUsageTracker
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ModelSelectorBar(
    selectedModel: GeminiModel,
    onModelSelected: (GeminiModel) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var showModelInfo by remember { mutableStateOf<GeminiModel?>(null) }
    val scope = rememberCoroutineScope()
    val stats = remember(selectedModel) { ModelUsageTracker.getStats(selectedModel.modelId) }

    // Auto-refresh usage stats every second for live countdown
    var refreshTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshTick++
        }
    }

    Box(modifier = modifier) {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Selected Model Chip Button
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(24.dp),
            color = if (isEnabled) BalanceGold.copy(alpha = 0.1f) else MutedGrayDark.copy(alpha = 0.05f),
            border = BorderStroke(0.5.dp, if (isEnabled) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark),
            modifier = Modifier.testTag("model_selector_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(selectedModel.category.color))
                )

                Icon(
                    imageVector = selectedModel.icon,
                    contentDescription = null,
                    tint = BalanceGold,
                    modifier = Modifier.size(16.dp)
                )

                Column {
                    Text(
                        text = selectedModel.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isEnabled) MilkyWhiteText else MutedGrayDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedModel.category.label,
                            fontSize = 9.sp,
                            color = Color(selectedModel.category.color).copy(alpha = 0.8f)
                        )
                        Text(
                            text = "•",
                            fontSize = 6.sp,
                            color = MutedGrayDark
                        )
                        Text(
                            text = "${selectedModel.contextWindowFormatted} ctx",
                            fontSize = 9.sp,
                            color = MutedGrayDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Live usage bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Text(
                        text = "RPM",
                        fontSize = 7.sp,
                        color = MutedGrayDark.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats.remainingRpm}/${stats.rpmLimit}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            stats.isRpmExhausted -> ErrorRed
                            stats.rpmUsageFraction > 0.8f -> WarningAmber
                            else -> SuccessGreen
                        }
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Text(
                        text = "RPD",
                        fontSize = 7.sp,
                        color = MutedGrayDark.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats.remainingRpd}/${stats.rpdLimit}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            stats.isRpdExhausted -> ErrorRed
                            stats.rpdUsageFraction > 0.8f -> WarningAmber
                            else -> SuccessGreen
                        }
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select model",
                    tint = if (isEnabled) BalanceGold else MutedGrayDark.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Dropdown Menu — Full Model List
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier
                .width(380.dp)
                .heightIn(max = 520.dp)
                .background(ShadowBlackCard, RoundedCornerShape(16.dp))
                .border(0.5.dp, BalanceGold.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            // Header
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MilkyWhiteText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "RPM: per minute",
                            fontSize = 8.sp,
                            color = MutedGrayDark
                        )
                        Text(
                            text = "•",
                            fontSize = 8.sp,
                            color = MutedGrayDark
                        )
                        Text(
                            text = "RPD: per day",
                            fontSize = 8.sp,
                            color = MutedGrayDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Reset countdown banner
                val currentStats = remember(refreshTick) { ModelUsageTracker.getStats(selectedModel.modelId) }
                if (currentStats.rpmUsageFraction > 0f || currentStats.rpdUsageFraction > 0f) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ShadowBlack)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = BalanceGold.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Limits reset in ${currentStats.nextResetFormatted}",
                                fontSize = 10.sp,
                                color = BalanceGold.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Session: ${currentStats.sessionTotal}",
                                fontSize = 9.sp,
                                color = MutedGrayDark,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(color = BorderGrayDark, thickness = 0.5.dp)
            }

            // Model list grouped by category
            val categories = ModelCategory.entries.filter { cat ->
                GeminiModel.entries.any { it.category == cat }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                categories.forEach { category ->
                    val categoryModels = GeminiModel.entries.filter { it.category == category }
                    if (categoryModels.isNotEmpty()) {
                        item(key = "cat_${category.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(category.color))
                                )
                                Text(
                                    text = category.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = Color(category.color),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = category.description,
                                    fontSize = 9.sp,
                                    color = MutedGrayDark.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        items(categoryModels, key = { it.modelId }) { model ->
                            val modelStats = remember(refreshTick) { ModelUsageTracker.getStats(model.modelId) }
                            val isSelected = model == selectedModel
                            val isExhausted = modelStats.isRpmExhausted || modelStats.isRpdExhausted

                            ModelDropdownItem(
                                model = model,
                                stats = modelStats,
                                isSelected = isSelected,
                                isExhausted = isExhausted && !isSelected,
                                onClick = {
                                    if (!isExhausted || isSelected) {
                                        onModelSelected(model)
                                        expanded = false
                                    }
                                },
                                onInfoClick = { showModelInfo = model }
                            )
                        }

                        item(key = "spacer_${category.name}") {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Model Info Dialog
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    showModelInfo?.let { model ->
        val infoStats = remember(refreshTick) { ModelUsageTracker.getStats(model.modelId) }
        AlertDialog(
            onDismissRequest = { showModelInfo = null },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            textContentColor = MutedGrayDark,
            icon = {
                Icon(
                    model.icon,
                    contentDescription = null,
                    tint = BalanceGold,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(model.displayName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(model.description, fontSize = 13.sp, color = MutedGrayDark)

                    HorizontalDivider(color = BorderGrayDark)

                    // Specs grid
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        SpecItem("Context", model.contextWindowFormatted)
                        SpecItem("Max Output", "${model.maxOutputTokens / 1000}K tokens")
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        SpecItem("Temperature", model.defaultTemperature.toString())
                        SpecItem("Top-P", model.defaultTopP.toString())
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        SpecItem("Top-K", model.defaultTopK.toString())
                        SpecItem("Category", model.category.label)
                    }

                    HorizontalDivider(color = BorderGrayDark)

                    // Features
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        FeatureChip("Vision", model.supportsVision)
                        FeatureChip("Audio", model.supportsAudio)
                        FeatureChip("Video", model.supportsVideo)
                        FeatureChip("Grounding", model.supportsGrounding)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        FeatureChip("Thinking", model.supportsThinking)
                        FeatureChip("Caching", model.supportsCaching)
                        if (model.isPreview) FeatureChip("Preview", true)
                        if (model.isExperimental) FeatureChip("Experimental", true)
                    }

                    HorizontalDivider(color = BorderGrayDark)

                    // Live usage
                    Text("Live Usage", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MilkyWhiteText)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text("RPM", fontSize = 10.sp, color = MutedGrayDark)
                            Text("${infoStats.remainingRpm} / ${infoStats.rpmLimit} remaining", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (infoStats.isRpmExhausted) ErrorRed else SuccessGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RPD", fontSize = 10.sp, color = MutedGrayDark)
                            Text("${infoStats.remainingRpd} / ${infoStats.rpdLimit} remaining", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (infoStats.isRpdExhausted) ErrorRed else SuccessGreen)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { infoStats.rpmUsageFraction },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = when { infoStats.isRpmExhausted -> ErrorRed; infoStats.rpmUsageFraction > 0.8f -> WarningAmber; else -> BalanceGold },
                        trackColor = BorderGrayDark
                    )
                    LinearProgressIndicator(
                        progress = { infoStats.rpdUsageFraction },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = when { infoStats.isRpdExhausted -> ErrorRed; infoStats.rpdUsageFraction > 0.8f -> WarningAmber; else -> BalanceGold },
                        trackColor = BorderGrayDark
                    )
                    Text("Resets in ${infoStats.nextResetFormatted}", fontSize = 10.sp, color = MutedGrayDark)
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelInfo = null }) {
                    Text("Close", color = BalanceGold, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ModelDropdownItem(
    model: GeminiModel,
    stats: com.example.data.api.ModelUsageStats,
    isSelected: Boolean,
    isExhausted: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "modelItemScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(enabled = !isExhausted) { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> BalanceGold.copy(alpha = 0.12f)
                isExhausted -> ShadowBlack.copy(alpha = 0.3f)
                else -> Color.Transparent
            }
        ),
        border = when {
            isSelected -> BorderStroke(1.dp, BalanceGold.copy(alpha = 0.3f))
            else -> null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(model.category.color).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    model.icon,
                    contentDescription = null,
                    tint = if (isExhausted) MutedGrayDark.copy(alpha = 0.3f) else Color(model.category.color),
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        model.displayName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = when {
                            isSelected -> BalanceGold
                            isExhausted -> MutedGrayDark.copy(alpha = 0.4f)
                            else -> MilkyWhiteText
                        }
                    )
                    if (model.isPreview) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = WarningAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "PREVIEW",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = BalanceGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    model.description,
                    fontSize = 10.sp,
                    color = MutedGrayDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mini usage bars
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                Text("RPM", fontSize = 7.sp, color = MutedGrayDark.copy(alpha = 0.5f))
                Text(
                    "${stats.remainingRpm}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        stats.isRpmExhausted -> ErrorRed
                        stats.rpmUsageFraction > 0.8f -> WarningAmber
                        else -> SuccessGreen
                    }
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                Text("RPD", fontSize = 7.sp, color = MutedGrayDark.copy(alpha = 0.5f))
                Text(
                    "${stats.remainingRpd}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        stats.isRpdExhausted -> ErrorRed
                        stats.rpdUsageFraction > 0.8f -> WarningAmber
                        else -> SuccessGreen
                    }
                )
            }

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "Model info",
                    tint = MutedGrayDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = MutedGrayDark.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun FeatureChip(label: String, enabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (enabled) SuccessGreen.copy(alpha = 0.1f) else MutedGrayDark.copy(alpha = 0.05f)
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) SuccessGreen else MutedGrayDark.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
