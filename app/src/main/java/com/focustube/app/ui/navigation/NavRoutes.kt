package com.focustube.app.ui.navigation

sealed class Screen(val route: String) {
    object Discover : Screen("discover")
    object Search : Screen("search")
    object Following : Screen("following")
    object Library : Screen("library")
    object Focus : Screen("focus")
    object Reels : Screen("reels")
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    object Channel : Screen("channel/{channelId}?name={channelName}") {
        fun createRoute(channelId: String, channelName: String) =
            "channel/$channelId?name=${java.net.URLEncoder.encode(channelName, "UTF-8")}"
    }
}
