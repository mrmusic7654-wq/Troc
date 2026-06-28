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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChatMessage
import com.example.ui.components.CodeBlock
import com.example.ui.components.YinYangLogo
import com.example.ui.components.VoiceInputOverlay
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

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(activeMessages.size, isGenerating) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        YinYangLogo(
                            size = 28.dp,
                            isSpinning = isGenerating,
                            modifier = Modifier.clickable {
                                viewModel.sendMessage("☯️ Balance my day with a beautiful Yin-Yang thought!")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Troc",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
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
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("drawer_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Open workspace menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startNewChat() },
                        modifier = Modifier.testTag("scaffold_new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = "New Balance Chat",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeMessages.isEmpty() && activeSessionId == null) {
                        DashboardIntro(
                            onSuggestionClick = { suggestion -> viewModel.sendMessage(suggestion) },
                            onPromptSubmit = { prompt ->
                                inputPrompt = prompt
                                viewModel.sendMessage(prompt)
                            },
                            isGenerating = isGenerating
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeMessages) { message ->
                                MessageBubble(
                                    message = message,
                                    isLightForce = MaterialTheme.colorScheme.background == MilkyWhite
                                )
                            }

                            if (isGenerating) {
                                item { AssistantThinkingBubble() }
                            }
                        }
                    }
                }

                errorMessage?.let { error -> ErrorBanner(error = error) }

                BottomChatControls(
                    prompt = inputPrompt,
                    onPromptChange = { inputPrompt = it },
                    isDeepThinkingEnabled = isDeepThinkingEnabled,
                    onDeepThinkingToggle = { viewModel.isDeepThinkingEnabled.value = !isDeepThinkingEnabled },
                    onSendClick = {
                        if (inputPrompt.isNotBlank()) {
                            viewModel.sendMessage(inputPrompt)
                            inputPrompt = ""
                            focusManager.clearFocus()
                        }
                    },
                    onMicClick = { viewModel.isListening.value = true },
                    isGenerating = isGenerating
                )
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

@Composable
private fun DashboardIntro(
    onSuggestionClick: (String) -> Unit,
    onPromptSubmit: (String) -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    var customPrompt by remember { mutableStateOf("") }

    val suggestions = listOf(
        "Build a meditation app" to "🧘",
        "Create a video script" to "🎬",
        "Write a poem about duality" to "✍️",
        "Explain quantum computing" to "⚛️",
        "Generate a workout plan" to "💪",
        "Design a logo concept" to "🎨"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        YinYangLogo(size = 100.dp, isSpinning = isGenerating)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Troc Agent",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your harmonized AI companion. Build apps, generate videos,\nwrite code, or explore ideas — all in perfect balance.",
            fontSize = 13.sp,
            color = MutedGrayDark,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = customPrompt,
            onValueChange = { customPrompt = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Describe what you want to create...", color = MutedGrayDark.copy(alpha = 0.5f), fontSize = 14.sp) },
            trailingIcon = {
                if (customPrompt.isNotBlank()) {
                    IconButton(onClick = { onPromptSubmit(customPrompt); customPrompt = "" }) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = BalanceGold)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BalanceGold,
                unfocusedBorderColor = BorderGrayDark,
                focusedContainerColor = ShadowBlackCard,
                unfocusedContainerColor = ShadowBlackCard,
                cursorColor = BalanceGold,
                focusedTextColor = MilkyWhiteText,
                unfocusedTextColor = MilkyWhiteText
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Try asking...",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedGrayDark,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        suggestions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (text, emoji) ->
                    SuggestionChip(emoji = emoji, text = text, onClick = { onSuggestionClick(text) }, modifier = Modifier.weight(1f))
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SuggestionChip(emoji: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = ShadowBlackCard,
        border = BorderStroke(0.5.dp, BorderGrayDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(text = text, fontSize = 12.sp, color = MilkyWhiteText.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isLightForce: Boolean, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        if (isLightForce) ShadowBlack else BalanceGold.copy(alpha = 0.15f)
    } else {
        if (isLightForce) MilkyWhiteCard else ShadowBlackCard
    }
    val bubbleShape = if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!isUser && message.reasoning != null) {
            ReasoningCard(reasoning = message.reasoning, durationMs = message.durationMs)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .border(0.5.dp, if (isUser) BorderGrayDark else BorderGrayDark.copy(alpha = 0.3f), bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MilkyWhiteText else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (!isUser && message.durationMs != null) {
            Text(
                text = "Generated in ${message.durationMs / 1000.0}s",
                fontSize = 10.sp,
                color = MutedGrayDark,
                modifier = Modifier.padding(top = 2.dp, start = 8.dp)
            )
        }
    }
}

@Composable
private fun ReasoningCard(reasoning: String, durationMs: Long?, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.widthIn(max = 300.dp).clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BalanceGold.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = BalanceGold, modifier = Modifier.size(14.dp))
                Text("Deep Thought Process", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BalanceGold)
                if (durationMs != null) Text("• ${durationMs / 1000.0}s", fontSize = 10.sp, color = MutedGrayDark)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Toggle",
                    tint = MutedGrayDark,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(
                    reasoning,
                    fontSize = 12.sp,
                    color = MutedGrayDark,
                    lineHeight = 18.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantThinkingBubble(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dot1Alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot1")
    val dot2Alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), label = "dot2")
    val dot3Alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), label = "dot3")

    Row(
        modifier = modifier
            .padding(start = 8.dp, top = 8.dp)
            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .background(ShadowBlackCard)
            .border(0.5.dp, BalanceGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        YinYangLogo(size = 16.dp, isSpinning = true)
        Spacer(modifier = Modifier.width(4.dp))
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
            Box(Modifier.size(5.dp).graphicsLayer(alpha = alpha).clip(CircleShape).background(BalanceGold))
        }
    }
}

@Composable
private fun ErrorBanner(error: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1CEF5350)),
        border = BorderStroke(0.5.dp, Color(0xFFEF5350).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF5350), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomChatControls(
    prompt: String,
    onPromptChange: (String) -> Unit,
    isDeepThinkingEnabled: Boolean,
    onDeepThinkingToggle: () -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onDeepThinkingToggle,
                    label = { Text("Deep Think", fontSize = 11.sp, fontWeight = if (isDeepThinkingEnabled) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDeepThinkingEnabled) BalanceGold else MutedGrayDark)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isDeepThinkingEnabled) BalanceGold.copy(alpha = 0.12f) else Color.Transparent,
                        labelColor = if (isDeepThinkingEnabled) BalanceGold else MutedGrayDark
                    ),
                    border = AssistChipDefaults.assistChipBorder(borderColor = if (isDeepThinkingEnabled) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark, enabled = true)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Harmonize your thoughts...", color = MutedGrayDark.copy(alpha = 0.5f), fontSize = 13.sp) },
                trailingIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onMicClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Voice input", tint = MutedGrayDark, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onSendClick, enabled = prompt.isNotBlank() && !isGenerating, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send",
                                tint = if (prompt.isNotBlank() && !isGenerating) BalanceGold else MutedGrayDark.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BalanceGold.copy(alpha = 0.5f),
                    unfocusedBorderColor = BorderGrayDark,
                    focusedContainerColor = ShadowBlackCard,
                    unfocusedContainerColor = ShadowBlackCard,
                    cursorColor = BalanceGold,
                    focusedTextColor = MilkyWhiteText,
                    unfocusedTextColor = MilkyWhiteText
                ),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
