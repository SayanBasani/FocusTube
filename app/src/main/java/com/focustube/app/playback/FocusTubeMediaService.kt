package com.focustube.app.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.focustube.app.MainActivity
import com.focustube.app.R

class FocusTubeMediaService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private var currentVideoId: String = ""
    private var currentTitle: String = "FocusTube Audio"
    private var currentChannel: String = "FocusTube"

    companion object {
        const val CHANNEL_ID = "focustube_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.focustube.app.action.PLAY"
        const val ACTION_PAUSE = "com.focustube.app.action.PAUSE"
        const val ACTION_STOP = "com.focustube.app.action.STOP"
        const val ACTION_SEEK_BACK = "com.focustube.app.action.SEEK_BACK"
        const val ACTION_SEEK_FORWARD = "com.focustube.app.action.SEEK_FORWARD"

        fun startService(
            context: Context,
            videoId: String,
            title: String,
            channel: String,
            audioUrl: String = "",
            startPositionMs: Long = 0L
        ) {
            val intent = Intent(context, FocusTubeMediaService::class.java).apply {
                action = ACTION_PLAY
                putExtra("video_id", videoId)
                putExtra("title", title)
                putExtra("channel", channel)
                putExtra("audio_url", audioUrl)
                putExtra("start_position_ms", startPositionMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusTubeMediaService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("FocusTube/1.0 (Linux; Android 15)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateNotification()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            updateNotification()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("FocusTubeMediaService", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}")
                    }
                })
            }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_STOP -> {
                player?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                player?.pause()
                updateNotification()
                return START_STICKY
            }
            ACTION_SEEK_BACK -> {
                player?.let { p ->
                    val newPos = (p.currentPosition - 10_000L).coerceAtLeast(0L)
                    p.seekTo(newPos)
                }
                return START_STICKY
            }
            ACTION_SEEK_FORWARD -> {
                player?.let { p ->
                    val newPos = (p.currentPosition + 10_000L).coerceAtMost(p.duration.coerceAtLeast(0L))
                    p.seekTo(newPos)
                }
                return START_STICKY
            }
        }

        val videoId = intent?.getStringExtra("video_id") ?: ""
        val title = intent?.getStringExtra("title") ?: currentTitle
        val channel = intent?.getStringExtra("channel") ?: currentChannel
        val audioUrl = intent?.getStringExtra("audio_url") ?: ""
        val startPositionMs = intent?.getLongExtra("start_position_ms", 0L) ?: 0L

        currentTitle = title
        currentChannel = channel

        if (videoId.isNotEmpty() && (videoId != currentVideoId || player?.playbackState == Player.STATE_IDLE)) {
            currentVideoId = videoId
            val streamUri = if (audioUrl.isNotEmpty()) {
                Uri.parse(audioUrl)
            } else {
                // Fallback stream for background playback demonstration
                Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(channel)
                .setDisplayTitle(title)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(streamUri)
                .setMediaMetadata(metadata)
                .build()

            player?.setMediaItem(mediaItem, startPositionMs)
            player?.prepare()
            player?.play()
        } else if (action == ACTION_PLAY && player?.isPlaying == false) {
            player?.play()
        }

        startForegroundWithNotification()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundWithNotification() {
        val notification = buildMediaNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildMediaNotification())
    }

    private fun buildMediaNotification(): android.app.Notification {
        val isPlaying = player?.isPlaying == true

        // Play / Pause toggle intent
        val toggleAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val toggleIntent = Intent(this, FocusTubeMediaService::class.java).apply { action = toggleAction }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop intent
        val stopIntent = Intent(this, FocusTubeMediaService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Rewind -10s intent
        val rewindIntent = Intent(this, FocusTubeMediaService::class.java).apply { action = ACTION_SEEK_BACK }
        val rewindPendingIntent = PendingIntent.getService(
            this,
            3,
            rewindIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Forward +10s intent
        val forwardIntent = Intent(this, FocusTubeMediaService::class.java).apply { action = ACTION_SEEK_FORWARD }
        val forwardPendingIntent = PendingIntent.getService(
            this,
            4,
            forwardIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(currentTitle)
            .setContentText(currentChannel)
            .setSubText("Background Playback Active")
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_rew, "-10s", rewindPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_ff, "+10s", forwardPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopPendingIntent)
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FocusTube Background Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background audio and lock-screen playback"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaSession.MediaItemsWithStartPosition> {
            val position = player?.currentPosition ?: 0L
            val item = player?.currentMediaItem
            val items = if (item != null) listOf(item) else emptyList()
            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.MediaSession.MediaItemsWithStartPosition(items, 0, position)
            )
        }
    }
}
