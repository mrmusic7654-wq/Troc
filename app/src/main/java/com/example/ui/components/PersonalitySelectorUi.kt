// app/src/main/java/com/example/ui/components/PersonalitySelectorUI.kt
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.personality.CustomPersonality
import com.example.data.personality.PersonalityProfile
import com.example.ui.theme.*

@Composable
fun PersonalitySelectorBar(
    selectedPersonality: PersonalityProfile,
    onPersonalitySelected: (PersonalityProfile) -> Unit,
    onCustomPersonalityCreated: ((CustomPersonality) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomCreator by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Selected Personality Chip
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(24.dp),
            color = if (isEnabled) selectedPersonality.color.copy(alpha = 0.1f) else MutedGrayDark.copy(alpha = 0.05f),
            border = BorderStroke(0.5.dp, if (isEnabled) selectedPersonality.color.copy(alpha = 0.3f) else BorderGrayDark),
            modifier = Modifier.testTag("personality_selector_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedPersonality.emoji,
                    fontSize = 14.sp
                )
                Text(
                    text = selectedPersonality.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (isEnabled) MilkyWhiteText else MutedGrayDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select personality",
                    tint = if (isEnabled) selectedPersonality.color.copy(alpha = 0.7f) else MutedGrayDark.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Dropdown Menu
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 480.dp)
                .background(ShadowBlackCard, RoundedCornerShape(16.dp))
                .border(0.5.dp, BalanceGold.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Persona",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MilkyWhiteText
                    )
                    TextButton(
                        onClick = {
                            expanded = false
                            showCustomCreator = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = BalanceGold)
                        Spacer(Modifier.width(4.dp))
                        Text("Custom", fontSize = 11.sp, color = BalanceGold, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = BorderGrayDark)
                Spacer(Modifier.height(4.dp))
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(PersonalityProfile.PRESETS) { personality ->
                    val isSelected = personality.id == selectedPersonality.id
                    PersonalityDropdownItem(
                        personality = personality,
                        isSelected = isSelected,
                        onClick = {
                            onPersonalitySelected(personality)
                            expanded = false
                        }
                    )
                    if (personality != PersonalityProfile.PRESETS.last()) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Custom Personality Creator Dialog
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    if (showCustomCreator) {
        CustomPersonalityDialog(
            onDismiss = { showCustomCreator = false },
            onSave = { custom ->
                val profile = custom.toPersonalityProfile()
                onPersonalitySelected(profile)
                onCustomPersonalityCreated?.invoke(custom)
                showCustomCreator = false
            }
        )
    }
}

@Composable
private fun PersonalityDropdownItem(
    personality: PersonalityProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "personalityScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) personality.color.copy(alpha = 0.12f) else Color.Transparent
        ),
        border = if (isSelected) BorderStroke(1.dp, personality.color.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(personality.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(personality.emoji, fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        personality.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isSelected) personality.color else MilkyWhiteText
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = personality.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (personality.isCustom) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = BalanceGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "CUSTOM",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = BalanceGold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    personality.description,
                    fontSize = 11.sp,
                    color = MutedGrayDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PersonalityStatBadge("Temp", "${personality.temperature}")
                    PersonalityStatBadge("Tokens", "${personality.maxTokens / 1000}K")
                }
            }
        }
    }
}

@Composable
private fun PersonalityStatBadge(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, fontSize = 8.sp, color = MutedGrayDark.copy(alpha = 0.5f), letterSpacing = 0.5.sp)
        Text(value, fontSize = 9.sp, color = MutedGrayDark, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CustomPersonalityDialog(
    onDismiss: () -> Unit,
    onSave: (CustomPersonality) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎭") }
    var description by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf(0.7f) }
    var maxTokens by remember { mutableStateOf(4096) }
    val focusManager = LocalFocusManager.current

    val emojis = listOf("🎭", "🤖", "🦊", "🐉", "🌟", "🔥", "💎", "🌙", "⚡", "🦉", "🧙", "🎪", "👑", "🦋", "🎯")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ShadowBlackCard,
        titleContentColor = MilkyWhiteText,
        textContentColor = MutedGrayDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = BalanceGold, modifier = Modifier.size(22.dp))
                Text("Create Custom Persona", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Emoji Picker
                Text("Choose an emoji", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MilkyWhiteText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    emojis.take(8).forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (emoji == e) BalanceGold.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (emoji == e) BalanceGold else Color.Transparent, CircleShape)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(e, fontSize = 16.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    emojis.drop(8).forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (emoji == e) BalanceGold.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (emoji == e) BalanceGold else Color.Transparent, CircleShape)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(e, fontSize = 16.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Persona Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. My Coding Sensei", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlack, unfocusedContainerColor = ShadowBlack,
                        cursorColor = BalanceGold, focusedLabelColor = BalanceGold, unfocusedLabelColor = MutedGrayDark
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description", fontSize = 11.sp) },
                    placeholder = { Text("What does this persona do?", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlack, unfocusedContainerColor = ShadowBlack,
                        cursorColor = BalanceGold
                    )
                )

                OutlinedTextField(
                    value = tone,
                    onValueChange = { tone = it },
                    label = { Text("Tone Description", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Sarcastic, formal, playful...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlack, unfocusedContainerColor = ShadowBlack,
                        cursorColor = BalanceGold
                    )
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt", fontSize = 11.sp) },
                    placeholder = { Text("Detailed instructions for the AI...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 8,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlack, unfocusedContainerColor = ShadowBlack,
                        cursorColor = BalanceGold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Temperature: $temperature", fontSize = 10.sp, color = MutedGrayDark)
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0f..2f,
                            steps = 19,
                            colors = SliderDefaults.colors(thumbColor = BalanceGold, activeTrackColor = BalanceGold, inactiveTrackColor = BorderGrayDark)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Max Tokens: $maxTokens", fontSize = 10.sp, color = MutedGrayDark)
                        Slider(
                            value = maxTokens.toFloat(),
                            onValueChange = { maxTokens = it.toInt() },
                            valueRange = 256f..8192f,
                            steps = 31,
                            colors = SliderDefaults.colors(thumbColor = BalanceGold, activeTrackColor = BalanceGold, inactiveTrackColor = BorderGrayDark)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CustomPersonality(
                            name = name.ifBlank { "Custom Persona" },
                            emoji = emoji,
                            description = description,
                            tone = tone,
                            systemPrompt = systemPrompt.ifBlank { "You are a helpful AI assistant named ${name.ifBlank { "Custom Persona" }}." },
                            temperature = temperature,
                            maxTokens = maxTokens
                        )
                    )
                },
                enabled = name.isNotBlank() || systemPrompt.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BalanceGold, contentColor = ShadowBlack)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create Persona", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedGrayDark) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
