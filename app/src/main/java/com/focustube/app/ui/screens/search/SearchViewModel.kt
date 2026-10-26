package com.focustube.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
import com.focustube.app.util.SafeLogger
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
    val suggestions: List<String> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val nextCursor: String? = null
)

class SearchViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater
    val recentSearches: StateFlow<List<String>> = storageRepository.recentSearches

    private var searchJob: Job? = null
    private var debounceJob: Job? = null
    private var currentPage: Int = 1
    private val seenVideoIds = mutableSetOf<String>()

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)

        // Cancel pending debounce suggestions request
        debounceJob?.cancel()

        val clean = newQuery.trim()
        if (clean.length < 2) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
            return
        }

        debounceJob = viewModelScope.launch {
            delay(250) // 250ms debounce
            val result = videoRepository.getSearchSuggestions(clean)
            result.onSuccess { list ->
                if (_uiState.value.query.trim() == clean) {
                    _uiState.value = _uiState.value.copy(suggestions = list)
                }
            }
        }
    }

    fun submitSearch(query: String = _uiState.value.query) {
        val clean = query.trim()
        if (clean.isEmpty()) return

        debounceJob?.cancel()
        searchJob?.cancel()
        currentPage = 1
        seenVideoIds.clear()

        // Record query in user's recent searches
        storageRepository.addSearchQuery(clean)

        searchJob = viewModelScope.launch {
            SafeLogger.d("Search", "Submitting search for '$clean'")
            _uiState.value = _uiState.value.copy(
                query = clean,
                isLoading = true,
                hasSearched = true,
                errorMessage = null,
                suggestions = emptyList()
            )

            val result = videoRepository.searchVideosPaged(clean, page = 1)
            result.onSuccess { paged ->
                seenVideoIds.addAll(paged.items.map { it.id })
                SafeLogger.d("Search", "Search success: ${paged.items.size} videos returned")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = paged.items,
                    canLoadMore = paged.hasMore,
                    nextCursor = paged.nextCursor,
                    errorMessage = null
                )
            }.onFailure { err ->
                SafeLogger.w("Search", "Search failed: ${err.message}", err)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = emptyList(),
                    errorMessage = err.localizedMessage ?: "Unable to load search results. Please check connection and retry."
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
            SafeLogger.d("Search", "Loading next search page $nextPage for '$clean' with cursor=${state.nextCursor}")
            val result = videoRepository.searchVideosPaged(clean, page = nextPage, nextPageToken = state.nextCursor)
            result.onSuccess { paged ->
                val distinct = paged.items.filter { it.id !in seenVideoIds }
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
                        canLoadMore = paged.hasMore,
                        nextCursor = paged.nextCursor
                    )
                }
            }.onFailure { err ->
                SafeLogger.w("Search", "Next search page failed: ${err.message}")
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun clearQuery() {
        debounceJob?.cancel()
        searchJob?.cancel()
        _uiState.value = SearchUiState()
        currentPage = 1
        seenVideoIds.clear()
    }

    fun removeRecentSearch(query: String) {
        storageRepository.removeSearchQuery(query)
    }

    fun clearRecentSearches() {
        storageRepository.clearRecentSearches()
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }
}
