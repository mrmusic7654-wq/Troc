// app/src/main/java/com/example/data/api/GeminiModels.kt
package com.example.data.api

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class GeminiModel(
    val modelId: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val category: ModelCategory,
    val contextWindow: Int = 1_048_576,
    val maxOutputTokens: Int = 65_535,
    val defaultTemperature: Float = 1.0f,
    val defaultTopP: Float = 0.95f,
    val defaultTopK: Int = 64,
    val supportsGrounding: Boolean = false,
    val supportsThinking: Boolean = false,
    val supportsVision: Boolean = true,
    val supportsAudio: Boolean = false,
    val supportsVideo: Boolean = false,
    val supportsCaching: Boolean = false,
    val rateLimitRpm: Int = 10,
    val rateLimitRpd: Int = 1500,
    val isPreview: Boolean = false,
    val isExperimental: Boolean = false
) {
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Latest speed-optimized model with thought preservation",
        icon = Icons.Rounded.FlashOn,
        category = ModelCategory.SPEED,
        supportsThinking = true,
        supportsCaching = true,
        rateLimitRpm = 10,
        rateLimitRpd = 1500
    ),
    GEMINI_3_5_LIVE_TRANSLATE(
        modelId = "gemini-3.5-live-translate-preview",
        displayName = "Gemini 3.5 Live Translate",
        description = "Bidirectional speech-to-speech translation across 70+ languages",
        icon = Icons.Rounded.Translate,
        category = ModelCategory.AUDIO,
        supportsAudio = true,
        isPreview = true,
        rateLimitRpm = 5,
        rateLimitRpd = 100
    ),
    GEMINI_3_1_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash-Lite",
        description = "Cost-optimized for high-throughput automation loops",
        icon = Icons.Rounded.Speed,
        category = ModelCategory.COST,
        rateLimitRpm = 30,
        rateLimitRpd = 1500
    ),
    GEMINI_3_1_FLASH_LIVE(
        modelId = "gemini-3.1-flash-live-preview",
        displayName = "Gemini 3.1 Flash Live",
        description = "Real-time audio-to-audio streaming via Live API",
        icon = Icons.Rounded.Mic,
        category = ModelCategory.AUDIO,
        supportsAudio = true,
        supportsGrounding = true,
        isPreview = true,
        rateLimitRpm = 5,
        rateLimitRpd = 100
    ),
    GEMINI_3_1_FLASH_TTS(
        modelId = "gemini-3.1-flash-tts-preview",
        displayName = "Gemini 3.1 Flash TTS",
        description = "Text-to-speech with expressive inline tags like [whispers]",
        icon = Icons.Rounded.RecordVoiceOver,
        category = ModelCategory.AUDIO,
        supportsAudio = true,
        isPreview = true,
        contextWindow = 32_768,
        rateLimitRpm = 5,
        rateLimitRpd = 100
    ),
    GEMINI_2_5_PRO(
        modelId = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        description = "Deep reasoning engine — 5 RPM limit, maximum intelligence",
        icon = Icons.Rounded.Psychology,
        category = ModelCategory.REASONING,
        supportsThinking = true,
        supportsCaching = true,
        rateLimitRpm = 5,
        rateLimitRpd = 50
    ),
    GEMINI_2_5_FLASH(
        modelId = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        description = "Balanced model with Google Search & Maps grounding",
        icon = Icons.Rounded.Search,
        category = ModelCategory.BALANCED,
        supportsGrounding = true,
        supportsThinking = true,
        rateLimitRpm = 10,
        rateLimitRpd = 500
    ),
    GEMINI_2_5_FLASH_LITE(
        modelId = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash-Lite",
        description = "High-volume variant with shared 500 RPD search pool",
        icon = Icons.Rounded.TrendingUp,
        category = ModelCategory.COST,
        supportsGrounding = true,
        rateLimitRpm = 30,
        rateLimitRpd = 500
    ),
    GEMINI_2_5_FLASH_TTS(
        modelId = "gemini-2.5-flash-preview-tts",
        displayName = "Gemini 2.5 Flash TTS",
        description = "Legacy text-to-speech generation model",
        icon = Icons.Rounded.VoiceChat,
        category = ModelCategory.AUDIO,
        supportsAudio = true,
        isPreview = true,
        contextWindow = 32_768,
        rateLimitRpm = 5,
        rateLimitRpd = 100
    ),
    GEMINI_EMBEDDING_2(
        modelId = "gemini-embedding-2",
        displayName = "Gemini Embedding 2",
        description = "Multimodal vector embeddings (128 to 3,072 dimensions)",
        icon = Icons.Rounded.Grain,
        category = ModelCategory.EMBEDDING,
        supportsVision = true,
        supportsAudio = true,
        supportsVideo = true,
        contextWindow = 8_192,
        maxOutputTokens = 0,
        rateLimitRpm = 1500,
        rateLimitRpd = 1500
    ),
    GEMINI_EMBEDDING_001(
        modelId = "gemini-embedding-001",
        displayName = "Gemini Embedding 001",
        description = "Legacy text-only vector mapping",
        icon = Icons.Rounded.TextFields,
        category = ModelCategory.EMBEDDING,
        supportsVision = false,
        contextWindow = 2_048,
        maxOutputTokens = 0,
        rateLimitRpm = 1500,
        rateLimitRpd = 1500
    ),
    GEMINI_ROBOTICS(
        modelId = "gemini-robotics-er-1.6-preview",
        displayName = "Gemini Robotics ER 1.6",
        description = "Embodied reasoning for hardware automation workflows",
        icon = Icons.Rounded.PrecisionManufacturing,
        category = ModelCategory.SPECIALIZED,
        isPreview = true,
        isExperimental = true,
        rateLimitRpm = 5,
        rateLimitRpd = 50
    ),
    GEMMA_4(
        modelId = "gemma-4",
        displayName = "Gemma 4",
        description = "Open weights model — completely free core infrastructure",
        icon = Icons.Rounded.LockOpen,
        category = ModelCategory.OPEN,
        rateLimitRpm = 10,
        rateLimitRpd = 1500
    );

    val contextWindowFormatted: String
        get() = when {
            contextWindow >= 1_000_000 -> "${contextWindow / 1_000_000}M"
            contextWindow >= 1_000 -> "${contextWindow / 1_000}K"
            else -> contextWindow.toString()
        }

    val rateLimitSummary: String
        get() = "$rateLimitRpm RPM / $rateLimitRpd RPD"

    val isFreeTier: Boolean = true

    val supportedFileFormats: List<String>
        get() = buildList {
            add("text/plain")
            add("application/pdf")
            if (supportsVision) {
                add("image/png"); add("image/jpeg"); add("image/webp")
                add("image/heic"); add("image/heif")
            }
            if (supportsAudio) {
                add("audio/mp3"); add("audio/wav")
                add("audio/ogg"); add("audio/flac")
            }
            if (supportsVideo) {
                add("video/mp4"); add("video/mpeg")
                add("video/quicktime"); add("video/avi")
            }
        }

    companion object {
        fun fromModelId(modelId: String): GeminiModel =
            entries.find { it.modelId == modelId } ?: GEMINI_2_5_FLASH

        val default: GeminiModel = GEMINI_2_5_FLASH
        val chatModels: List<GeminiModel> = entries.filter {
            it.category in listOf(ModelCategory.SPEED, ModelCategory.BALANCED, ModelCategory.REASONING, ModelCategory.COST, ModelCategory.OPEN)
        }
        val audioModels: List<GeminiModel> = entries.filter { it.category == ModelCategory.AUDIO }
        val embeddingModels: List<GeminiModel> = entries.filter { it.category == ModelCategory.EMBEDDING }
        val groundingModels: List<GeminiModel> = entries.filter { it.supportsGrounding }
    }
}

