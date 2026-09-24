package com.alix.tsuki.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alix.tsuki.TsukiApp
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.ui.navigation.MainTab
import com.alix.tsuki.ui.screens.library.LibraryScreen
import com.alix.tsuki.ui.screens.library.LibraryViewModel
import com.alix.tsuki.ui.screens.recents.RecentsScreen
import com.alix.tsuki.ui.screens.recents.RecentsViewModel
import com.alix.tsuki.ui.screens.settings.SettingsScreen
import com.alix.tsuki.ui.screens.settings.SettingsViewModel

data class BottomNavItem(
    val tab: MainTab,
    val icon: ImageVector,
    val label: String
)

@Composable
fun MainScreen(
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf<String>(MainTab.Library.route) }

    val navItems = listOf(
        BottomNavItem(MainTab.Library, Icons.AutoMirrored.Filled.MenuBook, "Library"),
        BottomNavItem(MainTab.Recents, Icons.Default.History, "Recents"),
        BottomNavItem(MainTab.Settings, Icons.Default.Settings, "Settings")
    )

    val app = TsukiApp.instance
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(app.repository))
    val recentsViewModel: RecentsViewModel = viewModel(factory = RecentsViewModel.Factory(app.repository))
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.preferences, app.repository)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                navItems.forEach { item ->
                    val isSelected = selectedTab == item.tab.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = item.tab.route },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(text = item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Library.route -> {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onMangaClick = onMangaClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainTab.Recents.route -> {
                RecentsScreen(
                    viewModel = recentsViewModel,
                    onMangaClick = onMangaClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainTab.Settings.route -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
