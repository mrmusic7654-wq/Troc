// app/src/main/java/com/example/data/settings/ApiKeyConfig.kt
package com.example.data.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyConfig(
    @PrimaryKey 
    val serviceName: String,
    val apiKey: String = "",
    val isEnabled: Boolean = true,
    val lastValidated: Long? = null,
    val isValid: Boolean = false,
    val displayName: String = "",
    val maskedKey: String = ""
) {
    companion object {
        fun maskKey(key: String): String {
            if (key.length <= 8) return "••••••••"
            return "${key.take(4)}${"•".repeat(key.length - 8)}${key.takeLast(4)}"
        }
    }
}

enum class ApiService(
    val displayName: String,
    val iconRes: String,
    val docsUrl: String,
    val keyUrl: String,
    val keyPrefix: String = "",
    val validationEndpoint: String? = null,
    val category: ServiceCategory
) {
    GEMINI(
        displayName = "Google Gemini",
        iconRes = "gemini",
        docsUrl = "https://ai.google.dev/gemini-api/docs",
        keyUrl = "https://aistudio.google.com/apikey",
        keyPrefix = "AI",
        validationEndpoint = "v1beta/models/gemini-2.0-flash:generateContent",
        category = ServiceCategory.AI_MODEL
    ),
    OPENAI(
        displayName = "OpenAI",
        iconRes = "openai",
        docsUrl = "https://platform.openai.com/docs",
        keyUrl = "https://platform.openai.com/api-keys",
        keyPrefix = "sk-",
        validationEndpoint = "v1/models",
        category = ServiceCategory.AI_MODEL
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        iconRes = "anthropic",
        docsUrl = "https://docs.anthropic.com",
        keyUrl = "https://console.anthropic.com/keys",
        keyPrefix = "sk-ant-",
        validationEndpoint = "v1/messages",
        category = ServiceCategory.AI_MODEL
    ),
    HUGGING_FACE(
        displayName = "Hugging Face",
        iconRes = "huggingface",
        docsUrl = "https://huggingface.co/docs",
        keyUrl = "https://huggingface.co/settings/tokens",
        keyPrefix = "hf_",
        validationEndpoint = "api/whoami-v2",
        category = ServiceCategory.AI_PLATFORM
    ),
    REPLICATE(
        displayName = "Replicate",
        iconRes = "replicate",
        docsUrl = "https://replicate.com/docs",
        keyUrl = "https://replicate.com/account/api-tokens",
        keyPrefix = "r8_",
        validationEndpoint = "v1/models",
        category = ServiceCategory.AI_PLATFORM
    ),
    TELEGRAM(
        displayName = "Telegram Bot",
        iconRes = "telegram",
        docsUrl = "https://core.telegram.org/bots/api",
        keyUrl = "https://t.me/BotFather",
        keyPrefix = "",
        validationEndpoint = "bot{key}/getMe",
        category = ServiceCategory.MESSAGING
    ),
    GITHUB(
        displayName = "GitHub",
        iconRes = "github",
        docsUrl = "https://docs.github.com/en/rest",
        keyUrl = "https://github.com/settings/tokens",
        keyPrefix = "ghp_",
        validationEndpoint = "user",
        category = ServiceCategory.DEVELOPMENT
    ),
    GITLAB(
        displayName = "GitLab",
        iconRes = "gitlab",
        docsUrl = "https://docs.gitlab.com/ee/api",
        keyUrl = "https://gitlab.com/-/profile/personal_access_tokens",
        keyPrefix = "glpat-",
        validationEndpoint = "api/v4/user",
        category = ServiceCategory.DEVELOPMENT
    ),
    SUPABASE(
        displayName = "Supabase",
        iconRes = "supabase",
        docsUrl = "https://supabase.com/docs",
        keyUrl = "https://app.supabase.com/account/tokens",
        keyPrefix = "sbp_",
        validationEndpoint = "v1/projects",
        category = ServiceCategory.BACKEND
    ),
    FIREBASE(
        displayName = "Firebase",
        iconRes = "firebase",
        docsUrl = "https://firebase.google.com/docs",
        keyUrl = "https://console.firebase.google.com/project/_/settings/serviceaccounts",
        keyPrefix = "",
        category = ServiceCategory.BACKEND
    ),
    ELEVEN_LABS(
        displayName = "ElevenLabs",
        iconRes = "elevenlabs",
        docsUrl = "https://elevenlabs.io/docs",
        keyUrl = "https://elevenlabs.io/app/settings/api-keys",
        keyPrefix = "",
        validationEndpoint = "v1/user",
        category = ServiceCategory.MEDIA
    ),
    RUNWAY(
        displayName = "Runway ML",
        iconRes = "runway",
        docsUrl = "https://docs.runwayml.com",
        keyUrl = "https://app.runwayml.com/settings/api",
        keyPrefix = "key_",
        validationEndpoint = "v1/models",
        category = ServiceCategory.MEDIA
    );

    val maskedTestKey: String
        get() = if (keyPrefix.isNotEmpty()) "$keyPrefix${"•".repeat(24)}" else "•".repeat(28)

    val requiresBaseUrl: Boolean
        get() = this == GITLAB || this == SUPABASE
}

enum class ServiceCategory(
    val label: String,
    val icon: String
) {
    AI_MODEL("AI Models", "psychology"),
    AI_PLATFORM("AI Platforms", "cloud"),
    MESSAGING("Messaging", "chat"),
    DEVELOPMENT("Development", "code"),
    BACKEND("Backend Services", "storage"),
    MEDIA("Media Generation", "movie")
}
