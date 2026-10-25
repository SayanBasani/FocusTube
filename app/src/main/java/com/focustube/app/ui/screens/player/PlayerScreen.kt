package com.focustube.app.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CommentsDisabled
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.focustube.app.MainActivity
import com.focustube.app.data.model.CommentItem
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.ProgressBarThickness
import com.focustube.app.data.model.VideoItem
import com.focustube.app.data.repository.FilterManager
import com.focustube.app.playback.FocusTubeMediaService
import com.focustube.app.ui.components.ChannelAvatarView
import com.focustube.app.ui.components.ErrorView
import com.focustube.app.ui.components.LoadingView
import com.focustube.app.ui.components.RelatedVideoItemView
import com.focustube.app.ui.theme.FocusBlue
import com.focustube.app.ui.theme.FocusTeal
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    videoId: String,
    viewModel: PlayerViewModel,
    isPiPMode: Boolean = false,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit = {},
    onNavigateToChannel: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val watchLaterList by viewModel.watchLater.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Overlay auto-hide state (Section 2)
    var controlsVisible by remember { mutableStateOf(true) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Start video details loading
    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
        controlsVisible = true
    }

    // Auto-hide controls after 3.5 seconds of inactivity when playing
    LaunchedEffect(controlsVisible, isUserInteracting, uiState.isPlaying, uiState.isSettingsSheetOpen) {
        if (controlsVisible && uiState.isPlaying && !isUserInteracting && !uiState.isSettingsSheetOpen) {
            delay(3500L)
            controlsVisible = false
        }
    }

    // Stop MediaService and clean up WebView on leaving PlayerScreen (Fix for Bug 2)
    DisposableEffect(videoId) {
        onDispose {
            try {
                webViewRef?.evaluateJavascript("if (window.player && player.pauseVideo) player.pauseVideo();", null)
                webViewRef?.stopLoading()
                webViewRef?.destroy()
            } catch (e: Exception) {}
            webViewRef = null
            FocusTubeMediaService.stopService(context)
        }
    }

    // Lifecycle observer for background audio playback when app minimized/locked
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, preferences.backgroundAudioEnabled, uiState.isPlaying, uiState.video) {
        val observer = LifecycleEventObserver { _, event ->
            val v = uiState.video
            if (event == Lifecycle.Event.ON_STOP) {
                if (preferences.backgroundAudioEnabled && uiState.isPlaying && !isPiPMode && v != null) {
                    FocusTubeMediaService.startService(
                        context = context,
                        videoId = v.id,
                        title = v.title,
                        channel = v.channelName,
                        audioUrl = v.audioStreamUrl,
                        startPositionMs = uiState.currentPositionSeconds * 1000L
                    )
                }
            } else if (event == Lifecycle.Event.ON_START) {
                if (preferences.backgroundAudioEnabled) {
                    FocusTubeMediaService.stopService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep screen awake handling
    DisposableEffect(preferences.keepScreenAwake) {
        if (preferences.keepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sleep timer pause observer
    LaunchedEffect(uiState.isSleepTimerActive, uiState.sleepTimerSecondsRemaining) {
        if (uiState.isSleepTimerActive && uiState.sleepTimerSecondsRemaining == 0 && uiState.sleepTimerMinutes > 0) {
            webViewRef?.evaluateJavascript("if (window.player && player.pauseVideo) player.pauseVideo();", null)
            viewModel.setPlayingState(false)
            FocusTubeMediaService.stopService(context)
        }
    }

    // Playback rate synchronization
    LaunchedEffect(uiState.currentSpeed) {
        webViewRef?.evaluateJavascript("if (window.player && player.setPlaybackRate) player.setPlaybackRate(${uiState.currentSpeed});", null)
    }

    fun enterFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun toggleFullscreen() {
        if (isLandscape) exitFullscreen() else enterFullscreen()
    }

    fun togglePlayPause() {
        if (uiState.isPlaying) {
            webViewRef?.evaluateJavascript("if (window.player && player.pauseVideo) player.pauseVideo();", null)
            viewModel.setPlayingState(false)
            FocusTubeMediaService.stopService(context)
        } else {
            webViewRef?.evaluateJavascript("if (window.player && player.playVideo) player.playVideo();", null)
            viewModel.setPlayingState(true)
        }
    }

    fun seekTo(seconds: Long) {
        viewModel.updatePlaybackPosition(seconds)
        webViewRef?.evaluateJavascript("if (window.player && player.seekTo) player.seekTo($seconds, true);", null)
    }

    fun seekRelative(seconds: Int) {
        val target = (uiState.currentPositionSeconds + seconds).coerceAtLeast(0L)
        seekTo(target)
    }

    BackHandler {
        if (isLandscape) {
            exitFullscreen()
        } else if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else {
            onBack()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (customView != null) {
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Compact Top Toolbar (Portrait only, uncrowded single settings button)
                if (!isLandscape && !isPiPMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // App / Focus Brand Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = FocusTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FocusTube",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Action buttons: Picture-in-Picture & Settings
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (preferences.pipEnabled) {
                                IconButton(onClick = { (activity as? MainActivity)?.enterPipMode() }) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPicture,
                                        contentDescription = "Picture-in-Picture",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.setSettingsSheetOpen(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Player Settings",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                // 2. Video Player Surface with Auto-Hiding Controls & Gestures
                val playerModifier = if (isLandscape || isPiPMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }

                Box(
                    modifier = playerModifier
                        // Vertical swipe gestures: UP -> Enter Fullscreen, DOWN -> Exit Fullscreen
                        .pointerInput(isLandscape) {
                            var totalDragY = 0f
                            var totalDragX = 0f
                            detectDragGestures(
                                onDragStart = {
                                    totalDragY = 0f
                                    totalDragX = 0f
                                    isUserInteracting = true
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragY += dragAmount.y
                                    totalDragX += dragAmount.x
                                },
                                onDragEnd = {
                                    isUserInteracting = false
                                    if (abs(totalDragY) > abs(totalDragX) * 1.3f && abs(totalDragY) > 70f) {
                                        if (totalDragY < 0 && !isLandscape) {
                                            enterFullscreen()
                                        } else if (totalDragY > 0 && isLandscape) {
                                            exitFullscreen()
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    // Underneath: Clean Privacy Player Embed
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.cacheMode = WebSettings.LOAD_DEFAULT

                                webChromeClient = object : WebChromeClient() {
                                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                        customView = view
                                        customViewCallback = callback
                                    }

                                    override fun onHideCustomView() {
                                        customView = null
                                        customViewCallback = null
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        return false
                                    }
                                }

                                addJavascriptInterface(
                                    FocusTubeBridge(
                                        onReady = {
                                            viewModel.setPlayerReady(true)
                                        },
                                        onStateChange = { state ->
                                            when (state) {
                                                1 -> {
                                                    viewModel.setPlayingState(true)
                                                    viewModel.setBuffering(false)
                                                    viewModel.setPlayerReady(true)
                                                }
                                                2 -> {
                                                    viewModel.setPlayingState(false)
                                                    viewModel.setBuffering(false)
                                                }
                                                3 -> {
                                                    viewModel.setBuffering(true)
                                                }
                                                5 -> {
                                                    viewModel.setPlayerReady(true)
                                                    viewModel.setBuffering(false)
                                                }
                                                0 -> {
                                                    viewModel.setPlayingState(false)
                                                    viewModel.setBuffering(false)
                                                    FocusTubeMediaService.stopService(context)
                                                }
                                            }
                                        },
                                        onTimeUpdate = { cur, dur, loaded ->
                                            viewModel.updatePlaybackPosition(cur.toLong())
                                            viewModel.updateDuration(dur.toLong())
                                            viewModel.updateBufferedFraction(loaded)
                                            viewModel.savePlaybackPosition(cur.toLong())
                                        },
                                        onLiveInfo = { isLive, behindEdge, delta ->
                                            viewModel.setLiveInfo(isLive, behindEdge, delta)
                                        }
                                    ),
                                    "FocusTubeBridge"
                                )

                                val savedPosition = viewModel.getSavedPositionSeconds()
                                val html = buildEmbedHtml(videoId, preferences.autoplay, uiState.currentSpeed, savedPosition)
                                loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Loading / Buffering indicator over WebView
                    if (!uiState.isPlayerReady || uiState.isBuffering) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    // Persistent thin 2 dp red progress line at the very bottom when controls are hidden
                    if (!controlsVisible && !isPiPMode && uiState.durationSeconds > 0 && !uiState.isLive) {
                        val progress = (uiState.currentPositionSeconds.toFloat() / uiState.durationSeconds).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(Color.Red)
                            )
                        }
                    }

                    // Tapping player area when controls are hidden reveals controls
                    if (!controlsVisible && !isPiPMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    controlsVisible = true
                                }
                        )
                    }

                    // Application-Controlled Overlay: Auto-hides on inactivity
                    if (!isPiPMode) {
                        VideoPlayerControlsOverlay(
                            visible = controlsVisible,
                            videoTitle = uiState.video?.title ?: "",
                            isLandscape = isLandscape,
                            isPlaying = uiState.isPlaying,
                            isPlayerReady = uiState.isPlayerReady,
                            currentPositionSeconds = uiState.currentPositionSeconds,
                            durationSeconds = uiState.durationSeconds,
                            bufferedFraction = uiState.bufferedFraction,
                            thickness = preferences.progressBarThickness,
                            isLive = uiState.isLive,
                            isBehindLiveEdge = uiState.isBehindLiveEdge,
                            onGoLive = {
                                webViewRef?.evaluateJavascript(
                                    "if (window.player && player.getSeekableRange) { var r = player.getSeekableRange(); if (r && r.end) player.seekTo(r.end, true); }",
                                    null
                                )
                            },
                            onBackClick = { exitFullscreen() },
                            onSettingsClick = { viewModel.setSettingsSheetOpen(true) },
                            onSeekRelative = { delta -> seekRelative(delta) },
                            onSeekTo = { pos -> seekTo(pos) },
                            onTogglePlayPause = { togglePlayPause() },
                            onToggleFullscreen = { toggleFullscreen() },
                            onOverlayClick = { controlsVisible = false }
                        )
                    }
                }

                // 3. YouTube-Inspired Watch Page Content (Portrait)
                if (!isLandscape && !isPiPMode) {
                    val video = uiState.video
                    if (video != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            // Sleep Timer Notification Banner if Active
                            if (uiState.isSleepTimerActive) {
                                item {
                                    val minutes = uiState.sleepTimerSecondsRemaining / 60
                                    val seconds = uiState.sleepTimerSecondsRemaining % 60
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .background(FocusTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Timer, contentDescription = null, tint = FocusTeal, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Study Timer: %02d:%02d remaining".format(minutes, seconds),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FocusTeal,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.cancelSleepTimer() },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Cancel", color = FocusTeal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            // Title & Quick Metadata
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (uiState.isLive || video.isLive) {
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFFCC0000), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color.White, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "LIVE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = video.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (!preferences.hideMetrics && (video.viewCountText.isNotEmpty() || video.publishedText.isNotEmpty())) {
                                        val meta = listOf(video.viewCountText, video.publishedText)
                                            .filter { it.isNotEmpty() }
                                            .joinToString(" • ")
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Action Buttons Row (Like, Dislike, Share, Save)
                            item {
                                val isSaved = watchLaterList.any { it.id == video.id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Like / Dislike Combined Pill
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { viewModel.toggleLike() }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                                contentDescription = "Like",
                                                tint = if (uiState.isLiked) FocusBlue else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val countText = if (video.likeCountText.isNotEmpty()) video.likeCountText else if (uiState.isLiked) "1" else "Like"
                                            Text(
                                                text = countText,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (uiState.isLiked) FocusBlue else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(16.dp)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { viewModel.toggleDislike() }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                                contentDescription = "Dislike",
                                                tint = if (uiState.isDisliked) FocusBlue else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Share Pill
                                    WatchActionPill(
                                        icon = Icons.Default.Share,
                                        label = "Share",
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$videoId")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                                        }
                                    )

                                    // Save to Watch Later Pill
                                    WatchActionPill(
                                        icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        label = if (isSaved) "Saved" else "Save",
                                        isActive = isSaved,
                                        onClick = { viewModel.toggleWatchLater(video) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Channel Information Row
                            item {
                                val isSubscribed = subscriptions.any { it.id == (video.channelId ?: video.channelName) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (video.channelId != null) {
                                                    onNavigateToChannel(video.channelId, video.channelName)
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ChannelAvatarView(
                                            avatarUrl = video.channelAvatarUrl,
                                            channelName = video.channelName,
                                            size = 40.dp
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = video.channelName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            val subText = if (video.subscriberCountText.isNotEmpty()) video.subscriberCountText else "Channel"
                                            Text(
                                                text = subText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Follow / Following Button
                                    Button(
                                        onClick = {
                                            viewModel.toggleSubscription(
                                                video.channelId ?: video.channelName,
                                                video.channelName,
                                                video.channelAvatarUrl
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                            contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isSubscribed) "Following" else "Follow",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Expandable Description Section
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable { viewModel.toggleDescription() },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val stats = listOf(video.viewCountText, video.publishedText)
                                                .filter { it.isNotEmpty() }
                                                .joinToString("   ")
                                            Text(
                                                text = if (stats.isNotEmpty()) stats else "Description",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Icon(
                                                imageVector = if (uiState.isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (uiState.isDescriptionExpanded) "Collapse" else "Expand",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (uiState.isDescriptionExpanded) {
                                            Text(
                                                text = if (video.description.isNotEmpty()) video.description else "No description provided.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 19.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Show less",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FocusBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            val preview = if (video.description.isNotEmpty()) {
                                                video.description.take(120).trim() + "..."
                                            } else {
                                                "Tap to view details..."
                                            }
                                            Text(
                                                text = preview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Comments Preview Card
                            if (!preferences.hideComments) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clickable { viewModel.setCommentsSheetOpen(true) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Comments",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (uiState.commentsData.totalCountText.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = uiState.commentsData.totalCountText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            when (uiState.commentsData.status) {
                                                CommentStatus.LOADING -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = FocusBlue)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Loading conversation...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                CommentStatus.DISABLED -> {
                                                    Text("Comments are turned off for this video.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                CommentStatus.EMPTY -> {
                                                    Text("No comments yet. Tap to view.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                CommentStatus.RESTRICTED -> {
                                                    Text("Comments unavailable from server.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                CommentStatus.ERROR -> {
                                                    Text("Could not load comments.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                CommentStatus.SUCCESS -> {
                                                    val topComment = uiState.commentsData.comments.firstOrNull()
                                                    if (topComment != null) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = topComment.author.take(1).uppercase(),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = topComment.content,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                CommentStatus.IDLE -> {}
                                            }
                                        }
                                    }
                                }
                            }

                            // Related Videos Section
                            if (!preferences.focusMode) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Related Videos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }

                                if (uiState.isRelatedLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = FocusBlue, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                } else if (uiState.relatedVideos.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No related videos found.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                } else {
                                    items(uiState.relatedVideos, key = { it.id }) { relatedVideo ->
                                        RelatedVideoItemView(
                                            video = relatedVideo,
                                            onClick = { onNavigateToVideo(relatedVideo.id) }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (uiState.isLoading) {
                        LoadingView(message = "Loading video...")
                    } else if (uiState.errorMessage != null) {
                        ErrorView(
                            message = uiState.errorMessage ?: "Failed to load video",
                            onRetry = { viewModel.loadVideo(videoId) }
                        )
                    }
                }
            }
        }

        // Consolidated Player Settings Bottom Sheet (Section 3)
        if (uiState.isSettingsSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setSettingsSheetOpen(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playback Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { viewModel.setSettingsSheetOpen(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Playback Quality
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = FocusBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Playback Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current: ${uiState.selectedQuality} • Adaptive stream quality is managed automatically by the video source.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val qualityOptions = listOf("Auto", "1080p", "720p", "480p", "360p", "240p")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(qualityOptions) { q ->
                            val isSel = uiState.selectedQuality == q
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setSelectedQuality(q) },
                                label = { Text(q, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FocusBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Playback Speed (up to 3x in 12 increments)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = FocusBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Playback Speed (${uiState.currentSpeed}×)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FilterManager.SUPPORTED_SPEEDS) { speed ->
                            val isSelected = uiState.currentSpeed == speed
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPlaybackSpeed(speed) },
                                label = { Text("${speed}×", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FocusBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Sleep & Study Timer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = FocusTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isSleepTimerActive) "Study Timer Active (${uiState.sleepTimerSecondsRemaining / 60}m remaining)" else "Study & Sleep Timer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val timerOptions = listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        timerOptions.forEach { (mins, label) ->
                            val isSel = uiState.sleepTimerMinutes == mins
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setSleepTimer(mins) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FocusTeal,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Background Audio Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = FocusBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Background Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Play audio when screen is locked or app minimized", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = preferences.backgroundAudioEnabled,
                            onCheckedChange = { viewModel.toggleBackgroundAudio(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FocusBlue,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Share Video Action
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$videoId")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                            viewModel.setSettingsSheetOpen(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Video Link", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Full Comments ModalBottomSheet
        if (uiState.isCommentsSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setCommentsSheetOpen(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (${uiState.commentsData.totalCountText})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { viewModel.setCommentsSheetOpen(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    when (uiState.commentsData.status) {
                        CommentStatus.LOADING -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = FocusBlue)
                            }
                        }
                        CommentStatus.DISABLED -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CommentsDisabled, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Comments are turned off", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("The creator has disabled comments for this video.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        CommentStatus.EMPTY -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No comments yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Be the first to join the conversation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        CommentStatus.RESTRICTED, CommentStatus.ERROR -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(uiState.commentsData.errorMessage ?: "Comments unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.loadComments(videoId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = FocusBlue)
                                    ) {
                                        Text("Retry", color = Color.White)
                                    }
                                }
                            }
                        }
                        CommentStatus.SUCCESS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.commentsData.comments, key = { it.id.ifEmpty { it.content.hashCode().toString() } }) { comment ->
                                    DetailedCommentItemView(comment)
                                }
                            }
                        }
                        CommentStatus.IDLE -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchActionPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) FocusBlue else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isActive) FocusBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailedCommentItemView(comment: CommentItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        ChannelAvatarView(
            avatarUrl = comment.authorAvatarUrl,
            channelName = comment.author,
            size = 32.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (comment.publishedTime.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = comment.publishedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            if (comment.likeCountText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ThumbUpOffAlt,
                        contentDescription = "Likes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = comment.likeCountText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

class FocusTubeBridge(
    private val onReady: () -> Unit,
    private val onStateChange: (Int) -> Unit,
    private val onTimeUpdate: (Float, Float, Float) -> Unit,
    private val onLiveInfo: (Boolean, Boolean, Float) -> Unit
) {
    @JavascriptInterface
    fun notifyReady() {
        onReady()
    }

    @JavascriptInterface
    fun notifyStateChange(state: Int) {
        onStateChange(state)
    }

    @JavascriptInterface
    fun notifyTimeUpdate(currentTime: Float, duration: Float, loadedFraction: Float) {
        onTimeUpdate(currentTime, duration, loadedFraction)
    }

    @JavascriptInterface
    fun notifyLiveInfo(isLive: Boolean, behindLiveEdge: Boolean, deltaSeconds: Float) {
        onLiveInfo(isLive, behindLiveEdge, deltaSeconds)
    }
}

fun formatPlayerTime(seconds: Long): String {
    val totalSec = seconds.coerceAtLeast(0L)
    val s = totalSec % 60
    val m = (totalSec / 60) % 60
    val h = totalSec / 3600
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

private fun buildEmbedHtml(videoId: String, autoplay: Boolean, speed: Float, startSeconds: Long): String {
    val autoplayParam = if (autoplay) 1 else 0
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background: #000000; overflow: hidden; }
                #player { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var pollInterval = null;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': $autoplayParam,
                            'controls': 0,
                            'rel': 0,
                            'modestbranding': 1,
                            'playsinline': 1,
                            'start': $startSeconds,
                            'fs': 0,
                            'iv_load_policy': 3,
                            'disablekb': 1,
                            'enablejsapi': 1
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange
                        }
                    });
                }

                function onPlayerReady(event) {
                    try {
                        player.setPlaybackRate($speed);
                    } catch(e) {}

                    if (window.FocusTubeBridge) {
                        window.FocusTubeBridge.notifyReady();
                    }

                    if ($autoplayParam === 1) {
                        try {
                            event.target.playVideo();
                        } catch(e) {}
                    }

                    checkLiveAndSendTime();

                    if (pollInterval) clearInterval(pollInterval);
                    pollInterval = setInterval(checkLiveAndSendTime, 500);
                }

                function checkLiveAndSendTime() {
                    try {
                        if (!window.player) return;
                        var cur = player.getCurrentTime ? (player.getCurrentTime() || 0) : 0;
                        var dur = player.getDuration ? (player.getDuration() || 0) : 0;
                        var loaded = 0;
                        if (player.getVideoLoadedFraction) {
                            loaded = player.getVideoLoadedFraction() || 0;
                        }

                        var isLive = false;
                        var behindEdge = false;
                        var liveDelta = 0;

                        if (player.getVideoData) {
                            var d = player.getVideoData();
                            if (d && (d.isLive === true || d.isLive === 1)) isLive = true;
                        }
                        if (player.isLiveContent && player.isLiveContent()) {
                            isLive = true;
                        }
                        if (dur === 0 && cur > 0) {
                            isLive = true;
                        }

                        if (isLive && player.getSeekableRange) {
                            var range = player.getSeekableRange();
                            if (range && range.end) {
                                liveDelta = Math.max(0, range.end - cur);
                                behindEdge = liveDelta > 15;
                            }
                        }

                        if (window.FocusTubeBridge) {
                            window.FocusTubeBridge.notifyTimeUpdate(cur, dur, loaded);
                            window.FocusTubeBridge.notifyLiveInfo(isLive, behindEdge, liveDelta);
                        }
                    } catch(e) {}
                }

                function onPlayerStateChange(event) {
                    if (window.FocusTubeBridge) {
                        window.FocusTubeBridge.notifyStateChange(event.data);
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun FocusTubeTimelineBar(
    currentPositionSeconds: Long,
    durationSeconds: Long,
    bufferedFraction: Float,
    thickness: ProgressBarThickness,
    isLive: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val totalSec = if (durationSeconds > 0) durationSeconds else 1L
    val activeFraction = (scrubFraction ?: (currentPositionSeconds.toFloat() / totalSec)).coerceIn(0f, 1f)

    val isScrubbing = scrubFraction != null
    val visualTrackHeight = if (isScrubbing) (thickness.trackDp + 1.5f).dp else thickness.trackDp.dp
    val thumbRadius = if (isScrubbing) (thickness.thumbDp * 0.75f).dp else (thickness.thumbDp / 2f).dp

    Box(
        modifier = modifier
            .height(40.dp)
            .pointerInput(totalSec, isLive) {
                if (isLive) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        scrubFraction = fraction
                        val targetSec = (fraction * totalSec).toLong()
                        val success = tryAwaitRelease()
                        if (success) {
                            onSeekTo(targetSec)
                        }
                        scrubFraction = null
                    }
                )
            }
            .pointerInput(totalSec, isLive) {
                if (isLive) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        scrubFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        scrubFraction?.let { frac ->
                            onSeekTo((frac * totalSec).toLong())
                        }
                        scrubFraction = null
                    },
                    onDragCancel = {
                        scrubFraction = null
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val trackH = visualTrackHeight.toPx()
            val centerY = size.height / 2f
            val w = size.width
            val progressX = activeFraction * w
            val bufferX = (bufferedFraction * w).coerceAtMost(w)

            // 1. Inactive background track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackH / 2f),
                size = androidx.compose.ui.geometry.Size(w, trackH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2, trackH / 2)
            )

            // 2. Buffered track
            if (bufferX > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackH / 2f),
                    size = androidx.compose.ui.geometry.Size(bufferX, trackH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2, trackH / 2)
                )
            }

            // 3. Active played track (Red)
            if (progressX > 0f) {
                drawRoundRect(
                    color = Color.Red,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackH / 2f),
                    size = androidx.compose.ui.geometry.Size(progressX, trackH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2, trackH / 2)
                )
            }

            // 4. Seek thumb (Red circle with white outline)
            if (!isLive) {
                val thumbR = thumbRadius.toPx()
                val clampedX = progressX.coerceIn(thumbR, w - thumbR)
                drawCircle(
                    color = Color.Red,
                    radius = thumbR,
                    center = androidx.compose.ui.geometry.Offset(clampedX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbR,
                    center = androidx.compose.ui.geometry.Offset(clampedX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun VideoPlayerControlsOverlay(
    visible: Boolean,
    videoTitle: String,
    isLandscape: Boolean,
    isPlaying: Boolean,
    isPlayerReady: Boolean,
    currentPositionSeconds: Long,
    durationSeconds: Long,
    bufferedFraction: Float,
    thickness: ProgressBarThickness,
    isLive: Boolean,
    isBehindLiveEdge: Boolean,
    onGoLive: () -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeekRelative: (Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOverlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOverlayClick
                )
        ) {
            // Top overlay bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLandscape) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center Playback Controls: Replay -10s, Play/Pause, Forward +10s
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLive) {
                    IconButton(
                        onClick = { onSeekRelative(-10) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Seek -10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                if (!isLive) {
                    IconButton(
                        onClick = { onSeekRelative(10) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Seek +10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Bottom Bar: Current Time, Custom Timeline Bar, Duration / LIVE, Fullscreen Toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlayerTime(currentPositionSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    FocusTubeTimelineBar(
                        currentPositionSeconds = currentPositionSeconds,
                        durationSeconds = durationSeconds,
                        bufferedFraction = bufferedFraction,
                        thickness = thickness,
                        isLive = isLive,
                        onSeekTo = onSeekTo,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    if (isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isBehindLiveEdge) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Red.copy(alpha = 0.25f))
                                        .clickable { onGoLive() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "GO LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = formatPlayerTime(durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
