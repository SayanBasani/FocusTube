package com.focustube.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focustube.app.data.repository.FilterManager
import com.focustube.app.ui.navigation.FocusBottomBar
import com.focustube.app.ui.navigation.Screen
import com.focustube.app.ui.screens.about.AboutScreen
import com.focustube.app.ui.screens.channel.ChannelScreen
import com.focustube.app.ui.screens.channel.ChannelViewModel
import com.focustube.app.ui.screens.home.HomeScreen
import com.focustube.app.ui.screens.home.HomeViewModel
import com.focustube.app.ui.screens.library.LibraryScreen
import com.focustube.app.ui.screens.library.LibraryViewModel
import com.focustube.app.ui.screens.player.PlayerSheetState
import com.focustube.app.ui.screens.player.PlayerViewModel
import com.focustube.app.ui.screens.player.UnifiedPlayerHost
import com.focustube.app.ui.screens.reels.ReelsScreen
import com.focustube.app.ui.screens.reels.ReelsViewModel
import com.focustube.app.ui.screens.search.SearchScreen
import com.focustube.app.ui.screens.search.SearchViewModel
import com.focustube.app.ui.screens.subscriptions.SubscriptionsScreen
import com.focustube.app.ui.screens.subscriptions.SubscriptionsViewModel
import com.focustube.app.ui.screens.settings.SettingsScreen
import com.focustube.app.ui.screens.settings.SettingsViewModel
import com.focustube.app.ui.theme.FocusTubeTheme

class MainActivity : ComponentActivity() {

    private val isPiPMode = mutableStateOf(false)
    private var currentRoute: String? = null
    private var isPlaybackActive: Boolean = false
    private var onNewVideoRequested: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as FocusTubeApp
        val initialVideoId = handleIntentData(intent)

        setContent {
            val preferences by app.storageRepository.preferences.collectAsState()
            val playerViewModel = rememberViewModel("global_player") {
                PlayerViewModel(app.videoRepository, app.storageRepository)
            }
            val playerUiState by playerViewModel.uiState.collectAsState()

            LaunchedEffect(playerUiState.sheetState, preferences.pipEnabled) {
                isPlaybackActive = playerUiState.sheetState != PlayerSheetState.HIDDEN
                updatePipParams(isPlaybackActive && preferences.pipEnabled)
            }

            LaunchedEffect(initialVideoId) {
                if (!initialVideoId.isNullOrEmpty()) {
                    playerViewModel.openVideo(initialVideoId)
                }
            }

            DisposableEffect(Unit) {
                onNewVideoRequested = { videoId ->
                    playerViewModel.openVideo(videoId)
                }
                onDispose {
                    onNewVideoRequested = null
                }
            }

            FocusTubeTheme(themeMode = preferences.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    FocusTubeMain(
                        navController = navController,
                        app = app,
                        playerViewModel = playerViewModel,
                        reelsEnabled = preferences.reelsEnabled,
                        isPiPMode = isPiPMode.value,
                        onRouteChanged = { route ->
                            currentRoute = route
                        }
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val app = application as FocusTubeApp
        val pipAllowed = app.storageRepository.preferences.value.pipEnabled
        if (pipAllowed && isPlaybackActive) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    // Ignore if PiP unavailable
                }
            }
        }
    }

    fun updatePipParams(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val paramsBuilder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    paramsBuilder.setAutoEnterEnabled(enabled)
                }
                setPictureInPictureParams(paramsBuilder.build())
            } catch (e: Exception) {
                // Ignore if PiP unavailable
            }
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Ignore if PiP unavailable
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPiPMode.value = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentData(intent)?.let { videoId ->
            onNewVideoRequested?.invoke(videoId)
        }
    }

    private fun handleIntentData(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val url = data.toString()
        return FilterManager.extractVideoId(url)
    }
}

