package com.alix.tsuki.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: MangaRepository
) : ViewModel() {

    val mangaList: StateFlow<List<Manga>> = repository.getAllManga()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.addFolder(uri)
            _isRefreshing.value = false
            result.onSuccess { count ->
                _userMessage.value = "Found $count comic(s)"
            }.onFailure { error ->
                _userMessage.value = "Failed to scan folder: ${error.localizedMessage}"
            }
        }
    }

    fun removeFolder(parentTreeUri: String) {
        viewModelScope.launch {
            repository.removeFolder(parentTreeUri)
            _userMessage.value = "Folder removed from library"
        }
    }

    fun deleteManga(mangaId: String) {
        viewModelScope.launch {
            repository.deleteManga(mangaId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.rescanAll()
            _isRefreshing.value = false
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    class Factory(private val repository: MangaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository) as T
        }
    }
}
