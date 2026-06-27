// app/src/main/java/com/example/data/database/ChatDao.kt
package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // ═══════════════════════════════════════
    // Session Operations
    // ═══════════════════════════════════════

    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    suspend fun getAllSessions(): List<ChatSession>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ChatSession?

    @Query("SELECT * FROM chat_sessions WHERE title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchSessions(query: String): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ChatSession>): List<Long>

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :id")
    suspend fun updateSessionTitle(id: Long, title: String)

    @Query("UPDATE chat_sessions SET title = :title, createdAt = :updatedAt WHERE id = :id")
    suspend fun updateSession(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun getSessionCount(): Int

    @Query("SELECT COUNT(*) FROM chat_sessions WHERE createdAt > :since")
    suspend fun getRecentSessionCount(since: Long): Int

    // ═══════════════════════════════════════
    // Message Operations
    // ═══════════════════════════════════════

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastMessages(sessionId: Long, limit: Int = 50): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): ChatMessage?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND role = :role ORDER BY timestamp ASC")
    fun getMessagesByRole(sessionId: Long, role: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>): List<Long>

    @Query("UPDATE chat_messages SET text = :text WHERE id = :id")
    suspend fun updateMessageText(id: Long, text: String)

    @Query("UPDATE chat_messages SET reasoning = :reasoning WHERE id = :id")
    suspend fun updateMessageReasoning(id: Long, reasoning: String?)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND id > :messageId")
    suspend fun deleteMessagesAfter(sessionId: Long, messageId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCountForSession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getTotalMessageCount(): Int

    @Query("SELECT role, COUNT(*) as count FROM chat_messages WHERE sessionId = :sessionId GROUP BY role")
    suspend fun getMessageRoleCounts(sessionId: Long): List<RoleCount>

    // ═══════════════════════════════════════
    // Bulk & Transactional Operations
    // ═══════════════════════════════════════

    @Transaction
    suspend fun duplicateSession(sessionId: Long): Long? {
        val originalSession = getSessionById(sessionId) ?: return null
        val newSessionId = insertSession(
            originalSession.copy(
                id = 0,
                title = "${originalSession.title} (Copy)",
                createdAt = System.currentTimeMillis()
            )
        )
        val messages = getMessagesForSession(sessionId)
        insertMessages(messages.map { it.copy(id = 0, sessionId = newSessionId) })
        return newSessionId
    }

    @Transaction
    suspend fun mergeSessions(sourceId: Long, targetId: Long) {
        val sourceMessages = getMessagesForSession(sourceId)
        insertMessages(sourceMessages.map { it.copy(id = 0, sessionId = targetId) })
        deleteSessionById(sourceId)
    }

    @Transaction
    suspend fun cleanupOrphanMessages() {
        val allSessionIds = getAllSessions().map { it.id }.toSet()
        deleteMessagesNotInSessions(allSessionIds)
    }

    @Query("DELETE FROM chat_messages WHERE sessionId NOT IN (:sessionIds)")
    suspend fun deleteMessagesNotInSessions(sessionIds: Set<Long>)

    @Query("SELECT * FROM chat_messages WHERE sessionId IN (SELECT id FROM chat_sessions ORDER BY createdAt DESC LIMIT :limit) ORDER BY timestamp DESC")
    fun getRecentMessagesAcrossSessions(limit: Int = 5): Flow<List<ChatMessage>>

    @Query("""
        SELECT DISTINCT cm.sessionId 
        FROM chat_messages cm 
        WHERE cm.text LIKE '%' || :query || '%' 
        OR cm.reasoning LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchMessagesAcrossSessions(query: String, limit: Int = 10): List<Long>

    @Transaction
    suspend fun exportSessionToJson(sessionId: Long): String {
        val session = getSessionById(sessionId)
        val messages = getMessagesForSession(sessionId)
        return buildString {
            appendLine("{")
            appendLine("  \"session\": {")
            appendLine("    \"id\": ${session?.id},")
            appendLine("    \"title\": \"${session?.title}\",")
            appendLine("    \"createdAt\": ${session?.createdAt}")
            appendLine("  },")
            appendLine("  \"messages\": [")
            messages.forEachIndexed { index, msg ->
                appendLine("    {")
                appendLine("      \"id\": ${msg.id},")
                appendLine("      \"role\": \"${msg.role}\",")
                appendLine("      \"text\": \"${msg.text.replace("\"", "\\\"")}\",")
                appendLine("      \"reasoning\": \"${msg.reasoning?.replace("\"", "\\\"")}\",")
                appendLine("      \"durationMs\": ${msg.durationMs},")
                appendLine("      \"timestamp\": ${msg.timestamp}")
                appendLine("    }${if (index < messages.lastIndex) "," else ""}")
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    // ═══════════════════════════════════════
    // Statistics & Analytics
    // ═══════════════════════════════════════

    @Query("SELECT AVG(durationMs) FROM chat_messages WHERE durationMs IS NOT NULL AND role = 'model'")
    suspend fun getAverageResponseTime(): Double?

    @Query("SELECT AVG(LENGTH(text)) FROM chat_messages WHERE role = 'user'")
    suspend fun getAveragePromptLength(): Double?

    @Query("SELECT AVG(LENGTH(text)) FROM chat_messages WHERE role = 'model'")
    suspend fun getAverageResponseLength(): Double?

    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch')) as date, 
               COUNT(*) as count 
        FROM chat_messages 
        GROUP BY date 
        ORDER BY date DESC 
        LIMIT :days
    """)
    suspend fun getMessageCountByDay(days: Int = 30): List<DailyCount>

    @Query("""
        SELECT strftime('%H', datetime(timestamp / 1000, 'unixepoch')) as hour,
               COUNT(*) as count
        FROM chat_messages
        WHERE timestamp > :since
        GROUP BY hour
        ORDER BY hour ASC
    """)
    suspend fun getMessageCountByHour(since: Long): List<HourlyCount>
}

// ═══════════════════════════════════════
// Data Classes for Query Results
// ═══════════════════════════════════════

data class RoleCount(
    val role: String,
    val count: Int
)

data class DailyCount(
    val date: String,
    val count: Int
)

data class HourlyCount(
    val hour: String,
    val count: Int
)
