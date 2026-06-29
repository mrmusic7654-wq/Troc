// app/src/main/java/com/example/data/api/GeminiService.kt
package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════
// Gemini API • Moshi Data Models
// ═══════════════════════════════════════

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null,
    val safetySettings: List<SafetySetting>? = null,
    val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val fileData: FileData? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class FileData(
    val mimeType: String,
    val fileUri: String
)

@JsonClass(generateAdapter = true)
data class FunctionCall(
    val name: String,
    val args: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class FunctionResponse(
    val name: String,
    val response: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val stopSequences: List<String>? = null,
    val candidateCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class SafetySetting(
    val category: String,
    val threshold: String
)

@JsonClass(generateAdapter = true)
data class Tool(
    val functionDeclarations: List<FunctionDeclaration>? = null
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null,
    val usageMetadata: UsageMetadata? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null,
    val citationMetadata: CitationMetadata? = null,
    val tokenCount: Int? = null,
    val index: Int? = null
)

@JsonClass(generateAdapter = true)
data class SafetyRating(
    val category: String,
    val probability: String,
    val severity: String? = null
)

@JsonClass(generateAdapter = true)
data class PromptFeedback(
    val safetyRatings: List<SafetyRating>? = null,
    val blockReason: String? = null
)

@JsonClass(generateAdapter = true)
data class UsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class CitationMetadata(
    val citationSources: List<CitationSource>? = null
)

@JsonClass(generateAdapter = true)
data class CitationSource(
    val startIndex: Int? = null,
    val endIndex: Int? = null,
    val uri: String? = null,
    val license: String? = null
)

// ═══════════════════════════════════════
// Model Info Models
// ═══════════════════════════════════════

@JsonClass(generateAdapter = true)
data class ModelListResponse(
    val models: List<ModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class ModelInfo(
    val name: String,
    val displayName: String,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class CountTokensRequest(
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class CountTokensResponse(
    val totalTokens: Int? = null
)

// ═══════════════════════════════════════
// Gemini API Service Interface
// ═══════════════════════════════════════

interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Path("model") model: String = "gemini-2.5-flash",
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:streamGenerateContent")
    suspend fun streamGenerateContent(
        @Query("key") apiKey: String,
        @Path("model") model: String = "gemini-2.5-flash",
        @Query("alt") alt: String = "sse",
        @Body request: GenerateContentRequest
    ): okhttp3.Response

    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): ModelListResponse

    @GET("v1beta/models/{model}")
    suspend fun getModel(
        @Query("key") apiKey: String,
        @Path("model") model: String
    ): ModelInfo

    @POST("v1beta/models/{model}:countTokens")
    suspend fun countTokens(
        @Query("key") apiKey: String,
        @Path("model") model: String = "gemini-2.5-flash",
        @Body request: CountTokensRequest
    ): CountTokensResponse

    @POST("v1beta/models/{model}:embedContent")
    suspend fun embedContent(
        @Query("key") apiKey: String,
        @Path("model") model: String = "text-embedding-004",
        @Body request: Map<String, Any>
    ): Map<String, Any>
}

// ═══════════════════════════════════════
// Legacy Endpoint (Backward Compatible)
// ═══════════════════════════════════════

interface GeminiLegacyService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// ═══════════════════════════════════════
// Retrofit Client Singleton
// ═══════════════════════════════════════

object RetrofitClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    val legacyService: GeminiLegacyService by lazy {
        retrofit.create(GeminiLegacyService::class.java)
    }

    fun enableDebugLogging() {
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    }

    fun disableDebugLogging() {
        loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
    }
}

// ═══════════════════════════════════════
// Safety Categories Constants
// ═══════════════════════════════════════

object SafetyCategories {
    const val HARM_CATEGORY_HARASSMENT = "HARM_CATEGORY_HARASSMENT"
    const val HARM_CATEGORY_HATE_SPEECH = "HARM_CATEGORY_HATE_SPEECH"
    const val HARM_CATEGORY_SEXUALLY_EXPLICIT = "HARM_CATEGORY_SEXUALLY_EXPLICIT"
    const val HARM_CATEGORY_DANGEROUS_CONTENT = "HARM_CATEGORY_DANGEROUS_CONTENT"
    const val HARM_CATEGORY_CIVIC_INTEGRITY = "HARM_CATEGORY_CIVIC_INTEGRITY"
}

object SafetyThresholds {
    const val BLOCK_NONE = "BLOCK_NONE"
    const val BLOCK_ONLY_HIGH = "BLOCK_ONLY_HIGH"
    const val BLOCK_MEDIUM_AND_ABOVE = "BLOCK_MEDIUM_AND_ABOVE"
    const val BLOCK_LOW_AND_ABOVE = "BLOCK_LOW_AND_ABOVE"
    const val HARM_BLOCK_THRESHOLD_UNSPECIFIED = "HARM_BLOCK_THRESHOLD_UNSPECIFIED"
}

object FinishReasons {
    const val FINISH_REASON_UNSPECIFIED = "FINISH_REASON_UNSPECIFIED"
    const val STOP = "STOP"
    const val MAX_TOKENS = "MAX_TOKENS"
    const val SAFETY = "SAFETY"
    const val RECITATION = "RECITATION"
    const val OTHER = "OTHER"
}
