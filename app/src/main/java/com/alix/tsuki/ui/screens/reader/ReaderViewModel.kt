package com.alix.tsuki.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alix.tsuki.data.local.preferences.TsukiPreferences
import com.alix.tsuki.data.model.Chapter
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.model.ReaderPage
import com.alix.tsuki.data.model.ReadingDirection
import com.alix.tsuki.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ReaderViewModel(
    val mangaId: String,
    initialChapterId: String,
    private val repository: MangaRepository,
    private val preferences: TsukiPreferences
) : ViewModel() {

    private val _manga = MutableStateFlow<Manga?>(null)
    val manga: StateFlow<Manga?> = _manga.asStateFlow()

    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()

    private val _pages = MutableStateFlow<List<ReaderPage>>(emptyList())
    val pages: StateFlow<List<ReaderPage>> = _pages.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val readingDirection: StateFlow<ReadingDirection> = preferences.readingDirectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingDirection.LTR)

    private val pageFileCache = ConcurrentHashMap<Int, File>()

    init {
        loadChapter(initialChapterId)
    }

    fun loadChapter(chapterId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            pageFileCache.clear()

            try {
                if (_manga.value == null) {
                    _manga.value = repository.getMangaById(mangaId)
                }

                val chapter = repository.getChapterById(chapterId)
                if (chapter == null) {
                    _errorMessage.value = "Chapter not found"
                    _isLoading.value = false
                    return@launch
                }
                _currentChapter.value = chapter

                val loadedPages = repository.loadPagesForChapter(chapter)
                _pages.value = loadedPages

                val initialPage = chapter.lastReadPage.coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0))
                _currentPage.value = initialPage

                // Preload nearby pages
                preloadPage(chapterId, initialPage)
                preloadPage(chapterId, initialPage + 1)
            } catch (e: Exception) {
                _errorMessage.value = "Error loading chapter: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPageChanged(newIndex: Int) {
        val pageCount = _pages.value.size
        if (pageCount == 0) return
        val clamped = newIndex.coerceIn(0, pageCount - 1)
        if (_currentPage.value != clamped) {
            _currentPage.value = clamped
            val chapter = _currentChapter.value
            if (chapter != null) {
                viewModelScope.launch {
                    repository.updateReadingProgress(mangaId, chapter.id, chapter.title, clamped)
                }
                preloadPage(chapter.id, clamped - 1)
                preloadPage(chapter.id, clamped + 1)
                preloadPage(chapter.id, clamped + 2)
            }
        }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch {
            preferences.setReadingDirection(direction)
        }
    }

    suspend fun getPageFile(page: ReaderPage): File? {
        val chapter = _currentChapter.value ?: return null
        pageFileCache[page.index]?.let { if (it.exists()) return it }
        val file = repository.getPageFile(chapter.id, page)
        if (file != null && file.exists()) {
            pageFileCache[page.index] = file
        }
        return file
    }

    private fun preloadPage(chapterId: String, index: Int) {
        viewModelScope.launch {
            val pagesList = _pages.value
            if (index in pagesList.indices && !pageFileCache.containsKey(index)) {
                val page = pagesList[index]
                val file = repository.getPageFile(chapterId, page)
                if (file != null && file.exists()) {
                    pageFileCache[page.index] = file
                }
            }
        }
    }

    class Factory(
        private val mangaId: String,
        private val chapterId: String,
        private val repository: MangaRepository,
        private val preferences: TsukiPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(mangaId, chapterId, repository, preferences) as T
        }
    }
}