enum class ModelCategory(
    val label: String,
    val description: String,
    val color: Long
) {
    SPEED("Speed", "Optimized for fast responses", 0xFF4CAF50),
    BALANCED("Balanced", "Best overall performance", 0xFFCBB28D),
    REASONING("Reasoning", "Deep analytical thinking", 0xFF82B1FF),
    COST("Cost Efficient", "High throughput, low cost", 0xFFFFB74D),
    AUDIO("Audio", "Speech and audio processing", 0xFFFF80AB),
    EMBEDDING("Embedding", "Vector representations", 0xFFCE93D8),
    SPECIALIZED("Specialized", "Domain-specific models", 0xFFB9F6CA),
    OPEN("Open", "Open weights models", 0xFF80CBC4)
}

data class ModelUsageStats(
    val modelId: String,
    val requestsThisMinute: Int = 0,
    val requestsToday: Int = 0,
    val rpmLimit: Int = 10,
    val rpdLimit: Int = 1500,
    val lastRequestTimestamp: Long = 0L,
    val lastResetTimestamp: Long = System.currentTimeMillis(),
    val sessionTotal: Int = 0
) {
    val remainingRpm: Int get() = (rpmLimit - requestsThisMinute).coerceAtLeast(0)
    val remainingRpd: Int get() = (rpdLimit - requestsToday).coerceAtLeast(0)
    val rpmUsageFraction: Float get() = if (rpmLimit > 0) requestsThisMinute.toFloat() / rpmLimit else 0f
    val rpdUsageFraction: Float get() = if (rpdLimit > 0) requestsToday.toFloat() / rpdLimit else 0f
    val isRpmExhausted: Boolean get() = requestsThisMinute >= rpmLimit
    val isRpdExhausted: Boolean get() = requestsToday >= rpdLimit

    val nextResetMillis: Long
        get() {
            val now = System.currentTimeMillis()
            val minuteReset = lastResetTimestamp + 60_000
            val dayReset = lastResetTimestamp + 86_400_000
            return if (now >= minuteReset) {
                if (now >= dayReset) now + 86_400_000 else now + 60_000
            } else minuteReset
        }

    val nextResetFormatted: String
        get() {
            val remaining = nextResetMillis - System.currentTimeMillis()
            if (remaining <= 0) return "Resetting..."
            val minutes = (remaining / 60_000).toInt()
            val seconds = ((remaining % 60_000) / 1000).toInt()
            return when {
                minutes > 60 -> "${minutes / 60}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
}

object ModelUsageTracker {
    private val statsMap = mutableStateMapOf<String, ModelUsageStats>()

    fun getStats(modelId: String): ModelUsageStats {
        val model = GeminiModel.fromModelId(modelId)
        return statsMap.getOrPut(modelId) {
            ModelUsageStats(
                modelId = modelId,
                rpmLimit = model.rateLimitRpm,
                rpdLimit = model.rateLimitRpd
            )
        }
    }

    fun recordRequest(modelId: String) {
        val now = System.currentTimeMillis()
        val current = getStats(modelId)
        val minuteElapsed = now - current.lastResetTimestamp >= 60_000
        val dayElapsed = now - current.lastResetTimestamp >= 86_400_000

        statsMap[modelId] = current.copy(
            requestsThisMinute = if (minuteElapsed) 1 else current.requestsThisMinute + 1,
            requestsToday = if (minuteElapsed || dayElapsed) {
                if (dayElapsed) 1 else current.requestsToday + 1
            } else current.requestsToday + 1,
            lastRequestTimestamp = now,
            lastResetTimestamp = if (minuteElapsed || dayElapsed) now else current.lastResetTimestamp,
            sessionTotal = current.sessionTotal + 1
        )
    }

    fun resetAll() {
        statsMap.clear()
    }

    fun getAllStats(): Map<String, ModelUsageStats> = statsMap.toMap()
}
