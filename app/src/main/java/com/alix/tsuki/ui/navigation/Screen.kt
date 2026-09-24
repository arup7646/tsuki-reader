package com.alix.tsuki.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Reader : Screen("reader/{mangaId}") {
        fun createRoute(mangaId: String): String {
            return "reader/${Uri.encode(mangaId)}"
        }
    }
}

sealed class MainTab(val route: String, val title: String) {
    data object Library : MainTab("library", "Library")
    data object Recents : MainTab("recents", "Recents")
    data object Settings : MainTab("settings", "Settings")
}
