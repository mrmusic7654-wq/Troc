// app/src/main/java/com/example/data/repository/ChatRepository.kt
package com.example.data.repository

import com.example.data.continuity.ChatContinuityManager
import com.example.data.database.ChatDao
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessionsFlow()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesForSessionFlow(sessionId)

    fun getActiveSessions(): Flow<List<ChatSession>> =
        chatDao.getActiveSessions()

    fun getPinnedSessions(): Flow<List<ChatSession>> =
        chatDao.getPinnedSessions()

    fun getArchivedSessions(): Flow<List<ChatSession>> =
        chatDao.getArchivedSessions()

    fun searchSessions(query: String): Flow<List<ChatSession>> =
        chatDao.searchSessions(query)

    suspend fun createSession(
        title: String,
        modelUsed: String = "",
        personalityId: String = "",
        personalityName: String = ""
    ): Long {
        val session = ChatSession.createNew(
            title = title,
            modelUsed = modelUsed,
            personalityId = personalityId,
            personalityName = personalityName
        )
        return chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(id: Long, title: String) {
        chatDao.updateSessionTitle(id, title)
    }

    suspend fun updateSessionModel(id: Long, modelId: String) {
        chatDao.updateSessionModel(id, modelId)
    }

    suspend fun updateSessionPersonality(id: Long, personalityId: String, personalityName: String) {
        chatDao.updateSessionPersonality(id, personalityId, personalityName)
    }

    suspend fun togglePinSession(id: Long, isPinned: Boolean) {
        chatDao.togglePinSession(id, isPinned)
    }

    suspend fun toggleArchiveSession(id: Long, isArchived: Boolean) {
        chatDao.toggleArchiveSession(id, isArchived)
    }

    suspend fun updateContextSnapshot(id: Long, snapshot: String?) {
        chatDao.updateContextSnapshot(id, snapshot)
    }

    suspend fun updateSessionColor(id: Long, color: String) {
        chatDao.updateSessionColor(id, color)
    }

    suspend fun deleteSession(id: Long) {
        chatDao.deleteSessionById(id)
        ChatContinuityManager.clearSession(id)
    }

    suspend fun clearAll() {
        chatDao.deleteAllSessions()
        ChatContinuityManager.clearAll()
    }

    suspend fun saveMessage(message: ChatMessage): Long =
        chatDao.insertMessage(message)

    suspend fun saveMessages(messages: List<ChatMessage>): List<Long> =
        chatDao.insertMessages(messages)

    suspend fun getSessionMessages(sessionId: Long): List<ChatMessage> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun getLastMessages(sessionId: Long, limit: Int = 50): List<ChatMessage> =
        chatDao.getLastMessages(sessionId, limit)

    suspend fun getMessageById(id: Long): ChatMessage? =
        chatDao.getMessageById(id)

    suspend fun updateMessageText(id: Long, text: String) {
        chatDao.updateMessageText(id, text)
    }

    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean) {
        chatDao.toggleBookmark(id, isBookmarked)
    }

    fun getBookmarkedMessages(): Flow<List<ChatMessage>> =
        chatDao.getBookmarkedMessages()

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessageById(id)
    }

    suspend fun deleteMessagesAfter(sessionId: Long, messageId: Long) {
        chatDao.deleteMessagesAfter(sessionId, messageId)
    }

    suspend fun getMessageCountForSession(sessionId: Long): Int =
        chatDao.getMessageCountForSession(sessionId)

    suspend fun getTotalMessageCount(): Int =
        chatDao.getTotalMessageCount()

    suspend fun getTotalCharacterCount(sessionId: Long): Long? =
        chatDao.getTotalCharacterCount(sessionId)

    suspend fun getMessageWindow(sessionId: Long, limit: Int, offset: Int): List<ChatMessage> =
        chatDao.getMessageWindow(sessionId, limit, offset)

    suspend fun duplicateSession(sessionId: Long): Long? =
        chatDao.duplicateSession(sessionId)

    suspend fun mergeSessions(sourceId: Long, targetId: Long) {
        chatDao.mergeSessions(sourceId, targetId)
    }

    suspend fun exportSessionToJson(sessionId: Long): String =
        chatDao.exportSessionToJson(sessionId)

    suspend fun searchMessagesAcrossSessions(query: String, limit: Int = 10): List<Long> =
        chatDao.searchMessagesAcrossSessions(query, limit)

    // Continuity helpers
    suspend fun createSessionWithContinuity(
        title: String,
        modelUsed: String = "",
        personalityId: String = "",
        personalityName: String = ""
    ): Long {
        val sessionId = createSession(title, modelUsed, personalityId, personalityName)
        ChatContinuityManager.getContinuityState(sessionId)
        return sessionId
    }

    suspend fun saveMessageWithContinuity(message: ChatMessage, sessionTitle: String) {
        val msgId = saveMessage(message)
        val messages = getSessionMessages(message.sessionId)
        val snapshot = ChatContinuityManager.createSessionSnapshot(
            sessionId = message.sessionId,
            title = sessionTitle,
            messages = messages,
            contextTokens = ChatContinuityManager.estimateTokenCount(messages)
        )
        ChatContinuityManager.updateContinuityState(message.sessionId) { state ->
            state.copy(
                sessionSnapshots = listOf(snapshot),
                lastSaveTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun getRecentMessagesAcrossSessions(limit: Int = 5): Flow<List<ChatMessage>> =
        chatDao.getRecentMessagesAcrossSessions(limit)

    suspend fun getAverageResponseTime(): Double? =
        chatDao.getAverageResponseTime()

    suspend fun getAveragePromptLength(): Double? =
        chatDao.getAveragePromptLength()

    suspend fun getAverageResponseLength(): Double? =
        chatDao.getAverageResponseLength()

    suspend fun getModelUsageStats(): List<com.example.data.database.ModelUsageCount> =
        chatDao.getModelUsageStats()

    suspend fun getPersonalityUsageStats(): List<com.example.data.database.PersonalityUsageCount> =
        chatDao.getPersonalityUsageStats()

    suspend fun getMessageCountByDay(days: Int = 30): List<com.example.data.database.DailyCount> =
        chatDao.getMessageCountByDay(days)
}
