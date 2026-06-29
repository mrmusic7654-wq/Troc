// app/src/main/java/com/example/data/continuity/ChatContinuityManager.kt
package com.example.data.continuity

import androidx.compose.runtime.mutableStateMapOf
import com.example.data.database.ChatMessage
import kotlinx.coroutines.flow.Flow

data class ContextWindow(
    val sessionId: Long,
    val messages: List<ChatMessage>,
    val totalTokens: Int = 0,
    val maxTokens: Int = 1_048_576,
    val isTrimmed: Boolean = false,
    val firstMessageId: Long? = null,
    val lastMessageId: Long? = null,
    val truncatedFromStart: Int = 0,
    val truncatedFromEnd: Int = 0
) {
    val usageFraction: Float
        get() = if (maxTokens > 0) totalTokens.toFloat() / maxTokens else 0f

    val remainingTokens: Int
        get() = (maxTokens - totalTokens).coerceAtLeast(0)

    val formattedUsage: String
        get() = when {
            totalTokens >= 1_000_000 -> "${"%.1f".format(totalTokens / 1_000_000.0)}M"
            totalTokens >= 1_000 -> "${totalTokens / 1_000}K"
            else -> "$totalTokens"
        }

    val formattedMax: String
        get() = "${maxTokens / 1_000_000}M"

    val summary: String
        get() = "${messages.size} messages • $formattedUsage / $formattedMax tokens • ${remainingTokens.coerceAtLeast(0).let { if (it >= 1000) "${it/1000}K" else "$it" }} remaining"
}

data class SessionSnapshot(
    val sessionId: Long,
    val title: String,
    val messageCount: Int,
    val lastMessagePreview: String,
    val lastActiveTimestamp: Long,
    val contextWindow: ContextWindow? = null
) {
    val formattedLastActive: String
        get() {
            val diff = System.currentTimeMillis() - lastActiveTimestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3_600_000 -> "${diff / 60_000}m ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                diff < 604_800_000 -> "${diff / 86_400_000}d ago"
                else -> "${diff / 604_800_000}w ago"
            }
        }
}

data class ContinuityState(
    val activeSessionId: Long? = null,
    val sessionSnapshots: List<SessionSnapshot> = emptyList(),
    val isRestoringSession: Boolean = false,
    val restoredMessageCount: Int = 0,
    val contextWindow: ContextWindow? = null,
    val lastSaveTimestamp: Long = System.currentTimeMillis(),
    val autoSaveEnabled: Boolean = true,
    val maxSavedSessions: Int = 50
)

object ChatContinuityManager {
    private val continuityStates = mutableStateMapOf<Long, ContinuityState>()

    fun getContinuityState(sessionId: Long): ContinuityState =
        continuityStates.getOrPut(sessionId) { ContinuityState(activeSessionId = sessionId) }

    fun updateContinuityState(sessionId: Long, update: (ContinuityState) -> ContinuityState) {
        continuityStates[sessionId] = update(getContinuityState(sessionId))
    }

    fun createSessionSnapshot(
        sessionId: Long,
        title: String,
        messages: List<ChatMessage>,
        contextTokens: Int = 0
    ): SessionSnapshot {
        val contextWindow = if (messages.isNotEmpty()) {
            ContextWindow(
                sessionId = sessionId,
                messages = messages,
                totalTokens = contextTokens.coerceAtLeast(messages.sumOf { it.text.length / 4 }),
                firstMessageId = messages.firstOrNull()?.id,
                lastMessageId = messages.lastOrNull()?.id
            )
        } else null

        return SessionSnapshot(
            sessionId = sessionId,
            title = title,
            messageCount = messages.size,
            lastMessagePreview = messages.lastOrNull()?.preview ?: "",
            lastActiveTimestamp = System.currentTimeMillis(),
            contextWindow = contextWindow
        )
    }

    fun estimateTokenCount(messages: List<ChatMessage>): Int {
        var totalTokens = 0
        for (message in messages) {
            // Rough estimation: 1 token ≈ 4 characters for English text
            totalTokens += message.text.length / 4
            message.reasoning?.let { totalTokens += it.length / 4 }
            // Add overhead per message
            totalTokens += 8
        }
        return totalTokens
    }

    fun trimContextWindow(
        messages: List<ChatMessage>,
        maxTokens: Int = 1_048_576,
        preserveLastN: Int = 20
    ): List<ChatMessage> {
        if (messages.isEmpty()) return messages

        val totalTokens = estimateTokenCount(messages)
        if (totalTokens <= maxTokens) return messages

        // Always preserve system messages and the last N messages
        val systemMessages = messages.filter { it.role == "system" }
        val recentMessages = messages.takeLast(preserveLastN)
        val olderMessages = messages.dropLast(preserveLastN).filter { it.role != "system" }

        val recentTokens = estimateTokenCount(systemMessages + recentMessages)
        val availableForOlder = maxTokens - recentTokens

        if (availableForOlder <= 0) {
            return systemMessages + recentMessages.takeLast(
                (maxTokens.toDouble() / (recentTokens.toDouble() / recentMessages.size)).toInt().coerceAtLeast(1)
            )
        }

        val trimmedOlder = mutableListOf<ChatMessage>()
        var usedTokens = 0
        for (message in olderMessages.reversed()) {
            val msgTokens = message.text.length / 4 + 8
            if (usedTokens + msgTokens <= availableForOlder) {
                trimmedOlder.add(0, message)
                usedTokens += msgTokens
            } else break
        }

        return systemMessages + trimmedOlder + recentMessages
    }

    fun getSessionPreviewFlow(
        sessionId: Long,
        allMessages: Flow<List<ChatMessage>>
    ): Flow<SessionSnapshot> {
        return kotlinx.coroutines.flow.flow {
            allMessages.collect { messages ->
                val snapshot = createSessionSnapshot(
                    sessionId = sessionId,
                    title = "Session $sessionId",
                    messages = messages,
                    contextTokens = estimateTokenCount(messages)
                )
                emit(snapshot)
            }
        }
    }

    fun canContinueSession(snapshot: SessionSnapshot): Boolean {
        return snapshot.messageCount > 0 &&
                System.currentTimeMillis() - snapshot.lastActiveTimestamp < 30L * 24 * 60 * 60 * 1000
    }

    fun getContextPercentage(tokens: Int, maxTokens: Int = 1_048_576): Float =
        (tokens.toFloat() / maxTokens).coerceIn(0f, 1f)

    fun clearSession(sessionId: Long) {
        continuityStates.remove(sessionId)
    }

    fun clearAll() {
        continuityStates.clear()
    }
}
