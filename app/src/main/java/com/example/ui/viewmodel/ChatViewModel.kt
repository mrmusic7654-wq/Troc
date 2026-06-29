// app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt
package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.continuity.ChatContinuityManager
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.data.filemanager.AttachedFile
import com.example.data.filemanager.FileUploadManager
import com.example.data.personality.PersonalityProfile
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
            if (sessionId != null) repository.getMessagesForSession(sessionId)
            else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isGenerating = MutableStateFlow(false)
    val isDeepThinkingEnabled = MutableStateFlow(true)
    val errorMessage = MutableStateFlow<String?>(null)
    val isListening = MutableStateFlow(false)
    val currentThinkingProcess = MutableStateFlow<String?>(null)
    val generationProgress = MutableStateFlow(0f)

    // Model & Personality
    val selectedModel = MutableStateFlow(GeminiModel.default)
    val selectedPersonality = MutableStateFlow(PersonalityProfile.DEFAULT)

    // Context window tracking
    val contextTokenCount = MutableStateFlow(0)
    val contextUsageFraction = MutableStateFlow(0f)
    val isContextNearLimit = MutableStateFlow(false)

    // Auto-save
    val autoSaveEnabled = MutableStateFlow(true)
    val lastSaveTimestamp = MutableStateFlow(System.currentTimeMillis())

    fun selectSession(sessionId: Long?) {
        activeSessionId.value = sessionId
        errorMessage.value = null
        currentThinkingProcess.value = null
        generationProgress.value = 0f
        updateContextStats()
    }

    fun startNewChat() {
        activeSessionId.value = null
        errorMessage.value = null
        currentThinkingProcess.value = null
        generationProgress.value = 0f
        FileUploadManager.clearAll()
        contextTokenCount.value = 0
        contextUsageFraction.value = 0f
        isContextNearLimit.value = false
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            ChatContinuityManager.clearSession(sessionId)
            if (activeSessionId.value == sessionId) {
                activeSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            ChatContinuityManager.clearAll()
            activeSessionId.value = null
            FileUploadManager.clearAll()
        }
    }

    fun setModel(model: GeminiModel) {
        selectedModel.value = model
    }

    fun setPersonality(personality: PersonalityProfile) {
        selectedPersonality.value = personality
    }

    fun toggleAutoSave() {
        autoSaveEnabled.value = !autoSaveEnabled.value
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isGenerating.value) return

        val currentSessionId = activeSessionId.value

        viewModelScope.launch {
            val targetSessionId = if (currentSessionId == null) {
                val rawTitle = if (text.length > 32) text.take(29) + "…" else text
                val newId = repository.createSession(rawTitle)
                activeSessionId.value = newId
                newId
            } else {
                currentSessionId
            }

            val userMsg = ChatMessage(
                sessionId = targetSessionId,
                role = "user",
                text = text
            )
            repository.saveMessage(userMsg)

            isGenerating.value = true
            errorMessage.value = null
            currentThinkingProcess.value = null
            generationProgress.value = 0.1f

            val history = repository.getSessionMessages(targetSessionId)

            try {
                val startTime = System.currentTimeMillis()
                val model = selectedModel.value
                val personality = selectedPersonality.value

                // Build system instruction from personality
                val systemInstruction = Content(
                    parts = listOf(Part(text = personality.toCustomPrompt()))
                )

                // Map messages to Gemini contents
                val contents = history.map { msg ->
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }

                val apiKey = BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = systemInstruction,
                    generationConfig = GenerationConfig(
                        temperature = personality.temperature,
                        topP = personality.topP,
                        topK = model.defaultTopK,
                        maxOutputTokens = personality.maxTokens
                    )
                )

                generationProgress.value = 0.3f

                // Track usage
                ModelUsageTracker.recordRequest(model.modelId)

                val response = RetrofitClient.service.generateContent(
                    apiKey,
                    model.modelId,
                    request
                )

                generationProgress.value = 0.7f

                val fullResponseText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text ?: "The void returns only silence. Balance is maintained."

                val duration = System.currentTimeMillis() - startTime

                var extractedReasoning: String? = null
                var finalAnswerText = fullResponseText

                val thoughtRegex = Regex("<thought>(.*?)</thought>", RegexOption.DOT_MATCHES_ALL)
                val match = thoughtRegex.find(fullResponseText)
                if (match != null) {
                    extractedReasoning = match.groupValues[1].trim()
                    currentThinkingProcess.value = extractedReasoning
                    finalAnswerText = fullResponseText
                        .replace(match.value, "")
                        .trim()
                        .removePrefix("\n")
                        .trimStart()
                }

                generationProgress.value = 0.9f

                val modelMsg = ChatMessage(
                    sessionId = targetSessionId,
                    role = "model",
                    text = finalAnswerText,
                    reasoning = extractedReasoning,
                    durationMs = if (isDeepThinkingEnabled.value) duration else null
                )
                repository.saveMessage(modelMsg)

                generationProgress.value = 1f

                if (history.size <= 2) {
                    val smartTitle = generateSmartTitle(text, finalAnswerText)
                    repository.updateSessionTitle(targetSessionId, smartTitle)
                }

                // Update context stats
                updateContextStats()

                // Auto-save continuity state
                if (autoSaveEnabled.value) {
                    val updatedHistory = repository.getSessionMessages(targetSessionId)
                    ChatContinuityManager.createSessionSnapshot(
                        sessionId = targetSessionId,
                        title = smartTitle ?: "Session $targetSessionId",
                        messages = updatedHistory,
                        contextTokens = ChatContinuityManager.estimateTokenCount(updatedHistory)
                    )
                    lastSaveTimestamp.value = System.currentTimeMillis()
                }

                // Clear attachments after successful send
                FileUploadManager.clearAll()

            } catch (e: Exception) {
                val errorMsg = when {
                    e is java.net.UnknownHostException -> "Network unreachable. Check your connection."
                    e.message?.contains("401") == true -> "Invalid API key. Please update in Settings."
                    e.message?.contains("429") == true -> "Rate limited. Pause for balance."
                    e.message?.contains("403") == true -> "Access denied. Verify API key permissions."
                    else -> "Harmony disrupted: ${e.localizedMessage ?: "Unknown error"}"
                }
                errorMessage.value = errorMsg
            } finally {
                isGenerating.value = false
                generationProgress.value = 0f
            }
        }
    }

    fun retryLastMessage() {
        viewModelScope.launch {
            val messages = activeMessages.value
            if (messages.isEmpty()) return@launch

            val lastUserMessage = messages.findLast { it.role == "user" }
            if (lastUserMessage != null) {
                val lastModelMessage = messages.findLast { it.role == "model" }
                if (lastModelMessage != null) {
                    repository.deleteSession(activeSessionId.value ?: return@launch)
                    val newSessionId = repository.createSession(
                        messages.firstOrNull()?.text?.take(30) ?: "Retry"
                    )
                    activeSessionId.value = newSessionId
                    messages.filter { it.role == "user" || it.id < lastModelMessage.id }
                        .forEach { repository.saveMessage(it.copy(sessionId = newSessionId, id = 0)) }
                }
                sendMessage(lastUserMessage.text)
            }
        }
    }

    fun stopGeneration() {
        isGenerating.value = false
        errorMessage.value = "Generation halted. Balance preserved."
    }

    fun continueSession(sessionId: Long) {
        viewModelScope.launch {
            activeSessionId.value = sessionId
            errorMessage.value = null
            val snapshot = ChatContinuityManager.getContinuityState(sessionId)
            if (snapshot.contextWindow != null) {
                contextTokenCount.value = snapshot.contextWindow.totalTokens
                contextUsageFraction.value = snapshot.contextWindow.usageFraction
                isContextNearLimit.value = snapshot.contextWindow.usageFraction > 0.9f
            }
        }
    }

    fun getContextWindowSummary(): String {
        val messages = activeMessages.value
        if (messages.isEmpty()) return "Empty context"
        val tokens = ChatContinuityManager.estimateTokenCount(messages)
        val percentage = ChatContinuityManager.getContextPercentage(tokens)
        return "${messages.size} msgs • ${if (tokens >= 1000) "${tokens/1000}K" else "$tokens"} / 1M tokens (${(percentage * 100).toInt()}%)"
    }

    private fun updateContextStats() {
        viewModelScope.launch {
            val messages = activeMessages.value
            val tokens = ChatContinuityManager.estimateTokenCount(messages)
            contextTokenCount.value = tokens
            contextUsageFraction.value = ChatContinuityManager.getContextPercentage(tokens)
            isContextNearLimit.value = contextUsageFraction.value > 0.9f
        }
    }

    private var smartTitle: String? = null

    private fun generateSmartTitle(userPrompt: String, aiResponse: String): String {
        val cleanPrompt = userPrompt.take(40).trim()
        val firstLine = aiResponse.lines().firstOrNull { it.isNotBlank() }?.take(40)?.trim()
        val title = firstLine?.let { line ->
            if (line.length < cleanPrompt.length) line else cleanPrompt
        } ?: cleanPrompt
        smartTitle = title
        return title
    }

    companion object {
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 4096
        const val MAX_CONTEXT_TOKENS = 1_048_576
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
