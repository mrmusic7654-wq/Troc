// app/src/main/java/com/example/ui/components/VoiceInputOverlay.kt
package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun VoiceInputOverlay(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    language: String = Locale.getDefault().language
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("Initializing voice capture...") }
    var partialText by remember { mutableStateOf("") }
    var errorState by remember { mutableStateOf<String?>(null) }
    var soundLevel by remember { mutableStateOf(0f) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(language) }
    var isPaused by remember { mutableStateOf(false) }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (!isGranted) {
            errorState = "Microphone permission denied. Voice input cannot function without it."
        }
    }

    // Pulse animation for the listening indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_waveform")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    val pulseScale3 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse3"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val startListening = {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@remember
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                isPaused = false
                transcriptionText = "I'm listening... speak your thoughts"
                partialText = ""
                errorState = null
            }

            override fun onBeginningOfSpeech() {
                transcriptionText = "Capturing your voice..."
                soundLevel = 0.5f
            }

            override fun onRmsChanged(rmsdB: Float) {
                soundLevel = (rmsdB + 10f).coerceIn(0f, 10f) / 10f
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                transcriptionText = "Processing your words..."
                isListening = false
                soundLevel = 0f
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check your microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Client connection issue. Try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied. Grant microphone access."
                    SpeechRecognizer.ERROR_NETWORK -> "Network unavailable. Check connection."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Try again."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Speak clearly and try again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Wait a moment."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Please try again."
                    SpeechRecognizer.ERROR_SERVER -> "Server error. Try again later."
                    else -> "Speech recognition interrupted: code $error"
                }
                errorState = message
                transcriptionText = message
                isListening = false
                isPaused = false
                soundLevel = 0f
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val resultText = matches[0]
                    transcriptionText = resultText
                    onResult(resultText)
                } else {
                    errorState = "No results captured. Please try again."
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    partialText = matches[0]
                    transcriptionText = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    val stopListening = {
        speechRecognizer?.stopListening()
        isListening = false
    }

    val togglePause = {
        if (isPaused) {
            speechRecognizer?.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
            isPaused = false
            isListening = true
        } else {
            speechRecognizer?.stopListening()
            isPaused = true
            isListening = false
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    // Full screen overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ShadowBlack.copy(alpha = 0.95f))
            .clickable(enabled = true, onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(enabled = false, onClick = {})
                .testTag("voice_overlay_card"),
            colors = CardDefaults.cardColors(
                containerColor = ShadowBlackCard
            ),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        YinYangLogo(
                            size = 24.dp,
                            isSpinning = isListening,
                            glowEnabled = true
                        )
                        Text(
                            text = "Vocal Harmony",
                            fontWeight = FontWeight.Bold,
                            color = MilkyWhiteText,
                            fontSize = 16.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close overlay",
                                tint = MutedGrayDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                if (!permissionGranted) {
                    PermissionRequestContent(
                        onGrantPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                } else {
                    // Animated waveform display
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        // Outer rings
                        if (isListening && !isPaused) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .scale(pulseScale3)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                BalanceGold.copy(alpha = glowAlpha * 0.3f),
                                                BalanceGold.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .scale(pulseScale2)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                BalanceGold.copy(alpha = glowAlpha * 0.5f),
                                                BalanceGold.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(pulseScale1)
                                    .clip(CircleShape)
                                    .background(BalanceGold.copy(alpha = glowAlpha * 0.3f))
                            )
                        }

                        // Sound level indicator ring
                        if (isListening && !isPaused) {
                            Canvas(
                                modifier = Modifier.size(90.dp)
                            ) {
                                val sweepAngle = soundLevel * 360f
                                drawArc(
                                    color = BalanceGold,
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

                        // Center button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPaused) MutedGrayDark.copy(alpha = 0.3f)
                                    else BalanceGold
                                )
                                .border(
                                    2.dp,
                                    if (isPaused) MutedGrayDark else BalanceGoldLight,
                                    CircleShape
                                )
                                .clickable { togglePause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isPaused -> Icons.Rounded.PlayArrow
                                    isListening -> Icons.Rounded.Mic
                                    else -> Icons.Rounded.MicNone
                                },
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = if (isPaused) MilkyWhiteText else ShadowBlack,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Transcription display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ShadowBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, BorderGrayDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (partialText.isNotBlank() && isListening) {
                                Text(
                                    text = "Partial:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BalanceGold.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = partialText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGrayDark,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(
                                    color = BorderGrayDark,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = transcriptionText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 26.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = if (errorState != null) ErrorRed else MilkyWhiteText,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPaused) {
                            Icon(
                                imageVector = Icons.Rounded.PauseCircle,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Paused — tap mic to resume",
                                fontSize = 12.sp,
                                color = WarningAmber.copy(alpha = 0.8f)
                            )
                        } else if (isListening) {
                            Icon(
                                imageVector = Icons.Rounded.FiberManualRecord,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = "Listening...",
                                fontSize = 12.sp,
                                color = SuccessGreen.copy(alpha = 0.8f)
                            )
                        } else if (errorState != null) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Error occurred",
                                fontSize = 12.sp,
                                color = ErrorRed.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "Processing complete",
                                fontSize = 12.sp,
                                color = MutedGrayDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                if (permissionGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGrayDark),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MutedGrayDark
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (isListening) {
                                    stopListening()
                                } else {
                                    startListening()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (errorState != null) ErrorRed else BalanceGold,
                                contentColor = ShadowBlack
                            )
                        ) {
                            Icon(
                                imageVector = if (isListening || isPaused) Icons.Rounded.Refresh
                                else Icons.Rounded.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (errorState != null) "Retry"
                                else if (isListening || isPaused) "Restart"
                                else "Start",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Language selector dialog
    if (showLanguageSelector) {
        val languages = listOf(
            "en" to "English",
            "es" to "Español",
            "fr" to "Français",
            "de" to "Deutsch",
            "it" to "Italiano",
            "pt" to "Português",
            "ru" to "Русский",
            "ja" to "日本語",
            "ko" to "한국어",
            "zh" to "中文",
            "hi" to "हिन्दी",
            "ar" to "العربية"
        )

        AlertDialog(
            onDismissRequest = { showLanguageSelector = false },
            containerColor = ShadowBlackCard,
            titleContentColor = MilkyWhiteText,
            textContentColor = MutedGrayDark,
            title = {
                Text("Select Language", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn {
                    items(languages.size) { index ->
                        val (code, name) = languages[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = code
                                    showLanguageSelector = false
                                    stopListening()
                                    startListening()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                color = if (code == selectedLanguage) BalanceGold else MilkyWhiteText,
                                fontWeight = if (code == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                            )
                            if (code == selectedLanguage) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = BalanceGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < languages.lastIndex) {
                            HorizontalDivider(color = BorderGrayDark)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageSelector = false }) {
                    Text("Close", color = BalanceGold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun PermissionRequestContent(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.MicOff,
            contentDescription = null,
            tint = MutedGrayDark.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "Microphone Access Required",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MilkyWhiteText
        )

        Text(
            text = "Troc needs microphone permission to capture your voice and transform it into balanced text. Your voice data is processed locally and never stored.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MutedGrayDark,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onGrantPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("grant_permission_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = BalanceGold,
                contentColor = ShadowBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grant Microphone Permission",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// Import required for Canvas drawing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
