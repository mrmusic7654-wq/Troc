package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChatMessage
import com.example.ui.components.CodeBlock
import com.example.ui.components.YinYangLogo
import com.example.ui.components.VoiceInputOverlay
import com.example.ui.theme.BalanceGold
import com.example.ui.theme.MilkyWhite
import com.example.ui.theme.ShadowBlack
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isDeepThinkingEnabled by viewModel.isDeepThinkingEnabled.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }
    var renameSessionTarget by remember { mutableStateOf<Long?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Scroll to bottom when a new message arrives
    LaunchedEffect(activeMessages.size, isGenerating) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerTonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        YinYangLogo(size = 36.dp, isSpinning = isGenerating)
                        Column {
                            Text(
                                text = "Troc Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Conversational Harmony",
                                style = MaterialTheme.typography.labelSmall,
                                color = BalanceGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // New Chat Action
                    Button(
                        onClick = {
                            viewModel.startNewChat()
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("drawer_new_chat_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "New Session",
                                modifier = Modifier.size(20.dp)
                            )
                            Text("New Balance Chat", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "History Logs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    // Sessions List
                    Box(modifier = Modifier.weight(1f)) {
                        if (sessions.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "Empty History",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Perfect void.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sessions) { session ->
                                    val isSelected = session.id == activeSessionId
                                    val isEditing = renameSessionTarget == session.id

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectSession(session.id)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .testTag("session_item_${session.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent
                                            }
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (isSelected) {
                                            BorderStroke(1.dp, BalanceGold)
                                        } else {
                                            null
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                                                    contentDescription = "Session Icon",
                                                    tint = if (isSelected) BalanceGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )

                                                if (isEditing) {
                                                    TextField(
                                                        value = renameInput,
                                                        onValueChange = { renameInput = it },
                                                        singleLine = true,
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            focusedIndicatorColor = BalanceGold
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("rename_input_field"),
                                                        trailingIcon = {
                                                            IconButton(
                                                                onClick = {
                                                                    if (renameInput.isNotBlank()) {
                                                                        viewModel.renameSession(session.id, renameInput)
                                                                    }
                                                                    renameSessionTarget = null
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Check,
                                                                    contentDescription = "Save title",
                                                                    tint = Color.Green,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    Text(
                                                        text = session.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (!isEditing) {
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            renameSessionTarget = session.id
                                                            renameInput = session.title
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Edit,
                                                            contentDescription = "Rename Session",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteSession(session.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = "Delete Session",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clear All History Button
                    if (sessions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearAllHistory() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Clear All Conversations",
                                fontSize = 13.sp,
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) {
        Scaffold(
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
                                    // Clicking title spins the logo! Fun touch!
                                    viewModel.sendMessage("☯️ Balance my day with a beautiful Yin-Yang thought!")
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Troc",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Menu logs",
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
                // Main Content Body
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (activeMessages.isEmpty()) {
                            // Empty suggestion state (DeepSeek dashboard!)
                            DashboardIntro(
                                onSuggestionClick = { suggestion ->
                                    inputPrompt = suggestion
                                    viewModel.sendMessage(suggestion)
                                    inputPrompt = ""
                                },
                                isGenerating = isGenerating
                            )
                        } else {
                            // Active Message Chat list
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(activeMessages) { message ->
                                    MessageBubble(
                                        message = message,
                                        isLightForce = MaterialTheme.colorScheme.background == MilkyWhite
                                    )
                                }

                                if (isGenerating) {
                                    item {
                                        AssistantThinkingBubble()
                                    }
                                }
                            }
                        }
                    }

                    // Display errors if any
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1CEF5350)),
                            border = BorderStroke(1.dp, Color(0xFFEF5350))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFEF5350),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Bottom Chat Controls Panel
                    BottomChatControls(
                        prompt = inputPrompt,
                        onPromptChange = { inputPrompt = it },
                        isDeepThinkingEnabled = isDeepThinkingEnabled,
                        onDeepThinkingToggle = { viewModel.isDeepThinkingEnabled.value = !isDeepThinkingEnabled },
                        onSendClick = {
                            if (inputPrompt.isNotBlank()) {
                                viewModel.sendMessage(inputPrompt)
                                inputPrompt = ""
                            }
                        },
                        onMicClick = { viewModel.isListening.value = true },
                        isGenerating = isGenerating
                    )
                }

                // Voice Dictation Overlay
                if (isListening) {
                    VoiceInputOverlay(
                        onDismiss = { viewModel.isListening.value = false },
                        onResult = { text ->
                            viewModel.isListening.value = false
                            if (text.isNotBlank()) {
                                inputPrompt = text
                                viewModel.sendMessage(text)
                                inputPrompt = ""
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// --- Subviews / Subcomposables ---

@Composable
fun DashboardIntro(
    onSuggestionClick: (String) -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large animated logo
        var logoPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (logoPressed) 0.85f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "logo_scale"
        )

        YinYangLogo(
            size = 110.dp,
            isSpinning = isGenerating || logoPressed,
            modifier = Modifier
                .scale(scale)
                .clickable {
                    logoPressed = true
                }
                .testTag("dashboard_yinyang_logo")
        )

        LaunchedEffect(logoPressed) {
            if (logoPressed) {
                kotlinx.coroutines.delay(1000)
                logoPressed = false
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hello, I'm Troc.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Deep thinking. Fast reasoning. The Yin to your information Yang.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Floating Suggestion Grid
        Text(
            text = "Quick Alignments",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = BalanceGold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            val suggestions = listOf(
                "🔍 Search web: Balances of light and shadow",
                "🪄 Brainstorm: A perfectly structured daily flow",
                "💻 Write code: Yin-Yang algorithm in Kotlin",
                "☯️ Philosophy: Explain core dual forces of Troc"
            )

            suggestions.forEach { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .testTag("suggestion_chip_${suggestion.take(15)}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Suggest",
                            tint = BalanceGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = suggestion,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isLightForce: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Troc mini avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BalanceGold)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                YinYangLogo(size = 28.dp, outlineColor = Color.Transparent)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message Bubble body
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // User vs Model Styling: Creates the Yin & Yang balance
            val containerColor = if (isUser) {
                // Opposite force styling: dark card in light theme, light card in dark theme!
                if (isLightForce) ShadowBlack else MilkyWhite
            } else {
                MaterialTheme.colorScheme.surface
            }

            val contentColor = if (isUser) {
                if (isLightForce) MilkyWhite else ShadowBlack
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)) else null,
                modifier = Modifier.testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 1. Deep Thinking Reasoning Accordion (DeepSeek iconic thought process)
                    if (!isUser && !message.reasoning.isNullOrBlank()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        val formattedDuration = message.durationMs?.let {
                            String.format(Locale.getDefault(), "%.1fs", it / 1000f)
                        } ?: "thought"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { isExpanded = !isExpanded }
                                .padding(10.dp)
                                .testTag("reasoning_accordion_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = "Reasoning Process",
                                        tint = BalanceGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Thought Process ($formattedDuration)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BalanceGold
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = "Expand reasoning",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = message.reasoning,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            lineHeight = 18.sp,
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 2. Main Response Text - with isolated code blocks
                    RenderMessageText(text = message.text, contentColor = contentColor)
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar representation (Yin and Yang contrasting dot)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isLightForce) ShadowBlack else MilkyWhite),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isLightForce) MilkyWhite else ShadowBlack)
                )
            }
        }
    }
}

@Composable
fun RenderMessageText(
    text: String,
    contentColor: Color
) {
    val parts = text.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block segment
                val lines = part.trim().split("\n")
                val language = if (lines.isNotEmpty() && lines[0].length < 15 && !lines[0].contains(" ")) {
                    lines[0]
                } else {
                    "code"
                }
                val codeContent = if (language == "code") part else lines.drop(1).joinToString("\n")
                CodeBlock(code = codeContent.trim(), language = language)
            } else {
                // Plain text segment
                if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantThinkingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Troc avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BalanceGold)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            YinYangLogo(size = 28.dp, outlineColor = Color.Transparent)
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Thinking card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YinYangLogo(size = 20.dp, isSpinning = true)
                Text(
                    text = "Troc is balancing thought forces...",
                    fontSize = 13.sp,
                    color = BalanceGold,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomChatControls(
    prompt: String,
    onPromptChange: (String) -> Unit,
    isDeepThinkingEnabled: Boolean,
    onDeepThinkingToggle: () -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Toggles bar (Deep Thinking toggle button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Deep Thinking Mode Switch
                Card(
                    modifier = Modifier
                        .clickable { onDeepThinkingToggle() }
                        .testTag("deep_thinking_toggle_pill"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDeepThinkingEnabled) {
                            BalanceGold.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDeepThinkingEnabled) BalanceGold else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = "Reasoning Mode",
                            tint = if (isDeepThinkingEnabled) BalanceGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Deep Thinking (Reasoning)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeepThinkingEnabled) BalanceGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Balance guide status
                Text(
                    text = if (isDeepThinkingEnabled) "☯️ HIGH REASONING ACTIVE" else "☯️ BALANCED CHAT ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = if (isDeepThinkingEnabled) BalanceGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Input field and action keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speech Input Button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .testTag("microphone_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Input",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Main Text Input
                TextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = {
                        Text(
                            text = "Message Troc...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .testTag("chat_input_text_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                // Send Button
                IconButton(
                    onClick = onSendClick,
                    enabled = prompt.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (prompt.isNotBlank() && !isGenerating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (prompt.isNotBlank() && !isGenerating) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
    }
}
