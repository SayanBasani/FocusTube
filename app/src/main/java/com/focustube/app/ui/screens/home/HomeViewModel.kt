package com.focustube.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
import com.focustube.app.util.SafeLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val selectedCategory: String = "All",
    val videos: List<VideoItem> = emptyList(),
    val errorMessage: String? = null,
    val pageLoadError: String? = null,
    val nextCursor: String? = null
)

class HomeViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater

    private var loadJob: Job? = null
    private var nextPageJob: Job? = null
    private var currentPage: Int = 1
    private var nextCursor: String? = null
    private val seenVideoIds = mutableSetOf<String>()

    init {
        loadVideos("All")
    }

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category && !uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        currentPage = 1
        nextCursor = null
        seenVideoIds.clear()
        loadVideos(category)
    }

    fun refresh() {
        currentPage = 1
        nextCursor = null
        seenVideoIds.clear()
        loadVideos(_uiState.value.selectedCategory)
    }

    fun retryNextPage() {
        _uiState.value = _uiState.value.copy(pageLoadError = null)
        loadNextPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        nextPageJob?.cancel()
        nextPageJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, pageLoadError = null)
            val nextPage = currentPage + 1
            SafeLogger.d("Feed", "Loading next page $nextPage for category '${state.selectedCategory}' with cursor=$nextCursor")

            val result = videoRepository.getHomeVideosPaged(
                category = state.selectedCategory,
                page = nextPage,
                cursor = nextCursor,
                existingVideos = state.videos
            )

            result.onSuccess { paged ->
                val distinctNew = paged.items.filter { it.id !in seenVideoIds }
                SafeLogger.d("Feed", "Page $nextPage returned ${paged.items.size} videos, ${distinctNew.size} distinct")

                if (distinctNew.isEmpty()) {
                    // If this batch had only duplicates, update cursor and check if more exist
                    nextCursor = paged.nextCursor
                    if (!paged.hasMore) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            canLoadMore = false
                        )
                    } else {
                        currentPage = nextPage
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            canLoadMore = paged.hasMore
                        )
                    }
                } else {
                    currentPage = nextPage
                    nextCursor = paged.nextCursor
                    seenVideoIds.addAll(distinctNew.map { it.id })
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        videos = state.videos + distinctNew,
                        canLoadMore = paged.hasMore,
                        pageLoadError = null,
                        nextCursor = paged.nextCursor
                    )
                }
            }.onFailure { err ->
                SafeLogger.w("Feed", "Next page load failed: ${err.message}", err)
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    pageLoadError = err.localizedMessage ?: "Failed to load more videos. Tap to retry."
                )
            }
        }
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }

    private fun loadVideos(category: String) {
        loadJob?.cancel()
        nextPageJob?.cancel()
        loadJob = viewModelScope.launch {
            SafeLogger.d("Feed", "Initial load for category '$category'")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                pageLoadError = null
            )

            val result = videoRepository.getHomeVideosPaged(category, page = 1, cursor = null)
            result.onSuccess { paged ->
                seenVideoIds.clear()
                seenVideoIds.addAll(paged.items.map { it.id })
                currentPage = 1
                nextCursor = paged.nextCursor
                SafeLogger.d("Feed", "Initial load success: ${paged.items.size} videos loaded")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = paged.items,
                    canLoadMore = paged.hasMore,
                    errorMessage = null,
                    pageLoadError = null,
                    nextCursor = paged.nextCursor
                )
            }.onFailure { err ->
                SafeLogger.w("Feed", "Initial load failed: ${err.message}", err)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load feed. Tap to retry."
                )
            }
        }
    }
}
