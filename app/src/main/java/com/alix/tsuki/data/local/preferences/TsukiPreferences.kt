package com.alix.tsuki.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alix.tsuki.data.model.PageCacheLimit
import com.alix.tsuki.data.model.ReadingDirection
import com.alix.tsuki.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tsuki_settings")

class TsukiPreferences(private val context: Context) {

    private object Keys {
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CACHE_LIMIT = stringPreferencesKey("cache_limit")
    }

    val readingDirectionFlow: Flow<ReadingDirection> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.READING_DIRECTION]
        raw?.let { runCatching { ReadingDirection.valueOf(it) }.getOrNull() } ?: ReadingDirection.LTR
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.THEME_MODE]
        raw?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val cacheLimitFlow: Flow<PageCacheLimit> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.CACHE_LIMIT]
        raw?.let { runCatching { PageCacheLimit.valueOf(it) }.getOrNull() } ?: PageCacheLimit.MEDIUM
    }

    suspend fun setReadingDirection(direction: ReadingDirection) {
        context.dataStore.edit { preferences ->
            preferences[Keys.READING_DIRECTION] = direction.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setCacheLimit(limit: PageCacheLimit) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CACHE_LIMIT] = limit.name
        }
    }
}
