package com.focustube.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
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
    val errorMessage: String? = null
)

class HomeViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater

    private var currentPage: Int = 1
    private val seenVideoIds = mutableSetOf<String>()

    init {
        loadVideos("All")
    }

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        currentPage = 1
        seenVideoIds.clear()
        loadVideos(category)
    }

    fun refresh() {
        currentPage = 1
        seenVideoIds.clear()
        loadVideos(_uiState.value.selectedCategory)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = currentPage + 1
            val result = videoRepository.getHomeVideos(state.selectedCategory, page = nextPage)
            result.onSuccess { newList ->
                val distinctNew = newList.filter { it.id !in seenVideoIds }
                if (distinctNew.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        canLoadMore = false
                    )
                } else {
                    currentPage = nextPage
                    seenVideoIds.addAll(distinctNew.map { it.id })
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        videos = state.videos + distinctNew,
                        canLoadMore = true
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }

    private fun loadVideos(category: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = videoRepository.getHomeVideos(category, page = 1)
            result.onSuccess { list ->
                seenVideoIds.clear()
                seenVideoIds.addAll(list.map { it.id })
                currentPage = 1
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = list,
                    canLoadMore = list.isNotEmpty(),
                    errorMessage = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load feed"
                )
            }
        }
    }
}
