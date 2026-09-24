package com.alix.tsuki.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alix.tsuki.TsukiApp
import com.alix.tsuki.ui.screens.details.MangaDetailsScreen
import com.alix.tsuki.ui.screens.details.MangaDetailsViewModel
import com.alix.tsuki.ui.screens.main.MainScreen
import com.alix.tsuki.ui.screens.reader.ReaderScreen
import com.alix.tsuki.ui.screens.reader.ReaderViewModel

@Composable
fun TsukiNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Main.route,
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            }
        ) {
            MainScreen(
                onMangaClick = { manga ->
                    navController.navigate(Screen.MangaDetails.createRoute(manga.id))
                },
                onResumeChapterClick = { mangaId, chapterId ->
                    navController.navigate(Screen.Reader.createRoute(mangaId, chapterId))
                }
            )
        }

        composable(
            route = Screen.MangaDetails.route,
            arguments = listOf(
                navArgument("mangaId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            }
        ) { backStackEntry ->
            val encodedMangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
            val mangaId = Uri.decode(encodedMangaId)

            val app = TsukiApp.instance
            val detailsViewModel: MangaDetailsViewModel = viewModel(
                key = mangaId,
                factory = MangaDetailsViewModel.Factory(
                    mangaId = mangaId,
                    repository = app.repository
                )
            )

            MangaDetailsScreen(
                viewModel = detailsViewModel,
                onBackClick = { navController.popBackStack() },
                onChapterClick = { chapter ->
                    navController.navigate(Screen.Reader.createRoute(chapter.mangaId, chapter.id))
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("mangaId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            }
        ) { backStackEntry ->
            val encodedMangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
            val encodedChapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val mangaId = Uri.decode(encodedMangaId)
            val chapterId = Uri.decode(encodedChapterId)

            val app = TsukiApp.instance
            val readerViewModel: ReaderViewModel = viewModel(
                key = "$mangaId-$chapterId",
                factory = ReaderViewModel.Factory(
                    mangaId = mangaId,
                    initialChapterId = chapterId,
                    repository = app.repository,
                    preferences = app.preferences
                )
            )

            ReaderScreen(
                viewModel = readerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
