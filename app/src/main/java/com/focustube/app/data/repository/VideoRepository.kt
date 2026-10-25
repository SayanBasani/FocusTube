package com.focustube.app.data.repository

import com.focustube.app.data.model.ChannelInfo
import com.focustube.app.data.model.CommentItem
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.VideoCommentsData
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.network.YouTubeApiClient

class VideoRepository(
    private val apiClient: YouTubeApiClient,
    private val storageRepo: LocalStorageRepository
) {

    suspend fun getHomeVideos(category: String = "All", page: Int = 1): Result<List<VideoItem>> {
        val result = if (category == "All") {
            apiClient.getTrendingVideos(page = page, category = category)
        } else {
            apiClient.searchVideos(category, page = page).map { it.first }
        }

        return result.map { list ->
            FilterManager.filterDiscover(list, storageRepo.preferences.value)
        }
    }

    suspend fun searchVideos(query: String, page: Int = 1): Result<List<VideoItem>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return Result.success(emptyList())

        val result = apiClient.searchVideos(cleanQuery, page = page).map { it.first }
        return result.map { list ->
            FilterManager.filterSearch(list, storageRepo.preferences.value)
        }
    }

    suspend fun getReelsVideos(page: Int = 1): Result<List<VideoItem>> {
        val result = apiClient.searchVideos("Shorts", page = page).map { it.first }
        return result.map { list ->
            val reels = FilterManager.extractReels(list)
            if (reels.isNotEmpty()) {
                reels
            } else {
                apiClient.getCuratedShorts()
            }
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> {
        return apiClient.getVideoDetails(videoId)
    }

    suspend fun getRelatedVideos(videoId: String, title: String): Result<List<VideoItem>> {
        if (storageRepo.preferences.value.focusMode) {
            return Result.success(emptyList())
        }
        val result = apiClient.getRelatedVideos(videoId, title)
        return result.map { list ->
            FilterManager.filterDiscover(list, storageRepo.preferences.value)
        }
    }

    suspend fun getChannelVideos(channelId: String): Result<List<VideoItem>> {
        val result = apiClient.getChannelVideos(channelId)
        return result.map { list ->
            FilterManager.filterDiscover(list, storageRepo.preferences.value)
        }
    }

    suspend fun getComments(videoId: String): Result<VideoCommentsData> {
        if (storageRepo.preferences.value.hideComments) {
            return Result.success(
                VideoCommentsData(
                    status = CommentStatus.DISABLED,
                    errorMessage = "Comments are hidden by FocusTube settings."
                )
            )
        }
        return apiClient.getComments(videoId)
    }

    suspend fun getSubscriptionFeed(): Result<List<VideoItem>> {
        val subs = storageRepo.subscriptions.value
        if (subs.isEmpty()) {
            return Result.success(emptyList())
        }

        val allVideos = mutableListOf<VideoItem>()
        for (sub in subs) {
            val res = apiClient.getChannelVideos(sub.id)
            res.getOrNull()?.let { allVideos.addAll(it) }
        }

        val filtered = FilterManager.filterDiscover(allVideos, storageRepo.preferences.value)
        return Result.success(filtered)
    }
}
