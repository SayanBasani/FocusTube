package com.focustube.app.ui.screens.library

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focustube.app.ui.components.VideoCard
import com.focustube.app.ui.theme.FocusBlue

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val preferences by viewModel.preferences.collectAsState()
    val watchLaterList by viewModel.watchLater.collectAsState()
    val historyList by viewModel.history.collectAsState()
    val historyPaused by viewModel.historyPaused.collectAsState()

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FocusBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FocusBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Saved for Later (${watchLaterList.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) FocusBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == 0) FocusBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Watch History (${historyList.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) FocusBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == 1) FocusBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Content
            if (selectedTab == 0) {
                // Saved / Watch Later
                if (watchLaterList.isEmpty()) {
                    EmptyLibraryPlaceholder(
                        title = "No Saved Videos",
                        subtitle = "Bookmark intentional videos to watch when you have dedicated focus time."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                    ) {
                        items(watchLaterList, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                isSaved = true,
                                grayscaleThumbnails = preferences.grayscaleThumbnails,
                                hideMetrics = preferences.hideMetrics,
                                onVideoClick = { onNavigateToVideo(it.id) },
                                onChannelClick = { id, name -> onNavigateToChannel(id, name) },
                                onToggleSave = { viewModel.toggleWatchLater(it) }
                            )
                        }
                    }
                }
            } else {
                // Watch History
                Column(modifier = Modifier.fillMaxSize()) {
                    // History Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.togglePauseHistory() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (historyPaused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (historyPaused) MaterialTheme.colorScheme.onSurfaceVariant else FocusBlue
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (historyPaused) "Resume History" else "Pause History",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (historyList.isNotEmpty()) {
                            TextButton(onClick = { showClearConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    "Clear All",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (historyList.isEmpty()) {
                        EmptyLibraryPlaceholder(
                            title = "Watch History is Empty",
                            subtitle = if (historyPaused) "History recording is currently paused." else "Videos you intentionally watch will be recorded here."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(historyList, key = { it.id }) { video ->
                                val isSaved = watchLaterList.any { it.id == video.id }
                                VideoCard(
                                    video = video,
                                    isSaved = isSaved,
                                    grayscaleThumbnails = preferences.grayscaleThumbnails,
                                    hideMetrics = preferences.hideMetrics,
                                    onVideoClick = { onNavigateToVideo(it.id) },
                                    onChannelClick = { id, name -> onNavigateToChannel(id, name) },
                                    onToggleSave = { viewModel.toggleWatchLater(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clear history confirmation dialog
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear Watch History?", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(
                        "Are you sure you want to remove all items from your local watch history?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearHistory()
                            showClearConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FocusBlue)
                    ) {
                        Text("Clear History", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun EmptyLibraryPlaceholder(title: String, subtitle: String) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
