package com.focustube.app.ui.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.ChannelInfo
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubscriptionsUiState(
    val isLoading: Boolean = false,
    val feedVideos: List<VideoItem> = emptyList(),
    val errorMessage: String? = null
)

class SubscriptionsViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val subscriptions: StateFlow<List<ChannelInfo>> = storageRepository.subscriptions
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater

    init {
        loadFeed()
    }

    fun loadFeed() {
        if (storageRepository.subscriptions.value.isEmpty()) {
            _uiState.value = SubscriptionsUiState(isLoading = false, feedVideos = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = videoRepository.getSubscriptionFeed()
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    feedVideos = list,
                    errorMessage = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to refresh subscriptions"
                )
            }
        }
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }
}
