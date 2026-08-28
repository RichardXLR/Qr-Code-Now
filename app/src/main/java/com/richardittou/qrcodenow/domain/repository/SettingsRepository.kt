package com.richardittou.qrcodenow.domain.repository

import com.richardittou.qrcodenow.domain.model.AppSettings
import com.richardittou.qrcodenow.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setVibration(enabled: Boolean)
    suspend fun setAutoOpenSafeUrls(enabled: Boolean)
}
