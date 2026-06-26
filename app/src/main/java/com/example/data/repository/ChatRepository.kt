package com.example.data.repository

import com.example.data.database.ChatDao
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessionsFlow()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSessionFlow(sessionId)
    }

    suspend fun createSession(title: String): Long {
        val session = ChatSession(title = title)
        return chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(id: Long, title: String) {
        chatDao.updateSessionTitle(id, title)
    }

    suspend fun deleteSession(id: Long) {
        chatDao.deleteSessionById(id)
    }

    suspend fun clearAll() {
        chatDao.deleteAllSessions()
    }

    suspend fun saveMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message)
    }
}
