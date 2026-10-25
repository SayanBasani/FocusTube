package com.focustube.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.ProgressBarThickness
import com.focustube.app.data.model.ThemeMode
import com.focustube.app.data.repository.LocalStorageRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val storageRepository: LocalStorageRepository
) : ViewModel() {

    val preferences: StateFlow<FocusPreferences> = storageRepository.preferences

    fun updatePreferences(newPrefs: FocusPreferences) {
        storageRepository.updatePreferences(newPrefs)
    }

    fun setThemeMode(mode: ThemeMode) {
        updatePreferences(preferences.value.copy(themeMode = mode))
    }

    fun toggleReels(enabled: Boolean) {
        updatePreferences(preferences.value.copy(reelsEnabled = enabled))
    }

    fun toggleExcludeShortsDiscover(enabled: Boolean) {
        updatePreferences(preferences.value.copy(excludeShortsFromDiscover = enabled))
    }

    fun toggleExcludeShortsSearch(enabled: Boolean) {
        updatePreferences(preferences.value.copy(excludeShortsFromSearch = enabled))
    }

    fun toggleAutoplay(enabled: Boolean) {
        updatePreferences(preferences.value.copy(autoplay = enabled))
    }

    fun toggleAutoPlayNext(enabled: Boolean) {
        updatePreferences(preferences.value.copy(autoPlayNext = enabled))
    }

    fun toggleFocusMode(enabled: Boolean) {
        updatePreferences(preferences.value.copy(focusMode = enabled))
    }

    fun toggleBackgroundAudio(enabled: Boolean) {
        updatePreferences(preferences.value.copy(backgroundAudioEnabled = enabled))
    }

    fun togglePipEnabled(enabled: Boolean) {
        updatePreferences(preferences.value.copy(pipEnabled = enabled))
    }

    fun toggleMediaNotifications(enabled: Boolean) {
        updatePreferences(preferences.value.copy(mediaNotificationsEnabled = enabled))
    }

    fun toggleResumePlayback(enabled: Boolean) {
        updatePreferences(preferences.value.copy(resumePlaybackPosition = enabled))
    }

    fun toggleKeepScreenAwake(enabled: Boolean) {
        updatePreferences(preferences.value.copy(keepScreenAwake = enabled))
    }

    fun setPreferredSpeed(speed: Float) {
        updatePreferences(preferences.value.copy(preferredSpeed = speed))
    }

    fun toggleHideComments(enabled: Boolean) {
        updatePreferences(preferences.value.copy(hideComments = enabled))
    }

    fun toggleHideMetrics(enabled: Boolean) {
        updatePreferences(preferences.value.copy(hideMetrics = enabled))
    }

    fun toggleGrayscaleThumbnails(enabled: Boolean) {
        updatePreferences(preferences.value.copy(grayscaleThumbnails = enabled))
    }

    fun setProgressBarThickness(thickness: ProgressBarThickness) {
        updatePreferences(preferences.value.copy(progressBarThickness = thickness))
    }

    fun clearHistory() {
        storageRepository.clearHistory()
    }
}
