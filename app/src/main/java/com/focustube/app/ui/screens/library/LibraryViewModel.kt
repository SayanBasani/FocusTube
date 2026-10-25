package com.focustube.app.ui.screens.library

import androidx.lifecycle.ViewModel
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.LocalStorageRepository
import kotlinx.coroutines.flow.StateFlow

class LibraryViewModel(
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences
    val watchLater: StateFlow<List<VideoItem>> = storageRepository.watchLater
    val history: StateFlow<List<VideoItem>> = storageRepository.history
    val historyPaused: StateFlow<Boolean> = storageRepository.historyPaused

    fun toggleWatchLater(video: VideoItem) {
        storageRepository.toggleWatchLater(video)
    }

    fun removeHistoryItem(videoId: String) {
        storageRepository.removeHistoryItem(videoId)
    }

    fun clearHistory() {
        storageRepository.clearHistory()
    }

    fun togglePauseHistory() {
        storageRepository.togglePauseHistory()
    }
}
