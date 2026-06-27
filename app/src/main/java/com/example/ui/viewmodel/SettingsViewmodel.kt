// app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt
package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ApiKeyRepository
import com.example.data.settings.ApiKeyConfig
import com.example.data.settings.ApiService
import com.example.data.settings.ServiceCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val _apiKeys = MutableStateFlow<Map<String, ApiKeyConfig>>(emptyMap())
    val apiKeys: StateFlow<Map<String, ApiKeyConfig>> = _apiKeys.asStateFlow()

    private val _isValidating = MutableStateFlow<Set<String>>(emptySet())
    val isValidating: StateFlow<Set<String>> = _isValidating.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<SnackbarEvent>()
    val snackbarMessage: SharedFlow<SnackbarEvent> = _snackbarMessage.asSharedFlow()

    private val _selectedCategory = MutableStateFlow<ServiceCategory?>(null)
    val selectedCategory: StateFlow<ServiceCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredServices: StateFlow<List<ApiService>> = combine(
        _selectedCategory,
        _searchQuery
    ) { category, query ->
        ApiService.entries.filter { service ->
            val matchesCategory = category == null || service.category == category
            val matchesSearch = query.isBlank() ||
                    service.displayName.contains(query, ignoreCase = true) ||
                    service.category.label.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApiService.entries.toList()
    )

    val categoryServices: StateFlow<Map<ServiceCategory, List<ApiService>>> =
        combine(_apiKeys, _searchQuery) { keys, query ->
            ServiceCategory.entries.associateWith { category ->
                ApiService.entries
                    .filter { it.category == category }
                    .filter { service ->
                        query.isBlank() ||
                                service.displayName.contains(query, ignoreCase = true)
                    }
                    .sortedBy { service ->
                        val config = keys[service.name]
                        if (config?.isValid == true) 0
                        else if (config?.apiKey?.isNotBlank() == true) 1
                        else 2
                    }
            }.filterValues { it.isNotEmpty() }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    init {
        loadApiKeys()
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            apiKeyRepository.getAllApiKeys().collect { keys ->
                _apiKeys.value = keys.associateBy { it.serviceName }
            }
        }
    }

    fun updateApiKey(service: ApiService, key: String) {
        viewModelScope.launch {
            val existingKey = _apiKeys.value[service.name]
            val config = ApiKeyConfig(
                serviceName = service.name,
                apiKey = key,
                isEnabled = existingKey?.isEnabled ?: true,
                lastValidated = existingKey?.lastValidated,
                isValid = false,
                displayName = service.displayName,
                maskedKey = ApiKeyConfig.maskKey(key)
            )
            apiKeyRepository.saveApiKey(config)
            _snackbarMessage.emit(
                SnackbarEvent.Success("${service.displayName} key encrypted & saved")
            )
        }
    }

    fun toggleApiKey(serviceName: String, enabled: Boolean) {
        viewModelScope.launch {
            apiKeyRepository.toggleApiKey(serviceName, enabled)
            val status = if (enabled) "activated" else "paused"
            val service = ApiService.entries.find { it.name == serviceName }
            _snackbarMessage.emit(
                SnackbarEvent.Info("${service?.displayName ?: serviceName} $status")
            )
        }
    }

    fun validateApiKey(service: ApiService) {
        viewModelScope.launch {
            val key = _apiKeys.value[service.name]
            if (key == null || key.apiKey.isBlank()) {
                _validationErrors.value = _validationErrors.value + 
                    (service.name to "Key cannot be empty")
                _snackbarMessage.emit(
                    SnackbarEvent.Error("Please enter ${service.displayName} key first")
                )
                return@launch
            }

            _isValidating.value = _isValidating.value + service.name
            _validationErrors.value = _validationErrors.value - service.name

            try {
                val isValid = apiKeyRepository.validateKey(service, key.apiKey)
                val timestamp = System.currentTimeMillis()
                apiKeyRepository.updateValidationStatus(service.name, timestamp, isValid)

                if (isValid) {
                    _snackbarMessage.emit(
                        SnackbarEvent.Success("${service.displayName} — Connection balanced ✓")
                    )
                } else {
                    _validationErrors.value = _validationErrors.value +
                        (service.name to "Validation failed — check your key")
                    _snackbarMessage.emit(
                        SnackbarEvent.Error("${service.displayName} rejected the key")
                    )
                }
            } catch (e: Exception) {
                _validationErrors.value = _validationErrors.value +
                    (service.name to "Network error: ${e.localizedMessage ?: "Unknown"}")
                _snackbarMessage.emit(
                    SnackbarEvent.Error("Validation disrupted: ${e.localizedMessage ?: "Network issue"}")
                )
            } finally {
                _isValidating.value = _isValidating.value - service.name
            }
        }
    }

    fun validateAllKeys() {
        viewModelScope.launch {
            val enabledKeys = _apiKeys.value.filter { 
                it.value.isEnabled && it.value.apiKey.isNotBlank() 
            }
            if (enabledKeys.isEmpty()) {
                _snackbarMessage.emit(SnackbarEvent.Info("No keys configured to validate"))
                return@launch
            }
            _snackbarMessage.emit(SnackbarEvent.Info("Validating ${enabledKeys.size} keys..."))
            enabledKeys.keys.forEach { serviceName ->
                val service = ApiService.entries.find { it.name == serviceName }
                if (service != null) validateApiKey(service)
            }
        }
    }

    fun deleteApiKey(serviceName: String) {
        viewModelScope.launch {
            apiKeyRepository.deleteApiKey(serviceName)
            _validationErrors.value = _validationErrors.value - serviceName
            val service = ApiService.entries.find { it.name == serviceName }
            _snackbarMessage.emit(
                SnackbarEvent.Info("${service?.displayName ?: serviceName} key removed")
            )
        }
    }

    fun getApiKey(service: ApiService): String? {
        val config = _apiKeys.value[service.name]
        return if (config?.isEnabled == true && config.apiKey.isNotBlank()) {
            config.apiKey
        } else null
    }

    fun selectCategory(category: ServiceCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError(serviceName: String) {
        _validationErrors.value = _validationErrors.value - serviceName
    }
}

sealed class SnackbarEvent {
    data class Success(val message: String) : SnackbarEvent()
    data class Error(val message: String) : SnackbarEvent()
    data class Info(val message: String) : SnackbarEvent()
}

class SettingsViewModelFactory(
    private val apiKeyRepository: ApiKeyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(apiKeyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
