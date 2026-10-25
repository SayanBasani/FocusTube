package com.focustube.app.ui.screens.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focustube.app.ui.components.ChannelAvatarView
import com.focustube.app.ui.components.ErrorView
import com.focustube.app.ui.components.LoadingView
import com.focustube.app.ui.components.VideoCard
import com.focustube.app.ui.theme.FocusBlue

@Composable
fun ChannelScreen(
    channelId: String,
    channelName: String,
    viewModel: ChannelViewModel,
    onNavigateToVideo: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val watchLaterList by viewModel.watchLater.collectAsState()

    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId, channelName)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Channel Hero Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatarUrl = uiState.videos.firstOrNull { it.channelAvatarUrl.isNotEmpty() }?.channelAvatarUrl
                    ?: subscriptions.find { it.id == channelId }?.avatarUrl ?: ""
                ChannelAvatarView(
                    avatarUrl = avatarUrl,
                    channelName = channelName,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Intentional Content Creator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val isSubscribed = subscriptions.any { it.id == channelId }
                    Button(
                        onClick = { viewModel.toggleSubscription(channelId, channelName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceContainer else FocusBlue,
                            contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Following" else "Follow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Channel Videos Feed
            when {
                uiState.isLoading -> {
                    LoadingView(message = "Loading channel videos...")
                }
                uiState.errorMessage != null -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Failed to load channel videos",
                        onRetry = { viewModel.loadChannel(channelId, channelName) }
                    )
                }
                uiState.videos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No intentional videos found for this channel.",
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
                        item {
                            Text(
                                text = "Videos (${uiState.videos.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(uiState.videos, key = { it.id }) { video ->
                            val isSaved = watchLaterList.any { it.id == video.id }
                            VideoCard(
                                video = video,
                                isSaved = isSaved,
                                grayscaleThumbnails = preferences.grayscaleThumbnails,
                                hideMetrics = preferences.hideMetrics,
                                onVideoClick = { onNavigateToVideo(it.id) },
                                onToggleSave = { viewModel.toggleWatchLater(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
