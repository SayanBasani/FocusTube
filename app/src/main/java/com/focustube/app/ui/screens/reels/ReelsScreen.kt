package com.focustube.app.ui.screens.reels

import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.focustube.app.data.model.CommentItem
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.VideoItem
import com.focustube.app.ui.components.ChannelAvatarView
import com.focustube.app.ui.components.ErrorView
import com.focustube.app.ui.components.LoadingView
import com.focustube.app.ui.theme.FocusBlue
import com.focustube.app.ui.theme.FocusTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel,
    onNavigateToVideo: (String) -> Unit,
    onExitReels: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val watchLater by viewModel.watchLater.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (!preferences.reelsEnabled) {
            // Disabled Lock Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reels is Disabled",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Short-form video feeds are disabled by default to maintain intentional viewing. You can enable Reels in Settings → Focus Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExitReels,
                        colors = ButtonDefaults.buttonColors(containerColor = FocusBlue)
                    ) {
                        Text("Return to Discover", color = Color.White)
                    }
                }
            }
        } else {
            when {
                uiState.isLoading -> {
                    LoadingView(message = "Loading short-form feed...")
                }
                uiState.errorMessage != null && uiState.reels.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Failed to load Reels",
                        onRetry = { viewModel.loadReels() }
                    )
                }
                uiState.reels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No short-form videos available.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    val pagerState = rememberPagerState(pageCount = { uiState.reels.size })

                    // Prefetch more reels near end
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            if (page >= uiState.reels.size - 2 && !uiState.isLoadingMore) {
                                viewModel.loadMoreReels()
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val reel = uiState.reels[page]
                            val isActive = pagerState.currentPage == page
                            val isLiked = uiState.likedVideoIds.contains(reel.id)
                            val isSaved = watchLater.any { it.id == reel.id }
                            val isSubscribed = subscriptions.any { it.id == (reel.channelId ?: reel.channelName) }

                            ReelItemView(
                                reel = reel,
                                isActive = isActive,
                                isLiked = isLiked,
                                isSaved = isSaved,
                                isSubscribed = isSubscribed,
                                onToggleLike = { viewModel.toggleLike(reel.id) },
                                onToggleSave = { viewModel.toggleWatchLater(reel) },
                                onToggleSubscribe = {
                                    viewModel.toggleSubscription(
                                        reel.channelId ?: reel.channelName,
                                        reel.channelName,
                                        reel.channelAvatarUrl
                                    )
                                },
                                onOpenComments = { viewModel.openComments(reel.id) },
                                onShare = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/shorts/${reel.id}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                                }
                            )
                        }

                        // Top Minimalist Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onExitReels,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Reels",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "Shorts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }

                    // Comments ModalBottomSheet
                    if (uiState.isCommentsSheetOpen) {
                        ModalBottomSheet(
                            onDismissRequest = { viewModel.closeComments() },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.7f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Comments (${uiState.commentsData.totalCountText.ifEmpty { uiState.commentsData.comments.size.toString() }})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = { viewModel.closeComments() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Comments")
                                    }
                                }

                                when {
                                    uiState.isCommentsLoading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = FocusBlue)
                                        }
                                    }
                                    uiState.commentsData.status == CommentStatus.DISABLED -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Comments are disabled for this short.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    uiState.commentsData.comments.isEmpty() -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No comments yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    else -> {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(uiState.commentsData.comments) { comment ->
                                                ReelCommentItemView(comment = comment)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReelItemView(
    reel: VideoItem,
    isActive: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isSubscribed: Boolean,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var showPauseOverlay by remember { mutableStateOf(false) }

    // When page changes from active to inactive, pause and destroy
    DisposableEffect(isActive) {
        if (!isActive) {
            webViewRef?.apply {
                evaluateJavascript("if (window.player && player.pauseVideo) player.pauseVideo();", null)
                stopLoading()
            }
        }
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isActive) {
            // Active Fullscreen Portrait Player
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

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
                        }

                        val html = buildReelEmbedHtml(reel.id)
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent Tap Layer to Toggle Play/Pause
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isPaused) {
                            webViewRef?.evaluateJavascript("if (window.player && player.playVideo) player.playVideo();", null)
                            isPaused = false
                        } else {
                            webViewRef?.evaluateJavascript("if (window.player && player.pauseVideo) player.pauseVideo();", null)
                            isPaused = true
                        }
                        showPauseOverlay = true
                    }
            )

            // Play/Pause Flash Icon Overlay
            LaunchedEffect(showPauseOverlay) {
                if (showPauseOverlay) {
                    delay(800)
                    showPauseOverlay = false
                }
            }

            AnimatedVisibility(
                visible = showPauseOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        } else {
            // Inactive Page: Render high-res thumbnail with NO running player
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(reel.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = reel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Gradient Scrim for Legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Right-Side Action Rail (Like, Comments, Share, Save)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like Action
            ReelActionButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (isLiked) "Liked" else if (reel.likeCountText.isNotEmpty()) reel.likeCountText else "Like",
                tint = if (isLiked) Color.Red else Color.White,
                onClick = onToggleLike
            )

            // Comments Action
            ReelActionButton(
                icon = Icons.Default.ChatBubble,
                label = "Comments",
                tint = Color.White,
                onClick = onOpenComments
            )

            // Share Action
            ReelActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                tint = Color.White,
                onClick = onShare
            )

            // Save Action
            ReelActionButton(
                icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = if (isSaved) "Saved" else "Save",
                tint = if (isSaved) FocusTeal else Color.White,
                onClick = onToggleSave
            )
        }

        // Bottom Creator & Title Metadata Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 84.dp, bottom = 28.dp)
        ) {
            // Creator Row: Avatar + Name + Follow Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChannelAvatarView(
                    avatarUrl = reel.channelAvatarUrl,
                    channelName = reel.channelName,
                    size = 36.dp
                )

                Text(
                    text = "@${reel.channelName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Follow / Following Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSubscribed) Color.White.copy(alpha = 0.2f) else FocusBlue)
                        .clickable(onClick = onToggleSubscribe)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSubscribed) "Following" else "Follow",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption / Title
            Text(
                text = reel.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Audio Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Original audio • ${reel.channelName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ReelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ReelCommentItemView(comment: CommentItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChannelAvatarView(
            avatarUrl = comment.authorAvatarUrl,
            channelName = comment.author,
            size = 32.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (comment.publishedTime.isNotEmpty()) {
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun buildReelEmbedHtml(reelId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background: #000000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                #player { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScript = document.getElementsByTagName('script')[0];
                firstScript.parentNode.insertBefore(tag, firstScript);

                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$reelId',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 0,
                            'loop': 1,
                            'playlist': '$reelId',
                            'rel': 0,
                            'playsinline': 1,
                            'modestbranding': 1,
                            'fs': 0,
                            'iv_load_policy': 3,
                            'disablekb': 1,
                            'enablejsapi': 1
                        },
                        events: {
                            'onReady': function(e) {
                                try { e.target.playVideo(); } catch(err) {}
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
