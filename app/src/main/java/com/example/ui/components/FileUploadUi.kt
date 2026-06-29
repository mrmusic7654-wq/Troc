// app/src/main/java/com/example/ui/components/FileUploadUI.kt
package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.filemanager.AttachedFile
import com.example.data.filemanager.FileType
import com.example.data.filemanager.FileUploadManager
import com.example.ui.theme.*

@Composable
fun FileUploadBar(
    onFilesChanged: (List<AttachedFile>) -> Unit = {},
    enabled: Boolean = true,
    compact: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val attachedFiles = FileUploadManager.getAttachedFiles()
    var showPickerMenu by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf<AttachedFile?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            FileUploadManager.attachFile(
                context = context,
                uri = uri,
                onFileAttached = { onFilesChanged(attachedFiles.toList()) },
                onError = { errorMessage = it }
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            FileUploadManager.attachFile(
                context = context,
                uri = Uri.fromFile(file),
                onFileAttached = { onFilesChanged(attachedFiles.toList()) },
                onError = { errorMessage = it }
            )
        }
    }

    if (compact) {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // COMPACT "+" BUTTON — ChatGPT Style
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Box(modifier = modifier) {
            IconButton(
                onClick = { showPickerMenu = true },
                enabled = enabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircle,
                    contentDescription = "Attach files",
                    tint = if (enabled) BalanceGold.copy(alpha = 0.8f) else MutedGrayDark.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showPickerMenu,
                onDismissRequest = { showPickerMenu = false },
                modifier = Modifier
                    .background(ShadowBlackCard, RoundedCornerShape(14.dp))
                    .border(0.5.dp, BalanceGold.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Image, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Images", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText)
                                Text("PNG, JPEG, WebP, HEIC, HEIF", fontSize = 10.sp, color = MutedGrayDark)
                            }
                        }
                    },
                    onClick = { filePickerLauncher.launch("image/*"); showPickerMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Description, null, tint = InfoBlue, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Documents", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText)
                                Text("PDF, Text, CSV, JSON", fontSize = 10.sp, color = MutedGrayDark)
                            }
                        }
                    },
                    onClick = { filePickerLauncher.launch("*/*"); showPickerMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AudioFile, null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Audio", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText)
                                Text("MP3, WAV, OGG, FLAC", fontSize = 10.sp, color = MutedGrayDark)
                            }
                        }
                    },
                    onClick = { filePickerLauncher.launch("audio/*"); showPickerMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VideoFile, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Video", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText)
                                Text("MP4, MPEG, QuickTime", fontSize = 10.sp, color = MutedGrayDark)
                            }
                        }
                    },
                    onClick = { filePickerLauncher.launch("video/*"); showPickerMenu = false }
                )
                HorizontalDivider(color = BorderGrayDark, modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = BalanceGold, modifier = Modifier.size(18.dp))
                            Text("Take Photo", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText)
                        }
                    },
                    onClick = { cameraLauncher.launch(null); showPickerMenu = false }
                )
                if (attachedFiles.isNotEmpty()) {
                    HorizontalDivider(color = BorderGrayDark, modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Text("Clear all (${attachedFiles.size})", fontSize = 13.sp, color = ErrorRed, fontWeight = FontWeight.Medium)
                            }
                        },
                        onClick = { FileUploadManager.clearAll(); onFilesChanged(emptyList()); showPickerMenu = false }
                    )
                }
            }
        }
    } else {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // FULL FILE UPLOAD BAR — Standalone Mode
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Column(modifier = modifier) {
            // Error Banner
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
                    border = BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        Text(error, fontSize = 11.sp, color = ErrorRed, modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Rounded.Close, "Dismiss", tint = ErrorRed, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Attached Files Preview Row
            if (attachedFiles.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 48.dp)
                ) {
                    items(attachedFiles.toList(), key = { it.id }) { file ->
                        AttachedFileChip(
                            file = file,
                            onRemove = {
                                FileUploadManager.removeFile(file.id)
                                onFilesChanged(attachedFiles.toList())
                            },
                            onPreview = { showPreviewDialog = file }
                        )
                    }
                }
            }

            // Action Buttons Row
            if (enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showPickerMenu = true },
                        shape = RoundedCornerShape(20.dp),
                        color = BalanceGold.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AttachFile, "Attach", tint = BalanceGold, modifier = Modifier.size(16.dp))
                            Text("Add Files", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BalanceGold)
                        }
                    }

                    Surface(
                        onClick = { cameraLauncher.launch(null) },
                        shape = RoundedCornerShape(20.dp),
                        color = MutedGrayDark.copy(alpha = 0.05f),
                        border = BorderStroke(0.5.dp, BorderGrayDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CameraAlt, "Camera", tint = MutedGrayDark, modifier = Modifier.size(14.dp))
                            Text("Camera", fontSize = 10.sp, color = MutedGrayDark)
                        }
                    }

                    if (attachedFiles.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SuccessGreen.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, SuccessGreen.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Checklist, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                Text(
                                    "${attachedFiles.size} file${if (attachedFiles.size != 1) "s" else ""}",
                                    fontSize = 10.sp,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }

            // Full Picker Menu
            DropdownMenu(
                expanded = showPickerMenu,
                onDismissRequest = { showPickerMenu = false },
                modifier = Modifier
                    .background(ShadowBlackCard, RoundedCornerShape(14.dp))
                    .border(0.5.dp, BalanceGold.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                DropdownMenuItem(
                    text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Image, null, tint = SuccessGreen, modifier = Modifier.size(18.dp)); Text("Images (PNG, JPEG, WebP, HEIC, HEIF)", fontSize = 12.sp, color = MilkyWhiteText) } },
                    onClick = { filePickerLauncher.launch("image/*"); showPickerMenu = false }
                )
                DropdownMenuItem(
                    text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Description, null, tint = InfoBlue, modifier = Modifier.size(18.dp)); Text("Documents (PDF, Text, CSV, JSON)", fontSize = 12.sp, color = MilkyWhiteText) } },
                    onClick = { filePickerLauncher.launch("*/*"); showPickerMenu = false }
                )
                DropdownMenuItem(
                    text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.AudioFile, null, tint = WarningAmber, modifier = Modifier.size(18.dp)); Text("Audio (MP3, WAV, OGG, FLAC)", fontSize = 12.sp, color = MilkyWhiteText) } },
                    onClick = { filePickerLauncher.launch("audio/*"); showPickerMenu = false }
                )
                DropdownMenuItem(
                    text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.VideoFile, null, tint = ErrorRed, modifier = Modifier.size(18.dp)); Text("Video (MP4, MPEG, QuickTime)", fontSize = 12.sp, color = MilkyWhiteText) } },
                    onClick = { filePickerLauncher.launch("video/*"); showPickerMenu = false }
                )
                HorizontalDivider(color = BorderGrayDark, modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.CameraAlt, null, tint = BalanceGold, modifier = Modifier.size(18.dp)); Text("Take Photo", fontSize = 12.sp, color = MilkyWhiteText) } },
                    onClick = { cameraLauncher.launch(null); showPickerMenu = false }
                )
                if (attachedFiles.isNotEmpty()) {
                    HorizontalDivider(color = BorderGrayDark, modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.DeleteSweep, null, tint = ErrorRed, modifier = Modifier.size(18.dp)); Text("Clear all (${attachedFiles.size})", fontSize = 12.sp, color = ErrorRed, fontWeight = FontWeight.Medium) } },
                        onClick = { FileUploadManager.clearAll(); onFilesChanged(emptyList()); showPickerMenu = false }
                    )
                }
            }
        }
    }

    // Preview Dialog
    showPreviewDialog?.let { file ->
        FilePreviewDialog(file = file, onDismiss = { showPreviewDialog = null })
    }
}

