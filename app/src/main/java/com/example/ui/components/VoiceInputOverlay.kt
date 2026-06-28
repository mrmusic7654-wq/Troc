// app/src/main/java/com/example/ui/components/VoiceInputOverlay.kt
package com.example.ui.components

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
import androidx.compose.ui.graphics.drawscope.Stroke
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
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        permissionGranted = isGranted
        if (!isGranted) errorState = "Microphone permission denied."
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_waveform")
    val pulseScale1 by infiniteTransition.animateFloat(0.9f, 1.4f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse1")
    val pulseScale2 by infiniteTransition.animateFloat(0.9f, 1.7f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse2")
    val pulseScale3 by infiniteTransition.animateFloat(0.9f, 2.0f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse3")
    val glowAlpha by infiniteTransition.animateFloat(0.1f, 0.35f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startListening() {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
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
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; isPaused = false; transcriptionText = "I'm listening..."; partialText = ""; errorState = null }
            override fun onBeginningOfSpeech() { transcriptionText = "Capturing..."; soundLevel = 0.5f }
            override fun onRmsChanged(rmsdB: Float) { soundLevel = (rmsdB + 10f).coerceIn(0f, 10f) / 10f }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { transcriptionText = "Processing..."; isListening = false; soundLevel = 0f }
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error."; SpeechRecognizer.ERROR_CLIENT -> "Client error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied."; SpeechRecognizer.ERROR_NETWORK -> "Network unavailable."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."; SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy."; SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout."
                    else -> "Recognition error: $error"
                }
                errorState = message; transcriptionText = message; isListening = false; isPaused = false; soundLevel = 0f
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) { transcriptionText = matches[0]; onResult(matches[0]) } else errorState = "No results."
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) { partialText = matches[0]; transcriptionText = matches[0] }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    fun stopListening() { speechRecognizer?.stopListening(); isListening = false }

    fun togglePause() {
        if (isPaused) {
            speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
            isPaused = false; isListening = true
        } else { speechRecognizer?.stopListening(); isPaused = true; isListening = false }
    }

    LaunchedEffect(permissionGranted) { if (permissionGranted) startListening() }
    DisposableEffect(Unit) { onDispose { speechRecognizer?.destroy(); speechRecognizer = null } }

    Box(modifier = modifier.fillMaxSize().background(ShadowBlack.copy(alpha = 0.95f)).clickable { onDismiss() }.padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().clickable(enabled = false) {}.testTag("voice_overlay_card"),
            colors = CardDefaults.cardColors(containerColor = ShadowBlackCard),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        YinYangLogo(size = 24.dp, isSpinning = isListening, glowEnabled = true)
                        Text("Vocal Harmony", fontWeight = FontWeight.Bold, color = MilkyWhiteText, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("close_voice_button")) {
                        Icon(Icons.Rounded.Close, "Close overlay", tint = MutedGrayDark, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(40.dp))

                if (!permissionGranted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.MicOff, null, tint = MutedGrayDark.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("Microphone Access Required", fontWeight = FontWeight.Bold, color = MilkyWhiteText)
                        Text("Troc needs microphone permission.", color = MutedGrayDark, fontSize = 13.sp)
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, colors = ButtonDefaults.buttonColors(containerColor = BalanceGold, contentColor = ShadowBlack), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Mic, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                        if (isListening && !isPaused) {
                            Box(Modifier.size(150.dp).scale(pulseScale3).clip(CircleShape).background(Brush.radialGradient(listOf(BalanceGold.copy(alpha = glowAlpha * 0.3f), Color.Transparent))))
                            Box(Modifier.size(120.dp).scale(pulseScale2).clip(CircleShape).background(Brush.radialGradient(listOf(BalanceGold.copy(alpha = glowAlpha * 0.5f), Color.Transparent))))
                            Box(Modifier.size(90.dp).scale(pulseScale1).clip(CircleShape).background(BalanceGold.copy(alpha = glowAlpha * 0.3f)))
                        }
                        if (isListening && !isPaused) {
                            Canvas(Modifier.size(90.dp)) { drawArc(BalanceGold, -90f, soundLevel * 360f, false, style = Stroke(3.dp.toPx())) }
                        }
                        Box(Modifier.size(72.dp).clip(CircleShape).background(if (isPaused) MutedGrayDark.copy(alpha = 0.3f) else BalanceGold).border(2.dp, if (isPaused) MutedGrayDark else BalanceGoldLight, CircleShape).clickable { togglePause() }, contentAlignment = Alignment.Center) {
                            Icon(when { isPaused -> Icons.Rounded.PlayArrow; isListening -> Icons.Rounded.Mic; else -> Icons.Rounded.MicNone }, if (isPaused) "Resume" else "Pause", tint = if (isPaused) MilkyWhiteText else ShadowBlack, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ShadowBlack), shape = RoundedCornerShape(16.dp), border = BorderStroke(0.5.dp, BorderGrayDark)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            if (partialText.isNotBlank() && isListening) {
                                Text("Partial:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BalanceGold.copy(alpha = 0.6f)); Spacer(Modifier.height(4.dp))
                                Text(partialText, color = MutedGrayDark, fontSize = 13.sp); Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = BorderGrayDark); Spacer(Modifier.height(4.dp))
                            }
                            Text(transcriptionText, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = if (errorState != null) ErrorRed else MilkyWhiteText, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            isPaused -> { Icon(Icons.Rounded.PauseCircle, null, tint = WarningAmber, modifier = Modifier.size(14.dp)); Text("Paused", fontSize = 12.sp, color = WarningAmber.copy(alpha = 0.8f)) }
                            isListening -> { Icon(Icons.Rounded.FiberManualRecord, null, tint = SuccessGreen, modifier = Modifier.size(8.dp)); Text("Listening...", fontSize = 12.sp, color = SuccessGreen.copy(alpha = 0.8f)) }
                            errorState != null -> { Icon(Icons.Rounded.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp)); Text("Error", fontSize = 12.sp, color = ErrorRed.copy(alpha = 0.8f)) }
                            else -> Text("Processing complete", fontSize = 12.sp, color = MutedGrayDark)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (permissionGranted) {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDismiss, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BorderGrayDark), colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedGrayDark)) { Text("Cancel", fontWeight = FontWeight.Medium) }
                        Button(onClick = { if (isListening) stopListening() else startListening() }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (errorState != null) ErrorRed else BalanceGold, contentColor = ShadowBlack)) {
                            Icon(if (isListening || isPaused) Icons.Rounded.Refresh else Icons.Rounded.Mic, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (errorState != null) "Retry" else if (isListening || isPaused) "Restart" else "Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
