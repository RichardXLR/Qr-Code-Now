package com.richardittou.qrcodenow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.richardittou.qrcodenow.domain.model.AppTheme

private val DarkColors = darkColorScheme(
    primary = BrandRedDark,
    onPrimary = BrandBlack,
    primaryContainer = Color(0xFF5D1015),
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFFFB3B6),
    onSecondary = Color(0xFF65000B),
    secondaryContainer = Color(0xFF3F2022),
    onSecondaryContainer = Color(0xFFFFDADB),
    tertiary = Color(0xFFE6BDBE),
    onTertiary = Color(0xFF432829),
    tertiaryContainer = Color(0xFF321C1D),
    onTertiaryContainer = Color(0xFFFFDADB),
    background = BrandBlack,
    onBackground = Color(0xFFF5F0F0),
    surface = BrandGraphite,
    onSurface = Color(0xFFF5F0F0),
    surfaceVariant = BrandSurface,
    onSurfaceVariant = Color(0xFFD6C2C3),
    surfaceContainer = Color(0xFF202024),
    surfaceContainerHigh = Color(0xFF29292E),
    outline = Color(0xFFA88C8E),
    outlineVariant = Color(0xFF574144),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColors = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADB),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF8F4A4E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDADB),
    onSecondaryContainer = Color(0xFF3B0710),
    tertiary = Color(0xFF775657),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADB),
    onTertiaryContainer = Color(0xFF2D1517),
    background = WarmWhite,
    onBackground = BrandBlack,
    surface = Color.White,
    onSurface = BrandBlack,
    surfaceVariant = SoftGray,
    onSurfaceVariant = Color(0xFF5A4041),
    surfaceContainer = Color(0xFFF5EEEE),
    surfaceContainerHigh = Color(0xFFEFE7E7),
    outline = Color(0xFF8B7173),
    outlineVariant = Color(0xFFDEC2C4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun QRCodeNowTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
