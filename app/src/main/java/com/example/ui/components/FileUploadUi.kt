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
                context = context, uri = Uri.fromFile(file),
                onFileAttached = { onFilesChanged(attachedFiles.toList()) },
                onError = { errorMessage = it }
            )
        }
    }

    if (compact) {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // COMPACT "+" BUTTON (ChatGPT style)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Box(modifier = modifier) {
            IconButton(
                onClick = { showPickerMenu = true },
                enabled = enabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Attach files",
                    tint = if (enabled) MutedGrayDark else MutedGrayDark.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showPickerMenu,
                onDismissRequest = { showPickerMenu = false },
                modifier = Modifier.background(ShadowBlackCard, RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Images", fontSize = 12.sp) },
                    onClick = { filePickerLauncher.launch("image/*"); showPickerMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.Image, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Documents", fontSize = 12.sp) },
                    onClick = { filePickerLauncher.launch("*/*"); showPickerMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.Description, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Audio", fontSize = 12.sp) },
                    onClick = { filePickerLauncher.launch("audio/*"); showPickerMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.AudioFile, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Video", fontSize = 12.sp) },
                    onClick = { filePickerLauncher.launch("video/*"); showPickerMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.VideoFile, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
                )
                HorizontalDivider(color = BorderGrayDark)
                DropdownMenuItem(
                    text = { Text("Camera", fontSize = 12.sp) },
                    onClick = { cameraLauncher.launch(null); showPickerMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.CameraAlt, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    } else {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // FULL FILE UPLOAD BAR
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Column(modifier = modifier) {
            errorMessage?.let { error ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    border = BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(8.dp), Alignment.CenterVertically, Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        Text(error, fontSize = 11.sp, color = ErrorRed, modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) { Icon(Icons.Rounded.Close, "Dismiss", tint = ErrorRed, modifier = Modifier.size(12.dp)) }
                    }
                }
            }

            if (attachedFiles.isNotEmpty()) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 48.dp)
                ) {
                    items(attachedFiles.toList(), key = { it.id }) { file ->
                        AttachedFileChip(file, { FileUploadManager.removeFile(file.id); onFilesChanged(attachedFiles.toList()) })
                    }
                }
            }

            if (enabled) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), Arrangement.spacedBy(6.dp)) {
                    Surface(onClick = { showPickerMenu = true }, shape = RoundedCornerShape(20.dp), color = BalanceGold.copy(alpha = 0.08f), border = BorderStroke(0.5.dp, BalanceGold.copy(alpha = 0.2f))) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), Alignment.CenterVertically, Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AttachFile, "Attach", tint = BalanceGold, modifier = Modifier.size(16.dp))
                            Text("Add Files", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BalanceGold)
                        }
                    }
                    Surface(onClick = { cameraLauncher.launch(null) }, shape = RoundedCornerShape(20.dp), color = MutedGrayDark.copy(alpha = 0.05f), border = BorderStroke(0.5.dp, BorderGrayDark)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), Alignment.CenterVertically, Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.CameraAlt, "Camera", tint = MutedGrayDark, modifier = Modifier.size(14.dp))
                            Text("Camera", fontSize = 10.sp, color = MutedGrayDark)
                        }
                    }
                }
            }

            DropdownMenu(expanded = showPickerMenu, onDismissRequest = { showPickerMenu = false }, modifier = Modifier.background(ShadowBlackCard, RoundedCornerShape(12.dp))) {
                DropdownMenuItem(text = { Text("Images (PNG, JPEG, WebP, HEIC)", fontSize = 12.sp) }, onClick = { filePickerLauncher.launch("image/*"); showPickerMenu = false }, leadingIcon = { Icon(Icons.Rounded.Image, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Documents (PDF, Text, CSV, JSON)", fontSize = 12.sp) }, onClick = { filePickerLauncher.launch("*/*"); showPickerMenu = false }, leadingIcon = { Icon(Icons.Rounded.Description, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Audio (MP3, WAV, OGG, FLAC)", fontSize = 12.sp) }, onClick = { filePickerLauncher.launch("audio/*"); showPickerMenu = false }, leadingIcon = { Icon(Icons.Rounded.AudioFile, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Video (MP4, MPEG, QuickTime)", fontSize = 12.sp) }, onClick = { filePickerLauncher.launch("video/*"); showPickerMenu = false }, leadingIcon = { Icon(Icons.Rounded.VideoFile, null, tint = MutedGrayDark, modifier = Modifier.size(18.dp)) })
                if (attachedFiles.isNotEmpty()) {
                    HorizontalDivider(color = BorderGrayDark)
                    DropdownMenuItem(text = { Text("Clear all (${attachedFiles.size})", fontSize = 12.sp, color = ErrorRed) }, onClick = { FileUploadManager.clearAll(); onFilesChanged(emptyList()); showPickerMenu = false }, leadingIcon = { Icon(Icons.Rounded.DeleteSweep, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) })
                }
            }
        }
    }
}

@Composable
private fun AttachedFileChip(file: AttachedFile, onRemove: () -> Unit) {
    Card(
        Modifier.width(100.dp), shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ShadowBlack),
        border = BorderStroke(0.5.dp, if (file.isWithinSizeLimit) BorderGrayDark else ErrorRed.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(6.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(BalanceGold.copy(alpha = 0.1f)), Alignment.Center) {
                    if (file.fileType == FileType.IMAGE) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(file.uri).crossfade(true).build(), contentDescription = "Preview", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                    } else Text(file.icon, fontSize = 12.sp)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) { Icon(Icons.Rounded.Close, "Remove", tint = MutedGrayDark, modifier = Modifier.size(10.dp)) }
            }
            Spacer(Modifier.height(2.dp))
            Text(file.fileName, fontSize = 9.sp, color = MilkyWhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(file.formattedSize, fontSize = 8.sp, color = if (file.isWithinSizeLimit) MutedGrayDark else ErrorRed, fontFamily = FontFamily.Monospace)
        }
    }
}
