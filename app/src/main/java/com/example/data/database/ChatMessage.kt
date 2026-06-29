// app/src/main/java/com/example/data/database/ChatMessage.kt
package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "timestamp"]),
        Index(value = ["role"]),
        Index(value = ["timestamp"])
    ]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sessionId: Long,

    val role: String,

    val text: String,

    val reasoning: String? = null,

    val durationMs: Long? = null,

    val timestamp: Long = System.currentTimeMillis(),

    val tokenCount: Int? = null,

    val isEdited: Boolean = false,

    val isBookmarked: Boolean = false,

    val parentMessageId: Long? = null,

    val metadata: String? = null,

    // File attachment support
    val attachedFilesJson: String? = null,

    val modelUsed: String? = null,

    val personalityUsed: String? = null,

    val generationConfigJson: String? = null
) {

    val isUser: Boolean get() = role == ROLE_USER
    val isModel: Boolean get() = role == ROLE_MODEL
    val isSystem: Boolean get() = role == ROLE_SYSTEM
    val hasReasoning: Boolean get() = !reasoning.isNullOrBlank()
    val hasAttachments: Boolean get() = !attachedFilesJson.isNullOrBlank()

    val formattedDuration: String
        get() {
            if (durationMs == null) return ""
            val seconds = durationMs / 1000.0
            return when {
                seconds < 1.0 -> "${durationMs}ms"
                seconds < 60.0 -> "${String.format("%.1f", seconds)}s"
                else -> "${(seconds / 60).toInt()}m ${(seconds % 60).toInt()}s"
            }
        }

    val preview: String
        get() = text.take(100).replace("\n", " ").trim() + if (text.length > 100) "…" else ""

    val wordCount: Int
        get() = text.split("\\s+".toRegex()).count { it.isNotBlank() }

    val characterCount: Int
        get() = text.length

    val estimatedTokens: Int
        get() = (text.length / 4) + (reasoning?.length?.div(4) ?: 0) + 8

    fun toPromptFormat(): String = when (role) {
        ROLE_USER -> "User: $text"
        ROLE_MODEL -> "Assistant: $text"
        ROLE_SYSTEM -> "System: $text"
        else -> text
    }

    fun copyWithEdit(newText: String): ChatMessage = copy(
        text = newText,
        isEdited = true,
        timestamp = System.currentTimeMillis()
    )

    fun toggleBookmark(): ChatMessage = copy(isBookmarked = !isBookmarked)

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODEL = "model"
        const val ROLE_SYSTEM = "system"

        fun createUserMessage(
            sessionId: Long,
            text: String,
            parentMessageId: Long? = null,
            attachedFilesJson: String? = null,
            modelUsed: String? = null,
            personalityUsed: String? = null
        ): ChatMessage = ChatMessage(
            sessionId = sessionId,
            role = ROLE_USER,
            text = text,
            parentMessageId = parentMessageId,
            attachedFilesJson = attachedFilesJson,
            modelUsed = modelUsed,
            personalityUsed = personalityUsed
        )

        fun createModelMessage(
            sessionId: Long,
            text: String,
            reasoning: String? = null,
            durationMs: Long? = null,
            tokenCount: Int? = null,
            parentMessageId: Long? = null,
            modelUsed: String? = null,
            personalityUsed: String? = null,
            generationConfigJson: String? = null
        ): ChatMessage = ChatMessage(
            sessionId = sessionId,
            role = ROLE_MODEL,
            text = text,
            reasoning = reasoning,
            durationMs = durationMs,
            tokenCount = tokenCount,
            parentMessageId = parentMessageId,
            modelUsed = modelUsed,
            personalityUsed = personalityUsed,
            generationConfigJson = generationConfigJson
        )

        fun createSystemMessage(
            sessionId: Long,
            text: String
        ): ChatMessage = ChatMessage(
            sessionId = sessionId,
            role = ROLE_SYSTEM,
            text = text
        )

        fun fromJson(json: Map<String, Any?>): ChatMessage {
            return ChatMessage(
                id = (json["id"] as? Number)?.toLong() ?: 0,
                sessionId = (json["sessionId"] as? Number)?.toLong() ?: 0,
                role = json["role"] as? String ?: ROLE_USER,
                text = json["text"] as? String ?: "",
                reasoning = json["reasoning"] as? String,
                durationMs = (json["durationMs"] as? Number)?.toLong(),
                timestamp = (json["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                tokenCount = (json["tokenCount"] as? Number)?.toInt(),
                isEdited = json["isEdited"] as? Boolean ?: false,
                isBookmarked = json["isBookmarked"] as? Boolean ?: false,
                parentMessageId = (json["parentMessageId"] as? Number)?.toLong(),
                metadata = json["metadata"] as? String,
                attachedFilesJson = json["attachedFilesJson"] as? String,
                modelUsed = json["modelUsed"] as? String,
                personalityUsed = json["personalityUsed"] as? String,
                generationConfigJson = json["generationConfigJson"] as? String
            )
        }
    }

    fun toJson(): Map<String, Any?> = mapOf(
        "id" to id,
        "sessionId" to sessionId,
        "role" to role,
        "text" to text,
        "reasoning" to reasoning,
        "durationMs" to durationMs,
        "timestamp" to timestamp,
        "tokenCount" to tokenCount,
        "isEdited" to isEdited,
        "isBookmarked" to isBookmarked,
        "parentMessageId" to parentMessageId,
        "metadata" to metadata,
        "attachedFilesJson" to attachedFilesJson,
        "modelUsed" to modelUsed,
        "personalityUsed" to personalityUsed,
        "generationConfigJson" to generationConfigJson
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessage) return false
        return id == other.id && sessionId == other.sessionId && role == other.role &&
                text == other.text && timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append("ChatMessage(id=$id, role='$role', sessionId=$sessionId, ")
            append("text='$preview', ")
            if (hasReasoning) append("reasoning=true, ")
            if (hasAttachments) append("attachments=true, ")
            if (durationMs != null) append("duration=$formattedDuration, ")
            if (isBookmarked) append("bookmarked=true, ")
            if (modelUsed != null) append("model=$modelUsed, ")
            append("timestamp=$timestamp)")
        }
    }
}