@Composable
private fun AttachedFileChip(
    file: AttachedFile,
    onRemove: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAnimation by rememberInfiniteTransition(label = "uploadPulse").animateFloat(
        1f, 1.03f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    Card(
        modifier = modifier
            .width(120.dp)
            .scale(if (!file.isUploaded) pulseAnimation else 1f)
            .clickable { onPreview() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ShadowBlackCard),
        border = BorderStroke(0.5.dp, if (file.isWithinSizeLimit) BorderGrayDark else ErrorRed.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BalanceGold.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.fileType == FileType.IMAGE) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(file.uri).crossfade(true).build(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(file.icon, fontSize = 14.sp)
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Rounded.Close, "Remove", tint = MutedGrayDark, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(file.fileName, fontSize = 10.sp, color = MilkyWhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(file.formattedSize, fontSize = 9.sp, color = if (file.isWithinSizeLimit) MutedGrayDark else ErrorRed, fontFamily = FontFamily.Monospace)
            if (!file.isUploaded) {
                Spacer(Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { file.uploadProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                    color = BalanceGold, trackColor = BorderGrayDark
                )
            }
        }
    }
}

@Composable
private fun FilePreviewDialog(file: AttachedFile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ShadowBlackCard,
        titleContentColor = MilkyWhiteText,
        textContentColor = MutedGrayDark,
        icon = { Text(file.icon, fontSize = 32.sp) },
        title = { Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (file.fileType == FileType.IMAGE) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(file.uri).crossfade(true).build(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                HorizontalDivider(color = BorderGrayDark)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Type", fontSize = 11.sp, color = MutedGrayDark); Text(file.fileType.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Size", fontSize = 11.sp, color = MutedGrayDark); Text(file.formattedSize, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MilkyWhiteText) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("MIME", fontSize = 11.sp, color = MutedGrayDark); Text(file.mimeType, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MilkyWhiteText) }
                if (!file.isWithinSizeLimit) {
                    Card(colors = CardDefaults.cardColors(containerColor = ErrorRedBg), shape = RoundedCornerShape(6.dp)) {
                        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Text("File exceeds size limit", fontSize = 11.sp, color = ErrorRed)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = BalanceGold, fontWeight = FontWeight.Bold) } },
        shape = RoundedCornerShape(16.dp)
    )
}
