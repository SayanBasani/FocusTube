package com.focustube.app.ui.screens.channel

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

data class ChannelUiState(
    val channelId: String = "",
    val channelName: String = "",
    val isLoading: Boolean = true,
    val videos: List<VideoItem> = emptyList(),
    val errorMessage: String? = null
)

class ChannelViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val subscriptions: StateFlow<List<ChannelInfo>> = storageRepository.subscriptions
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater

    fun loadChannel(channelId: String, channelName: String) {
        _uiState.value = ChannelUiState(channelId = channelId, channelName = channelName, isLoading = true)
        viewModelScope.launch {
            val result = videoRepository.getChannelVideos(channelId)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = list,
                    errorMessage = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load channel videos"
                )
            }
        }
    }

    fun toggleSubscription(channelId: String, channelName: String) {
        val channel = ChannelInfo(id = channelId, name = channelName)
        storageRepository.toggleSubscription(channel)
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }
}
