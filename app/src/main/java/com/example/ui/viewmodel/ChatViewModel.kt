package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeSessionId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessage>> = activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isGenerating = MutableStateFlow(false)
    val isDeepThinkingEnabled = MutableStateFlow(true) // Default to true (DeepSeek's iconic mode!)
    val errorMessage = MutableStateFlow<String?>(null)
    val isListening = MutableStateFlow(false)

    // Selects or switches active session
    fun selectSession(sessionId: Long?) {
        activeSessionId.value = sessionId
        errorMessage.value = null
    }

    // Starts a clean new chat session
    fun startNewChat() {
        activeSessionId.value = null
        errorMessage.value = null
    }

    // Renames session
    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    // Deletes session
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (activeSessionId.value == sessionId) {
                activeSessionId.value = null
            }
        }
    }

    // Clears all conversations
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            activeSessionId.value = null
        }
    }

    // Sends user prompt and handles Gemini deep reasoning response
    fun sendMessage(text: String) {
        if (text.isBlank() || isGenerating.value) return

        val currentSessionId = activeSessionId.value

        viewModelScope.launch {
            // 1. Create a session if none is active
            val targetSessionId = if (currentSessionId == null) {
                val rawTitle = if (text.length > 24) text.take(24) + "..." else text
                val newId = repository.createSession(rawTitle)
                activeSessionId.value = newId
                newId
            } else {
                currentSessionId
            }

            // 2. Save user message to database
            val userMsg = ChatMessage(
                sessionId = targetSessionId,
                role = "user",
                text = text
            )
            repository.saveMessage(userMsg)

            isGenerating.value = true
            errorMessage.value = null

            // 3. Fetch session history to send as context (supports multi-turn!)
            val history = repository.getMessagesForSession(targetSessionId).first()

            try {
                val startTime = System.currentTimeMillis()

                // Define system instructions based on deep thinking toggle
                val systemInstruction = if (isDeepThinkingEnabled.value) {
                    Content(
                        parts = listOf(
                            Part(
                                text = "You are Troc, a highly advanced AI reasoning assistant modeled on Yin and Yang dual balance. " +
                                       "CRITICAL RULE: You MUST start your response by detailing your inner thoughts, step-by-step reasoning process, " +
                                       "logical debates, and breakdown inside <thought>...</thought> tags, followed IMMEDIATELY by your final, polished, " +
                                       "direct response. Do not mention or explain these tags. " +
                                       "Example of output format:\n" +
                                       "<thought>Evaluating user request... checking algorithms... balancing light/dark factors...</thought>\n" +
                                       "Here is the balanced solution:\n\n..."
                            )
                        )
                    )
                } else {
                    Content(
                        parts = listOf(
                            Part(
                                text = "You are Troc, a balanced, highly capable AI assistant styled in Milky White and Shadow Black. " +
                                       "Give exceptionally polished, precise, and direct answers without thinking process tags."
                            )
                        )
                    )
                }

                // Map database messages to Gemini contents (roles: user -> "user", model -> "model")
                val contents = history.map { msg ->
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }

                val apiKey = BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = systemInstruction
                )

                // Call the Gemini Service
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val fullResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No reply was generated. The system remains in perfect silence."

                val duration = System.currentTimeMillis() - startTime

                // Parse the reasoning process
                var extractedReasoning: String? = null
                var finalAnswerText = fullResponseText

                val thoughtRegex = Regex("<thought>(.*?)</thought>", RegexOption.DOT_MATCHES_ALL)
                val match = thoughtRegex.find(fullResponseText)
                if (match != null) {
                    extractedReasoning = match.groupValues[1].trim()
                    finalAnswerText = fullResponseText.replace(match.value, "").trim()
                }

                // 4. Save model message
                val modelMsg = ChatMessage(
                    sessionId = targetSessionId,
                    role = "model",
                    text = finalAnswerText,
                    reasoning = extractedReasoning,
                    durationMs = if (isDeepThinkingEnabled.value) duration else null
                )
                repository.saveMessage(modelMsg)

                // Auto-update title of session if it is still using the default prompt
                if (history.size <= 2) {
                    val smartTitle = if (text.length > 25) text.take(22) + "..." else text
                    repository.updateSessionTitle(targetSessionId, smartTitle)
                }

            } catch (e: Exception) {
                errorMessage.value = "Failed to balance thoughts: ${e.localizedMessage ?: "Network disruption"}"
            } finally {
                isGenerating.value = false
            }
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
