// app/src/main/java/com/example/ui/components/BrowserWorkspace.kt
package com.example.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWorkspace(
    initialUrl: String = "https://www.google.com",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUrl by remember { mutableStateOf(initialUrl) }
    var urlInput by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var pageTitle by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var showAIAssistant by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isAIProcessing by remember { mutableStateOf(false) }

    val bookmarks = remember {
        mutableStateListOf(
            "https://www.google.com" to "Google",
            "https://github.com" to "GitHub",
            "https://aistudio.google.com" to "AI Studio",
            "https://huggingface.co" to "Hugging Face",
            "https://replicate.com" to "Replicate",
            "https://www.android.com" to "Android"
        )
    }

    val tabs = remember {
        mutableStateListOf(
            BrowserTab("https://www.google.com", "Google", false),
            BrowserTab("https://github.com", "GitHub", false)
        )
    }
    var activeTabIndex by remember { mutableStateOf(0) }

    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // URL Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ShadowBlackCard,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Navigation
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MutedGrayDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBackIos,
                            contentDescription = "Previous page",
                            tint = if (canGoBack) MilkyWhiteText else MutedGrayDark.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { webView?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowForwardIos,
                            contentDescription = "Next page",
                            tint = if (canGoForward) MilkyWhiteText else MutedGrayDark.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { webView?.reload() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Reload",
                            tint = MilkyWhiteText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // URL Input
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Search or enter URL...",
                                fontSize = 12.sp,
                                color = MutedGrayDark.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = BalanceGold,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BalanceGold.copy(alpha = 0.3f),
                            unfocusedBorderColor = BorderGrayDark,
                            focusedContainerColor = ShadowBlack,
                            unfocusedContainerColor = ShadowBlack,
                            cursorColor = BalanceGold,
                            focusedTextColor = MilkyWhiteText,
                            unfocusedTextColor = MilkyWhiteText
                        ),
                        textStyle = TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(autoCorrect = false)
                    )

                    // Go button
                    IconButton(
                        onClick = {
                            val url = if (urlInput.contains(".") && !urlInput.contains(" ")) {
                                if (!urlInput.startsWith("http")) "https://$urlInput" else urlInput
                            } else {
                                "https://www.google.com/search?q=${urlInput.replace(" ", "+")}"
                            }
                            webView?.loadUrl(url)
                            currentUrl = url
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = "Go",
                            tint = BalanceGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = { showBookmarks = !showBookmarks },
                        label = { Text("Bookmarks", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (showBookmarks) BalanceGoldSubtle else Color.Transparent,
                            labelColor = if (showBookmarks) BalanceGold else MutedGrayDark
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = if (showBookmarks) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark,
                            enabled = true
                        )
                    )

                    AssistChip(
                        onClick = { showTabs = !showTabs },
                        label = { Text("Tabs (${tabs.size})", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Tab,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (showTabs) BalanceGoldSubtle else Color.Transparent,
                            labelColor = if (showTabs) BalanceGold else MutedGrayDark
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = if (showTabs) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark,
                            enabled = true
                        )
                    )

                    AssistChip(
                        onClick = { isDesktopMode = !isDesktopMode },
                        label = { Text("Desktop", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                if (isDesktopMode) Icons.Rounded.DesktopWindows else Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDesktopMode) BalanceGoldSubtle else Color.Transparent,
                            labelColor = if (isDesktopMode) BalanceGold else MutedGrayDark
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = if (isDesktopMode) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark,
                            enabled = true
                        )
                    )

                    AssistChip(
                        onClick = { showAIAssistant = !showAIAssistant },
                        label = { Text("AI", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (showAIAssistant) BalanceGoldSubtle else Color.Transparent,
                            labelColor = if (showAIAssistant) BalanceGold else MutedGrayDark
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = if (showAIAssistant) BalanceGold.copy(alpha = 0.3f) else BorderGrayDark,
                            enabled = true
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Add bookmark
                    if (currentUrl.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val exists = bookmarks.any { it.first == currentUrl }
                                if (!exists) {
                                    bookmarks.add(currentUrl to (pageTitle.ifBlank { currentUrl }))
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.BookmarkAdd,
                                contentDescription = "Add bookmark",
                                tint = MutedGrayDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Loading Progress
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = BalanceGold,
                        trackColor = BorderGrayDark
                    )
                }
            }
        }

        // Bookmarks Panel
        AnimatedVisibility(
            visible = showBookmarks,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ShadowBlackCard),
                border = BorderStroke(0.5.dp, BorderGrayDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Bookmarks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MilkyWhiteText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    bookmarks.forEach { (url, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    webView?.loadUrl(url)
                                    urlInput = url
                                    currentUrl = url
                                    showBookmarks = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = BalanceGold.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    title,
                                    fontSize = 12.sp,
                                    color = MilkyWhiteText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    url,
                                    fontSize = 10.sp,
                                    color = MutedGrayDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { bookmarks.removeAll { it.first == url } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    tint = MutedGrayDark,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tabs Panel
        AnimatedVisibility(
            visible = showTabs,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ShadowBlackCard),
                border = BorderStroke(0.5.dp, BorderGrayDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tabs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MilkyWhiteText
                        )
                        TextButton(onClick = {
                            tabs.add(BrowserTab("https://www.google.com", "New Tab", true))
                            activeTabIndex = tabs.lastIndex
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Tab", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    tabs.forEachIndexed { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeTabIndex = index
                                    webView?.loadUrl(tab.url)
                                    showTabs = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = index == activeTabIndex,
                                onClick = null,
                                modifier = Modifier.size(16.dp),
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BalanceGold,
                                    unselectedColor = MutedGrayDark
                                )
                            )
                            Text(
                                tab.title,
                                fontSize = 12.sp,
                                color = if (index == activeTabIndex) BalanceGold else MilkyWhiteText,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (tabs.size > 1) {
                                IconButton(
                                    onClick = { tabs.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close tab",
                                        tint = MutedGrayDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Content
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = if (isDesktopMode) {
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        } else {
                            settings.userAgentString
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress.toFloat()
                            }
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                pageTitle = title ?: ""
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    urlInput = it
                                }
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }
                        }

                        webView = this
                        loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // AI Assistant Panel
            if (showAIAssistant) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .widthIn(max = 300.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShadowBlackCard),
                    border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "AI Assistant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BalanceGold
                            )
                            IconButton(
                                onClick = { showAIAssistant = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = MutedGrayDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (aiResponse.isNotBlank()) {
                            Text(
                                aiResponse,
                                fontSize = 11.sp,
                                color = MilkyWhiteText,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Ask about this page...",
                                    fontSize = 11.sp,
                                    color = MutedGrayDark
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BalanceGold,
                                unfocusedBorderColor = BorderGrayDark,
                                focusedContainerColor = ShadowBlack,
                                unfocusedContainerColor = ShadowBlack,
                                cursorColor = BalanceGold
                            ),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isAIProcessing = true
                                    delay(1500)
                                    aiResponse = "Based on this page content, I can help you analyze the information. The page appears to be about: ${pageTitle.ifBlank { "various topics" }}. You can ask me to summarize, extract data, or explain concepts found on this page."
                                    isAIProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = aiPrompt.isNotBlank() && !isAIProcessing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BalanceGold,
                                contentColor = ShadowBlack
                            )
                        ) {
                            if (isAIProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = ShadowBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isAIProcessing) "Analyzing..." else "Ask AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BrowserTab(
    val url: String,
    val title: String,
    val isActive: Boolean
)
