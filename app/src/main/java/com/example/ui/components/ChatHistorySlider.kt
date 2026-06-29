// app/src/main/java/com/example/ui/components/ChatHistorySlider.kt
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.continuity.ChatContinuityManager
import com.example.data.continuity.ContextWindow
import com.example.data.continuity.SessionSnapshot
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistorySlider(
    sessions: List<ChatSession>,
    activeSessionId: Long?,
    activeMessages: List<ChatMessage>,
    onSessionSelect: (Long) -> Unit,
    onContinueSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSnapshot by remember { mutableStateOf<SessionSnapshot?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    var showRenameDialog by remember { mutableStateOf<SessionSnapshot?>(null) }
    var renameText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Build snapshots from sessions
    val sessionSnapshots = remember(sessions) {
        sessions.map { session ->
            ChatContinuityManager.createSessionSnapshot(
                sessionId = session.id,
                title = session.displayTitle,
                messages = if (session.id == activeSessionId) activeMessages else emptyList(),
                contextTokens = if (session.id == activeSessionId)
                    ChatContinuityManager.estimateTokenCount(activeMessages) else 0
            )
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Trigger Button — Context window indicator
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    Surface(
        onClick = { showBottomSheet = true },
        modifier = modifier.testTag("history_slider_button"),
        shape = RoundedCornerShape(12.dp),
        color = BalanceGold.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.History, "Chat History", tint = BalanceGold, modifier = Modifier.size(18.dp))

            Column {
                Text(
                    "Chat History",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MilkyWhiteText
                )
                if (activeSessionId != null && activeMessages.isNotEmpty()) {
                    val tokens = ChatContinuityManager.estimateTokenCount(activeMessages)
                    val percentage = ChatContinuityManager.getContextPercentage(tokens)
                    Text(
                        "${activeMessages.size} msgs • ${if (tokens >= 1000) "${tokens/1000}K" else "$tokens"} / 1M tokens",
                        fontSize = 10.sp,
                        color = when {
                            percentage > 0.9f -> ErrorRed
                            percentage > 0.7f -> WarningAmber
                            else -> SuccessGreen
                        },
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        "${sessionSnapshots.size} sessions saved",
                        fontSize = 10.sp,
                        color = MutedGrayDark
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Context bar
            if (activeSessionId != null && activeMessages.isNotEmpty()) {
                val tokens = ChatContinuityManager.estimateTokenCount(activeMessages)
                val percentage = ChatContinuityManager.getContextPercentage(tokens)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when {
                            percentage > 0.9f -> ErrorRed
                            percentage > 0.7f -> WarningAmber
                            else -> BalanceGold
                        },
                        trackColor = BorderGrayDark
                    )
                    Text(
                        "${(percentage * 100).toInt()}% full",
                        fontSize = 8.sp,
                        color = MutedGrayDark
                    )
                }
            }

            Icon(Icons.Rounded.KeyboardArrowUp, null, tint = MutedGrayDark, modifier = Modifier.size(16.dp))
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Bottom Sheet — Full History Slider
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = ShadowBlack,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BalanceGold.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Chat History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MilkyWhiteText
                    )
                    Text(
                        "Swipe down to close • Tap a session to continue",
                        fontSize = 11.sp,
                        color = MutedGrayDark
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // New chat button
                Button(
                    onClick = {
                        onNewChat()
                        showBottomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BalanceGold, contentColor = ShadowBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(Modifier.height(12.dp))

                if (sessionSnapshots.isEmpty()) {
                    EmptyHistoryPlaceholder()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Active session first
                        val activeSnapshot = sessionSnapshots.find { it.sessionId == activeSessionId }
                        if (activeSnapshot != null && activeMessages.isNotEmpty()) {
                            item(key = "active_context") {
                                ActiveContextCard(
                                    snapshot = activeSnapshot,
                                    messages = activeMessages,
                                    onContinue = { showBottomSheet = false }
                                )
                            }

                            item(key = "divider_active") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGrayDark)
                                    Text(
                                        "Saved Sessions",
                                        fontSize = 10.sp,
                                        color = MutedGrayDark,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGrayDark)
                                }
                            }
                        }

                        items(
                            sessionSnapshots.filter { it.sessionId != activeSessionId || activeMessages.isEmpty() },
                            key = { it.sessionId }
                        ) { snapshot ->
                            HistorySessionCard(
                                snapshot = snapshot,
                                isActive = snapshot.sessionId == activeSessionId,
                                onClick = {
                                    selectedSnapshot = snapshot
                                    onContinueSession(snapshot.sessionId)
                                    showBottomSheet = false
                                },
                                onDelete = { showDeleteConfirm = snapshot.sessionId },
                                onRename = {
                                    renameText = snapshot.title
                                    showRenameDialog = snapshot
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Delete Confirmation Dialog
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    showDeleteConfirm?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            icon = { Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Delete Session?") },
            text = { Text("This conversation will be permanently removed.", fontSize = 13.sp, color = MutedGrayDark) },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(sessionId); showDeleteConfirm = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel", color = MutedGrayDark) } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Rename Dialog
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    showRenameDialog?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlack, unfocusedContainerColor = ShadowBlack,
                        cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onRenameSession(snapshot.sessionId, renameText); showRenameDialog = null }) {
                    Text("Save", color = BalanceGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Cancel", color = MutedGrayDark) } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ActiveContextCard(
    snapshot: SessionSnapshot,
    messages: List<ChatMessage>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ChatContinuityManager.estimateTokenCount(messages)
    val percentage = ChatContinuityManager.getContextPercentage(tokens)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BalanceGold.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, BalanceGold.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Chat, null, tint = BalanceGold, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Active Session", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BalanceGold)
                    Text(snapshot.title, fontSize = 12.sp, color = MilkyWhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BalanceGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${snapshot.messageCount} msgs",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = BalanceGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Context window visual
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Context Window", fontSize = 10.sp, color = MutedGrayDark)
                    Text(
                        when {
                            percentage > 0.9f -> "Near limit"
                            percentage > 0.7f -> "Filling up"
                            else -> "Healthy"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = when { percentage > 0.9f -> ErrorRed; percentage > 0.7f -> WarningAmber; else -> SuccessGreen }
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = when { percentage > 0.9f -> ErrorRed; percentage > 0.7f -> WarningAmber; else -> BalanceGold },
                    trackColor = BorderGrayDark
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${if (tokens >= 1000) "${tokens/1000}K" else "$tokens"} tokens", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MutedGrayDark)
                    Text("1M max", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MutedGrayDark)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Last message preview
            if (snapshot.lastMessagePreview.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = ShadowBlack)
                ) {
                    Text(
                        snapshot.lastMessagePreview,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 11.sp,
                        color = MutedGrayDark,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySessionCard(
    snapshot: SessionSnapshot,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canContinue = ChatContinuityManager.canContinueSession(snapshot)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) BalanceGold.copy(alpha = 0.06f) else ShadowBlackCard
        ),
        border = if (isActive) BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) BalanceGold.copy(alpha = 0.15f) else MutedGrayDark.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isActive) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                    null,
                    tint = if (isActive) BalanceGold else MutedGrayDark,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    snapshot.title,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isActive) BalanceGold else MilkyWhiteText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${snapshot.messageCount} msgs", fontSize = 10.sp, color = MutedGrayDark)
                    Text("•", fontSize = 10.sp, color = MutedGrayDark)
                    Text(snapshot.formattedLastActive, fontSize = 10.sp, color = MutedGrayDark)
                }
                if (snapshot.contextWindow != null) {
                    Text(
                        snapshot.contextWindow.summary,
                        fontSize = 9.sp,
                        color = MutedGrayDark.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Edit, "Rename", tint = MutedGrayDark, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Delete, "Delete", tint = MutedGrayDark.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Rounded.Inbox,
            null,
            tint = MutedGrayDark.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp)
        )
        Text(
            "No Saved Sessions",
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = MutedGrayDark.copy(alpha = 0.5f)
        )
        Text(
            "Your chat history will appear here.\nSessions auto-save with full 1M context.",
            fontSize = 12.sp,
            color = MutedGrayDark.copy(alpha = 0.3f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
