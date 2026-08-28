package com.richardittou.qrcodenow.domain.model

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val vibrationEnabled: Boolean = true,
    val autoOpenSafeUrls: Boolean = false
)
