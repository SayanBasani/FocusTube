package com.focustube.app

import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.FilterManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterManagerTest {

    @Test
    fun testIsShortVideo_identifiesDirectShortFlag() {
        val short = VideoItem(
            id = "test1",
            title = "Normal Title",
            channelName = "Channel",
            isShort = true
        )
        assertTrue(FilterManager.isShortVideo(short))
    }

    @Test
    fun testIsShortVideo_identifiesShortsUrl() {
        val short = VideoItem(
            id = "test2",
            title = "Fun Video",
            channelName = "Channel",
            videoUrl = "https://www.youtube.com/shorts/abc12345678"
        )
        assertTrue(FilterManager.isShortVideo(short))
    }

    @Test
    fun testIsShortVideo_identifiesShortsHashtag() {
        val short = VideoItem(
            id = "test3",
            title = "Check this trick out! #shorts",
            channelName = "Channel",
            durationSeconds = 45
        )
        assertTrue(FilterManager.isShortVideo(short))
    }

    @Test
    fun testIsShortVideo_retainsLongFormVideos() {
        val longForm = VideoItem(
            id = "aircAruvnKk",
            title = "But what is a neural network? | Deep learning chapter 1",
            channelName = "3Blue1Brown",
            durationSeconds = 1152,
            durationFormatted = "19:12",
            videoUrl = "https://www.youtube.com/watch?v=aircAruvnKk"
        )
        assertFalse(FilterManager.isShortVideo(longForm))
    }

    @Test
    fun testFilterDiscover_excludesShortsWhenEnabled() {
        val videos = listOf(
            VideoItem(id = "1", title = "Lecture 1", channelName = "MIT", durationSeconds = 3600),
            VideoItem(id = "2", title = "Crazy Dance #shorts", channelName = "Dancer", durationSeconds = 30),
            VideoItem(id = "3", title = "Documentary", channelName = "BBC", durationSeconds = 2700)
        )
        val prefs = FocusPreferences(excludeShortsFromDiscover = true)
        val filtered = FilterManager.filterDiscover(videos, prefs)

        assertEquals(2, filtered.size)
        assertEquals("1", filtered[0].id)
        assertEquals("3", filtered[1].id)
    }

    @Test
    fun testFilterSearch_excludesShortsWhenEnabled() {
        val videos = listOf(
            VideoItem(id = "s1", title = "Cooking 101", channelName = "Chef", durationSeconds = 900),
            VideoItem(id = "s2", title = "Quick Chop #shorts", channelName = "Chef", durationSeconds = 25)
        )
        val prefs = FocusPreferences(excludeShortsFromSearch = true)
        val filtered = FilterManager.filterSearch(videos, prefs)

        assertEquals(1, filtered.size)
        assertEquals("s1", filtered[0].id)
    }

    @Test
    fun testExtractReels_filtersOnlyShorts() {
        val videos = listOf(
            VideoItem(id = "1", title = "Full Lecture", channelName = "Uni", durationSeconds = 3600),
            VideoItem(id = "2", title = "Quick Tip #shorts", channelName = "Creator", durationSeconds = 25, isShort = true)
        )
        val reels = FilterManager.extractReels(videos)
        assertEquals(1, reels.size)
        assertEquals("2", reels[0].id)
    }

    @Test
    fun testExtractVideoId_handlesVariousFormats() {
        assertEquals("aircAruvnKk", FilterManager.extractVideoId("https://www.youtube.com/watch?v=aircAruvnKk"))
        assertEquals("abc12345678", FilterManager.extractVideoId("https://www.youtube.com/shorts/abc12345678"))
        assertEquals("9bZkp7q19f0", FilterManager.extractVideoId("https://youtu.be/9bZkp7q19f0"))
        assertNull(FilterManager.extractVideoId("https://google.com/about"))
    }

    @Test
    fun testSanitizeSpeed_supportsUpToThreeX() {
        assertEquals(1.0f, FilterManager.sanitizeSpeed(1.0f), 0.001f)
        assertEquals(2.0f, FilterManager.sanitizeSpeed(2.0f), 0.001f)
        assertEquals(3.0f, FilterManager.sanitizeSpeed(3.0f), 0.001f)
        assertEquals(0.25f, FilterManager.sanitizeSpeed(0.25f), 0.001f)
        assertEquals(1.5f, FilterManager.sanitizeSpeed(1.5f), 0.001f)
        assertEquals(12, FilterManager.SUPPORTED_SPEEDS.size)
    }
}
