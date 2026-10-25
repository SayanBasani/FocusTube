package com.focustube.app.ui.screens.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.ChannelInfo
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoCommentsData
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReelsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val reels: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val likedVideoIds: Set<String> = emptySet(),
    val commentsData: VideoCommentsData = VideoCommentsData(),
    val isCommentsLoading: Boolean = false,
    val isCommentsSheetOpen: Boolean = false,
    val activeCommentsVideoId: String? = null,
    val errorMessage: String? = null
)

class ReelsViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val subscriptions = storageRepository.subscriptions
    val watchLater = storageRepository.watchLater

    private var currentPage = 1
    private val seenReelIds = mutableSetOf<String>()

    init {
        loadReels()
    }

    fun loadReels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            currentPage = 1
            seenReelIds.clear()
            val result = videoRepository.getReelsVideos(page = 1)
            result.onSuccess { list ->
                val unique = list.filter { seenReelIds.add(it.id) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    reels = unique,
                    currentIndex = 0,
                    errorMessage = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load Reels"
                )
            }
        }
    }

    fun loadMoreReels() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            currentPage++
            val result = videoRepository.getReelsVideos(page = currentPage)
            result.onSuccess { list ->
                val newItems = list.filter { seenReelIds.add(it.id) }
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    reels = _uiState.value.reels + newItems
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleLike(videoId: String) {
        val current = _uiState.value.likedVideoIds
        val updated = if (current.contains(videoId)) current - videoId else current + videoId
        _uiState.value = _uiState.value.copy(likedVideoIds = updated)
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }

    fun toggleSubscription(channelId: String, channelName: String, avatarUrl: String) {
        val channel = ChannelInfo(
            id = channelId,
            name = channelName,
            avatarUrl = avatarUrl,
            isSubscribed = true
        )
        storageRepository.toggleSubscription(channel)
    }

    fun openComments(videoId: String) {
        _uiState.value = _uiState.value.copy(
            isCommentsSheetOpen = true,
            activeCommentsVideoId = videoId,
            isCommentsLoading = true
        )
        viewModelScope.launch {
            val result = videoRepository.getComments(videoId)
            result.onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    commentsData = data,
                    isCommentsLoading = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    commentsData = VideoCommentsData(
                        status = CommentStatus.ERROR,
                        errorMessage = err.localizedMessage ?: "Could not load comments"
                    ),
                    isCommentsLoading = false
                )
            }
        }
    }

    fun closeComments() {
        _uiState.value = _uiState.value.copy(
            isCommentsSheetOpen = false,
            activeCommentsVideoId = null
        )
    }
}
