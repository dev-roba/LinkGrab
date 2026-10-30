package dev.personal.linkgrab.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "Home")
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
    data object Downloads : Screen("downloads", "Downloads")
}

private val bottomBarScreens = listOf(Screen.Home, Screen.Downloads, Screen.History, Screen.Settings)

private fun navLabel(screen: Screen): String = when (screen) {
    Screen.Home -> "HOME"
    Screen.Downloads -> "QUEUE"
    Screen.History -> "LIBRARY"
    Screen.Settings -> "SETTINGS"
}

@Composable
fun LinkGrabNavGraph(initialUrl: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Sticker.Yellow)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bottomBarScreens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    StickerButton(
                        text = navLabel(screen),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        container = if (isSelected) Sticker.White else Sticker.Yellow,
                        content = Sticker.Ink,
                        height = 48.dp,
                        radius = 14.dp,
                        shadowOffset = 0.dp,
                        fontSize = 12.sp,
                        horizontalPadding = 2.dp,
                        selected = isSelected
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                dev.personal.linkgrab.ui.home.HomeScreen(initialUrl)
            }
            composable(Screen.History.route) {
                dev.personal.linkgrab.ui.history.HistoryScreen()
            }
            composable(Screen.Downloads.route) {
                dev.personal.linkgrab.ui.downloads.DownloadsScreen()
            }
            composable(Screen.Settings.route) {
                dev.personal.linkgrab.ui.settings.SettingsScreen()
            }
        }
    }
}
