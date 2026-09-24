package com.alix.tsuki.ui.navigation

import android.util.Base64

sealed class Screen(val route: String) {
    data object Main : Screen("main")

    data object MangaDetails : Screen("details/{mangaId}") {
        fun createRoute(mangaId: String): String {
            return "details/${encodeId(mangaId)}"
        }
    }

    data object Reader : Screen("reader/{mangaId}/{chapterId}") {
        fun createRoute(mangaId: String, chapterId: String): String {
            return "reader/${encodeId(mangaId)}/${encodeId(chapterId)}"
        }
    }

    companion object {
        fun encodeId(id: String): String {
            return Base64.encodeToString(
                id.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        }

        fun decodeId(encoded: String): String {
            return try {
                String(Base64.decode(encoded, Base64.URL_SAFE), Charsets.UTF_8)
            } catch (_: Exception) {
                encoded
            }
        }
    }
}

sealed class MainTab(val route: String, val title: String) {
    data object Library : MainTab("library", "Library")
    data object Recents : MainTab("recents", "Recents")
    data object Settings : MainTab("settings", "Settings")
}
