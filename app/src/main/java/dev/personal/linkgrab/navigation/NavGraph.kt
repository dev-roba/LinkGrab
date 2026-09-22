package dev.personal.linkgrab.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "Home")
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
    data object Downloads : Screen("downloads", "Downloads")
}

private val bottomBarScreens = listOf(Screen.Home, Screen.Downloads, Screen.History, Screen.Settings)

@Composable
fun LinkGrabNavGraph(initialUrl: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomBarScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.Home -> Icons.Default.Home
                                    Screen.History -> Icons.Default.History
                                    Screen.Settings -> Icons.Default.Settings
                                    Screen.Downloads -> Icons.Default.Download
                                },
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
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
