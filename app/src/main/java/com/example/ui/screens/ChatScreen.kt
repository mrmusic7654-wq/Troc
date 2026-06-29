// app/src/main/java/com/example/ui/screens/ChatScreen.kt
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.GeminiModel
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.data.filemanager.AttachedFile
import com.example.data.filemanager.FileUploadManager
import com.example.data.personality.PersonalityProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isDeepThinkingEnabled by viewModel.isDeepThinkingEnabled.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val selectedPersonality by viewModel.selectedPersonality.collectAsStateWithLifecycle()
    val contextTokenCount by viewModel.contextTokenCount.collectAsStateWithLifecycle()
    val contextUsageFraction by viewModel.contextUsageFraction.collectAsStateWithLifecycle()
    val currentThinkingProcess by viewModel.currentThinkingProcess.collectAsStateWithLifecycle()
    val generationProgress by viewModel.generationProgress.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }
    var showHistoryPanel by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val attachedFiles = FileUploadManager.getAttachedFiles()

    LaunchedEffect(activeMessages.size, isGenerating) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            YinYangLogo(
                                size = 32.dp,
                                isSpinning = isGenerating,
                                hexagonal = true,
                                modifier = Modifier.clickable {
                                    viewModel.sendMessage("☯️ Balance my day with a beautiful Yin-Yang thought!")
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Troc",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (isDeepThinkingEnabled) {
                                    Text(
                                        text = "Deep Think Active",
                                        fontSize = 9.sp,
                                        color = BalanceGold,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Rounded.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        ModelSelectorBar(
                            selectedModel = selectedModel,
                            onModelSelected = { viewModel.setModel(it) },
                            isEnabled = !isGenerating
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { isDarkMode = !isDarkMode }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                "Toggle dark mode",
                                tint = if (isDarkMode) BalanceGold else MutedGrayDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.startNewChat() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.AddCircleOutline, "New Chat", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                PersonalitySelectorBar(
                    selectedPersonality = selectedPersonality,
                    onPersonalitySelected = { viewModel.setPersonality(it) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    isEnabled = !isGenerating
                )

                if (activeSessionId != null && activeMessages.isNotEmpty()) {
                    Surface(modifier = Modifier.fillMaxWidth(), color = ShadowBlackCard) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Memory, null,
                                        tint = when { contextUsageFraction > 0.9f -> ErrorRed; contextUsageFraction > 0.7f -> WarningAmber; else -> BalanceGold },
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("${activeMessages.size} messages", fontSize = 10.sp, color = MutedGrayDark, fontFamily = FontFamily.Monospace)
                                }
                                Text(
                                    "${(contextUsageFraction * 100).toInt()}% of 1M • ${if (contextTokenCount >= 1000) "${contextTokenCount / 1000}K" else "$contextTokenCount"} tokens",
                                    fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                    color = when { contextUsageFraction > 0.9f -> ErrorRed; contextUsageFraction > 0.7f -> WarningAmber; else -> SuccessGreen }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { contextUsageFraction },
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = when { contextUsageFraction > 0.9f -> ErrorRed; contextUsageFraction > 0.7f -> WarningAmber; else -> BalanceGold },
                                trackColor = BorderGrayDark
                            )
                        }
                    }
                }

                if (isGenerating) {
                    LinearProgressIndicator(
                        progress = { generationProgress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = BalanceGold, trackColor = BorderGrayDark
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeMessages.isEmpty() && activeSessionId == null) {
                        DashboardIntro(
                            onSuggestionClick = { viewModel.sendMessage(it) },
                            onPromptSubmit = { viewModel.sendMessage(it) },
                            isGenerating = isGenerating,
                            selectedModel = selectedModel,
                            selectedPersonality = selectedPersonality
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeMessages) { message ->
                                MessageBubble(message = message, isLightForce = !isDarkMode)
                            }
                            if (isGenerating && currentThinkingProcess != null) {
                                item { ReasoningCardLive(reasoning = currentThinkingProcess ?: "", isStreaming = true) }
                            }
                            if (isGenerating) {
                                item { AssistantThinkingBubble() }
                            }
                        }
                    }
                }

                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
                        border = BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, "Error", tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
                        }
                    }
                }

                ChatHistoryTogglePanel(
                    expanded = showHistoryPanel,
                    sessions = sessions,
                    activeSessionId = activeSessionId,
                    activeMessages = activeMessages,
                    onToggle = { showHistoryPanel = !showHistoryPanel },
                    onSessionSelect = { viewModel.selectSession(it) },
                    onContinueSession = { viewModel.continueSession(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                    onNewChat = { viewModel.startNewChat() }
                )

                if (attachedFiles.isNotEmpty()) {
                    Surface(modifier = Modifier.fillMaxWidth(), color = ShadowBlackCard, border = BorderStroke(0.5.dp, BorderGrayDark)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(attachedFiles.toList(), key = { it.id }) { file ->
                                    Card(
                                        modifier = Modifier.width(110.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = ShadowBlack),
                                        border = BorderStroke(0.5.dp, BorderGrayDark)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(file.icon, fontSize = 12.sp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.fileName, fontSize = 9.sp, color = MilkyWhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(file.formattedSize, fontSize = 8.sp, color = MutedGrayDark, fontFamily = FontFamily.Monospace)
                                            }
                                            IconButton(onClick = { FileUploadManager.removeFile(file.id) }, modifier = Modifier.size(16.dp)) {
                                                Icon(Icons.Rounded.Close, "Remove", tint = MutedGrayDark, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { FileUploadManager.clearAll() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.ClearAll, "Clear all", tint = MutedGrayDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background, shadowElevation = 12.dp) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FileUploadBar(onFilesChanged = {}, enabled = !isGenerating, compact = true)

                            OutlinedTextField(
                                value = inputPrompt,
                                onValueChange = { inputPrompt = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Harmonize your thoughts...", color = MutedGrayDark.copy(alpha = 0.5f), fontSize = 13.sp) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BalanceGold.copy(alpha = 0.4f), unfocusedBorderColor = BorderGrayDark,
                                    focusedContainerColor = ShadowBlackCard, unfocusedContainerColor = ShadowBlackCard,
                                    cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText, unfocusedTextColor = MilkyWhiteText
                                ),
                                maxLines = 3,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            val canSend = (inputPrompt.isNotBlank() || attachedFiles.isNotEmpty()) && !isGenerating
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        viewModel.sendMessage(inputPrompt)
                                        inputPrompt = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = if (canSend) BalanceGold else MutedGrayDark.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { viewModel.isDeepThinkingEnabled.value = !viewModel.isDeepThinkingEnabled.value },
                                label = { Text("Deep Think", fontSize = 10.sp, fontWeight = if (isDeepThinkingEnabled) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = { Icon(Icons.Rounded.Psychology, null, Modifier.size(12.dp), tint = if (isDeepThinkingEnabled) BalanceGold else MutedGrayDark) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = if (isDeepThinkingEnabled) BalanceGold.copy(alpha = 0.12f) else Color.Transparent, labelColor = if (isDeepThinkingEnabled) BalanceGold else MutedGrayDark),
                                border = AssistChipDefaults.assistChipBorder(borderColor = if (isDeepThinkingEnabled) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark, enabled = true),
                                modifier = Modifier.height(28.dp)
                            )

                            IconButton(onClick = { viewModel.isListening.value = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Mic, "Voice", tint = MutedGrayDark, modifier = Modifier.size(16.dp))
                            }

                            Spacer(Modifier.weight(1f))

                            Text(
                                if (isGenerating) "Generating..." else "Ready",
                                fontSize = 9.sp,
                                color = if (isGenerating) BalanceGold else MutedGrayDark.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            if (isListening) {
                VoiceInputOverlay(
                    onDismiss = { viewModel.isListening.value = false },
                    onResult = { result ->
                        inputPrompt = result
                        viewModel.isListening.value = false
                        if (result.isNotBlank()) viewModel.sendMessage(result)
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT HISTORY TOGGLE PANEL
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ChatHistoryTogglePanel(
    expanded: Boolean,
    sessions: List<ChatSession>,
    activeSessionId: Long?,
    activeMessages: List<ChatMessage>,
    onToggle: () -> Unit,
    onSessionSelect: (Long) -> Unit,
    onContinueSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onNewChat: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(onClick = onToggle, modifier = Modifier.fillMaxWidth(), color = ShadowBlackCard, border = BorderStroke(0.5.dp, BorderGrayDark)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ChatBubble, null, tint = BalanceGold, modifier = Modifier.size(16.dp))
                    Text("Troc Conversations", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MilkyWhiteText)
                    Surface(shape = RoundedCornerShape(8.dp), color = BalanceGold.copy(alpha = 0.15f)) {
                        Text("${sessions.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BalanceGold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Icon(if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp, "Toggle", tint = MutedGrayDark, modifier = Modifier.size(20.dp))
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300))
        ) {
            Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), color = ShadowBlack, border = BorderStroke(0.5.dp, BorderGrayDark)) {
                if (sessions.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Inbox, null, tint = MutedGrayDark.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                            Text("No saved conversations", fontSize = 13.sp, color = MutedGrayDark.copy(alpha = 0.5f))
                            Text("Start a new chat to begin", fontSize = 11.sp, color = MutedGrayDark.copy(alpha = 0.3f))
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(sessions, key = { it.id }) { session ->
                            val isActive = session.id == activeSessionId
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onContinueSession(session.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isActive) BalanceGold.copy(alpha = 0.08f) else Color.Transparent),
                                border = if (isActive) BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(if (isActive) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline, null, tint = if (isActive) BalanceGold else MutedGrayDark, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.displayTitle, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp, color = if (isActive) BalanceGold else MilkyWhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${session.messageCount} msgs", fontSize = 10.sp, color = MutedGrayDark)
                                            Text("•", fontSize = 10.sp, color = MutedGrayDark)
                                            Text(session.formattedDate, fontSize = 10.sp, color = MutedGrayDark)
                                            if (session.modelUsed.isNotBlank()) { Text("•", fontSize = 10.sp, color = MutedGrayDark); Text(session.modelDisplayName, fontSize = 10.sp, color = MutedGrayDark.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        }
                                        if (session.hasPersonality) Text(session.personalityName, fontSize = 9.sp, color = MutedGrayDark.copy(alpha = 0.5f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(onClick = { onRenameSession(session.id, session.title) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Edit, "Rename", tint = MutedGrayDark, modifier = Modifier.size(14.dp)) }
                                        IconButton(onClick = { onDeleteSession(session.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Delete, "Delete", tint = MutedGrayDark.copy(alpha = 0.5f), modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DASHBOARD INTRO
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DashboardIntro(
    onSuggestionClick: (String) -> Unit,
    onPromptSubmit: (String) -> Unit,
    isGenerating: Boolean,
    selectedModel: GeminiModel,
    selectedPersonality: PersonalityProfile
) {
    var customPrompt by remember { mutableStateOf("") }
    val suggestions = listOf(
        "Build a meditation app" to "🧘", "Create a video script" to "🎬",
        "Write a poem about duality" to "✍️", "Explain quantum computing" to "⚛️",
        "Generate a workout plan" to "💪", "Design a logo concept" to "🎨"
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        YinYangLogo(size = 100.dp, isSpinning = isGenerating, hexagonal = true)
        Spacer(Modifier.height(24.dp))
        Text("Troc Agent", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(selectedModel.category.color).copy(alpha = 0.1f), border = BorderStroke(0.5.dp, Color(selectedModel.category.color).copy(alpha = 0.3f))) {
                Text("${selectedPersonality.emoji} ${selectedModel.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Surface(shape = RoundedCornerShape(12.dp), color = selectedPersonality.color.copy(alpha = 0.1f), border = BorderStroke(0.5.dp, selectedPersonality.color.copy(alpha = 0.3f))) {
                Text(selectedPersonality.name, fontSize = 11.sp, color = MilkyWhiteText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Your harmonized AI companion. Build apps, generate videos,\nwrite code, or explore ideas — all in perfect balance.", fontSize = 13.sp, color = MutedGrayDark, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = customPrompt, onValueChange = { customPrompt = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Describe what you want to create...", color = MutedGrayDark.copy(alpha = 0.5f), fontSize = 14.sp) },
            trailingIcon = { if (customPrompt.isNotBlank()) IconButton(onClick = { onPromptSubmit(customPrompt); customPrompt = "" }) { Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = BalanceGold) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark, focusedContainerColor = ShadowBlackCard, unfocusedContainerColor = ShadowBlackCard, cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText, unfocusedTextColor = MilkyWhiteText),
            singleLine = true, textStyle = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Text("Try asking...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedGrayDark, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        suggestions.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (text, emoji) -> SuggestionChip(emoji, text, onClick = { onSuggestionClick(text) }, modifier = Modifier.weight(1f)) }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SuggestionChip(emoji: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(12.dp), color = ShadowBlackCard, border = BorderStroke(0.5.dp, BorderGrayDark)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp)
            Text(text, fontSize = 12.sp, color = MilkyWhiteText.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MESSAGE BUBBLE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MessageBubble(message: ChatMessage, isLightForce: Boolean, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) { if (isLightForce) ShadowBlack else BalanceGold.copy(alpha = 0.15f) } else { if (isLightForce) MilkyWhiteCard else ShadowBlackCard }
    val bubbleShape = if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    val resolvedModel = remember(message.modelUsed) { if (message.modelUsed != null) try { GeminiModel.fromModelId(message.modelUsed) } catch (_: Exception) { null } else null }
    val resolvedPersonality = remember(message.personalityUsed) { if (message.personalityUsed != null) try { PersonalityProfile.fromId(message.personalityUsed) } catch (_: Exception) { null } else null }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = alignment) {
        if (!isUser && message.reasoning != null) { ReasoningCard(message.reasoning, message.durationMs); Spacer(Modifier.height(6.dp)) }
        if (!isUser && (resolvedModel != null || resolvedPersonality != null)) {
            Row(modifier = Modifier.padding(bottom = 4.dp, start = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                resolvedModel?.let { model -> Surface(shape = RoundedCornerShape(4.dp), color = Color(model.category.color).copy(alpha = 0.1f)) { Text(model.displayName, fontSize = 8.sp, color = MutedGrayDark.copy(alpha = 0.6f), fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) } }
                resolvedPersonality?.let { personality -> Text("${personality.emoji} ${personality.name}", fontSize = 8.sp, color = MutedGrayDark.copy(alpha = 0.6f)) }
            }
        }
        Box(Modifier.widthIn(max = 320.dp).clip(bubbleShape).background(bubbleColor).border(0.5.dp, if (isUser) BorderGrayDark else BorderGrayDark.copy(alpha = 0.3f), bubbleShape).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(message.text, color = if (isUser) MilkyWhiteText else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp)
        }
        if (!isUser && message.durationMs != null) Text("Generated in ${message.durationMs / 1000.0}s", fontSize = 10.sp, color = MutedGrayDark, modifier = Modifier.padding(top = 2.dp, start = 8.dp))
    }
}

@Composable
private fun ReasoningCard(reasoning: String, durationMs: Long?, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = modifier.widthIn(max = 300.dp).clickable { expanded = !expanded }, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BalanceGold.copy(alpha = 0.05f)), border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                Icon(Icons.Rounded.Psychology, null, tint = BalanceGold, modifier = Modifier.size(14.dp))
                Text("Deep Thought Process", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BalanceGold)
                if (durationMs != null) Text("• ${durationMs / 1000.0}s", fontSize = 10.sp, color = MutedGrayDark)
                Spacer(Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "Toggle", tint = MutedGrayDark, modifier = Modifier.size(16.dp))
            }
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(reasoning, fontSize = 12.sp, color = MutedGrayDark, lineHeight = 18.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ReasoningCardLive(reasoning: String, isStreaming: Boolean, modifier: Modifier = Modifier) {
    val shimmerAlpha by rememberInfiniteTransition().animateFloat(0.3f, 0.6f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Card(modifier = modifier.widthIn(max = 300.dp).padding(start = 8.dp, top = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BalanceGold.copy(alpha = if (isStreaming) shimmerAlpha * 0.1f else 0.05f)), border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = if (isStreaming) shimmerAlpha else 0.2f))) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                if (isStreaming) CircularProgressIndicator(Modifier.size(12.dp), color = BalanceGold, strokeWidth = 1.5.dp)
                Icon(Icons.Rounded.Psychology, null, tint = BalanceGold, modifier = Modifier.size(14.dp))
                Text(if (isStreaming) "Thinking..." else "Thought Process", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BalanceGold)
            }
            Spacer(Modifier.height(6.dp))
            Text(reasoning, fontSize = 12.sp, color = MutedGrayDark, lineHeight = 18.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun AssistantThinkingBubble(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dot1Alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot1")
    val dot2Alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), label = "dot2")
    val dot3Alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), label = "dot3")
    Row(modifier = modifier.padding(start = 8.dp, top = 8.dp).clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).background(ShadowBlackCard).border(0.5.dp, BalanceGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        YinYangLogo(size = 16.dp, isSpinning = true, hexagonal = true)
        Spacer(Modifier.width(4.dp))
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha -> Box(Modifier.size(5.dp).graphicsLayer(alpha = alpha).clip(CircleShape).background(BalanceGold)) }
    }
}
