package com.focustube.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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
import com.focustube.app.ui.screens.channel.ChannelScreen
import com.focustube.app.ui.screens.channel.ChannelViewModel
import com.focustube.app.ui.screens.home.HomeScreen
import com.focustube.app.ui.screens.home.HomeViewModel
import com.focustube.app.ui.screens.library.LibraryScreen
import com.focustube.app.ui.screens.library.LibraryViewModel
import com.focustube.app.ui.screens.player.PlayerScreen
import com.focustube.app.ui.screens.player.PlayerViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as FocusTubeApp
        val initialVideoId = handleIntentData(intent)

        setContent {
            val preferences by app.storageRepository.preferences.collectAsState()

            FocusTubeTheme(themeMode = preferences.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(initialVideoId) {
                        if (!initialVideoId.isNullOrEmpty()) {
                            navController.navigate(Screen.Player.createRoute(initialVideoId))
                        }
                    }

                    FocusTubeMain(
                        navController = navController,
                        app = app,
                        reelsEnabled = preferences.reelsEnabled,
                        isPiPMode = isPiPMode.value,
                        onRouteChanged = { route ->
                            currentRoute = route
                            val isPlayer = route?.startsWith("player/") == true
                            updatePipParams(isPlayer && preferences.pipEnabled)
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
        if (pipAllowed && currentRoute?.startsWith("player/") == true) {
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
    reelsEnabled: Boolean,
    isPiPMode: Boolean = false,
    onRouteChanged: (String?) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        onRouteChanged(currentRoute)
    }

    // Hide bottom bar when on player, full landscape, or in PiP mode
    val showBottomBar = !isPiPMode && currentRoute?.startsWith("player/") != true

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
        NavHost(
            navController = navController,
            startDestination = Screen.Discover.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Discover Destination
            composable(Screen.Discover.route) {
                val homeViewModel = rememberViewModel { HomeViewModel(app.videoRepository, app.storageRepository) }
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
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
                        navController.navigate(Screen.Player.createRoute(videoId))
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
                        navController.navigate(Screen.Player.createRoute(videoId))
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
                        navController.navigate(Screen.Player.createRoute(videoId))
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
                    viewModel = settingsViewModel
                )
            }

            // Optional 6. Reels Destination (Gated)
            composable(Screen.Reels.route) {
                val reelsViewModel = rememberViewModel { ReelsViewModel(app.videoRepository, app.storageRepository) }
                ReelsScreen(
                    viewModel = reelsViewModel,
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onExitReels = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(Screen.Discover.route) { inclusive = false }
                        }
                    }
                )
            }

            // Video Player Route
            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                val playerViewModel = rememberViewModel(videoId) { PlayerViewModel(app.videoRepository, app.storageRepository) }
                PlayerScreen(
                    videoId = videoId,
                    viewModel = playerViewModel,
                    isPiPMode = isPiPMode,
                    onBack = { navController.popBackStack() },
                    onNavigateToVideo = { newId ->
                        navController.navigate(Screen.Player.createRoute(newId))
                    },
                    onNavigateToChannel = { channelId, channelName ->
                        navController.navigate(Screen.Channel.createRoute(channelId, channelName))
                    }
                )
            }

            // Channel Route
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
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
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
