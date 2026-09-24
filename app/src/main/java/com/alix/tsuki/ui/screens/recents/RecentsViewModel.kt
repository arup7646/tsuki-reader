package com.alix.tsuki.ui.screens.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.repository.MangaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecentsViewModel(
    private val repository: MangaRepository
) : ViewModel() {

    val recentManga: StateFlow<List<Manga>> = repository.getRecentManga(limit = 10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearRecents(mangaId: String) {
        viewModelScope.launch {
            repository.clearMangaProgress(mangaId)
        }
    }

    class Factory(private val repository: MangaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecentsViewModel(repository) as T
        }
    }
}
