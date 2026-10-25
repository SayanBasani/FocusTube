package com.focustube.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val results: List<VideoItem> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
)

class SearchViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater

    private var searchJob: Job? = null
    private var currentPage: Int = 1
    private val seenVideoIds = mutableSetOf<String>()

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
    }

    fun submitSearch(query: String = _uiState.value.query) {
        val clean = query.trim()
        if (clean.isEmpty()) return

        searchJob?.cancel()
        currentPage = 1
        seenVideoIds.clear()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasSearched = true, errorMessage = null)
            val result = videoRepository.searchVideos(clean, page = 1)
            result.onSuccess { list ->
                seenVideoIds.addAll(list.map { it.id })
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = list,
                    canLoadMore = list.isNotEmpty(),
                    errorMessage = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Search failed"
                )
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val clean = state.query.trim()
        if (clean.isEmpty() || state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = currentPage + 1
            val result = videoRepository.searchVideos(clean, page = nextPage)
            result.onSuccess { newList ->
                val distinct = newList.filter { it.id !in seenVideoIds }
                if (distinct.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        canLoadMore = false
                    )
                } else {
                    currentPage = nextPage
                    seenVideoIds.addAll(distinct.map { it.id })
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        results = state.results + distinct,
                        canLoadMore = true
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun clearQuery() {
        _uiState.value = SearchUiState()
        currentPage = 1
        seenVideoIds.clear()
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }
}
