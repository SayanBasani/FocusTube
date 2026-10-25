package com.focustube.app.ui.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focustube.app.ui.components.ErrorView
import com.focustube.app.ui.components.LoadingView
import com.focustube.app.ui.components.VideoCard

@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val watchLaterList by viewModel.watchLater.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Followed Channels (${subscriptions.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = { viewModel.loadFeed() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Feed",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            when {
                subscriptions.isEmpty() -> {
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
                                imageVector = Icons.Default.Subscriptions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Followed Channels Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Follow channels you deliberately choose to learn from. Their long-form videos will appear here with zero distractions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                uiState.isLoading -> {
                    LoadingView(message = "Fetching deliberate channel updates...")
                }
                uiState.errorMessage != null -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Failed to refresh feed",
                        onRetry = { viewModel.loadFeed() }
                    )
                }
                uiState.feedVideos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recent videos from followed channels.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.feedVideos, key = { it.id }) { video ->
                            val isSaved = watchLaterList.any { it.id == video.id }
                            VideoCard(
                                video = video,
                                isSaved = isSaved,
                                grayscaleThumbnails = preferences.grayscaleThumbnails,
                                hideMetrics = preferences.hideMetrics,
                                onVideoClick = { onNavigateToVideo(it.id) },
                                onChannelClick = { channelId, channelName ->
                                    onNavigateToChannel(channelId, channelName)
                                },
                                onToggleSave = { viewModel.toggleWatchLater(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
