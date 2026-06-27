// app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt
package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.*
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
    val selectedModel = MutableStateFlow("gemini-2.0-flash")
    val temperature = MutableStateFlow(0.7f)
    val maxOutputTokens = MutableStateFlow(4096)

    private val _availableModels = listOf(
        "gemini-2.0-flash" to "Fast & Responsive",
        "gemini-2.0-pro" to "Deep Reasoning",
        "gemini-1.5-pro" to "Balanced Legacy"
    )
    val availableModels: List<Pair<String, String>> = _availableModels

    fun selectSession(sessionId: Long?) {
        activeSessionId.value = sessionId
        errorMessage.value = null
        currentThinkingProcess.value = null
        generationProgress.value = 0f
    }

    fun startNewChat() {
        activeSessionId.value = null
        errorMessage.value = null
        currentThinkingProcess.value = null
        generationProgress.value = 0f
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (activeSessionId.value == sessionId) {
                activeSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            activeSessionId.value = null
            currentThinkingProcess.value = null
        }
    }

    fun setModel(model: String) {
        selectedModel.value = model
    }

    fun setTemperature(temp: Float) {
        temperature.value = temp.coerceIn(0f, 2f)
    }

    fun setMaxTokens(tokens: Int) {
        maxOutputTokens.value = tokens.coerceIn(256, 8192)
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

                val systemInstruction = buildSystemInstruction()

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
                        temperature = temperature.value,
                        topP = 0.95f,
                        topK = 40,
                        maxOutputTokens = maxOutputTokens.value
                    )
                )

                generationProgress.value = 0.3f

                val response = RetrofitClient.service.generateContent(
                    apiKey,
                    selectedModel.value,
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

                val thoughtRegex = Regex(
                    "<thought>(.*?)</thought>",
                    RegexOption.DOT_MATCHES_ALL
                )
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

    private fun buildSystemInstruction(): Content {
        val basePrompt = if (isDeepThinkingEnabled.value) {
            """
            You are Troc Agent — a master-level AI reasoning system embodying Yin-Yang dual balance.

            ═══ CORE DIRECTIVES ═══
            1. DEEP THINKING PROTOCOL:
               - Begin EVERY response inside <thought>...</thought> tags
               - Detail your step-by-step reasoning, analysis, and internal debates
               - Consider multiple perspectives before converging on the optimal answer
               - Show logical chains, trade-offs evaluated, and why alternatives were rejected

            2. RESPONSE STRUCTURE:
               <thought>
               [Your complete reasoning process here]
               </thought>
               
               [Your polished, direct, comprehensive final answer here]

            3. QUALITY STANDARDS:
               - Be thorough yet concise in final output
               - Use markdown formatting for clarity
               - Provide code examples when relevant
               - Cite reasoning when giving technical answers
               - Balance creativity with accuracy

            4. TONE:
               - Professional yet warm
               - Confident yet humble
               - Precise yet flowing
               - Like a wise mentor in perfect balance
            """.trimIndent()
        } else {
            """
            You are Troc Agent — a highly capable AI assistant.
            Provide exceptionally polished, precise, and direct answers.
            Be thorough, accurate, and helpful in every response.
            Use markdown formatting for clarity and structure.
            """.trimIndent()
        }

        return Content(
            parts = listOf(Part(text = basePrompt))
        )
    }

    private fun generateSmartTitle(userPrompt: String, aiResponse: String): String {
        val cleanPrompt = userPrompt.take(40).trim()
        val firstLine = aiResponse.lines().firstOrNull { it.isNotBlank() }?.take(40)?.trim()
        return firstLine?.let { line ->
            if (line.length < cleanPrompt.length) line else cleanPrompt
        } ?: cleanPrompt
    }

    fun getSessionPreview(sessionId: Long): String {
        return try {
            viewModelScope.launch {
                repository.getSessionMessages(sessionId)
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 4096
        const val THINKING_MODEL = "gemini-2.0-pro"
        const val FAST_MODEL = "gemini-2.0-flash"
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
