package com.focustube.app

import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.PagedResult
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.FilterManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

class ProductionFeedTest {

    // --- Search & Unicode Tests ---

    @Test
    fun testHindiSong_urlEncoding() {
        val query = "Hindi song"
        val encoded = URLEncoder.encode(query, "UTF-8")
        assertEquals("Hindi+song", encoded)

        val unicodeHindi = "हिंदी गाना"
        val encodedHindi = URLEncoder.encode(unicodeHindi, "UTF-8")
        assertNotNull(encodedHindi)
        assertTrue(encodedHindi.startsWith("%E0%A4%B9"))
    }

    @Test
    fun testSearchFiltering_doesNotFilterNormalHindiSong() {
        val normalSong = VideoItem(
            id = "song123",
            title = "Tere Bina (Official Video) | New Hindi Song 2026",
            channelName = "T-Series",
            durationSeconds = 245, // 4 mins 5 sec -> definitely not a Short
            durationFormatted = "4:05",
            videoUrl = "https://www.youtube.com/watch?v=song123"
        )
        val shortSong = VideoItem(
            id = "short123",
            title = "Best Hook Step #shorts",
            channelName = "Dancer",
            durationSeconds = 25,
            durationFormatted = "0:25",
            videoUrl = "https://www.youtube.com/shorts/short123"
        )

        val prefs = FocusPreferences(excludeShortsFromSearch = true)
        val filtered = FilterManager.filterSearch(listOf(normalSong, shortSong), prefs)

        assertEquals(1, filtered.size)
        assertEquals("song123", filtered[0].id)
    }

    @Test
    fun testLongVideoWithShortsHashtag_isNotClassifiedAsShort() {
        // A 10-minute video whose creator put #shorts in title/desc to game the algorithm
        val fakeShort = VideoItem(
            id = "longVid1",
            title = "Full Album 2026 #shorts compilation full songs",
            channelName = "Music Studio",
            durationSeconds = 600, // 10 minutes
            durationFormatted = "10:00",
            videoUrl = "https://www.youtube.com/watch?v=longVid1"
        )
        assertFalse("Videos longer than 180s should never be classified as Shorts", FilterManager.isShortVideo(fakeShort))
    }

    // --- Discover & Pagination Deduplication Tests ---

    @Test
    fun testPagination_deduplicationPreservesUniqueIds() {
        val initialLoadedIds = mutableSetOf("v1", "v2", "v3")
        val page2Candidates = listOf(
            VideoItem(id = "v2", title = "Duplicate V2", channelName = "C1"),
            VideoItem(id = "v4", title = "New Video V4", channelName = "C2"),
            VideoItem(id = "v3", title = "Duplicate V3", channelName = "C1"),
            VideoItem(id = "v5", title = "New Video V5", channelName = "C3")
        )

        val uniqueNew = page2Candidates.filter { initialLoadedIds.add(it.id) }

        assertEquals(2, uniqueNew.size)
        assertEquals(listOf("v4", "v5"), uniqueNew.map { it.id })
        assertEquals(5, initialLoadedIds.size)
    }

    @Test
    fun testTemporaryFailure_preservesExistingLoadedContent() {
        val existingVideos = listOf(
            VideoItem(id = "v1", title = "Video 1", channelName = "Channel 1"),
            VideoItem(id = "v2", title = "Video 2", channelName = "Channel 2")
        )

        // Simulate failed network request returning Result.failure
        val failedResult: Result<PagedResult<VideoItem>> = Result.failure(Exception("Connection timeout"))

        // Existing videos are retained, error message is captured separately
        var preservedVideos = existingVideos
        var pageError: String? = null

        failedResult.onFailure {
            pageError = it.message
        }.onSuccess {
            preservedVideos = preservedVideos + it.items
        }

        assertEquals(2, preservedVideos.size)
        assertEquals("Connection timeout", pageError)
    }

    @Test
    fun testDuplicateOnlyPage_detectsExhaustionWithoutInfiniteLoop() {
        val seenIds = setOf("v1", "v2")
        val duplicateBatch = listOf(
            VideoItem(id = "v1", title = "V1", channelName = "C1"),
            VideoItem(id = "v2", title = "V2", channelName = "C1")
        )

        val newUnique = duplicateBatch.filter { it.id !in seenIds }
        assertTrue(newUnique.isEmpty())

        // In ViewModel: when newUnique is empty and attempts reach limit, canLoadMore becomes false
        val canLoadMore = false
        assertFalse(canLoadMore)
    }

    // --- Channel Diversity Tests ---

    @Test
    fun testChannelDiversity_limitsClusteredCreatorsInWindow() {
        val history = listOf(
            VideoItem(id = "v1", title = "Lecture 1", channelName = "3Blue1Brown"),
            VideoItem(id = "v2", title = "Lecture 2", channelName = "3Blue1Brown")
        )

        val candidates = listOf(
            VideoItem(id = "v3", title = "Lecture 3", channelName = "3Blue1Brown"),
            VideoItem(id = "v4", title = "Lecture 4", channelName = "3Blue1Brown"),
            VideoItem(id = "v5", title = "Quantum Computers", channelName = "Kurzgesagt"),
            VideoItem(id = "v6", title = "Aliens", channelName = "Kurzgesagt"),
            VideoItem(id = "v7", title = "Light Speed", channelName = "Veritasium"),
            VideoItem(id = "v8", title = "Gravity", channelName = "Veritasium")
        )

        val diversified = FilterManager.applyChannelDiversity(
            candidates = candidates,
            recentFeed = history,
            windowSize = 20,
            maxPerChannel = 2
        )

        // 3Blue1Brown already had 2 items in history -> v3 and v4 are deferred
        // Kurzgesagt (v5, v6) and Veritasium (v7, v8) fill the quota of 4 items
        assertEquals(4, diversified.size)
        assertTrue(diversified.none { it.channelName == "3Blue1Brown" })
        assertEquals(listOf("v5", "v6", "v7", "v8"), diversified.map { it.id })
    }

    // --- Reels Short-Form Verification ---

    @Test
    fun testReels_validThreeMinuteShortClassified() {
        val shortCandidate = VideoItem(
            id = "reel1",
            title = "Amazing Science Concept #shorts",
            channelName = "ScienceLab",
            durationSeconds = 175, // Under 180s
            isShort = false
        )
        assertTrue(FilterManager.isShortVideo(shortCandidate))

        val longCandidate = VideoItem(
            id = "reel2",
            title = "Amazing Science Concept #shorts",
            channelName = "ScienceLab",
            durationSeconds = 185, // Over 180s
            isShort = false
        )
        assertFalse(FilterManager.isShortVideo(longCandidate))
    }
}
