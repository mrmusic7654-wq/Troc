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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    modifier: Modifier = Modifier,
    onFilesChanged: (List<AttachedFile>) -> Unit = {},
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val attachedFiles = FileUploadManager.getAttachedFiles()
    var showFilePickerMenu by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf<AttachedFile?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Multiple file picker launcher
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

    // Single file picker for camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Save bitmap to cache and get URI
            val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            val uri = Uri.fromFile(file)
            FileUploadManager.attachFile(
                context = context,
                uri = uri,
                onFileAttached = { onFilesChanged(attachedFiles.toList()) },
                onError = { errorMessage = it }
            )
        }
    }

    Column(modifier = modifier) {
        // Error snackbar
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                border = BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                    Text(error, fontSize = 11.sp, color = ErrorRed, modifier = Modifier.weight(1f))
                    IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Rounded.Close, "Dismiss", tint = ErrorRed, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Attached files row
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

        // File picker trigger row
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Add files button
                Surface(
                    onClick = { showFilePickerMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    color = BalanceGold.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.AttachFile, "Attach files", tint = BalanceGold, modifier = Modifier.size(16.dp))
                        Text("Add Files", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BalanceGold)
                    }
                }

                // Quick camera button
                Surface(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(20.dp),
                    color = MutedGrayDark.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, BorderGrayDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, "Camera", tint = MutedGrayDark, modifier = Modifier.size(14.dp))
                        Text("Camera", fontSize = 10.sp, color = MutedGrayDark)
                    }
                }

                // File count indicator
                if (attachedFiles.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SuccessGreen.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, SuccessGreen.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Checklist, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Text("${attachedFiles.size} file${if (attachedFiles.size != 1) "s" else ""}", fontSize = 10.sp, color = SuccessGreen)
                        }
                    }
                }
            }
        }

        // File picker dropdown
        DropdownMenu(
            expanded = showFilePickerMenu,
            onDismissRequest = { showFilePickerMenu = false },
            modifier = Modifier.background(ShadowBlackCard, RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Images (PNG, JPEG, WebP, HEIC, HEIF)", fontSize = 12.sp) },
                onClick = {
                    filePickerLauncher.launch("image/*")
                    showFilePickerMenu = false
                },
                leadingIcon = { Icon(Icons.Rounded.Image, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Documents (PDF, Text, CSV, JSON)", fontSize = 12.sp) },
                onClick = {
                    filePickerLauncher.launch("*/*")
                    showFilePickerMenu = false
                },
                leadingIcon = { Icon(Icons.Rounded.Description, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Audio (MP3, WAV, OGG, FLAC)", fontSize = 12.sp) },
                onClick = {
                    filePickerLauncher.launch("audio/*")
                    showFilePickerMenu = false
                },
                leadingIcon = { Icon(Icons.Rounded.AudioFile, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Video (MP4, MPEG, QuickTime)", fontSize = 12.sp) },
                onClick = {
                    filePickerLauncher.launch("video/*")
                    showFilePickerMenu = false
                },
                leadingIcon = { Icon(Icons.Rounded.VideoFile, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
            )
            if (attachedFiles.isNotEmpty()) {
                HorizontalDivider(color = BorderGrayDark)
                DropdownMenuItem(
                    text = { Text("Clear all (${attachedFiles.size})", fontSize = 12.sp, color = ErrorRed) },
                    onClick = {
                        FileUploadManager.clearAll()
                        onFilesChanged(emptyList())
                        showFilePickerMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }

    // Preview dialog
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
                // File type badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BalanceGold.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.fileType == FileType.IMAGE) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file.uri)
                                .crossfade(true)
                                .build(),
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

            Text(
                file.fileName,
                fontSize = 10.sp,
                color = MilkyWhiteText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                file.formattedSize,
                fontSize = 9.sp,
                color = if (file.isWithinSizeLimit) MutedGrayDark else ErrorRed,
                fontFamily = FontFamily.Monospace
            )

            if (!file.isUploaded) {
                Spacer(Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { file.uploadProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                    color = BalanceGold,
                    trackColor = BorderGrayDark
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
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(file.icon, fontSize = 20.sp)
                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (file.fileType == FileType.IMAGE) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(file.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                HorizontalDivider(color = BorderGrayDark)

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Type", fontSize = 11.sp, color = MutedGrayDark)
                    Text(file.fileType.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MilkyWhiteText)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Size", fontSize = 11.sp, color = MutedGrayDark)
                    Text(file.formattedSize, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MilkyWhiteText)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("MIME", fontSize = 11.sp, color = MutedGrayDark)
                    Text(file.mimeType, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MilkyWhiteText)
                }
                if (file.pageCount != null) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Pages", fontSize = 11.sp, color = MutedGrayDark)
                        Text("${file.pageCount}", fontSize = 11.sp, color = MilkyWhiteText)
                    }
                }
                if (file.durationSeconds != null) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Duration", fontSize = 11.sp, color = MutedGrayDark)
                        Text("${file.durationSeconds}s", fontSize = 11.sp, color = MilkyWhiteText)
                    }
                }

                if (!file.isWithinSizeLimit) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Text("File exceeds size limit", fontSize = 11.sp, color = ErrorRed)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = BalanceGold, fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
