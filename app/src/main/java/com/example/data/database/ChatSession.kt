// app/src/main/java/com/example/data/database/ChatSession.kt
package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    val isPinned: Boolean = false,

    val isArchived: Boolean = false,

    val messageCount: Int = 0,

    val lastMessagePreview: String = "",

    val lastMessageRole: String = "",

    val lastMessageTimestamp: Long = 0,

    val totalTokensUsed: Int = 0,

    val modelUsed: String = "",

    val personalityId: String = "",

    val personalityName: String = "",

    val contextSnapshotJson: String? = null,

    val tags: String = "",

    val colorLabel: String = ""
) {

    val displayTitle: String
        get() = title.ifBlank { "Empty Balance" }

    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val formattedDate: String
        get() {
            val diff = System.currentTimeMillis() - createdAt
            return when {
                diff < 60_000 -> "Just now"
                diff < 3_600_000 -> "${diff / 60_000}m ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                diff < 604_800_000 -> "${diff / 86_400_000}d ago"
                diff < 2_592_000_000 -> "${diff / 604_800_000}w ago"
                else -> {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(createdAt))
                }
            }
        }

    val isActive: Boolean get() = !isArchived
    val hasMessages: Boolean get() = messageCount > 0
    val hasLastMessage: Boolean get() = lastMessagePreview.isNotBlank()
    val hasPersonality: Boolean get() = personalityId.isNotBlank()

    val lastMessageSnippet: String
        get() = lastMessagePreview.take(80).replace("\n", " ") +
                if (lastMessagePreview.length > 80) "…" else ""

    val modelDisplayName: String
        get() {
            if (modelUsed.isBlank()) return ""
            return try {
                com.example.data.api.GeminiModel.fromModelId(modelUsed).displayName
            } catch (e: Exception) {
                modelUsed
            }
        }

    fun toJson(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isPinned" to isPinned,
        "isArchived" to isArchived,
        "messageCount" to messageCount,
        "lastMessagePreview" to lastMessagePreview,
        "lastMessageRole" to lastMessageRole,
        "lastMessageTimestamp" to lastMessageTimestamp,
        "totalTokensUsed" to totalTokensUsed,
        "modelUsed" to modelUsed,
        "personalityId" to personalityId,
        "personalityName" to personalityName,
        "contextSnapshotJson" to contextSnapshotJson,
        "tags" to tags,
        "colorLabel" to colorLabel
    )

    fun pin(): ChatSession = copy(isPinned = true, updatedAt = System.currentTimeMillis())
    fun unpin(): ChatSession = copy(isPinned = false, updatedAt = System.currentTimeMillis())
    fun archive(): ChatSession = copy(isArchived = true, updatedAt = System.currentTimeMillis())
    fun unarchive(): ChatSession = copy(isArchived = false, updatedAt = System.currentTimeMillis())

    fun rename(newTitle: String): ChatSession = copy(
        title = newTitle,
        updatedAt = System.currentTimeMillis()
    )

    fun addTag(tag: String): ChatSession {
        val currentTags = tagList.toMutableList()
        if (tag.isNotBlank() && !currentTags.contains(tag.trim())) {
            currentTags.add(tag.trim())
        }
        return copy(tags = currentTags.joinToString(", "), updatedAt = System.currentTimeMillis())
    }

    fun removeTag(tag: String): ChatSession {
        val currentTags = tagList.toMutableList()
        currentTags.remove(tag.trim())
        return copy(tags = currentTags.joinToString(", "), updatedAt = System.currentTimeMillis())
    }

    fun updateWithLastMessage(
        preview: String,
        role: String,
        timestamp: Long = System.currentTimeMillis(),
        tokenCount: Int = 0
    ): ChatSession = copy(
        messageCount = messageCount + 1,
        lastMessagePreview = preview,
        lastMessageRole = role,
        lastMessageTimestamp = timestamp,
        totalTokensUsed = totalTokensUsed + tokenCount,
        updatedAt = System.currentTimeMillis()
    )

    fun setModel(modelId: String): ChatSession = copy(
        modelUsed = modelId,
        updatedAt = System.currentTimeMillis()
    )

    fun setPersonality(personalityId: String, personalityName: String): ChatSession = copy(
        personalityId = personalityId,
        personalityName = personalityName,
        updatedAt = System.currentTimeMillis()
    )

    fun setContextSnapshot(snapshotJson: String?): ChatSession = copy(
        contextSnapshotJson = snapshotJson,
        updatedAt = System.currentTimeMillis()
    )

    fun setColorLabel(color: String): ChatSession = copy(
        colorLabel = color,
        updatedAt = System.currentTimeMillis()
    )

    fun touch(): ChatSession = copy(updatedAt = System.currentTimeMillis())

    companion object {
        const val COLOR_NONE = ""
        const val COLOR_GOLD = "gold"
        const val COLOR_MILKY = "milky"
        const val COLOR_SHADOW = "shadow"
        const val COLOR_ROSE = "rose"
        const val COLOR_OCEAN = "ocean"
        const val COLOR_FOREST = "forest"

        val availableColors = listOf(
            COLOR_NONE to "None",
            COLOR_GOLD to "Gold",
            COLOR_MILKY to "Milky",
            COLOR_SHADOW to "Shadow",
            COLOR_ROSE to "Rose",
            COLOR_OCEAN to "Ocean",
            COLOR_FOREST to "Forest"
        )

        fun createNew(
            title: String = "New Balance Chat",
            modelUsed: String = "",
            personalityId: String = "",
            personalityName: String = ""
        ): ChatSession = ChatSession(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelUsed = modelUsed,
            personalityId = personalityId,
            personalityName = personalityName
        )

        fun fromJson(json: Map<String, Any?>): ChatSession {
            return ChatSession(
                id = (json["id"] as? Number)?.toLong() ?: 0,
                title = json["title"] as? String ?: "",
                createdAt = (json["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (json["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isPinned = json["isPinned"] as? Boolean ?: false,
                isArchived = json["isArchived"] as? Boolean ?: false,
                messageCount = (json["messageCount"] as? Number)?.toInt() ?: 0,
                lastMessagePreview = json["lastMessagePreview"] as? String ?: "",
                lastMessageRole = json["lastMessageRole"] as? String ?: "",
                lastMessageTimestamp = (json["lastMessageTimestamp"] as? Number)?.toLong() ?: 0,
                totalTokensUsed = (json["totalTokensUsed"] as? Number)?.toInt() ?: 0,
                modelUsed = json["modelUsed"] as? String ?: "",
                personalityId = json["personalityId"] as? String ?: "",
                personalityName = json["personalityName"] as? String ?: "",
                contextSnapshotJson = json["contextSnapshotJson"] as? String,
                tags = json["tags"] as? String ?: "",
                colorLabel = json["colorLabel"] as? String ?: ""
            )
        }

        val SORT_BY_UPDATED = Comparator<ChatSession> { a, b ->
            when {
                a.isPinned && !b.isPinned -> -1
                !a.isPinned && b.isPinned -> 1
                else -> b.updatedAt.compareTo(a.updatedAt)
            }
        }

        val SORT_BY_CREATED = Comparator<ChatSession> { a, b ->
            when {
                a.isPinned && !b.isPinned -> -1
                !a.isPinned && b.isPinned -> 1
                else -> b.createdAt.compareTo(a.createdAt)
            }
        }

        val SORT_BY_MESSAGE_COUNT = Comparator<ChatSession> { a, b ->
            when {
                a.isPinned && !b.isPinned -> -1
                !a.isPinned && b.isPinned -> 1
                else -> b.messageCount.compareTo(a.messageCount)
            }
        }

        val SORT_BY_TITLE = Comparator<ChatSession> { a, b ->
            when {
                a.isPinned && !b.isPinned -> -1
                !a.isPinned && b.isPinned -> 1
                else -> a.title.compareTo(b.title, ignoreCase = true)
            }
        }
    }
}
