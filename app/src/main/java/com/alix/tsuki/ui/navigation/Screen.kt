package com.alix.tsuki.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Main : Screen("main")

    data object MangaDetails : Screen("details/{mangaId}") {
        fun createRoute(mangaId: String): String {
            return "details/${Uri.encode(mangaId)}"
        }
    }

    data object Reader : Screen("reader/{mangaId}/{chapterId}") {
        fun createRoute(mangaId: String, chapterId: String): String {
            return "reader/${Uri.encode(mangaId)}/${Uri.encode(chapterId)}"
        }
    }
}

sealed class MainTab(val route: String, val title: String) {
    data object Library : MainTab("library", "Library")
    data object Recents : MainTab("recents", "Recents")
    data object Settings : MainTab("settings", "Settings")
}
