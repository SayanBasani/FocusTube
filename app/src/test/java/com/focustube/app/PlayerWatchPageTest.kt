package com.focustube.app

import com.focustube.app.data.model.CommentItem
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.LiveStatus
import com.focustube.app.data.model.ProgressBarThickness
import com.focustube.app.data.model.VideoCommentsData
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.network.YouTubeApiClient
import com.focustube.app.data.repository.FilterManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerWatchPageTest {

    @Test
    fun testCommentsData_defaultStateIsIdle() {
        val data = VideoCommentsData()
        assertEquals(CommentStatus.IDLE, data.status)
        assertTrue(data.comments.isEmpty())
        assertEquals(0, data.totalCount)
    }

    @Test
    fun testCommentsData_disabledStatusRecognized() {
        val data = VideoCommentsData(
            status = CommentStatus.DISABLED,
            errorMessage = "Comments are turned off for this video."
        )
        assertEquals(CommentStatus.DISABLED, data.status)
        assertEquals("Comments are turned off for this video.", data.errorMessage)
    }

    @Test
    fun testCommentsData_emptyStatusDistinguishedFromError() {
        val emptyData = VideoCommentsData(
            status = CommentStatus.EMPTY,
            totalCount = 0,
            totalCountText = "0",
            comments = emptyList()
        )
        assertEquals(CommentStatus.EMPTY, emptyData.status)

        val errorData = VideoCommentsData(
            status = CommentStatus.ERROR,
            errorMessage = "Network timeout"
        )
        assertEquals(CommentStatus.ERROR, errorData.status)
        assertTrue(emptyData.status != errorData.status)
    }

    @Test
    fun testRelatedVideos_excludesShortsViaFilterManager() {
        val mixedVideos = listOf(
            VideoItem(
                id = "long_1",
                title = "Deep Learning Chapter 1",
                channelName = "3Blue1Brown",
                durationSeconds = 1200,
                isShort = false
            ),
            VideoItem(
                id = "short_1",
                title = "Quick Trick #shorts",
                channelName = "Quickie",
                durationSeconds = 30,
                isShort = true
            ),
            VideoItem(
                id = "long_2",
                title = "Quantum Physics Lecture",
                channelName = "Physics Channel",
                durationSeconds = 2400,
                isShort = false
            )
        )

        val prefs = FocusPreferences(excludeShortsFromDiscover = true)
        val filtered = FilterManager.filterDiscover(mixedVideos, prefs)

        assertEquals(2, filtered.size)
        assertTrue(filtered.none { it.isShort || it.title.contains("#shorts") })
        assertEquals("long_1", filtered[0].id)
        assertEquals("long_2", filtered[1].id)
    }

    @Test
    fun testVideoItem_richMetadataAndChannelAvatar() {
        val item = VideoItem(
            id = "test_1",
            title = "Sample Video",
            channelName = "Sample Channel",
            channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_sample",
            subscriberCountText = "1.5M subscribers",
            likeCountText = "45K",
            audioStreamUrl = "https://example.com/audio.mp4"
        )
        assertEquals("https://yt3.googleusercontent.com/ytc/AIdro_sample", item.channelAvatarUrl)
        assertEquals("1.5M subscribers", item.subscriberCountText)
        assertEquals("45K", item.likeCountText)
        assertEquals("https://example.com/audio.mp4", item.audioStreamUrl)
    }

    @Test
    fun testCuratedFallback_includesChannelAvatars() {
        val client = YouTubeApiClient()
        val curated = client.getCuratedEducationalFallback()
        assertTrue(curated.isNotEmpty())
        for (item in curated) {
            assertTrue("Item ${item.title} must have a valid HTTPS channel avatar", item.channelAvatarUrl.startsWith("https://"))
            assertTrue("Item ${item.title} must have a valid HTTPS audio stream URL", item.audioStreamUrl.startsWith("https://"))
        }
    }

    @Test
    fun testLiveStatus_detectionAndDistinction() {
        val liveVideo = VideoItem(
            id = "live_1",
            title = "Live Stream Broadcast",
            channelName = "News Channel",
            liveStatus = LiveStatus.LIVE,
            durationFormatted = "LIVE"
        )
        val vodVideo = VideoItem(
            id = "vod_1",
            title = "Completed Broadcast Recording",
            channelName = "News Channel",
            liveStatus = LiveStatus.POST_LIVE,
            durationSeconds = 3600,
            durationFormatted = "1:00:00"
        )
        val normalVideo = VideoItem(
            id = "norm_1",
            title = "Ordinary Uploaded Video",
            channelName = "Regular Channel",
            liveStatus = LiveStatus.NONE,
            durationSeconds = 600,
            durationFormatted = "10:00"
        )

        assertTrue("liveVideo must report isLive = true", liveVideo.isLive)
        assertFalse("vodVideo must report isLive = false", vodVideo.isLive)
        assertFalse("normalVideo must report isLive = false", normalVideo.isLive)
        assertEquals("LIVE", liveVideo.durationFormatted)
        assertEquals("1:00:00", vodVideo.durationFormatted)
    }

    @Test
    fun testProgressBarThickness_valuesAndDefaults() {
        val defaultPrefs = FocusPreferences()
        assertEquals(ProgressBarThickness.THIN, defaultPrefs.progressBarThickness)
        assertEquals(2, ProgressBarThickness.THIN.trackDp)
        assertEquals(8, ProgressBarThickness.THIN.thumbDp)
        assertEquals(4, ProgressBarThickness.NORMAL.trackDp)
        assertEquals(12, ProgressBarThickness.NORMAL.thumbDp)
        assertEquals(6, ProgressBarThickness.THICK.trackDp)
        assertEquals(16, ProgressBarThickness.THICK.thumbDp)
    }

    @Test
    fun testCuratedFallback_includesLiveAndVodStreams() {
        val client = YouTubeApiClient()
        val curated = client.getCuratedEducationalFallback()
        val hasLive = curated.any { it.liveStatus == LiveStatus.LIVE && it.isLive }
        val hasVod = curated.any { it.liveStatus == LiveStatus.POST_LIVE }

        assertTrue("Curated list should contain an active live stream for testing", hasLive)
        assertTrue("Curated list should contain a post-live VOD stream for testing", hasVod)
    }

    @Test
    fun testPlayerSheetState_transitions() {
        val hidden = com.focustube.app.ui.screens.player.PlayerSheetState.HIDDEN
        val collapsed = com.focustube.app.ui.screens.player.PlayerSheetState.COLLAPSED
        val expanded = com.focustube.app.ui.screens.player.PlayerSheetState.EXPANDED

        // Ensure distinct states exist
        assertTrue(hidden != collapsed)
        assertTrue(collapsed != expanded)
        assertTrue(hidden != expanded)

        // Initial default state in PlayerUiState
        val uiState = com.focustube.app.ui.screens.player.PlayerUiState()
        assertEquals(com.focustube.app.ui.screens.player.PlayerSheetState.HIDDEN, uiState.sheetState)

        // Collapsed state
        val collapsedUiState = uiState.copy(sheetState = com.focustube.app.ui.screens.player.PlayerSheetState.COLLAPSED)
        assertEquals(com.focustube.app.ui.screens.player.PlayerSheetState.COLLAPSED, collapsedUiState.sheetState)

        // Expanded state
        val expandedUiState = uiState.copy(sheetState = com.focustube.app.ui.screens.player.PlayerSheetState.EXPANDED)
        assertEquals(com.focustube.app.ui.screens.player.PlayerSheetState.EXPANDED, expandedUiState.sheetState)
    }

    @Test
    fun testDeveloperConstants_values() {
        assertEquals("Sayan Basani", com.focustube.app.util.DeveloperConstants.DEVELOPER_NAME)
        assertEquals("Developer of FocusTube", com.focustube.app.util.DeveloperConstants.DEVELOPER_ROLE)
        assertEquals("https://github.com/sayanbasani", com.focustube.app.util.DeveloperConstants.GITHUB_PROFILE_URL)
        assertEquals("https://www.linkedin.com/in/sayanbasani/", com.focustube.app.util.DeveloperConstants.LINKEDIN_PROFILE_URL)
        assertEquals("https://github.com/SayanBasani/FocusTube", com.focustube.app.util.DeveloperConstants.REPOSITORY_URL)
        assertEquals("https://github.com/SayanBasani/FocusTube.git", com.focustube.app.util.DeveloperConstants.REPOSITORY_GIT_URL)
        assertEquals("Made by Sayan Basani", com.focustube.app.util.DeveloperConstants.ATTRIBUTION)
        assertTrue(com.focustube.app.util.DeveloperConstants.APP_DESCRIPTION.contains("distraction-aware"))
    }
}
