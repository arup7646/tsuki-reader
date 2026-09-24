package com.alix.tsuki.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alix.tsuki.data.model.Chapter
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MangaDetailsViewModel(
    private val mangaId: String,
    private val repository: MangaRepository
) : ViewModel() {

    private val _manga = MutableStateFlow<Manga?>(null)
    val manga: StateFlow<Manga?> = _manga.asStateFlow()

    val chapters: StateFlow<List<Chapter>> = repository.getChapters(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadManga()
    }

    private fun loadManga() {
        viewModelScope.launch {
            _manga.value = repository.getMangaById(mangaId)
        }
    }

    fun getResumeChapter(): Chapter? {
        val chapterList = chapters.value
        if (chapterList.isEmpty()) return null
        val currentManga = _manga.value ?: return chapterList.firstOrNull()

        return if (currentManga.lastReadChapterId != null) {
            chapterList.find { it.id == currentManga.lastReadChapterId } ?: chapterList.firstOrNull()
        } else {
            chapterList.firstOrNull()
        }
    }

    class Factory(
        private val mangaId: String,
        private val repository: MangaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MangaDetailsViewModel(mangaId, repository) as T
        }
    }
}
