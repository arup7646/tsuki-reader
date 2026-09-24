package com.alix.tsuki.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alix.tsuki.data.local.preferences.TsukiPreferences
import com.alix.tsuki.data.model.PageCacheLimit
import com.alix.tsuki.data.model.ReadingDirection
import com.alix.tsuki.data.model.ThemeMode
import com.alix.tsuki.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: TsukiPreferences,
    private val repository: MangaRepository
) : ViewModel() {

    val readingDirection: StateFlow<ReadingDirection> = preferences.readingDirectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingDirection.LTR)

    val themeMode: StateFlow<ThemeMode> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val cacheLimit: StateFlow<PageCacheLimit> = preferences.cacheLimitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PageCacheLimit.MEDIUM)

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun setReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch {
            preferences.setReadingDirection(direction)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setCacheLimit(limit: PageCacheLimit) {
        viewModelScope.launch {
            preferences.setCacheLimit(limit)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearPageCache()
            refreshCacheSize()
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = repository.getCacheSizeBytes()
        }
    }

    class Factory(
        private val preferences: TsukiPreferences,
        private val repository: MangaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferences, repository) as T
        }
    }
}
