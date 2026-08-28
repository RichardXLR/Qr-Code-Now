package com.richardittou.qrcodenow

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.richardittou.qrcodenow.navigation.AppDestination
import com.richardittou.qrcodenow.presentation.generator.GeneratorScreen
import com.richardittou.qrcodenow.presentation.history.HistoryScreen
import com.richardittou.qrcodenow.presentation.scanner.ScannerScreen
import com.richardittou.qrcodenow.presentation.settings.CreditsScreen
import com.richardittou.qrcodenow.presentation.settings.LicensesScreen
import com.richardittou.qrcodenow.presentation.settings.PrivacyScreen
import com.richardittou.qrcodenow.presentation.settings.SettingsScreen
import com.richardittou.qrcodenow.presentation.settings.SettingsViewModel
import com.richardittou.qrcodenow.ui.theme.QRCodeNowTheme

@Composable
fun QrCodeNowApp(settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    QRCodeNowTheme(settings.theme) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val showBottomBar = AppDestination.bottom.any { it.route == route }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 600.dp && showBottomBar) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(Modifier.width(96.dp)) {
                        AppDestination.bottom.forEach { destination ->
                            NavigationRailItem(
                                selected = route == destination.route,
                                onClick = { navController.openTopLevel(destination) },
                                icon = { Icon(requireNotNull(destination.icon), destination.label) },
                                label = { Text(destination.label, maxLines = 1) }
                            )
                        }
                    }
                    AppNavHost(navController, settingsViewModel, Modifier.weight(1f))
                }
            } else {
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) NavigationBar {
                            AppDestination.bottom.forEach { destination ->
                                NavigationBarItem(
                                    selected = route == destination.route,
                                    onClick = { navController.openTopLevel(destination) },
                                    icon = { Icon(requireNotNull(destination.icon), destination.label) },
                                    label = { Text(destination.label, maxLines = 1) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    AppNavHost(navController, settingsViewModel, Modifier.padding(padding))
                }
            }
        }
    }
}

private fun NavHostController.openTopLevel(destination: AppDestination) {
    navigate(destination.route) {
        popUpTo(AppDestination.Scanner.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = AppDestination.Scanner.route, modifier = modifier) {
        composable(AppDestination.Scanner.route) {
            ScannerScreen(onCreateQr = { navController.openTopLevel(AppDestination.Generator) })
        }
        composable(AppDestination.Generator.route) { GeneratorScreen() }
        composable(AppDestination.History.route) { HistoryScreen(favoritesOnly = false) }
        composable(AppDestination.Favorites.route) { HistoryScreen(favoritesOnly = true) }
        composable(AppDestination.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onCredits = { navController.navigate(AppDestination.Credits.route) },
                onPrivacy = { navController.navigate(AppDestination.Privacy.route) },
                onLicenses = { navController.navigate(AppDestination.Licenses.route) }
            )
        }
        composable(AppDestination.Credits.route) { CreditsScreen(onBack = navController::popBackStack) }
        composable(AppDestination.Privacy.route) { PrivacyScreen(onBack = navController::popBackStack) }
        composable(AppDestination.Licenses.route) { LicensesScreen(onBack = navController::popBackStack) }
    }
}
