package com.focustube.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustube.app.data.model.ChannelInfo
import com.focustube.app.data.model.CommentItem
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoCommentsData
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.FilterManager
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlayerSheetState {
    HIDDEN,
    COLLAPSED,
    EXPANDED
}

enum class PlayerCommand {
    PLAY,
    PAUSE,
    TOGGLE_PLAY_PAUSE,
    STOP
}

data class PlayerUiState(
    val videoId: String = "",
    val sheetState: PlayerSheetState = PlayerSheetState.HIDDEN,
    val video: VideoItem? = null,
    val isLoading: Boolean = true,
    val isPlayerReady: Boolean = false,
    val isBuffering: Boolean = false,
    val isLive: Boolean = false,
    val isBehindLiveEdge: Boolean = false,
    val liveDeltaSeconds: Float = 0f,
    val commentsData: VideoCommentsData = VideoCommentsData(),
    val isCommentsLoading: Boolean = false,
    val relatedVideos: List<VideoItem> = emptyList(),
    val isRelatedLoading: Boolean = false,
    val isDescriptionExpanded: Boolean = false,
    val isCommentsSheetOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val selectedQuality: String = "Auto",
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val currentSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerSecondsRemaining: Int = 0,
    val isSleepTimerActive: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val bufferedFraction: Float = 0f,
    val errorMessage: String? = null
)

