// app/src/main/java/com/example/ui/components/SessionDrawer.kt
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChatSession
import com.example.ui.navigation.Workspace
import com.example.ui.theme.*

@Composable
fun SessionDrawer(
    sessions: List<ChatSession>,
    activeSessionId: Long?,
    isGenerating: Boolean,
    activeKeyCount: Int,
    currentWorkspace: Workspace,
    onSessionSelect: (Long) -> Unit,
    onSessionRename: (Long, String) -> Unit,
    onSessionDelete: (Long) -> Unit,
    onNewChat: () -> Unit,
    onClearAll: () -> Unit,
    onWorkspaceSelect: (Workspace) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }
    var sortMode by remember { mutableStateOf(SortMode.UPDATED) }

    val filteredSessions = remember(sessions, searchQuery, filterMode) {
        sessions
            .filter { session ->
                when (filterMode) {
                    FilterMode.ALL -> true
                    FilterMode.PINNED -> session.isPinned
                    FilterMode.ARCHIVED -> session.isArchived
                    FilterMode.ACTIVE -> session.isActive
                }
            }
            .filter { session ->
                searchQuery.isBlank() ||
                        session.title.contains(searchQuery, ignoreCase = true) ||
                        session.lastMessagePreview.contains(searchQuery, ignoreCase = true) ||
                        session.tagList.any { it.contains(searchQuery, ignoreCase = true) }
            }
            .sortedWith(
                when (sortMode) {
                    SortMode.UPDATED -> ChatSession.SORT_BY_UPDATED
                    SortMode.CREATED -> ChatSession.SORT_BY_CREATED
                    SortMode.MESSAGES -> ChatSession.SORT_BY_MESSAGE_COUNT
                    SortMode.TITLE -> ChatSession.SORT_BY_TITLE
                }
            )
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = ShadowBlack,
        drawerTonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            DrawerHeader(
                isGenerating = isGenerating,
                activeKeyCount = activeKeyCount,
                onCloseDrawer = onCloseDrawer
            )

            // Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onNewChat()
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("drawer_new_chat_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BalanceGold,
                        contentColor = ShadowBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        filterMode = when (filterMode) {
                            FilterMode.ALL -> FilterMode.PINNED
                            FilterMode.PINNED -> FilterMode.ACTIVE
                            FilterMode.ACTIVE -> FilterMode.ARCHIVED
                            FilterMode.ARCHIVED -> FilterMode.ALL
                        }
                    },
                    modifier = Modifier
                        .weight(0.6f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGrayDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MutedGrayDark
                    )
                ) {
                    Icon(
                        imageVector = when (filterMode) {
                            FilterMode.ALL -> Icons.Rounded.FilterList
                            FilterMode.PINNED -> Icons.Rounded.PushPin
                            FilterMode.ACTIVE -> Icons.Rounded.Circle
                            FilterMode.ARCHIVED -> Icons.Rounded.Archive
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = when (filterMode) {
                            FilterMode.ALL -> "All"
                            FilterMode.PINNED -> "Pinned"
                            FilterMode.ACTIVE -> "Active"
                            FilterMode.ARCHIVED -> "Archived"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Search
            if (sessions.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    placeholder = {
                        Text(
                            "Search conversations...",
                            fontSize = 12.sp,
                            color = MutedGrayDark.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MutedGrayDark,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MutedGrayDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BalanceGold.copy(alpha = 0.5f),
                        unfocusedBorderColor = BorderGrayDark,
                        focusedContainerColor = ShadowBlackCard,
                        unfocusedContainerColor = ShadowBlackCard,
                        cursorColor = BalanceGold,
                        focusedTextColor = MilkyWhiteText,
                        unfocusedTextColor = MilkyWhiteText
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // Sort toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        fontSize = 10.sp,
                        color = MutedGrayDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            sortMode = when (sortMode) {
                                SortMode.UPDATED -> SortMode.TITLE
                                SortMode.TITLE -> SortMode.MESSAGES
                                SortMode.MESSAGES -> SortMode.CREATED
                                SortMode.CREATED -> SortMode.UPDATED
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = when (sortMode) {
                                SortMode.UPDATED -> "Recent"
                                SortMode.TITLE -> "Name"
                                SortMode.MESSAGES -> "Activity"
                                SortMode.CREATED -> "Created"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BalanceGold
                        )
                        Icon(
                            Icons.Rounded.SwapVert,
                            contentDescription = null,
                            tint = BalanceGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                DrawerDivider()

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Sessions List
            Box(modifier = Modifier.weight(1f)) {
                if (filteredSessions.isEmpty() && sessions.isNotEmpty()) {
                    EmptySearchResult(
                        query = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (sessions.isEmpty()) {
                    EmptySessionsPlaceholder(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = filteredSessions,
                            key = { it.id }
                        ) { session ->
                            SessionDrawerItem(
                                session = session,
                                isSelected = session.id == activeSessionId,
                                onSelect = {
                                    onSessionSelect(session.id)
                                    onCloseDrawer()
                                },
                                onRename = { newTitle ->
                                    onSessionRename(session.id, newTitle)
                                },
                                onDelete = { onSessionDelete(session.id) },
                                onPin = {
                                    onSessionRename(
                                        session.id,
                                        session.title
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Workspace Section
            DrawerDivider(label = "Workspaces")

            Spacer(modifier = Modifier.height(4.dp))

            Workspace.entries.forEach { workspace ->
                WorkspaceDrawerItem(
                    workspace = workspace,
                    isSelected = currentWorkspace == workspace,
                    isPremiumUnlocked = true,
                    onClick = {
                        onWorkspaceSelect(workspace)
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            DrawerDivider()

            // Clear All Button
            if (sessions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = ErrorRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Clear All Conversations",
                        fontSize = 12.sp,
                        color = ErrorRed.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Footer
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Troc Agent v1.0",
                    fontSize = 10.sp,
                    color = MutedGrayDark.copy(alpha = 0.5f)
                )
                Text(
                    text = "${sessions.size} sessions",
                    fontSize = 10.sp,
                    color = MutedGrayDark.copy(alpha = 0.5f)
                )
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            textContentColor = MutedGrayDark,
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Clear All Conversations?")
            },
            text = {
                Text("This will permanently delete all ${sessions.size} chat sessions and their messages. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                        onCloseDrawer()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ErrorRed
                    )
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MutedGrayDark
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SessionDrawerItem(
    session: ChatSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember(session.title) { mutableStateOf(session.title) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isRenaming) {
        if (isRenaming) {
            focusRequester.requestFocus()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> BalanceGoldSubtle
                session.isPinned -> ShadowBlackHover
                else -> Color.Transparent
            }
        ),
        border = when {
            isSelected -> BorderStroke(1.dp, BalanceGold.copy(alpha = 0.3f))
            session.isPinned && !isSelected -> BorderStroke(
                0.5.dp,
                BalanceGold.copy(alpha = 0.1f)
            )
            else -> null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (isRenaming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused && isRenaming) {
                                    if (renameText.isNotBlank()) {
                                        onRename(renameText)
                                    }
                                    isRenaming = false
                                }
                            },
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = MilkyWhiteText,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(BalanceGold),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (renameText.isNotBlank()) {
                                onRename(renameText)
                            }
                            isRenaming = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Save",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            renameText = session.title
                            isRenaming = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            tint = MutedGrayDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pin indicator
                    if (session.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            tint = BalanceGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Color label
                    if (session.colorLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (session.colorLabel) {
                                        ChatSession.COLOR_GOLD -> BalanceGold
                                        ChatSession.COLOR_MILKY -> MilkyWhite
                                        ChatSession.COLOR_ROSE -> Color(0xFFFF8A80)
                                        ChatSession.COLOR_OCEAN -> Color(0xFF82B1FF)
                                        ChatSession.COLOR_FOREST -> Color(0xFFB9F6CA)
                                        else -> MutedGrayDark
                                    }
                                )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.displayTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) MilkyWhiteText else MilkyWhiteText.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (session.hasLastMessage) {
                            Text(
                                text = session.lastMessageSnippet,
                                fontSize = 11.sp,
                                color = MutedGrayDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.formattedDate,
                                fontSize = 10.sp,
                                color = MutedGrayDark.copy(alpha = 0.6f)
                            )
                            if (session.messageCount > 0) {
                                Text(
                                    text = "${session.messageCount} msgs",
                                    fontSize = 10.sp,
                                    color = MutedGrayDark.copy(alpha = 0.4f)
                                )
                            }
                            if (session.isArchived) {
                                Icon(
                                    Icons.Rounded.Archive,
                                    contentDescription = "Archived",
                                    tint = MutedGrayDark.copy(alpha = 0.4f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    // More options
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = MutedGrayDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = ShadowBlackCard
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", fontSize = 13.sp) },
                                onClick = {
                                    renameText = session.title
                                    isRenaming = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (session.isPinned) "Unpin" else "Pin",
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    onPin()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (session.isPinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (session.isArchived) "Unarchive" else "Archive",
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    onPin()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (session.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            HorizontalDivider(color = BorderGrayDark)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        fontSize = 13.sp,
                                        color = ErrorRed
                                    )
                                },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceDrawerItem(
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
        label = "workspaceScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BalanceGoldSubtle else Color.Transparent
        ),
        border = if (isSelected) BorderStroke(1.dp, BalanceGold.copy(alpha = 0.3f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = workspace.icon,
                contentDescription = workspace.contentDescription,
                tint = when {
                    isSelected -> BalanceGold
                    isLocked -> MutedGrayDark.copy(alpha = 0.3f)
                    else -> MutedGrayDark
                },
                modifier = Modifier
                    .size(18.dp)
                    .alpha(if (isLocked) 0.3f else 1f)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workspace.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = when {
                        isSelected -> MilkyWhiteText
                        isLocked -> MutedGrayDark.copy(alpha = 0.3f)
                        else -> MilkyWhiteText.copy(alpha = 0.8f)
                    }
                )
                Text(
                    text = if (isLocked) "Premium Feature" else workspace.description,
                    fontSize = 10.sp,
                    color = if (isLocked) BalanceGold.copy(alpha = 0.4f) else MutedGrayDark
                )
            }

            if (isLocked) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = "Premium",
                    tint = BalanceGold.copy(alpha = 0.3f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySessionsPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Inbox,
            contentDescription = null,
            tint = MutedGrayDark.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Perfect Void",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MutedGrayDark.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Begin a new conversation\nto create balance",
            fontSize = 11.sp,
            color = MutedGrayDark.copy(alpha = 0.3f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MutedGrayDark.copy(alpha = 0.3f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No matches for \"$query\"",
            fontSize = 12.sp,
            color = MutedGrayDark.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

private enum class FilterMode {
    ALL, PINNED, ACTIVE, ARCHIVED
}

private enum class SortMode {
    UPDATED, CREATED, MESSAGES, TITLE
}
