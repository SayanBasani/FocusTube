package com.focustube.app.data.repository

import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem

object FilterManager {

    val SUPPORTED_SPEEDS = listOf(
        0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f, 2.75f, 3.0f
    )

    private val SHORTS_URL_REGEX = Regex("(?:youtube\\.com/shorts/|youtu\\.be/shorts/)([a-zA-Z0-9_-]{11})")
    private val SHORTS_TAG_REGEX = Regex("(?i)#shorts?\\b|#reel\\b")

    /**
     * Determines whether a video is a YouTube Short based on:
     * 1. Direct metadata flag (isShort)
     * 2. URL pattern (/shorts/{id})
     * 3. Title or description hashtag (#shorts, #reel)
     * 4. Duration <= 180s (YouTube 2024+ 3-minute Shorts support) with shorts indicators
     */
    fun isShortVideo(item: VideoItem): Boolean {
        if (item.isShort) return true
        if (SHORTS_URL_REGEX.containsMatchIn(item.videoUrl)) return true
        if (SHORTS_TAG_REGEX.containsMatchIn(item.title)) return true
        if (SHORTS_TAG_REGEX.containsMatchIn(item.description)) return true
        if (item.durationSeconds in 1..180 && item.title.contains(Regex("(?i)\\bshorts?\\b|#shorts?\\b|#reel\\b"))) {
            return true
        }
        return false
    }

    /**
     * Applies a soft channel diversity filter.
     * In a rolling window of recent videos (default 20), aims to show at most
     * maxPerChannel videos (default 2) from any single channel.
     * If too few unique channels exist, gradually relaxes the restriction
     * to prevent feed starvation.
     */
    fun applyChannelDiversity(
        candidates: List<VideoItem>,
        recentFeed: List<VideoItem>,
        windowSize: Int = 20,
        maxPerChannel: Int = 2
    ): List<VideoItem> {
        val recentWindow = recentFeed.takeLast(windowSize)
        val channelCounts = mutableMapOf<String, Int>()

        for (v in recentWindow) {
            val key = v.channelId?.takeIf { it.isNotBlank() } ?: v.channelName.trim().lowercase()
            if (key.isNotBlank()) {
                channelCounts[key] = (channelCounts[key] ?: 0) + 1
            }
        }

        val accepted = mutableListOf<VideoItem>()
        val deferred = mutableListOf<VideoItem>()

        for (candidate in candidates) {
            val key = candidate.channelId?.takeIf { it.isNotBlank() } ?: candidate.channelName.trim().lowercase()
            val currentCount = if (key.isNotBlank()) channelCounts[key] ?: 0 else 0

            if (key.isBlank() || currentCount < maxPerChannel) {
                accepted.add(candidate)
                if (key.isNotBlank()) {
                    channelCounts[key] = currentCount + 1
                }
            } else {
                deferred.add(candidate)
            }
        }

        // Soft fallback: if accepted items are too few (< 4) and we have deferred candidates,
        // relax the quota so the user is never starved of content.
        if (accepted.size < 4 && deferred.isNotEmpty()) {
            val needed = 4 - accepted.size
            accepted.addAll(deferred.take(needed))
        }

        return accepted
    }

    /**
     * Filters Discover feed videos according to configured distraction rules.
     */
    fun filterDiscover(
        videos: List<VideoItem>,
        preferences: FocusPreferences
    ): List<VideoItem> {
        return videos.mapNotNull { video ->
            if (preferences.excludeShortsFromDiscover && isShortVideo(video)) {
                null
            } else {
                applyMetricClean(video, preferences)
            }
        }
    }

    /**
     * Filters Search results according to configured distraction rules.
     */
    fun filterSearch(
        videos: List<VideoItem>,
        preferences: FocusPreferences
    ): List<VideoItem> {
        return videos.mapNotNull { video ->
            if (preferences.excludeShortsFromSearch && isShortVideo(video)) {
                null
            } else {
                applyMetricClean(video, preferences)
            }
        }
    }

    /**
     * Extracts only short-form content for the isolated Reels section.
     */
    fun extractReels(videos: List<VideoItem>): List<VideoItem> {
        return videos.filter { isShortVideo(it) }
    }

    private fun applyMetricClean(video: VideoItem, preferences: FocusPreferences): VideoItem {
        return if (preferences.hideMetrics) {
            video.copy(
                viewCountText = "",
                publishedText = ""
            )
        } else {
            video
        }
    }

    /**
     * Sanitizes external URLs: if a user clicks or enters a Shorts URL,
     * extracts the underlying 11-char video ID so it can be handled safely.
     */
    fun extractVideoId(url: String): String? {
        val shortsMatch = SHORTS_URL_REGEX.find(url)
        if (shortsMatch != null) {
            return shortsMatch.groupValues[1]
        }
        val watchRegex = Regex("(?:v=|youtu\\.be/|embed/)([a-zA-Z0-9_-]{11})")
        return watchRegex.find(url)?.groupValues?.get(1)
    }

    /**
     * Validates and snaps a playback speed to the nearest supported speed up to 3.0x.
     */
    fun sanitizeSpeed(speed: Float): Float {
        return if (speed in SUPPORTED_SPEEDS) {
            speed
        } else {
            SUPPORTED_SPEEDS.minByOrNull { kotlin.math.abs(it - speed) } ?: 1.0f
        }
    }
}