class PlayerViewModel(
    private val videoRepository: VideoRepository,
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState(videoId = ""))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater
    val subscriptions = storageRepository.subscriptions

    private val _playerCommands = MutableSharedFlow<PlayerCommand>(extraBufferCapacity = 8)
    val playerCommands: SharedFlow<PlayerCommand> = _playerCommands.asSharedFlow()

    private var sleepTimerJob: Job? = null

    fun openVideo(videoId: String) {
        if (_uiState.value.videoId == videoId && _uiState.value.video != null) {
            _uiState.value = _uiState.value.copy(sheetState = PlayerSheetState.EXPANDED)
            return
        }
        loadVideo(videoId)
        _uiState.value = _uiState.value.copy(sheetState = PlayerSheetState.EXPANDED)
    }

    fun collapsePlayer() {
        _uiState.value = _uiState.value.copy(sheetState = PlayerSheetState.COLLAPSED)
    }

    fun expandPlayer() {
        _uiState.value = _uiState.value.copy(sheetState = PlayerSheetState.EXPANDED)
    }

    fun closePlayer() {
        _playerCommands.tryEmit(PlayerCommand.STOP)
        _uiState.value = _uiState.value.copy(
            sheetState = PlayerSheetState.HIDDEN,
            isPlaying = false,
            videoId = ""
        )
    }

    fun requestTogglePlayPause() {
        _playerCommands.tryEmit(PlayerCommand.TOGGLE_PLAY_PAUSE)
    }

    fun requestPlay() {
        _playerCommands.tryEmit(PlayerCommand.PLAY)
    }

    fun requestPause() {
        _playerCommands.tryEmit(PlayerCommand.PAUSE)
    }

    fun loadVideo(videoId: String) {
        if (_uiState.value.videoId == videoId && _uiState.value.video != null) return

        val initialSpeed = storageRepository.preferences.value.preferredSpeed
        val targetSheetState = if (_uiState.value.sheetState == PlayerSheetState.HIDDEN) {
            PlayerSheetState.EXPANDED
        } else {
            _uiState.value.sheetState
        }

        _uiState.value = PlayerUiState(
            videoId = videoId,
            sheetState = targetSheetState,
            isLoading = true,
            isPlayerReady = false,
            isPlaying = false,
            currentSpeed = initialSpeed
        )

        viewModelScope.launch {
            val result = videoRepository.getVideoDetails(videoId)
            result.onSuccess { item ->
                val isItemLive = item.liveStatus == com.focustube.app.data.model.LiveStatus.LIVE
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    video = item,
                    isLive = isItemLive,
                    durationSeconds = if (isItemLive) 0L else item.durationSeconds
                )
                // Record in history if not paused
                storageRepository.recordHistory(item)

                // Load comments
                loadComments(videoId)

                // Load related videos
                loadRelatedVideos(videoId, item.title)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load video details"
                )
            }
        }
    }

    fun loadComments(videoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCommentsLoading = true)
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
                        errorMessage = err.localizedMessage ?: "Unable to load comments"
                    ),
                    isCommentsLoading = false
                )
            }
        }
    }

    fun loadRelatedVideos(videoId: String, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRelatedLoading = true)
            val result = videoRepository.getRelatedVideos(videoId, title)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    relatedVideos = list,
                    isRelatedLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isRelatedLoading = false)
            }
        }
    }

    fun toggleLike() {
        _uiState.value = _uiState.value.copy(
            isLiked = !_uiState.value.isLiked,
            isDisliked = false
        )
    }

    fun toggleDislike() {
        _uiState.value = _uiState.value.copy(
            isDisliked = !_uiState.value.isDisliked,
            isLiked = false
        )
    }

    fun toggleDescription() {
        _uiState.value = _uiState.value.copy(
            isDescriptionExpanded = !_uiState.value.isDescriptionExpanded
        )
    }

    fun setCommentsSheetOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            isCommentsSheetOpen = open
        )
    }

    fun setSettingsSheetOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSettingsSheetOpen = open
        )
    }

    fun setSelectedQuality(quality: String) {
        _uiState.value = _uiState.value.copy(
            selectedQuality = quality
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        val sanitized = FilterManager.sanitizeSpeed(speed)
        _uiState.value = _uiState.value.copy(currentSpeed = sanitized)
    }

    fun setPlayingState(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun setPlayerReady(ready: Boolean) {
        _uiState.value = _uiState.value.copy(isPlayerReady = ready)
    }

    fun setBuffering(buffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = buffering)
    }

    fun setLiveInfo(isLive: Boolean, behindLiveEdge: Boolean, deltaSeconds: Float) {
        _uiState.value = _uiState.value.copy(
            isLive = isLive,
            isBehindLiveEdge = behindLiveEdge,
            liveDeltaSeconds = deltaSeconds
        )
    }

    fun updatePlaybackPosition(seconds: Long) {
        _uiState.value = _uiState.value.copy(currentPositionSeconds = seconds)
    }

    fun updateDuration(seconds: Long) {
        if (seconds > 0) {
            _uiState.value = _uiState.value.copy(durationSeconds = seconds)
        }
    }

    fun updateBufferedFraction(fraction: Float) {
        _uiState.value = _uiState.value.copy(bufferedFraction = fraction.coerceIn(0f, 1f))
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.value = _uiState.value.copy(
                sleepTimerMinutes = 0,
                sleepTimerSecondsRemaining = 0,
                isSleepTimerActive = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            sleepTimerMinutes = minutes,
            sleepTimerSecondsRemaining = minutes * 60,
            isSleepTimerActive = true
        )

        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerSecondsRemaining = remaining)
            }
            _uiState.value = _uiState.value.copy(
                isSleepTimerActive = false,
                sleepTimerMinutes = 0,
                isPlaying = false
            )
        }
    }

    fun cancelSleepTimer() {
        setSleepTimer(0)
    }

    fun toggleBackgroundAudio(enabled: Boolean) {
        storageRepository.updatePreferences(storageRepository.preferences.value.copy(backgroundAudioEnabled = enabled))
    }

    fun savePlaybackPosition(positionSeconds: Long) {
        storageRepository.savePlaybackPosition(_uiState.value.videoId, positionSeconds * 1000)
    }

    fun getSavedPositionSeconds(): Long {
        return storageRepository.getPlaybackPosition(_uiState.value.videoId) / 1000
    }

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }

    fun toggleSubscription(channelId: String, channelName: String, avatarUrl: String = "") {
        val channel = ChannelInfo(
            id = channelId,
            name = channelName,
            avatarUrl = avatarUrl
        )
        storageRepository.toggleSubscription(channel)
    }

    fun isSaved(videoId: String): Boolean {
        return storageRepository.isWatchLater(videoId)
    }
}
