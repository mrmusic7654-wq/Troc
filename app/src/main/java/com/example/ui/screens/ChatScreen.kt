// app/src/main/java/com/example/ui/screens/ChatScreen.kt
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

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
    var showRightDrawer by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(true) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    var renameInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val attachedFiles = FileUploadManager.getAttachedFiles()

    LaunchedEffect(activeMessages.size, isGenerating) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // MAIN CONTENT
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                YinYangLogo(
                                    size = 28.dp,
                                    isSpinning = isGenerating,
                                    modifier = Modifier.clickable {
                                        viewModel.sendMessage("☯️ Balance my day with a beautiful Yin-Yang thought!")
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Troc",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Rounded.Menu, "Workspaces", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        actions = {
                            ModelSelectorBar(
                                selectedModel = selectedModel,
                                onModelSelected = { viewModel.setModel(it) },
                                isEnabled = !isGenerating
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(onClick = { showRightDrawer = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.ChatBubble, "Chat History", tint = BalanceGold, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { isDarkMode = !isDarkMode }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                    "Dark mode",
                                    tint = MutedGrayDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.startNewChat() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.AddCircleOutline, "New Chat", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
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
                        ContextBar(
                            messageCount = activeMessages.size,
                            usageFraction = contextUsageFraction,
                            tokenCount = contextTokenCount
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Messages
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
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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

                // Error
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
                        border = BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Text(error, fontSize = 11.sp, color = ErrorRed, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Attached files
                if (attachedFiles.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        attachedFiles.take(3).forEach { file ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ShadowBlackCard,
                                border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(file.icon, fontSize = 11.sp)
                                    Text(file.fileName.take(12), fontSize = 10.sp, color = MilkyWhiteText, maxLines = 1)
                                    IconButton(onClick = { FileUploadManager.removeFile(file.id) }, modifier = Modifier.size(16.dp)) {
                                        Icon(Icons.Rounded.Close, null, tint = MutedGrayDark, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                        if (attachedFiles.size > 3) {
                            Text("+${attachedFiles.size - 3}", fontSize = 10.sp, color = MutedGrayDark)
                        }
                    }
                }

                // Input bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ShadowBlackCard,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        FileUploadBar(onFilesChanged = {}, enabled = !isGenerating, compact = true)

                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp, max = 100.dp),
                            placeholder = {
                                Text(
                                    "Ask anything...",
                                    color = MutedGrayDark.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BalanceGold.copy(alpha = 0.4f),
                                unfocusedBorderColor = BorderGrayDark,
                                focusedContainerColor = ShadowBlack.copy(alpha = 0.4f),
                                unfocusedContainerColor = ShadowBlack.copy(alpha = 0.4f),
                                cursorColor = BalanceGold,
                                focusedTextColor = MilkyWhiteText,
                                unfocusedTextColor = MilkyWhiteText
                            ),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        val canSend = (inputPrompt.isNotBlank() || attachedFiles.isNotEmpty()) && !isGenerating
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (canSend) BalanceGold else BorderGrayDark.copy(alpha = 0.3f),
                            shadowElevation = if (canSend) 4.dp else 0.dp
                        ) {
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        viewModel.sendMessage(inputPrompt)
                                        inputPrompt = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send, "Send",
                                    tint = if (canSend) ShadowBlack else MutedGrayDark.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom micro-bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { viewModel.isDeepThinkingEnabled.value = !viewModel.isDeepThinkingEnabled.value },
                        label = { Text("Deep Think", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Psychology, null, Modifier.size(12.dp),
                                tint = if (isDeepThinkingEnabled) BalanceGold else MutedGrayDark)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDeepThinkingEnabled) BalanceGold.copy(alpha = 0.12f) else Color.Transparent,
                            labelColor = if (isDeepThinkingEnabled) BalanceGold else MutedGrayDark
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = if (isDeepThinkingEnabled) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark, enabled = true
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                    IconButton(onClick = { viewModel.isListening.value = true }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.Mic, "Voice", tint = MutedGrayDark, modifier = Modifier.size(14.dp))
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

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RIGHT DRAWER — Chat History (ChatGPT-style)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        AnimatedVisibility(
            visible = showRightDrawer,
            enter = fadeIn(tween(200)) + slideInHorizontally(tween(300)) { it },
            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showRightDrawer = false }
                )
                // Drawer panel
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(300.dp),
                    color = ShadowBlack,
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Surface(color = ShadowBlackCard) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.ChatBubble, null, tint = BalanceGold, modifier = Modifier.size(20.dp))
                                        Text("Chat History", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MilkyWhiteText)
                                    }
                                    IconButton(onClick = { showRightDrawer = false }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Close, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.startNewChat(); showRightDrawer = false },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BalanceGold, contentColor = ShadowBlack),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("New Chat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGrayDark)

                        // Sessions list
                        if (sessions.isEmpty()) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Inbox, null, tint = MutedGrayDark.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
                                    Text("No conversations yet", fontSize = 12.sp, color = MutedGrayDark.copy(alpha = 0.4f))
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(sessions, key = { it.id }) { session ->
                                    val isActive = session.id == activeSessionId
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.selectSession(session.id)
                                            showRightDrawer = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isActive) BalanceGold.copy(alpha = 0.08f) else Color.Transparent
                                        ),
                                        border = if (isActive) BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f)) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isActive) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                                                null, tint = if (isActive) BalanceGold else MutedGrayDark,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    session.displayTitle,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = if (isActive) BalanceGold else MilkyWhiteText,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${session.messageCount} msgs • ${session.formattedDate}",
                                                    fontSize = 9.sp, color = MutedGrayDark
                                                )
                                            }
                                            IconButton(onClick = {
                                                renameTarget = session.id
                                                renameInput = session.title
                                            }, modifier = Modifier.size(22.dp)) {
                                                Icon(Icons.Rounded.Edit, null, tint = MutedGrayDark.copy(alpha = 0.4f), modifier = Modifier.size(11.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteSession(session.id) }, modifier = Modifier.size(22.dp)) {
                                                Icon(Icons.Rounded.Delete, null, tint = MutedGrayDark.copy(alpha = 0.4f), modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGrayDark)

                        // Footer actions
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (sessions.isNotEmpty()) {
                                TextButton(
                                    onClick = { showDeleteAllDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.DeleteSweep, null, tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Clear All", fontSize = 12.sp, color = ErrorRed.copy(alpha = 0.7f))
                                }
                            }
                            Text(
                                "Troc Agent v1.0",
                                fontSize = 9.sp,
                                color = MutedGrayDark.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // VOICE OVERLAY
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
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

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DIALOGS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            icon = { Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(24.dp)) },
            title = { Text("Clear All Conversations?") },
            text = { Text("This will permanently delete all ${sessions.size} sessions.", fontSize = 12.sp, color = MutedGrayDark) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllHistory(); showDeleteAllDialog = false }) {
                    Text("Delete All", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel", color = MutedGrayDark) } },
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlack, unfocusedContainerColor = ShadowBlack,
                        cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameSession(renameTarget!!, renameInput)
                    renameTarget = null
                }) { Text("Save", color = BalanceGold, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel", color = MutedGrayDark) } },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CONTEXT BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ContextBar(messageCount: Int, usageFraction: Float, tokenCount: Int) {
    Surface(modifier = Modifier.fillMaxWidth(), color = ShadowBlackCard) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Memory, null, tint = BalanceGold.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                Text("${messageCount} msgs", fontSize = 9.sp, color = MutedGrayDark, fontFamily = FontFamily.Monospace)
            }
            Text(
                "${(usageFraction * 100).toInt()}% of 1M",
                fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                color = when { usageFraction > 0.9f -> ErrorRed; usageFraction > 0.7f -> WarningAmber; else -> SuccessGreen }
            )
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
        "Explain quantum computing" to "⚛️",
        "Write a poem about duality" to "✍️",
        "Build a meditation app" to "🧘",
        "Create a video script" to "🎬"
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        YinYangLogo(size = 80.dp, isSpinning = isGenerating)
        Spacer(Modifier.height(20.dp))
        Text("Troc Agent", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color(selectedModel.category.color).copy(alpha = 0.1f), border = BorderStroke(0.5.dp, Color(selectedModel.category.color).copy(alpha = 0.2f))) {
                Text("${selectedPersonality.emoji} ${selectedModel.displayName}", fontSize = 10.sp, color = MilkyWhiteText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = customPrompt, onValueChange = { customPrompt = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Describe what you want to create...", color = MutedGrayDark.copy(alpha = 0.5f), fontSize = 14.sp) },
            trailingIcon = { if (customPrompt.isNotBlank()) IconButton(onClick = { onPromptSubmit(customPrompt); customPrompt = "" }) { Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = BalanceGold) } },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark, focusedContainerColor = ShadowBlackCard, unfocusedContainerColor = ShadowBlackCard, cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText, unfocusedTextColor = MilkyWhiteText),
            singleLine = true, textStyle = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))
        Text("Try asking...", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MutedGrayDark, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        suggestions.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (text, emoji) -> SuggestionChip(emoji, text, onClick = { onSuggestionClick(text) }, modifier = Modifier.weight(1f)) }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SuggestionChip(emoji: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(10.dp), color = ShadowBlackCard, border = BorderStroke(0.5.dp, BorderGrayDark)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 13.sp)
            Text(text, fontSize = 11.sp, color = MilkyWhiteText.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val bubbleShape = if (isUser) RoundedCornerShape(14.dp, 4.dp, 14.dp, 14.dp) else RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)

    val resolvedModel = remember(message.modelUsed) {
        if (message.modelUsed != null) try { GeminiModel.fromModelId(message.modelUsed) } catch (_: Exception) { null } else null
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalAlignment = alignment) {
        if (!isUser && message.reasoning != null) {
            ReasoningCard(message.reasoning, message.durationMs)
            Spacer(Modifier.height(4.dp))
        }
        if (!isUser && resolvedModel != null) {
            Text(resolvedModel.displayName, fontSize = 8.sp, color = MutedGrayDark.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 2.dp, start = 4.dp))
        }
        Box(
            Modifier.widthIn(max = 300.dp).clip(bubbleShape).background(bubbleColor)
                .border(0.5.dp, if (isUser) BorderGrayDark else BorderGrayDark.copy(alpha = 0.3f), bubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(message.text, color = if (isUser) MilkyWhiteText else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 19.sp)
        }
        if (!isUser && message.durationMs != null) {
            Text("${message.durationMs / 1000.0}s", fontSize = 9.sp, color = MutedGrayDark, modifier = Modifier.padding(top = 1.dp, start = 6.dp))
        }
    }
}

@Composable
private fun ReasoningCard(reasoning: String, durationMs: Long?, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.widthIn(max = 280.dp).clickable { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BalanceGold.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                Icon(Icons.Rounded.Psychology, null, tint = BalanceGold, modifier = Modifier.size(12.dp))
                Text("Thought Process", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BalanceGold)
                if (durationMs != null) Text("• ${durationMs / 1000.0}s", fontSize = 9.sp, color = MutedGrayDark)
                Spacer(Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MutedGrayDark, modifier = Modifier.size(14.dp))
            }
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(reasoning, fontSize = 11.sp, color = MutedGrayDark, lineHeight = 16.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ReasoningCardLive(reasoning: String, isStreaming: Boolean, modifier: Modifier = Modifier) {
    val shimmerAlpha by rememberInfiniteTransition().animateFloat(0.3f, 0.5f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Card(
        modifier = modifier.widthIn(max = 280.dp).padding(start = 6.dp, top = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BalanceGold.copy(alpha = if (isStreaming) shimmerAlpha * 0.08f else 0.04f)),
        border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = if (isStreaming) shimmerAlpha else 0.15f))
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                if (isStreaming) CircularProgressIndicator(Modifier.size(10.dp), color = BalanceGold, strokeWidth = 1.5.dp)
                Icon(Icons.Rounded.Psychology, null, tint = BalanceGold, modifier = Modifier.size(12.dp))
                Text(if (isStreaming) "Thinking..." else "Thought Process", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BalanceGold)
            }
            Spacer(Modifier.height(4.dp))
            Text(reasoning, fontSize = 11.sp, color = MutedGrayDark, lineHeight = 16.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun AssistantThinkingBubble(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dot1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, 150), RepeatMode.Reverse), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, 300), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = modifier.padding(start = 6.dp, top = 6.dp)
            .clip(RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp))
            .background(ShadowBlackCard)
            .border(0.5.dp, BalanceGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YinYangLogo(size = 14.dp, isSpinning = true)
        Spacer(Modifier.width(2.dp))
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(Modifier.size(4.dp).graphicsLayer(alpha = alpha).clip(CircleShape).background(BalanceGold))
        }
    }
}
