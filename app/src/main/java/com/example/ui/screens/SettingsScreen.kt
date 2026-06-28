// app/src/main/java/com/example/ui/screens/SettingsScreen.kt
package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.settings.ApiKeyConfig
import com.example.data.settings.ApiService
import com.example.data.settings.ServiceCategory
import com.example.ui.theme.*
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SnackbarEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apiKeys by viewModel.apiKeys.collectAsStateWithLifecycle()
    val isValidating by viewModel.isValidating.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryServices by viewModel.categoryServices.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { event ->
            val message = when (event) {
                is SnackbarEvent.Success -> event.message
                is SnackbarEvent.Error -> event.message
                is SnackbarEvent.Info -> event.message
            }
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = ShadowBlackCard, contentColor = MilkyWhiteText, shape = RoundedCornerShape(12.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = BalanceGold, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("API Keys & Configuration", style = MaterialTheme.typography.labelSmall, color = BalanceGold, fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    val activeKeysCount = apiKeys.count { it.value.isEnabled && it.value.apiKey.isNotBlank() }
                    if (activeKeysCount > 0) {
                        AssistChip(
                            onClick = { viewModel.validateAllKeys() },
                            label = { Text("$activeKeysCount active", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp), tint = BalanceGold) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = BalanceGold.copy(alpha = 0.1f), labelColor = BalanceGold),
                            border = BorderStroke(1.dp, BalanceGold.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search services...", color = MutedGrayDark, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedGrayDark, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MutedGrayDark, modifier = Modifier.size(18.dp))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark, focusedContainerColor = ShadowBlackCard, unfocusedContainerColor = ShadowBlackCard, cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText, unfocusedTextColor = MilkyWhiteText),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            ScrollableTabRow(
                selectedTabIndex = ServiceCategory.entries.indexOf(selectedCategory).coerceAtLeast(0),
                modifier = Modifier.padding(vertical = 4.dp),
                containerColor = Color.Transparent,
                contentColor = BalanceGold,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedCategory != null) {
                        val index = ServiceCategory.entries.indexOf(selectedCategory)
                        if (index < tabPositions.size) TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[index]), color = BalanceGold, height = 2.dp)
                    }
                }
            ) {
                Tab(selected = selectedCategory == null, onClick = { viewModel.selectCategory(null) }) {
                    Text("All", fontSize = 12.sp, fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal, color = if (selectedCategory == null) BalanceGold else MutedGrayDark)
                }
                ServiceCategory.entries.forEach { category ->
                    Tab(selected = selectedCategory == category, onClick = { viewModel.selectCategory(category) }) {
                        Text(category.label, fontSize = 12.sp, fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal, color = if (selectedCategory == category) BalanceGold else MutedGrayDark)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryServices.forEach { (category, services) ->
                    item(key = "header_${category.name}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(BalanceGold.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (category) {
                                        ServiceCategory.AI_MODEL -> Icons.Rounded.Psychology
                                        ServiceCategory.AI_PLATFORM -> Icons.Rounded.Cloud
                                        ServiceCategory.MESSAGING -> Icons.Rounded.Chat
                                        ServiceCategory.DEVELOPMENT -> Icons.Rounded.Code
                                        ServiceCategory.BACKEND -> Icons.Rounded.Storage
                                        ServiceCategory.MEDIA -> Icons.Rounded.Movie
                                    },
                                    contentDescription = null, tint = BalanceGold, modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(category.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MilkyWhiteText)
                                Text("${services.size} service${if (services.size != 1) "s" else ""}", fontSize = 11.sp, color = MutedGrayDark)
                            }
                        }
                    }
                    items(items = services, key = { it.name }) { service ->
                        val config = apiKeys[service.name]
                        val isValidatingThis = isValidating.contains(service.name)
                        val error = validationErrors[service.name]
                        ApiKeyCard(service, config, isValidatingThis, error, { viewModel.updateApiKey(service, it) }, { viewModel.toggleApiKey(service.name, it) }, { viewModel.validateApiKey(service) }, { viewModel.deleteApiKey(service.name) }, { viewModel.clearError(service.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyCard(
    service: ApiService, config: ApiKeyConfig?, isValidating: Boolean, validationError: String?,
    onKeyChange: (String) -> Unit, onToggle: (Boolean) -> Unit, onValidate: () -> Unit,
    onDelete: () -> Unit, onClearError: () -> Unit, modifier: Modifier = Modifier
) {
    var keyInput by remember(config?.apiKey) { mutableStateOf(config?.apiKey ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val isConfigured = config?.apiKey?.isNotBlank() == true
    val isEnabled = config?.isEnabled ?: true
    val isValid = config?.isValid == true
    val lastValidated = config?.lastValidated

    val statusColor = when {
        isValidating -> BalanceGold
        isValid -> SuccessGreen
        validationError != null -> ErrorRed
        isConfigured -> BalanceGold
        else -> MutedGrayDark
    }

    val pulseAnimation by rememberInfiniteTransition(label = "pulse").animateFloat(1f, 1.05f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse_scale")

    Card(
        modifier = modifier.fillMaxWidth().scale(if (isValidating) pulseAnimation else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isEnabled) ShadowBlackCard else ShadowBlackCard.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, when { isValid -> SuccessGreen.copy(alpha = 0.3f); validationError != null -> ErrorRed.copy(alpha = 0.3f); isConfigured -> BalanceGold.copy(alpha = 0.2f); else -> BorderGrayDark }),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        if (isValidating) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BalanceGold, strokeWidth = 2.dp)
                        else Icon(when { isValid -> Icons.Rounded.CheckCircle; isConfigured -> Icons.Rounded.VpnKey; else -> Icons.Rounded.Key }, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(service.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (isEnabled) MilkyWhiteText else MutedGrayDark)
                        if (isConfigured && !isValidating) {
                            Text(
                                when { isValid -> "Verified • ${formatTimestamp(lastValidated)}"; validationError != null -> validationError; else -> config?.maskedKey ?: "••••••••" },
                                fontSize = 11.sp, color = when { isValid -> SuccessGreen; validationError != null -> ErrorRed; else -> MutedGrayDark }, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isConfigured) IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.DeleteOutline, "Delete key", tint = MutedGrayDark, modifier = Modifier.size(16.dp)) }
                    Switch(isEnabled, { onToggle(it) }, Modifier.padding(start = 4.dp), colors = SwitchDefaults.colors(checkedThumbColor = ShadowBlack, checkedTrackColor = BalanceGold, uncheckedThumbColor = MilkyWhiteText, uncheckedTrackColor = BorderGrayDark))
                }
            }
            if (isEnabled) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    keyInput, { keyInput = it; onClearError() }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter ${service.displayName} API key...", color = MutedGrayDark.copy(alpha = 0.5f), fontSize = 13.sp) },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton({ isKeyVisible = !isKeyVisible }, Modifier.size(28.dp)) { Icon(if (isKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Toggle visibility", tint = MutedGrayDark, modifier = Modifier.size(16.dp)) }
                            if (keyInput.isNotBlank()) IconButton({ keyInput = ""; onClearError() }, Modifier.size(28.dp)) { Icon(Icons.Rounded.Clear, "Clear", tint = MutedGrayDark, modifier = Modifier.size(14.dp)) }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BalanceGold, unfocusedBorderColor = BorderGrayDark, focusedContainerColor = ShadowBlack.copy(alpha = 0.3f), unfocusedContainerColor = ShadowBlack.copy(alpha = 0.3f), cursorColor = BalanceGold, focusedTextColor = MilkyWhiteText, unfocusedTextColor = MilkyWhiteText),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(service.keyUrl))) }, Modifier.weight(0.4f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, BorderGrayDark), colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedGrayDark)) { Icon(Icons.Outlined.OpenInNew, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Get Key", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    Button({ if (keyInput.isNotBlank()) onKeyChange(keyInput); onValidate() }, Modifier.weight(0.6f), enabled = keyInput.isNotBlank() && !isValidating, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isValid) SuccessGreen else BalanceGold, contentColor = ShadowBlack, disabledContainerColor = BorderGrayDark, disabledContentColor = MutedGrayDark)) {
                        if (isValidating) CircularProgressIndicator(Modifier.size(14.dp), color = ShadowBlack, strokeWidth = 2.dp)
                        else Icon(if (isValid) Icons.Rounded.Check else Icons.Rounded.Bolt, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(when { isValidating -> "Validating..."; isValid -> "Verified"; isConfigured -> "Revalidate"; else -> "Save & Validate" }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (showDeleteDialog) AlertDialog({ showDeleteDialog = false }, containerColor = ShadowBlackCard, titleContentColor = MilkyWhiteText, textContentColor = MutedGrayDark, icon = { Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(28.dp)) }, title = { Text("Remove API Key?") }, text = { Text("This will delete your ${service.displayName} key. Connected features will stop working.") }, confirmButton = { TextButton({ onDelete(); keyInput = ""; showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Delete", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton({ showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = MutedGrayDark)) { Text("Cancel") } }, shape = RoundedCornerShape(16.dp))
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
