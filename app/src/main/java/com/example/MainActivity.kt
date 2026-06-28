// app/src/main/java/com/example/MainActivity.kt
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.RetrofitClient
import com.example.data.database.AppDatabase
import com.example.data.repository.ApiKeyRepository
import com.example.data.repository.ChatRepository
import com.example.ui.components.DrawerDivider
import com.example.ui.components.DrawerHeader
import com.example.ui.components.SettingsDrawerItem
import com.example.ui.navigation.Workspace
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val chatRepository by lazy { ChatRepository(database.chatDao()) }
    private val apiKeyRepository by lazy {
        ApiKeyRepository(database.apiKeyDao(), RetrofitClient.service)
    }

    private val chatViewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(chatRepository)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(apiKeyRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(
                    chatViewModel = chatViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainApp(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentWorkspace by remember { mutableStateOf(Workspace.CHAT) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val apiKeys by settingsViewModel.apiKeys.collectAsStateWithLifecycle()
    val activeKeyCount = apiKeys.count { it.value.isEnabled && it.value.apiKey.isNotBlank() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = ShadowBlack,
                drawerTonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DrawerHeader(
                        isGenerating = isGenerating,
                        activeKeyCount = activeKeyCount
                    )

                    DrawerDivider(label = "Workspaces")

                    Spacer(modifier = Modifier.height(4.dp))

                    Workspace.entries.forEach { workspace ->
                        SettingsDrawerItem(
                            workspace = workspace,
                            isSelected = currentWorkspace == workspace,
                            isPremiumUnlocked = true,
                            onClick = {
                                currentWorkspace = workspace
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    DrawerDivider()

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Troc Agent v1.0",
                        fontSize = 10.sp,
                        color = MutedGrayDark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                when (currentWorkspace) {
                    Workspace.CHAT -> {}
                    Workspace.SETTINGS -> {}
                    else -> {
                        TopAppBar(
                            title = {
                                Text(
                                    text = currentWorkspace.label,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = currentWorkspace.icon,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentWorkspace,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(animationSpec = tween(300)) { it / 4 } togetherWith
                                fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(animationSpec = tween(300)) { -it / 4 }
                    },
                    label = "workspace_transition"
                ) { workspace ->
                    when (workspace) {
                        Workspace.CHAT -> {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        Workspace.SETTINGS -> {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { scope.launch { drawerState.open() } }
                            )
                        }
                        else -> {
                            ComingSoonScreen(
                                workspace = workspace,
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(
    workspace: Workspace,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = workspace.icon,
                contentDescription = null,
                tint = BalanceGold.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = workspace.label,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MilkyWhiteText
            )
            Text(
                text = "Coming in the next phase",
                fontSize = 14.sp,
                color = MutedGrayDark
            )
        }
    }
}
