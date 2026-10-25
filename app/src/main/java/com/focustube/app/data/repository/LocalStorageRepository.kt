package com.focustube.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.focustube.app.data.model.ChannelInfo
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.ProgressBarThickness
import com.focustube.app.data.model.ThemeMode
import com.focustube.app.data.model.VideoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalStorageRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_tube_prefs", Context.MODE_PRIVATE)
    private val playbackPrefs: SharedPreferences =
        context.getSharedPreferences("focus_tube_playback_pos", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<FocusPreferences> = _preferences.asStateFlow()

    private val _watchLater = MutableStateFlow(loadWatchLater())
    val watchLater: StateFlow<List<VideoItem>> = _watchLater.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<VideoItem>> = _history.asStateFlow()

    private val _subscriptions = MutableStateFlow(loadSubscriptions())
    val subscriptions: StateFlow<List<ChannelInfo>> = _subscriptions.asStateFlow()

    private val _historyPaused = MutableStateFlow(prefs.getBoolean("history_paused", false))
    val historyPaused: StateFlow<Boolean> = _historyPaused.asStateFlow()

    // --- Focus Preferences ---

    private fun loadPreferences(): FocusPreferences {
        val themeStr = prefs.getString("pref_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        val thicknessStr = prefs.getString("pref_progress_bar_thickness", ProgressBarThickness.THIN.name) ?: ProgressBarThickness.THIN.name
        val progressBarThickness = try {
            ProgressBarThickness.valueOf(thicknessStr)
        } catch (e: Exception) {
            ProgressBarThickness.THIN
        }

        return FocusPreferences(
            themeMode = themeMode,
            reelsEnabled = prefs.getBoolean("pref_reels_enabled", false), // OFF by default
            excludeShortsFromDiscover = prefs.getBoolean("pref_exclude_shorts_discover", true),
            excludeShortsFromSearch = prefs.getBoolean("pref_exclude_shorts_search", true),
            autoplay = prefs.getBoolean("pref_autoplay", true),
            autoPlayNext = prefs.getBoolean("pref_autoplay_next", false),
            focusMode = prefs.getBoolean("pref_focus_mode", false),
            backgroundAudioEnabled = prefs.getBoolean("pref_background_audio", true),
            pipEnabled = prefs.getBoolean("pref_pip_enabled", true),
            mediaNotificationsEnabled = prefs.getBoolean("pref_media_notifications", true),
            resumePlaybackPosition = prefs.getBoolean("pref_resume_playback", true),
            preferredSpeed = prefs.getFloat("pref_preferred_speed", 1.0f),
            keepScreenAwake = prefs.getBoolean("pref_keep_screen_awake", true),
            sessionReminderMinutes = prefs.getInt("pref_session_reminder", 0),
            hideComments = prefs.getBoolean("pref_hide_comments", false),
            hideMetrics = prefs.getBoolean("pref_hide_metrics", false),
            grayscaleThumbnails = prefs.getBoolean("pref_grayscale_thumbnails", false),
            progressBarThickness = progressBarThickness
        )
    }

    fun updatePreferences(newPrefs: FocusPreferences) {
        prefs.edit()
            .putString("pref_theme_mode", newPrefs.themeMode.name)
            .putBoolean("pref_reels_enabled", newPrefs.reelsEnabled)
            .putBoolean("pref_exclude_shorts_discover", newPrefs.excludeShortsFromDiscover)
            .putBoolean("pref_exclude_shorts_search", newPrefs.excludeShortsFromSearch)
            .putBoolean("pref_autoplay", newPrefs.autoplay)
            .putBoolean("pref_autoplay_next", newPrefs.autoPlayNext)
            .putBoolean("pref_focus_mode", newPrefs.focusMode)
            .putBoolean("pref_background_audio", newPrefs.backgroundAudioEnabled)
            .putBoolean("pref_pip_enabled", newPrefs.pipEnabled)
            .putBoolean("pref_media_notifications", newPrefs.mediaNotificationsEnabled)
            .putBoolean("pref_resume_playback", newPrefs.resumePlaybackPosition)
            .putFloat("pref_preferred_speed", newPrefs.preferredSpeed)
            .putBoolean("pref_keep_screen_awake", newPrefs.keepScreenAwake)
            .putInt("pref_session_reminder", newPrefs.sessionReminderMinutes)
            .putBoolean("pref_hide_comments", newPrefs.hideComments)
            .putBoolean("pref_hide_metrics", newPrefs.hideMetrics)
            .putBoolean("pref_grayscale_thumbnails", newPrefs.grayscaleThumbnails)
            .putString("pref_progress_bar_thickness", newPrefs.progressBarThickness.name)
            .apply()
        _preferences.value = newPrefs
    }

    fun setProgressBarThickness(thickness: ProgressBarThickness) {
        val current = _preferences.value
        updatePreferences(current.copy(progressBarThickness = thickness))
    }

    // --- Playback Positions ---

    fun savePlaybackPosition(videoId: String, positionMs: Long) {
        if (!_preferences.value.resumePlaybackPosition) return
        playbackPrefs.edit().putLong("pos_$videoId", positionMs).apply()
    }

    fun getPlaybackPosition(videoId: String): Long {
        if (!_preferences.value.resumePlaybackPosition) return 0L
        return playbackPrefs.getLong("pos_$videoId", 0L)
    }

    // --- Watch Later ---

    private fun loadWatchLater(): List<VideoItem> {
        val json = prefs.getString("saved_videos", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VideoItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isWatchLater(videoId: String): Boolean {
        return _watchLater.value.any { it.id == videoId }
    }

    fun toggleWatchLater(video: VideoItem) {
        val current = _watchLater.value.toMutableList()
        val index = current.indexOfFirst { it.id == video.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(0, video)
        }
        _watchLater.value = current
        saveList("saved_videos", current)
    }

    // --- History ---

    private fun loadHistory(): List<VideoItem> {
        val json = prefs.getString("watch_history", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VideoItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordHistory(video: VideoItem) {
        if (_historyPaused.value) return
        val current = _history.value.toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        if (current.size > 100) {
            current.subList(100, current.size).clear()
        }
        _history.value = current
        saveList("watch_history", current)
    }

    fun removeHistoryItem(videoId: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == videoId }
        _history.value = current
        saveList("watch_history", current)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("watch_history").apply()
    }

    fun togglePauseHistory() {
        val newVal = !_historyPaused.value
        _historyPaused.value = newVal
        prefs.edit().putBoolean("history_paused", newVal).apply()
    }

    // --- Subscriptions ---

    private fun loadSubscriptions(): List<ChannelInfo> {
        val json = prefs.getString("local_subscriptions", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChannelInfo>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isSubscribed(channelId: String): Boolean {
        return _subscriptions.value.any { it.id == channelId }
    }

    fun toggleSubscription(channel: ChannelInfo) {
        val current = _subscriptions.value.toMutableList()
        val index = current.indexOfFirst { it.id == channel.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(0, channel.copy(isSubscribed = true))
        }
        _subscriptions.value = current
        saveList("local_subscriptions", current)
    }

    private fun <T> saveList(key: String, list: List<T>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }
}
