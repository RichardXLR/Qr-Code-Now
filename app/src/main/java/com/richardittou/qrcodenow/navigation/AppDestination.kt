package com.richardittou.qrcodenow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(val route: String, val label: String, val icon: ImageVector?) {
    data object Scanner : AppDestination("scanner", "Scanner", Icons.Outlined.QrCodeScanner)
    data object Generator : AppDestination("generator", "Criar", Icons.Outlined.AddBox)
    data object History : AppDestination("history", "Histórico", Icons.Outlined.History)
    data object Favorites : AppDestination("favorites", "Favoritos", Icons.Outlined.FavoriteBorder)
    data object Settings : AppDestination("settings", "Ajustes", Icons.Outlined.Settings)
    data object Credits : AppDestination("credits", "Créditos", null)
    data object Privacy : AppDestination("privacy", "Privacidade", null)
    data object Licenses : AppDestination("licenses", "Licenças", null)

    companion object {
        val bottom = listOf(Scanner, Generator, History, Favorites, Settings)
    }
}