@Composable
fun FocusTubeMain(
    navController: NavHostController,
    app: FocusTubeApp,
    playerViewModel: PlayerViewModel,
    reelsEnabled: Boolean,
    isPiPMode: Boolean = false,
    onRouteChanged: (String?) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val playerUiState by playerViewModel.uiState.collectAsState()

    LaunchedEffect(currentRoute) {
        onRouteChanged(currentRoute)
    }

    // Hide bottom bar when in PiP mode or when player is EXPANDED (full watch page)
    val showBottomBar = !isPiPMode && playerUiState.sheetState != PlayerSheetState.EXPANDED

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    FocusBottomBar(
                        currentRoute = currentRoute,
                        reelsEnabled = reelsEnabled,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Discover.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            val extraBottomPadding = if (playerUiState.sheetState == PlayerSheetState.COLLAPSED) 68.dp else 0.dp
            NavHost(
                navController = navController,
                startDestination = Screen.Discover.route,
                modifier = Modifier.padding(
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = innerPadding.calculateBottomPadding() + extraBottomPadding
                )
            ) {
                // 1. Discover Destination
                composable(Screen.Discover.route) {
                    val homeViewModel = rememberViewModel { HomeViewModel(app.videoRepository, app.storageRepository) }
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToVideo = { videoId ->
                            playerViewModel.openVideo(videoId)
                        },
                        onNavigateToChannel = { channelId, channelName ->
                            navController.navigate(Screen.Channel.createRoute(channelId, channelName))
                        },
                        onNavigateToSearch = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                // 2. Search Destination
                composable(Screen.Search.route) {
                    val searchViewModel = rememberViewModel { SearchViewModel(app.videoRepository, app.storageRepository) }
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateToVideo = { videoId ->
                            playerViewModel.openVideo(videoId)
                        },
                        onNavigateToChannel = { channelId, channelName ->
                            navController.navigate(Screen.Channel.createRoute(channelId, channelName))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 3. Following Destination
                composable(Screen.Following.route) {
                    val subsViewModel = rememberViewModel { SubscriptionsViewModel(app.videoRepository, app.storageRepository) }
                    SubscriptionsScreen(
                        viewModel = subsViewModel,
                        onNavigateToVideo = { videoId ->
                            playerViewModel.openVideo(videoId)
                        },
                        onNavigateToChannel = { channelId, channelName ->
                            navController.navigate(Screen.Channel.createRoute(channelId, channelName))
                        }
                    )
                }

                // 4. Library Destination
                composable(Screen.Library.route) {
                    val libViewModel = rememberViewModel { LibraryViewModel(app.storageRepository) }
                    LibraryScreen(
                        viewModel = libViewModel,
                        onNavigateToVideo = { videoId ->
                            playerViewModel.openVideo(videoId)
                        },
                        onNavigateToChannel = { channelId, channelName ->
                            navController.navigate(Screen.Channel.createRoute(channelId, channelName))
                        }
                    )
                }

                // 5. Focus Destination (Settings & Distraction Controls)
                composable(Screen.Focus.route) {
                    val settingsViewModel = rememberViewModel { SettingsViewModel(app.storageRepository) }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToAbout = {
                            navController.navigate(Screen.About.route)
                        }
                    )
                }

                // Optional 6. Reels Destination (Gated)
                composable(Screen.Reels.route) {
                    val reelsViewModel = rememberViewModel { ReelsViewModel(app.videoRepository, app.storageRepository) }
                    ReelsScreen(
                        viewModel = reelsViewModel,
                        onNavigateToVideo = { videoId ->
                            playerViewModel.openVideo(videoId)
                        },
                        onExitReels = {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(Screen.Discover.route) { inclusive = false }
                            }
                        }
                    )
                }

                // 7. Channel Route
                composable(
                    route = Screen.Channel.route,
                    arguments = listOf(
                        navArgument("channelId") { type = NavType.StringType },
                        navArgument("channelName") { type = NavType.StringType; defaultValue = "Channel" }
                    )
                ) { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    val channelName = backStackEntry.arguments?.getString("channelName") ?: "Channel"
                    val channelViewModel = rememberViewModel(channelId) { ChannelViewModel(app.videoRepository, app.storageRepository) }
                    ChannelScreen(
                        channelId = channelId,
                        channelName = channelName,
                        viewModel = channelViewModel,
                        onNavigateToVideo = { videoId ->
                            playerViewModel.openVideo(videoId)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 8. About & Developer Route
                composable(Screen.About.route) {
                    AboutScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // 9. Legacy Player Route (Fallback redirection)
                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                    LaunchedEffect(videoId) {
                        if (videoId.isNotEmpty()) {
                            playerViewModel.openVideo(videoId)
                            navController.popBackStack()
                        }
                    }
                }
            }
        }

        // Unified Player Overlay (Full watch page or Mini-player docked above bottom bar)
        UnifiedPlayerHost(
            viewModel = playerViewModel,
            isPiPMode = isPiPMode,
            bottomBarHeight = if (showBottomBar) 80.dp else 0.dp,
            onNavigateToChannel = { channelId, channelName ->
                navController.navigate(Screen.Channel.createRoute(channelId, channelName))
            }
        )
    }
}

@Composable
inline fun <reified T : androidx.lifecycle.ViewModel> rememberViewModel(
    key: String? = null,
    crossinline factory: () -> T
): T {
    return androidx.lifecycle.viewmodel.compose.viewModel(
        key = key,
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : androidx.lifecycle.ViewModel> create(modelClass: Class<VM>): VM {
                return factory() as VM
            }
        }
    )
}
