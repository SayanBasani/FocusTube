package com.focustube.app.data.model

enum class LiveStatus {
    NONE,        // Normal uploaded video
    LIVE,        // Currently active livestream
    POST_LIVE,   // Completed livestream recording (VOD)
    PREMIERE     // Upcoming or active premiere
}

enum class ProgressBarThickness(val displayName: String, val trackDp: Int, val thumbDp: Int) {
    THIN("Thin (2 dp)", 2, 8),
    NORMAL("Normal (4 dp)", 4, 12),
    THICK("Thick (6 dp)", 6, 16)
}

data class VideoItem(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String? = null,
    val channelAvatarUrl: String = "",
    val subscriberCountText: String = "",
    val durationSeconds: Long = 0,
    val durationFormatted: String = "",
    val thumbnailUrl: String = "",
    val viewCountText: String = "",
    val publishedText: String = "",
    val description: String = "",
    val likeCountText: String = "",
    val audioStreamUrl: String = "",
    val isShort: Boolean = false,
    val liveStatus: LiveStatus = LiveStatus.NONE,
    val videoUrl: String = "https://www.youtube.com/watch?v=$id"
) {
    val isLive: Boolean get() = liveStatus == LiveStatus.LIVE
}

data class ChannelInfo(
    val id: String,
    val name: String,
    val avatarUrl: String = "",
    val subscriberCountText: String = "",
    val description: String = "",
    val isSubscribed: Boolean = false
)

data class CommentItem(
    val id: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val content: String,
    val publishedTime: String = "",
    val likeCountText: String = ""
)

enum class CommentStatus {
    IDLE,
    LOADING,
    SUCCESS,
    EMPTY,       // Verified that video has 0 comments
    DISABLED,    // Comments are turned off by the creator
    ERROR,       // Network or server error
    RESTRICTED   // Restricted or unavailable on this instance
}

data class VideoCommentsData(
    val status: CommentStatus = CommentStatus.IDLE,
    val comments: List<CommentItem> = emptyList(),
    val totalCount: Int = 0,
    val totalCountText: String = "",
    val errorMessage: String? = null
)

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

data class FocusPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reelsEnabled: Boolean = false, // Strictly OFF by default
    val excludeShortsFromDiscover: Boolean = true,
    val excludeShortsFromSearch: Boolean = true,
    val autoplay: Boolean = true, // Autoplay on watch page open
    val autoPlayNext: Boolean = false, // OFF by default
    val focusMode: Boolean = false,
    val backgroundAudioEnabled: Boolean = true,
    val pipEnabled: Boolean = true,
    val mediaNotificationsEnabled: Boolean = true,
    val resumePlaybackPosition: Boolean = true,
    val preferredSpeed: Float = 1.0f,
    val keepScreenAwake: Boolean = true,
    val sessionReminderMinutes: Int = 0,
    val hideComments: Boolean = false,
    val hideMetrics: Boolean = false,
    val grayscaleThumbnails: Boolean = false,
    val progressBarThickness: ProgressBarThickness = ProgressBarThickness.THIN
)
