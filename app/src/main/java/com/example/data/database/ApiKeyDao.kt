// app/src/main/java/com/example/data/database/ApiKeyDao.kt
package com.example.data.database

import androidx.room.*
import com.example.data.settings.ApiKeyConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {

    @Query("SELECT * FROM api_keys ORDER BY serviceName ASC")
    fun getAllApiKeys(): Flow<List<ApiKeyConfig>>

    @Query("SELECT * FROM api_keys WHERE serviceName = :serviceName LIMIT 1")
    suspend fun getApiKey(serviceName: String): ApiKeyConfig?

    @Query("SELECT * FROM api_keys WHERE isEnabled = 1")
    fun getEnabledApiKeys(): Flow<List<ApiKeyConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKeys(apiKeys: List<ApiKeyConfig>)

    @Query("UPDATE api_keys SET isEnabled = :enabled WHERE serviceName = :serviceName")
    suspend fun toggleApiKey(serviceName: String, enabled: Boolean)

    @Query("UPDATE api_keys SET apiKey = :key, maskedKey = :masked, lastValidated = NULL, isValid = 0 WHERE serviceName = :serviceName")
    suspend fun updateApiKey(serviceName: String, key: String, masked: String)

    @Query("UPDATE api_keys SET lastValidated = :timestamp, isValid = :valid WHERE serviceName = :serviceName")
    suspend fun updateValidationStatus(serviceName: String, timestamp: Long, valid: Boolean)

    @Query("DELETE FROM api_keys WHERE serviceName = :serviceName")
    suspend fun deleteApiKey(serviceName: String)

    @Query("DELETE FROM api_keys")
    suspend fun deleteAllApiKeys()

    @Query("SELECT COUNT(*) FROM api_keys WHERE apiKey != '' AND isEnabled = 1")
    suspend fun getActiveKeyCount(): Int

    @Transaction
    suspend fun replaceAll(keys: List<ApiKeyConfig>) {
        deleteAllApiKeys()
        insertApiKeys(keys)
    }
}
