// app/src/main/java/com/example/data/filemanager/FileUploadManager.kt
package com.example.data.filemanager

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.io.FileOutputStream

data class AttachedFile(
    val id: String = "file_${System.currentTimeMillis()}",
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val fileType: FileType,
    val localPath: String? = null,
    val isUploaded: Boolean = false,
    val uploadProgress: Float = 0f,
    val pageCount: Int? = null,
    val durationSeconds: Int? = null
) {
    val formattedSize: String
        get() = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            fileSize < 1024 * 1024 * 1024 -> "${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(fileSize / (1024.0 * 1024.0 * 1024.0))} GB"
        }

    val icon: String
        get() = when (fileType) {
            FileType.IMAGE -> "🖼️"
            FileType.AUDIO -> "🎵"
            FileType.VIDEO -> "🎬"
            FileType.DOCUMENT -> "📄"
            FileType.PDF -> "📑"
            FileType.TEXT -> "📝"
            FileType.CODE -> "💻"
            FileType.ARCHIVE -> "📦"
            FileType.OTHER -> "📎"
        }

    val maxAllowedSize: Long
        get() = when (fileType) {
            FileType.IMAGE -> 7 * 1024 * 1024
            FileType.AUDIO -> 7 * 1024 * 1024
            FileType.VIDEO -> 50 * 1024 * 1024
            FileType.DOCUMENT -> 50 * 1024 * 1024
            FileType.PDF -> 50 * 1024 * 1024
            else -> 7 * 1024 * 1024
        }

    val isWithinSizeLimit: Boolean
        get() = fileSize <= maxAllowedSize
}

enum class FileType(
    val label: String,
    val mimeTypes: List<String>,
    val maxCount: Int,
    val maxFileSize: Long
) {
    IMAGE("Image", listOf("image/png", "image/jpeg", "image/webp", "image/heic", "image/heif", "image/gif"), 3000, 7 * 1024 * 1024),
    AUDIO("Audio", listOf("audio/mp3", "audio/wav", "audio/ogg", "audio/flac", "audio/mpeg", "audio/aac"), 1, 7 * 1024 * 1024),
    VIDEO("Video", listOf("video/mp4", "video/mpeg", "video/quicktime", "video/avi", "video/webm"), 10, 50 * 1024 * 1024),
    DOCUMENT("Document", listOf("text/plain", "text/csv", "text/html", "application/json", "application/xml"), 3000, 50 * 1024 * 1024),
    PDF("PDF", listOf("application/pdf"), 3000, 50 * 1024 * 1024),
    TEXT("Text", listOf("text/plain", "text/markdown"), 3000, 7 * 1024 * 1024),
    CODE("Code", listOf("text/x-python", "text/x-java", "text/x-kotlin", "text/javascript", "text/typescript", "application/x-sh"), 3000, 7 * 1024 * 1024),
    ARCHIVE("Archive", listOf("application/zip", "application/gzip", "application/x-tar"), 10, 50 * 1024 * 1024),
    OTHER("Other", listOf("*/*"), 10, 7 * 1024 * 1024);

    companion object {
        fun fromMimeType(mimeType: String): FileType {
            for (type in entries) {
                if (type.mimeTypes.any { mimeType.matchesGlob(it) || mimeType == it }) return type
            }
            return OTHER
        }
    }
}

object FileUploadManager {
    private val attachedFiles: SnapshotStateList<AttachedFile> = mutableStateListOf()

    fun getAttachedFiles(): SnapshotStateList<AttachedFile> = attachedFiles

    fun attachFile(
        context: Context,
        uri: Uri,
        onFileAttached: (AttachedFile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileType = FileType.fromMimeType(mimeType)

            var fileName = "unknown"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: "unknown"
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                }
            }

            // Check file count limits
            val existingCount = attachedFiles.count { it.fileType == fileType }
            if (existingCount >= fileType.maxCount) {
                onError("Maximum ${fileType.maxCount} ${fileType.label.lowercase()} files allowed per request")
                return
            }

            // Check total image count
            val totalImages = attachedFiles.count { it.fileType == FileType.IMAGE }
            if (fileType == FileType.IMAGE && totalImages >= 3000) {
                onError("Maximum 3,000 images per request")
                return
            }

            // Check video count
            val totalVideos = attachedFiles.count { it.fileType == FileType.VIDEO }
            if (fileType == FileType.VIDEO && totalVideos >= 10) {
                onError("Maximum 10 videos per request")
                return
            }

            // Check audio count
            val totalAudio = attachedFiles.count { it.fileType == FileType.AUDIO }
            if (fileType == FileType.AUDIO && totalAudio >= 1) {
                onError("Maximum 1 audio file per request")
                return
            }

            // Copy to local storage for upload
            val localFile = copyToLocalCache(context, uri, fileName)

            val attached = AttachedFile(
                uri = uri,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize,
                fileType = fileType,
                localPath = localFile?.absolutePath
            )

            if (fileSize > attached.maxAllowedSize) {
                onError("${attached.formattedSize} exceeds ${attached.formattedSize} limit for ${fileType.label}")
                return
            }

            attachedFiles.add(attached)
            onFileAttached(attached)
        } catch (e: Exception) {
            onError("Failed to attach file: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun removeFile(fileId: String) {
        val file = attachedFiles.find { it.id == fileId }
        if (file != null) {
            file.localPath?.let { File(it).delete() }
            attachedFiles.remove(file)
        }
    }

    fun clearAll() {
        attachedFiles.forEach { it.localPath?.let { path -> File(path).delete() } }
        attachedFiles.clear()
    }

    fun getTotalSize(): Long = attachedFiles.sumOf { it.fileSize }

    fun validateForModel(supportsVision: Boolean, supportsAudio: Boolean, supportsVideo: Boolean): String? {
        if (attachedFiles.any { it.fileType == FileType.IMAGE } && !supportsVision) {
            return "Selected model does not support image inputs"
        }
        if (attachedFiles.any { it.fileType == FileType.AUDIO } && !supportsAudio) {
            return "Selected model does not support audio inputs"
        }
        if (attachedFiles.any { it.fileType == FileType.VIDEO } && !supportsVideo) {
            return "Selected model does not support video inputs"
        }
        return null
    }

    private fun copyToLocalCache(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "troc_uploads")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun String.matchesGlob(pattern: String): Boolean {
        if (pattern == "*/*") return true
        val regex = Regex(pattern.replace("*", ".*").replace("?", "."))
        return regex.matches(this)
    }
}
