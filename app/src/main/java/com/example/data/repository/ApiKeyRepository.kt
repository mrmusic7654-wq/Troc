// app/src/main/java/com/example/data/repository/ApiKeyRepository.kt
package com.example.data.repository

import com.example.data.api.GeminiApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.database.ApiKeyDao
import com.example.data.settings.ApiKeyConfig
import com.example.data.settings.ApiService
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

class ApiKeyRepository(
    private val apiKeyDao: ApiKeyDao,
    private val geminiService: GeminiApiService
) {

    fun getAllApiKeys(): Flow<List<ApiKeyConfig>> =
        apiKeyDao.getAllApiKeys()

    fun getEnabledApiKeys(): Flow<List<ApiKeyConfig>> =
        apiKeyDao.getEnabledApiKeys()

    suspend fun getApiKey(serviceName: String): ApiKeyConfig? =
        apiKeyDao.getApiKey(serviceName)

    suspend fun getActiveApiKey(service: ApiService): String? {
        val config = apiKeyDao.getApiKey(service.name)
        return if (config?.isEnabled == true && config.apiKey.isNotBlank()) {
            config.apiKey
        } else null
    }

    suspend fun saveApiKey(apiKey: ApiKeyConfig) {
        val masked = ApiKeyConfig.maskKey(apiKey.apiKey)
        apiKeyDao.insertApiKey(
            apiKey.copy(maskedKey = masked)
        )
    }

    suspend fun deleteApiKey(serviceName: String) {
        apiKeyDao.deleteApiKey(serviceName)
    }

    suspend fun toggleApiKey(serviceName: String, enabled: Boolean) {
        apiKeyDao.toggleApiKey(serviceName, enabled)
    }

    suspend fun validateKey(service: ApiService, key: String): Boolean {
        return try {
            when (service) {
                ApiService.GEMINI -> validateGeminiKey(key)
                ApiService.HUGGING_FACE -> validateHuggingFaceKey(key)
                ApiService.GITHUB -> validateGitHubKey(key)
                ApiService.TELEGRAM -> validateTelegramKey(key)
                ApiService.OPENAI -> validateOpenAIKey(key)
                ApiService.ANTHROPIC -> validateAnthropicKey(key)
                ApiService.REPLICATE -> validateReplicateKey(key)
                ApiService.GITLAB -> validateGitLabKey(key)
                ApiService.ELEVEN_LABS -> validateElevenLabsKey(key)
                ApiService.RUNWAY -> validateRunwayKey(key)
                else -> validateGenericKey(service, key)
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateGeminiKey(key: String): Boolean {
        return try {
            val response = geminiService.generateContent(
                key,"gemini-2.0-flash", 
                GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(Part(text = "Respond with just: ok"))
                        )
                    )
                )
            )
            response.candidates != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateHuggingFaceKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://huggingface.co/api/whoami-v2")
            .header("Authorization", "Bearer $key")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateGitHubKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "token $key")
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateTelegramKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$key/getMe")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateOpenAIKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer $key")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateAnthropicKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", key)
            .header("anthropic-version", "2023-06-01")
            .post(okhttp3.RequestBody.create(null, ByteArray(0)))
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.code != 401
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateReplicateKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.replicate.com/v1/models")
            .header("Authorization", "Token $key")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateGitLabKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://gitlab.com/api/v4/user")
            .header("PRIVATE-TOKEN", key)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateElevenLabsKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/user")
            .header("xi-api-key", key)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateRunwayKey(key: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.runwayml.com/v1/models")
            .header("Authorization", "Bearer $key")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateGenericKey(service: ApiService, key: String): Boolean {
        if (service.keyPrefix.isNotEmpty()) {
            return key.startsWith(service.keyPrefix) && key.length >= 24
        }
        return key.length >= 20
    }

    suspend fun getActiveKeyCount(): Int =
        apiKeyDao.getActiveKeyCount()

    suspend fun updateValidationStatus(
        serviceName: String,
        timestamp: Long,
        valid: Boolean
    ) {
        apiKeyDao.updateValidationStatus(serviceName, timestamp, valid)
    }
}
