package com.focustube.app

import android.app.Application
import com.focustube.app.data.network.YouTubeApiClient
import com.focustube.app.data.repository.LocalStorageRepository
import com.focustube.app.data.repository.VideoRepository

class FocusTubeApp : Application() {

    lateinit var apiClient: YouTubeApiClient
        private set

    lateinit var storageRepository: LocalStorageRepository
        private set

    lateinit var videoRepository: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        apiClient = YouTubeApiClient()
        storageRepository = LocalStorageRepository(this)
        videoRepository = VideoRepository(apiClient, storageRepository)
    }

    companion object {
        lateinit var instance: FocusTubeApp
            private set
    }
}
